#! /bin/sh
#
# Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

# Parse the first contiguous comment block in this script and generate
# a java comment block. If this script is invoked with a copyright
# year/year range, the java comment block will contain a Sun copyright.

COPYRIGHT_YEARS="$1"

cat <<__END__
/*
__END__

if [ "x$COPYRIGHT_YEARS" != x ]; then
  cat <<__END__
 * Copyright (c) $COPYRIGHT_YEARS Oracle and/or its affiliates. All rights reserved.
__END__
fi

$AWK ' /^#.*Copyright.*Oracle/ { next }
    /^#([^!]|$)/ { sub(/^#/, " *"); print }
    /^$/ { print " */"; exit } ' $0
