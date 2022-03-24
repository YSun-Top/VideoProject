package com.voidcom.v_base.utils.audioplayer;

/**
 * 添加人：  何统
 * 添加时间：2018/5/9 17:58
 * <p>
 * 修改人：  何统
 * 修改时间：2018/5/9 17:58
 * <p>
 * 功能描述：
 */
public interface PlayerCallbacks {
    void onPlaybackCompleted();

    void onMediaError(Exception exp);

    boolean onMediaReady();
}
