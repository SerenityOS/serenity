# SerenityOS

Graphical Unix-like operating system for x86 computers.

![Travis CI status](https://api.travis-ci.com/SerenityOS/serenity.svg?branch=master)

## About

SerenityOS is a love letter to '90s user interfaces with a custom Unix-like core. It flatters with sincerity by stealing beautiful ideas from various other systems.

Roughly speaking, the goal is a marriage between the aesthetic of late-1990s productivity software and the power-user accessibility of late-2000s \*nix. This is a system by me, for me, based on the things I like.

If you like some of the same things, you are welcome to join the project. It would be great to one day change the above to say "this is a system by us, for us, based on the things we like." :^)

I regularly post raw hacking sessions and demos on [my YouTube channel](https://www.youtube.com/c/AndreasKling/).

Sometimes I write about the system on [my github.io blog](https://awesomekling.github.io/).

I'm also on [Patreon](https://www.patreon.com/serenityos) and [GitHub Sponsors](https://github.com/sponsors/awesomekling) if you would like to show some support that way.

## Screenshot

![Screenshot as of 1133aca](https://raw.githubusercontent.com/SerenityOS/serenity/master/Meta/screenshot-1133aca.png)

## Current features (all under development, some more mature than others)

* Pre-emptive multitasking
* Multithreading
* Compositing window server
* IPv4 networking with ARP, TCP, UDP and ICMP
* ext2 filesystem
* Unix-like libc and userland
* POSIX signals
* Shell with pipes and I/O redirection
* mmap()
* Purgeable memory
* /proc filesystem
* Local sockets
* Pseudoterminals (with /dev/pts filesystem)
* Filesystem notifications
* JSON framework
* Low-level utility library (LibCore)
* Mid-level 2D graphics library (LibDraw)
* High-level GUI library (LibGUI)
* HTML/CSS engine
* Web browser
* C++ IDE
* Sampling profiler with GUI
* Emojis (UTF-8)
* HTTP downloads
* SoundBlaster 16 driver
* Software-mixing sound daemon
* WAV playback
* Simple desktop piano/synthesizer
* Visual GUI design tool
* PNG format support
* Text editor
* IRC client
* Simple painting application
* DNS lookup
* Desktop games: Minesweeper and Snake
* Color theming
* Ports system (needs more packages!)
* Other stuff I can't think of right now...

## How do I build and run this?

See the [https://github.com/SerenityOS/serenity/blob/master/Documentation/BuildInstructions.md](SerenityOS build instructions).

## Wanna talk?

Come chat with us in `#serenityos` on the Freenode IRC network.

## Author

* **Andreas Kling** - [awesomekling](https://twitter.com/awesomekling)

## Contributors

* **Robin Burchell** - [rburchell](https://github.com/rburchell)
* **Conrad Pankoff** - [deoxxa](https://github.com/deoxxa)
* **Sergey Bugaev** - [bugaevc](https://github.com/bugaevc)

(And many more!) Feel free to append yourself here if you've made some sweet contributions. :)

## License

SerenityOS is licensed under a 2-clause BSD license.
