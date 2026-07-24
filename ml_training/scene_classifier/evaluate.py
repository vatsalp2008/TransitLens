"""Evaluate the trained scene classifier: accuracy, per-class report, confusion matrix."""
import argparse
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
import tensorflow as tf  # noqa: E402
from sklearn.metrics import classification_report, confusion_matrix  # noqa: E402

from config import CLASSES, DATA_DIR, IMG_SIZE, MODEL_DIR  # noqa: E402


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--model", default=str(MODEL_DIR / "scene_classifier.keras"))
    ap.add_argument("--data-dir", default=str(DATA_DIR))
    ap.add_argument("--out", default=str(MODEL_DIR))
    args = ap.parse_args()

    test = tf.keras.utils.image_dataset_from_directory(
        Path(args.data_dir) / "test",
        labels="inferred",
        label_mode="int",
        class_names=CLASSES,
        image_size=(IMG_SIZE, IMG_SIZE),
        batch_size=16,
        shuffle=False,
    )
    model = tf.keras.models.load_model(args.model)

    _, acc = model.evaluate(test)
    print(f"\nTest accuracy: {acc:.4f}\n")

    y_true = np.concatenate([y.numpy() for _, y in test])
    y_pred = model.predict(test).argmax(axis=1)
    print(classification_report(y_true, y_pred, target_names=CLASSES))

    cm = confusion_matrix(y_true, y_pred, labels=range(len(CLASSES)))
    fig, ax = plt.subplots(figsize=(6, 6))
    ax.imshow(cm, cmap="Blues")
    ax.set_xticks(range(len(CLASSES)), CLASSES, rotation=45, ha="right")
    ax.set_yticks(range(len(CLASSES)), CLASSES)
    for i in range(len(CLASSES)):
        for j in range(len(CLASSES)):
            ax.text(j, i, str(cm[i, j]), ha="center", va="center")
    ax.set_xlabel("predicted")
    ax.set_ylabel("true")
    fig.tight_layout()

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    fig.savefig(out / "confusion_matrix.png", dpi=120)
    print("Saved confusion matrix ->", out / "confusion_matrix.png")


if __name__ == "__main__":
    main()
