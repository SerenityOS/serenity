#!/bin/sh

cd Source/pkgsrc/bootstrap
./bootstrap --full --unprivileged 2>&1 | tee ~/bootstrap.log
