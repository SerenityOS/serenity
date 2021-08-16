#!/bin/bash -e
#
# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

# Create a bundle in the current directory, containing what's needed to run
# the 'autoconf' program by the OpenJDK build.

# Autoconf depends on m4, so download and build that first.
AUTOCONF_VERSION=2.69
M4_VERSION=1.4.18

PACKAGE_VERSION=1.0.1
TARGET_PLATFORM=linux_x86
MODULE_NAME=autoconf-$TARGET_PLATFORM-$AUTOCONF_VERSION+$PACKAGE_VERSION
BUNDLE_NAME=$MODULE_NAME.tar.gz

TMPDIR=`mktemp -d -t autoconfbundle-XXXX`
trap "rm -rf \"$TMPDIR\"" EXIT

ORIG_DIR=`pwd`
cd $TMPDIR
OUTPUT_DIR=$TMPDIR/$MODULE_NAME
mkdir -p $OUTPUT_DIR/usr

# Download and build m4

if test "x$TARGET_PLATFORM" = xcygwin_x64; then
  # On cygwin 64-bit, just copy the cygwin .exe file
  mkdir -p $OUTPUT_DIR/usr/bin
  cp /usr/bin/m4 $OUTPUT_DIR/usr/bin
elif test "x$TARGET_PLATFORM" = xcygwin_x86; then
  # On cygwin 32-bit, just copy the cygwin .exe file
  mkdir -p $OUTPUT_DIR/usr/bin
  cp /usr/bin/m4 $OUTPUT_DIR/usr/bin
elif test "x$TARGET_PLATFORM" = xlinux_x64; then
  M4_VERSION=1.4.13-5
  wget http://yum.oracle.com/repo/OracleLinux/OL6/latest/x86_64/getPackage/m4-$M4_VERSION.el6.x86_64.rpm
  cd $OUTPUT_DIR
  rpm2cpio ../m4-$M4_VERSION.el6.x86_64.rpm | cpio -d -i
elif test "x$TARGET_PLATFORM" = xlinux_x86; then
  M4_VERSION=1.4.13-5
  wget http://yum.oracle.com/repo/OracleLinux/OL6/latest/i386/getPackage/m4-$M4_VERSION.el6.i686.rpm
  cd $OUTPUT_DIR
  rpm2cpio ../m4-$M4_VERSION.el6.i686.rpm | cpio -d -i
else
  wget https://ftp.gnu.org/gnu/m4/m4-$M4_VERSION.tar.gz
  tar xzf m4-$M4_VERSION.tar.gz
  cd m4-$M4_VERSION
  ./configure --prefix=$OUTPUT_DIR/usr
  make
  make install
  cd ..
fi

# Download and build autoconf

wget https://ftp.gnu.org/gnu/autoconf/autoconf-$AUTOCONF_VERSION.tar.gz
tar xzf autoconf-$AUTOCONF_VERSION.tar.gz
cd autoconf-$AUTOCONF_VERSION
./configure --prefix=$OUTPUT_DIR/usr M4=$OUTPUT_DIR/usr/bin/m4
make
make install
cd ..

perl -pi -e "s!$OUTPUT_DIR/!./!" $OUTPUT_DIR/usr/bin/auto* $OUTPUT_DIR/usr/share/autoconf/autom4te.cfg
cp $OUTPUT_DIR/usr/share/autoconf/autom4te.cfg $OUTPUT_DIR/autom4te.cfg

cat > $OUTPUT_DIR/autoconf << EOF
#!/bin/bash
# Get an absolute path to this script
this_script_dir=\`dirname \$0\`
this_script_dir=\`cd \$this_script_dir > /dev/null && pwd\`

export M4="\$this_script_dir/usr/bin/m4"
export AUTOM4TE="\$this_script_dir/usr/bin/autom4te"
export AUTOCONF="\$this_script_dir/usr/bin/autoconf"
export AUTOHEADER="\$this_script_dir/usr/bin/autoheader"
export AC_MACRODIR="\$this_script_dir/usr/share/autoconf"
export autom4te_perllibdir="\$this_script_dir/usr/share/autoconf"

autom4te_cfg=\$this_script_dir/usr/share/autoconf/autom4te.cfg
cp \$this_script_dir/autom4te.cfg \$autom4te_cfg

echo 'begin-language: "M4sugar"' >> \$autom4te_cfg
echo "args: --prepend-include '"\$this_script_dir/usr/share/autoconf"'" >> \$autom4te_cfg
echo 'end-language: "M4sugar"' >> \$autom4te_cfg

exec \$this_script_dir/usr/bin/autoconf "\$@"
EOF
chmod +x $OUTPUT_DIR/autoconf
cd $OUTPUT_DIR
tar -cvzf ../$BUNDLE_NAME *
cd ..
cp $BUNDLE_NAME "$ORIG_DIR"
