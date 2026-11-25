package io.github.mcello23.onda.playback

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import io.github.mcello23.onda.data.AppSettings
import io.github.mcello23.onda.data.AppSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioFxInfo(
    val supported: Boolean = false,
    val bandFrequenciesHz: List<Int> = listOf(60, 230, 910, 3600, 14000),
    val minLevelMb: Int = -1200,
    val maxLevelMb: Int = 1200,
)

object AudioFxState {
    private val _info = MutableStateFlow(AudioFxInfo())
    val info: StateFlow<AudioFxInfo> = _info.asStateFlow()

    internal fun update(info: AudioFxInfo) {
        _info.value = info
    }
}

class AudioFxManager {
    private var attachedSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var loudness: LoudnessEnhancer? = null

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == attachedSessionId) {
            applySettings(AppSettingsStore.current)
            return
        }
        release()
        attachedSessionId = audioSessionId
        runCatching {
            equalizer = Equalizer(0, audioSessionId).also { eq ->
                val count = eq.numberOfBands.toInt().coerceAtLeast(1)
                val freqs = (0 until count).map { (eq.getCenterFreq(it.toShort()) / 1000) }
                val range = eq.bandLevelRange
                AudioFxState.update(
                    AudioFxInfo(
                        supported = true,
                        bandFrequenciesHz = freqs,
                        minLevelMb = range.getOrNull(0)?.toInt() ?: -1200,
                        maxLevelMb = range.getOrNull(1)?.toInt() ?: 1200,
                    )
                )
            }
        }
        runCatching {
            loudness = LoudnessEnhancer(audioSessionId)
        }
        applySettings(AppSettingsStore.current)
    }

    fun applySettings(settings: AppSettings) {
        runCatching {
            loudness?.let { l ->
                l.setTargetGain(if (settings.normalizeVolume) 450 else 0)
                l.enabled = settings.normalizeVolume
            }
        }
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = settings.eqEnabled
                val range = eq.bandLevelRange
                val minLv = range.getOrNull(0)?.toInt() ?: -1200
                val maxLv = range.getOrNull(1)?.toInt() ?: 1200
                val bands = eq.numberOfBands.toInt()
                val levels = settings.eqBandLevels
                for (i in 0 until bands) {
                    val srcIdx = if (levels.isEmpty()) 0 else (i * levels.size / bands).coerceIn(0, levels.lastIndex)
                    val target = levels.getOrElse(srcIdx) { 0 }.coerceIn(minLv, maxLv)
                    eq.setBandLevel(i.toShort(), target.toShort())
                }
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        loudness = null
        attachedSessionId = 0
    }
}
