"""Export the trained scene classifier to TFLite (float16 + INT8) and verify it loads.

Both variants keep float32 input/output for simple Android integration. INT8 uses a
representative dataset for weight quantization to shrink the model.
"""
import argparse
import shutil
from pathlib import Path

import numpy as np
import tensorflow as tf

from config import CLASSES, DATA_DIR, EXPORT_DIR, IMG_SIZE, MODEL_DIR, APP_MODELS_DIR


def representative_dataset(data_dir: Path, n: int = 100):
    ds = tf.keras.utils.image_dataset_from_directory(
        data_dir / "train",
        labels="inferred",
        label_mode="int",
        class_names=CLASSES,
        image_size=(IMG_SIZE, IMG_SIZE),
        batch_size=1,
        shuffle=True,
    )

    def gen():
        for i, (x, _) in enumerate(ds):
            if i >= n:
                break
            yield [tf.cast(x, tf.float32)]

    return gen


def convert_fp16(model: tf.keras.Model) -> bytes:
    c = tf.lite.TFLiteConverter.from_keras_model(model)
    c.optimizations = [tf.lite.Optimize.DEFAULT]
    c.target_spec.supported_types = [tf.float16]
    return c.convert()


def convert_int8(model: tf.keras.Model, data_dir: Path) -> bytes:
    c = tf.lite.TFLiteConverter.from_keras_model(model)
    c.optimizations = [tf.lite.Optimize.DEFAULT]
    c.representative_dataset = representative_dataset(data_dir)
    return c.convert()


def verify(tflite_bytes: bytes) -> str:
    # Disable the default XNNPACK delegate: it fails to prepare some INT8 graphs on
    # macOS/arm64 desktop. Android supplies its own delegates (GPU/NNAPI) at runtime.
    interp = tf.lite.Interpreter(
        model_content=tflite_bytes,
        experimental_op_resolver_type=tf.lite.experimental.OpResolverType.BUILTIN_WITHOUT_DEFAULT_DELEGATES,
    )
    interp.allocate_tensors()
    inp = interp.get_input_details()[0]
    out = interp.get_output_details()[0]
    x = (np.random.rand(*inp["shape"]) * 255).astype(inp["dtype"])
    interp.set_tensor(inp["index"], x)
    interp.invoke()
    y = interp.get_tensor(out["index"])
    return f"in={inp['shape'].tolist()}{inp['dtype'].__name__} out={y.shape.__str__()}"


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--model", default=str(MODEL_DIR / "scene_classifier.keras"))
    ap.add_argument("--data-dir", default=str(DATA_DIR))
    ap.add_argument("--out", default=str(EXPORT_DIR))
    ap.add_argument("--copy-to-app", action="store_true", help="also copy .tflite into app assets")
    args = ap.parse_args()

    model = tf.keras.models.load_model(args.model)
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    variants = {
        "scene_classifier_fp16.tflite": convert_fp16(model),
        "scene_classifier_int8.tflite": convert_int8(model, Path(args.data_dir)),
    }
    for name, blob in variants.items():
        path = out / name
        path.write_bytes(blob)
        print(f"{name}: {len(blob) / 1024:.0f} KB | {verify(blob)}")
        if args.copy_to_app:
            APP_MODELS_DIR.mkdir(parents=True, exist_ok=True)
            shutil.copy(path, APP_MODELS_DIR / name)

    if args.copy_to_app:
        print("Copied .tflite models ->", APP_MODELS_DIR)


if __name__ == "__main__":
    main()
