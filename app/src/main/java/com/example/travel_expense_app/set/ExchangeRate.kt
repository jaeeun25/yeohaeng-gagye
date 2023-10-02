package com.example.travel_expense_app.set

import org.jsoup.Jsoup
import com.example.travel_expense_app.set.StartSet.Companion.rateMap
import com.example.travel_expense_app.set.StartSet.Companion.spinnerData

class Rate : Runnable{
    override fun run() {
        val url = "https://finance.naver.com//marketindex/exchangeList.naver"
        val doc = Jsoup.connect(url).get()
        val country = doc.select(".tit a")      // 국가명
        val sale = doc.select(".sale")          // 환율
        try{
            // 대한민국 정보 추가
            rateMap["KRW"] = "1"
            spinnerData.add("KRW 1")

            for (i in 0 until country.size) {
                try {
                    // html 태그들 제거
                    val cItem = Jsoup.parse(country[i].toString()).text()
                    val cItemArr: MutableList<String> = cItem.split(" ") as MutableList<String>
                    // 일본은 100엔을 기준으로 환율 정보를 가짐. 이런 예외 처리
                    val sItem = if(cItem.contains("100")) String.format("%.2f", Jsoup.parse(sale[i].toString()).text().toFloat() / 100)
                                else Jsoup.parse(sale[i].toString()).text()

                    if(cItem.contains("공화국")) {     // 공화국이 포함되어 있는 경우
                        cItemArr[0] = "${cItemArr[0]} ${cItemArr[1]}"    // 국가명
                        cItemArr[1] = cItemArr[2]        // 통화코드
                    }

                    // 환율 정보 담기
                    rateMap[cItemArr[1]] = sItem
                    spinnerData.add("${cItemArr[1]} $sItem")    // 통화코드 환율
                } catch (e: Exception) {
                    println("환율 정보를 가져오지 못했습니다.")
                }
            }
        }catch (e : Exception){
            println("환율 정보를 가져오지 못했습니다.")
        }
    }
}
