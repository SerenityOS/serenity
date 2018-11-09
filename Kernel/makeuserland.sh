#!/bin/bash

sudo id

make -C ../LibC clean && \
make -C ../LibC && \
make -C ../Userland clean && \
make -C ../Userland && \
sudo ./sync.sh

