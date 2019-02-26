#!/bin/bash

mkdir -p ../Root/usr/include/sys/
mkdir -p ../Root/usr/lib/
cp *.h ../Root/usr/include/
cp libm.a ../Root/usr/lib/
