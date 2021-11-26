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
built_ports=""

for file in *; do
    if [ -d $file ]; then
        pushd $file > /dev/null
            port=$(basename $file)
            port_built=0
            for built_port in $built_ports; do
                if [ "$built_port" = "$port" ]; then
                    port_built=1
                    break
                fi
            done
            if [ $port_built -eq 1 ]; then
                echo "Built $port."
                popd > /dev/null
                continue
            fi
            if ! [ -f package.sh ]; then
                echo "ERROR: Skipping $port because its package.sh script is missing."
                popd > /dev/null
                continue
            fi

            if [ "$clean" == true ]; then
                if [ "$verbose" == true ]; then
                    ./package.sh clean_all
                else
                    ./package.sh clean_all > /dev/null 2>&1
                fi
            fi
            if [ "$verbose" == true ]; then
                if ./package.sh; then
                    echo "Built ${port}."
                else
                    echo "ERROR: Build of ${port} was not successful!"
                    some_failed=true
                    popd > /dev/null
                    continue
                fi
            else
                if ./package.sh > /dev/null 2>&1; then
                    echo "Built ${port}."
                else
                    echo "ERROR: Build of ${port} was not successful!"
                    some_failed=true
                    popd > /dev/null
                    continue
                fi
            fi

            built_ports="$built_ports $port $(./package.sh showproperty depends) "
        popd > /dev/null
    fi
done

if [ "$some_failed" == false ]; then
    exit 0
else
    exit 1
fi
