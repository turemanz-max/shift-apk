package com.example.shiftwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ShiftWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        // 패턴: 주주휴휴야야휴휴 (8일 주기)
        private val PATTERN = listOf("주", "주", "휴", "휴", "야", "야", "휴", "휴")
        // 기준일: 2026-11-24 (D조=주간 시작(0), B조=야간 시작(4), A조=휴(6), C조=휴(2))
        private val BASE_DATE = LocalDate.of(2026, 11, 24)
        private val TEAM_OFFSETS = mapOf(
            "A" to 6,
            "B" to 4,
            "C" to 2,
            "D" to 0
        )

        fun getShift(team: String, date: LocalDate): String {
            val offset = TEAM_OFFSETS[team] ?: 4
            val days = ChronoUnit.DAYS.between(BASE_DATE, date).toInt()
            val idx = (((days + offset) % PATTERN.size) + PATTERN.size) % PATTERN.size
            return PATTERN[idx]
        }

        private fun getKoreanDow(date: LocalDate): String {
            return when (date.dayOfWeek.value) {
                1 -> "월"
                2 -> "화"
                3 -> "수"
                4 -> "목"
                5 -> "금"
                6 -> "토"
                else -> "일"
            }
        }

        private fun getCircleDrawable(shift: String): Int {
            return when (shift) {
                "주" -> R.drawable.widget_circle_day
                "야" -> R.drawable.widget_circle_night
                else -> R.drawable.widget_circle_off
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences("shift_prefs", Context.MODE_PRIVATE)
            val myTeam = prefs.getString("my_team", "B") ?: "B"

            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            val todayShift = getShift(myTeam, today)
            val tomorrowShift = getShift(myTeam, tomorrow)

            val views = RemoteViews(context.packageName, R.layout.widget_shift_2day)
            views.setTextViewText(R.id.tv_today_day, today.dayOfMonth.toString())
            views.setTextViewText(R.id.tv_today_dow, getKoreanDow(today))
            views.setTextViewText(R.id.tv_today_shift, todayShift)
            views.setInt(R.id.tv_today_shift, "setBackgroundResource", getCircleDrawable(todayShift))

            views.setTextViewText(R.id.tv_tmr_day, tomorrow.dayOfMonth.toString())
            views.setTextViewText(R.id.tv_tmr_dow, getKoreanDow(tomorrow))
            views.setTextViewText(R.id.tv_tmr_shift, tomorrowShift)
            views.setInt(R.id.tv_tmr_shift, "setBackgroundResource", getCircleDrawable(tomorrowShift))

            // 위젯 터치 시 앱 실행
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
