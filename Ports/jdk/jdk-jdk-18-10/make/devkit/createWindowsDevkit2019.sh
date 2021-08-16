#!/bin/bash
#
# Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
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

# This script copies parts of a Visual Studio installation into a devkit
# suitable for building OpenJDK and OracleJDK. Needs to run in Cygwin or WSL.
# erik.joelsson@oracle.com

VS_VERSION="2019"
VS_VERSION_NUM_NODOT="160"
VS_DLL_VERSION="140"
SDK_VERSION="10"
SDK_FULL_VERSION="10.0.17763.0"
MSVC_DIR="Microsoft.VC142.CRT"
MSVC_FULL_VERSION="14.12.27508"
REDIST_FULL_VERSION="14.20.27508"

SCRIPT_DIR="$(cd "$(dirname $0)" > /dev/null && pwd)"
BUILD_DIR="${SCRIPT_DIR}/../../build/devkit"

################################################################################
# Prepare settings

UNAME_SYSTEM=`uname -s`
UNAME_RELEASE=`uname -r`

# Detect cygwin or WSL
IS_CYGWIN=`echo $UNAME_SYSTEM | grep -i CYGWIN`
IS_WSL=`echo $UNAME_RELEASE | grep Microsoft`
if test "x$IS_CYGWIN" != "x"; then
    BUILD_ENV="cygwin"
elif test "x$IS_WSL" != "x"; then
    BUILD_ENV="wsl"
else
    echo "Unknown environment; only Cygwin and WSL are supported."
    exit 1
fi

if test "x$BUILD_ENV" = "xcygwin"; then
    WINDOWS_PATH_TO_UNIX_PATH="cygpath -u"
elif test "x$BUILD_ENV" = "xwsl"; then
    WINDOWS_PATH_TO_UNIX_PATH="wslpath -u"
fi

# Work around the insanely named ProgramFiles(x86) env variable
PROGRAMFILES_X86="$($WINDOWS_PATH_TO_UNIX_PATH "$(cmd.exe /c set | sed -n 's/^ProgramFiles(x86)=//p' | tr -d '\r')")"

# Find Visual Studio installation dir
VSNNNCOMNTOOLS=`cmd.exe /c echo %VS${VS_VERSION_NUM_NODOT}COMNTOOLS% | tr -d '\r'`
if [ -d "$VSNNNCOMNTOOLS" ]; then
    VS_INSTALL_DIR="$($WINDOWS_PATH_TO_UNIX_PATH "$VSNNNCOMNTOOLS/../..")"
else
    VS_INSTALL_DIR="${PROGRAMFILES_X86}/Microsoft Visual Studio/2019"
    VS_INSTALL_DIR="$(ls -d "${VS_INSTALL_DIR}/"{Community,Professional,Enterprise} 2>/dev/null | head -n1)"
fi
echo "VS_INSTALL_DIR: $VS_INSTALL_DIR"

# Extract semantic version
POTENTIAL_INI_FILES="Common7/IDE/wdexpress.isolation.ini Common7/IDE/devenv.isolation.ini"
for f in $POTENTIAL_INI_FILES; do
    if [ -f "$VS_INSTALL_DIR/$f" ]; then
        VS_VERSION_SP="$(grep ^SemanticVersion= "$VS_INSTALL_DIR/$f")"
        # Remove SemnaticVersion=
        VS_VERSION_SP="${VS_VERSION_SP#*=}"
        # Remove suffix of too detailed numbering starting with +
        VS_VERSION_SP="${VS_VERSION_SP%+*}"
        break
    fi
done
if [ -z "$VS_VERSION_SP" ]; then
    echo "Failed to find SP version"
    exit 1
fi
echo "Found Version SP: $VS_VERSION_SP"

# Setup output dirs
DEVKIT_ROOT="${BUILD_DIR}/VS${VS_VERSION}-${VS_VERSION_SP}-devkit"
DEVKIT_BUNDLE="${DEVKIT_ROOT}.tar.gz"

echo "Creating devkit in $DEVKIT_ROOT"

MSVCR_DLL=${MSVC_DIR}/vcruntime${VS_DLL_VERSION}.dll
VCRUNTIME_1_DLL=${MSVC_DIR}/vcruntime${VS_DLL_VERSION}_1.dll
MSVCP_DLL=${MSVC_DIR}/msvcp${VS_DLL_VERSION}.dll

