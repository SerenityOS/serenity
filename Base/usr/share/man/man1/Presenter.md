## Name

![Icon](/res/icons/16x16/app-display-settings.png) Presenter - Present slides to an audience

## Synopsis

```**sh
$ Presenter [file]
```

## Description

Presenter is a simple slide presentation software, capable of displaying presentations stored in a simple format. It provides a simple user interface and experience that is specifically aimed at "getting out of the way" while you are giving the presentation to a live audience.

### Opening Files

Use **File -> Open...** to open a presentation in Presenter, or specify it on the command line (see [Synopsis](#synopsis)). The file format currently understood by Presenter is based on JSON and explained in [presenter(5)](help://man/5/presenter).

### Terminology

-   The **display area** is the window of Presenter, or the entire screen in full screen mode, where the presentation is visible.
-   A **slide** is a single page of the presentation and the most top-level structure.
-   A **frame** is a possibly animated step within a slide.

### Controlling the Presentation

During the presentation, the following keybindings are always available, though there are also corresponding menu options. Some of these keybindings mirror functionality of other common presentation software, though they don't usually behave exactly the same.

-   Right, Down, Space, Enter: Next slide or frame
-   Left, Up, Backspace: Previous slide or frame
-   B: Black-out display. Going forward or back a slide or frame or pressing Escape will disable the black-out and resume the presentation.
-   W: White-out display. The behavior is identical to black-out.
-   Escape: Exit full-screen mode.
-   F11, Shift + F5: Enter full-screen mode.
-   F5: Return to first slide and enter full-screen mode.
-   Typing a number followed by Enter will go to the first frame of that slide.

### Presentation Appearance

The presentation's appearance is mostly determined by the presentation itself, but it can also be customized in Presenter's settings. For the footer, you can use the following placeholders both in Presenter settings and in presentation files themselves:

-   {presentation_title}: Possibly empty title of presentation
-   {slide_title}: Possibly empty title of slide
-   {author}: Possibly empty slide author
-   {slides_total}: Total number of slides in the presentation
-   {frames_total}: Total number of frames in the presentation (across all slides)
-   {date}: Possibly empty modification date of the presentation
-   {slide_number}: Slide number
-   {slide_frame_number}: Number of frame within slide
-   {slide_frames_total}: Total number of frames within the current slide
-   {frame_number}: Counts all frames on all slides

## See Also

-   [presenter(5)](help://man/5/presenter) for the file format
