#!/bin/bash

echo "extern const char $1[];"
echo "const char $1[] = \"\\"
IFS=$'\n'
for line in $(cat $2); do
    echo $line"\\"
done
echo "\";"

