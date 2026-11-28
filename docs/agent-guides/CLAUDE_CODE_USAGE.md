# Claude Code: Subscription Usage and Context Guide

**Scope:** Practical guidance for contributors using Claude Code, including the Code experience in Claude Desktop and the terminal/IDE integrations.  
**Status:** Optional provider-specific supplement; it does not make Claude Code or any Claude model mandatory for Motion.  
**Last reviewed:** 2026-11-28  
**Authority:** [AGENTS.md](../../AGENTS.md), [docs/RULES.md](../RULES.md), [docs/AI_USAGE_POLICY.md](../AI_USAGE_POLICY.md), and [docs/MOTION.md](../MOTION.md) remain authoritative. This guide cannot override them.  
**Model note:** Claude Opus 5.5 is one supported model option, not Motion's required default.

## 1. The goal: preserve included usage while completing verified work

Claude Code can use a subscription allowance or a separately billed API/provider account. Know which path the session is using before interpreting its usage.

For subscription sign-in, the scarce resource is the account's included usage allowance and its reset windows—not a per-turn invoice calculated from public API prices. Anthropic currently documents five-hour session and weekly limits for Pro and Max; some plans have different arrangements, and account limits can change. Claude and Claude Code activity share applicable account limits. Always use the account's own Settings > Usage display as the source of truth.

A long session can consume more of the included allowance. Each turn can contain the conversation so far, project instructions, files read, tool results, and the new prompt. Long histories, large file reads, generated diffs, attached files, tool/connector activity, model selection, and effort level can all affect usage.

A model's context window is a separate limit. A 1-million-token context window does not provide unlimited subscription usage and does not mean a long session is free to keep extending.

## 2. Check usage without obsessively polling

For a substantial task or coding session:

- Before starting a substantial task, check **Settings → Usage** in Claude Desktop or the Claude account interface. Note the session/weekly usage indicators and reset times.
- Use `/context` when a session becomes long, after several large file reads, or when the agent begins repeating itself. It shows what is occupying the active context window.
- Use `/cost` only for API billing; it reports the current session's API token/cost usage and is not the subscription-plan usage meter.
- Do not issue usage/context checks on every turn. Check at meaningful task boundaries or when the session appears to be growing substantially.
- Do not estimate remaining subscription quota from token counts, message counts, or API list prices. Use the provider's displayed usage information.
- If the account prompts to continue with extra usage or API credits after reaching the included limit, decline unless the owner has explicitly approved the additional paid usage. Do not enable auto-reload or paid overages as a convenience.

For API-key, Console, Bedrock, Vertex, or Microsoft Foundry access, the billing meter is different. Use the relevant account's actual usage dashboard or tool reporting. Public Anthropic API prices do not describe subscription-plan consumption.

## 3. Keep sessions bounded

A productive session should have one clear objective: an issue, one coherent PR, or a bounded milestone task.

1. Start with the approved task, its acceptance criteria, the active branch, and the current commit.
2. Reuse a concise handoff from ChatGPT, a human reviewer, or an earlier session. Do not ask Claude Code to re-read and re-investigate the entire repository when the code state and evidence have not changed.
3. Search first; open only relevant files and line ranges. Avoid whole-repository dumps, huge generated files, repeated copies of the same instructions, and full build logs when a focused excerpt is enough.
4. Finish the current scoped task and verify its changed surface before starting unrelated work.
5. At the end, save durable decisions and progress in the PR, issue, specification, or task notes—not only in the chat transcript.
6. Record the branch and exact commit, objective, files changed, decisions, tests/evidence, what remains unverified, and the single next action.

### When to compact or start fresh

- Use `/compact` when continuing the same task after a long investigation or implementation phase. Provide a focus instruction where helpful, for example: `/compact keep the current objective, decisions, changed files, test results, blockers, and next action; omit repeated logs and unrelated exploration`.
- Review the compacted result before continuing. Restore any missing critical decision, commit SHA, test result, or limitation from the repository/PR.
- Use `/clear` when switching to an unrelated task. The conversation history is cleared from the active context; files, commits, branches, and PRs in the repository remain. Save a handoff and make sure important work is not left only in an unsaved conversation.
- Use automatic compaction as a safety net, not as a reason to let a session grow without limit.
- Do not repeatedly compact and then reload the same large files or logs; that can refill the context and waste usage again.

The right rule is: **compact to continue the same task; clear and hand off to start a different task.**

## 4. Avoid expensive context habits

- Keep always-loaded project instructions short. Put detailed procedures in referenced documents, milestone specifications, or optional skills instead of duplicating them in every instruction file.
- Read the relevant portions of `docs/RULES.md` and the milestone plan for the task, not every document, unless the task is a full-document audit.
- Avoid loading multiple copies of the same plan, old issue descriptions, full transcripts, duplicate logs, or generated build output.
- Use targeted repository search and narrow line ranges. Ask tools to return the relevant function, section, or failure summary.
- Keep connectors, browser integrations, extensions, and other tool descriptions disconnected or disabled when the task does not need them. Extra tool schemas and connector activity can consume context and usage.
- Do not fan out subagents or parallel sessions just because the feature exists. Each active agent adds its own context and model usage. Delegate only genuinely independent tasks and define non-overlapping outputs.
- Do not ask multiple assistants to perform the same repository audit or produce duplicate plans. Use one assistant for planning/review and another for a well-scoped execution handoff when that division is useful.
- Prefer deterministic scripts, formatters, compilers, and test runners for mechanical work rather than asking the model to reason through output that a tool can verify.

## 5. Work with ChatGPT and Claude Code without duplicating effort

When ChatGPT or another assistant helps plan a task and Claude Code performs repository work, use a short handoff rather than duplicating the full research:

