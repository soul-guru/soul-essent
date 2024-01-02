"""
Model Initialization Script

This script initializes models from the Hugging Face Transformers library using the preload_models configuration.

Usage:
    python initialize_models.py

Author: Vladislav Maslow
"""

from transformers import pipeline

from preload_models import preload_models


def initialize_models():
    """
    Initialize models from the Hugging Face Transformers library using the preload_models configuration.

    Returns:
        None
    """
    for task, model in preload_models:
        print(f"Initializing model for task: {task}, Model: {model}")
        # Initialize model using Hugging Face pipeline
        _ = pipeline(task, model=model)


if __name__ == "__main__":
    initialize_models()
