package com.example.util

import com.example.model.AppState
import com.example.model.Subject
import kotlin.math.roundToInt

data class ReviewKindMeta(
    val title: String,
    val nameLabel: String,
    val placeholder: String,
    val hoursLabel: String,
    val daysLabel: String,
    val hint: String,
)

data class DefaultSubjectPreset(
    val ar: String,
    val en: String,
    val targetHours: Double,
)

object Strings {
    val default6thSubjects = listOf(
        DefaultSubjectPreset("الإسلامية", "Islamic", 2.0),
        DefaultSubjectPreset("اللغة العربية", "Arabic", 3.0),
        DefaultSubjectPreset("اللغة الإنكليزية", "English", 3.0),
        DefaultSubjectPreset("الفيزياء", "Physics", 5.0),
        DefaultSubjectPreset("الكيمياء", "Chemistry", 5.0),
        DefaultSubjectPreset("الأحياء", "Biology", 5.0),
        DefaultSubjectPreset("الرياضيات", "Mathematics", 5.0),
    )

    val daysShortAr = listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")
    val daysShortEn = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val daysLongAr = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val daysLongEn = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val scheduleDayOrder = listOf(6, 0, 1, 2, 3, 4, 5) // Sat..Fri

    fun pk(ar: String, en: String, lang: String): String = if (lang == "en") en else ar

    fun nfmt(text: String, lang: String, arDigits: Boolean): String =
        DateUtils.applyDigits(text, lang, arDigits)

    fun fmtMin(m: Int, lang: String, arDigits: Boolean): String {
        val raw = if (m < 60) {
            "$m ${pk("دقيقة", "min", lang)}"
        } else {
            val h = m / 60
            val r = m % 60
            "$h ${pk("ساعة", "h", lang)}" + if (r > 0) " $r ${pk("دقيقة", "min", lang)}" else ""
        }
        return nfmt(raw, lang, arDigits)
    }

    fun fmtHours(h: Double, lang: String, arDigits: Boolean): String {
        val roundedMin = ((h * 60.0) / 5.0).roundToInt() * 5
        return fmtMin(roundedMin, lang, arDigits)
    }

    fun daysWord(n: Int, lang: String): String = if (lang == "ar") {
        when {
            n == 2 -> "يومان"
            n in 3..10 -> "أيام"
            n == 1 -> "يوم"
            else -> "يومًا"
        }
    } else {
        if (n == 1) "day" else "days"
    }

    fun greetingText(index: Int, name: String, lang: String): String {
        val list = if (lang == "ar") {
            listOf("صباح الخير", "نهارك سعيد", "مساء الخير", "ليلة موفقة")
        } else {
            listOf("Good morning", "Good afternoon", "Good evening", "Good night")
        }
        val base = list[index.coerceIn(0, 3)]
        return if (name.isNotBlank()) "$base${if (lang == "ar") "، " else ", "}$name" else base
    }

    fun openTasksMsg(count: Int, lang: String, arDigits: Boolean): String {
        val raw = if (lang == "en") {
            if (count > 0) "$count incomplete task${if (count > 1) "s" else ""}" else "No incomplete tasks"
        } else {
            when {
                count == 0 -> "لا توجد مهام غير مكتملة"
                count == 1 -> "عندك مهمة واحدة غير مكتملة"
                count == 2 -> "عندك مهمتان غير مكتملتين"
                count <= 10 -> "عندك $count مهام غير مكتملة"
                else -> "عندك $count مهمة غير مكتملة"
            }
        }
        return nfmt(raw, lang, arDigits)
    }

    fun doneTasksMsg(count: Int, lang: String, arDigits: Boolean): String {
        val raw = if (lang == "en") {
            if (count > 0) "$count completed task${if (count > 1) "s" else ""}" else "No completed tasks"
        } else {
            when {
                count == 0 -> "لا توجد مهام مكتملة"
                count == 1 -> "عندك مهمة واحدة مكتملة"
                count == 2 -> "عندك مهمتان مكتملتان"
                count <= 10 -> "عندك $count مهام مكتملة"
                else -> "عندك $count مهمة مكتملة"
            }
        }
        return nfmt(raw, lang, arDigits)
    }

    fun flameStage(streak: Int): Int = (streak / 4).coerceIn(0, 5)

    fun flameStageName(streak: Int, lang: String): String {
        val ar = listOf("شرارة", "جمرة متوهجة", "لهب مشتعل", "لهب قوي", "نار كبيرة", "نار مشتعلة قوية")
        val en = listOf("Spark", "Glowing ember", "Burning flame", "Strong flame", "Big fire", "Blazing fire")
        val s = flameStage(streak)
        return (if (lang == "ar") ar else en)[s]
    }

    fun fontSizeName(idx: Int, lang: String): String {
        val pairs = listOf(
            "صغير جدًا" to "Extra small",
            "صغير" to "Small",
            "متوسط" to "Medium",
            "كبير" to "Large",
            "كبير جدًا" to "Extra large",
        )
        val p = pairs.getOrElse(idx) { pairs[2] }
        return pk(p.first, p.second, lang)
    }

