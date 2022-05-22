package com.voidcom.videoproject.ui.videoFilter

import android.widget.RadioGroup
import com.voidcom.v_base.ui.BaseDefaultFragment
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.log.LogUtils
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.FragmentFiltersBinding
import com.voidcom.videoproject.viewModel.videoFilter.FiltersViewModel

/**
 * Created by voidcom on 2022/3/28 16:30
 * Description:
 * 滤镜Fragment
 */
class FiltersFragment : BaseDefaultFragment<FragmentFiltersBinding, FiltersViewModel>(),
    RadioGroup.OnCheckedChangeListener {
    override val mViewModel: FiltersViewModel by lazy { FiltersViewModel() }

    override fun onInitUI() {
        mBinding.filterView.setOnCheckedChangeListener(this)
    }

    override fun onInitListener() {
    }

    override fun onInitData() {
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            R.id.filter_1 -> {
                LogUtils.d(AppCode.log_videoProcess, "素描")
            }
            R.id.filter_2 -> {
                LogUtils.d(AppCode.log_videoProcess, "鲜明")
            }
            R.id.filter_3 -> {
                LogUtils.d(AppCode.log_videoProcess, "旋转90")
            }
            R.id.filter_4 -> {
                LogUtils.d(AppCode.log_videoProcess, "九宫格")
            }
            R.id.filter_5 -> {
                LogUtils.d(AppCode.log_videoProcess, "矩形")
            }
            R.id.filter_6 -> {
                LogUtils.d(AppCode.log_videoProcess, "翻转")
            }
            R.id.filter_7 -> {
                LogUtils.d(AppCode.log_videoProcess, "锐化")
            }
        }
    }
}