package com.voidcom.v_base.ui

import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference

/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/13 17:40
 * @UpdateDate: 2022/11/13 17:40
 */
abstract class BaseActivityViewModel<A : AppCompatActivity> : BaseViewModel() {
    private lateinit var mActivity: WeakReference<A>
    override fun getModel(): BaseModel? = null

    fun setActivity(activity: A) {
        mActivity = WeakReference(activity)
    }

    fun getActivity(): A? = mActivity.get()
}