"""Fine-tune MobileNetV3-Small on the transit scene dataset (transfer learning)."""
import argparse
from pathlib import Path

import tensorflow as tf

from config import CLASSES, DATA_DIR, IMG_SIZE, MODEL_DIR


def make_datasets(data_dir: Path, batch: int):
    common = dict(
        labels="inferred",
        label_mode="int",
        class_names=CLASSES,
        image_size=(IMG_SIZE, IMG_SIZE),
        batch_size=batch,
    )
    train = tf.keras.utils.image_dataset_from_directory(data_dir / "train", shuffle=True, seed=42, **common)
    val = tf.keras.utils.image_dataset_from_directory(data_dir / "val", shuffle=False, **common)
    autotune = tf.data.AUTOTUNE
    return train.prefetch(autotune), val.prefetch(autotune)


def build_model() -> tf.keras.Model:
    augment = tf.keras.Sequential(
        [
            tf.keras.layers.RandomFlip("horizontal"),
            tf.keras.layers.RandomRotation(0.08),
            tf.keras.layers.RandomBrightness(0.3, value_range=(0, 255)),
            tf.keras.layers.RandomContrast(0.2),
        ],
        name="augment",
    )
    # include_preprocessing=True => model expects raw [0,255] input and rescales internally.
    base = tf.keras.applications.MobileNetV3Small(
        input_shape=(IMG_SIZE, IMG_SIZE, 3),
        include_top=False,
        weights="imagenet",
        include_preprocessing=True,
    )
    base.trainable = False

    inputs = tf.keras.Input((IMG_SIZE, IMG_SIZE, 3))
    x = augment(inputs)
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.2)(x)
    outputs = tf.keras.layers.Dense(len(CLASSES), activation="softmax")(x)

    model = tf.keras.Model(inputs, outputs, name="transit_scene_classifier")
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--epochs", type=int, default=8)
    ap.add_argument("--batch", type=int, default=16)
    ap.add_argument("--data-dir", default=str(DATA_DIR))
    ap.add_argument("--out", default=str(MODEL_DIR))
    args = ap.parse_args()

    train, val = make_datasets(Path(args.data_dir), args.batch)
    model = build_model()
    model.fit(train, validation_data=val, epochs=args.epochs)

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    model.save(out / "scene_classifier.keras")
    print("Saved model ->", out / "scene_classifier.keras")


if __name__ == "__main__":
    main()
