import json
import sys

with open(sys.argv[1]) as f:
    sarif = json.load(f)

for run in sarif.get("runs", []):
    for result in run.get("results", []):
        if result.get("level") != "error":
            continue
        print(result.get("ruleId"), result.get("message", {}).get("text"))
        for key in ("locations", "relatedLocations"):
            for loc in result.get(key, []):
                phys = loc.get("physicalLocation", {})
                uri = phys.get("artifactLocation", {}).get("uri", "")
                line = phys.get("region", {}).get("startLine")
                note = loc.get("message", {}).get("text", "")
                print(f"  {key}: {uri}:{line} {note}")
        for flow in result.get("codeFlows", []):
            for tf in flow.get("threadFlows", []):
                for step in tf.get("locations", []):
                    loc = step.get("location", {})
                    phys = loc.get("physicalLocation", {})
                    uri = phys.get("artifactLocation", {}).get("uri", "")
                    line = phys.get("region", {}).get("startLine")
                    print(f"  flow: {uri}:{line} {loc.get('message', {}).get('text', '')}")
