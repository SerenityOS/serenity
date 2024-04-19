#!/bin/bash
################################################################################
# Part of the LLVM Project, under the Apache License v2.0 with LLVM Exceptions.
# See https://llvm.org/LICENSE.txt for license information.
# SPDX-License-Identifier: Apache-2.0 WITH LLVM-exception
################################################################################
#
# This script will install the llvm toolchain on the different
# Debian and Ubuntu versions

set -eux

usage() {
    set +x
    echo "Usage: $0 [llvm_major_version] [all] [OPTIONS]" 1>&2
    echo -e "all\t\t\tInstall all packages." 1>&2
    echo -e "-n=code_name\t\tSpecifies the distro codename, for example bionic" 1>&2
    echo -e "-h\t\t\tPrints this help." 1>&2
    echo -e "-m=repo_base_url\tSpecifies the base URL from which to download." 1>&2
    exit 1;
}

CURRENT_LLVM_STABLE=18
BASE_URL="http://apt.llvm.org"

# Check for required tools
needed_binaries=(lsb_release wget add-apt-repository gpg)
missing_binaries=()
for binary in "${needed_binaries[@]}"; do
    if ! which $binary &>/dev/null ; then
        missing_binaries+=($binary)
    fi
done
if [[ ${#missing_binaries[@]} -gt 0 ]] ; then
    echo "You are missing some tools this script requires: ${missing_binaries[@]}"
    echo "(hint: apt install lsb-release wget software-properties-common gnupg)"
    exit 4
fi

# Set default values for commandline arguments
# We default to the current stable branch of LLVM
LLVM_VERSION=$CURRENT_LLVM_STABLE
ALL=0
DISTRO=$(lsb_release -is)
VERSION=$(lsb_release -sr)
UBUNTU_CODENAME=""
CODENAME_FROM_ARGUMENTS=""
# Obtain VERSION_CODENAME and UBUNTU_CODENAME (for Ubuntu and its derivatives)
source /etc/os-release
DISTRO=${DISTRO,,}
case ${DISTRO} in
    debian)
        # Debian Trixie has a workaround because of
        # https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=1038383
        if [[ "${VERSION}" == "unstable" ]] || [[ "${VERSION}" == "testing" ]] || [[ "${VERSION_CODENAME}" == "trixie" ]]; then
            CODENAME=unstable
            LINKNAME=
        else
            # "stable" Debian release
            CODENAME=${VERSION_CODENAME}
            LINKNAME=-${CODENAME}
        fi
        ;;
    *)
        # ubuntu and its derivatives
        if [[ -n "${UBUNTU_CODENAME}" ]]; then
            CODENAME=${UBUNTU_CODENAME}
            if [[ -n "${CODENAME}" ]]; then
                LINKNAME=-${CODENAME}
            fi
        fi
        ;;
esac

# read optional command line arguments
if [ "$#" -ge 1 ] && [ "${1::1}" != "-" ]; then
    if [ "$1" != "all" ]; then
        LLVM_VERSION=$1
    else
        # special case for ./llvm.sh all
        ALL=1
    fi
    OPTIND=2
    if [ "$#" -ge 2 ]; then
      if [ "$2" == "all" ]; then
          # Install all packages
          ALL=1
          OPTIND=3
      fi
    fi
fi

while getopts ":hm:n:" arg; do
    case $arg in
    h)
        usage
        ;;
    m)
        BASE_URL=${OPTARG}
        ;;
    n)
        CODENAME=${OPTARG}
        if [[ "${CODENAME}" == "unstable" ]]; then
            # link name does not apply to unstable repository
            LINKNAME=
        else
            LINKNAME=-${CODENAME}
        fi
        CODENAME_FROM_ARGUMENTS="true"
        ;;
    esac
done

if [[ $EUID -ne 0 ]]; then
   echo "This script must be run as root!"
   exit 1
fi

declare -A LLVM_VERSION_PATTERNS
LLVM_VERSION_PATTERNS[9]="-9"
LLVM_VERSION_PATTERNS[10]="-10"
LLVM_VERSION_PATTERNS[11]="-11"
LLVM_VERSION_PATTERNS[12]="-12"
LLVM_VERSION_PATTERNS[13]="-13"
LLVM_VERSION_PATTERNS[14]="-14"
LLVM_VERSION_PATTERNS[15]="-15"
LLVM_VERSION_PATTERNS[16]="-16"
LLVM_VERSION_PATTERNS[17]="-17"
LLVM_VERSION_PATTERNS[18]="-18"
LLVM_VERSION_PATTERNS[19]=""

if [ ! ${LLVM_VERSION_PATTERNS[$LLVM_VERSION]+_} ]; then
    echo "This script does not support LLVM version $LLVM_VERSION"
    exit 3
fi

LLVM_VERSION_STRING=${LLVM_VERSION_PATTERNS[$LLVM_VERSION]}

# join the repository name
if [[ -n "${CODENAME}" ]]; then
    REPO_NAME="deb ${BASE_URL}/${CODENAME}/  llvm-toolchain${LINKNAME}${LLVM_VERSION_STRING} main"

    # check if the repository exists for the distro and version
    if ! wget -q --method=HEAD ${BASE_URL}/${CODENAME} &> /dev/null; then
        if [[ -n "${CODENAME_FROM_ARGUMENTS}" ]]; then
            echo "Specified codename '${CODENAME}' is not supported by this script."
        else
            echo "Distribution '${DISTRO}' in version '${VERSION}' is not supported by this script."
        fi
        exit 2
    fi
fi


# install everything

if [[ ! -f /etc/apt/trusted.gpg.d/apt.llvm.org.asc ]]; then
    # download GPG key once
    wget -qO- https://apt.llvm.org/llvm-snapshot.gpg.key | tee /etc/apt/trusted.gpg.d/apt.llvm.org.asc
fi

if [[ -z "`apt-key list 2> /dev/null | grep -i llvm`" ]]; then
    # Delete the key in the old format
    apt-key del AF4F7421
fi
add-apt-repository -y "${REPO_NAME}"
apt-get update
PKG="clang-$LLVM_VERSION lldb-$LLVM_VERSION lld-$LLVM_VERSION clangd-$LLVM_VERSION"
if [[ $ALL -eq 1 ]]; then
    # same as in test-install.sh
    # No worries if we have dups
    PKG="$PKG clang-tidy-$LLVM_VERSION clang-format-$LLVM_VERSION clang-tools-$LLVM_VERSION llvm-$LLVM_VERSION-dev lld-$LLVM_VERSION lldb-$LLVM_VERSION llvm-$LLVM_VERSION-tools libomp-$LLVM_VERSION-dev libc++-$LLVM_VERSION-dev libc++abi-$LLVM_VERSION-dev libclang-common-$LLVM_VERSION-dev libclang-$LLVM_VERSION-dev libclang-cpp$LLVM_VERSION-dev libunwind-$LLVM_VERSION-dev"
    if test $LLVM_VERSION -gt 14; then
        PKG="$PKG libclang-rt-$LLVM_VERSION-dev libpolly-$LLVM_VERSION-dev"
    fi
fi
apt-get install -y $PKG
