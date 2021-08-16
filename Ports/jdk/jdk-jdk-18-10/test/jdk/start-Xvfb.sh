#!/bin/sh -f
#
# Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

#
# Original Author: Tim Bell
#
usage() {
   echo "Starts up an Xvfb dummy X server with fvwm2 window manager"
   echo " usage:"
   echo "    ${0} display_number_file"
   echo "        display_number_file gets display number when it's ready"
   exit
 }
#
currentDir=`pwd`
rm -f $1
DD=":$$"
DISPLAY=${DD}
export DISPLAY
cd /tmp
#
if [ ! -x "/usr/bin/X11/Xvfb" ]; then
  # We have Solaris-flavored X windows, and the /usr/openwin Xvfb is
  # a simple wrapper script around the Xsun server.  Massage the
  # arguments: server number must be first; others are slightly
  # different.
  #
  # Also the default Visual Class (DirectColor) triggers an awt bug
  # (probably 4131533/6505852) and some tests will loop endlessly
  # when they hit the display.  The workaround is:
  #  1) Ask for PseudoColor instead.
  #  2) Omit 32-bit depth.
  /usr/bin/nohup /usr/openwin/bin/Xvfb ${DISPLAY} -dev vfb screen 0 1280x1024x24 pixdepths 8 16 24 defclass PseudoColor > ${currentDir}/nohup.$$ 2>&1 &
else
  # Linux...
  /usr/bin/nohup /usr/bin/X11/Xvfb -fbdir ${currentDir} -pixdepths 8 16 24 32 ${DISPLAY} > ${currentDir}/nohup.$$ 2>&1 &
fi
WM="/usr/bin/X11/fvwm2"
#
# Wait for Xvfb to initialize:
sleep 5
#
if [ -x "${WM}" ]; then
# 2 JCK tests require a window manager
# mwm fails (key name errors) and twm fails (hangs),
# but fvwm2 works well.
  /usr/bin/nohup ${WM} -display ${DISPLAY} -replace -f /dev/null > ${currentDir}/nohup.$$ 2>&1 &
else
  echo "Error: ${WM} not found"
  exit 1
fi
#
# Wait some more to see if the xhost command gets through:
sleep 10
# Allow access to all - this is a brute force approach,
# but I do not see how it could be a security problem...
DISPLAY="${DD}" xhost +
#
echo "Virtual frame buffer started on ${DISPLAY}"
echo "$$" > $1
wait
