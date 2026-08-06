package com.example.safetyapp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline wake-word detector using TFLite model bundled in assets.
 * Priority: marvin.tflite -> conv_actions_frozen.tflite
 *
 * Audio pipeline:
 *  • AudioRecord captures 16-bit PCM at 16 kHz mono (the sample rate the model expects)
 *  • A 1-second window (~16 000 samples) is fed to the interpreter on every tick
 *  • The interpreter outputs a softmax vector; label index for "marvin" is dynamically detected
 *  • If confidence >= CONFIDENCE_THRESHOLD the listener callback is fired
 */
public class MarvinDetector {

    public interface OnMarvinDetectedListener {
        void onMarvinDetected();
    }

    private static final String TAG                   = "MarvinDetector";
    private static final String MODEL_FILE            = "marvin.tflite";
    private static final String FALLBACK_MODEL_FILE   = "conv_actions_frozen.tflite";
    private static final String LABELS_FILE          = "conv_actions_labels.txt";
    private static final int    SAMPLE_RATE           = 16_000;
    private static final int    WINDOW_SAMPLES        = 16_000;          // 1-second window
    private static final float  CONFIDENCE_THRESHOLD  = 0.85f;

    private final Context                   context;
    private final OnMarvinDetectedListener  listener;
    private final Handler                   mainHandler = new Handler(Looper.getMainLooper());

    private Interpreter  interpreter;
    private AudioRecord  audioRecord;
    private Thread       recordThread;
    private List<String> labels = new ArrayList<>();
    private int          marvinClassIndex = 2; // Default class index for marvin
    private volatile boolean isListening = false;

    public MarvinDetector(Context context, OnMarvinDetectedListener listener) {
        this.context  = context;
        this.listener = listener;
        loadLabels();
        loadModel();
    }

    // ── Label loading ────────────────────────────────────────────────────────────

    private void loadLabels() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(LABELS_FILE)))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                String label = line.trim().toLowerCase();
                labels.add(label);
                if (label.contains("marvin")) {
                    marvinClassIndex = index;
                    Log.d(TAG, "Found 'marvin' in labels file at index " + index);
                }
                index++;
            }
        } catch (Exception e) {
            Log.d(TAG, "No labels file found or error reading labels, using default index 2");
        }
    }

    // ── Model loading ───────────────────────────────────────────────────────────

    private void loadModel() {
        try {
            MappedByteBuffer model = loadModelFile(MODEL_FILE);
            Interpreter.Options opts = new Interpreter.Options();
            opts.setNumThreads(2);
            interpreter = new Interpreter(model, opts);
            Log.d(TAG, "TFLite model loaded: " + MODEL_FILE);
        } catch (Exception e) {
            Log.w(TAG, "Failed to load " + MODEL_FILE + ", trying fallback model " + FALLBACK_MODEL_FILE);
            try {
                MappedByteBuffer fallbackModel = loadModelFile(FALLBACK_MODEL_FILE);
                Interpreter.Options opts = new Interpreter.Options();
                opts.setNumThreads(2);
                interpreter = new Interpreter(fallbackModel, opts);
                Log.d(TAG, "TFLite fallback model loaded: " + FALLBACK_MODEL_FILE);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to load fallback TFLite model — wake-word detection unavailable", ex);
                interpreter = null;
            }
        }
    }

    private MappedByteBuffer loadModelFile(String fileName) throws Exception {
        AssetFileDescriptor afd = context.getAssets().openFd(fileName);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel fc = fis.getChannel();
        return fc.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    public void startListening() {
        if (isListening) return;
        if (interpreter == null) {
            Log.w(TAG, "No TFLite interpreter — cannot start wake-word detection");
            return;
        }
        isListening = true;
        startAudioRecord();
        Log.d(TAG, "Listening for 'Marvin' (offline TFLite)");
    }

    public void stopListening() {
        isListening = false;
        stopAudioRecord();
    }

    public void release() {
        stopListening();
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    // ── Audio capture & inference ───────────────────────────────────────────────

    private void startAudioRecord() {
        int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize");
                audioRecord = null;
                return;
            }

            audioRecord.startRecording();
            final int finalBufferSize = bufferSize;

            recordThread = new Thread(() -> {
                short[] window = new short[WINDOW_SAMPLES];
                int     filled = 0;

                while (isListening) {
                    short[] chunk = new short[finalBufferSize / 2];
                    int read = audioRecord.read(chunk, 0, chunk.length);
                    if (read <= 0) continue;

                    int space = WINDOW_SAMPLES - filled;
                    int copy  = Math.min(read, space);
                    System.arraycopy(chunk, 0, window, filled, copy);
                    filled += copy;

                    if (filled >= WINDOW_SAMPLES) {
                        runInference(window);
                        System.arraycopy(window, WINDOW_SAMPLES / 2,
                                window, 0, WINDOW_SAMPLES / 2);
                        filled = WINDOW_SAMPLES / 2;
                    }
                }
            }, "MarvinRecordThread");
            recordThread.start();

        } catch (SecurityException e) {
            Log.e(TAG, "RECORD_AUDIO permission denied", e);
        }
    }

    private void runInference(short[] samples) {
        if (interpreter == null) return;

        float[] floatSamples = new float[WINDOW_SAMPLES];
        for (int i = 0; i < WINDOW_SAMPLES; i++) {
            floatSamples[i] = samples[i] / 32768f;
        }

        float[][] input  = new float[1][WINDOW_SAMPLES];
        input[0] = floatSamples;

        int[] outShape = interpreter.getOutputTensor(0).shape();
        int numClasses = outShape[outShape.length - 1];
        float[][] output = new float[1][numClasses];

        try {
            interpreter.run(input, output);
        } catch (Exception e) {
            Log.e(TAG, "Inference error", e);
            return;
        }

        int targetIndex = (marvinClassIndex < numClasses) ? marvinClassIndex : 2;
        if (targetIndex < numClasses) {
            float confidence = output[0][targetIndex];
            Log.v(TAG, "Marvin confidence: " + confidence);
            if (confidence >= CONFIDENCE_THRESHOLD) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onMarvinDetected();
                });
            }
        }
    }

    private void stopAudioRecord() {
        if (recordThread != null) {
            recordThread.interrupt();
            recordThread = null;
        }
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord", e);
            }
            audioRecord = null;
        }
    }
}
