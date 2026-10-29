import json
import sys

with open(sys.argv[1]) as f:
    sarif = json.load(f)

for run in sarif.get("runs", []):
    for result in run.get("results", []):
        if result.get("ruleId") == "ThreadConstraint":
            print(json.dumps(result, separators=(",", ":")))
