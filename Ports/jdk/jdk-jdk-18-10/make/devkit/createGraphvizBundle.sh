#!/bin/bash -e
#
# Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
# the 'dot' program from the graphviz suite by the OpenJDK build.

TMPDIR=`mktemp -d -t graphvizbundle-XXXX`
trap "rm -rf \"$TMPDIR\"" EXIT

ORIG_DIR=`pwd`
cd "$TMPDIR"
GRAPHVIZ_VERSION=2.38.0-1
PACKAGE_VERSION=1.1
TARGET_PLATFORM=linux_x64
BUNDLE_NAME=graphviz-$TARGET_PLATFORM-$GRAPHVIZ_VERSION+$PACKAGE_VERSION.tar.gz
wget http://www.graphviz.org/pub/graphviz/stable/redhat/el6/x86_64/os/graphviz-$GRAPHVIZ_VERSION.el6.x86_64.rpm
wget http://www.graphviz.org/pub/graphviz/stable/redhat/el6/x86_64/os/graphviz-libs-$GRAPHVIZ_VERSION.el6.x86_64.rpm
wget http://www.graphviz.org/pub/graphviz/stable/redhat/el6/x86_64/os/graphviz-plugins-core-$GRAPHVIZ_VERSION.el6.x86_64.rpm
wget http://www.graphviz.org/pub/graphviz/stable/redhat/el6/x86_64/os/graphviz-plugins-x-$GRAPHVIZ_VERSION.el6.x86_64.rpm
wget http://public-yum.oracle.com/repo/OracleLinux/OL6/latest/x86_64/getPackage/libtool-ltdl-2.2.6-15.5.el6.x86_64.rpm

mkdir graphviz
cd graphviz
for rpm in ../*.rpm; do
  rpm2cpio $rpm | cpio --extract --make-directories
done

cat > dot << EOF
#!/bin/bash
# Get an absolute path to this script
this_script_dir=\`dirname \$0\`
this_script_dir=\`cd \$this_script_dir > /dev/null && pwd\`
export LD_LIBRARY_PATH="\$this_script_dir/usr/lib64:\$LD_LIBRARY_PATH"
exec \$this_script_dir/usr/bin/dot "\$@"
EOF
chmod +x dot
export LD_LIBRARY_PATH="$TMPDIR/graphviz/usr/lib64:$LD_LIBRARY_PATH"
# create config file
./dot -c
tar -cvzf ../$BUNDLE_NAME *
cp ../$BUNDLE_NAME "$ORIG_DIR"
