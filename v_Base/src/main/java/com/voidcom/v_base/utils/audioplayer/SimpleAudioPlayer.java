package com.voidcom.v_base.utils.audioplayer;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Locale;

/**
 * 简易音频播放器
 */
public class SimpleAudioPlayer {
    private PlayerCallbacks callbacks;
    private MediaPlayer mediaPlayer;

    //播放数据是否准备完成
    private boolean isSourceReady;

    public SimpleAudioPlayer(final PlayerCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void prepare(FileDescriptor fileDescriptor, long offset, long length, boolean loop) {
        reset();
        try {
            this.mediaPlayer.setLooping(loop);
            this.mediaPlayer.setDataSource(fileDescriptor, offset, length);
            this.mediaPlayer.prepareAsync();
        } catch (IOException exp) {
            callbacks.onMediaError(exp);
        }
    }

    public void prepare(@NonNull AssetFileDescriptor fileDescriptor, boolean loop) {
        prepare(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength(), loop);
    }

    public void prepare(String audioURL, boolean loop) {
        reset();
        try {
            this.mediaPlayer.setLooping(loop);
            this.mediaPlayer.setDataSource(audioURL);
            this.mediaPlayer.prepareAsync();
        } catch (IOException exp) {
            callbacks.onMediaError(exp);
        }
    }

    public boolean isSourceReady() {
        return isSourceReady;
    }

    public boolean isPlaying() {
        return this.mediaPlayer.isPlaying();
    }

    public long getPosition() {
        try {
            return this.mediaPlayer.getCurrentPosition();
        } catch (Exception ignore) {
            return 0L;
        }
    }

    public long getDuration() {
        try {
            return this.mediaPlayer.getDuration();
        } catch (Exception ignore) {
            return 0L;
        }
    }

    public void start() {
        try {
            this.mediaPlayer.start();
        } catch (Exception ignore) {
        }
    }

    public void pause() {
        try {
            this.mediaPlayer.pause();
        } catch (Exception ignore) {
        }
    }

    public void stop() {
        try {
            this.mediaPlayer.stop();
        } catch (Exception ignore) {
        }
    }

    public void seek(int position) {
        try {
            this.mediaPlayer.seekTo(position);
        } catch (Exception ignore) {
        }
    }

    public void reset() {
        if (this.mediaPlayer == null) {
            this.mediaPlayer = new MediaPlayer();
            this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            this.mediaPlayer.setOnCompletionListener(mp -> callbacks.onPlaybackCompleted());

            this.mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isSourceReady = false;
                String msg;
                switch (extra) {
                    case MediaPlayer.MEDIA_ERROR_IO:
                        msg = "网络异常/没有文件读写权限";
                        break;
                    case MediaPlayer.MEDIA_ERROR_MALFORMED:
                        msg = "比特流不符合相关的编码标准或文件规范";
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                        msg = "比特流符合相关的编码标准或文件规范，但媒体框架不支持该功能";
                        break;
                    case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                        msg = "操作超时";
                        break;
                    default:
                        msg = String.format(Locale.CHINA, "未知错误(%d,%d)", what, extra);
                }
                callbacks.onMediaError(new Exception(msg));
                return false;
            });

            this.mediaPlayer.setOnPreparedListener(mp -> {
                isSourceReady = true;
                if (callbacks.onMediaReady()) {
                    mp.start();
                }
            });
        }

        this.isSourceReady = false;

        try {
            this.mediaPlayer.reset();
        } catch (Exception ignore) {
        }
    }

    public void release() {
        this.isSourceReady = false;
        try {
            this.mediaPlayer.release();
        } catch (Exception ignore) {
        }
        this.mediaPlayer = null;
    }
}
