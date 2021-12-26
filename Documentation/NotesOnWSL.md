## Notes on WSL

SerenityOS can also be built and run under WSL Version 2.
WSL Version 1 is not supported since Version 1 does not support ext2, which is needed for the setup.

WSL Version 2 requires Windows version 2004 or higher, with OS Build 19041 or greater. Here is a guide on how to [get WSL2](https://docs.microsoft.com/en-us/windows/wsl/install-win10).
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

### Note on filesystems

WSL2 filesystem performance for IO heavy tasks (such as compiling a large C++ project) on the host Windows filesystem is terrible.
This is because WSL2 runs as a Hyper-V virtual machine and uses the 9p file system protocol to access host windows files, over Hyper-V sockets.

For a more in depth explaination of the technical limitations of their approach, see [this issue on the WSL github](https://github.com/microsoft/WSL/issues/4197#issuecomment-604592340)

The recommendation from the Microsoft team on that issue is:

> [I]f it's at all possible, store your projects in the Linux file system in WSL2.

In practice, this means cloning and building the project to somewhere such as `/home/username/serenity`.

If you're using the native Windows QEMU binary from the above steps, QEMU is not able to access the ext4 root partition of the
WSL2 installation without proper massaging. To avoid this, you might copy or symlink `Build/_disk_image` and `Build/Kernel/Kernel` to a native Windows partition (e.g. `/mnt/c`) before running the QEUMU launch commands in `Meta/CLion/run.sh`.
