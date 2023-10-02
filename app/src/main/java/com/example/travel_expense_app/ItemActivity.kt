package com.example.travel_expense_app

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat.canScrollVertically
import androidx.core.view.ViewCompat.setNestedScrollingEnabled
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.travel_expense_app.set.StartSet.Companion.db
import com.example.travel_expense_app.databinding.ActivityItemBinding
import com.example.travel_expense_app.set.*

class ItemActivity : AppCompatActivity() {
    private lateinit var binding: ActivityItemBinding
    private lateinit var adapter: ItemAdapter
    private lateinit var requestLauncher: ActivityResultLauncher<Intent>
    private val categoryNo = mapOf("음식" to 0, "숙박" to 1, "교통" to 2, "쇼핑" to 3, "관광" to 4, "기타" to 5)        // 카테고리 데이터 초기화
    private var image: Boolean = false                  // 이미지 유무
    var detailList = mutableListOf<Detail>()    // 상세 지출 내역 리스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var itemId: String
        var cardId: String
        var sDay: String
        var eDay: String
        var rate: String
        var unit: String

        // 지출 내역 수정 시 수정 데이터 메인에 넘김
        requestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ it ->
            it.data!!.getStringArrayExtra("main_modify_result")?.let {
                intent.putExtra("modify", it)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

        // 메인에서 넘어온 데이터 받아서 세팅
        intent.getStringArrayExtra("itemData").let{
            itemId = it?.get(0).toString()
            cardId = it?.get(1).toString()
            sDay = it?.get(2).toString()
            eDay = it?.get(3).toString()
            rate = it?.get(4).toString()
            unit = it?.get(5).toString()
        }

        itemId.let{
            // 지출 내역 정보 가져오기
            db.collection("${StartSet.email}").document(cardId).collection("trip_item").document(itemId).get()
                .addOnSuccessListener { item ->
                    binding.tiTitle.text = item.data?.get("title").toString()
                    binding.tiKrw.text = "${comma((item.data?.get("price").toString().toFloat()*rate.toFloat()).toInt().toString())} KRW"
                    binding.tiContent.text = item.data?.get("content").toString()
                    binding.tiDate.text = item.data?.get("date").toString()
                    binding.tiTime.text = item.data?.get("time").toString()
                    binding.tiCategory.text = item.data?.get("category").toString()
                    binding.tiRate.text = item.data?.get("price").toString()
                    binding.tiUnit.text = " $unit"
                    image = item.data?.get("image") as Boolean
                    if(image) imageEvent(cardId, itemId, "down")    // 이미지 o -> 이미지 로드
                    else binding.tiImage.visibility = View.GONE           // 이미지 x -> 이미지뷰 숨기기
                    if(item.data?.get("content").toString().isEmpty()) binding.tiContent.visibility =  View.GONE    // 지출 내역 메모 없는 경우 해당 뷰 숨기기

                    // 상세 내역 가져오기
                    setDetail(cardId, itemId)
                }
                .addOnFailureListener{
                    Toast.makeText(this, "지출 내역을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
        }

        // x버튼 클릭 시
        binding.xBtn.setOnClickListener{
            back()
        }

        // 수정 버튼 클릭 시
        binding.tiModify.setOnClickListener {
            val data = arrayOf(
                cardId,
                itemId,
                binding.tiRate.text.toString(),
                binding.tiTitle.text.toString(),
                binding.tiContent.text.toString(),
                categoryNo["${binding.tiCategory.text}"].toString(),
                binding.tiCategory.text.toString(),
                binding.tiDate.text.toString(),
                binding.tiTime.text.toString(),
                image.toString()
            )

            val intent = Intent(this, AddActivity::class.java)
            intent.putExtra("main_modify", data)
            intent.putExtra("startDay", sDay)
            intent.putExtra("endDay", eDay)
            intent.putExtra("unit", unit)
            requestLauncher.launch(intent)
        }

        // 삭제 버튼 클릭 시
        binding.tiDelete.setOnClickListener{
            if(detailList?.isNotEmpty()){
                for(d in detailList){
                    db.collection("${StartSet.email}").document("$cardId").collection("trip_item").document(itemId).collection("detail")
                        .document(d.d_id).delete()
                }
            }
            db.collection("${StartSet.email}").document(cardId).collection("trip_item").document(itemId).delete()
            if(image) imageEvent(cardId, itemId, "del")     // 이미지 있는 경우 이미지 삭제

            intent.putExtra("delete_main", "remove")
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        // 리사이클러 뷰 출력
        val layoutManager = LinearLayoutManager(this)
        binding.detailRecyclerView.layoutManager=layoutManager
        adapter= ItemAdapter(this, detailList, unit)
        binding.detailRecyclerView.adapter=adapter
        binding.detailRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        binding.detailRecyclerView.layoutManager
    }

    // 뒤로가기
    override fun onBackPressed() {
        back()
    }

    // x버튼, 뒤로가기 클릭 시 동작 함수
    private fun back(){
        intent.putExtra("status", "standard")
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    // 이미지 관련 함수
    private fun imageEvent(cardId: String, itemId: String, event: String){
        val imgRef = StartSet.storage.reference.child("${cardId}/${itemId}.jpg")    // 실제 파일 참조하는 레퍼런스 생성
        if(event == "down") {       // 이미지 로드
            imgRef.downloadUrl.addOnCompleteListener {
                if (it.isSuccessful) Glide.with(this).load(it.result).into(binding.tiImage)
            }
        }
        else imgRef.delete()        // 이미지 삭제
    }

    private fun setDetail(cardId: String, itemId: String){
        db.collection("${StartSet.email}").document(cardId).collection("trip_item").document(itemId).collection("detail").get()
            .addOnSuccessListener { item ->
                if(item.isEmpty) binding.detailContainer.visibility =  View.GONE
                for(i in item){
                    detailList.add(Detail(i.id, i.data?.get("name").toString(), i.data?.get("price").toString().toInt(), i.data?.get("qty").toString().toInt()))
                }
                adapter.notifyDataSetChanged()

            }
            .addOnFailureListener{
                Toast.makeText(this, "상세 내역을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}