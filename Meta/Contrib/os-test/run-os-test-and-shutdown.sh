#!/bin/sh

# Shell don't fully support all the required features, so let's use bash instead.
# FIXME: Use Shell once it's good enough.
ln /usr/local/bin/bash /usr/local/bin/sh

cd /usr/local/os-test/ || exit

make test

sync
shutdown -n
