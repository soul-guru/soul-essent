"""
Preload Models Configuration

This module defines a list of preloaded models used in the FastAPI application for text transformations.

Preloaded Models:
- summarization: Falconsai/text_summarization
- text-classification: mohameddhiab/humor-no-humor
- text-classification: Falconsai/offensive_speech_detection
- text-classification: helinivan/english-sarcasm-detector
- sentiment-analysis: michellejieli/emotion_text_classifier

Usage:
    from preload_models import preload_models

    for task, model in preload_models:
        # Access task and model information
        print(f"Task: {task}, Model: {model}")

Author: Vladislav Maslow
"""

import os

# List of preloaded models
preload_models = [
    ("summarization", "Falconsai/text_summarization"),
    ("text-classification", "mohameddhiab/humor-no-humor"),
    ("text-classification", "SamLowe/roberta-base-go_emotions"),
    ("text-classification", "bhadresh-savani/distilbert-base-uncased-emotion"),
    ("text-classification", "Falconsai/offensive_speech_detection"),
    ("text-classification", "helinivan/english-sarcasm-detector"),
    ("sentiment-analysis", "michellejieli/emotion_text_classifier"),
]
