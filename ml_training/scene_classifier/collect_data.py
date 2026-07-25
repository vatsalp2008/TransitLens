"""Build the scene-classifier dataset.

Modes:
  smoke       (default) synthetic images to validate the pipeline end-to-end (no downloads).
  places365   real Places365 images for all 6 classes (~500 MB streamed) — see datasets_places365.py.
  openimages  real Open Images v7 images via FiftyOne — see datasets_openimages.py.
  real        places365 + openimages combined.

Layout produced: data/scene/{train,val,test}/{class}/*.{png,jpg}
Real modes are trained on a GPU (Colab); see ml_training/README.md.
"""
import argparse

import numpy as np
from PIL import Image, ImageDraw

from config import CLASSES, DATA_DIR, IMG_SIZE

# Distinct base color per class so a real model can learn to separate them.
PALETTE = {
    "bus_stop": (30, 90, 160),
    "train_platform": (110, 110, 122),
    "street_corner": (200, 180, 40),
    "vehicle_interior": (38, 44, 54),
    "transfer_hub": (40, 140, 92),
    "unknown": (128, 128, 128),
}


def _draw_sample(cls: str, rng: np.random.Generator) -> Image.Image:
    img = Image.new("RGB", (IMG_SIZE, IMG_SIZE), PALETTE[cls])
    d = ImageDraw.Draw(img)
    if cls == "bus_stop":
        x = int(rng.integers(70, 150))
        d.rectangle([x, 40, x + 12, 200], fill=(225, 225, 225))
        d.rectangle([x - 30, 40, x + 42, 90], fill=(240, 200, 40))
    elif cls == "train_platform":
        for y in range(40, 200, 24):
            d.line([0, y, IMG_SIZE, y], fill=(180, 180, 190), width=4)
        d.rectangle([0, 194, IMG_SIZE, 214], fill=(240, 220, 40))
    elif cls == "street_corner":
        for x in range(20, 205, 28):
            d.rectangle([x, 120, x + 14, 210], fill=(250, 250, 250))
    elif cls == "vehicle_interior":
        for x in range(20, 205, 60):
            d.rectangle([x, 40, x + 40, 110], fill=(150, 180, 210))
    elif cls == "transfer_hub":
        d.ellipse([40, 40, 120, 120], fill=(232, 232, 232))
        d.rectangle([130, 90, 205, 180], fill=(200, 60, 60))
    arr = np.array(img).astype(np.int16)
    arr = np.clip(arr + rng.integers(-25, 26, arr.shape), 0, 255).astype(np.uint8)
    return Image.fromarray(arr)


def build_smoke(train_n: int, val_n: int, test_n: int) -> None:
    rng = np.random.default_rng(42)
    for split, n in (("train", train_n), ("val", val_n), ("test", test_n)):
        for cls in CLASSES:
            out = DATA_DIR / split / cls
            out.mkdir(parents=True, exist_ok=True)
            for i in range(n):
                _draw_sample(cls, rng).save(out / f"{cls}_{i:04d}.png")
    print(f"Synthetic dataset -> {DATA_DIR} ({train_n}/{val_n}/{test_n} per class per split).")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--mode", choices=["smoke", "places365", "openimages", "real"], default="smoke")
    ap.add_argument("--train-n", type=int, default=80)
    ap.add_argument("--val-n", type=int, default=20)
    ap.add_argument("--test-n", type=int, default=20)
    ap.add_argument("--max-per-class", type=int, default=200, help="cap for real modes")
    args = ap.parse_args()

    if args.mode == "smoke":
        build_smoke(args.train_n, args.val_n, args.test_n)
    if args.mode in ("places365", "real"):
        import datasets_places365
        datasets_places365.run(max_per_class=args.max_per_class)
    if args.mode in ("openimages", "real"):
        import datasets_openimages
        datasets_openimages.run(max_per_class=args.max_per_class)


if __name__ == "__main__":
    main()
