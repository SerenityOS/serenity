#!/bin/bash ../.port_include.sh

source version.sh

port=python-3.6
version=3.6
workdir=Python-3.6.0
useconfigure=true
configopts="--build=i686 --without-threads --enable-optimizations"
makeopts="-j$(nproc) build_all"
installopts="-j$(nproc) build_all"
files="${PYTHON_URL} ${PYTHON_ARCHIVE}"

export CONFIG_SITE=$(pwd)/config.site

if [ -x "$(command -v python3)" ]; then
  # check if major and minor version of python3 are matching
  if python3 -c "import sys;sys.exit('.'.join(str(n) for n in sys.version_info[:2]) in '$PYTHON_VERSION')"; then
    echo 'Error: python3 version does not match needed version to build:' $PYTHON_VERSION >&2
    echo 'Please build python3.6 with Toolchain/BuildPython.sh !' >&2
    exit 1
  fi
else
    echo 'Error: python3 is not installed, please build python3.6 with Toolchain/BuildPython.sh !' >&2
    exit 1
fi
