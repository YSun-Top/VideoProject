package com.example.libpushvideo

class NativeLib {

    /**
     * A native method that is implemented by the 'libpushvideo' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'libpushvideo' library on application startup.
        init {
            System.loadLibrary("libpushvideo")
        }
    }
}