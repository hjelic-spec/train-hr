package hr.trainhr.core.util

import java.util.Locale

/** Kilogrami u hrvatskom zapisu: 22,5 / 20. */
fun formatKg(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString()
    else String.format(Locale.forLanguageTag("hr"), "%.2f", v).trimEnd('0').trimEnd(',')
