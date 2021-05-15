#!/usr/bin/env bash

packagesdb="${SERENITY_BUILD_DIR}/packages.db"

clean=false
case "$1" in
    clean)
        clean=true
        ;;
    *)
        ;;
esac

some_failed=false

while IFS= read -r line; do
    port="$(echo "$line" | cut -d' ' -f2)"
    if [ -d "$port" ]; then
        pushd $port > /dev/null
            dirname=$(basename $port)
            if [ "$clean" == true ]; then
                ./package.sh clean_all
            fi
            if ./package.sh; then
                echo "Built ${dirname}."
            else
                echo "ERROR: Build of ${dirname} was not successful!"
                some_failed=true
            fi
        popd > /dev/null
    else
        echo "ERROR: Previously installed port $port doesn't exist!"
        some_failed=true
    fi
done < <(grep -E "^(auto|manual)" "$packagesdb")

if [ "$some_failed" == false ]; then
    exit 0
else
    exit 1
fi