################################################################################
# Copy Visual Studio files

TOOLS_VERSION="$(ls "$VS_INSTALL_DIR/VC/Tools/MSVC" | sort -r -n | head -n1)"
echo "Found Tools version: $TOOLS_VERSION"
VC_SUBDIR="VC/Tools/MSVC/$TOOLS_VERSION"
REDIST_VERSION="$(ls "$VS_INSTALL_DIR/VC/Redist/MSVC" | sort -r -n | head -n1)"
echo "Found Redist version: $REDIST_VERSION"
REDIST_SUBDIR="VC/Redist/MSVC/$REDIST_VERSION"
echo "Copying VC..."
rm -rf $DEVKIT_ROOT/VC
mkdir -p $DEVKIT_ROOT/VC/bin
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/bin/Hostx64/arm64" $DEVKIT_ROOT/VC/bin/
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/bin/Hostx64/x64" $DEVKIT_ROOT/VC/bin/
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/bin/Hostx86/x86" $DEVKIT_ROOT/VC/bin/
mkdir -p $DEVKIT_ROOT/VC/lib
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/lib/arm64" $DEVKIT_ROOT/VC/lib/
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/lib/x64" $DEVKIT_ROOT/VC/lib/
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/lib/x86" $DEVKIT_ROOT/VC/lib/
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/include" $DEVKIT_ROOT/VC/
mkdir -p $DEVKIT_ROOT/VC/atlmfc/lib
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/atlmfc/lib/arm64" $DEVKIT_ROOT/VC/atlmfc/lib/
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/atlmfc/lib/x64" $DEVKIT_ROOT/VC/atlmfc/lib/
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/atlmfc/lib/x86" $DEVKIT_ROOT/VC/atlmfc/lib/
cp -r "$VS_INSTALL_DIR/${VC_SUBDIR}/atlmfc/include" $DEVKIT_ROOT/VC/atlmfc/
mkdir -p $DEVKIT_ROOT/VC/Auxiliary
cp -r "$VS_INSTALL_DIR/VC/Auxiliary/Build" $DEVKIT_ROOT/VC/Auxiliary/
mkdir -p $DEVKIT_ROOT/VC/redist
cp -r "$VS_INSTALL_DIR/$REDIST_SUBDIR/arm64" $DEVKIT_ROOT/VC/redist/
cp -r "$VS_INSTALL_DIR/$REDIST_SUBDIR/x64" $DEVKIT_ROOT/VC/redist/
cp -r "$VS_INSTALL_DIR/$REDIST_SUBDIR/x86" $DEVKIT_ROOT/VC/redist/

# The redist runtime libs are needed to run the compiler but may not be
# installed on the machine where the devkit will be used.
cp $DEVKIT_ROOT/VC/redist/x86/$MSVCR_DLL $DEVKIT_ROOT/VC/bin/x86
cp $DEVKIT_ROOT/VC/redist/x86/$MSVCP_DLL $DEVKIT_ROOT/VC/bin/x86
cp $DEVKIT_ROOT/VC/redist/x64/$MSVCR_DLL $DEVKIT_ROOT/VC/bin/x64
cp $DEVKIT_ROOT/VC/redist/x64/$MSVCP_DLL $DEVKIT_ROOT/VC/bin/x64
cp $DEVKIT_ROOT/VC/redist/arm64/$MSVCR_DLL $DEVKIT_ROOT/VC/bin/arm64
cp $DEVKIT_ROOT/VC/redist/arm64/$MSVCP_DLL $DEVKIT_ROOT/VC/bin/arm64

################################################################################
# Copy SDK files

SDK_INSTALL_DIR="$PROGRAMFILES_X86/Windows Kits/$SDK_VERSION"
echo "SDK_INSTALL_DIR: $SDK_INSTALL_DIR"

