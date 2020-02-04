#!/bin/bash

clean=false
case "$1" in
    clean)
        clean=true
        ;;
    *)
        ;;
esac

some_failed=false

for file in *; do
    if [ -d $file ]; then
        pushd $file > /dev/null
            dirname=$(basename $file)
            if [ "$clean" == true ]; then
                ./package.sh clean_all > /dev/null 2>&1
            fi
            if $(./package.sh > /dev/null 2>&1 ); then
                echo "Built ${dirname}."
            else
                echo "ERROR: Built ${dirname} not succesful!"
                some_failed=true
            fi
        popd > /dev/null
    fi
done

if [ "$some_failed" == false ]; then
    exit 0
else
    exit 1
fi