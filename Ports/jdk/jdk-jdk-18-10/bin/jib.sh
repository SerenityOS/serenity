#!/bin/bash
#
# Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# This script installs the JIB tool into it's own local repository and
# puts a wrapper scripts into <source-root>/.jib

mydir="$(dirname "${BASH_SOURCE[0]}")"
myname="$(basename "${BASH_SOURCE[0]}")"

installed_jib_script=${mydir}/../.jib/jib
install_data=${mydir}/../.jib/.data

setup_url() {
    if [ -f ~/.config/jib/jib.conf ]; then
        source ~/.config/jib/jib.conf
    fi

    jib_repository="jdk-virtual"
    jib_organization="jpg/infra/builddeps"
    jib_module="jib"
    jib_revision="3.0-SNAPSHOT"
    jib_ext="jib.sh.gz"

    closed_script="${mydir}/../../closed/make/conf/jib-install.conf"
    if [ -f "${closed_script}" ]; then
        source "${closed_script}"
    fi

    if [ -n "${JIB_SERVER}" ]; then
        jib_server="${JIB_SERVER}"
    fi
    if [ -n "${JIB_SERVER_MIRRORS}" ]; then
        jib_server_mirrors="${JIB_SERVER_MIRRORS}"
    fi
    if [ -n "${JIB_REPOSITORY}" ]; then
        jib_repository="${JIB_REPOSITORY}"
    fi
    if [ -n "${JIB_ORGANIZATION}" ]; then
        jib_organization="${JIB_ORGANIZATION}"
    fi
    if [ -n "${JIB_MODULE}" ]; then
        jib_module="${JIB_MODULE}"
    fi
    if [ -n "${JIB_REVISION}" ]; then
        jib_revision="${JIB_REVISION}"
    fi
    if [ -n "${JIB_EXTENSION}" ]; then
        jib_extension="${JIB_EXTENSION}"
    fi

    if [ -n "${JIB_URL}" ]; then
        jib_url="${JIB_URL}"
        data_string="${jib_url}"
    else
        jib_path="${jib_repository}/${jib_organization}/${jib_module}/${jib_revision}/${jib_module}-${jib_revision}.${jib_ext}"
        data_string="${jib_path}"
        jib_url="${jib_server}/${jib_path}"
    fi
}

install_jib() {
    if [ -z "${jib_server}" -a -z "${JIB_URL}" ]; then
        echo "No jib server or URL provided, set either"
        echo "JIB_SERVER=<base server address>"
        echo "or"
        echo "JIB_URL=<full path to install script>"
        exit 1
    fi

    if command -v curl > /dev/null; then
        getcmd="curl -s -L --retry 3 --retry-delay 5"
    elif command -v wget > /dev/null; then
        getcmd="wget --quiet -O -"
    else
        echo "Could not find either curl or wget"
        exit 1
    fi

    if ! command -v gunzip > /dev/null; then
        echo "Could not find gunzip"
        exit 1
    fi

    echo "Downloading JIB bootstrap script"
    mkdir -p "${installed_jib_script%/*}"
    rm -f "${installed_jib_script}.gz"
    ${getcmd} ${jib_url} > "${installed_jib_script}.gz"
    if [ ! -s "${installed_jib_script}.gz" ]; then
        echo "Failed to download ${jib_url}"
        if [ -n "${jib_path}" -a -n "${jib_server_mirrors}" ]; then
            OLD_IFS="${IFS}"
            IFS=" ,"
            for mirror in ${jib_server_mirrors}; do
                echo "Trying mirror ${mirror}"
                jib_url="${mirror}/${jib_path}"
                ${getcmd} ${jib_url} > "${installed_jib_script}.gz"
                if [ -s "${installed_jib_script}.gz" ]; then
                    echo "Download from mirror successful"
                    break
                else
                    echo "Failed to download ${jib_url}"
                fi
            done
            IFS="${OLD_IFS}"
        fi
        if [ ! -s "${installed_jib_script}.gz" ]; then
            exit 1
        fi
    fi
    echo "Extracting JIB bootstrap script"
    rm -f "${installed_jib_script}"
    gunzip "${installed_jib_script}.gz"
    chmod +x "${installed_jib_script}"
    echo "${data_string}" > "${install_data}"
}

# Main body starts here

setup_url

if [ ! -x "${installed_jib_script}" ]; then
    install_jib
elif [ ! -e "${install_data}" ] || [ "${data_string}" != "$(cat "${install_data}")" ]; then
    echo "Install url changed since last time, reinstalling"
    install_jib
fi

# Provide a reasonable default for the --src-dir parameter if run out of tree
if [ -z "${JIB_SRC_DIR}" ]; then
    export JIB_SRC_DIR="${mydir}/../"
fi

${installed_jib_script} "$@"
