#!/usr/bin/env bash

some_failed='false'
action='build'
verbose='false'
failfast='false'

for arg in "$@"; do
    case "$arg" in
        clean*)
            action="$arg"
            ;;
        verbose)
            verbose='true'
            ;;
        failfast)
            failfast='true'
            ;;
    esac
done

some_failed=false
processed_ports=()

log_success() {
    echo -e "\033[1;32m[#]\033[0m $1"
}

log_warn() {
    echo -e "\033[1;33m[!]\033[0m $1"
}

log_info() {
    echo -e "\033[1;36m[*]\033[0m $1"
}

log_process() {
    echo -e "\033[1m[~]\033[0m $1"
}

log_error() {
    echo -e "\033[1;31m[x]\033[0m $1"
}

do_build_port() {
    log_process "Building $port_name"
    if $verbose; then
        ./package.sh
    else
        ./package.sh &> /dev/null
    fi
}

do_clean_port() {
    log_process "Cleaning $port_name"
    if $verbose; then
        ./package.sh "$1"
    else
        ./package.sh "$1" &> /dev/null
    fi
}

ports_dir=$(realpath "$(dirname "${BASH_SOURCE[0]}")")
mapfile -d '' directories < <(find "$ports_dir" -mindepth 1 -maxdepth 1 -type d -print0 | sort -z)
for port_dir in "${directories[@]}"; do
    port_name="$(basename "$port_dir")"
    if [[ " ${processed_ports[*]} " == *" $port_name "* ]]; then
        log_info "$port_name is already processed"
        continue
    fi

    if ! cd "$port_dir"; then
        log_error "Can not change directory to '$port_name'"
        exit 1
    fi

    if [[ ! -x ./package.sh ]]; then
        log_warn "$port_name does not have executable package.sh"
        continue
    fi

    case "$action" in
        clean*)
            if do_clean_port "$action"; then
                log_success "Cleaned $port_name"
            else
                log_error "Failed cleaning $port_name"
                some_failed='true'
                if $failfast; then
                    exit 1
                fi
            fi
        ;;
        build)
            if do_build_port; then
                log_success "Built $port_name"
            else
                log_error "Failed building $port_name"
                some_failed='true'
                if $failfast; then
                    exit 1
                fi
            fi
        ;;
    esac

    # shellcheck disable=SC2207
    processed_ports+=("$port_name" $(./package.sh showproperty depends))
done

if $some_failed; then
    exit 1
fi
