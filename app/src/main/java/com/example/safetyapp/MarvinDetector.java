package com.example.safetyapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

public class MarvinDetector {

    public interface OnMarvinDetectedListener {
        void onMarvinDetected();
    }

    private SpeechRecognizer speechRecognizer;
    private OnMarvinDetectedListener listener;
    private Context context;
    private boolean isListening = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    public MarvinDetector(Context context, OnMarvinDetectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void startListening() {
        if (isListening) return;
        isListening = true;
        createAndStart();
    }

    private void createAndStart() {
        if (!isListening) return;

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onResults(Bundle results) {
                checkForMarvin(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                scheduleRestart(2000);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                checkForMarvin(partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
            }

            @Override
            public void onError(int error) {
                scheduleRestart(2000);
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        speechRecognizer.startListening(intent);
    }

    private void checkForMarvin(ArrayList<String> matches) {
        if (matches == null) return;
        for (String match : matches) {
            if (match.toLowerCase().contains("marvin")) {
                if (listener != null) {
                    listener.onMarvinDetected();
                }
                break;
            }
        }
    }

    private void scheduleRestart(long delayMs) {
        handler.postDelayed(() -> {
            if (isListening) {
                createAndStart();
            }
        }, delayMs);
    }

    public void stopListening() {
        isListening = false;
        handler.removeCallbacksAndMessages(null);
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    public void release() {
        stopListening();
    }
}
