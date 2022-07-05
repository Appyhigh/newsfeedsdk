package com.appyhigh.newsfeedsdk.utils

import android.util.Log
import com.appyhigh.newsfeedsdk.FeedSdk
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.formatter.ValueFormatter
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class DayAxisValueFormatter(private val type: Int, private var formattedDateCache: HashMap<Float, String>) : ValueFormatter() {

    private val mMonths = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    private var i = 0
    override fun getFormattedValue(value: Float): String {
//        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
//        cal.timeZone = TimeZone.getTimeZone("Asia/Calcutta")
        if(formattedDateCache.containsKey(value)){
            return formattedDateCache[value]!!
        }
        val cal = Calendar.getInstance()
        cal.timeInMillis = value.toLong()
        i+=1
        val result = when {
            type<2 -> cal.get(Calendar.HOUR).toString()+":"+if(cal.get(Calendar.MINUTE) <10) "0"+cal.get(Calendar.MINUTE).toString() else ""+cal.get(Calendar.MINUTE).toString()
            type==6 -> mMonths[cal.get(Calendar.MONTH)]+" ''"+cal.get(Calendar.YEAR).toString().substring(2)
            else -> mMonths[cal.get(Calendar.MONTH)]+" "+cal.get(Calendar.DAY_OF_MONTH).toString()
        }
        formattedDateCache[value] = result
        return result
    }
}