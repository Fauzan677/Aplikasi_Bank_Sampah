package com.gemahripah.banksampah.ui.nasabah

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gemahripah.banksampah.data.model.pengguna.Pengguna

class NasabahViewModel : ViewModel() {

    private val _pengguna = MutableLiveData<Pengguna?>()
    val pengguna: LiveData<Pengguna?> = _pengguna

    fun setPengguna(pengguna: Pengguna?) {
        _pengguna.value = pengguna
    }
}