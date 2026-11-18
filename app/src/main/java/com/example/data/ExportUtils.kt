package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.toArgb
import com.example.model.AppState
import com.example.ui.theme.MudhakiratiColors
import com.example.util.DateUtils
import com.example.util.Strings
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ExportUtils {

    fun buildBackupJson(email: String, state: AppState): String =
        JSONObject().apply {
            put("app", "mudhakirati")
            put("v", 1)
            put("email", email)
            put("data", JsonSerializer.stateToJson(state))
        }.toString()

    fun buildCsvTasks(state: AppState): String {
        val lang = state.lang
        val head = listOf(
            Strings.pk("المهمة", "Task", lang),
            Strings.pk("المادة", "Subject", lang),
            Strings.pk("الموعد", "Due", lang),
            Strings.pk("الأولوية", "Priority", lang),
            Strings.pk("الحالة", "Status", lang),
        )
        val subjMap = state.subjects.associateBy { it.id }
        val rows = state.tasks.map { k ->
            listOf(
                k.title,
                subjMap[k.subj]?.name ?: "",
                k.due,
                Strings.priorityLabel(k.pri, lang),
                if (k.done) Strings.pk("مكتملة", "Completed", lang) else Strings.pk("غير مكتملة", "Incomplete", lang),
            )
        }
        return "\uFEFF" + (listOf(head) + rows).joinToString("\r\n") { row ->
            row.joinToString(",") { csvCell(it) }
        }
    }

    fun buildCsvLogs(state: AppState): String {
        val lang = state.lang
        val head = listOf(
            Strings.pk("التاريخ", "Date", lang),
            Strings.pk("المادة", "Subject", lang),
            Strings.pk("الدقائق", "Minutes", lang),
        )
        val subjMap = state.subjects.associateBy { it.id }
        val rows = state.logs.sortedBy { it.date }.map { l ->
            listOf(l.date, subjMap[l.subj]?.name ?: "", l.min.toString())
        }
        return "\uFEFF" + (listOf(head) + rows).joinToString("\r\n") { row ->
            row.joinToString(",") { csvCell(it) }
        }
    }

    fun buildExcelXml(state: AppState): String {
        val lang = state.lang
        val ar = lang == "ar"
        val subjMap = state.subjects.associateBy { it.id }

        fun xmlE(s: String): String = s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        fun cell(v: String, isNum: Boolean = false, isHead: Boolean = false): String =
            "<Cell${if (isHead) " ss:StyleID=\"h\"" else ""}><Data ss:Type=\"${if (isNum) "Number" else "String"}\">${xmlE(v)}</Data></Cell>"

        fun sheet(name: String, head: List<String>, rows: List<List<Pair<String, Boolean>>>): String {
            val cleanName = name.replace(Regex("""[\\/?*\[\]:]"""), " ").take(31)
            val headRow = "<Row>${head.joinToString("") { cell(it, isHead = true) }}</Row>"
            val bodyRows = rows.joinToString("") { r ->
                "<Row>${r.joinToString("") { cell(it.first, isNum = it.second) }}</Row>"
            }
            val rtlTag = if (ar) "<DisplayRightToLeft/>" else ""
            return "<Worksheet ss:Name=\"${xmlE(cleanName)}\"><Table ss:DefaultColumnWidth=\"110\">$headRow$bodyRows</Table><WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">$rtlTag</WorksheetOptions></Worksheet>"
        }

        val tHead = listOf(
            Strings.pk("المهمة", "Task", lang),
            Strings.pk("المادة", "Subject", lang),
            Strings.pk("الموعد", "Due", lang),
            Strings.pk("الأولوية", "Priority", lang),
            Strings.pk("الحالة", "Status", lang),
        )
        val tRows = state.tasks.map { k ->
            listOf(
                k.title to false,
                (subjMap[k.subj]?.name ?: "") to false,
                k.due to false,
                Strings.priorityLabel(k.pri, lang) to false,
                (if (k.done) Strings.pk("مكتملة", "Completed", lang) else Strings.pk("غير مكتملة", "Incomplete", lang)) to false,
            )
        }

        val sHead = listOf(
            Strings.pk("الاسم", "Name", lang),
            Strings.pk("الهدف الأسبوعي (ساعة)", "Weekly target (h)", lang),
            Strings.pk("دقائق هذا الأسبوع", "Minutes this week", lang),
            Strings.pk("مجموع الدقائق", "Total minutes", lang),
        )
        val sRows = state.subjects.map { s ->
            val tot = state.logs.filter { it.subj == s.id }.sumOf { it.min }
            listOf(
                s.name to false,
                s.target.toString() to true,
                DateUtils.weekMins(state, s.id).toString() to true,
                tot.toString() to true,
            )
        }

        val lHead = listOf(
            Strings.pk("التاريخ", "Date", lang),
            Strings.pk("المادة", "Subject", lang),
            Strings.pk("الدقائق", "Minutes", lang),
        )
        val lRows = state.logs.sortedBy { it.date }.map { l ->
            listOf(
                l.date to false,
                (subjMap[l.subj]?.name ?: "") to false,
                l.min.toString() to true,
            )
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><?mso-application progid=\"Excel.Sheet\"?>" +
            "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">" +
            "<Styles><Style ss:ID=\"h\"><Font ss:Bold=\"1\"/><Interior ss:Color=\"#E6ECFF\" ss:Pattern=\"Solid\"/></Style></Styles>" +
            sheet(Strings.pk("المهام", "Tasks", lang), tHead, tRows) +
            sheet(Strings.pk("المواد", "Subjects", lang), sHead, sRows) +
            sheet(Strings.pk("سجل الدراسة", "Study log", lang), lHead, lRows) +
            "</Workbook>"
    }

    fun buildIcs(state: AppState): String? {
        val lang = state.lang
        val subjMap = state.subjects.associateBy { it.id }

        fun icsE(s: String): String = s
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(Regex("""\r?\n"""), "\\n")

        fun icsFold(line: String): String {
            val out = mutableListOf<String>()
            val cur = StringBuilder()
            var bytes = 0
            for (ch in line) {
                val b = ch.toString().toByteArray(Charsets.UTF_8).size
                if (bytes + b > 74) {
                    out.add(cur.toString())
                    cur.clear().append(' ').append(ch)
                    bytes = 1 + b
                } else {
                    cur.append(ch)
                    bytes += b
                }
            }
            out.add(cur.toString())
            return out.joinToString("\r\n")
        }

        val stamp = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        val events = mutableListOf<List<String>>()
        state.tasks.filter { !it.done && it.due.isNotBlank() }.forEach { k ->
            val s = subjMap[k.subj]
            val endKey = DateUtils.addDaysKey(1, k.due).replace("-", "")
            val startKey = k.due.replace("-", "")
            val ev = mutableListOf(
                "BEGIN:VEVENT",
                "UID:${k.id}@mudhakirati",
                "DTSTAMP:$stamp",
                "DTSTART;VALUE=DATE:$startKey",
                "DTEND;VALUE=DATE:$endKey",
                "SUMMARY:${icsE((if (k.pri == "high") "! " else "") + k.title)}",
            )
            if (s != null) {
                ev.add("DESCRIPTION:${icsE("${s.name} · ${Strings.priorityLabel(k.pri, lang)}")}")
                ev.add("CATEGORIES:${icsE(s.name)}")
            }
            ev.addAll(
                listOf(
                    "TRANSP:TRANSPARENT",
                    "BEGIN:VALARM",
                    "ACTION:DISPLAY",
                    "DESCRIPTION:${icsE(k.title)}",
                    "TRIGGER:PT9H",
                    "END:VALARM",
                    "END:VEVENT",
                )
            )
            events.add(ev)
        }
        val pl = DateUtils.planInfo(state)
        if (pl != null && !pl.done && state.plan != null) {
            val endDate = DateUtils.addDaysKey(state.plan.days, state.plan.start)
            val endNext = DateUtils.addDaysKey(1, endDate)
            events.add(
                listOf(
                    "BEGIN:VEVENT",
                    "UID:plan-${state.plan.start}@mudhakirati",
                    "DTSTAMP:$stamp",
                    "DTSTART;VALUE=DATE:${endDate.replace("-", "")}",
                    "DTEND;VALUE=DATE:${endNext.replace("-", "")}",
                    "SUMMARY:${icsE(pl.name.ifBlank { Strings.pk("العد التنازلي", "Countdown", lang) })}",
                    "END:VEVENT",
                )
            )
        }
        if (events.isEmpty()) return null
        val lines = mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//Mudhakirati//AR",
            "CALSCALE:GREGORIAN",
            "X-WR-CALNAME:${icsE(Strings.pk("مذاكرتي", "Mudhakirati", lang))}",
        )
        events.forEach { lines.addAll(it) }
        lines.add("END:VCALENDAR")
        return lines.joinToString("\r\n") { icsFold(it) } + "\r\n"
    }

    private fun csvCell(raw: String): String {
        var v = raw
        if (Regex("""^[=+\-@\t\r]""").containsMatchIn(v) && v.toDoubleOrNull() == null) {
            v = "'$v"
        }
        return if (Regex("""[",\r\n]""").containsMatchIn(v)) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else {
            v
        }
    }

    fun saveToDownloads(context: Context, fileName: String, mimeType: String, content: ByteArray): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(content) }
                }
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(content) }
                Uri.fromFile(file)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun renderStreakBitmap(state: AppState, colors: MudhakiratiColors): Bitmap {
        val w = 1080
        val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val n = DateUtils.computeStreak(state)
        val lang = state.lang
        val ar = lang == "ar"

        // Background
        p.color = colors.bg.toArgb()
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

        // Radial glow
        val hotArgb = colors.hot.toArgb()
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = 72
            shader = RadialGradient(
                w / 2f, 430f, 620f,
                hotArgb, hotArgb and 0x00FFFFFF,
                Shader.TileMode.CLAMP,
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glowPaint)

        // Surface card
        p.shader = null
        p.alpha = 255
        p.color = colors.surface.toArgb()
        c.drawRoundRect(RectF(70f, 90f, w - 70f, h - 90f), 64f, 64f, p)

        // Flame icon silhouette
        val flPath = Path().apply {
            moveTo(12f, 3f)
            cubicTo(13f, 6.5f, 17f, 8.5f, 17f, 13f)
            cubicTo(17f, 15.76f, 14.76f, 18f, 12f, 18f)
            cubicTo(9.24f, 18f, 7f, 15.76f, 7f, 13f)
            cubicTo(7f, 11f, 8f, 10f, 9f, 9f)
            cubicTo(9f, 11f, 10f, 12f, 11f, 12f)
            cubicTo(11f, 9f, 10f, 7f, 12f, 3f)
            close()
        }
        c.save()
        c.translate(w / 2f - 168f, 165f)
        c.scale(14f, 14f)
        p.color = hotArgb
        c.drawPath(flPath, p)
        c.restore()

        // Streak big number
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        p.color = colors.ink.toArgb()
        p.textSize = 250f
        c.drawText(DateUtils.applyDigits(n.toString(), lang, state.arDigits), w / 2f, 710f, p)

        p.color = colors.muted.toArgb()
        p.textSize = 60f
        c.drawText(Strings.pk("أيام متتالية", "Day streak", lang), w / 2f, 800f, p)

        val nm = state.profile.name
        if (nm.isNotBlank()) {
            p.color = colors.ink.toArgb()
            p.textSize = 54f
            c.drawText(nm, w / 2f, 895f, p)
        }

        val wkMin = (0..6).sumOf { DateUtils.minsOn(state, DateUtils.addDaysKey(-it)) }
        p.color = colors.muted.toArgb()
        p.textSize = 44f
        c.drawText(
            "${Strings.pk("هذا الأسبوع", "This week", lang)}: ${Strings.fmtHours(wkMin / 60.0, lang, state.arDigits)}",
            w / 2f,
            if (nm.isNotBlank()) 960f else 920f,
            p,
        )

        // Last 7 days circles
        val cy = 1075f
        val r = 38f
        val gap = 122f
        val x0 = w / 2f - gap * 3f
        val daysNames = if (ar) Strings.daysShortAr else Strings.daysShortEn
        val cal = java.util.Calendar.getInstance()
        for (i in 0..6) {
            val offset = i - 6
            val key = DateUtils.addDaysKey(offset)
            val on = DateUtils.actOn(state, key)
            val cx = x0 + i * gap
            if (on) {
                p.style = Paint.Style.FILL
                p.color = hotArgb
                c.drawCircle(cx, cy, r, p)
                p.style = Paint.Style.STROKE
                p.color = 0xFFFFFFFF.toInt()
                p.strokeWidth = 7f
                p.strokeCap = Paint.Cap.ROUND
                c.drawLine(cx - 14f, cy + 1f, cx - 4f, cy + 12f, p)
                c.drawLine(cx - 4f, cy + 12f, cx + 15f, cy - 10f, p)
            } else {
                p.style = Paint.Style.STROKE
                p.color = colors.line.toArgb()
                p.strokeWidth = 6f
                c.drawCircle(cx, cy, r, p)
            }
            cal.time = DateUtils.parseDateKey(key) ?: java.util.Date()
            val dow = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
            p.style = Paint.Style.FILL
            p.color = colors.muted.toArgb()
            p.textSize = 32f
            c.drawText(daysNames[dow], cx, cy + 78f, p)
        }

        // App wordmark
        p.color = colors.accent.toArgb()
        p.textSize = 58f
        c.drawText(Strings.pk("مذاكرتي", "Mudhakirati", lang), w / 2f, 1265f, p)

        return bmp
    }

    fun printStudyReport(context: Context, state: AppState) {
        val html = buildPrintHtml(state)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val adapter = view.createPrintDocumentAdapter("Mudhakirati_${DateUtils.todayKey()}")
                pm.print(Strings.pk("مذاكرتي", "Mudhakirati", state.lang), adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun buildPrintHtml(state: AppState): String {
        val lang = state.lang
        val ar = lang == "ar"
        val dir = if (ar) "rtl" else "ltr"
        val td = DateUtils.todayKey()
        val goal = state.settings.goal
        val days = (6 downTo 0).map { DateUtils.addDaysKey(-it) }
        val pl = DateUtils.planInfo(state)
        val pn = state.profile.name

        val daysHeader = days.joinToString("") { k ->
            val shortWd = DateUtils.formatWeekdayShort(k, lang, state.arDigits)
            val dm = DateUtils.formatDayMonthNum(k, lang, state.arDigits)
            "<th>${esc(shortWd)}<br><small>${esc(dm)}</small></th>"
        }

        val subjRows = state.subjects.joinToString("") { s ->
            val w = DateUtils.weekMins(state, s.id)
            val tg = (s.target * 60.0).roundToInt()
            val pct = if (tg > 0) minOf(100, ((w.toDouble() / tg) * 100).roundToInt()) else 0
            val cells = days.joinToString("") { d ->
                val v = DateUtils.minsOn(state, d, s.id)
                "<td>${if (v > 0) DateUtils.applyDigits(v.toString(), lang, state.arDigits) else "–"}</td>"
            }
            "<tr><td><span class=\"dot\" style=\"background:${esc(s.color)}\"></span>${esc(s.name)}</td>$cells" +
                "<td><b>$w</b></td><td>${if (tg > 0) tg else "–"}</td><td>${if (tg > 0) "$pct%" else "–"}</td></tr>"
        }

        val totalCells = days.joinToString("") { d ->
            val v = DateUtils.minsOn(state, d)
            "<td>${if (v > 0) v else "–"}${if (goal > 0 && v >= goal) " ✓" else ""}</td>"
        }

        val taskRows = (state.tasks.filter { !it.done } + state.tasks.filter { it.done }).joinToString("") { k ->
            val s = state.subjects.firstOrNull { it.id == k.subj }
            "<tr><td>${if (k.done) "✓" else ""}</td><td>${esc(k.title)}</td><td>${esc(s?.name ?: "–")}</td>" +
                "<td>${if (k.due.isNotBlank()) esc(Strings.dueLabel(k.due, lang, state.arDigits)) else "–"}</td>" +
                "<td>${esc(Strings.priorityLabel(k.pri, lang))}</td></tr>"
        }

        return """
            <!doctype html><html lang="$lang" dir="$dir"><head><meta charset="utf-8">
            <style>
            body{font-family:sans-serif;padding:20px;color:#1C1914;background:#fff}
            h1{margin:0;font-size:22px} h2{margin:20px 0 8px;font-size:15px}
            table{width:100%;border-collapse:collapse;font-size:12px}
            th,td{border:1px solid #DDD;padding:6px;text-align:center}
            th:first-child,td:first-child{text-align:start}
            .dot{display:inline-block;width:9px;height:9px;border-radius:50%;margin-inline-end:6px}
            </style></head><body>
            <h1>${esc(Strings.pk("مذاكرتي", "Mudhakirati", lang))}</h1>
            <div>${if (pn.isNotBlank()) "${esc(pn)} · " else ""}${esc(DateUtils.formatFullPrintDate(lang, state.arDigits))}
            ${if (pl != null) " · ${esc(pl.name.ifBlank { Strings.pk("العد التنازلي", "Countdown", lang) })}: ${pl.left} ${esc(Strings.daysWord(pl.left, lang))}" else ""}</div>
            <h2>${esc(Strings.pk("جدول آخر 7 أيام (بالدقائق)", "Last 7 days (minutes)", lang))}</h2>
            <table><thead><tr><th>${esc(Strings.pk("المادة", "Subject", lang))}</th>$daysHeader<th>${esc(Strings.pk("المجموع", "Total", lang))}</th><th>${esc(Strings.pk("الهدف", "Target", lang))}</th><th>%</th></tr></thead>
            <tbody>$subjRows</tbody>
            <tfoot><tr><td><b>${esc(Strings.pk("الإجمالي", "Total", lang))}</b></td>$totalCells<td><b>${DateUtils.weekMins(state)}</b></td><td>${(state.subjects.sumOf { it.target } * 60).roundToInt()}</td><td></td></tr></tfoot></table>
            <h2>${esc(Strings.pk("المهام", "Tasks", lang))}</h2>
            <table><thead><tr><th>✓</th><th>${esc(Strings.pk("المهمة", "Task", lang))}</th><th>${esc(Strings.pk("المادة", "Subject", lang))}</th><th>${esc(Strings.pk("الموعد", "Due", lang))}</th><th>${esc(Strings.pk("الأولوية", "Priority", lang))}</th></tr></thead>
            <tbody>$taskRows</tbody></table>
            </body></html>
        """.trimIndent()
    }
}
