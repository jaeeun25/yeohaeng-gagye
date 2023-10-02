package com.example.travel_expense_app.set

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel_expense_app.AddActivity
import com.example.travel_expense_app.databinding.AddItemBinding
import com.example.travel_expense_app.databinding.DetailAddItemBinding
import com.example.travel_expense_app.databinding.DetailItemBinding
import com.example.travel_expense_app.databinding.HomeItemBinding

// 가계부 어댑터(home)
class HomeViewHolder(val binding: HomeItemBinding): RecyclerView.ViewHolder(binding.root)
class HomeAdapter(val context: Context, private val homeDatas: MutableList<Trip>?, private val img: Array<Int>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    interface ItemClick{
        fun onClick(view: View, position: Int)
    }
    var itemClick: ItemClick? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return HomeViewHolder(HomeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding=(holder as HomeViewHolder).binding
        binding.tripTitle.text = homeDatas?.get(position)?.mainTitle    // 여행 제목
        binding.tripCountry.text = homeDatas?.get(position)?.country    // 여행지
        binding.tripDate.text= "${homeDatas?.get(position)?.start_date} - ${homeDatas?.get(position)?.end_date}"         // 여행 기간
        binding.tripPrice.text =            // 경비
            if(homeDatas?.get(position)?.price?.toInt()!! >= 10000000) "${comma((homeDatas[position].price.toInt()/1000).toString())}K KRW"   // 경비 천만원 이상시 축약
            else "${comma(homeDatas[position].price)} KRW"
        binding.tripImage.setImageResource(img[homeDatas[position].image])        // 가계부 이미지

        // 가계부 클릭 이벤트
        if(itemClick != null){
            holder.binding.root.setOnClickListener {
                itemClick?.onClick(it, position)
            }
        }
    }

    override fun getItemCount(): Int{
        return homeDatas?.size ?: 0
    }
}

// 지출 내역 어댑터(main)
class ViewHolder(val binding: AddItemBinding): RecyclerView.ViewHolder(binding.root)
class Adapter(val context: Context, private val mainDatas: MutableList<TabItem>?, private var cardId:String?, var rate: Float, private val unit: String, var line: MutableMap<String, Boolean>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface ItemClick{
        fun onClick(view: View, position: Int)
    }
    var itemClick: ItemClick? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(AddItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding=(holder as ViewHolder).binding
        binding.itemKrw.text = "${comma(mainDatas?.get(position)?.price?.times(rate)?.toInt().toString())} KRW"     // 지출 금액 (원화)
        binding.itemTitle.text = mainDatas?.get(position)?.title                // 지출 내역 제목
        binding.itemRate.text = "${mainDatas?.get(position)?.price} $unit"    // 지출 금액 (환율 적용)

        // 이미지 관련
        if(mainDatas?.get(position)?.image == true){    // 이미지 있는 경우
            // 이미지 다운로드
            val imgRef= StartSet.storage.reference.child("${cardId}/${mainDatas[position].id}.jpg")
            imgRef.downloadUrl.addOnCompleteListener { task ->
                if (task.isSuccessful) Glide.with(context).load(task.result).into(holder.binding.itemImage)
            }
        }
        else binding.itemImage.setImageResource(0)

        // 날짜 구분선
        val date = mainDatas?.get(position)?.date?.substring(8, 10)     // 지출 날짜
        if(line[date] == false) {                                      // 해당 날짜의 첫 내역일 경우
            binding.itemBar.visibility = View.VISIBLE
            binding.itemDate.text = mainDatas?.get(position)?.date
            binding.itemRoot.layoutParams.height = 175      // px 사이즈
            line[date.toString()] = true
        }
        else{       // 이미 날짜가 표시된 경우
            binding.itemBar.visibility = View.GONE
            binding.itemRoot.layoutParams.height = 145      // px 사이즈
        }

        // 지출 내역 클릭 이벤트
        if(itemClick != null){
            holder.binding.root.setOnClickListener {
                itemClick?.onClick(it, position)
            }
        }
    }

    override fun getItemCount(): Int{
        return mainDatas?.size ?: 0
    }
}

// item 화면_상세 내역 어댑터(item_detail)
class ItemViewHolder(val binding: DetailItemBinding): RecyclerView.ViewHolder(binding.root)
class ItemAdapter(val context: Context, private val itemDatas: MutableList<Detail>?, val unit: String): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemViewHolder(DetailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as ItemViewHolder).binding
        binding.dName.text = itemDatas?.get(position)?.d_name
        binding.dPrice.text = "${itemDatas?.get(position)?.d_price.toString()} ${unit}"
        binding.dQty.text = itemDatas?.get(position)?.d_qty.toString()
    }

    override fun getItemCount(): Int {
        return itemDatas?.size ?: 0
    }
}

// add 화면_상세 내역 어댑터(add_detail)
class DetailViewHolder(val binding: DetailAddItemBinding): RecyclerView.ViewHolder(binding.root)
class DetailAdapter(val context: Context, private val _detailDatas: MutableList<Detail>?, val unit: String, private val detailId: MutableList<String>?): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return DetailViewHolder(DetailAddItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as DetailViewHolder).binding
        binding.daName.text = _detailDatas?.get(position)?.d_name
        binding.daPrice.text = "${_detailDatas?.get(position)?.d_price.toString()} ${unit}"
        binding.daQty.text = _detailDatas?.get(position)?.d_qty.toString()

        binding.daDel.setOnClickListener {
            detailId?.add(_detailDatas?.get(position)?.d_id.toString())
            _detailDatas?.removeAt(position)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return _detailDatas?.size ?: 0
    }
}