**Planning/review handoff should contain:**
- Repository and branch/base commit.
- Task objective and why it is next.
- Approved scope and explicit non-goals.
- Files/sections already reviewed and relevant findings.
- Acceptance criteria and checks required.
- Decisions already made and choices still requiring owner approval.

**Claude Code execution should return:**
- Branch and exact resulting commit.
- Files and behaviours changed.
- Tests/checks actually run, with results.
- Checks not run and why.
- New dependencies, external calls, costs or quota limitations, if any.
- Remaining risks and the next action.

Do not make either assistant re-read the same large files or repeat the same web/repository research unless new commits or evidence justify it. ChatGPT can review the handoff and PR diff without duplicating Claude Code's implementation exploration.

## 6. Model choice and Opus 5.5

Claude Opus 5.5 is designed for demanding, long-running agentic coding and knowledge work. Check its current context window and default effort in the [model overview](https://platform.claude.com/docs/en/about-claude/models/overview) rather than relying on this guide. A large context window or a high effort setting is a capability, not a reason to use either for every task.

Recommended approach:

- Anthropic's current guidance positions Sonnet for most coding work, Opus for difficult debugging, broad refactors, and architectural reasoning, and Haiku for quick mechanical tasks. For Motion, follow the tool tiers in [docs/AI_USAGE_POLICY.md](../AI_USAGE_POLICY.md) section 2: Sonnet is the default for standard coding, Haiku for mechanical work, and Opus is the escalation tier for public API, Core motion and concurrency design, stubborn bugs, and optional independent review. Opus uses significantly more of a subscription allowance than Sonnet, so use it only where it is likely to reduce meaningful risk or rework. The current task-by-task assignment is the task queue in [docs/CHATGPT_HANDOFF.md](../CHATGPT_HANDOFF.md).
- Use the model and effort that can complete the current task reliably; do not select Opus 5.5 for every small edit or simple documentation change merely because it is available.
- Medium/default effort is a sensible starting point for ordinary coding and review. Increase effort for difficult debugging, security-sensitive work, or high-impact architecture analysis when the task warrants it.
- Do not confuse model effort with a guaranteed quota cost or a quality guarantee. The provider's actual usage meter is authoritative.
- Do not change the model mid-task without a reason. Claude Code documentation notes that changing models can affect prompt-cache reuse and cause the conversation to be read again under the new model.
- Never set Opus 5.5 as a repository-wide required model. Contributors may use another suitable model or coding tool.

### Prompt caching

Prompt caching may reduce billed input for supported API requests when stable prompt prefixes are reused. It does not remove cached tokens from the model's context window. Do not assume API prompt-caching discounts apply to subscription usage or reduce the subscription allowance by the same ratio. The actual product/account usage meter remains authoritative.

## 7. What worked in this repository (2026-11-28)

These are observations from one Claude Code session on the web, not guarantees for other surfaces:

- **Route inside one session.** An Opus session kept the reviews (Core frame scheduling, the toolbar prototype) and delegated two bounded doc tasks to Sonnet subagents with self-contained briefs. Each brief named the exact files to touch, the facts to use, and "do not commit"; the Opus session reviewed each diff and committed. Subagents start without the conversation, so a vague brief wastes their usage on re-exploration.
- **Give subagents non-overlapping files** so parallel edits cannot conflict.
- **Cloud sessions are ephemeral.** Commit and push to the session branch before the session ends; anything left only in the container is lost.
- **Watch PRs instead of polling.** Subscribing the session to a PR's activity delivers CI results and review comments as events, so no turns are spent re-checking an unchanged run.
- **Slash commands vary by surface.** Some commands in this guide (`/context`, `/compact`, `/cost`) behave differently or may be unavailable in Claude Code on the web or in the app; check the surface you are using.

## 8. A practical session checklist

Before a substantial Claude Code task:

- [ ] Is this one bounded task with explicit acceptance criteria?
- [ ] Am I using the intended subscription sign-in, rather than an API key or paid fallback?
- [ ] Have I checked Settings → Usage if I use a subscription and the task is expected to be long?
- [ ] Do I understand that `/cost` is for API billing, while `/context` shows context capacity?
- [ ] Do I have a concise handoff rather than repeated repository history?
- [ ] Am I loading only the relevant files and tools?
- [ ] Is the model/effort appropriate to the task?
- [ ] Have I avoided unnecessary parallel agents and duplicate research?

Before ending or switching tasks:

- [ ] Is the work saved in the repository or PR?
- [ ] Is the exact branch and commit recorded?
- [ ] Are tests and unrun checks accurately reported?
- [ ] Is there a short handoff for the next session?
- [ ] Should I `/compact` to continue the same task, or `/clear` before an unrelated one?

## 9. Official references

- [Models, usage, and limits in Claude Code](https://support.claude.com/en/articles/14552983-models-usage-and-limits-in-claude-code)
- [Usage limit best practices](https://support.claude.com/en/articles/9797557-usage-limit-best-practices)
- [How usage and length limits work](https://support.claude.com/en/articles/11647753-how-usage-and-length-limits-work)
- [Explore the Claude Code context window](https://code.claude.com/docs/en/context-window)
- [Manage Claude Code sessions](https://code.claude.com/docs/en/sessions)
- [Claude Code troubleshooting](https://code.claude.com/docs/en/troubleshooting)
- [Claude model overview](https://platform.claude.com/docs/en/about-claude/models/overview)
- [Claude Code cheatsheet](https://support.claude.com/en/articles/14553413-claude-code-cheatsheet)

Provider limits, model details, and product commands can change. Re-check official references before relying on a model-specific or account-specific detail.
