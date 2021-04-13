## Name

bt - print a backtrace of specified process

## Synopsis

```**sh
$ bt <pid>
```

## Description

This program will display a backtrace (virtual address, function name and source code location if possible) of specified process. It uses SymbolServer for out-of-process symbolication.

The kernel addresses will be changed to `0xdeadc0de` if called as non-superuser. Otherwise, they will display, but without function names or source code locations.

## Arguments

* `pid`: PID of process

## Files

* `/tmp/portal/symbol`: to communicate with SymbolServer
* `/usr/src`: to find relevant source files

## Examples

```sh
# Print backtrace of PID 22 (Shell in this example)
$ bt 22
0xdeadc0de  
0xdeadc0de  
0xdeadc0de  
0xdeadc0de  
0xdeadc0de  
0xdeadc0de  
0xdeadc0de  
0xdeadc0de  
0xdeadc0de  
0x0003b81e  pselect (select.cpp:50)
0x0003b8c4  select (select.cpp:44)
0x0001f217  Core::EventLoop::wait_for_event(Core::EventLoop::WaitMode) (EventLoop.cpp:673)
0x0001faee  Core::EventLoop::pump(Core::EventLoop::WaitMode) (NonnullOwnPtr.h:124)
0x0009e930  Shell::Shell::block_on_job(AK::RefPtr<Shell::Job, AK::RefPtrTraits<Shell::Job> >) (Shell.cpp:1079)
0x000b1e4f  Shell::Shell::run_commands(AK::Vector<Shell::AST::Command, 0ul>&) (Atomic.h:266)
0x0005294c  Shell::AST::Execute::for_each_entry(AK::RefPtr<Shell::Shell, AK::RefPtrTraits<Shell::Shell> >, AK::Function<AK::IterationDecision (AK::NonnullRefPtr<Shell::AST::Value>)>) (Vector.h:176)
0x00053680  Shell::AST::Execute::run(AK::RefPtr<Shell::Shell, AK::RefPtrTraits<Shell::Shell> >) (Atomic.h:266)
0x000aec9c  Shell::Shell::run_command(AK::StringView const&, AK::Optional<Shell::Shell::SourcePosition>) (Atomic.h:266)
0x000af924  Shell::Shell::read_single_line() [clone .localalias] (Optional.h:113)
0x000afa69  Shell::Shell::custom_event(Core::CustomEvent&) (Shell.cpp:1618)
0x0003662c  Core::Object::dispatch_event(Core::Event&, Core::Object*) (Object.h:103)
0x0001fe94  Core::EventLoop::pump(Core::EventLoop::WaitMode) (Atomic.h:266)
0x0002045b  Core::EventLoop::exec() (EventLoop.cpp:390)
0x00002d10  main (main.cpp:204)
0x00002dd5  _start (crt0_shared.cpp:60)
0x00029602  ELF::DynamicLinker::linker_main(AK::String&&, int, bool, int, char**, char**) (DynamicLinker.cpp:296)
0x0000157b  _start (main.cpp:136)
# Print backtrace of WindowServer (as root)
# bt $(pidof WindowServer)
0xc0540b31  
0xc037e8a1  
0xc038054c  
0xc0380c0c  
0xc0489682  
0xc04307b8  
0xc0429f6f  
0xc038c04b  
0xc038a061  
0x0003b81e  pselect (select.cpp:50)
0x0003b8c4  select (select.cpp:44)
0x0001f217  Core::EventLoop::wait_for_event(Core::EventLoop::WaitMode) (EventLoop.cpp:673)
0x0001faee  Core::EventLoop::pump(Core::EventLoop::WaitMode) (NonnullOwnPtr.h:124)
0x0002045b  Core::EventLoop::exec() (EventLoop.cpp:390)
0x0000a9dc  main (main.cpp:118)
0x0000ab25  _start (crt0_shared.cpp:60)
0x00029602  ELF::DynamicLinker::linker_main(AK::String&&, int, bool, int, char**, char**) (DynamicLinker.cpp:296)
0x0000157b  _start (main.cpp:136)
```
