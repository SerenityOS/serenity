#!/bin/sh
set -e pipefail

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path/.."

# The dollar symbol in sed's argument is for "end of line", not any shell variable.
# shellcheck disable=SC2016
grep -Pirh '(?<!file://)(?<!\.)(?<!})(?<!\()/(etc|res|usr|www)/' AK/ Applications/ Base Demos/ DevTools/ Documentation/ Games/ Kernel/ Libraries/ MenuApplets/ Services/ Shell/ Userland/ | \
sed -re 's,^.*["= `]/([^"%`: ]+[^"%`: /.])/?(["%`: .].*)?$,\1,' | \
sort -u | \
while read -r referenced_resource
do
    if ! [ -r "Base/${referenced_resource}" ] && ! [ -r "Build/Root/${referenced_resource}" ]
    then
        echo "Potentially missing resource: ${referenced_resource}"
    fi
done
