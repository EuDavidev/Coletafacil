"""Static project checks; these do NOT replace Android compilation or device tests."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re
import yaml

root = Path(__file__).resolve().parents[1]
for path in (root / "app/src/main").rglob("*.xml"):
    ET.parse(path)
workflow = yaml.safe_load((root / ".github/workflows/android.yml").read_text())
assert workflow["jobs"]["android"]["steps"]
assert (root / "gradle/wrapper/gradle-wrapper.jar").stat().st_size == 43583

def luminance(value):
    rgb = [int(value[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    linear = [(v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4) for v in rgb]
    return sum(a * b for a, b in zip(linear, (0.2126, 0.7152, 0.0722)))

theme = (root / "app/src/main/java/br/com/coletafacil/ui/Theme.kt").read_text()
themes = theme.split("private val ")[1:]
count = 0
for section in themes:
    colors = dict(re.findall(r'(\w+)\s*=\s*Color\(0xFF([0-9A-Fa-f]{6})\)', section))
    colors.update({k: 'FFFFFF' for k in re.findall(r'(\w+)\s*=\s*Color.White', section)})
    for background, foreground in [('primary', 'onPrimary'), ('primaryContainer', 'onPrimaryContainer'), ('secondary', 'onSecondary'), ('secondaryContainer', 'onSecondaryContainer'), ('background', 'onBackground'), ('surface', 'onSurface'), ('surfaceVariant', 'onSurfaceVariant'), ('error', 'onError')]:
        a, b = sorted([luminance(colors[background]), luminance(colors[foreground])])
        ratio = (b + .05) / (a + .05)
        assert ratio >= 4.5, (background, foreground, ratio)
        count += 1
print(f'PASS: XML/YAML project files, official wrapper size, {count} explicit theme contrast pairs >= 4.5:1')
