#!/bin/bash ../.toolchain_include.sh

# get python details from port
source "$SERENITY_ROOT/Ports/python-3.6/version.sh"

package=python
version=$PYTHON_VERSION
useconfigure=true
sourcedir=Python-$version
files="$PYTHON_URL $PYTHON_ARCHIVE $PYTHON_MD5SUM"
