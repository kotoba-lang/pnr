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
| `pnr.lef-adapter` | — (new) | Resolves `kotoba-lang/org-si2-lef` (LEF) cell geometry into placement site-widths and real GDSII boundary elements |
| `pnr.def-adapter` | — (new) | Converts placement output into `kotoba-lang/org-si2-def` (DEF) component records |
| `pnr.upf-adapter` | — (new) | Power-domain-aware placement validation via `kotoba-lang/org-ieee-upf` (UPF power intent) |

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

## LEF / DEF adapters

`pnr.placement` previously only knew abstract `:width-sites` counts
(hand-picked by whoever built the netlist), and `pnr.gdsii/export-gdsii`
required the caller to already have fully-resolved geometry — neither
knew anything about a real physical cell library. Two adapters close that
gap, wiring in the two `kotoba-lang` standards-substrate repos for
physical libraries (LEF) and placed-design exchange (DEF):

- **`pnr.lef-adapter`** (`kotoba-lang/org-si2-lef`) — `macro->site-width`
  turns a LEF macro's real `:size` (physical microns) into the number of
  placement sites it occupies, so `netlist-cell-from-lef` can build
  `pnr.placement/place-cells` input from an actual cell library instead
  of guessed widths. `placed-cell->gdsii-elements` /
  `design->gdsii-structure` then turn placed cells + their LEF macros into
  real `pnr.gdsii/export-gdsii`-ready boundary elements (cell outline +
  one boundary per pin PORT rect, on a simple metal-layer -> GDSII-layer
  map), translated to each cell's placement offset and scaled to GDSII
  database units. First-pass simplification, documented in the
  docstring: cell **orientation (R0/MX/MY/etc) is not applied to the
  geometry** — only translation. A mirrored/rotated cell's local geometry
  is emitted un-mirrored/un-rotated, just moved to the right place.
- **`pnr.def-adapter`** (`kotoba-lang/org-si2-def`) — `placed-cell->def-component`
  / `placement->def-design` convert `pnr.placement/place-cells` output
  into `def-format.component` records and a DEF-shaped design map
  (DESIGN + COMPONENTS; `:nets`/`:tracks` left empty, out of scope for
  this adapter), giving `pnr` a standard, more interoperable DEF export
  path alongside its GDSII-only output.

## UPF adapter (power-domain-aware placement)

`pnr.placement` and `pnr.floorplan` previously knew nothing about power
intent — no notion that two cells might belong to different power
domains, and no way for a floorplan block to declare one. Power-domain-
aware floorplanning/placement is a standard EDA requirement (cells in
different power domains must not violate domain boundaries, and
level-shifters/isolation cells are needed at domain crossings), so
**`pnr.upf-adapter`** (`kotoba-lang/org-ieee-upf`) closes that gap:

- `cell-power-domain` resolves one placed cell's UPF power domain by
  running its `:instance-name` through `upf.domain/instance-domain`'s
  longest-prefix instance-path match against a UPF domain registry.
- `partition-by-domain` groups a `pnr.placement/place-cells` result by
  resolved domain, `{domain-name [cells...] ... :unscoped [cells...]}`.
- `domain-crossing-nets` takes a `net->pins` map (`{net-name
  [instance-name ...] ...}`) and reports every net that connects cells in
  more than one resolved domain — exactly the nets a real UPF flow needs
  isolation cells or level shifters on — bundling in the
  `upf.strategy/applicable-strategies` that apply to each domain the net
  touches.
- `validate-domain-placement` checks `pnr.floorplan` blocks: a block
  opts into a domain policy via an (adapter-level, `pnr.floorplan`-
  agnostic) optional `:power-domain` key, and every placed cell
  geometrically inside that block must resolve to the same domain
  (an unscoped cell inside a domain-scoped block is a violation; a
  block with no `:power-domain` is never checked). Returns a vector of
  violation maps, empty when placement is domain-consistent.

## Develop

```bash
clojure -M:test
```
