package com.example.travel_expense_app.set

import android.widget.Toast
import androidx.multidex.MultiDexApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class StartSet: MultiDexApplication() {
    companion object {
        lateinit var auth: FirebaseAuth
        lateinit var db: FirebaseFirestore
        lateinit var storage: FirebaseStorage
        lateinit var rateMap: MutableMap<String, String>
        lateinit var spinnerData: MutableList<String>
        lateinit var countryList: MutableList<String>
        var email: String? = null
        fun checkAuth(): Boolean {  // 로그인 여부 확인
            val currentUser = auth.currentUser  // 현재 로그인한 사용자
            return currentUser?.let {
                email = currentUser.email
                currentUser.isEmailVerified     // 메일로 인증이 된 사용자인지
            } ?: let {
                false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        storage = Firebase.storage
        rateMap = mutableMapOf()        // 환율 정보 담을 map
        spinnerData = mutableListOf()   // home_add 스피너에 담을 데이터
        setCountry()
    }

    // 여행지 목록 세팅 함수
    fun setCountry(){
        countryList = mutableListOf()
        countryList.add("대한민국")
        db.collection("country").orderBy("name", Query.Direction.ASCENDING).get().addOnSuccessListener { result ->
                for (item in result) countryList.add(item.data["name"].toString())         // 국가 가져오기
            }
    }
}
