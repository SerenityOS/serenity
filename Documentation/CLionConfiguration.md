## CLion Project Configuration

Configuring CLion for development requires a CMakeLists.txt file.
The one provided in Meta/CLion packs the bare minimum configuration required for code completion and basic error checking.
This is not necessarily the best setup, there could be better ways to configure the IDE.
Below instructions are intended for people not familiar with CMake, or who don't want to get bothered setting up the IDE first.

It's assumed that a directory named `serenity` contains all your source files.
If you cloned the repository directly from GitHub, this is the directory that git creates to copy sources.

- Create the project directory and cd to it: `mkdir serenity-project && cd serenity-project`
- Move `serenity` to the project directory: `mv <path/to/serenity> .`
- Copy CMake file to project directory: `cp serenity/Meta/CLion/CMakeLists.txt .`
- Create new CMake project in IDE: `File->New CMake Project from Sources`
- In the file selection dialog, find and select `serenity-project`, then click OK.

The project will start loading.
Building the index for the first time takes a while. Let it finish the first scan.

## Notes

**Note:** If you have created the project after building the toolchain, you may get errors on `make install` after moving `serenity` to the project directory.
If you move `serenity` out to its original location, the build will succeed again.
It is recommended that you create a script that merely moves `serenity` before attempting to rebuild and once the build finishes, moves `serenity` back to the project directory.
Also during the build, be careful not to switch to the IDE window.
Doing so will cause CLion to panic because the `serenity` directory has suddenly disappeared.
CLion will try to delete indexes and when the build finishes and `serenity` is moved back to the project directory, indexes will be built again. This is a waste of time.
In order to completely eliminate the rebuilding of indexes, you have to rebuild everything (toolchain included) from scratch while `serenity` resides in the project directory.

**Note:** Don't create a new git repo in `serenity-project`.
The whole point of wrapping the sources to a new directory is to keep the project files away from the main git repository.  

**Note:** If you don't want the workspace to list files ending with certain extensions, go to `File->Settings->Editor->File Types` and add entries to the `Ignore files and folders` field. 

## Notes for WSL Users

It is possible to set the embedded terminal in CLion to the one that your WSL distribution provides.
This way you can build and run serenity without leaving the IDE.
Note that following will only help if you don't use an X-window server to access qemu.
It is possible to install qemu natively on Windows and allow WSL to use it instead of installing qemu first on (wsl) linux and then use X server to launch serenity inside of it.
Check the updated manual [here](https://github.com/SerenityOS/serenity/blob/master/Documentation/NotesOnWSL.md).

- Locate the terminal emulator for your linux distribution.
Open CMD with elevated privileges and cd to `C:/Program Files/WindowsApps/`.
The directory is usually hidden and requires additional privileges. You should be able to cd as administrator.
`dir` and look for your distribution in directory names. In case of Ubuntu, it starts with `CanonicalGroupLimited.Ubuntu20.04onWindows_2004.2020.424.0_x64`.
cd to it. The directory should contain the shell executable. In my case it's named `ubuntu2004.exe`.
Copy `absolute/path/to/ubuntu2004.exe`.

- Go to your IDE settings: `File->Settings->Tools->Terminal` and paste the path you just copied to `shell path`. Click OK.

- Close CLion and restart.

The default IDE terminal should now be changed to WSL, and now you can run `CLion/run.sh`.
You may also want to copy `serenity/Meta/CLion/run.sh` to your project directory and run it from there, so that you don't have to fight with git every time you modify the script.
