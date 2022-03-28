package com.voidcom.videoproject.model.videoFilter

import com.voidcom.v_base.ui.BaseModel

/**
 * Created by voidcom on 2022/3/27 17:36
 * Description:
 */
class FiltersModel: BaseModel() {

    //vflip is up and down, hflip is left and right
    private val filtersHint = arrayOf(
            "lutyuv='u=128:v=128'",//素描
            "hue='h=60:s=-3'",//鲜明
            "transpose=2",//旋转90
            "drawtext=\"fontsize=100:fontcolor=white:text='hello world':x=(w-text_w)/2:y=(h-text_h)/2\"",//文字
            "drawgrid=w=iw/3:h=ih/3:t=2:c=white@0.5",//九宫格
            "drawbox=x=100:y=100:w=100:h=100:color=red@0.5'",//矩形
            "vflip",//翻转
            "unsharp"//锐化
    )
}