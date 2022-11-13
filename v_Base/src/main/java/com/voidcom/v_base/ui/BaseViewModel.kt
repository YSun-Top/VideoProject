package com.voidcom.v_base.ui

import android.content.Context
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {

    abstract fun getModel(): BaseModel?

    abstract fun onInit(context: Context)

    abstract fun onInitData()

}