#!/bin/bash

sudo id

make -C ../LibC clean && \
make -C ../LibC && \
make -C ../LibGUI clean && \
make -C ../LibGUI && \
make -C ../Userland clean && \
make -C ../Userland && \
make -C ../Application/Terminal clean && \
make -C ../Application/Terminal && \
make -C ../Application/FontEditor clean && \
make -C ../Application/FontEditor && \
make -C ../Application/Clock clean && \
make -C ../Application/Clock && \
make -C ../Application/Launcher clean && \
make -C ../Application/Launcher && \
make -C ../Application/FileManager clean && \
make -C ../Application/FileManager && \
make clean &&\
make && \
sudo ./sync.sh

