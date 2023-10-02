package com.example.travel_expense_app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.travel_expense_app.set.StartSet.Companion.db
import com.example.travel_expense_app.set.StartSet.Companion.spinnerData
import com.example.travel_expense_app.databinding.ActivityHomeAddBinding
import com.example.travel_expense_app.set.ExchangeRate
import com.example.travel_expense_app.set.StartSet
import com.example.travel_expense_app.set.StartSet.Companion.countryList
import com.example.travel_expense_app.set.comma
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class HomeAddActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeAddBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeAddBinding.inflate(layoutInflater)    // 바인딩 객체 획득
        setContentView(binding.root)

        // 가계부 이미지 랜덤 지정
        val homeImg = (0..5).random()

        // 시작일 현재 날짜로 초기화
        var startDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        // 여행지, 환율 선택 초기화
        var cSelected = "대한민국"
        var rSelected = ExchangeRate("KRW", "1")
        binding.homeCountry.text = "대한민국"
        binding.homeRate.text = "KRW 1"

        // 가계부 수정시
        val data = intent.getStringArrayExtra("home_modify")
        if (data != null) {
            // 여행지, 시작일, 종료일, 여행제목, 경비, 환율 기존 정보 가져오기
            binding.homeCountry.text = data[1]
            startDate = data[2]
            binding.homeEndDate.text = data[3]
            binding.homeMainTitle.setText(data[4])
            binding.homePrice.setText(data[5])
            binding.homeRate.text = "${data[7]} ${data[6]}"
            binding.homeSave.isEnabled = true   // 등록 버튼 활성화
            binding.homeSave.setTextColor(Color.rgb(0, 0, 0))

            // 여행지, 환율, 계획 수정 제한
            binding.homeSave.text = "수정"
            binding.homeCountry.setTextColor(Color.rgb(187, 187, 187))
            binding.homeCountryBtn.setImageResource(R.drawable.icon_btn_false)
            binding.homeCountryContainer.isEnabled = false
            binding.homeRate.setTextColor(Color.rgb(187, 187, 187))
            binding.homeRateBtn.setImageResource(R.drawable.icon_btn_false)
            binding.homeRateContainer.isEnabled = false
            binding.homePlan.isEnabled = false
            binding.homePlan.setTextColor(Color.rgb(187, 187, 187))
        }

        // 국가 정보 AlertDialog로 띄우기
        binding.homeCountryContainer.setOnClickListener {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.popup_hm, null)
            val cList : Array<String> = countryList.toTypedArray()   // 국가 정보
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("여행지를 선택해주세요")
                .setItems(cList){ _, i ->
                    cSelected = cList[i]
                    binding.homeCountry.text = cList[i]
                }
                .create()

            alertDialog.setView(view)
            alertDialog.show()
        }

        // 환율 정보 AlertDialog로 띄우기
        binding.homeRateContainer.setOnClickListener {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.popup_hm, null)
            val dataList : Array<String> = spinnerData.toTypedArray()   // 환율 정보
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("통화를 선택해주세요")
                .setItems(dataList){ _, i ->
                    val eData = dataList[i].split(" ")
                    rSelected = ExchangeRate(eData[0], eData[1])     // 선택한 화폐단위
                    binding.homeRate.text = dataList[i]
                }
                .create()

            alertDialog.setView(view)
            alertDialog.show()
        }

        // 여행 시작일 현재날짜(추가) or 기존정보(수정)로 세팅
        binding.homeStartDate.text = startDate
        // 선택한 날짜 여행 시작일로 초기화(종료일 선택 제한 두기 위함)
        var selectDay = Calendar.getInstance().apply { set(startDate.substring(0, 4).toInt(), startDate.substring(5, 7).toInt() - 1, startDate.substring(8, 10).toInt()) }

        // 여행 시작일 선택 시
        binding.homeStartDate.setOnClickListener {
            val cal = Calendar.getInstance()    //캘린더뷰 만들기
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    startDate = if (month in 0..8 && dayOfMonth in 1..9) "${year}.0${month + 1}.0${dayOfMonth}"  // 2022.01.01
                                else if (month in 0..8) "${year}.0${month + 1}.${dayOfMonth}"   // 2022.02.15
                                else if (dayOfMonth in 1..9) "${year}.${month + 1}.0${dayOfMonth}"   // 2022.10.05
                                else "${year}.${month + 1}.${dayOfMonth}"    // 2022.12.12
                    binding.homeStartDate.text = startDate
                    selectDay = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                }
            // DatePickerDialog 띄우기
            DatePickerDialog(this, R.style.AppTheme_AlertDialogTheme, dateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        // 여행 종료일 선택 시
        binding.homeEndDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                val dateString = if (month in 0..8 && dayOfMonth in 1..9) "${year}.0${month + 1}.0${dayOfMonth}"
                                 else if (month in 0..8) "${year}.0${month + 1}.${dayOfMonth}"
                                 else if (dayOfMonth in 1..9) "${year}.${month + 1}.0${dayOfMonth}"
                                 else "${year}.${month + 1}.${dayOfMonth}"
                binding.homeEndDate.text = dateString
            }
            DatePickerDialog(this, R.style.AppTheme_AlertDialogTheme, dateSetListener,
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.minDate = selectDay.timeInMillis }.show()   // 여행 시작일 부터 선택 가능
        }

        // x버튼 클릭 시
        binding.homeXBtn.setOnClickListener {
            back()
        }

        // 예산 계획 버튼 클릭시
        var planData = mutableListOf<String>()
        binding.homePlan.setOnClickListener {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.popup_plan_add, null)
            val totalView = view.findViewById<TextView>(R.id.popup_total)
            val foodView = view.findViewById<TextView>(R.id.popup_food)
            val houseView = view.findViewById<TextView>(R.id.popup_house)
            val busView = view.findViewById<TextView>(R.id.popup_bus)
            val shoppingView = view.findViewById<TextView>(R.id.popup_shopping)
            val tourismView = view.findViewById<TextView>(R.id.popup_tourism)

            totalView.text = "여행 예산: ${comma(binding.homePrice.text.toString())} KRW"
            foodView.text = "0"
            houseView.text = "0"
            busView.text = "0"
            shoppingView.text = "0"
            tourismView.text = "0"

            val alertDialog = AlertDialog.Builder(this)
                .setTitle("카테고리별 지출 계획을 작성해주세요.")
                .setPositiveButton("확인"){ _, _ ->      // 확인 클릭시
                    planData.add(foodView.text.toString())
                    planData.add(houseView.text.toString())
                    planData.add(busView.text.toString())
                    planData.add(shoppingView.text.toString())
                    planData.add(tourismView.text.toString())

                    binding.homeSave.isEnabled = true   // 등록 버튼 활성화
                    binding.homeSave.setTextColor(Color.rgb(0, 0, 0))
                }
                .setNeutralButton("취소", null)       // 취소 클릭시
                .create()
            alertDialog.setView(view)
            alertDialog.show()
        }

        // 등록 버튼 클릭시
        binding.homeSave.setOnClickListener {
            // 여행 제목 미입력
            if (binding.homeMainTitle.text.isEmpty()) Toast.makeText(this, "여행 제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            // 경비 미입력
            else if (binding.homePrice.text.isEmpty()) Toast.makeText(this, "경비를 입력해주세요.", Toast.LENGTH_SHORT).show()
            // 여행 종료일 미입력
            else if (binding.homeEndDate.text.equals("-")) Toast.makeText(this, "여행 종료일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            else if(data != null){    // 수정
                db.collection("${StartSet.email}").document(data[0]).update(mapOf(
                    "title" to binding.homeMainTitle.text.toString(),
                    "start_date" to binding.homeStartDate.text.toString(),
                    "end_date" to binding.homeEndDate.text.toString(),
                    "price" to binding.homePrice.text.toString().toInt()
                ))
                    .addOnFailureListener {
                        Toast.makeText(this,"가계부 정보를 수정하지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                // 탭, 경비, 여행제목 home으로 넘김
                val modify = arrayOf(binding.homePrice.text.toString(), binding.homeStartDate.text.toString(), binding.homeEndDate.text.toString(), binding.homeMainTitle.text.toString())
                intent.putExtra("home_modify_result", modify)

                setResult(RESULT_OK, intent)
                finish()
            }
            else {      // 추가
                val addData = mapOf(
                    "country" to cSelected,
                    "title" to binding.homeMainTitle.text.toString(),
                    "start_date" to binding.homeStartDate.text.toString(),
                    "end_date" to binding.homeEndDate.text.toString(),
                    "price" to binding.homePrice.text.toString().toInt(),
                    "rate" to rSelected.rate,
                    "unit" to rSelected.unit,
                    "home_image" to homeImg,
                    // 계획 데이터
                    "plan_food" to planData[0].toInt(),
                    "plan_house" to planData[1].toInt(),
                    "plan_bus" to planData[2].toInt(),
                    "plan_shopping" to planData[3].toInt(),
                    "plan_tourism" to planData[4].toInt()
                    )

                // firebase에 데이터 추가
                db.collection("${StartSet.email}").add(addData)
                    .addOnSuccessListener {
                        // home에 넘길 데이터
                        val inputData = arrayOf(
                            it.id,
                            cSelected,
                            binding.homePrice.text.toString(),
                            binding.homeStartDate.text.toString(),
                            binding.homeEndDate.text.toString(),
                            rSelected.rate,
                            rSelected.unit,
                            homeImg.toString(),
                            binding.homeMainTitle.text.toString(),
                            planData[0], planData[1], planData[2], planData[3], planData[4]
                        )

                        val intent = intent
                        intent.putExtra("home_result", inputData)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "가계부를 등록하지 못했습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
        }
    }

    // 뒤로가기
    override fun onBackPressed() {
        back()
    }

    // x버튼, 뒤로가기 선택 시 동작 메소드
    private fun back(){
        setResult(RESULT_OK, intent)
        finish()
    }
}