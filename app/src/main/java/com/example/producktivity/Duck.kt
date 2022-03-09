package com.example.producktivity

import kotlin.random.Random

data class Duck(var name: String) {
    var time_today: Int = 0
    var this_week: Int = 0
    var all_time: Int = 0
    var timeEachDayOfWeek = Array(7) {i -> 0}
    var isActive: Boolean = true

    open fun getTimeString(time: Int): String {
        val hours: Int = time / 60
        val minutes: Int = time % 60
        if (hours > 0) {
            return "${hours}h${minutes}m"
        } else {
            return "${minutes}m"
        }
    }
}

class DuckCompareTimeToday() {
    companion object : Comparator<Duck> {

        override fun compare(a: Duck, b: Duck): Int = when {
            a.time_today != b.time_today -> b.time_today - a.time_today
            else -> b.this_week - a.this_week
        }
    }
}

class DuckCompareThisWeek() {
    companion object : Comparator<Duck> {

        override fun compare(a: Duck, b: Duck): Int = when {
            a.this_week != b.this_week -> b.this_week - a.this_week
            else -> b.time_today - a.time_today
        }
    }
}