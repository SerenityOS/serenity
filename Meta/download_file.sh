#!/usr/bin/env bash

# This will be overridden in .port_include.sh
run_nocd() {
  ("$@")
}

# 1 = type
# 2 = path
MIRROR_URL_PATTERN="^mirror://([^/]+)/(.+)$"

# shellcheck disable=SC2034
MIRROR_URLS_gnu="https://ftpmirror.gnu.org/gnu/ https://ftp.gnu.org/gnu/"

resolve_mirror_urls() {
    local type="${1}"
    local path="${2}"

    local mirror_urls_variable="MIRROR_URLS_${type}"
    local mirrors_string="${!mirror_urls_variable:-}"

    if [ -z "${mirrors_string}" ]; then
        echo "error: Unknown mirror '${type}'" >&2
        return 1
    fi

    local mirrors
    read -ra mirrors <<< "${mirrors_string}"

    local mirror
    for mirror in "${mirrors[@]}"; do
        printf '%s%s\n' "${mirror}" "${path}"
    done
}

# FIXME: Migrate users of do_download_file to download_file and remove
#        redundant work from this function.
do_download_file() {
    local url="$1"
    local filename="$2"
    local accept_existing="${3:-true}"
    local expected_checksum="${4:-}"

    local urls=("${url}")
    local resolved_urls

    if [[ "${url}" =~ ${MIRROR_URL_PATTERN} ]]; then
        local type="${BASH_REMATCH[1]}"
        local path="${BASH_REMATCH[2]}"
        if ! resolved_urls="$(resolve_mirror_urls "${type}" "${path}")"; then
            return 1
        fi

        urls=()
        local resolved_url
        while IFS= read -r resolved_url; do
            urls+=("${resolved_url}")
        done <<< "${resolved_urls}"
    fi

    if $accept_existing && [ -f "$filename" ]; then
        echo "$filename already exists"
        return
    fi

    local download_status=1

    for candidate_url in "${urls[@]}"; do
        echo "Downloading URL: ${candidate_url}"
        if which curl; then
            # shellcheck disable=SC2086
            if run_nocd curl ${curlopts:-} "$candidate_url" --fail -L -o "$filename"; then
                return 0
            else
                download_status="$?"
                rm -f "${filename}"
            fi
        else
            if run_nocd pro "$candidate_url" > "$filename"; then
                if [ -n "${expected_checksum}" ]; then
                    actual_checksum="$(sha256sum "$filename" | cut -f1 -d' ')"
                    local actual_checksum

                    if [ "${actual_checksum}" != "${expected_checksum}" ]; then
                        rm -f "${filename}"
                        continue
                    fi
                fi

                return 0
            else
                download_status="$?"
                rm -f "${filename}"
            fi
        fi
    done
    return "${download_status}"
}

download_file() {
    local url="${1}"
    local destination="${2}"
    local checksum="${3}"

    local filename
    filename="$(basename "${url}")"

    local tried_download_again=0

    while true; do
        do_download_file "${url}" "${destination}" true "${checksum}"

        actual_checksum="$(sha256sum "${destination}" | cut -f1 -d' ')"

        if [ "${actual_checksum}" = "${checksum}" ]; then
            break
        fi

        echo "SHA256 checksum of downloaded file '${filename}' does not match!"
        echo "Expected: ${checksum}"
        echo "Actual:   ${actual_checksum}"
        rm -f "${destination}"
        echo "Removed erroneous download."
        if [ "${tried_download_again}" -eq 1 ]; then
            echo "Please run script again."
            exit 1
        fi
        echo "Trying to download the file again."
        tried_download_again=1
    done
}
