package com.example.travel_expense_app

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import com.example.travel_expense_app.databinding.ActivityPlanBinding
import com.example.travel_expense_app.set.comma
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class PlanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlanBinding

    // 축 라벨 값 바꿔주기 위한 클래스
    inner class MyXAxisFormatter : ValueFormatter(){
        private val days = arrayOf("음식","숙박","교통","쇼핑","관광", "기타")
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return days.getOrNull(value.toInt()-1) ?: value.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanBinding.inflate(layoutInflater)
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
        val plan = intent.getStringArrayExtra("pPlan")
        val price = intent.getStringArrayExtra("pPrice")
        val rate = intent.getFloatExtra("pRate", 1.0f)
        val unit = intent.getStringExtra("pUnit")
        val won = intent.getIntExtra("pWon", 0)

        val planData = ArrayList<BarEntry>()
        val priceData = ArrayList<BarEntry>()

        // 표 안에 있는 TextView 바인딩 객체 array
        val planText = arrayOf(binding.planFood, binding.planHouse, binding.planBus, binding.planShopping, binding.planTourism, binding.planEtc)
        val priceText = arrayOf(binding.priceFood, binding.priceHouse, binding.priceBus, binding.priceShopping, binding.priceTourism, binding.priceEtc)
        val remainingText = arrayOf(binding.remainingFood, binding.remainingHouse, binding.remainingBus, binding.remainingShopping, binding.remainingTourism, binding.remainingEtc)
        val percentText = arrayOf(binding.percentFood, binding.percentHouse, binding.percentBus, binding.percentShopping, binding.percentTourism, binding.percentEtc)
        val planKrwText = arrayOf(binding.planFoodKrw, binding.planHouseKrw, binding.planBusKrw, binding.planShoppingKrw, binding.planTourismKrw, binding.planEtcKrw)
        val priceKrwText = arrayOf(binding.priceFoodKrw, binding.priceHouseKrw, binding.priceBusKrw, binding.priceShoppingKrw, binding.priceTourismKrw, binding.priceEtcKrw)
        val remainingKrwText = arrayOf(binding.remainingFoodKrw, binding.remainingHouseKrw, binding.remainingBusKrw, binding.remainingShoppingKrw, binding.remainingTourismKrw, binding.remainingEtcKrw)

        // 기타(카테고리) 금액 구하기 위한 변수
        var total = 0.0f

        for(i in 0..plan!!.size){
            val expend = price?.get(i)?.toFloat() ?: 0.0f
            val krw = expend.times(rate)
            if(i != plan.size){     // 카테고리(기타) 아닌 경우
                val krw_plan = plan?.get(i)?.toFloat() ?: 0.0f      // 원화
                val _plan = krw_plan/rate                           // 외화
                total += krw_plan

                // 차트 데이터 세팅(계획한 금액, 지출금액)
                planData.add(BarEntry(i+1.1f, krw_plan))
                priceData.add(BarEntry(i+0.9f, krw))

                // 표 안에 있는 TextView 채우기
                // 외화
                planText[i].text ="${roundDigit(_plan, 2)} $unit"                   // 계획한 금액
                priceText[i].text = "${expend.toInt()} $unit"                             // 지출 금액
                remainingText[i].text = "${roundDigit((_plan-expend), 2)} $unit"    // 남은 금액
                // 원화
                planKrwText[i].text = "${comma(krw_plan.toInt().toString())} KRW"         // 계획한 금액
                priceKrwText[i].text = "${comma(krw.toInt().toString())} KRW"             // 지출 금액
                remainingKrwText[i].text = "${comma((krw_plan-krw).toInt().toString())} KRW"    // 남은 금액
                percentText[i].text = roundDigit(((krw_plan-krw)/krw_plan)*100, 0).toInt().toString() + "%"     // 남은 금액(%)
            }
            else {  // 카테고리(기타)
                val etc = won-total     // 전체 금액 - 계획한 금액 = 기타(카테고리)로 책정된 금액
                if (etc > 0) {      // 기타에 책정된 금액 있는 경우
                    planData.add(BarEntry(i+1.1f, etc))     // 차트 데이터 세팅

                    // 표 안에 있는 TextView 채우기
                    planText[i].text = "${roundDigit((etc/rate), 2)} $unit"
                    remainingText[i].text = "${roundDigit(((etc/rate)-expend), 2)} $unit"

                    priceKrwText[i].text = "${comma(krw.toInt().toString())} KRW"
                    remainingKrwText[i].text = "${comma((etc-krw).toInt().toString())} KRW"
                    percentText[i].text = roundDigit(((etc-krw)/etc)*100, 0).toInt().toString() + "%"
                }
                else {      // 기타에 책정된 금액 없는 경우
                    planData.add(BarEntry(i+1.1f, 0.0f))     // 차트 데이터 세팅

                    // 표 안에 있는 TextView 채우기
                    planText[i].text = "-"
                    remainingText[i].text = "-"
                    percentText[i].text = "-"
                    priceKrwText[i].text = "-"
                    remainingKrwText[i].text = "-"
                }
                priceData.add(BarEntry(i+0.9f, krw))        // 차트 데이터 세팅(krw)

                // 표 안에 있는 TextView 채우기
                priceText[i].text = "${expend.toInt()} $unit"
                planKrwText[i].text = "${comma(krw.toInt().toString())} KRW"
            }
        }

        binding.plan.run {
            description.isEnabled = false       // 차트 옆에 별도로 표기되는 description
            setMaxVisibleValueCount(5)          // 최대 보이는 그래프 개수
            setPinchZoom(false)                 // 줌 설정
            setDrawBarShadow(false)             // 그래프 그림자
            setDrawGridBackground(false)        // 격자구조
            setDrawValueAboveBar(true)
            extraBottomOffset = 20f
            axisLeft.run {                      // 왼쪽 축(Y방향)
                axisMinimum = 0f                // 최소값 0
                setDrawLabels(true)             // 값 적는거 허용
                setDrawGridLines(true)          // 격자 라인 활용
                setDrawAxisLine(false)          // 축 그리기 설정
                textSize = 14f                  // 라벨 텍스트 크기
            }
            xAxis.run {
                position = XAxis.XAxisPosition.BOTTOM       // X축 위치 아래로
                granularity = 1f                            // 간격
                setDrawAxisLine(true)                       // 축 그림
                setDrawGridLines(false)                     // 격자
                valueFormatter = MyXAxisFormatter()         // 축 라벨 값 바꿈
                textSize = 14f                              // 텍스트 크기

            }
            axisRight.isEnabled = false         // 오른쪽 Y축 안보이게
            setTouchEnabled(false)              // 그래프 터치 시 변화
            animateY(1000)           // 애니매이션 적용

            legend.isEnabled = true             // 차트 범례 설정
            // 범례 위치 및 모양
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.form = Legend.LegendForm.CIRCLE
            legend.yOffset = 10f
        }

        // 데이터셋 초기화
        var planSet = BarDataSet(planData,"계획한 금액 (KRW)")
        var priceSet = BarDataSet(priceData,"지출 금액 (KRW)")

        // 차트 색상
        planSet.color = ContextCompat.getColor(this, R.color.bar_chart)
        priceSet.setColors(color, 255)

        val dataSet :ArrayList<IBarDataSet> = ArrayList()
        dataSet.add(planSet)
        dataSet.add(priceSet)

        val data = BarData(dataSet)
        data.barWidth = 0.3f        //막대 너비 설정
        binding.plan.run {
            this.data = data        //차트의 데이터를 data로 설정
            setFitBars(true)
            invalidate()
        }

        // x버튼 클릭 시
        binding.planXBtn.setOnClickListener {
            finish()
        }
    }

    // 반올림 함수
    private fun roundDigit(number: Float, digits: Int): Double {
        return (number * 10.0.pow(digits.toDouble())).roundToInt() / 10.0.pow(digits.toDouble())
    }
}
