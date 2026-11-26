import pathlib
import re
import xml.etree.ElementTree as ET

root = pathlib.Path("build/test-results/test")
rows = {}
passed = 0
for path in root.glob("TEST-*.xml"):
    suite = ET.parse(path).getroot()
    for case in suite.findall("testcase"):
        if "longRunningPopulation" in case.get("name", "") and not any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
            passed += 1
    for output in suite.iter("system-out"):
        for line in (output.text or "").splitlines():
            if "days=" not in line or "p95TickMs=" not in line:
                continue
            data = dict(re.findall(r"(\w+)=([\w.\-]+)", line))
            if all(k in data for k in ("days", "day", "p95TickMs", "mipMs", "indexBytes", "checkpointBytes")):
                rows[int(data["days"]), int(data["day"])] = data

complete = passed == 2 and all((n, d) in rows for n in (7, 30) for d in range(1, n+1))
peak = max((float(r["p95TickMs"]) for r in rows.values()), default=None)
report = ["# DNMS endurance quality report", "",
          f"- Long test cases passed: {passed}/2",
          f"- Daily samples: {len(rows)}/37",
          f"- Endurance evidence complete: {complete}",
          f"- Worst daily p95 tick ms: {peak}",
          "- Native profiling decision: " + ("required for investigation" if complete and peak > 25 else "not triggered by p95" if complete else "insufficient evidence"),
          "- Memory leak, live rendering and Java/C++ comparison: not certified by this report.",
          "", "| Duration | Day | p95 tick ms | MIP ms | Index bytes | Save bytes | Search ms | Heap bytes |", "|---|---|---|---|---|---|---|---|"]
for (n,d), r in sorted(rows.items()):
    report.append("| " + " | ".join([str(n), str(d)] + [r.get(k,"n/a") for k in ("p95TickMs","mipMs","indexBytes","checkpointBytes","searchMs","heapBytes")]) + " |")
pathlib.Path("quality-report.md").write_text("\n".join(report)+"\n", encoding="utf-8")
print("\n".join(report))
if not complete:
    raise SystemExit("Missing successful endurance evidence")
