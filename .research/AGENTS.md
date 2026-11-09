# Research Instructions

These instructions apply whenever you are asked to **research, investigate, explore, or deeply analyze a topic**.

## Research workspace

Each research task must have its own directory:

```text
.research/<research-name>/
```

Use a short, descriptive `<research-name>` based on the topic, for example:

```text
.research/jev-pi-agent-kernel/
.research/agent-memory/
.research/tool-routing/
```

Do not store research findings directly in `.research/`.

For a new research task, create the appropriate `<research-name>/` directory and keep all research-specific notes, experiments, and results inside it.

A typical structure is:

```text
.research/<research-name>/
├── README.md
├── sources.md
├── findings.md
├── analysis.md
├── experiments.md
└── conclusions.md
```

Create additional files or subdirectories when useful.

## Persistent research memory

Research can consume a large amount of context. Do not keep the entire research process in the conversation context.

After every few meaningful research steps, write the important findings to the current research directory.

Persist:

- source URLs and source names
- important factual findings
- technical details
- measurements and benchmark results
- relevant code/repository locations
- competing claims
- unresolved questions
- hypotheses
- conclusions and supporting evidence

After information has been written to disk, use those files as the persistent research record instead of keeping everything in active context.

Before major synthesis or experimentation, reread the relevant research files rather than relying only on conversational memory.

## Research quality

Prefer primary sources:

1. official documentation and technical papers
2. original repositories and source code
3. independent benchmarks and experiments
4. technical analyses
5. community discussion

Do not treat vendor or author claims as established facts without checking their evidence.

Clearly distinguish:

- verified facts
- claimed results
- independent measurements
- inference
- speculation

For important quantitative claims, record the source and the conditions under which the number was measured.

Do not copy entire webpages or large amounts of irrelevant text into the research files. Preserve the useful information and enough context to understand it later.

## From research to validation

When researching a technology, architecture, optimization, or engineering technique, do not stop at explaining the concept.

After sufficient information has been collected:

1. determine whether the concept is applicable to the target system;
2. identify the smallest useful proof of concept;
3. implement an experiment when practical;
4. benchmark it against a baseline;
5. measure both benefits and regressions.

For AI/agent systems, evaluate both efficiency and capability. At minimum consider:

- token usage
- cost
- latency
- number of model calls
- context usage
- task success
- correctness/quality
- regressions and failure modes

A cost reduction is not considered a success if it comes from silently reducing quality, reasoning ability, or precision.

## Research completion

Keep:

```text
.research/<research-name>/conclusions.md
```

as the concise final record containing:

- what was discovered
- what was verified
- what was tested
- benchmark results
- limitations
- final technical conclusion

The final response should summarize the conclusion and reference the research files rather than reproducing the entire research corpus.
