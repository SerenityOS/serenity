## Notes on WSL

SerenityOS can also be built and run under WSL Version 2. WSL Version 1 is not supported since Version 1 does not support ext2, which is needed for the setup.

WSL Version 2 is currently only available for Insider Builds of Windows which are more unstable and prone to crashes. Therefore, running SerenityOS on WSL Version 2 and running Insider Builds, in general, is not recommended on production systems.

Nevertheless, here is a guide on how to [get an Insider Build](https://insider.windows.com/en-us/how-to-pc/) and how to [get WSL2](https://docs.microsoft.com/en-us/windows/wsl/wsl2-install). The installation then proceeds as usual.

WSL2 does not natively support graphical applications. This means that to actually **./run** SerenityOS, you need an X Server for windows. [Vcxsrv](https://sourceforge.net/projects/vcxsrv/) is a good option. When you start up Vcxsrv, make sure to set the Display number to 0, and to Disable access control. Before actually doing **./run**, you need to set the DISPLAY environmental variable as such:

```bash
export DISPLAY=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):0
```
This is due to a bug in WSL2. For more information, microsoft/WSL#4106.

Now you can finally, **./run**.
