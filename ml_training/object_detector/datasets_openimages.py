"""Fetch Open Images v7 detections (Bus, Train) into the YOLO detector dataset."""
import argparse
import shutil
from collections import defaultdict
from pathlib import Path

from config import CLASSES, DATA_DIR

# Open Images detection class -> our detector class.
OI_DET_TO_CLASS = {"Bus": "bus", "Train": "train_car"}


def _split_for(n: int) -> str:
    return "train" if n % 5 < 4 else "val"  # 80/20


def _write_data_yaml(out_dir: Path) -> None:
    import yaml

    (out_dir / "data.yaml").write_text(
        yaml.safe_dump(
            {
                "path": str(out_dir),
                "train": "train/images",
                "val": "val/images",
                "names": {i: name for i, name in enumerate(CLASSES)},
            },
            sort_keys=False,
        )
    )


def run(max_samples: int = 400, split: str = "train", out: str = str(DATA_DIR)):
    try:
        import fiftyone.zoo as foz
    except ImportError as exc:  # pragma: no cover
        raise SystemExit("pip install fiftyone (see ml_training/requirements.txt)") from exc

    dataset = foz.load_zoo_dataset(
        "open-images-v7",
        split=split,
        label_types=["detections"],
        classes=sorted(OI_DET_TO_CLASS),
        max_samples=max_samples,
        dataset_name=f"tl-oi-detect-{split}",
        overwrite=True,
    )

    out_dir = Path(out)
    box_counts = defaultdict(int)
    written = 0
    for sample in dataset:
        dets = getattr(sample, "detections", None) or getattr(sample, "ground_truth", None)
        if dets is None:
            continue
        lines = []
        for det in dets.detections:
            cls = OI_DET_TO_CLASS.get(det.label)
            if cls is None:
                continue
            x, y, w, h = det.bounding_box  # normalized top-left x,y + width,height
            lines.append(f"{CLASSES.index(cls)} {x + w / 2:.6f} {y + h / 2:.6f} {w:.6f} {h:.6f}")
            box_counts[cls] += 1
        if not lines:
            continue
        split_name = _split_for(written)
        img_dir = out_dir / split_name / "images"
        lbl_dir = out_dir / split_name / "labels"
        img_dir.mkdir(parents=True, exist_ok=True)
        lbl_dir.mkdir(parents=True, exist_ok=True)
        src = Path(sample.filepath)
        stem = f"oi_{written:05d}"
        shutil.copy(src, img_dir / f"{stem}{src.suffix}")
        (lbl_dir / f"{stem}.txt").write_text("\n".join(lines) + "\n")
        written += 1

    _write_data_yaml(out_dir)
    print(f"Open Images detector: {written} images, box counts {dict(box_counts)}")


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--max-samples", type=int, default=400)
    ap.add_argument("--split", default="train")
    ap.add_argument("--out", default=str(DATA_DIR))
    args = ap.parse_args()
    run(args.max_samples, args.split, args.out)


if __name__ == "__main__":
    main()