SDK_FULL_VERSION="$(ls "$SDK_INSTALL_DIR/bin" | sort -r -n | head -n1)"
echo "Found SDK version: $SDK_FULL_VERSION"
UCRT_VERSION="$(ls "$SDK_INSTALL_DIR/Redist" | grep $SDK_VERSION | sort -r -n | head -n1)"
echo "Found UCRT version: $UCRT_VERSION"
echo "Copying SDK..."
rm -rf $DEVKIT_ROOT/$SDK_VERSION
mkdir -p $DEVKIT_ROOT/$SDK_VERSION/bin
cp -r "$SDK_INSTALL_DIR/bin/$SDK_FULL_VERSION/x64" $DEVKIT_ROOT/$SDK_VERSION/bin/
cp -r "$SDK_INSTALL_DIR/bin/$SDK_FULL_VERSION/x86" $DEVKIT_ROOT/$SDK_VERSION/bin/
mkdir -p $DEVKIT_ROOT/$SDK_VERSION/lib
cp -r "$SDK_INSTALL_DIR/lib/$SDK_FULL_VERSION/um/arm64" $DEVKIT_ROOT/$SDK_VERSION/lib/
cp -r "$SDK_INSTALL_DIR/lib/$SDK_FULL_VERSION/um/x64" $DEVKIT_ROOT/$SDK_VERSION/lib/
cp -r "$SDK_INSTALL_DIR/lib/$SDK_FULL_VERSION/um/x86" $DEVKIT_ROOT/$SDK_VERSION/lib/
cp -r "$SDK_INSTALL_DIR/lib/$SDK_FULL_VERSION/ucrt/arm64" $DEVKIT_ROOT/$SDK_VERSION/lib/
cp -r "$SDK_INSTALL_DIR/lib/$SDK_FULL_VERSION/ucrt/x64" $DEVKIT_ROOT/$SDK_VERSION/lib/
cp -r "$SDK_INSTALL_DIR/lib/$SDK_FULL_VERSION/ucrt/x86" $DEVKIT_ROOT/$SDK_VERSION/lib/
mkdir -p $DEVKIT_ROOT/$SDK_VERSION/Redist
cp -r "$SDK_INSTALL_DIR/Redist/$UCRT_VERSION/ucrt" $DEVKIT_ROOT/$SDK_VERSION/Redist/
mkdir -p $DEVKIT_ROOT/$SDK_VERSION/include
cp -r "$SDK_INSTALL_DIR/include/$SDK_FULL_VERSION/"* $DEVKIT_ROOT/$SDK_VERSION/include/

################################################################################
# Generate devkit.info

echo-info() {
    echo "$1" >> $DEVKIT_ROOT/devkit.info
}

