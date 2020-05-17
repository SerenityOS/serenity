## Notes on WSL

SerenityOS can also be built and run under WSL Version 2.
WSL Version 1 is not supported since Version 1 does not support ext2, which is needed for the setup.

WSL Version 2 is currently only available for Insider Builds of Windows which are more unstable and prone to crashes.
Therefore, running SerenityOS on WSL Version 2 and running Insider Builds, in general, is not recommended on production systems.

Nevertheless, here is a guide on how to [get an Insider Build](https://insider.windows.com/en-us/how-to-pc/) and how to [get WSL2](https://docs.microsoft.com/en-us/windows/wsl/wsl2-install).
Once installed, you will need to make sure the distribution you want to use (and the new default) is using Version 2:
- `wsl -l -v` will list distros and versions,<br/>
- `wsl --set-version <distro> <version>` is used to convert a distro to another version, and<br/>
- `wsl --set-default-version 2` will set the default version for all new distros (if desired.)<br/>

The installation then proceeds as usual.

WSL2 does not natively support graphical applications.
You can either install qemu natively on windows and allow WSL to talk to it, or you can install an X Server for windows.

### Setting up an X server with WSL:

- Install [Vcxsrv](https://sourceforge.net/projects/vcxsrv/) on Windows.
- When you start up Vcxsrv, make sure to set the Display number to 0, and to Disable access control.
- Before actually doing **make run**, you need to set the DISPLAY environmental variable as such:

```bash
export DISPLAY=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):0
```
This is due to a bug in WSL2. For more information, microsoft/WSL#4106.
- Connect to the window server from WSL.

Now you can finally, **make run**.

### Using native Qemu install with WSL:

- Grab latest qemu from [here](https://www.qemu.org/download/#windows) and install for windows.
- Locate the executable `qemu-system-i386.exe` in WSL.
In a 64-bit machine, it's located at `/mnt/c/Program Files/qemu/qemu-system-i386.exe`.

- Edit `serenity/Meta/CLion/run.sh`. Set **SERENITY_QEMU_BIN**  to point to the windows installation of `qemu-system-i386.exe`.
Also verify that the value of **SERENITY_BUILD** is valid.
In a 64-bit machine, if qemu was installed in the default location you shouldn't need to alter anything.

- Execute `serenity/Meta/CLion/run.sh`.
