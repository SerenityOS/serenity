## CLion Project Configuration

CLion can integrate with CMake to provide code comprehension features.

After opening the `serenity` repository in CLion as a new project, go to "`File->Settings->Build, Execution, Deployment->Cmake`",
and set the following fields: (Assuming you use `Ninja` as the build system and configured the CMake build directory to `Build`)

`CMake Options`: `-G Ninja -DBUILD_LAGOM=ON -DCMAKE_C_COMPILER=gcc-10 -DCMAKE_CXX_COMPILER=g++-10`

`Build Directory`: `Build`

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
