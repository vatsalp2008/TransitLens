"""Build the scene-classifier dataset.

Two modes:
  --mode smoke       (default) Generate a small synthetic dataset with a distinct
                     visual signature per class. This exists to VALIDATE the
                     training/export pipeline end-to-end without downloads. It is
                     NOT a production model dataset.
  --mode openimages  Guidance for assembling the real dataset from Google Open
                     Images (bus_stop, train_station) + Mapillary + custom Seattle
                     photos. Real accuracy targets (see ADR-002) require this.

Layout produced: data/scene/{train,val,test}/{class}/*.png
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
    if cls == "bus_stop":  # pole + sign
        x = int(rng.integers(70, 150))
        d.rectangle([x, 40, x + 12, 200], fill=(225, 225, 225))
        d.rectangle([x - 30, 40, x + 42, 90], fill=(240, 200, 40))
    elif cls == "train_platform":  # platform edge lines + tactile strip
        for y in range(40, 200, 24):
            d.line([0, y, IMG_SIZE, y], fill=(180, 180, 190), width=4)
        d.rectangle([0, 194, IMG_SIZE, 214], fill=(240, 220, 40))
    elif cls == "street_corner":  # crosswalk stripes
        for x in range(20, 205, 28):
            d.rectangle([x, 120, x + 14, 210], fill=(250, 250, 250))
    elif cls == "vehicle_interior":  # windows
        for x in range(20, 205, 60):
            d.rectangle([x, 40, x + 40, 110], fill=(150, 180, 210))
    elif cls == "transfer_hub":  # mixed signage shapes
        d.ellipse([40, 40, 120, 120], fill=(232, 232, 232))
        d.rectangle([130, 90, 205, 180], fill=(200, 60, 60))
    # "unknown": base color + noise only, no motif.

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
    print(f"Synthetic dataset written to {DATA_DIR} "
          f"({train_n}/{val_n}/{test_n} per class per split).")


def openimages_guidance() -> None:
    print(
        "Real dataset assembly (openimages mode):\n"
        "  pip install fiftyone\n"
        "  Download Open Images v7 detections for 'Bus stop' and 'Train station',\n"
        "  crop/label to our classes, add Mapillary Vistas street-level imagery for\n"
        "  street_corner/transfer_hub, and 200+ custom Seattle photos per ADR-002.\n"
        "  Place images under data/scene/{train,val,test}/{class}/ (70/15/15).\n"
    )


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--mode", choices=["smoke", "openimages"], default="smoke")
    ap.add_argument("--train-n", type=int, default=80)
    ap.add_argument("--val-n", type=int, default=20)
    ap.add_argument("--test-n", type=int, default=20)
    args = ap.parse_args()

    if args.mode == "smoke":
        build_smoke(args.train_n, args.val_n, args.test_n)
    else:
        openimages_guidance()


if __name__ == "__main__":
    main()
