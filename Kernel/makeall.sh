#!/bin/bash

sudo id

make -C ../LibC clean && \
make -C ../LibC && \
make -C ../LibGUI clean && \
make -C ../LibGUI && \
make -C ../Userland clean && \
make -C ../Userland && \
make -C ../Terminal clean && \
make -C ../Terminal && \
make -C ../FontEditor clean && \
make -C ../FontEditor && \
make -C ../Clock clean && \
make -C ../Clock && \
make -C ../Launcher clean && \
make -C ../Launcher && \
make -C ../FileManager clean && \
make -C ../FileManager && \
make clean &&\
make && \
sudo ./sync.sh

