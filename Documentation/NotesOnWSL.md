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
You can either install QEMU natively on windows and allow WSL to talk to it, or you can install an X Server for windows.

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

### Using native QEMU install with WSL:

- Grab latest QEMU from [here](https://www.qemu.org/download/#windows) and install for Windows.

- Locate the executable `qemu-system-i386.exe` in WSL.
By default this will be located at `/mnt/c/Program Files/qemu/qemu-system-i386.exe`.

- Set the `SERENITY_QEMU_BIN` environment variable to the location above. For example: \
`export SERENITY_QEMU_BIN='/mnt/c/Program Files/qemu/qemu-system-i386.exe'`

- Locate the _Windows_ path to the SerenityOS disk image, as native QEMU will be accessing it via the Windows filesystem. If your build tree is located in the WSL2 partition, this will be accessible under the `\\wsl$` network file share (see [notes below](#note-on-filesystems)).

- Set the `SERENITY_DISK_IMAGE` environment variable to the full path of the SerenityOS disk image file from above. For example: \
`export SERENITY_DISK_IMAGE='\\wsl$\Ubuntu-20.04\home\username\serenity\Build\_disk_image'`

- `make run` as usual.

#### Hardware acceleration

The steps above will run QEMU in software virtualisation mode, which is very slow.
QEMU supports hardware acceleration on Windows via the [Windows Hypervisor Platform](https://docs.microsoft.com/en-us/virtualization/api/) (WHPX), a user-mode virtualisation API that can be used alongside Hyper-V.
This is important to note as WSL2 itself runs on top of Hyper-V, which conflicts with other acceleration technologies such as Intel HAXM.

To run SerenityOS in a WHPX-enabled QEMU VM:

- If you have not already done so, enable Windows Hypervisor Platform, either using "Turn Windows features on or off", or by running the following command in an elevated PowerShell session: \
`dism /Online /Enable-Feature /All /FeatureName:HypervisorPlatform`

- Specify QEMU acceleration option: \
`export SERENITY_EXTRA_QEMU_ARGS="-accel whpx"`

- Disable Virtual Machine eXtensions on the vCPU, otherwise some versions of QEMU will crash out with a "WHPX: Unexpected VP exit code 4" error: \
`export SERENITY_QEMU_CPU="max,vmx=off"`

- `make run` as usual.

### Note on filesystems

WSL2 filesystem performance for IO heavy tasks (such as compiling a large C++ project) on the host Windows filesystem is terrible.
This is because WSL2 runs as a Hyper-V virtual machine and uses the 9P file system protocol to access host windows files, over Hyper-V sockets.

For a more in depth explanation of the technical limitations of their approach, see [this issue on the WSL github](https://github.com/microsoft/WSL/issues/4197#issuecomment-604592340)

The recommendation from the Microsoft team on that issue is:

> If it's at all possible, store your projects in the Linux file system in WSL2.

In practice, this means cloning and building the project to somewhere such as `/home/username/serenity`.

If you're using the native Windows QEMU binary from the above steps, QEMU is not able to access the ext4 root partition of the
WSL2 installation without going via the 9P network file share. The root of your WSL2 distro will begin at the network path `\\wsl$\{distro-name}`.

Alternatively, you may prefer to copy `Build/_disk_image` and `Build/Kernel/Kernel` to a native Windows partition (e.g. `/mnt/c`) before running `make run`, in which case `SERENITY_DISK_IMAGE` will be a regular Windows path (e.g. `'D:\serenity\_disk_image'`)
