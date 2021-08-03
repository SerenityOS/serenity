# Setting up a development environment on Windows

SerenityOS can be built and run under WSL Version 2.
WSL Version 1 is not supported since Version 1 does not support ext2, which is needed for the setup.

WSL Version 2 requires Windows 10 version 2004 or higher, with OS Build 19041 or greater. Here is a guide on how to
[get WSL2](https://docs.microsoft.com/en-us/windows/wsl/install-win10).

Once installed, you will need to make sure the distribution you want to use (and the new default) is using Version 2:
- `wsl -l -v` lists distros and versions,<br/>
- `wsl --set-version <distro> <version>` is used to convert a distro to another version, and<br/>
- `wsl --set-default-version 2` will set the default version for all new distros (if desired.)<br/>

## Note on filesystems

WSL2 filesystem performance for IO heavy tasks (such as compiling a large C++ project) on the host Windows filesystem is
pretty bad. See [this issue on the WSL GitHub project](https://github.com/microsoft/WSL/issues/4197#issuecomment-604592340)
for details.

The recommendation from the Microsoft team on that issue is:

> If it's at all possible, store your projects in the Linux file system in WSL2.

In practice, this means cloning and building the project to somewhere such as `/home/username/serenity`.

## Setting up build tools

Please see the general build instructions for a list of tools you need to install in your WSL Linux environment.

## Setting up QEMU

Grab the latest QEMU binaries from [here](https://www.qemu.org/download/#windows) and install them. At a minimum you
will need to install the tools as well as the system emulators for i386 and x86_64.

![QEMU Components](QEMU_Components.png)

Run `Meta/serenity.sh run` to build and run SerenityOS as usual.

### Hardware acceleration

The steps above will run QEMU in software virtualization mode, which is very slow.
QEMU supports hardware acceleration on Windows via the [Windows Hypervisor Platform](https://docs.microsoft.com/en-us/virtualization/api/)
(WHPX).

Enable the Windows Hypervisor Platform feature, either using "Turn Windows features on or off", or by running the
following command in an elevated PowerShell session: \
`dism /Online /Enable-Feature /All /FeatureName:HypervisorPlatform`

![WHPX Windows Feature](WHPX_Feature.png)

Set the `SERENITY_VIRT_TECH_ARG` environment variable in your WSL2 shell: \
`export SERENITY_VIRT_TECH_ARG="-accel whpx,kernel-irqchip=off"`

You might want to add those environment variables to your shell's configuration file, so that you don't have to set them
manually each time you start a new shell.

Start the VM with `Meta/serenity.sh run` as usual.
