# HighDPI design

## Background

-   macOS: Only integer scale factors at app level. Can stretch final composited framebuffer at very end for non-integer display scales.

    -   advantages: simple programming model, still fairly flexible; scaling at end produces coherent final image
    -   disadvantages: needs 4x memory even for 1.5x scale; scaling at very makes final image less detailed than it could be

-   Android: Many (but discrete) scale levels (ldpi (0.75x), mdpi, hdpi (1.5x), xhdpi (2x), xxhdpi (3x), xxhdpi (4x))

-   Windows: has "not recommended" free form text entry for scale factor between 100% and 500%.

Integer scale factors are needed in any case so let's get that working first. Actually, let's focus on just 2x for now.

## Desired end state

-   All rects (Window and Widget rects, mouse cursor, even bitmap sizes) are in "logical" coordinates, which is the same as pixels at 1x scale, as much as possible.
-   If something needs to be in pixels, its name starts with `physical_`. Physical coordinates should as much as possible not cross API boundaries.
-   Jury's still out if logical coordinates should stay ints. Probably, but it means mouse cursor etc only have point resolution, not pixel resolution
-   We should have something that can store a collection of (lazily-loaded?) bitmaps and fonts that each represent a single image / font at different scale levels, and at paint time the right representation is picked for the current scale

## Resource loading

Resources such as icons, cursors, bitmap fonts are scale-dependent: In HighDPI modes, different resources need to be loaded.

### Art direction

A 2x resource should look like a 1x resource, just with less jagged edges. A horizontal or vertical line that's 1 pixel wide in 1x should be 2 pixels wide in 2x.

A good guideline for black-and-white images: start with a 1x bitmap, resize it to 200% using nearest-neighbor filtering, and then move black pixels around to smooth diagonal edges -- but the number of black pixels shouldn't change relative to the 200% nearest-neighbor-resampled image. If that's not possible, err towards making the icon smaller instead of larger. A good technique is to use the Ctrl-Shift-Super-I shortcut in HighDPI mode to toggle between low-res and high-res icons in HighDPI mode.

While a 1x 32x32 bitmap and a 2x 16x16 bitmap both have 32x32 pixels, they don't have to look the same: The 2x 16x16 should look exactly like the corresponding 1x 16x16, just with smoother edges. The 1x 32x32 pixel resource could instead pick a different crop. As a concrete example, the 1x 7x10 ladybug emoji image currently just has the ladybug's shell (for space reasons), and so should the 2x version of that emoji. On the other hand, if we add a higher-res 1x 14x20 ladybug emoji at some point, we might want show the ladybug's legs on it, instead of just a smoother rendition of just the shell. (The 2x version of that 14x20 emoji would then have legs and shell in less jagged.)

### Directory structure

currently:

    res/
      cursors/
        arrowx2y2.png
        ...
      emoji/  (currently all 7x10 px)
        U+1F346.png
        ...
      fonts/
        CsillaRegular10.font
        ...
      graphics/
        brand-banner.png
      ...
      icons/
        16x16/
          small app icons, small filetype icons, toolbar icons, window buttons, ...
        32x32/
          large app icons, large filetype icons, message box icons
        various per-app folders with in-app UI images (XXX: maybe move into "apps" subdir?)
        themes/
          Coffee/
            16x16/
              custom window buttons
          (more themes)
      ...
      wallpapers/
        desktop wallpapers

Every one of these could grow a 2x variant (and if we do more scale factors later, even more variants).

Possible new structures:

1. Have "1x", "2x" folders right inside res and then mirror folder structures inside them:

    res/
    1x/
    cursors/
    16x16/
    emoji/
    ...
    2x/
    cursors/
    16x16/
    emoji/
    ...

2. Instead of having the 1x/2x fork at the root, have it at each leaf:

    res/
    cursors/
    1x/
    arrowx2y2.png
    ...
    2x/
    arrowx2y2.png
    ...
    emoji/
    1/
    U+1F346.png
    ...
    2/
    U+1F346.png
    ...
    ...

