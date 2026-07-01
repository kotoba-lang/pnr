# kotoba-lang/pnr

Zero-dep portable `.cljc` — restored from the legacy `kami-engine/kami-pnr`
Rust crate (deleted in kotoba-lang/kami-engine PR #82 "Remove Rust workspace
from kami-engine") as part of the **clj-wgsl migration** (ADR-2607010930,
`com-junkawasaki/root`).

Physical design backend for VLSI/ASIC layout.

| Namespace | Restored from | Purpose |
|---|---|---|
| `pnr.floorplan` | `floorplan` | Block placement, IO pin assignment, utilization analysis |
| `pnr.placement` | `placement` | Greedy left-to-right row-based standard cell placement |
| `pnr.cts` | `cts` | Clock tree synthesis via recursive bisection (balanced H-tree) |
| `pnr.routing` | `routing` | Lee/BFS maze routing on a multi-layer grid |
| `pnr.gdsii` | `gdsii` | GDSII binary stream export (JVM-only, `java.io`) |

Depends on `kotoba-lang/engineer` for shared contracts (constraint/DRC/etc).

## Status

Restored — all 5 modules ported from the original 1150-line Rust source
(`lib.rs` + `floorplan.rs` + `placement.rs` + `cts.rs` + `routing.rs` +
`gdsii.rs`), with all 10 original Rust unit tests mirrored 1:1 in
`test/pnr_test.cljc` (+1 smoke test). Pure data + pure functions throughout
(gdsii is JVM-only, `java.io.ByteArrayOutputStream`/`java.nio.ByteBuffer` —
a CLJS arm can be added if a browser consumer needs GDSII export).
`pnr.routing`'s occupancy/visited state uses sets of `[layer x y]` triples
rather than the original's nested 3D bool arrays — equivalent semantics,
more natural in Clojure.

## Develop

```bash
clojure -M:test
```
