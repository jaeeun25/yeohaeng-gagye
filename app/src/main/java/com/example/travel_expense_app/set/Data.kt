package com.example.travel_expense_app.set

// 가계부
data class Trip(var id: String, var country: String,  var price: String, var start_date: String, var end_date: String, var rate: String, var unit: String, var image: Int, var mainTitle: String,
           var plan: Plan)
// 전체 지출 내역
data class TripItem(var id: String, var price: Int, var title: String, var date: String, var image: Boolean, var category: String, var time: String)
// 선택 탭 지출 내역
data class TabItem(var id: String, var price: Int, var title: String, var image: Boolean, var date: String, var time: String)
// 환율 내역
data class ExchangeRate(var unit: String, var rate: String)
// 예산 계획 데이터
data class Plan(var food: String, var house: String, var bus: String, var shopping: String, var tourism: String)
// 상세 내역
data class Detail(var d_id:String, var d_name: String, var d_price: Int, var d_qty: Int)

// 원화, 추가 함수
fun comma(krw: String): String{
    var newKrw = ""
    for(i in krw.indices) {
        when(krw.length % 3){
            0 -> if (i != 0 && i % 3 == 0) newKrw += ","                 //123,456,789
            1 -> if (i != 0 && (i == 1 || i % 3 == 1)) newKrw += ","     //1,234,567
            2 -> if (i != 0 && (i == 2 || i % 3 == 2)) newKrw += ","     //12,345,678
        }
        newKrw += krw[i]
    }
    return newKrw
}