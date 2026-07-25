"""Build the object-detector dataset in YOLO format.

Modes:
  smoke       (default) synthetic bbox images to validate the pipeline (no downloads).
  openimages  real Open Images v7 boxes (Bus, Train) via FiftyOne — see datasets_openimages.py.
  roboflow    real accessibility-class boxes from Roboflow Universe — see datasets_roboflow.py.
  real        openimages + roboflow combined.

Produces: data/detect/{train,val}/images/*, .../labels/*.txt, and data.yaml
"""
import argparse

import numpy as np
import yaml
from PIL import Image, ImageDraw

from config import CLASSES, DATA_DIR, DATA_YAML, IMG_SIZE

COLORS = [
    (60, 120, 200), (150, 60, 60), (90, 90, 90), (200, 140, 40),
    (240, 240, 240), (40, 160, 90), (240, 210, 40), (60, 60, 200),
]


def _one_image(rng: np.random.Generator):
    img = Image.new("RGB", (IMG_SIZE, IMG_SIZE), (24, 26, 30))
    d = ImageDraw.Draw(img)
    labels = []
    for _ in range(int(rng.integers(1, 4))):
        cls = int(rng.integers(0, len(CLASSES)))
        w = int(rng.integers(40, 120))
        h = int(rng.integers(40, 120))
        x = int(rng.integers(0, IMG_SIZE - w))
        y = int(rng.integers(0, IMG_SIZE - h))
        if cls % 2 == 0:
            d.rectangle([x, y, x + w, y + h], fill=COLORS[cls])
        else:
            d.ellipse([x, y, x + w, y + h], fill=COLORS[cls])
        labels.append(
            f"{cls} {(x + w / 2) / IMG_SIZE:.6f} {(y + h / 2) / IMG_SIZE:.6f} "
            f"{w / IMG_SIZE:.6f} {h / IMG_SIZE:.6f}"
        )
    arr = np.array(img).astype(np.int16)
    arr = np.clip(arr + rng.integers(-12, 13, arr.shape), 0, 255).astype(np.uint8)
    return Image.fromarray(arr), labels


def build_smoke(train_n: int, val_n: int) -> None:
    rng = np.random.default_rng(7)
    for split, n in (("train", train_n), ("val", val_n)):
        img_dir = DATA_DIR / split / "images"
        lbl_dir = DATA_DIR / split / "labels"
        img_dir.mkdir(parents=True, exist_ok=True)
        lbl_dir.mkdir(parents=True, exist_ok=True)
        for i in range(n):
            img, labels = _one_image(rng)
            img.save(img_dir / f"img_{i:04d}.png")
            (lbl_dir / f"img_{i:04d}.txt").write_text("\n".join(labels) + "\n")
    DATA_YAML.write_text(
        yaml.safe_dump(
            {
                "path": str(DATA_DIR),
                "train": "train/images",
                "val": "val/images",
                "names": {i: name for i, name in enumerate(CLASSES)},
            },
            sort_keys=False,
        )
    )
    print(f"Synthetic detector dataset -> {DATA_DIR} ({train_n}/{val_n}); wrote {DATA_YAML}")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--mode", choices=["smoke", "openimages", "roboflow", "real"], default="smoke")
    ap.add_argument("--train-n", type=int, default=120)
    ap.add_argument("--val-n", type=int, default=30)
    ap.add_argument("--max-samples", type=int, default=400, help="Open Images sample cap")
    args = ap.parse_args()

    if args.mode == "smoke":
        build_smoke(args.train_n, args.val_n)
    if args.mode in ("openimages", "real"):
        import datasets_openimages
        datasets_openimages.run(max_samples=args.max_samples)
    if args.mode in ("roboflow", "real"):
        import datasets_roboflow
        datasets_roboflow.run()


if __name__ == "__main__":
    main()
