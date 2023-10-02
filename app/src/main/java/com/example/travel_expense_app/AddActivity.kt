package com.example.travel_expense_app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.travel_expense_app.databinding.ActivityAddBinding
import com.example.travel_expense_app.set.Detail
import com.example.travel_expense_app.set.DetailAdapter
import com.example.travel_expense_app.set.ItemAdapter
import com.example.travel_expense_app.set.StartSet
import com.example.travel_expense_app.set.StartSet.Companion.db
import com.example.travel_expense_app.set.StartSet.Companion.storage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class AddActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddBinding
    private lateinit var photoURI: Uri
    private lateinit var adapter: DetailAdapter
    private var filePath = ""           // 이미지파일 절대경로 저장 변수
    private var category = ""           // 카테고리 저장 변수
    private var cardId = ""             // 가계부 id 저장 변수
    private var itemId = ""             // 지출 내역 id 저장 변수
    private var price = "0"             // 지출 금액 저장 변수
    private var code = ""               // 이미지 추가시 갤러리 or 카메라 구분 변수
    private var image = false           // 이미지 유무
    private var modifyImage = false     // 수정버튼 클릭 전 이미지 유무
    private var changeImage = false     // 수정할때 이미지 변경하는가 유무
    private var modifyMode = false      // 메뉴 제목 바꾸기 위한 변수
    private var detailList :MutableList<Detail> = mutableListOf()       // 상세 지출 내역 확정 리스트
    var _detailList :MutableList<Detail> = mutableListOf()      // 상세 지출 내역 임시 리스트
    var detailId:MutableList<String> = mutableListOf()          // 변경사항 있는 상제 지출 내역 id들


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바
        setSupportActionBar(binding.addTb)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)   // 뒤로가기 활성화
        supportActionBar?.setDisplayShowTitleEnabled(false) // 제목 비활성화

        // 가계부 id, 시작일, 종료일 초기화
        cardId = intent.getStringExtra("card").toString()
        val sDay = intent.getStringExtra("startDay")
        val eDay = intent.getStringExtra("endDay")

        val unit = intent.getStringExtra("unit").toString()
        binding.addDate.text = sDay?.substring(0, 10)
        binding.addTime.text = SimpleDateFormat("HH:mm").format(System.currentTimeMillis())
        binding.addUnit.text = unit

        // 지출 내역 수정 시
        intent.getStringArrayExtra("main_modify")?.let{ it ->
            modifyMode = true
            cardId = it[0]
            itemId = it[1]
            price = it[2]
            binding.addTitle.setText(it[3])
            binding.addContent.setText(it[4])
            selectCategory(it[5].toInt(), it[6])
            binding.addDate.text = it[7]
            binding.addTime.text = it[8]
            binding.addPrice.text = price
            image = it[9].toBoolean()
            if(image) {     // 이미지 있는 경우
                val imgRef = storage.reference.child("${cardId}/${itemId}.jpg")         // 실제 파일 참조하는 레퍼런스 생성
                imgRef.downloadUrl.addOnCompleteListener {
                    if (it.isSuccessful) Glide.with(this).load(it.result).into(binding.addImage)        // 이미지 불러오기 성공
                }
                modifyImage = true      // 기존 이미지 존재
            }

            setDetail()     // 상세 지출 내역 세팅
        }

        // 여행 시작일, 종료일 세팅
        val startDay = Calendar.getInstance().apply {
            if (sDay != null) set(sDay.substring(0, 4).toInt(), sDay.substring(5, 7).toInt() - 1, sDay.substring(8, 10).toInt())
        }
        val endDay = Calendar.getInstance().apply {
            if (eDay != null) set(eDay.substring(0, 4).toInt(), eDay.substring(5, 7).toInt() - 1, eDay.substring(8, 10).toInt())
        }

        // 지출 내역 일자 선택
        binding.addDt.setOnClickListener {
            // 시간 다이얼로그
            val timePickerDialog = TimePickerDialog(this, R.style.AppTheme_AlertDialogTheme,
                { _, hourOfDay, minute ->
                    val timeString = if(hourOfDay in 0..9 && minute in 0..9) "0${hourOfDay}:0${minute}"
                                    else if(hourOfDay in 0..9) "0${hourOfDay}:${minute}"
                                    else if(minute in 0..9) "${hourOfDay}:0${minute}"
                                    else "${hourOfDay}:${minute}"
                    binding.addTime.text = timeString
                }, binding.addTime.text.substring(0,2).toInt(), binding.addTime.text.substring(3,5).toInt(), true)
            timePickerDialog.show()

            // 날짜 다이얼로그
            val cal = Calendar.getInstance()    //캘린더뷰 만들기
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                val dateString = if(month in 0..8 && dayOfMonth in 1..9) "${year}.0${month+1}.0${dayOfMonth}"
                                else if(month in 0..8) "${year}.0${month+1}.${dayOfMonth}"
                                else if(dayOfMonth in 1..9) "${year}.${month+1}.0${dayOfMonth}"
                                else "${year}.${month+1}.${dayOfMonth}"
                binding.addDate.text = dateString
            }
            DatePickerDialog(this, R.style.AppTheme_AlertDialogTheme, dateSetListener,
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.minDate = startDay.timeInMillis
                         datePicker.maxDate = endDay.timeInMillis}  // 여행 총 기간 중에서만 선택 가능
                .show()
        }

        // 카테고리 버튼 선택
        binding.foodBtn.setOnClickListener { selectCategory(0, "음식") }
        binding.houseBtn.setOnClickListener { selectCategory(1, "숙박") }
        binding.busBtn.setOnClickListener { selectCategory(2, "교통") }
        binding.shoppingBtn.setOnClickListener { selectCategory(3, "쇼핑") }
        binding.tourismBtn.setOnClickListener { selectCategory(4, "관광") }
        binding.etcBtn.setOnClickListener { selectCategory(5, "기타") }

        // 숫자 선택
        binding.num1.setOnClickListener { selectNumber(1) }
        binding.num2.setOnClickListener { selectNumber(2) }
        binding.num3.setOnClickListener { selectNumber(3) }
        binding.num4.setOnClickListener { selectNumber(4) }
        binding.num5.setOnClickListener { selectNumber(5) }
        binding.num6.setOnClickListener { selectNumber(6) }
        binding.num7.setOnClickListener { selectNumber(7) }
        binding.num8.setOnClickListener { selectNumber(8) }
        binding.num9.setOnClickListener { selectNumber(9) }
        binding.num0.setOnClickListener { selectNumber(0) }
        binding.numDel.setOnClickListener { selectNumber(-1) }

        binding.addImage.setOnClickListener {
            // 카메라, 갤러리 선택 alertDialog 띄우기
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.popup_hm, null)
            val cg = arrayOf("카메라", "갤러리")
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("사진 선택")
                .setItems(cg){_, i ->
                    when(i){
                        // 카메라 선택
                        0 -> {
                            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                                cameraOpen()          // 카메라 권한 허용된 경우
                            else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),99) // 권한 요청
                        }
                        // 갤러리 선택
                        1 -> {
                            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                                galleryOpen()          // 갤러리 권한 허용된 경우
                            else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),1) // 권한 요청
                        }
                    }
                }
                .create()
            alertDialog.setView(view)
            alertDialog.show()
            alertDialog.window?.setLayout(600, 400)
        }

        // 상세 지출 내역 추가
        binding.detail.setOnClickListener {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.popup_detail, null)
            val recyclerView = view.findViewById<RecyclerView>(R.id.detail_add_recyclerView)
            val nameView = view.findViewById<EditText>(R.id.detail_name)
            val priceView = view.findViewById<EditText>(R.id.detail_price)
            val unitView = view.findViewById<TextView>(R.id.detail_unit)
            val qtyView = view.findViewById<EditText>(R.id.detail_qty)
            val btnView = view.findViewById<Button>(R.id.detail_btn)
            unitView.text = unit

            // 리사이클러 뷰 출력
            val layoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager=layoutManager
            adapter= DetailAdapter(this, _detailList, unit, detailId)
            recyclerView.adapter=adapter
            recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

            val alertDialog = AlertDialog.Builder(this)
                .setPositiveButton("확인"){ _, _ ->      // 확인 클릭시
                    detailList.clear()
                    for(d in _detailList) detailList.add(d) // 임시 목록 아이템을 확정 목록에 저장
                }
                .setNeutralButton("취소"){ _, _ ->      // 취소 클릭시
                    _detailList.clear()
                    detailId.clear()
                    for(d in detailList) _detailList.add(d) // 원래 가지고 있던 상세 지출 내역 임시 목록에 다시 저장해 복구
                }
                .create()
            alertDialog.setView(view)
            alertDialog.show()

            btnView.setOnClickListener {
                if(nameView.text.isEmpty()) Toast.makeText(this, "메뉴 / 상품명을 입력해주세요.", Toast.LENGTH_SHORT).show()
                else if(priceView.text.isEmpty()) Toast.makeText(this, "금액을 입력해주세요.", Toast.LENGTH_SHORT).show()
                else if(qtyView.text.isEmpty()) Toast.makeText(this, "수량을 입력해주세요.", Toast.LENGTH_SHORT).show()
                else {
                    _detailList.add(Detail("", nameView.text.toString(), priceView.text.toString().toInt(), qtyView.text.toString().toInt()))   // 추가한 내역 임시 목록에 추가
                    adapter.notifyDataSetChanged()

                    nameView.text.clear()
                    priceView.text.clear()
                    qtyView.text.clear()
                }
            }
        }
    }

    private val requestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult())
    {
        if (it.resultCode === Activity.RESULT_OK) {
            when (code) {
                "camera" -> {       // 카메라 선택한 경우
                    binding.addImage.setImageURI(photoURI)
                    image = true
                    changeImage = true
                }
                "gallery" -> {      // 갤러리 선택한 경우
                    Glide.with(applicationContext).load(it.data?.data).apply(RequestOptions()
                        .override(250, 200)).centerCrop().into(binding.addImage)
                    val cursor = contentResolver.query(it.data?.data as Uri, arrayOf(MediaStore.Images.Media.DATA),
                        null, null, null)
                    cursor?.moveToFirst().let {
                        filePath = cursor?.getString(0) as String
                        image = true
                        changeImage = true
                    }
                }
            }
        }
    }

    // 카메라, 갤러리 권한 요청 처리 결과 수신 함수
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            99 -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) cameraOpen()          // 카메라 권한 허용된 경우
                else Toast.makeText(this, "카메라 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            }
            1 -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) galleryOpen()          // 갤러리 권한 허용된 경우
                else Toast.makeText(this, "갤러리 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 메뉴 생성
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_menu, menu)
        if(modifyMode){     // 지출 내역 수정시
            val modifyMenuItem = menu?.findItem(R.id.save)
            modifyMenuItem?.title = "수정"
        }
        return super.onCreateOptionsMenu(menu)
    }

    // 메뉴 선택
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when(item.itemId) {
        android.R.id.home -> {  // 뒤로가기(화살표)
            back()
            false
        }
        R.id.save -> {  // 등록 or 수정 클릭 시
            if(binding.addTitle.text.isEmpty()){        // 제목 미입력
                Toast.makeText(this,"제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                false
            }
            else if(binding.addPrice.text == "0"){       // 지출 금액 미입력
                Toast.makeText(this,"지출 금액을 입력해주세요.", Toast.LENGTH_SHORT).show()
                false
            }
            else if(category.isEmpty()){                // 카테고리 미선택
                Toast.makeText(this,"카테고리를 입력해주세요.", Toast.LENGTH_SHORT).show()
                false
            }
            else if(itemId.isNotEmpty()){               // 수정 시
                db.collection("${StartSet.email}").document(cardId).collection("trip_item").document(itemId).update(mapOf(
                    "title" to binding.addTitle.text.toString(),
                    "price" to binding.addPrice.text.toString().toInt(),
                    "content" to binding.addContent.text.toString(),
                    "date" to binding.addDate.text.toString(),
                    "category" to category,
                    "image" to image,
                    "time" to binding.addTime.text.toString()
                ))
                    .addOnFailureListener {
                        Toast.makeText(this,"지출 내역을 수정하지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                if(modifyImage && changeImage)deleteImage(itemId)   // 기존 이미지가 존재하나 이미지 변경한 경우

                // 상세 지출 내역은 삭제로 수정 가능
                for(d in detailId)
                    if(d != "")     // 삭제한 상세 지출 내역이 있는 경우 삭제
                        db.collection("${StartSet.email}").document(cardId).collection("trip_item").document(itemId).collection("detail").document(d).delete()

                for(d in detailList){   // db에 상세 지출 내역 추가
                    if(d.d_id == ""){
                        val dData = mapOf(
                            "name" to d.d_name,
                            "price" to d.d_price,
                            "qty" to d.d_qty
                        )
                        db.collection("${StartSet.email}").document(cardId).collection("trip_item").document(itemId).collection("detail").add(dData)
                            .addOnSuccessListener { dIt ->
                                dIt.update("id", dIt.id)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this,"상세 내역을 추가하지 못했습니다.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                val modifyData = arrayOf(itemId, filePath, changeImage.toString())
                intent.putExtra("main_modify_result", modifyData)
                setResult(Activity.RESULT_OK, intent)
                finish()
                true
            }
            else {                              // 등록 시
                val data = mapOf(
                    "card_id" to cardId,
                    "title" to binding.addTitle.text.toString(),
                    "price" to binding.addPrice.text.toString().toInt(),
                    "content" to binding.addContent.text.toString(),
                    "date" to binding.addDate.text.toString(),
                    "category" to category,
                    "image" to image,
                    "time" to binding.addTime.text.toString()
                )
                db.collection("${StartSet.email}").document(cardId).collection("trip_item").add(data)
                    .addOnSuccessListener {
                        val inputData = arrayOf(
                            it.id,             // 아이템 id 겸 이미지 id
                            binding.addPrice.text.toString(),
                            binding.addTitle.text.toString(),
                            binding.addDate.text.toString(),
                            image.toString(),
                            filePath,
                            category,
                            binding.addTime.text.toString()
                        )

                        // 상세 지출 내역 추가
                        for(d in detailList){
                            val dData = mapOf(
                                "name" to d.d_name,
                                "price" to d.d_price,
                                "qty" to d.d_qty
                            )
                            db.collection("${StartSet.email}").document(cardId).collection("trip_item").document(it.id).collection("detail").add(dData)
                                .addOnSuccessListener { dIt ->
                                    dIt.update("id", dIt.id)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this,"상세 내역을 추가하지 못했습니다.", Toast.LENGTH_SHORT).show()
                                }
                        }

                        val intent = intent
                        intent.putExtra("result", inputData)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this,"지출 내역을 추가하지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                true
            }
        }
        else -> true
    }

    // 뒤로가기
    override fun onBackPressed() {
        back()
    }

    // 카테고리 버튼 선택에 따른 배경색 변경 함수
    private fun selectCategory(no: Int, type: String){
        val btnList = arrayOf(binding.foodBtn, binding.houseBtn, binding.busBtn, binding.shoppingBtn, binding.tourismBtn, binding.etcBtn)
        for((index, btn) in  btnList.withIndex()){
            if(no == index) {
                category = type
                btn.setBackgroundResource(R.drawable.icon_round)
            }
            else btn.setBackgroundColor(Color.parseColor("#00000000"))
        }
    }

    // 금액 입력 버튼 선택 함수
    private fun selectNumber(num: Int){
        when(num){
            in 0..9 -> {
                if(price == "0" && num != 0) {
                    price = ""
                    price += num.toString()
                }
                else if(price != "0") price += num.toString()
            }
            -1 -> {
                price = if(price.length == 1) "0"
                        else price.dropLast(1)
            }
        }
        binding.addPrice.text = price
    }

    // 이미지 삭제 함수
    private fun deleteImage(id: String){
        val storage = storage
        val storageRef: StorageReference = storage.reference     // 스토리지 참조하는 레퍼런스 생성
        val imgRef: StorageReference = storageRef.child("${cardId}/${id}.jpg")      // 실제 파일 참조하는 레퍼런스 생성
        // 파일 삭제
        imgRef.delete()
    }

    // 카메라 앱 열기
    private fun cameraOpen(){
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)     // 해당 앱에서만 사용 가능한 사진
        val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)    // 파일 생성
        filePath = file.absolutePath    // 파일 절대경로
        photoURI = FileProvider.getUriForFile(this, "com.example.travel_expense_app.fileprovider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        code = "camera"
        requestLauncher.launch(intent)
    }
    // 갤러리 앱 열기
    private fun galleryOpen(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*")
        code = "gallery"
        requestLauncher.launch(intent)
    }

    // x버튼, 뒤로가기 선택 시 동작 함수
    private fun back(){
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    // 상세 지출 내역 세팅 함수
    private fun setDetail(){
        db.collection("${StartSet.email}").document(cardId).collection("trip_item").document(itemId).collection("detail").get()
            .addOnSuccessListener { item ->
                for(i in item) {
                    _detailList.add(Detail(i.id, i.data?.get("name").toString(), i.data?.get("price").toString().toInt(), i.data?.get("qty").toString().toInt()))
                    detailList.add(Detail(i.id, i.data?.get("name").toString(), i.data?.get("price").toString().toInt(), i.data?.get("qty").toString().toInt()))
                }
            }
    }
}