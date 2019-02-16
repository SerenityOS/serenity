#!/bin/bash

sudo id

make_cmd="make -j2"

$make_cmd -C ../LibC clean && \
$make_cmd -C ../LibC && \
$make_cmd -C ../LibGUI clean && \
$make_cmd -C ../LibGUI && \
$make_cmd -C ../Userland clean && \
$make_cmd -C ../Userland && \
$make_cmd -C ../WindowServer clean && \
$make_cmd -C ../WindowServer && \
$make_cmd -C ../Applications/Terminal clean && \
$make_cmd -C ../Applications/Terminal && \
$make_cmd -C ../Applications/FontEditor clean && \
$make_cmd -C ../Applications/FontEditor && \
$make_cmd -C ../Applications/Clock clean && \
$make_cmd -C ../Applications/Clock && \
$make_cmd -C ../Applications/Launcher clean && \
$make_cmd -C ../Applications/Launcher && \
$make_cmd -C ../Applications/FileManager clean && \
$make_cmd -C ../Applications/FileManager && \
$make_cmd -C ../Applications/About clean && \
$make_cmd -C ../Applications/About && \
$make_cmd clean &&\
$make_cmd && \
sudo ./sync.sh

