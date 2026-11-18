package com.example.data

import com.example.model.*
import com.example.ui.theme.AppPalettes
import com.example.ui.theme.SubjectColors
import com.example.ui.theme.colorToHex
import com.example.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

object JsonSerializer {
    private const val SCHEMA = 3
    private val THEME_IDS = setOf("", "light", "dark", "custom")
    private val PALETTE_IDS = AppPalettes.map { it.id }.toSet()
    private val LAYOUT_IDS = setOf("auto", "phone", "tablet", "laptop")
    private val SCH_ORDER = listOf(6, 0, 1, 2, 3, 4, 5)
    private val COLOR_HEXES = SubjectColors.map { colorToHex(it) }

    fun seed(lang: String = "ar"): AppState = AppState(
        v = SCHEMA,
        lang = if (lang == "en") "en" else "ar",
        view = AppTab.HOME,
        theme = "custom",
        palette = AppPalettes[0].id,
        layout = "auto",
        onboarded = false,
    )

    fun stateToJson(s: AppState): JSONObject = JSONObject().apply {
        put("v", SCHEMA)
        put("lang", s.lang)
        put("view", s.view.id)
        put("sched", JSONObject().apply {
            put("n", s.sched.n)
            put("sat", s.sched.sat)
            put("days", JSONArray(s.sched.days))
            put("cells", JSONObject().apply {
                s.sched.cells.forEach { (k, v) ->
                    put(k, JSONObject().apply { put("t", v) })
                }
            })
        })
        put("act", JSONObject().apply {
            s.act.forEach { d -> put(d, 1) }
        })
        put("lockDone", s.lockDone)
        put("notes", JSONArray().apply {
            s.notes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("txt", n.txt)
                    put("color", n.color)
                    put("pin", n.pin)
                    put("ts", n.ts)
                })
            }
        })
        put("fid", s.fid)
        put("nowT", s.nowT)
        put("nowAt", s.nowAt)
        put("settings", JSONObject().apply {
            put("focus", s.settings.focus)
            put("brk", s.settings.brk)
            put("goal", s.settings.goal)
        })
        put("subjects", JSONArray().apply {
            s.subjects.forEach { sb ->
                put(JSONObject().apply {
                    put("id", sb.id)
                    put("name", sb.name)
                    put("color", sb.color)
                    put("target", sb.target)
                    put("parts", JSONArray().apply {
                        sb.parts.forEach { p ->
                            put(JSONObject().apply {
                                put("id", p.id)
                                put("name", p.name)
                                put("hours", p.hours)
                                put("lec", p.lec)
                                put("done", p.done)
                            })
                        }
                    })
                    put("doneH", sb.doneH)
                    if (sb.day != null) {
                        put("day", JSONObject().apply {
                            put("date", sb.day.date)
                            put("amt", sb.day.amt)
                            put("logId", sb.day.logId)
                        })
                    }
                    if (sb.ex) put("ex", true)
                })
            }
        })
        put("tasks", JSONArray().apply {
            s.tasks.forEach { tk ->
                put(JSONObject().apply {
                    put("id", tk.id)
                    put("title", tk.title)
                    put("subj", tk.subj)
                    put("due", tk.due)
                    put("pri", tk.pri)
                    put("rem", tk.rem)
                    put("remLast", tk.remLast)
                    put("done", tk.done)
                    if (tk.ex) put("ex", true)
                })
            }
        })
        put("logs", JSONArray().apply {
            s.logs.forEach { l ->
                put(JSONObject().apply {
                    put("id", l.id)
                    put("subj", l.subj)
                    put("min", l.min)
                    put("date", l.date)
                    if (l.ex) put("ex", true)
                })
            }
        })
        put("reviews", JSONArray().apply {
            s.reviews.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("hours", r.hours)
                    put("days", r.days)
                    put("start", r.start)
                    put("chk", r.chk)
                    put("dd", JSONObject().apply { r.dd.forEach { put(it, 1) } })
                    put("ext", r.ext)
                    put("doneH", r.doneH)
                    if (r.day != null) {
                        put("day", JSONObject().apply {
                            put("date", r.day.date)
                            put("amt", r.day.amt)
                            put("logId", r.day.logId)
                        })
                    }
                    put("kind", r.kind)
                    put("color", r.color)
                })
            }
        })
        put("lists", JSONArray().apply {
            s.lists.forEach { li ->
                put(JSONObject().apply {
                    put("id", li.id)
                    put("kind", li.kind)
                    put("name", li.name)
                    put("done", li.done)
                })
            }
        })
        if (s.plan != null) {
            put("plan", JSONObject().apply {
                put("days", s.plan.days)
                put("start", s.plan.start)
                put("name", s.plan.name)
            })
        }
        put("profile", JSONObject().apply {
            put("name", s.profile.name)
            put("photo", s.profile.photo)
        })
        put("remind", JSONObject().apply {
            put("on", s.remind.on)
            put("time", s.remind.time)
            put("subj", s.remind.subj)
            put("last", s.remind.last)
        })
        put("theme", s.theme)
        put("palette", s.palette)
        put("layout", s.layout)
        put("taskSort", s.taskSort.id)
        put("taskView", s.taskView.id)
        put("progRange", s.progRange.id)
        put("arDigits", s.arDigits)
        put("onboarded", s.onboarded)
    }

    fun migrate(raw: JSONObject?, fallbackLang: String = "ar"): AppState {
        if (raw == null) return seed(fallbackLang)
        val lang = if (raw.optString("lang") == "en") "en" else "ar"
        val view = AppTab.fromId(raw.optString("view"))
        val stObj = raw.optJSONObject("settings")
        val focus = stObj?.optInt("focus", 25)?.takeIf { it > 0 } ?: 25
        val brk = stObj?.optInt("brk", 5)?.takeIf { it > 0 } ?: 5
        val goal = stObj?.optInt("goal", 120)?.takeIf { it > 0 } ?: 120
        val settings = TimerSettings(focus = focus, brk = min(60, brk), goal = goal)

        val subjectsArr = raw.optJSONArray("subjects")
        val subjects = mutableListOf<Subject>()
        if (subjectsArr != null) {
            for (i in 0 until subjectsArr.length()) {
                val obj = subjectsArr.optJSONObject(i) ?: continue
                val name = obj.optString("name").trim()
                if (name.isEmpty()) continue
                val partsArr = obj.optJSONArray("parts")
                val parts = mutableListOf<SubjectPart>()
                if (partsArr != null) {
                    for (j in 0 until partsArr.length()) {
                        val p = partsArr.optJSONObject(j) ?: continue
                        parts.add(
                            SubjectPart(
                                id = p.optString("id").ifEmpty { DateUtils.uid() },
                                name = p.optString("name", ""),
                                hours = max(0.0, p.optDouble("hours", 0.0).takeIf { !it.isNaN() } ?: 0.0),
                                lec = max(0, p.optInt("lec", 0)),
                                done = p.optBoolean("done", false),
                            )
                        )
                    }
                }
                val dayObj = obj.optJSONObject("day")
                val dayMark = if (dayObj != null && DateUtils.isValidDateKey(dayObj.optString("date"))) {
                    DayAmountMark(
                        date = dayObj.optString("date"),
                        amt = max(0.0, dayObj.optDouble("amt", 0.0).takeIf { !it.isNaN() } ?: 0.0),
                        logId = dayObj.optString("logId", ""),
                    )
                } else null
                val tgt = obj.optDouble("target", 3.0).takeIf { !it.isNaN() && it >= 0 } ?: 3.0
                subjects.add(
                    Subject(
                        id = obj.optString("id").ifEmpty { DateUtils.uid() },
                        name = name,
                        color = obj.optString("color").ifEmpty { COLOR_HEXES[0] },
                        target = tgt,
                        parts = parts,
                        doneH = max(0.0, obj.optDouble("doneH", 0.0).takeIf { !it.isNaN() } ?: 0.0),
                        day = dayMark,
                        ex = obj.optBoolean("ex", false),
                    )
                )
            }
        }

        val tasksArr = raw.optJSONArray("tasks")
        val tasks = mutableListOf<StudyTask>()
        if (tasksArr != null) {
            for (i in 0 until tasksArr.length()) {
                val obj = tasksArr.optJSONObject(i) ?: continue
                val title = obj.optString("title").trim()
                if (title.isEmpty()) continue
                val due = obj.optString("due").takeIf { DateUtils.isValidDateKey(it) } ?: ""
                val pri = obj.optString("pri").takeIf { it == "high" || it == "med" || it == "low" } ?: "med"
                val rem = obj.optString("rem").takeIf { Regex("""^\d{2}:\d{2}$""").matches(it) } ?: ""
                tasks.add(
                    StudyTask(
                        id = obj.optString("id").ifEmpty { DateUtils.uid() },
                        title = title,
                        subj = obj.optString("subj", ""),
                        due = due,
                        pri = pri,
                        rem = rem,
                        remLast = obj.optString("remLast", ""),
                        done = obj.optBoolean("done", false),
                        ex = obj.optBoolean("ex", false),
                    )
                )
            }
        }

        val logsArr = raw.optJSONArray("logs")
        val logs = mutableListOf<StudyLog>()
        if (logsArr != null) {
            for (i in 0 until logsArr.length()) {
                val obj = logsArr.optJSONObject(i) ?: continue
                val date = obj.optString("date")
                val minVal = obj.optInt("min", 0)
                if (!DateUtils.isValidDateKey(date) || minVal <= 0) continue
                logs.add(
                    StudyLog(
                        id = obj.optString("id").ifEmpty { DateUtils.uid() },
                        subj = obj.optString("subj", ""),
                        min = minVal,
                        date = date,
                        ex = obj.optBoolean("ex", false),
                    )
                )
            }
        }

        val reviewsArr = raw.optJSONArray("reviews")
        val reviews = mutableListOf<ReviewItem>()
        if (reviewsArr != null) {
            for (i in 0 until reviewsArr.length()) {
                val obj = reviewsArr.optJSONObject(i) ?: continue
                val name = obj.optString("name").trim()
                val hrs = obj.optDouble("hours", 0.0)
                val dys = obj.optInt("days", 0)
                if (name.isEmpty() || hrs.isNaN() || hrs <= 0.0 || dys <= 0) continue
                val start = obj.optString("start").takeIf { DateUtils.isValidDateKey(it) } ?: DateUtils.todayKey()
                val ddSet = mutableSetOf<String>()
                obj.optJSONObject("dd")?.keys()?.forEach { k ->
                    if (DateUtils.isValidDateKey(k)) ddSet.add(k)
                }
                val dayObj = obj.optJSONObject("day")
                val dayMark = if (dayObj != null && DateUtils.isValidDateKey(dayObj.optString("date"))) {
                    DayAmountMark(
                        date = dayObj.optString("date"),
                        amt = max(0.0, dayObj.optDouble("amt", 0.0).takeIf { !it.isNaN() } ?: 0.0),
                        logId = dayObj.optString("logId", ""),
                    )
                } else null
                val kd = obj.optString("kind").takeIf { it == "misc" || it == "hw" } ?: "rev"
                reviews.add(
                    ReviewItem(
                        id = obj.optString("id").ifEmpty { DateUtils.uid() },
                        name = name,
                        hours = hrs,
                        days = dys,
                        start = start,
                        chk = obj.optString("chk", ""),
                        dd = ddSet,
                        ext = obj.optInt("ext", 0),
                        doneH = max(0.0, obj.optDouble("doneH", 0.0).takeIf { !it.isNaN() } ?: 0.0),
                        day = dayMark,
                        kind = kd,
                        color = obj.optString("color").ifEmpty { COLOR_HEXES[i % COLOR_HEXES.size] },
                    )
                )
            }
        }

        val actSet = mutableSetOf<String>()
        raw.optJSONObject("act")?.let { actObj ->
            actObj.keys().forEach { k ->
                if (DateUtils.isValidDateKey(k) && actObj.optInt(k, 0) != 0) actSet.add(k)
            }
        }

        val listsArr = raw.optJSONArray("lists")
        val lists = mutableListOf<CheckListItem>()
        if (listsArr != null) {
            for (i in 0 until listsArr.length()) {
                val obj = listsArr.optJSONObject(i) ?: continue
                val name = obj.optString("name").trim()
                if (name.isEmpty()) continue
                lists.add(
                    CheckListItem(
                        id = obj.optString("id").ifEmpty { DateUtils.uid() },
                        kind = if (obj.optString("kind") == "hw") "hw" else "misc",
                        name = name,
                        done = obj.optBoolean("done", false),
                    )
                )
            }
        }

        val schedObj = raw.optJSONObject("sched")
        val schN = min(12, max(1, schedObj?.optInt("n", 7) ?: 7))
        val satLegacy = schedObj?.optBoolean("sat", false) ?: false
        val daysRawArr = schedObj?.optJSONArray("days")
        val daysRaw = mutableListOf<Int>()
        if (daysRawArr != null) {
            for (i in 0 until daysRawArr.length()) {
                val v = daysRawArr.optInt(i, -1)
                if (v in 0..6) daysRaw.add(v)
            }
        }
        val schDays = SCH_ORDER.filter { daysRaw.contains(it) }.ifEmpty {
            if (satLegacy) listOf(6, 0, 1, 2, 3, 4) else listOf(0, 1, 2, 3, 4)
        }
        val schCells = mutableMapOf<String, String>()
        schedObj?.optJSONObject("cells")?.let { cObj ->
            cObj.keys().forEach { ck ->
                if (Regex("""^\d-\d{1,2}$""").matches(ck)) {
                    val txt = cObj.optJSONObject(ck)?.optString("t")?.trim()?.take(30) ?: ""
                    if (txt.isNotEmpty()) schCells[ck] = txt
                }
            }
        }

        val notesArr = raw.optJSONArray("notes")
        val notes = mutableListOf<StudyNote>()
        if (notesArr != null) {
            for (i in 0 until notesArr.length()) {
                val obj = notesArr.optJSONObject(i) ?: continue
                val txt = obj.optString("txt").trim()
                if (txt.isEmpty()) continue
                notes.add(
                    StudyNote(
                        id = obj.optString("id").ifEmpty { DateUtils.uid() },
                        title = obj.optString("title", "").take(60),
                        txt = txt.take(600),
                        color = obj.optString("color").ifEmpty { COLOR_HEXES[5] },
                        pin = obj.optBoolean("pin", false),
                        ts = obj.optLong("ts", 0L),
                    )
                )
            }
        }

        val planObj = raw.optJSONObject("plan")
        val plan = if (planObj != null && planObj.optInt("days", 0) > 0 && DateUtils.isValidDateKey(planObj.optString("start"))) {
            StudyPlan(
                days = planObj.optInt("days"),
                start = planObj.optString("start"),
                name = planObj.optString("name", ""),
            )
        } else null

        val profObj = raw.optJSONObject("profile")
        val profile = UserProfile(
            name = profObj?.optString("name", "") ?: "",
            photo = profObj?.optString("photo", "") ?: "",
        )

        val remObj = raw.optJSONObject("remind")
        val remind = DailyReminder(
            on = remObj?.optBoolean("on", false) ?: false,
            time = remObj?.optString("time", "18:00")?.takeIf { Regex("""^\d{2}:\d{2}$""").matches(it) } ?: "18:00",
            subj = remObj?.optString("subj", "") ?: "",
            last = remObj?.optString("last", "") ?: "",
        )

        val rawTheme = raw.optString("theme", "custom")
        val theme = if (THEME_IDS.contains(rawTheme)) rawTheme else "custom"
        val rawPal = raw.optString("palette", AppPalettes[0].id)
        val palette = if (PALETTE_IDS.contains(rawPal)) rawPal else AppPalettes[0].id
        val rawLay = raw.optString("layout", "auto")
        val layout = if (LAYOUT_IDS.contains(rawLay)) rawLay else "auto"
        val fid = raw.optString("fid", "").takeIf { Regex("""^[A-Z0-9]{8}$""").matches(it) } ?: ""
        val nowT = raw.optString("nowT", "").take(60)
        val nowAt = if (nowT.isNotEmpty()) raw.optLong("nowAt", 0L) else 0L

        return AppState(
            v = SCHEMA,
            lang = lang,
            view = view,
            sched = WeeklySchedule(n = schN, sat = schDays.contains(6), days = schDays, cells = schCells),
            act = actSet,
            lockDone = if (raw.has("lockDone")) raw.optBoolean("lockDone", true) else true,
            notes = notes,
            fid = fid,
            nowT = nowT,
            nowAt = nowAt,
            settings = settings,
            subjects = subjects,
            tasks = tasks,
            logs = logs,
            reviews = reviews,
            lists = lists,
            plan = plan,
            profile = profile,
            remind = remind,
            theme = theme,
            palette = palette,
            layout = layout,
            taskSort = if (raw.optString("taskSort") == "manual") SortMode.MANUAL else SortMode.AUTO,
            taskView = if (raw.optString("taskView") == "week") TaskViewMode.WEEK else TaskViewMode.LIST,
            progRange = if (raw.optString("progRange") == "month") ProgressRange.MONTH else ProgressRange.WEEK,
            arDigits = raw.optBoolean("arDigits", false),
            onboarded = if (raw.has("onboarded")) raw.optBoolean("onboarded", true) else true,
        )
    }

    fun compactLogs(logs: List<StudyLog>): List<StudyLog> {
        val cut = DateUtils.addDaysKey(-60)
        val keep = mutableListOf<StudyLog>()
        val oldGrouped = linkedMapOf<String, StudyLog>()
        for (l in logs) {
            if (l.min <= 0) continue
            if (l.date < cut) {
                val k = "${l.date}|${l.subj}"
                val prev = oldGrouped[k]
                if (prev != null) {
                    oldGrouped[k] = prev.copy(min = prev.min + l.min)
                } else {
                    oldGrouped[k] = l
                }
            } else {
                keep.add(l)
            }
        }
        return oldGrouped.values.toList() + keep
    }

    fun rollReviews(reviews: List<ReviewItem>): List<ReviewItem> {
        val td = DateUtils.todayKey()
        return reviews.map { r ->
            if (r.chk.isEmpty()) {
                val dd = r.dd.toMutableSet()
                if (r.day != null && r.day.date.isNotEmpty()) dd.add(r.day.date)
                r.copy(chk = td, dd = dd)
            } else {
                var ext = r.ext
                var cursor = r.chk
                var guard = 0
                while (cursor < td && guard < 400) {
                    guard++
                    if (cursor >= r.start && !r.dd.contains(cursor) && max(0.0, r.hours - r.doneH) > 0.0) {
                        ext++
                    }
                    cursor = DateUtils.addDaysKey(1, cursor)
                }
                if (r.chk != td || ext != r.ext) r.copy(chk = td, ext = ext) else r
            }
        }
    }
}
