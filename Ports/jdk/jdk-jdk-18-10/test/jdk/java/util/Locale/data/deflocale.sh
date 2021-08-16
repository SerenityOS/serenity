#!/bin/sh
# 
# Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
#
#
#
# A simple tool to output all the installed locales on a Unix machine, and 
# corresponding Java default locale/file.encoding using PrintDefaultLocale
#
# Usage: "deflocale.sh <java launcher>
#
cat /etc/*release
uname -a
echo "Testing all available locales"
/usr/bin/locale -a | while read line; do
    echo ""
    echo "OS Locale: " $line
    env LC_ALL= LC_CTYPE= LC_MESSAGES= LANG=$line $1 $2 $3 $4 $5 $6 $7 $8 $9 PrintDefaultLocale
done

echo ""
echo "Testing some typical combinations"
echo ""
while read lcctype lcmessages; do
    if [ "$lcctype" = "#" -o "$lcctype" = "" ]; then
        continue
    fi
    echo ""
    echo "OS Locale (LC_CTYPE: "$lcctype", LC_MESSAGES: "$lcmessages")"
    env LC_ALL= LC_CTYPE=$lcctype LC_MESSAGES=$lcmessages $1 $2 $3 $4 $5 $6 $7 $8 $9 PrintDefaultLocale
done < deflocale.input
