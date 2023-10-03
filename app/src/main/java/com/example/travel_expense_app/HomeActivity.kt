package com.example.travel_expense_app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import com.example.travel_expense_app.set.StartSet.Companion.auth
import com.example.travel_expense_app.set.StartSet.Companion.db
import com.example.travel_expense_app.set.StartSet.Companion.email
import com.example.travel_expense_app.databinding.ActivityHomeBinding
import com.example.travel_expense_app.set.*
import com.google.firebase.auth.EmailAuthProvider

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: HomeAdapter
    private var mode: Boolean = false               // 로그인 여부 저장
    private var list: MutableList<Trip>? = null     // 가계부 리스트
    private var clickPosition: Int = 0              // 클릭한 가계부 위치
    private val img = arrayOf(R.drawable.home1, R.drawable.home2, R.drawable.home3,
        R.drawable.home4, R.drawable.home5, R.drawable.home6 )  // 가계부 이미지
    // 이미지 출처
    // home1: https://pixabay.com/ko/photos/%ED%98%B8%EC%88%98-%EC%9E%90%EC%97%B0-%EC%97%AC%ED%96%89%ED%95%98%EB%8B%A4-%ED%83%90%EA%B5%AC-6701636/
    // home2: https://pixabay.com/ko/photos/%EC%99%80%EC%9D%B8%EB%94%A9%EB%8F%84%EB%A1%9C-%EC%9D%BC%EB%AA%B0-%EC%82%B0-%EC%82%B0-%ED%92%8D%EA%B2%BD-1556177/
    // home3: https://pixabay.com/ko/photos/%EC%B2%A0%EB%8F%84-%EA%B8%B0%EC%B0%A8-%ED%8A%B8%EB%9E%99-%EC%97%AC%ED%96%89%ED%95%98%EB%8B%A4-5517562/
    // home4: https://pixabay.com/ko/photos/%ED%95%B4%EB%B3%80-%EB%8C%80%EC%96%91-%EC%A7%80%EC%A3%BC-%EB%B0%94%EB%8B%A4-%ED%95%B4%EC%95%88-2413081/
    // home5: https://pixabay.com/ko/photos/%EA%B3%A0%EC%B8%B5-%EB%B9%8C%EB%94%A9-%EA%B1%B4%EB%AC%BC-%ED%95%AD%EA%B5%AC-%EC%A7%80%ED%8F%89%EC%84%A0-5838029/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바
        setSupportActionBar(binding.homeTb)

        exchangeRate()      // 환율 정보 가져오기
        makeTrip()          // db 저장된 내용 불러오기
        makeHomeText()
        list = mutableListOf()

        // 리사이클러 뷰 그리드로 출력
        val layoutManager = GridLayoutManager(this, 2)
        binding.homeRecyclerView.layoutManager = layoutManager
        adapter = HomeAdapter(this, list, img)
        binding.homeRecyclerView.adapter=adapter

        val requestLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult())
        { it ->
            // 로그인 시
            it.data!!.getStringExtra("mode")?.let {
                mode = true
                binding.homeTb.menu.clear()     // 로그인 시 메뉴 목록 변경
                onCreateOptionsMenu(binding.homeTb.menu)
                makeTrip()
            }

            // 가계부 추가
            it.data!!.getStringArrayExtra("home_result")?.let {
                // 가계부 id, 여행지, 경비, 시작일, 종료일, 환율, 통화코드, 가계부 이미지, 여행 제목
                val data = Trip(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7].toInt(), it[8],
                    Plan(it[9].toString(), it[10].toString(), it[11].toString(), it[12].toString(), it[13].toString()))
                list?.add(data)
                list?.sortWith(compareByDescending(Trip::start_date).thenByDescending(Trip::id))    // 시작일 내림차순, id 내림차순으로 정렬
                makeHomeText()
                adapter.notifyDataSetChanged()
            }

            // 가계부 삭제
            it.data!!.getStringExtra("delete_result")?.let{
                list?.removeAt(clickPosition)
                makeHomeText()
                adapter.notifyDataSetChanged()
            }

            // main에서 x버튼 클릭시 (가계부 정보를 수정했을 때 데이터 받기 위함)
            it.data!!.getStringArrayExtra("xBtn")?.let{
                val card = list?.get(clickPosition)
                card?.price = it[0]
                card?.start_date = it[1].substring(0, 10)
                card?.end_date = it[2].substring(0, 10)
                card?.rate = it[3]
                card?.mainTitle = it[4]
                adapter.notifyDataSetChanged()
            }
        }

        // 가계부 추가 아이콘 클릭 시
        binding.homeFab.setOnClickListener {
            // 로그인 여부에 따라 접근 제한
            if (mode) {
                val intent = Intent(this, HomeAddActivity::class.java)
                requestLauncher.launch(intent)
            } else {
                Toast.makeText(this, "로그인 해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 메뉴 클릭
        binding.homeTb.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.menu_login -> {    // 로그인
                    val intent = Intent(this, AuthActivity::class.java)
                    requestLauncher.launch(intent)
                }
                R.id.menu_logout -> {   // 로그아웃
                    auth.signOut()
                    userLogout()
                    Toast.makeText(this, "로그아웃 했습니다.", Toast.LENGTH_SHORT).show()
                }
                R.id.menu_user_del -> { // 회원탈퇴
                    // AlertDialog 띄우기
                    val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val view = inflater.inflate(R.layout.popup_user_del, null)
                    val subView = view.findViewById<TextView>(R.id.popup_subtitle)
                    val pwdView = view.findViewById<TextView>(R.id.popup_pwd)
                    subView.text = "삭제시 모든 데이터는 영구적으로 삭제됩니다."

                    val alertDialog = AlertDialog.Builder(this)
                        .setTitle("계정을 삭제하시겠습니까?")
                        .setPositiveButton("확인"){ _, _ ->      // 확인 클릭시
                            val user = auth.currentUser         // 현재 로그인한 사용자
                            val pwd = pwdView.text.toString()   // 입력한 비밀번호
                            val email = user?.email!!           // 현재 로그인한 사용자 이메일
                            // 회원 재인증(탈퇴는 민감한 사안이기 때문)
                            val credential = EmailAuthProvider.getCredential(email, pwd)
                            user.reauthenticate(credential).addOnSuccessListener {     // 재인증 성공
                                user.delete().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {    // 탈퇴 성공
                                        for(i in list!!){
                                            db.collection(email).document(i.id).collection("trip_item").get()
                                                .addOnSuccessListener { result ->
                                                    for(item in result){
                                                        if(item.data["image"] == true){     // 이미지 있는 지출 내역의 이미지 삭제
                                                            val imgRef = StartSet.storage.reference.child("${i.id}/${item.id}.jpg")
                                                            imgRef.delete()
                                                        }
                                                        // 지출 내역 삭제
                                                        db.collection(email).document(i.id).collection("trip_item").document(
                                                            item.id
                                                        ).delete()
                                                    }
                                                    // 가계부 삭제
                                                    db.collection(email).document(i.id).delete()
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(this, "데이터를 삭제하지 못했습니다.", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        userLogout()
                                        Toast.makeText(this, "회원탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()
                                    } else
                                        Toast.makeText(this, "회원탈퇴에 실패하였습니다", Toast.LENGTH_SHORT).show()
                                }
                            }
                                .addOnFailureListener{      // 재인증 실패
                                    Toast.makeText(this, "비밀번호가 맞지않습니다", Toast.LENGTH_SHORT).show()
                            }

                        }
                        .setNeutralButton("취소", null)       // 취소 클릭시
                        .create()

                    alertDialog.setView(view)
                    alertDialog.show()
                }
            }
            true
        }

        // 가계부 클릭 시
        adapter.itemClick = object: HomeAdapter.ItemClick{
            override fun onClick(view: View, position: Int) {
                val intent = Intent(this@HomeActivity, MainActivity::class.java)
                val card = list?.get(position)
                val cardId = card?.id
                clickPosition = position    // 가계부 위치

                val homeData = arrayOf(cardId, "${card?.start_date} 00:00:00", "${card?.end_date} 00:00:00", card?.rate, card?.unit)  // 가계부 id, 시작일, 종료일, 환율, 통화코드
                val barData = arrayOf(card?.country, card?.mainTitle)     // 여행지, 여행제목
                val planData = arrayOf(card?.plan?.food, card?.plan?.house, card?.plan?.bus, card?.plan?.shopping, card?.plan?.tourism)
                intent.putExtra("homeData", homeData)
                intent.putExtra("barData", barData)
                intent.putExtra("total", card?.price?.toInt())      // 경비
                intent.putExtra("plan", planData)
                requestLauncher.launch(intent)
            }
        }
    }

    // 메뉴 등록
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // 로그인 여부에 따른 메뉴
        if(mode) menuInflater.inflate(R.menu.home_menu, menu)       // 로그인 o
        else menuInflater.inflate(R.menu.home_login, menu)          // 로그인 x
        return super.onCreateOptionsMenu(menu)
    }

    private fun makeHomeText(){
        if(!mode || list?.isEmpty() == true) binding.homeText.text = "여행 정보가 없습니다. 정보를 추가 해주세요."     // 로그인 x 이거나 가계부 없는 경우
        else binding.homeText.text = ""
    }

    private fun makeTrip(){
        mode = StartSet.checkAuth()     // 이전 로그인 여부

        // firebase에서 로그인한 사용자 데이터 가져오기
        db.collection("$email").get().addOnSuccessListener { result ->
                for (item in result) {
                    val planDatas = Plan(item.data["plan_food"].toString(), item.data["plan_house"].toString(), item.data["plan_bus"].toString(),
                        item.data["plan_shopping"].toString(), item.data["plan_tourism"].toString())

                    // 가계부 id, 여행지, 경비, 시작일, 종료일, 환율, 통화코드, 가계부 이미지, 여행 제목
                    val data = Trip(item.id, item.data["country"].toString(), item.data["price"].toString(),
                        item.data["start_date"].toString(), item.data["end_date"].toString(), item.data["rate"].toString(),
                        item.data["unit"].toString(), item.data["home_image"].toString().toInt(), item.data["title"].toString(),
                        planDatas)
                    list?.add(data)
                }
                list?.sortWith(compareByDescending(Trip::start_date).thenByDescending(Trip::id))    // 시작일 내림차순, id 내림차순으로 정렬
                makeHomeText()
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener{
                Toast.makeText(this, "가계부를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exchangeRate() {    // 환율 가져오기
        val t = Thread(Rate())
        t.start()
    }

    private fun userLogout(){       // 로그아웃시 동작
        email = null
        mode = false
        list?.clear()
        adapter.notifyDataSetChanged()
        binding.homeTb.menu.clear()
        onCreateOptionsMenu(binding.homeTb.menu)
        makeHomeText()
    }
}