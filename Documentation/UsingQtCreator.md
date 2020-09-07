# Using Qt Creator for working with SerenityOS

## Setup

First, make sure you have a working toolchain and can build and run SerenityOS. Go [here](https://github.com/SerenityOS/serenity/blob/master/Documentation/BuildInstructions.md) for instructions for setting that up.

* Install [Qt Creator](https://www.qt.io/offline-installers). You don't need the entire Qt setup, just click 'Qt Creator' on the left side, and install that.
* Open Qt Creator, select `File -> New File or Project...`
* Select `Import Existing Project`
* Give it a name (some tools assume lower-case `serenity`), and navigate to the root of your SerenityOS project checkout. Click Next.
* Wait for the file list to generate. This can take a minute or two!
* Ignore the file list, we will overwrite it later. Click Next.
* Set `Add to version control` to `<None>`. Click Finish.
* In your shell, go to your SerenityOS project directory, and invoke the `Meta/refresh-serenity-qtcreator.sh` script to regenerate the `serenity.files` file. You will also have to do this every time you delete or add a new file to the project.
* Edit the `serenity.config` file (In Qt Creator, hit ^K or CMD+K on a Mac to open the search dialog, type the name of the file and hit return to open it)
* Add the following `#define`s to the file: `DEBUG`, `SANITIZE_PTRS`, and `KERNEL`. Depending on what you are working on, you need to have that last define commented out. If you're planning on working in the userland, comment out `#define KERNEL`. If you're working on the Kernel, then uncomment `#define KERNEL`.
* Edit the `serenity.cxxflags` file to say `-std=c++2a -m32`
* Edit the `serenity.includes` file, add the following lines: `.`, `..`, `../..`, `Services/`, `Libraries/`, `Libraries/LibC/`, `Libraries/LibPthread/`, `Libraries/LibM/`, `Toolchain/Local/i686-pc-serenity/include/c++/10.1.0`, `Build/Services/`, `Build/Libraries/`, `AK/`

Qt Creator should be set up correctly now, go ahead and explore the project and try making changes. Have fun! :^)

## Auto-Formatting

You can use `clang-format` to help you with the [style guide](https://github.com/SerenityOS/serenity/blob/master/Documentation/CodingStyle.md). Before you proceed, check that you're actually using clang-format version 10, as some OSes still ship clang-format version 9 by default.

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

## Compiler Kits

You can slightly improve how well Qt interprets the code by adding and setting up an appropriate "compiler kit".
For that you will need to reference the compilers at `Toolchain/Local/bin/i686-pc-serenity-gcc` and `Toolchain/Local/bin/i686-pc-serenity-g++`.
