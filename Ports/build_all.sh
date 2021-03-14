#!/usr/bin/env bash

clean=false
verbose=false

case "$1" in
    clean)
        clean=true
        ;;
    verbose)
        verbose=true
        ;;
    *)
        ;;
esac

case "$2" in
    clean)
        clean=true
        ;;
    verbose)
        verbose=true
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
                if [ "$verbose" == true ]; then
                    ./package.sh clean_all
                else
                    ./package.sh clean_all > /dev/null 2>&1
                fi
            fi
            if [ "$verbose" == true ]; then
                if $(./package.sh); then
                    echo "Built ${dirname}."
                else
                    echo "ERROR: Build of ${dirname} was not successful!"
                    some_failed=true
                fi
            else
                if $(./package.sh > /dev/null 2>&1); then
                    echo "Built ${dirname}."
                else
                    echo "ERROR: Build of ${dirname} was not successful!"
                    some_failed=true
                fi
            fi
        popd > /dev/null
    fi
done

if [ "$some_failed" == false ]; then
    exit 0
else
    exit 1
fi
