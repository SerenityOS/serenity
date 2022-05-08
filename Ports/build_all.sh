#!/usr/bin/env bash

build=true
clean=false
verbose=false

for argument in "$@"; do
    case "$argument" in
        clean)
            clean=true
            ;;
        clean_only)
            build=false
            clean=true
            ;;
        verbose)
            verbose=true
            ;;
        *)
            ;;
    esac
done

some_failed=false
built_ports=""

if [ "$clean" == true ]; then
    for file in *; do
        if [ -d "$file" ]; then
            pushd ${file} > /dev/null
            port=$(basename "$file")
            if ! [ -f package.sh ]; then
                echo "ERROR: Skipping cleaning of $port because its package.sh script is missing."
                popd > /dev/null
                continue
            fi
            if [ "$verbose" == true ]; then
                ./package.sh clean_all
            else
                ./package.sh clean_all > /dev/null 2>&1
            fi
            popd > /dev/null
        fi
    done
fi

if [ "$build" == true ]; then
    for file in *; do
        if [ -d "$file" ]; then
            pushd ${file} > /dev/null
            port=$(basename "$file")
            port_built=0
            for built_port in ${built_ports}; do
                if [ "$built_port" = "$port" ]; then
                    port_built=1
                    break
                fi
            done
            if (( port_built )); then
                echo "Built $port."
                popd > /dev/null
                continue
            fi
            if ! [ -f package.sh ]; then
                echo "ERROR: Skipping $port because its package.sh script is missing."
                popd > /dev/null
                continue
            fi
            if [ "$verbose" == true ]; then
                if ./package.sh; then
                    echo "Built $port."
                else
                    echo "ERROR: Build of $port was not successful!"
                    some_failed=true
                    popd > /dev/null
                    continue
                fi
            else
                if ./package.sh > /dev/null 2>&1; then
                    echo "Built $port."
                else
                    echo "ERROR: Build of $port was not successful!"
                    some_failed=true
                    popd > /dev/null
                    continue
                fi
            fi
            built_ports="$built_ports $port $(./package.sh showproperty depends) "
            popd > /dev/null
        fi
    done
    if [ "$verbose" == true ]; then
        echo "Built these ports: $built_ports"
    fi
fi

if [ "$some_failed" == false ]; then
    exit 0
else
    exit 1
fi
