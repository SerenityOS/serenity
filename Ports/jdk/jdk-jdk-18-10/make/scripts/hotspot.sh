#!/bin/sh

# Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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


# This script launches HotSpot.
#
# If the first parameter is either "-gdb" or "-gud", HotSpot will be
# launched inside gdb. "-gud" means "open an Emacs window and run gdb
# inside Emacs".
#
# If the first parameter is "-dbx", HotSpot will be launched inside dbx.
#
# If the first parameter is "-valgrind", HotSpot will be launched
# inside Valgrind (http://valgrind.kde.org) using the Memcheck skin,
# and with memory leak detection enabled.  This currently (2005jan19)
# requires at least Valgrind 2.3.0.  -Xmx16m will also be passed as
# the first parameter to HotSpot, since lowering HotSpot's memory
# consumption makes execution inside of Valgrind *a lot* faster.
#


#
# User changeable parameters ------------------------------------------------
#

# This is the name of the gdb binary to use
if [ ! "$GDB" ]
then
    GDB=gdb
fi

# This is the name of the dbx binary to use
if [ ! "$DBX" ]
then
    DBX=dbx
fi

# This is the name of the Valgrind binary to use
if [ ! "$VALGRIND" ]
then
    VALGRIND=valgrind
fi

# This is the name of Emacs for running GUD
EMACS=emacs

#
# End of user changeable parameters -----------------------------------------
#

OS=`uname -s`

# Make sure the paths are fully specified, i.e. they must begin with /.
REL_MYDIR=`dirname $0`
MYDIR=`cd $REL_MYDIR && pwd`
case "$OS" in
CYGWIN*)
    MYDIR=`cygpath -m "$MYDIR"`
    ;;
esac

#
# Look whether the user wants to run inside gdb
case "$1" in
    -gdb)
        MODE=gdb
        shift
        ;;
    -gud)
        MODE=gud
        shift
        ;;
    -dbx)
        MODE=dbx
        shift
        ;;
    -valgrind)
        MODE=valgrind
        shift
        ;;
    *)
        MODE=run
        ;;
esac

if [ "${ALT_JAVA_HOME}" != "" ]; then
    JDK=${ALT_JAVA_HOME%%/jre}
else
    JDK=@@JDK_IMPORT_PATH@@
fi

if [ "${JDK}" != "" ]; then
    case "$OS" in
    CYGWIN*)
        JDK=`cygpath -m "$JDK"`
        ;;
	esac

else
    echo "Failed to find JDK." \
        "Either ALT_JAVA_HOME is not set or JDK_IMPORT_PATH is empty."
    exit 1
fi

# We will set the LD_LIBRARY_PATH as follows:
#     o		$JVMPATH (directory portion only)
#     o		$JRE/lib
# followed by the user's previous effective LD_LIBRARY_PATH, if
# any.
JRE=$JDK
JAVA_HOME=$JDK
export JAVA_HOME

SBP=${MYDIR}:${JRE}/lib


# Set up a suitable LD_LIBRARY_PATH or DYLD_LIBRARY_PATH
if [ "${OS}" = "Darwin" ]
then
    if [ -z "$DYLD_LIBRARY_PATH" ]
    then
        DYLD_LIBRARY_PATH="$SBP"
    else
        DYLD_LIBRARY_PATH="$SBP:$DYLD_LIBRARY_PATH"
    fi
    export DYLD_LIBRARY_PATH
else
    # not 'Darwin'
    if [ -z "$LD_LIBRARY_PATH" ]
    then
        LD_LIBRARY_PATH="$SBP"
    else
        LD_LIBRARY_PATH="$SBP:$LD_LIBRARY_PATH"
    fi
    export LD_LIBRARY_PATH
fi

JPARMS="-XXaltjvm=$MYDIR -Dsun.java.launcher.is_altjvm=true";

# Locate the java launcher
LAUNCHER=$JDK/bin/java
if [ ! -x $LAUNCHER ] ; then
    echo Error: Cannot find the java launcher \"$LAUNCHER\"
    exit 1
fi

GDBSRCDIR=$MYDIR
BASEDIR=`cd $MYDIR/../../.. && pwd`
case "$OS" in
CYGWIN*)
    BASEDIR=`cygpath -m "$BASEDIR"`
    ;;
esac

init_gdb() {
# Create a gdb script in case we should run inside gdb
    GDBSCR=/tmp/hsl.$$
    rm -f $GDBSCR
    cat >>$GDBSCR <<EOF
cd `pwd`
handle SIGUSR1 nostop noprint
handle SIGUSR2 nostop noprint
directory $GDBSRCDIR
# Get us to a point where we can set breakpoints in libjvm.so
set breakpoint pending on
break JNI_CreateJavaVM
run
# Stop in JNI_CreateJavaVM
delete 1
# We can now set breakpoints wherever we like
EOF
}

case "$MODE" in
    gdb)
	init_gdb
        $GDB -x $GDBSCR --args $LAUNCHER $JPARMS "$@" $JAVA_ARGS
	rm -f $GDBSCR
        ;;
    gud)
	init_gdb
# First find out what emacs version we're using, so that we can
# use the new pretty GDB mode if emacs -version >= 22.1
	case `$EMACS -version 2> /dev/null` in
	    *GNU\ Emacs\ 2[23]*)
	    emacs_gud_cmd="gdba"
	    emacs_gud_args="--annotate=3"
	    ;;
	    *)
		emacs_gud_cmd="gdb"
		emacs_gud_args=
		;;
	esac
        $EMACS --eval "($emacs_gud_cmd \"$GDB $emacs_gud_args -x $GDBSCR\")";
	rm -f $GDBSCR
        ;;
    dbx)
        $DBX -s $HOME/.dbxrc -c "loadobject -load libjvm.so; stop in JNI_CreateJavaVM; run $JPARMS $@ $JAVA_ARGS; delete all" $LAUNCHER
        ;;
    valgrind)
        echo Warning: Defaulting to 16Mb heap to make Valgrind run faster, use -Xmx for larger heap
        echo
        $VALGRIND --tool=memcheck --leak-check=yes --num-callers=50 $LAUNCHER -Xmx16m $JPARMS "$@" $JAVA_ARGS
        ;;
    run)
        LD_PRELOAD=$PRELOADING exec $LAUNCHER $JPARMS "$@" $JAVA_ARGS
        ;;
    *)
        echo Error: Internal error, unknown launch mode \"$MODE\"
        exit 1
        ;;
esac
RETVAL=$?
exit $RETVAL
