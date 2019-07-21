#!/bin/bash

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/sys/
mkdir -p $SERENITY_ROOT/Root/usr/include/netinet/
mkdir -p $SERENITY_ROOT/Root/usr/include/arpa/
mkdir -p $SERENITY_ROOT/Root/usr/lib/
cp *.h $SERENITY_ROOT/Root/usr/include/
cp sys/*.h $SERENITY_ROOT/Root/usr/include/sys/
cp arpa/*.h $SERENITY_ROOT/Root/usr/include/arpa/
cp netinet/*.h $SERENITY_ROOT/Root/usr/include/netinet/
cp libc.a $SERENITY_ROOT/Root/usr/lib/
cp crt0.o $SERENITY_ROOT/Root/usr/lib/
cp crti.ao $SERENITY_ROOT/Root/usr/lib/crti.o
cp crtn.ao $SERENITY_ROOT/Root/usr/lib/crtn.o
