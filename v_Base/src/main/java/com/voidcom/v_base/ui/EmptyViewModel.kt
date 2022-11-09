package com.voidcom.v_base.ui

import android.content.Context

class EmptyViewModel : BaseViewModel() {
    override fun getModel(): BaseModel? = null

    override fun onInit(context: Context) {
    }

    override fun onInitData() {
    }
}