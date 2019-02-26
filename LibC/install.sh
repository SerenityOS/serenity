#!/bin/bash

mkdir -p ../Root/usr/include/sys/
mkdir -p ../Root/usr/lib/
cp *.h ../Root/usr/include/
cp sys/*.h ../Root/usr/include/sys/
cp libc.a ../Root/usr/lib/
cp crt0.o ../Root/usr/lib/
