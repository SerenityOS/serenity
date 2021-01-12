HighDPI design
==============

Background
----------

- macOS: Only integer scale factors at app level. Can stretch final composited framebuffer at very end for non-integer display scales.
  - advantages: simple programming model, still fairly flexible; scaling at end produces coherent final image
  - disadvantages: needs 4x memory even for 1.5x scale; scaling at very makes final image less detailed than it could be

- Android: Many (but discrete) scale levels (ldpi (0.75x), mdpi, hdpi (1.5x), xhdpi (2x), xxhdpi (3x), xxhdpi (4x))

- Windows: has "not recommended" free form text entry for scale factor between 100% and 500%.

Integer scale factors are needed in any case so let's get that working first. Actually, let's focus on just 2x for now.


Desired end state
-----------------

- Window and Widget rects are in "logical" coordinates, which is the same as pixels at 1x scale
- Same for mouse cursor
- Jury's still out if logical coordinates should stay ints. Probably, but it means mouse cursor etc only have point resolution, not pixel resolution
- We should have something that can store a collection of (lazily-loaded?) bitmaps and fonts that each represent a single image / font at different scale levels, and at paint time the right representation is picked for the current scale

Implementation plan
-------------------

The plan is to have all applications use highdpi backbuffers eventually. It'll take some time to get there though, so here's a plan for getting there incrementally.

0. Add some scaling support to Painter. Make it do 2x nearest neighbor scaling of everything at paint time for now.
1. Add scale factor concept to WindowServer and let DisplaySettings toggle it. WindowServer has a scaled framebuffer/backbuffer. All other bitmaps (both other bitmaps in WindowServer, as well as everything WindowServer-client-side) are always stored at 1x and scaled up when they're painted to the framebuffer. Things will look fine at 2x, but pixely (but window gradients will be smooth already).
2. Come up with a system to have scale-dependent bitmap and font resources. Use that to use a high-res cursor bitmaps and high-res menu bar text painting in window server. Menu text and cursor will look less pixely. (And window frames too, I suppose.)
3. Let apps opt in to high-res window framebuffers, and convert all apps one-by-one
4. Remove high-res window framebuffer opt-in since all apps have it now.

We're currently before point 0.
