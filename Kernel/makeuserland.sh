#!/bin/bash

sudo id

make -C ../LibC clean && \
make -C ../LibC && \
make -C ../LibGUI clean && \
make -C ../LibGUI && \
make -C ../Terminal clean && \
make -C ../Terminal && \
make -C ../Clock clean && \
make -C ../Clock && \
make -C ../Launcher clean && \
make -C ../Launcher && \
make -C ../Userland clean && \
make -C ../Userland && \
sudo ./sync.sh

