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

# Fill from Roboflow Universe. class_map: THEIR label name -> our class name.
DATASETS = [
    # {
    #     "workspace": "some-workspace",
    #     "project": "elevator-door-detection",
    #     "version": 2,
    #     "class_map": {"elevator": "elevator_door", "elevator-door": "elevator_door"},
    # },
    # {"workspace": "...", "project": "tactile-paving", "version": 1,
    #  "class_map": {"tactile_paving": "tactile_paving", "braille-block": "tactile_paving"}},
]


def _merge(download_dir: Path, class_map: dict, out_dir: Path) -> int:
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
                if len(parts) != 5:
                    continue
                their_idx = int(parts[0])
                if their_idx in remap:
                    out_lines.append(f"{remap[their_idx]} {parts[1]} {parts[2]} {parts[3]} {parts[4]}")
            if not out_lines:
                continue
            img = next(
                (img_src / (lbl.stem + ext) for ext in (".jpg", ".jpeg", ".png")
                 if (img_src / (lbl.stem + ext)).exists()),
                None,
            )
            if img is None:
                continue
            stem = f"rf_{written:05d}"
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
    total = 0
    for spec in DATASETS:
        version = rf.workspace(spec["workspace"]).project(spec["project"]).version(spec["version"])
        downloaded = version.download("yolov8")
        total += _merge(Path(downloaded.location), spec["class_map"], out_dir)
    print(f"Merged {total} Roboflow images into {out_dir}")


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--out", default=str(DATA_DIR))
    args = ap.parse_args()
    run(args.out)


if __name__ == "__main__":
    main()
