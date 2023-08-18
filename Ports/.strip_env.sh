#!/usr/bin/env bash

environment_variables=(
    "HOME=${HOME}"
    "PATH=${PATH}"
    "SERENITY_STRIPPED_ENV=1"
    "TERM=${TERM}"
    "USER=${USER}"  
)

# If any of the following optional variables are set, keep them.
keep_environment_variables=(
    'EDITOR'
    'IN_SERENITY_PORT_DEV'
    'MAKEJOBS'
    'SERENITY_ARCH'
    'SERENITY_TOOLCHAIN'
    'VISUAL'
)

for environment_variable_name in "${keep_environment_variables[@]}"; do
    if [ -v "${environment_variable_name}" ]; then
        environment_variables+=("${environment_variable_name}=${!environment_variable_name}")
    fi
done

exec env -i "${environment_variables[@]}" "${@}"
