package com.example.travel_expense_app

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.travel_expense_app.set.StartSet.Companion.auth
import com.example.travel_expense_app.databinding.ActivityAuthBinding
import com.example.travel_expense_app.set.StartSet

class AuthActivity : AppCompatActivity() {
    lateinit var binding: ActivityAuthBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이메일 editView에 포커스 가도록
        binding.authEmail.isFocusableInTouchMode = true
        binding.authEmail.requestFocus()

        // 로그인
        binding.loginBtn.setOnClickListener {
            val email = binding.authEmail.text.toString()
            val pwd = binding.authPwd.text.toString()
            auth.signInWithEmailAndPassword(email, pwd).addOnCompleteListener(this) { task ->
                binding.authEmail.text.clear()
                binding.authPwd.text.clear()
                if (task.isSuccessful) {    // 로그인 성공
                    if (StartSet.checkAuth()) {    // 메일 인증 여부 확인
                        StartSet.email = email
                        intent.putExtra("mode", "login")    // home 액티비티에 login 했음을 알리기 위함
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                    // 메일로 인증 확인을 안 한 경우
                    else Toast.makeText(this,"이메일이 인증되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
                // 로그인 실패
                else Toast.makeText(this, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 회원가입 버튼 클릭 시
        binding.sign.setOnClickListener {
            binding.authEmail.requestFocus()
            binding.authEmail.text = null
            binding.authPwd.text = null
            binding.authTitle.text = "회원가입"
            binding.authPwd.hint = "6자리 이상"
            binding.loginBtn.visibility = View.GONE
            binding.sign.visibility = View.GONE
            binding.signBtn.visibility = View.VISIBLE
        }

        // 회원가입
        binding.signBtn.setOnClickListener {
            val email: String = binding.authEmail.text.toString()
            val pwd: String = binding.authPwd.text.toString()
            auth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(this) { task ->
                binding.authEmail.text.clear()
                binding.authPwd.text.clear()
                if (task.isSuccessful) {    // 회원가입 성공
                    auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { sendTask ->
                        // 인증 메일 발송 성공
                        if (sendTask.isSuccessful) {
                            Toast.makeText(this, "회원가입에 성공하였습니다. 전송된 메일을 확인해 주세요.", Toast.LENGTH_SHORT).show()
                            signToLogin()   // 로그인 화면으로 전환
                        }
                        // 인증 메일 발송 실패
                        else Toast.makeText(this, "인증 메일 발송 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                // 회원가입 실패(비밀번호 6자 이하)
                else Toast.makeText(this, "회원가입 실패. 비밀번호는 최소 6자리 이상 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // x 누르면 동작할 내용
        binding.authXBtn.setOnClickListener {
            back()
        }
    }

    // 뒤로가기
    override fun onBackPressed() {
        back()
    }

    // x버튼, 뒤로가기 선택 시 동작 메소드
    private fun back() {
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun signToLogin(){
        binding.authEmail.requestFocus()
        binding.authEmail.text = null
        binding.authPwd.text = null
        binding.authTitle.text = "로그인"
        binding.authPwd.hint = "영문, 숫자, 특수문자"
        binding.loginBtn.visibility = View.VISIBLE
        binding.sign.visibility = View.VISIBLE
        binding.signBtn.visibility = View.GONE
    }
}