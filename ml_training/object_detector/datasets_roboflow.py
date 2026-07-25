"""Download Roboflow Universe datasets and merge them into the YOLO detector set.

The accessibility classes (elevator_door, escalator, crosswalk_marking,
wheelchair_ramp, tactile_paving, accessibility_sign) aren't well covered by the
big public datasets, but https://universe.roboflow.com hosts community datasets
for exactly these. Fill DATASETS below with real projects (search Universe for
each class), mapping each dataset's label names to our classes.

Requires a free Roboflow API key: export ROBOFLOW_API_KEY=...
"""
import argparse
import os
import shutil
from pathlib import Path

from config import CLASSES, DATA_DIR

# Verified Roboflow Universe datasets. class_map: THEIR label name -> our class name.
# Gaps (no good public dataset found): elevator_door, wheelchair_ramp, accessibility_sign.
DATASETS = [
    {"workspace": "tcc-xn3st", "project": "yolov8-crosswalk-detection", "version": 1,
     "class_map": {"crosswalk": "crosswalk_marking"}},
    {"workspace": "project-5wrkt", "project": "zebra-crossing-inwkp", "version": 9,
     "class_map": {"Zebra-Crossing": "crosswalk_marking"}},
    {"workspace": "susam", "project": "tactile-pavement", "version": 2,
     "class_map": {"border": "tactile_paving"}},
    {"workspace": "escalatorsafetysystem", "project": "mess-e9kep", "version": 1,
     "class_map": {"escalator": "escalator"}},
]


def _merge(download_dir: Path, class_map: dict, out_dir: Path, prefix: str) -> int:
    import yaml

    names = yaml.safe_load((download_dir / "data.yaml").read_text())["names"]
    if isinstance(names, dict):
        names = [names[i] for i in sorted(names)]
    remap = {}
    for their_idx, their_name in enumerate(names):
        our = class_map.get(their_name)
        if our in CLASSES:
            remap[their_idx] = CLASSES.index(our)

    written = 0
    for split in ("train", "valid", "val", "test"):
        img_src = download_dir / split / "images"
        lbl_src = download_dir / split / "labels"
        if not img_src.is_dir():
            continue
        dst_split = "train" if split == "train" else "val"
        img_dst = out_dir / dst_split / "images"
        lbl_dst = out_dir / dst_split / "labels"
        img_dst.mkdir(parents=True, exist_ok=True)
        lbl_dst.mkdir(parents=True, exist_ok=True)
        for lbl in lbl_src.glob("*.txt"):
            out_lines = []
            for line in lbl.read_text().splitlines():
                parts = line.split()
                if len(parts) < 5:
                    continue
                their_idx = int(parts[0])
                if their_idx not in remap:
                    continue
                coords = [float(v) for v in parts[1:]]
                if len(coords) == 4:  # detection bbox: cx cy w h
                    cx, cy, w, h = coords
                else:  # segmentation polygon: x1 y1 x2 y2 ... -> enclosing bbox
                    xs, ys = coords[0::2], coords[1::2]
                    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
                    cx, cy, w, h = (x0 + x1) / 2, (y0 + y1) / 2, x1 - x0, y1 - y0
                out_lines.append(f"{remap[their_idx]} {cx:.6f} {cy:.6f} {w:.6f} {h:.6f}")
            if not out_lines:
                continue
            img = next(
                (img_src / (lbl.stem + ext) for ext in (".jpg", ".jpeg", ".png")
                 if (img_src / (lbl.stem + ext)).exists()),
                None,
            )
            if img is None:
                continue
            stem = f"{prefix}_{written:05d}"
            shutil.copy(img, img_dst / f"{stem}{img.suffix}")
            (lbl_dst / f"{stem}.txt").write_text("\n".join(out_lines) + "\n")
            written += 1
    return written


def run(out: str = str(DATA_DIR)):
    if not DATASETS:
        print(
            "No Roboflow datasets configured. Edit DATASETS in datasets_roboflow.py with "
            "projects from https://universe.roboflow.com (one per accessibility class), then re-run."
        )
        return
    api_key = os.environ.get("ROBOFLOW_API_KEY")
    if not api_key:
        raise SystemExit("Set ROBOFLOW_API_KEY (free key from roboflow.com).")
    try:
        from roboflow import Roboflow
    except ImportError as exc:  # pragma: no cover
        raise SystemExit("pip install roboflow (see ml_training/requirements.txt)") from exc

    rf = Roboflow(api_key=api_key)
    out_dir = Path(out)
    staging = out_dir / "_roboflow_raw"  # gitignored (under data/detect)
    total = 0
    for spec in DATASETS:
        location = staging / f"{spec['project']}-{spec['version']}"
        if not location.exists():  # reuse already-downloaded data on re-runs
            rf.workspace(spec["workspace"]).project(spec["project"]).version(spec["version"]).download(
                "yolov8", location=str(location)
            )
        total += _merge(location, spec["class_map"], out_dir, prefix=spec["project"])
    print(f"Merged {total} Roboflow images into {out_dir}")


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--out", default=str(DATA_DIR))
    args = ap.parse_args()
    run(args.out)


if __name__ == "__main__":
    main()