    fun reviewMeta(kind: String, lang: String): ReviewKindMeta = when (kind) {
        "misc" -> ReviewKindMeta(
            title = pk("مواضيع متفرقة", "Misc topics", lang),
            nameLabel = pk("اسم الموضوع", "Topic name", lang),
            placeholder = pk("مثال: موضوع التعبير", "e.g. Essay topic", lang),
            hoursLabel = pk("مجموع الوقت المطلوب (ساعات)", "Total time needed (hours)", lang),
            daysLabel = pk("عدد الأيام المتاحة للموضوع", "Days available for the topic", lang),
            hint = pk("موضوع واحد: وقته يُقسَّم على الأيام المتاحة وتشوف حصتك اليومية", "One topic: its time is divided across the available days to give your daily share", lang),
        )
        "hw" -> ReviewKindMeta(
            title = pk("الواجبات المنزلية", "Homework", lang),
            nameLabel = pk("اسم الواجب", "Homework name", lang),
            placeholder = pk("مثال: واجب الرياضيات", "e.g. Math homework", lang),
            hoursLabel = pk("مجموع وقت الواجب (ساعات)", "Total homework time (hours)", lang),
            daysLabel = pk("عدد الأيام المتاحة للتسليم", "Days until it is due", lang),
            hint = pk("واجب واحد: وقته يُقسَّم على الأيام المتاحة وتشوف حصتك اليومية", "One assignment: its time is divided across the available days to give your daily share", lang),
        )
        else -> ReviewKindMeta(
            title = pk("مراجعة مركزة", "Focused review", lang),
            nameLabel = pk("اسم الفصل", "Chapter name", lang),
            placeholder = pk("مثال: الفصل الأول", "e.g. Chapter 1", lang),
            hoursLabel = pk("مجموع وقت مراجعة الفصل (ساعات)", "Total review time for the chapter (hours)", lang),
            daysLabel = pk("عدد الأيام المتاحة للفصل", "Days available for the chapter", lang),
            hint = pk("فصل واحد: وقت المراجعة يُقسَّم على الأيام المتاحة وتشوف حصتك اليومية", "One chapter: review time is divided across the available days to give your daily share", lang),
        )
    }

    fun dueLabel(dueKey: String, lang: String, arDigits: Boolean): String {
        val td = DateUtils.todayKey()
        return when (dueKey) {
            td -> pk("اليوم", "Today", lang)
            DateUtils.addDaysKey(1) -> pk("غدًا", "Tomorrow", lang)
            else -> DateUtils.formatDateShort(dueKey, lang, arDigits)
        }
    }

    fun formatFriendId(code: String): String =
        if (code.length >= 5) code.take(4) + "-" + code.drop(4) else code

    fun quickCheers(lang: String): List<String> = listOf(
        pk("كمّل! 🔥", "Keep going! 🔥", lang),
        pk("وين وصلت؟ ⏰", "How far are you? ⏰", lang),
        pk("أحسنت 👏", "Well done 👏", lang),
        pk("ذاكر! 💪", "Study! 💪", lang),
        pk("لا تنام 😴", "Don’t sleep 😴", lang),
    )

    fun streakDoneMessage(subjectName: String?, streakCount: Int, lang: String): String {
        val base = if (!subjectName.isNullOrBlank()) {
            pk("خلص وقت $subjectName 🎉", "Time is up for $subjectName 🎉", lang)
        } else {
            pk("انتهت جلسة التركيز، خذ استراحة 🎉", "Focus session finished, take a break 🎉", lang)
        }
        return if (streakCount > 0) "$base · 🔥 $streakCount" else base
    }

    fun remindBody(state: AppState): String {
        val lang = state.lang
        val ar = state.arDigits
        val r = state.remind
        val s = state.subjects.firstOrNull { it.id == r.subj }
        val base = pk("حان وقت الدراسة", "Time to study", lang) + (if (s != null) " · ${s.name}" else "")
        val info = DateUtils.planInfo(state)
        val share = if (s != null && info != null && DateUtils.partsTotalHours(s).first > 0.0) {
            DateUtils.subjectRemainingHours(s) / info.left.coerceAtLeast(1)
        } else if (s == null) {
            DateUtils.todayShareTotal(state)
        } else {
            0.0
        }
        return if (share > 0.0) {
            "$base · ${pk("حصة اليوم", "Today’s share", lang)}: ${fmtHours(share, lang, ar)}"
        } else {
            base
        }
    }

    fun subjectKeyIcon(name: String): String {
        val n = name.lowercase()
        return when {
            Regex("اسلام|إسلام|دين|قرآن|قران|islam|relig|quran").containsMatchIn(n) -> "dome"
            Regex("كيمي|chem").containsMatchIn(n) -> "flask"
            Regex("فيزي|phys").containsMatchIn(n) -> "atom"
            Regex("احي|أحي|bio").containsMatchIn(n) -> "dna"
            Regex("رياض|math").containsMatchIn(n) -> "calc"
            Regex("انكلي|إنكلي|انجلي|إنجلي|english").containsMatchIn(n) -> "globe"
            else -> "book"
        }
    }

    fun priorityLabel(pri: String, lang: String): String = when (pri) {
        "high" -> pk("عالية", "High", lang)
        "low" -> pk("منخفضة", "Low", lang)
        else -> pk("عادية", "Normal", lang)
    }

    fun missingDefaultSubjects(subjects: List<Subject>, lang: String): List<DefaultSubjectPreset> {
        val have = subjects.map { it.name }.toSet()
        return default6thSubjects.filter { !have.contains(if (lang == "ar") it.ar else it.en) }
    }
}