echo "Generating devkit.info..."
rm -f $DEVKIT_ROOT/devkit.info
echo-info "# This file describes to configure how to interpret the contents of this devkit"
echo-info "DEVKIT_NAME=\"Microsoft Visual Studio $VS_VERSION $VS_VERSION_SP (devkit)\""
echo-info "DEVKIT_VS_VERSION=\"$VS_VERSION\""
echo-info ""
echo-info "DEVKIT_TOOLCHAIN_PATH_x86=\"\$DEVKIT_ROOT/VC/bin/x86:\$DEVKIT_ROOT/$SDK_VERSION/bin/x86\""
echo-info "DEVKIT_VS_INCLUDE_x86=\"\$DEVKIT_ROOT/VC/include;\$DEVKIT_ROOT/VC/atlmfc/include;\$DEVKIT_ROOT/$SDK_VERSION/include/shared;\$DEVKIT_ROOT/$SDK_VERSION/include/ucrt;\$DEVKIT_ROOT/$SDK_VERSION/include/um;\$DEVKIT_ROOT/$SDK_VERSION/include/winrt\""
echo-info "DEVKIT_VS_LIB_x86=\"\$DEVKIT_ROOT/VC/lib/x86;\$DEVKIT_ROOT/VC/atlmfc/lib/x86;\$DEVKIT_ROOT/$SDK_VERSION/lib/x86\""
echo-info "DEVKIT_MSVCR_DLL_x86=\"\$DEVKIT_ROOT/VC/redist/x86/$MSVCR_DLL\""
echo-info "DEVKIT_MSVCP_DLL_x86=\"\$DEVKIT_ROOT/VC/redist/x86/$MSVCP_DLL\""
echo-info "DEVKIT_UCRT_DLL_DIR_x86=\"\$DEVKIT_ROOT/10/Redist/ucrt/DLLs/x86\""
echo-info ""
echo-info "DEVKIT_TOOLCHAIN_PATH_x86_64=\"\$DEVKIT_ROOT/VC/bin/x64:\$DEVKIT_ROOT/$SDK_VERSION/bin/x64:\$DEVKIT_ROOT/$SDK_VERSION/bin/x86\""
echo-info "DEVKIT_VS_INCLUDE_x86_64=\"\$DEVKIT_ROOT/VC/include;\$DEVKIT_ROOT/VC/atlmfc/include;\$DEVKIT_ROOT/$SDK_VERSION/include/shared;\$DEVKIT_ROOT/$SDK_VERSION/include/ucrt;\$DEVKIT_ROOT/$SDK_VERSION/include/um;\$DEVKIT_ROOT/$SDK_VERSION/include/winrt\""
echo-info "DEVKIT_VS_LIB_x86_64=\"\$DEVKIT_ROOT/VC/lib/x64;\$DEVKIT_ROOT/VC/atlmfc/lib/x64;\$DEVKIT_ROOT/$SDK_VERSION/lib/x64\""
echo-info "DEVKIT_MSVCR_DLL_x86_64=\"\$DEVKIT_ROOT/VC/redist/x64/$MSVCR_DLL\""
echo-info "DEVKIT_VCRUNTIME_1_DLL_x86_64=\"\$DEVKIT_ROOT/VC/redist/x64/$VCRUNTIME_1_DLL\""
echo-info "DEVKIT_MSVCP_DLL_x86_64=\"\$DEVKIT_ROOT/VC/redist/x64/$MSVCP_DLL\""
echo-info "DEVKIT_UCRT_DLL_DIR_x86_64=\"\$DEVKIT_ROOT/10/Redist/ucrt/DLLs/x64\""
echo-info ""
echo-info "DEVKIT_TOOLCHAIN_PATH_aarch64=\"\$DEVKIT_ROOT/VC/bin/arm64:\$DEVKIT_ROOT/$SDK_VERSION/bin/x64:\$DEVKIT_ROOT/$SDK_VERSION/bin/x86\""
echo-info "DEVKIT_VS_INCLUDE_aarch64=\"\$DEVKIT_ROOT/VC/include;\$DEVKIT_ROOT/VC/atlmfc/include;\$DEVKIT_ROOT/$SDK_VERSION/include/shared;\$DEVKIT_ROOT/$SDK_VERSION/include/ucrt;\$DEVKIT_ROOT/$SDK_VERSION/include/um;\$DEVKIT_ROOT/$SDK_VERSION/include/winrt\""
echo-info "DEVKIT_VS_LIB_aarch64=\"\$DEVKIT_ROOT/VC/lib/arm64;\$DEVKIT_ROOT/VC/atlmfc/lib/arm64;\$DEVKIT_ROOT/$SDK_VERSION/lib/arm64\""
echo-info "DEVKIT_MSVCR_DLL_aarch64=\"\$DEVKIT_ROOT/VC/redist/arm64/$MSVCR_DLL\""
echo-info "DEVKIT_VCRUNTIME_1_DLL_aarch64=\"\$DEVKIT_ROOT/VC/redist/arm64/$VCRUNTIME_1_DLL\""
echo-info "DEVKIT_MSVCP_DLL_aarch64=\"\$DEVKIT_ROOT/VC/redist/arm64/$MSVCP_DLL\""
echo-info "DEVKIT_UCRT_DLL_DIR_aarch64=\"\$DEVKIT_ROOT/10/Redist/ucrt/DLLs/arm64\""
echo-info ""
echo-info "DEVKIT_TOOLS_VERSION=\"$TOOLS_VERSION\""
echo-info "DEVKIT_REDIST_VERSION=\"$REDIST_VERSION\""
echo-info "DEVKIT_SDK_VERSION=\"$SDK_FULL_VERSION\""
echo-info "DEVKIT_UCRT_VERSION=\"$UCRT_VERSION\""

################################################################################
# Copy this script

echo "Copying this script..."
cp $0 $DEVKIT_ROOT/

################################################################################
# Create bundle

echo "Creating bundle: $DEVKIT_BUNDLE"
(cd "$DEVKIT_ROOT" && tar zcf "$DEVKIT_BUNDLE" .)