3. Use filename suffixes instead of directories (similar to macOS):

    res/
    cursors/
    arrowx2y2.png
    arrowx2y2@2x.png
    ...
    emoji/
    U+1F346.png
    U+1F346@2x.png
    ...

4. Use suffixes on directory instead of subdirectory:

    res/
    cursors/
    arrowx2y2.png
    ...
    cursors-2x/
    arrowx2y2.png
    ...

Root-level split makes it easy to see which scale factors exist and is subjectively aesthetically pleasing.

Filename suffixes make it easy to see which icons don't have high-res versions (but in return clutter up an icon directory), and it makes it easy to get the intrinsic scale factor of a bitmap (just need to look at the image's basename, not at any directory).

Android has additional modifiers in addition to scale factors in its resource system (UI language, light/dark mode, screen size in addition to resolution, etc). If we ever add more factors to the resource system, a suffix-based system would probably extend more nicely than a nesting-based one.

In the end probably doesn't matter all that much which version to pick.

For now, we're going with a "-2x" suffix on the file name.

### Resource loading strategy tradeoffs

-   eagerly load one scale, reload at new scale on scale factor change events

    -   needs explicit code
    -   random code in LibGfx currently loads icons, and scale factor change events would be more a LibGUI level concept, not clear how to plumb the event to there

    *   memory efficient -- only have one copy of each resource in memory
    *   easy to understand: Bitmap stays Bitmap, Font stays Font, no need for collections

-   have BitmapCollection that stores high-res and low-res path and load lazily when needed

    -   need to do synchronous disk access at first paint on UI thread
        -   or load compressed data at each scale eagerly and decompress lazily. still a blocking decode on UI thread then, and needs more memory -- 2x compressed resources in memory even if they might never be needed

    *   puts complexity in framework, app doesn't have to care
    *   can transparently paint UI at both 1x and 2x into different backbuffers (eg for multiple screens that have different scale factors)

-   eagerly load both and use the right one at paint time
    -   similar to GUI::Icon
    -   400% memory overhead in 1x mode (but most icons are small)
    *   conceptually easy to understand, but still need some collection class
    *   puts (less) complexity in framework, app doesn't have to care
    *   can transparently paint UI at both 1x and 2x into different backbuffers (eg for multiple screens that have different scale factors)

This isn't figured out yet, for now we're doing the first approach in select places in the window server.

### Resource loading API

Currently:

    auto app_icon = GUI::Icon::default_icon("app-gml-playground");

or

    s_unfilled_circle_bitmap = Bitmap::load_from_file("/res/icons/serenity/unfilled-radio-circle.png");

or

    header.set_font(Gfx::Font::load_from_file("/res/fonts/PebbletonBold14.font"));

Going forward:

    FIXME (depends on loading strategy decision a bit?)

## Implementation plan

The plan is to have all applications use highdpi backbuffers eventually. It'll take some time to get there though, so here's a plan for getting there incrementally.

0. Add some scaling support to Painter. Make it do 2x nearest neighbor scaling of everything at paint time for now.
1. Add scale factor concept to WindowServer. WindowServer has a scaled framebuffer/backbuffer. All other bitmaps (both other bitmaps in WindowServer, as well as everything WindowServer-client-side) are always stored at 1x and scaled up when they're painted to the framebuffer. Things will look fine at 2x, but pixely (but window gradients will be smooth already).
2. Let DisplaySettings toggle it WindowServer scale. Now it's possible to switch to and from HighDPI dynamically, using UI.
3. Come up with a system to have scale-dependent bitmap and font resources. Use that to use a high-res cursor bitmaps and high-res menu bar text painting in window server. Menu text and cursor will look less pixely. (And window frames too, I suppose.)
4. Let apps opt in to high-res window framebuffers, and convert all apps one-by-one
5. Remove high-res window framebuffer opt-in since all apps have it now.

We're currently in the middle of point 3. Some window server icons are high-resolution, but fonts aren't yet, and in-window-server things with their own backing store (eg menus) aren't yet either.
