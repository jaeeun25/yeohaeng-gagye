package com.example.travel_expense_app

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.travel_expense_app.databinding.ActivityChartBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class ChartActivity: AppCompatActivity() {
    private lateinit var binding: ActivityChartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 차트 색상 세팅
        val color = IntArray(6) { 0 }
        color[0] = Color.rgb(255, 205, 210)
        color[1] = Color.rgb(255, 224, 178)
        color[2] = Color.rgb(255, 249, 196)
        color[3] = Color.rgb(220, 237, 200)
        color[4] = Color.rgb(179, 229, 252)
        color[5] = Color.rgb(209, 196, 233)

        // main에서 데이터 받아오기
        val type = intent.getStringArrayExtra("cType")
        val price = intent.getStringArrayExtra("cPrice")
        val unit = intent.getStringExtra("cUnit")
        val data = ArrayList<PieEntry>()
        var totalExpend = 0.0f     // 총 지출 금액

        for(i in 0 until type!!.size){
            val expend = price?.get(i)?.toFloat() ?: 0.0f
            if(expend != 0.0f) {
                data.add(PieEntry(expend, type[i]))     // 차트 데이터 세팅
                totalExpend += expend
            }
        }

        val pieDataSet = PieDataSet(data,"")
        pieDataSet.colors = ColorTemplate.createColors(color)   // 차트 색상
        pieDataSet.sliceSpace = 8f          // 차트 간격
        pieDataSet.selectionShift = 25f     // 차트 크기

        val pieData = PieData(pieDataSet)
        pieData.setValueTextSize(20f)
        pieData.setValueTextColor(Color.BLACK)

        val chart = binding.chart
        chart.data = pieData
        chart.description.isEnabled = false
        chart.setEntryLabelColor(Color.BLACK)
        chart.setEntryLabelTextSize(13f)
        chart.setCenterTextSize(18f)
        if(data.isEmpty()) chart.centerText = "지출 내역이 없습니다."                             // 지출 내역 없는 경우
        else chart.centerText = "총 지출 \n\n ${String.format("%.2f", totalExpend)} $unit"   // 지출 내역 존재
        chart.animateY(1300, Easing.EaseInOutCubic)                               // 차트 애니메이션

        // 범례
        val l: Legend = chart.legend
        // 범례 위치
        l.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        l.orientation = Legend.LegendOrientation.HORIZONTAL
        // 범례 아이콘 모양
        l.form = Legend.LegendForm.CIRCLE
        // 범례 x축 간격
        l.xEntrySpace = 12f
        // 범례 y축 위치
        l.yOffset = 45f
        // 범례 글씨 및 아이콘 크기
        l.textSize = 15f
        l.formSize = 15f

        // x버튼 클릭 시
        binding.chartXBtn.setOnClickListener {
            finish()
        }
    }
}