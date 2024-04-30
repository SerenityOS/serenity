# Qt Creator Project Configuration

## Setup

First, make sure you have a working toolchain and can build and run SerenityOS. Go [here](BuildInstructions.md) for instructions for setting that up.

* Install [Qt Creator](https://www.qt.io/offline-installers). You don't need the entire Qt setup, just click 'Qt Creator' on the left side, and install that.
* Open Qt Creator, select `File -> New File or Project...`
* Select `Import Existing Project`
* Give it a name (some tools assume lower-case `serenity`), and navigate to the root of your SerenityOS project checkout. Click Next.
* Wait for the file list to generate. This can take a minute or two!
* Ignore the file list, we will overwrite it later. Click Next.
* Set `Add to version control` to `<None>`. Click Finish.
* In your shell, go to your SerenityOS project directory, and invoke the `Meta/refresh-serenity-qtcreator.sh` script to regenerate the `serenity.files` file. You will also have to do this every time you delete or add a new file to the project.
* Edit the `serenity.config` file (In Qt Creator, hit ^K or CMD+K on a Mac to open the search dialog, type the name of the file and hit return to open it)
* Add the following `#define`s to the file:
    ```
    //#define KERNEL

    #define ENABLE_UNICODE_DATA 1
    //#define ENABLE_COMPILETIME_FORMAT_CHECK

    #define __serenity__
    #define SANITIZE_PTRS 1
    #define __SSE__
    ```
    If you're working on the Kernel, just uncomment `#define KERNEL`.
* Edit the `serenity.cxxflags` file to say `-std=c++23 -fsigned-char -fconcepts -fno-exceptions -fno-semantic-interposition -fPIC`
* Edit the `serenity.includes` file to list the following lines:
    ```
    ./
    Userland/
    Userland/Libraries/
    Userland/Libraries/LibC/
    Userland/Libraries/LibSystem/
    Userland/Services/
    Toolchain/Local/x86_64/x86_64-pc-serenity/include/c++/13.1.0
    Build/x86_64/
    Build/x86_64/Userland/
    Build/x86_64/Userland/Libraries/
    Build/x86_64/Userland/Services/
    AK/
    ```

Finally, search in the options for "BOM" (Text Editor > Behavior > File Encodings > UTF-8 BOM), and switch to "Always delete".

Qt Creator should be set up correctly now, go ahead and explore the project and try making changes. Have fun! :^)

## Auto-Formatting

You can use `clang-format` to help you with the [style guide](CodingStyle.md). Before you proceed, check that you're actually using clang-format version 18, as some OSes will ship older clang-format versions by default.

- In QtCreator, go to "Help > About Plugins…"
- Find the `Beautifier (experimental)` row (for example, by typing `beau` into the search)
- Put a checkmark into the box
- Restart QtCreator if it asks you
- In QtCreator, go to "Tools > Options…"
- Type "beau" in the search box, go to "Beautifier > Clang Format"
- Select the "customized" style, click "edit"
- Paste the entire content of the file `.clang-format` into the "value" box, and click "OK"
- In the "Beautifier > General" tab, check "Enable auto format on file save"
- Select the tool "ClangFormat" if not already selected, and click "OK"

Note that not the entire project is clang-format-clean (yet), so sometimes you will see large diffs.
Use your own judgement whether you want to include such changes. Generally speaking, if it's a few lines then it's a good idea; if it's the entire file then maybe there's a better way to do it, like doing a separate commit, or just ignoring the clang-format changes.

You may want to read up what `git add -p` does (or `git checkout -p`, to undo).

QtCreator tends to interpret IPC definitions as C++ headers, and then tries to format them. This is not useful. One way to avoid that is telling QtCreator that IPC definitions are not C++ headers.
- In QtCreator, go to "Tools > Options…"
- Type "beau" in the search box, go to "Environment > MIME Types"
- In the little search box, type "plain", and select "text/plain"
- In the "details" section, you should now see the list of Patterns, something like `*.txt;*.asc;*,v`. Extend it in the following way: `*.txt;*.asc;*,v;*.ipc;*.gml`
- Click "OK" to close the dialog.
- Maybe you need to close and open again the IPC files. You can check what QtCreator is doing by right-clicking the filename in the editor tab, and clicking "Properties...". In the third line, you should see `MIME type: text/plain`.

## License template

You may have noticed how Andreas just types `lic` and the [license appears](https://youtu.be/i0J6J1Twwyo?t=346).

In order to so, create a new file anywhere, for example `license-template.creator`, with the standard license:

```
/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
```

In QtCreator, select the menu "Tools", item "Options", section "C++", tab
"File Naming" (don't ask me why it's here). At the bottom there should be the
option "License template:". Click "Browse…", select your file (i.e.,
`license-template.creator`). Click "OK", and you're done! :)

## Compiler Kits

You can slightly improve how well Qt interprets the code by adding and setting up an appropriate "compiler kit".
For that you will need to reference the compilers at `Toolchain/Local/x86_64/bin/x86_64-pc-serenity-gcc` and `Toolchain/Local/x86_64/bin/x86_64-pc-serenity-g++`.
