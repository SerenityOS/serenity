## Name

![Icon](/res/icons/16x16/app-presenter.png) Presenter - Present slides to an audience

[Open](file:///bin/Presenter)

## Synopsis

```**sh
$ Presenter [file]
```

## Description

Presenter is a simple slide presentation application, capable of displaying presentations stored in a simple format. It provides a simple user interface that is specifically aimed at "getting out of the way" while you are giving a presentation to a live audience.

### Opening Files

Use `File → Open…` to open a presentation in Presenter, or specify it in the command line (see [Synopsis](#synopsis)). The file format currently understood by Presenter is based on JSON and explained in [presenter(5)](help://man/5/presenter).

### Terminology

-   The **display area** is the window of Presenter, or the entire screen in full screen mode, where the presentation is visible.
-   A **slide** is a single page of the presentation and the most top-level structure.
-   A **frame** is a possibly animated step within a slide.

### Controlling the Presentation

During the presentation, the following keybindings are always available, though there are also corresponding menu options. Some of these keybindings mirror the functionality of other presentation software, though they don't usually behave exactly the same.

-   `Right`, `Down`, `Space`, `Enter`: Next slide or frame
-   `Left`, `Up`, `Backspace`: Previous slide or frame
-   `B`: Black-out display. Going forward or back a slide or frame or pressing Escape will disable the black-out and resume the presentation.
-   `W`: White-out display. The behavior is identical to black-out.
-   `F11`, `Shift + F5`: Toggle full-screen mode.
-   `Escape`: Exit full-screen mode.
-   `F5`: Return to first slide and enter full-screen mode.
-   Typing a number followed by Enter will go to the first frame of that slide.

## See Also

-   [presenter(5)](help://man/5/presenter) for the file format
