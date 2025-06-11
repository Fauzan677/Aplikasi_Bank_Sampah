package com.gemahripah.banksampah.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gemahripah.banksampah.data.model.pengguna.Pengguna

class AdminViewModel : ViewModel() {
    private val _pengguna = MutableLiveData<Pengguna>()
    val pengguna: LiveData<Pengguna> get() = _pengguna

    fun setPengguna(data: Pengguna) {
        _pengguna.value = data
    }
}