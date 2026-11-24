package com.example.shiftwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object ShiftAlarmScheduler {

    fun scheduleNextAlarms(context: Context) {
        val prefs = context.getSharedPreferences("shift_prefs", Context.MODE_PRIVATE)
        val team = prefs.getString("my_team", "B") ?: "B"
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = LocalDateTime.now()
        // 향후 7일간 근무에 맞는 백그라운드 알람 예약
        var requestCode = 1000
        for (dayOffset in 0..6) {
            val date = LocalDate.now().plusDays(dayOffset.toLong())
            val shift = ShiftWidgetProvider.getShift(team, date)
            val nextShift = ShiftWidgetProvider.getShift(team, date.plusDays(1))

            val targets = mutableListOf<Triple<Int, Int, String>>()
            when (shift) {
                "주" -> {
                    targets.add(Triple(5, 20, "주간 출근 기상 (06:30 출근)"))
                    targets.add(Triple(6, 0, "주간 출근 출발"))
                }
                "야" -> {
                    targets.add(Triple(17, 0, "야간 출근 기상/식사 (18:30 출근)"))
                    targets.add(Triple(18, 0, "야간 출근 출발"))
                }
                "휴" -> {
                    if (nextShift == "주" || nextShift == "야") {
                        val nextLabel = if (nextShift == "주") "주간(06:30~18:30)" else "야간(18:30~06:30)"
                        targets.add(Triple(21, 0, "내일은 $nextLabel 출근일입니다!"))
                    }
                }
            }

            for ((hh, mm, msg) in targets) {
                val triggerDt = date.atTime(hh, mm)
                if (triggerDt.isAfter(now)) {
                    val triggerMs = triggerDt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
                        putExtra("alarm_title", "${team}조 교대근무 알림")
                        putExtra("alarm_body", msg)
                        putExtra("notif_id", requestCode)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                    }
                }
                requestCode++
            }
        }
    }
}
