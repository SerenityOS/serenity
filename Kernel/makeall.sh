#!/bin/bash

sudo id

make -C ../LibC clean && \
make -C ../LibC && \
make -C ../Userland clean && \
make -C ../Userland && \
make -C ../Terminal clean && \
make -C ../Terminal && \
make clean &&\
make && \
sudo ./sync.sh

