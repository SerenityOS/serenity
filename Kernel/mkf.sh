#!/bin/bash

sudo id

make -C ../FontEditor clean && \
make -C ../FontEditor && \
sudo ./sync.sh

