## CLion Project Configuration

Configuring CLion for development requires a CMakeLists.txt file. The one provided in Meta/CLion packs the bare minimum configuration so that you can take advantage of code completion and basic error checking. This is not necessarily the best setup, there could be better ways to configure the IDE. Below instructions are intended for people not familiar with CMake, or who don't want to get bothered setting up the IDE first.

It's assumed that a directory named `serenity` contains all your source files. If you cloned the repository directly from GitHub, this is the directory that git creates to copy sources.

- Create the project directory and cd to it: `mkdir serenity-project && cd serenity-project`
- Move `serenity` to the project directory: `mv <path/to/serenity> .`
- Copy CMake file to project directory: `cp serenity/Meta/CLion/CMakeLists.txt .`
- Create new CMake project in IDE: `File->New CMake Project from Sources`
- In the file selection dialog, find and select `serenity-project`, then click OK.

The project will start loading. Building the index for the first time takes a while. Let it finish the first scan. Once it's done, you can start hacking :)

**Note:** Don't create a new git repo in `serenity-project`. The whole point of wrapping the sources to a new directory is to keep the project files created by CLion away from the main repository (the `.git` in `serenity`).  

**Note:** If you don't want the workspace to list files ending with certain extensions, go to `File->Settings->Editor->File Types` and add entries in the `Ignore files and folders` field.

**Note:** If you have created the project after building serenity, you may get link errors on `makeall.sh` after moving `serenity` to the project directory. If you move `serenity` out to its original location, the build will succeed. It is recommended that you create a simple script that merely moves `serenity` before attempting to rebuild and once the build finishes, moves `serenity` back to the project directory. It's inconvenient, but kind of necessary.
