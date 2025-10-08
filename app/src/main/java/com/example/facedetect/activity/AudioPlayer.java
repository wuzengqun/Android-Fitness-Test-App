package com.example.facedetect.activity;

import android.content.Context;
import android.media.MediaPlayer;

public class AudioPlayer {
    private MediaPlayer mediaPlayer;
    private Context context;

    public AudioPlayer(Context context) {
        this.context = context;
        mediaPlayer = new MediaPlayer();
    }

    public void playSound(int resId) {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer = MediaPlayer.create(context, resId);
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
