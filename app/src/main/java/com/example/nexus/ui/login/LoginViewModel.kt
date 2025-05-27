package com.example.nexus.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel(){
    private val auth = FirebaseAuth.getInstance()

    fun signIn (email :String, password: String, onResult: (Boolean,String?)->Unit){
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email,password).await()
                onResult(true,null)
            }
            catch (e: Exception){
                onResult(false,e.message)
            }
        }
    }
    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}