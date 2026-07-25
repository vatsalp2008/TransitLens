"""Fetch Places365 images and map them to the 6 transit scene classes.

Sources (no auth):
  - categories:  GitHub mirror (CSAILVision/places365)
  - val labels:  filelist_places365-standard.tar on the MIT host (~67 MB)
  - images:      val_256.tar on the MIT host (~500 MB, streamed — not stored whole)

The category -> class map below is validated against categories_places365.txt.
macOS note: export SSL_CERT_FILE (see requirements.txt) so urllib can verify TLS.
"""
import argparse
import io
import tarfile
import urllib.request
from collections import defaultdict
from pathlib import Path

from config import DATA_DIR

CATEGORIES_URL = "https://raw.githubusercontent.com/CSAILVision/places365/master/categories_places365.txt"
FILELIST_TAR_URL = "https://data.csail.mit.edu/places/places365/filelist_places365-standard.tar"
VAL_IMAGES_TAR_URL = "https://data.csail.mit.edu/places/places365/val_256.tar"

# Places365 category -> our transit scene class (validated against categories_places365.txt).
CATEGORY_TO_CLASS = {
    "/a/airport_terminal": "transfer_hub",
    "/b/bus_interior": "vehicle_interior",
    "/t/train_interior": "vehicle_interior",
    "/b/bus_station/indoor": "bus_stop",
    "/c/crosswalk": "street_corner",
    "/s/street": "street_corner",
    "/s/subway_station/platform": "train_platform",
    "/t/train_station/platform": "train_platform",
    "/r/railroad_track": "train_platform",
}


def _fetch(url: str) -> bytes:
    with urllib.request.urlopen(url) as response:
        return response.read()


def load_category_index():
    """Return (places_index -> our_class dict, list of 'other' indices for 'unknown')."""
    index_to_class = {}
    other = []
    for line in _fetch(CATEGORIES_URL).decode().splitlines():
        if not line.strip():
            continue
        name, idx = line.rsplit(" ", 1)
        idx = int(idx)
        cls = CATEGORY_TO_CLASS.get(name)
        if cls:
            index_to_class[idx] = cls
        else:
            other.append(idx)
    return index_to_class, other


def load_val_labels():
    """Extract places365_val.txt from the filelist tar -> [(basename, category_index)]."""
    raw = _fetch(FILELIST_TAR_URL)
    with tarfile.open(fileobj=io.BytesIO(raw), mode="r:") as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("places365_val.txt"))
        content = tar.extractfile(member).read().decode()
    labels = []
    for line in content.splitlines():
        if not line.strip():
            continue
        fname, idx = line.rsplit(" ", 1)
        labels.append((Path(fname.strip()).name, int(idx)))
    return labels


def plan_selection(labels, index_to_class, other_indices, max_per_class, unknown_total):
    """Choose val basenames per class (capped). Returns {basename: our_class}."""
    other_set = set(other_indices)
    counts = defaultdict(int)
    selection = {}
    for basename, idx in labels:
        cls = index_to_class.get(idx)
        if cls is not None:
            if counts[cls] < max_per_class:
                selection[basename] = cls
                counts[cls] += 1
        elif idx in other_set and counts["unknown"] < unknown_total:
            selection[basename] = "unknown"
            counts["unknown"] += 1
    return selection


def _split_for(n: int) -> str:
    r = n % 10
    return "train" if r < 7 else "val" if r < 8 else "test"  # 70/10/20-ish by count


def download_and_extract(selection, out_dir: Path):
    """Stream val_256.tar and write only selected images into split/class folders."""
    per_class = defaultdict(int)
    with urllib.request.urlopen(VAL_IMAGES_TAR_URL) as response:
        with tarfile.open(fileobj=response, mode="r|*") as tar:  # streaming
            for member in tar:
                if not member.isfile():
                    continue
                name = Path(member.name).name
                cls = selection.get(name)
                if cls is None:
                    continue
                data = tar.extractfile(member).read()
                dest = out_dir / _split_for(per_class[cls]) / cls
                dest.mkdir(parents=True, exist_ok=True)
                (dest / name).write_bytes(data)
                per_class[cls] += 1
    return dict(per_class)


def run(max_per_class: int = 200, unknown_total: int = 300, out: str = str(DATA_DIR)):
    index_to_class, other = load_category_index()
    print("Places365 -> classes:", sorted(set(index_to_class.values())) + ["unknown"])
    labels = load_val_labels()
    selection = plan_selection(labels, index_to_class, other, max_per_class, unknown_total)
    print(f"Selected {len(selection)} val images; streaming val_256.tar (~500 MB)...")
    counts = download_and_extract(selection, Path(out))
    print("Wrote per-class counts:", counts)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--max-per-class", type=int, default=200)
    ap.add_argument("--unknown", type=int, default=300)
    ap.add_argument("--out", default=str(DATA_DIR))
    args = ap.parse_args()
    run(args.max_per_class, args.unknown, args.out)


if __name__ == "__main__":
    main()
