#!/bin/bash

mkdir -p ../Root/usr/include/sys/
mkdir -p ../Root/usr/include/netinet/
mkdir -p ../Root/usr/include/arpa/
mkdir -p ../Root/usr/lib/
cp *.h ../Root/usr/include/
cp sys/*.h ../Root/usr/include/sys/
cp arpa/*.h ../Root/usr/include/arpa/
cp netinet/*.h ../Root/usr/include/netinet/
cp libc.a ../Root/usr/lib/
cp crt0.o ../Root/usr/lib/
cp crti.ao ../Root/usr/lib/crti.o
cp crtn.ao ../Root/usr/lib/crtn.o
