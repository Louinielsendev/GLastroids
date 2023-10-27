package com.example.glastroids

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.io.IOException

const val MAX_STREAMS = 3
const val DEFAULT_SFX_VOLUME = 1
const val DEFAULT_MUSIC_VOLUME = 0.8f

private const val SOUNDS_PREF_KEY = "sounds_pref_key"
private const val MUSIC_PREF_KEY = "music_pref_key"
class Jukebox(private val engine: Game) {

    private val TAG = "Jukebox"
    private var mSoundPool: SoundPool? = null
    private var mBgPlayer: MediaPlayer? = null
    private val mSoundsMap = HashMap<GameEvent, Int>()
    private var mSoundEnabled: Boolean
    private var mMusicEnabled: Boolean

    init {
        engine.getActivity().volumeControlStream = AudioManager.STREAM_MUSIC
        val prefs = engine.getPreferences()
        mSoundEnabled = prefs.getBoolean(SOUNDS_PREF_KEY, true)
        mMusicEnabled = prefs.getBoolean(MUSIC_PREF_KEY, true)
        loadIfNeeded()
    }

    fun toggleSoundStatus() {
        mSoundEnabled = !mSoundEnabled
        if (mSoundEnabled) {
            loadSounds()
        } else {
            unloadSounds()
        }
        engine.savePreference(SOUNDS_PREF_KEY, mSoundEnabled)
    }

    fun toggleMusicStatus() {
        mMusicEnabled = !mMusicEnabled
        if (mMusicEnabled) {
            loadMusic()
        } else {
            unloadMusic()
        }
        engine.savePreference(MUSIC_PREF_KEY, mSoundEnabled)
    }
    private fun loadIfNeeded() {
        if (mSoundEnabled) {
            loadSounds()
        }
        if (mMusicEnabled) {
            loadMusic()
        }
    }

    private fun loadSounds() {
        val attr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        mSoundPool = SoundPool.Builder()
            .setAudioAttributes(attr)
            .setMaxStreams(MAX_STREAMS)
            .build()

        mSoundsMap.clear()
        loadEventSound(GameEvent.AsteroidExplode, "sfx/asteroidexplode.wav")
        loadEventSound(GameEvent.Bullet, "sfx/bullet.wav")
        loadEventSound(GameEvent.Thrust, "sfx/thrust.wav")
        ///[... and so on...]
    }

    private fun loadEventSound(event: GameEvent, fileName: String) {
        try {
            val afd = engine.getAssets().openFd(fileName)
            val soundId = mSoundPool!!.load(afd, 1)
            mSoundsMap[event] = soundId
        } catch (e: IOException) {
            Log.e(TAG, "Error loading sound $e")
        }
    }

    private fun unloadSounds() {
        if (mSoundPool == null) {
            return
        }
        mSoundPool!!.release()
        mSoundPool = null
        mSoundsMap.clear()
    }

    fun playEventSound(event: GameEvent) {
        if (!mSoundEnabled) {
            return
        }

        val leftVolume = DEFAULT_SFX_VOLUME
        val rightVolume = DEFAULT_SFX_VOLUME
        val priority = 1
        val loop = 0 //-1 loop forever, 0 play once
        val rate = 1.0f
        val soundID = mSoundsMap[event]
        if(soundID == null){
            Log.e(TAG, "Attempting to play non-existent event sound: {event}")
            return
        }
        if (soundID > 0) { //if soundID is 0, the file failed to load. Make sure you catch this in the loading routine.
            mSoundPool!!.play(soundID, leftVolume.toFloat(),
                rightVolume.toFloat(), priority, loop, rate)
        }
    }

    private fun loadMusic() {
        try {
            mBgPlayer = MediaPlayer()
            val afd = engine.getAssets().openFd("bgm/bgm.mp3")
            mBgPlayer!!.setDataSource(
                afd.fileDescriptor,
                afd.startOffset,
                afd.length
            )
            mBgPlayer!!.isLooping = true

            mBgPlayer!!.setVolume(DEFAULT_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME)
            mBgPlayer!!.prepare()
        } catch (e: IOException) {
            Log.e(TAG, "Unable to create MediaPlayer.", e)
        }
    }

    fun pauseBgMusic() {
        if (!mMusicEnabled) {
            return
        }
        mBgPlayer!!.pause()
    }

    fun resumeBgMusic() {
        if (!mMusicEnabled) {
            return
        }
        mBgPlayer!!.start()
    }

    private fun unloadMusic() {
        if (mBgPlayer == null) {
            return
        }
        mBgPlayer!!.stop()
        mBgPlayer!!.release()
    }
}