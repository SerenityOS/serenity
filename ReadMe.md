# Serenity

Graphical Unix-like operating system for x86 computers.

![Travis CI status](https://api.travis-ci.com/SerenityOS/serenity.svg?branch=master)

## About

*I always wondered what it would be like to write my own operating system, but I never took it seriously. Until now.*

Serenity is a love letter to '90s user interfaces with a custom Unix-like core. It flatters with sincerity by stealing beautiful ideas from various other systems.

Roughly speaking, the goal is a marriage between the aesthetic of late-1990s productivity software and the power-user accessibility of late-2000s \*nix. This is a system by me, for me, based on the things I like.

If you like some of the same things, you are welcome to join the project. It would be great to one day change the above to say "this is a system by us, for us, based on the things we like." :^)

I regularly post raw hacking sessions and demos on [my YouTube channel](https://www.youtube.com/c/AndreasKling/).

Sometimes I write about about the system on [my github.io blog](https://awesomekling.github.io/).

There's also a [Patreon](https://www.patreon.com/serenityos) if you would like to show some support that way.

## Screenshot

![Screenshot as of 191112e](https://raw.githubusercontent.com/SerenityOS/serenity/master/Meta/screenshot-191112e.png)

## Current features

* Pre-emptive multitasking
* Multithreading
* Compositing window server
* IPv4 networking with ARP, TCP, UDP and ICMP
* ext2 filesystem
* Unix-like libc and userland
* POSIX signals
* Shell with pipes and I/O redirection
* mmap()
* /proc filesystem
* Local sockets
* Pseudoterminals (with /dev/pts filesystem)
* JSON framework
* Low-level utility library (LibCore)
* High-level GUI library (LibGUI)
* Visual GUI design tool
* PNG format support
* Text editor
* IRC client
* Simple painting application
* DNS lookup
* Desktop games: Minesweeper and Snake
* Ports system (needs more packages!)
* Other stuff I can't think of right now...

## How do I build and run this?

Make sure you have all the dependencies installed:

```
sudo apt install libmpfr-dev libmpc-dev libgmp-dev e2fsprogs qemu-system-i386 qemu-utils
```

Go into the `Toolchain/` directory and run the **BuildIt.sh** script. Then ***source*** the **UseIt.sh** script to put the `i686-pc-serenity` toolchain in your `$PATH`.

Once you've done both of those, go into the `Kernel/` directory, then run
**./makeall.sh**, and if nothing breaks too much, take it for a spin by using
**./run**.

Later on, when you `git pull` to get the latest changes, there's no need to rebuild the toolchain. You can simply rerun **./makeall.sh** in the `Kernel/` directory and you'll be good to **./run** again.

## Notes on WSL

SerenityOS can also be build and run under WSL Version 2. WSL Version 1 is not supported since Version 1 does not support ext2, which is needed for the setup.

WSL Version 2 is currently only available for Insider Builds of Windows which are more unstable and prone to crashes. Therefore, running SerenityOS on WSL Version 2 and running Insider Builds, in general, is not recommended on production systems.

Nevertheless, here is a guide on how to [get an Insider Build](https://insider.windows.com/en-us/how-to-pc/) and how to [get WSL2](https://docs.microsoft.com/en-us/windows/wsl/wsl2-install). The installation then procedes as usual.

WSL2 does not natively support graphical applications. This means that to actually **./run** SerenityOS, you need an X Server for windows. [Vcxsrv](https://sourceforge.net/projects/vcxsrv/) is a good option. When you start up Vcxsrv, make sure to set the Display number to 0, and to Disable access control. Before actually doing **./run**, you need to set the DISPLAY environmental variable as such:

```
export DISPLAY=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}'):0
```
This is due to a bug in WSL2. For more information, microsoft/WSL#4106.

Now you can finally, **./run**.

## IRC

Come chat in `#serenityos` on the Freenode IRC network.

## Author

* **Andreas Kling** - [awesomekling](https://twitter.com/awesomekling)

## Contributors

* **Robin Burchell** - [rburchell](https://github.com/rburchell)

Feel free to append yourself here if you've made some sweet contributions. :)

## License

Serenity is licensed under a 2-clause BSD license.

