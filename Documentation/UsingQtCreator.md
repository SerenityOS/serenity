## Using Qt Creator for working with SerenityOS

First, make sure you have a working toolchain and can build and run SerenityOS. Go [here](https://github.com/SerenityOS/serenity/blob/master/Documentation/BuildInstructions.md) for instructions for setting that up.

* Install [Qt Creator](https://www.qt.io/offline-installers). You don't need the entire Qt setup, just click 'Qt Creator' on the left side, and install that.
* Open Qt Creator, select `File -> New File or Project...`
* Select `Import Existing Project`
* Give it a name, and navigate to the root of your SerenityOS project checkout. Click Next.
* Wait for the file list to generate. This can take a minute or two!
* Ignore the file list, we will overwrite it later. Click Next.
* Set `Add to version control` to `<None>`. Click Finish.
* In your shell, go to your SerenityOS project directory, and invoke the `Meta/refresh-serenity-qtcreator.sh` script to regenerate the `serenity.files` file. You will also have to do this every time you delete or add a new file to the project.
* Edit the `serenity.config` file (In Qt Creator, hit ^K or CMD+K on a Mac to open the search dialog, type the name of the file and hit return to open it)
* Add the following `#define`s to the file: `DEBUG`, `SANITIZE_PTRS`, and `KERNEL`. Depending on what you are working on, you need to have that last define commented out. If you're planning on working in the userland, comment out `#define KERNEL`. If you're working on the Kernel, then uncomment `#define KERNEL`.
* Edit the `serenity.cxxflags` file to say `-std=c++2a -m32`
* Edit the `serenity.includes` file, add the following lines: `.`, `..`, `../..`, `Services/`, `Libraries/`, `Libraries/LibC/`, `Libraries/LibPthread/`, `Libraries/LibM/`, `Toolchain/Local/i686-pc-serenity/include/c++/10.1.0`, `Build/Services/`, `Build/Libraries/`, `AK/`

Qt Creator should be set up correctly now, go ahead and explore the project and try making changes. Have fun! :^)
