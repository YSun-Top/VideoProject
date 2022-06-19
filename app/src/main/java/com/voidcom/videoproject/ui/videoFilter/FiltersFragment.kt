package com.voidcom.videoproject.ui.videoFilter

import android.view.View
import com.voidcom.v_base.ui.BaseDefaultFragment
import com.voidcom.v_base.utils.AppCode
import com.voidcom.v_base.utils.KLog
import com.voidcom.videoproject.R
import com.voidcom.videoproject.databinding.FragmentFiltersBinding
import com.voidcom.videoproject.model.videoFilter.PlayVideoHandler
import com.voidcom.videoproject.viewModel.videoFilter.FiltersViewModel

/**
 * Created by voidcom on 2022/3/28 16:30
 * Description:
 * 滤镜Fragment
 */
class FiltersFragment : BaseDefaultFragment<FragmentFiltersBinding, FiltersViewModel>(),
    View.OnClickListener {
    lateinit var playHandler: PlayVideoHandler
    override val mViewModel: FiltersViewModel by lazy { FiltersViewModel() }

    override fun onInitUI() {
    }

    override fun onInitListener() {
        var v: View
        for (i in 0 until mBinding.filterView.childCount) {
            v = mBinding.filterView.getChildAt(i)
            if (v is androidx.appcompat.widget.AppCompatRadioButton) {
                v.setOnClickListener(this)
            }
        }
    }

    override fun onInitData() {
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.filter_1 -> {
                playHandler.setFilterValue("lutyuv='u=128:v=128'")
                KLog.d(AppCode.log_videoProcess, "素描")
            }
            R.id.filter_2 -> {
                playHandler.setFilterValue("hue='h=60:s=-3'")
                KLog.d(AppCode.log_videoProcess, "鲜明")
            }
            R.id.filter_3 -> {
                playHandler.setFilterValue("transpose=2")
                KLog.d(AppCode.log_videoProcess, "旋转90")
            }
            R.id.filter_4 -> {
                playHandler.setFilterValue("drawgrid=w=iw/3:h=ih/3:t=2:c=white@0.5")
                KLog.d(AppCode.log_videoProcess, "九宫格")
            }
            R.id.filter_5 -> {
                playHandler.setFilterValue("drawbox=x=100:y=100:w=100:h=100:color=red@0.5'")
                KLog.d(AppCode.log_videoProcess, "矩形")
            }
            R.id.filter_6 -> {
                playHandler.setFilterValue("vflip")
                KLog.d(AppCode.log_videoProcess, "翻转")
            }
            R.id.filter_7 -> {
                playHandler.setFilterValue("unsharp")
                KLog.d(AppCode.log_videoProcess, "锐化")
            }
            R.id.filter_8->{
                playHandler.setFilterValue("drawtext=\"fontsize=100:fontcolor=white:text='hello world':x=(w-text_w)/2:y=(h-text_h)/2\"")
                KLog.d(AppCode.log_videoProcess, "文字")
            }
        }
    }
}