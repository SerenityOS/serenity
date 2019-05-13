#!/bin/bash

sudo id

make_cmd="make -j3"

$make_cmd -C ../LibC clean && \
$make_cmd -C ../LibC && \
(cd ../LibC && ./install.sh) && \
$make_cmd -C ../LibM clean && \
$make_cmd -C ../LibM && \
(cd ../LibM && ./install.sh) && \
$make_cmd -C ../LibCore clean && \
$make_cmd -C ../LibCore && \
$make_cmd -C ../Servers/LookupServer clean && \
$make_cmd -C ../Servers/LookupServer && \
$make_cmd -C ../Servers/WindowServer clean && \
$make_cmd -C ../Servers/WindowServer && \
$make_cmd -C ../LibGUI clean && \
$make_cmd -C ../LibGUI && \
$make_cmd -C ../Userland clean && \
$make_cmd -C ../Userland && \
$make_cmd -C ../Applications/Terminal clean && \
$make_cmd -C ../Applications/Terminal && \
$make_cmd -C ../Applications/FontEditor clean && \
$make_cmd -C ../Applications/FontEditor && \
$make_cmd -C ../Applications/Launcher clean && \
$make_cmd -C ../Applications/Launcher && \
$make_cmd -C ../Applications/FileManager clean && \
$make_cmd -C ../Applications/FileManager && \
$make_cmd -C ../Applications/ProcessManager clean && \
$make_cmd -C ../Applications/ProcessManager && \
$make_cmd -C ../Applications/TextEditor clean && \
$make_cmd -C ../Applications/TextEditor && \
$make_cmd -C ../Applications/About clean && \
$make_cmd -C ../Applications/About && \
$make_cmd -C ../Applications/IRCClient clean && \
$make_cmd -C ../Applications/IRCClient && \
$make_cmd -C ../Applications/Taskbar clean && \
$make_cmd -C ../Applications/Taskbar && \
$make_cmd -C ../Applications/Downloader clean && \
$make_cmd -C ../Applications/Downloader && \
$make_cmd -C ../DevTools/VisualBuilder clean && \
$make_cmd -C ../DevTools/VisualBuilder && \
$make_cmd -C ../Games/Minesweeper clean && \
$make_cmd -C ../Games/Minesweeper && \
$make_cmd -C ../Games/Snake clean && \
$make_cmd -C ../Games/Snake && \
$make_cmd -C ../Shell clean && \
$make_cmd -C ../Shell && \
$make_cmd -C ../Demos/HelloWorld clean && \
$make_cmd -C ../Demos/HelloWorld && \
$make_cmd -C ../Demos/RetroFetch clean && \
$make_cmd -C ../Demos/RetroFetch && \
$make_cmd clean &&\
$make_cmd && \
sudo ./sync.sh

