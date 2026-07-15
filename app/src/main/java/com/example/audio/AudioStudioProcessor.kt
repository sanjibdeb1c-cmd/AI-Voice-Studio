package com.example.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

object AudioStudioProcessor {
    private const val TAG = "AudioStudioProcessor"

    // Configuration
    const val SAMPLE_RATE = 44100
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val BUFFER_SIZE_FACTOR = 2

    /**
     * Writes standard 44-byte RIFF/WAV header to raw PCM file.
     */
    fun rawPcmToWav(pcmFile: File, wavFile: File) {
        val sampleRate = SAMPLE_RATE
        val channels = 1
        val bitsPerSample = 16

        val pcmLength = pcmFile.length()
        val totalDataLen = pcmLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        
        // Size of the overall file minus 8 bytes
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()

        header[8] = 'W'.code.toByte() // WAVE
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte() // fmt
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        header[16] = 16 // Size of fmt chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0

        header[20] = 1 // Format: 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0

        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()

        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()

        header[32] = (channels * bitsPerSample / 8).toByte() // Block align
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0

        header[36] = 'd'.code.toByte() // data
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        header[40] = (pcmLength and 0xff).toByte()
        header[41] = ((pcmLength shr 8) and 0xff).toByte()
        header[42] = ((pcmLength shr 16) and 0xff).toByte()
        header[43] = ((pcmLength shr 24) and 0xff).toByte()

        FileInputStream(pcmFile).use { pcmIn ->
            FileOutputStream(wavFile).use { wavOut ->
                wavOut.write(header)
                val buffer = ByteArray(4096)
                var read: Int
                while (pcmIn.read(buffer).also { read = it } != -1) {
                    wavOut.write(buffer, 0, read)
                }
            }
        }
    }

    /**
     * Enhanced Audio Processor: Apply Noise Gate, Volume Normalization, and Bandpass Filters.
     */
    fun processAudio(
        inputFile: File,
        outputFile: File,
        applyNoiseGate: Boolean = false,
        applySilenceRemoval: Boolean = false,
        applyHighPassFilter: Boolean = false,
        applyLowPassFilter: Boolean = false,
        applyNormalization: Boolean = false
    ): Boolean {
        if (!inputFile.exists()) {
            Log.e(TAG, "Input file does not exist: ${inputFile.absolutePath}")
            return false
        }

        try {
            // Read all PCM samples from the WAV (skip 44 bytes header)
            val pcmBytes = inputFile.readBytes()
            if (pcmBytes.size <= 44) {
                Log.e(TAG, "File too small or invalid WAV format")
                return false
            }

            // Convert to 16-bit Short Array
            val sampleCount = (pcmBytes.size - 44) / 2
            val samples = ShortArray(sampleCount)
            ByteBuffer.wrap(pcmBytes, 44, pcmBytes.size - 44)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .get(samples)

            var processedSamples = samples

            // 1. High Pass Filter (removes hum/rumble below 100Hz)
            if (applyHighPassFilter) {
                processedSamples = applyHighPass(processedSamples, RC = 0.0016f) // approx 100Hz cutoff
            }

            // 2. Low Pass Filter (removes hiss above 7000Hz)
            if (applyLowPassFilter) {
                processedSamples = applyLowPass(processedSamples, RC = 0.000022f) // approx 7kHz cutoff
            }

            // 3. Noise Gate & Silence Removal
            if (applyNoiseGate || applySilenceRemoval) {
                processedSamples = applyNoiseGateAndGaps(
                    processedSamples,
                    threshold = 300, // Amplitude threshold for noise
                    removeCompletely = applySilenceRemoval
                )
            }

            // 4. Volume Normalization
            if (applyNormalization) {
                processedSamples = applyNormalization(processedSamples)
            }

            // Write processed short array back to a temporary PCM file
            val tempPcmFile = File(inputFile.parent, "temp_processed.pcm")
            val outputBuffer = ByteBuffer.allocate(processedSamples.size * 2)
                .order(ByteOrder.LITTLE_ENDIAN)
            for (sample in processedSamples) {
                outputBuffer.putShort(sample)
            }

            tempPcmFile.writeBytes(outputBuffer.array())

            // Convert PCM back to proper WAV
            rawPcmToWav(tempPcmFile, outputFile)
            tempPcmFile.delete()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio: ${e.message}", e)
            return false
        }
    }

    private fun applyHighPass(samples: ShortArray, RC: Float): ShortArray {
        val dt = 1.0f / SAMPLE_RATE
        val alpha = RC / (RC + dt)
        val output = ShortArray(samples.size)
        if (samples.isEmpty()) return output

        output[0] = samples[0]
        for (i in 1 until samples.size) {
            val processedVal = alpha * (output[i - 1] + samples[i] - samples[i - 1])
            output[i] = processedVal.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
        }
        return output
    }

    private fun applyLowPass(samples: ShortArray, RC: Float): ShortArray {
        val dt = 1.0f / SAMPLE_RATE
        val alpha = dt / (RC + dt)
        val output = ShortArray(samples.size)
        if (samples.isEmpty()) return output

        output[0] = samples[0]
        for (i in 1 until samples.size) {
            val processedVal = output[i - 1] + alpha * (samples[i] - output[i - 1])
            output[i] = processedVal.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
        }
        return output
    }

    private fun applyNoiseGateAndGaps(samples: ShortArray, threshold: Short, removeCompletely: Boolean): ShortArray {
        val outputList = mutableListOf<Short>()
        val chunkLength = SAMPLE_RATE / 50 // 20ms chunk analysis window
        
        var i = 0
        while (i < samples.size) {
            val end = (i + chunkLength).coerceAtMost(samples.size)
            var maxVal = 0
            for (j in i until end) {
                val absVal = abs(samples[j].toInt())
                if (absVal > maxVal) maxVal = absVal
            }

            val isSilence = maxVal < threshold
            if (isSilence) {
                if (!removeCompletely) {
                    // Suppress noise to complete silence (Noise Gate)
                    for (j in i until end) {
                        outputList.add(0)
                    }
                }
                // If removeCompletely, we skip adding these frames entirely!
            } else {
                for (j in i until end) {
                    outputList.add(samples[j])
                }
            }
            i = end
        }
        return outputList.toShortArray()
    }

    private fun applyNormalization(samples: ShortArray): ShortArray {
        var maxSample = 0
        for (sample in samples) {
            val absSample = abs(sample.toInt())
            if (absSample > maxSample) {
                maxSample = absSample
            }
        }

        if (maxSample == 0) return samples

        val targetMax = 31000 // Close to Short.MAX_VALUE (32767) but leaving headroom
        val gain = targetMax.toFloat() / maxSample.toFloat()

        val output = ShortArray(samples.size)
        for (i in samples.indices) {
            val normSample = samples[i] * gain
            output[i] = normSample.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
        }
        return output
    }
}
