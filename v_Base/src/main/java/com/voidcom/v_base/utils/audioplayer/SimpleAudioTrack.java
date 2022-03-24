package com.voidcom.v_base.utils.audioplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.voidcom.v_base.utils.KLog;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Void on 2020/5/29 09:56
 * 用于播放音频，如果是完整的音频文件，直接使用MediaPlayer播放即可。
 * 这个类用于播放纯PCM音频数据
 */
public class SimpleAudioTrack {
    private AudioTrack mAudioTrack;
    //采样率
    private int mFrequency;
    //声道
    private int mChannel;
    //采样精度
    private int mSampBit;
    // 获得构建对象的最小缓冲区大小
    private int minBufSize;
    private boolean isPlayAudio = false;

    public SimpleAudioTrack() {
        this(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }

    public SimpleAudioTrack(int mFrequency, int mChannel, int mSampBit) {
        this.mFrequency = mFrequency;
        this.mChannel = mChannel;
        this.mSampBit = mSampBit;
    }

    public void init() {
        if (mAudioTrack != null) release();
        minBufSize = AudioTrack.getMinBufferSize(mFrequency, mChannel, mSampBit);
        /*
         * AudioManager.STREAM_ALARM：警告声
         * AudioManager.STREAM_MUSCI：音乐声，例如music等
         * AudioManager.STREAM_RING：铃声
         * AudioManager.STREAM_SYSTEM：系统声音
         * AudioManager.STREAM_VOCIE_CALL：电话声音
         * AudioTrack中有MODE_STATIC和MODE_STREAM两种分类。
         * STREAM的意思是由用户在应用程序通过write方式把数据一次一次得写到audiotrack中。
         * 这个和我们在socket中发送数据一样，应用层从某个地方获取数据，例如通过编解码得到PCM数据，然后write到audiotrack。
         * 这种方式的坏处就是总是在JAVA层和Native层交互，效率损失较大。
         * 而STATIC的意思是一开始创建的时候，就把音频数据放到一个固定的buffer，然后直接传给audiotrack，
         * 后续就不用一次次得write了。AudioTrack会自己播放这个buffer中的数据。
         * 这种方法对于铃声等内存占用较小，延时要求较高的声音来说很适用。
         * */
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                mFrequency,
                mChannel,
                mSampBit,
                minBufSize,
                AudioTrack.MODE_STREAM);
    }

    public void release() {
        if (!isPlayAudio) {
            KLog.d("录音已经停止");
            return;
        }
        isPlayAudio = false;
        if (mAudioTrack == null) return;
        try {
            mAudioTrack.stop();
            mAudioTrack.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放音频
     *
     * @param file 音频文件
     */
    public void playAudio(File file) {
        if (isPlayAudio) {
            KLog.d("请先暂停播放");
            return;
        }
        if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) return;
        if (file == null || !file.exists())
            throw new NullPointerException("file==null or file not exists");
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (dis == null) throw new NullPointerException("DataInputStream == null  !!");
        byte[] data = new byte[minBufSize];
        mAudioTrack.play();
        isPlayAudio = true;
        while (isPlayAudio) {
            int i = 0;
            try {
                while (dis.available() > 0 && i < data.length) {
                    data[i] = dis.readByte();//录音时write Byte 那么读取时就该为readByte要相互对应
                    i++;
                }
                playAudioTrack(data, 0, data.length);
                if (i != minBufSize) {//表示读取完了
                    release();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void playAudioTrack(byte[] data, int offset, int length) {
        if (data == null || data.length == 0) return;
        try {
            mAudioTrack.write(data, offset, length);
        } catch (Exception e) {
            Log.i("SimpleAudioTrack", "catch exception...");
        }
    }

    public int getPrimePlaySize() {
        return AudioTrack.getMinBufferSize(mFrequency, mChannel, mSampBit) * 2;
    }
}
