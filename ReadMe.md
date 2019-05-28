# Serenity

Graphical Unix-like operating system for x86 computers.

![Travis CI status](https://api.travis-ci.com/SerenityOS/serenity.svg?branch=master)

## About

*I always wondered what it would be like to write my own operating system, but I never took it seriously. Until now.*

Serenity is a love letter to '90s user interfaces, with a custom Unix-like core. It flatters with sincerity by stealing beautiful ideas from various other systems.

Roughly speaking, the goal is a marriage between the aesthetic of late-1990s productivity software and the power-user accessibility of late-2000s \*nix. This is a system by me, for me, based on the things I like.

If you like some of the same things, you are welcome to join the project. It would be great to one day change the above to say "this is a system by us, for us, based on the things we like." :^)

I regularly post raw hacking sessions and demos on [my YouTube channel](https://www.youtube.com/channel/UC3ts8coMP645hZw9JSD3pqQ).

There's also a [Patreon](https://www.patreon.com/serenityos) if you would like to show some support that way.

## Screenshot

![Screenshot as of d727005](https://raw.githubusercontent.com/awesomekling/serenity/master/Meta/screenshot-d727005.png)

## Current features

* Pre-emptive multitasking
* Multithreading
* Compositing window server
* IPv4 networking with ARP, TCP, UDP and ICMP
* ext2 filesystem
* Unix-like libc and userland
* Shell with pipes and I/O redirection
* mmap()
* /proc filesystem
* Local sockets
* Pseudoterminals (with /dev/pts filesystem)
* Event loop library (LibCore)
* High-level GUI library (LibGUI)
* Visual GUI design tool
* PNG format support
* Text editor
* IRC client
* DNS lookup
* Desktop games: Minesweeper and Snake
* Other stuff I can't think of right now...

## How do I build and run this?

Go into the Toolchain/ directory and run the **BuildIt.sh** script. Then source the **UseIt.sh** script to put the i686-pc-serenity toolchain in your $PATH.

Once you've done both of those, go into the Kernel directory, then run
**makeall.sh**, and if nothing breaks too much, take it for a spin by using
**run**.

Otherwise, see the older [step-by-step guide to building Serenity](https://github.com/awesomekling/serenity/blob/master/Meta/BuildInstructions.md)

## IRC

Come chat in `#serenityos` on the Freenode IRC network.

## Author

* **Andreas Kling** - [awesomekling](https://github.com/awesomekling)
* **Robin Burchell** - [rburchell](https://github.com/rburchell)

## License

Serenity is licensed under a 2-clause BSD license.
