package com.example.travel_expense_app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travel_expense_app.set.StartSet.Companion.db
import com.example.travel_expense_app.set.StartSet.Companion.rateMap
import com.example.travel_expense_app.set.StartSet.Companion.storage
import com.example.travel_expense_app.databinding.ActivityMainBinding
import com.example.travel_expense_app.set.*
import com.example.travel_expense_app.set.Adapter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.storage.StorageReference
import java.io.File
import java.text.SimpleDateFormat
import kotlin.math.pow
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: Adapter
    lateinit var requestLauncher: ActivityResultLauncher<Intent>
    private var categoryPrice: MutableMap<String, Int>? = null      // 카테고리별 지출금액
    var clickPosition: Int = 0                                      // 지출 내역 위치
    private var itemList: MutableList<TripItem>? = null             // 지출 내역 전체 리스트
    var tabList: MutableList<TabItem>? = null                       // 선택 tab 지출 내역 리스트
    private var rateTotal: Float = 0.0f                             // 총금액 / 환율
    private var sum: Int = 0                                        // 총 지출 금액
    var rate: Float = 0.0f                                          // 환율
    var unit: String = ""                                           // 통화코드
    var cardId: String? = ""                                        // 가계부 id
    var clickTab:String = "A"                                       // 선택 tab
    private var start: String? = null                                       // 여행 시작일
    private var end: String? = null                                 // 여행 종료일
    private var selected: String = "전체"                            // 선택한 카테고리
    private var won: Int = 0                                        // 남은금액(원화)
    var line = mutableMapOf<String, Boolean>()                      // 리사이클러뷰 지출 날짜 표시 여부 확인
    private var mainTitle = ""                                      // 여행 제목
    private var _plan : Plan? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바
        setSupportActionBar(binding.mainTb)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)   // 뒤로가기 활성화
        supportActionBar?.setDisplayShowTitleEnabled(false) // 제목 비활성화

        // 바 데이터
        intent.getStringArrayExtra("barData").let {
            mainTitle = it?.get(1).toString()       // 여행 제목
            supportActionBar?.title = mainTitle
            supportActionBar?.subtitle = it?.get(0) // 여행지
            supportActionBar?.setDisplayShowTitleEnabled(true)
        }

        // home에서 받은 데이터
        intent.getStringArrayExtra("homeData").let{
            // 가계부 id, 경비, 시작일, 종료일, 환율, 통화코드
            cardId = it?.get(0)
            won = intent.getIntExtra("total", 0)
            start = it?.get(1)
            end = it?.get(2)
            rate = it?.get(3).toString().replace(",","").toFloat()
            unit = it?.get(4).toString()
            rateTotal = won.toFloat() / rate        // 총금액 / 환율
            intent.getStringArrayExtra("plan").let {
                _plan = Plan(it?.get(0).toString(), it?.get(1).toString(), it?.get(2).toString(), it?.get(3).toString(), it?.get(4).toString())
            }
        }

        // tab 생성 및 db 저장된 지출 내역가져오기
        itemList = mutableListOf()
        tabList = mutableListOf()
        makeTab(start.toString(), end.toString())
        makeList("standard", false)

        // 카테고리 선택 AlertDialog 띄우기
        binding.mainCategory.setOnClickListener {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.popup_hm, null)
            val categoryType = arrayOf("카테고리 전체", "음식", "숙박", "교통", "쇼핑", "관광", "기타")
            var sNum = 0
            val alertDialog = AlertDialog.Builder(this, R.style.AppTheme_AlertDialogTheme)
                .setTitle("카테고리를 선택해주세요")
                .setSingleChoiceItems(categoryType,0){ _, i ->
                    sNum = i
                }
                .setPositiveButton("확인"){ _, _ ->
                    when (sNum) {
                        0 -> selected = "전체"
                        1 -> selected = "음식"
                        2 -> selected = "숙박"
                        3 -> selected = "교통"
                        4 -> selected = "쇼핑"
                        5 -> selected = "관광"
                        6 -> selected = "기타"
                    }
                    binding.mainSelect.text = categoryType[sNum]
                    selectTab("standard", false)
                }
                .create()

            alertDialog.setView(view)
            alertDialog.show()
        }

    requestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { it ->
            // 지출 내역 추가
            it.data!!.getStringArrayExtra("result")?.let {
                // 지출내역 id, 지출 금액, 제목, 날짜, 이미지 추가 여부, 카테고리, 시간
                val data = TripItem(it[0], it[1].toInt(), it[2], it[3], it[4].toBoolean(), it[6], it[7])
                itemList?.add(data)
                itemList?.sortWith(compareByDescending(TripItem::date).thenByDescending(TripItem::time))    // 날짜 내림차순, 시간 내림차순으로 정렬

                chartData(it[6], it[1].toInt(), "")        // 추가한 데이터 차트 반영
                selectTab("modify", true)              // 새로 추가했으므로 선택 tab에 따른 리스트 내역 변경
                sum = sum.plus(it[1].toInt())                       // 총 지출 금액 변경
                makeTotal()                                             // 총 금액 변경

                if(it[4].toBoolean()) uploadImage(it[0], it[5])         // 이미지 추가한 경우
                else adapter.notifyDataSetChanged()
            }

            // 지출 내역 삭제
            it.data!!.getStringExtra("delete_main")?.let {
                val idPosition = tabList?.get(clickPosition)?.id    // 삭제할 내역 id
                tabList?.removeAt(clickPosition)                    // tab 리스트에서 삭제
                for((index, item) in itemList!!.withIndex()){
                    if(item.id == idPosition){                      // 전체 리스트에서 id값으로 해당 내역 찾아 삭제
                        sum -= itemList?.get(index)?.price!!        // 총 지출 금액 변경
                        chartData(itemList?.get(index)?.category!!, itemList?.get(index)?.price!!, it)  // 삭제한 데이터 차트 반영
                        itemList?.removeAt(index)
                        break
                    }
                }
                lineFalse()
                makeTotal()
                adapter.notifyDataSetChanged()
            }

            // 가계부 정보 수정
            it.data!!.getStringArrayExtra("home_modify_result")?.let {
                // 경비, 시작일, 종료일, 여행제목 가져오기
                won = it[0].toInt()
                start = "${it[1]} 00:00:00"
                end = "${it[2]} 00:00:00"
                mainTitle = it[3]
                supportActionBar?.title = mainTitle
                rateTotal = won.toFloat() / rate    // 변경된 경비에 따른 총금액/환율 변경
                binding.mainTab.removeAllTabs()
                makeTotal()
                makeTab(start!!, end!!)             // 시작일, 종료일 변경에 따른 tab 변경
                adapter.notifyDataSetChanged()
            }

            // 지출 내역 하나 수정
            it.data!!.getStringArrayExtra("modify")?.let {
                if(it[2].toBoolean()) {     // 이미지가 변경된 경우
                    uploadImage(it[0], it[1])   // 지출내역 id, 파일경로
                }
                makeList("modify", it[2].toBoolean())
            }
        }

        binding.mainTab.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener{
            // tab 선택할 때 이벤트
            override fun onTabSelected(tab: TabLayout.Tab?) {
                clickTab = tab?.text.toString()     // 선택한 tab 저장
                if(clickTab != "A" && clickTab.length == 1) clickTab = "0${clickTab}"       // 선택 tab이 A이거나 한자리 날짜 일 때
                selectTab("standard", false)        // 선택한 tab 따른 tab 리스트 변경
            }
            override fun onTabUnselected(tab: TabLayout.Tab) { }
            override fun onTabReselected(tab: TabLayout.Tab) { }
        })

        // 지출 내역 추가 아이콘 클릭 시
        binding.mainFab.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            // 가계부 id, 시작일, 종료일, 통화 코드
            intent.putExtra("card", cardId)
            intent.putExtra("startDay", start)
            intent.putExtra("endDay", end)
            intent.putExtra("unit", unit)
            requestLauncher.launch(intent)
        }

        // 리사이클러 뷰 출력
        val layoutManager = LinearLayoutManager(this)
        binding.mainRecyclerView.layoutManager=layoutManager
        adapter= Adapter(this, tabList, cardId, rate, unit, line)
        binding.mainRecyclerView.adapter=adapter
        binding.mainRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))


        // 각 지출 내역 클릭 시 화면 전환
        adapter.itemClick = object: Adapter.ItemClick{
            override fun onClick(view: View, position: Int) {
                val intent = Intent(this@MainActivity, ItemActivity::class.java)
                clickPosition = position    // 지출 내역 위치

                val itemData = arrayOf(tabList?.get(position)?.id, cardId, start, end, rate.toString(), unit)   // 지출내역 id, 가계부 id, 시작일, 종료일, 환율, 통화코드
                intent.putExtra("itemData", itemData)
                requestLauncher.launch(intent)
            }
        }
    }

    // 뒤로가기
    override fun onBackPressed() {
        back()
    }

    // 메뉴 등록
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
    // 메뉴 선택
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when(item.itemId) {
        android.R.id.home -> {  // 화살표(뒤로가기)
            back()
            false
        }
        R.id.ti_modify -> {  // 가계부 정보 수정
            db.collection("${StartSet.email}").document("$cardId").get()
                .addOnSuccessListener {
                    val data = arrayOf(
                        it.id,
                        it.data?.get("country").toString(),
                        it.data?.get("start_date").toString(),
                        it.data?.get("end_date").toString(),
                        it.data?.get("title").toString(),
                        it.data?.get("price").toString(),
                        it.data?.get("rate").toString(),
                        it.data?.get("unit").toString(),
                        it.data?.get("time").toString()
                    )
                    val intent = Intent(this, HomeAddActivity::class.java)

                    intent.putExtra("home_modify", data)
                    requestLauncher.launch(intent)
                }
                .addOnFailureListener{
                    Toast.makeText(this, "가계부 정보를 수정할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            true
        }
        R.id.ti_delete -> {  // 가계부 삭제
            val del = db.collection("${StartSet.email}").document("$cardId")
            if(itemList?.isNotEmpty() == true) {    // 지출 내역 있는 경우
                for (i in itemList!!) {
                    del.collection("trip_item").document(i.id).collection("detail").get().addOnSuccessListener {
                        for(d in it) del.collection("trip_item").document(i.id).collection("detail").document(d.id).delete()
                    }
                    del.collection("trip_item").document(i.id).delete()
                    if(i.image) {    // 이미지가 있는 경우
                        val imgRef = storage.reference.child("${cardId}/${i.id}.jpg")
                        imgRef.delete()
                    }
                }
            }
            del.delete()   // 가계부 삭제

            intent.putExtra("delete_result", "delete")
            setResult(Activity.RESULT_OK, intent)
            finish()
            true
        }
        R.id.exchange -> {    // 환율 변경
            val reRate = rateMap[unit]?.replace(",", "")
            db.collection("${StartSet.email}").document("$cardId").update("rate", reRate)     // db에 저장된 환율 변경

            // 변경된 환율에 따른 데이터 변경
            rate = reRate?.toFloat()!!
            rateTotal = won.toFloat() / rate
            adapter.rate = rate
            makeTotal()
            lineFalse()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "환율이 변경되었습니다.", Toast.LENGTH_SHORT).show()
            true
        }
        R.id.chart -> {  // 차트 보기
            val intent = Intent(this@MainActivity, ChartActivity::class.java)
            val type = arrayOf("음식", "숙박", "교통", "쇼핑", "관광", "기타")
            val price = arrayOf(categoryPrice?.get("음식").toString(),
                                categoryPrice?.get("숙박").toString(),
                                categoryPrice?.get("교통").toString(),
                                categoryPrice?.get("쇼핑").toString(),
                                categoryPrice?.get("관광").toString(),
                                categoryPrice?.get("기타").toString())

            intent.putExtra("cUnit", unit)  // 화폐단위
            intent.putExtra("cType", type)  // 카테고리
            intent.putExtra("cPrice", price)    // 카테고리별 지출금액
            startActivity(intent)
            true
        }
        R.id.plan -> {
            val intent = Intent(this@MainActivity, PlanActivity::class.java)
            val price = arrayOf(categoryPrice?.get("음식").toString(),
                categoryPrice?.get("숙박").toString(),
                categoryPrice?.get("교통").toString(),
                categoryPrice?.get("쇼핑").toString(),
                categoryPrice?.get("관광").toString(),
                categoryPrice?.get("기타").toString())
            val planData = arrayOf(_plan?.food, _plan?.house, _plan?.bus, _plan?.shopping, _plan?.tourism)

            intent.putExtra("pWon", won)
            intent.putExtra("pRate", rate)  // 환율
            intent.putExtra("pUnit", unit)  // 통화코드
            intent.putExtra("pPrice", price)    // 카테고리별 지출금액
            intent.putExtra("pPlan", planData)
            startActivity(intent)
            true
        }
        else -> true
    }

    // 총 지출금액 및 지출 퍼센트 변경 함수
    private fun makeTotal(){
        // 지출 퍼센트
        if(rateTotal-sum > 0) binding.mainPercent.text = "${roundDigit((((rateTotal-sum).toDouble()/rateTotal)*100).toFloat(), 0).toInt()}%"
        else binding.mainPercent.text = "0%"     // 금액 초과
        binding.mainRate.text = "${roundDigit((rateTotal-sum), 2)} $unit"    // 총 지출 금액

        // 총 지출금액 원화 표시
        val krw = ((rateTotal-sum)*rate).toInt().toString()
        binding.mainKrw.text = "${comma(krw)} KRW"
    }

    // 여행 시작일~종료일 만큼 tab 생성하는 함수
    private fun makeTab(s_date: String, e_date: String){
        line.clear()

        val sf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        val period = (sf.parse(e_date).time - sf.parse(s_date).time) / (60*60*24*1000) + 1
        val year = s_date.substring(0, 4).toInt()
        var month = s_date.substring(5, 7).toInt()
        var day = s_date.substring(8, 10).toInt()

        for(i in 0..period){
            val tab: TabLayout.Tab = binding.mainTab.newTab()

            if(i.toInt() != 0) {            // 리사이클러 뷰에 날짜 표시하기 위해 여행 일자만큼 값 세팅
                val key = if (day.toString().length == 1) "0$day"
                          else day.toString()
                line[key] = false
            }

            if(i.toInt() == 0){     // 전체 내역 보여줄 tab 생성
                tab.text = "A"
            }
            else if(day == 31){     // 31일까지 있는 달
                if((month == 1) || (month == 3) || (month == 5) || (month == 7) || (month == 8) || (month == 10) || (month == 12)){
                    tab.text = (day).toString()
                    day = 1
                }
                else{               // 30일까지 있는 달
                    day = 1
                    tab.text = (day++).toString()
                }
                if(month == 12) month = 1
                else month += 1
            }
            else if((month == 2 && day == 28) || (month == 2 && day == 29)){   // 2월
                if(year % 100 != 0 && year % 4 == 0 || year % 400 == 0){    // 윤년인 경우
                    tab.text = (day).toString()
                    if(day == 29) {
                        day = 1
                        month += 1
                    }
                    else day += 1
                }
                else{
                    tab.text = (day).toString()
                    day = 1
                    month += 1
                }
            }
            else{
                tab.text= (day++).toString()
            }
            binding.mainTab.addTab(tab)
        }
    }

    // 지출내역 가져오는 함수
    private fun makeList(status: String, image:Boolean){
        // 초기화
        sum = 0
        itemList?.clear()
        categoryPrice?.clear()
        categoryPrice = mutableMapOf("음식" to 0, "숙박" to 0, "교통" to 0, "쇼핑" to 0, "관광" to 0, "기타" to 0)

        // 로그인한 사용자의 특정 가계부 id의 지출 내역 가져오기
        db.collection("${StartSet.email}").document("$cardId").collection("trip_item").get()
            .addOnSuccessListener { result ->
                for (item in result) {
                    // 전체 내역 리스트 세팅
                    val data = TripItem(
                        item.id,
                        item.data["price"].toString().toInt(),
                        item.data["title"].toString(),
                        item.data["date"].toString(),
                        item.data["image"].toString().toBoolean(),
                        item.data["category"].toString(),
                        item.data["time"].toString()
                    )
                    itemList?.add(data)
                    chartData(item.data["category"].toString(), item.data["price"].toString().toInt(), "")
                    sum = sum.plus(item.data["price"].toString().toInt())    // 총 지출 금액 추가
                }
                // 리스트 정렬
                itemList?.sortWith(compareByDescending(TripItem::date).thenByDescending(TripItem::time))

                makeTotal()
                selectTab(status, image)
            }
            .addOnFailureListener {
                Toast.makeText(this, "지출 내역을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 선택 tab에 따른 tab 리스트 변경 함수
    private fun selectTab(status: String, ChangeImage: Boolean){
        tabList?.clear()
        lineFalse()

        for(i in 0..(itemList?.size?.minus(1) ?: 0)){
            val item = itemList?.get(i)
            if(clickTab == item?.date?.substring(8, 10) || clickTab == "A"){   // 선택한 tab과 지출 내역의 날짜가 동일한 경우 or tab이 A(전체)인 경우
                if(selected == item?.category || selected == "전체")       // 선택한 카테고리와 지출 내역 카테고리가 동일한 경우 or 카테고리 전체인 경우
                    tabList?.add(TabItem(item!!.id, item.price, item.title, item.image, item.date, item.time))
            }
        }
        tabList?.sortWith(compareByDescending(TabItem::date).thenByDescending(TabItem::time))
        if(status != "modify" || !ChangeImage) adapter.notifyDataSetChanged()        // 내역을 수정하지 않았거나 이미지를 변경하지 않은 경우
    }

    // firebase storage에 이미지 업로드 함수
    private fun uploadImage(id: String, filePath: String){
        val storage = storage
        val storageRef: StorageReference = storage.reference    // 스토리지 참조하는 레퍼런스 생성
        val imgRef: StorageReference = storageRef.child("${cardId}/${id}.jpg")  // 실제 업로드하는 파일 참조하는 레퍼런스 생성

        // 파일 업로드
        val file = Uri.fromFile(File(filePath))
        imgRef.putFile(file).addOnSuccessListener {     // 이미지 업로드 성공
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {                     // 이미지 업로드 실패
                Toast.makeText(this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 차트 데이터 세팅 함수
    private fun chartData(type: String, price: Int, status: String){
        val tPrice = categoryPrice!![type]
        if(status == "remove") categoryPrice!![type] = tPrice!!.minus(price)    // 지출 내역 삭제한 경우
        else categoryPrice!![type] = tPrice!!.plus(price)
    }

    // 리사이클러뷰에 나타낼 일자 표시 여부 초기화 함수
    private fun lineFalse(){
        for(i in line) i.setValue(false)
        adapter.line = line
    }

    // x버튼, 뒤로가기 시 동작 함수
    private fun back(){
        val data = arrayOf(won.toString(), start, end, rate.toString(), mainTitle)
        intent.putExtra("xBtn", data)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    // 반올림 함수
    private fun roundDigit(number: Float, digits: Int): Double {
        return (number * 10.0.pow(digits.toDouble())).roundToInt() / 10.0.pow(digits.toDouble())
    }
}