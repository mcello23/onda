#!/usr/bin/env python3
"""Antigravity PreToolUse hook adapter for mjmirza/app-store-compliance guard.

Reads Antigravity's PreToolUse JSON payload from stdin, forwards shell commands to
~/repositories/app-store-compliance/agent-os/hooks/app-store-compliance-guard.sh,
and returns Antigravity's allow/deny JSON response on stdout.
"""

import json
import os
import subprocess
import sys


GUARD_PATH = os.path.expanduser(
    "~/repositories/app-store-compliance/agent-os/hooks/app-store-compliance-guard.sh"
)


def emit(decision: str, reason: str = "") -> int:
    out = {"decision": decision}
    if reason:
        out["reason"] = reason
    sys.stdout.write(json.dumps(out) + "\n")
    return 0


def main() -> int:
    raw = sys.stdin.read()
    if not raw.strip():
        return emit("allow")
    try:
        payload = json.loads(raw)
    except Exception:
        return emit("allow")

    tool_call = payload.get("toolCall") if isinstance(payload, dict) else None
    if not isinstance(tool_call, dict) or tool_call.get("name") != "run_command":
        return emit("allow")

    args = tool_call.get("args")
    cmd = args.get("CommandLine") if isinstance(args, dict) else None
    if not isinstance(cmd, str) or not cmd.strip():
        return emit("allow")

    if not os.path.isfile(GUARD_PATH):
        return emit("allow")

    cwd = payload.get("cwd")
    if not isinstance(cwd, str) or not os.path.isdir(cwd):
        roots = payload.get("workspacePaths")
        cwd = roots[0] if isinstance(roots, list) and roots and os.path.isdir(roots[0]) else os.getcwd()

    guard_in = json.dumps(
        {
            "hook_event_name": "PreToolUse",
            "tool_name": "Bash",
            "tool_input": {"command": cmd},
            "cwd": cwd,
        }
    )

    try:
        proc = subprocess.run(
            ["bash", GUARD_PATH],
            input=guard_in,
            capture_output=True,
            text=True,
            cwd=cwd,
            timeout=110,
        )
    except Exception:
        return emit("allow")

    if proc.returncode == 2:
        combined = (proc.stdout + "\n" + proc.stderr).strip()
        lines = [
            line
            for line in combined.splitlines()
            if "[CRITICAL]" in line or line.strip().startswith(("file.", "fix.", "BLOCKED.", "Summary."))
        ]
        summary = "\n".join(lines[:20]) if lines else combined[-1200:]
        return emit(
            "deny",
            "App Store Compliance Guard blocked this release/submit command. "
            "Fix critical risks or set APP_STORE_GUARD_OK=1 to override.\n" + summary,
        )

    return emit("allow")


if __name__ == "__main__":
    sys.exit(main())
