#!/usr/bin/env node
/**
 * Milestone 1 orchestrator hook (M1-016 → M1-035).
 * Reads DEVELOPMENT_TASKS.md and, if work remains, asks the agent to do the next ID.
 */
const fs = require("fs");
const path = require("path");

const QUEUE = [];
for (let n = 16; n <= 35; n++) {
  QUEUE.push("M1-" + String(n).padStart(3, "0"));
}

function readStdin() {
  return new Promise((resolve) => {
    let raw = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => {
      raw += chunk;
    });
    process.stdin.on("end", () => resolve(raw));
    process.stdin.on("error", () => resolve(raw));
  });
}

function headingComplete(headingLine) {
  return /\[x\]/i.test(headingLine);
}

function nextIncompleteId(markdown) {
  for (const id of QUEUE) {
    const re = new RegExp(
      "^#{1,3}\\s+" + id.replace("-", "\\-") + "\\b([^\\n]*)",
      "im"
    );
    const match = markdown.match(re);
    if (!match) {
      return id;
    }
    if (!headingComplete(match[0])) {
      return id;
    }
  }
  return null;
}

function looksFailed(payload) {
  const status = String(
    payload.status || payload.outcome || payload.result || ""
  ).toLowerCase();
  return /(fail|error|abort|cancel|denied)/.test(status);
}

async function main() {
  const raw = await readStdin();
  let payload = {};
  try {
    payload = raw.trim() ? JSON.parse(raw) : {};
  } catch {
    payload = {};
  }

  const tasksPath = path.join(process.cwd(), "DEVELOPMENT_TASKS.md");
  if (!fs.existsSync(tasksPath)) {
    process.stdout.write("{}\n");
    return;
  }

  const markdown = fs.readFileSync(tasksPath, "utf8");
  const next = nextIncompleteId(markdown);

  if (!next) {
    process.stdout.write("{}\n");
    return;
  }

  if (looksFailed(payload)) {
    process.stdout.write(
      JSON.stringify({
        followup_message:
          "The previous Milestone 1 worker did not finish cleanly. Inspect DEVELOPMENT_TASKS.md, mark the current ID [!] if blocked, and do not start a new ID until that is resolved. Next intended ID: " +
          next +
          ".",
      }) + "\n"
    );
    return;
  }

  process.stdout.write(
    JSON.stringify({
      followup_message:
        "Continue the Milestone 1 orchestrator. Implement only " +
        next +
        " (one ID, unless DEVELOPMENT_TASKS.md lists a tightly coupled pair). Follow AGENTS.md, AGENT_WORKFLOW.md, and .cursor/skills/m1-orchestrator/SKILL.md. Mark [~] then complete with implementation → mvn -q test → docs → [x]. Never send live SMTP. Do not implement Milestone 2.",
    }) + "\n"
  );
}

main().catch(() => {
  process.stdout.write("{}\n");
});
