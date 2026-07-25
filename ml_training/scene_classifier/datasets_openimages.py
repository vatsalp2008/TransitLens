"""Fetch Open Images v7 image-level labels into transit scene classes via FiftyOne.

FiftyOne downloads only the requested classes/samples (not the full dataset).
Covers the classes Open Images labels well; Places365 fills the interiors/hubs.
"""
import argparse
import shutil
from collections import defaultdict
from pathlib import Path

from config import DATA_DIR

# Open Images image-level class -> our scene class.
OI_TO_CLASS = {
    "Bus stop": "bus_stop",
    "Train station": "train_platform",
    "Subway": "train_platform",
    "Street": "street_corner",
    "Pedestrian crossing": "street_corner",
    "Zebra crossing": "street_corner",
}


def _split_for(n: int) -> str:
    r = n % 10
    return "train" if r < 7 else "val" if r < 8 else "test"


def run(max_per_class: int = 300, split: str = "train", out: str = str(DATA_DIR)):
    try:
        import fiftyone.zoo as foz
    except ImportError as exc:  # pragma: no cover
        raise SystemExit("pip install fiftyone (see ml_training/requirements.txt)") from exc

    oi_classes = sorted(set(OI_TO_CLASS))
    target_classes = set(OI_TO_CLASS.values())
    dataset = foz.load_zoo_dataset(
        "open-images-v7",
        split=split,
        label_types=["classifications"],
        classes=oi_classes,
        max_samples=max_per_class * len(target_classes),
        dataset_name=f"tl-oi-scene-{split}",
        overwrite=True,
    )

    counts = defaultdict(int)
    out_dir = Path(out)
    for sample in dataset:
        positive = getattr(sample, "positive_labels", None)
        if positive is None:
            continue
        chosen = next((OI_TO_CLASS[c.label] for c in positive.classifications if c.label in OI_TO_CLASS), None)
        if chosen is None or counts[chosen] >= max_per_class:
            continue
        dest = out_dir / _split_for(counts[chosen]) / chosen
        dest.mkdir(parents=True, exist_ok=True)
        src = Path(sample.filepath)
        shutil.copy(src, dest / src.name)
        counts[chosen] += 1
    print("Open Images scene counts:", dict(counts))


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--max-per-class", type=int, default=300)
    ap.add_argument("--split", default="train")
    ap.add_argument("--out", default=str(DATA_DIR))
    args = ap.parse_args()
    run(args.max_per_class, args.split, args.out)


if __name__ == "__main__":
    main()
