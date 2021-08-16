#
# Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

setup() {
    # Verify directory context variables are set
    if [ "${TESTJAVA}" = "" ] ; then
        echo "TESTJAVA not set. Test cannot execute.  Failed."
        exit 1
    fi

    if [ "${TESTCLASSES}" = "" ] ; then
        TESTCLASSES="."
    fi

    if [ "${TESTSRC}" = "" ] ; then
        TESTSRC="."
    fi

    OS=`uname -s`
    case ${OS} in
    Windows_* | CYGWIN*)
        PS=";"
        FS="\\"
        ;;
    *)
        PS=":"
        FS="/"
        ;;
    esac
}

verify_os() {
    OS=`uname -s`
    case ${OS} in
    Windows_95 | Windows_98 | Windows_ME | CYGWIN* )
        echo "Test bypassed: jvmstat feature not supported on ${OS}"
        exit 0
        ;;
    Windows_*)
        # verify that the tmp directory supports persistent ACLs, which
        # are required for jvmstat to enable its shared memory feature.
        # NOTE: FAT type files systems do not support ACLs, but NTFS files
        # systems do.
        #
        echo "temp directory is in: `windir -t`"
        TMPDRIVE=`windir -t | cut -d: -f1`
        if [ "${TMPDRIVE}" = "" ] ; then
            echo "Could not get temp directory drive letter"
            exit 1
        fi

        echo "temp file system characteristics:"
        sysinf drives -a | grep "^${TMPDRIVE}"
        sysinf drives -a | grep "^${TMPDRIVE}" | grep PERSISTENT_ACLS > /dev/null
        case $? in
        0)
            ;;
        1)
            echo "Test bypassed: jvmstat feature disabled because the temp"
            echo "directory doesn't support persistent ACLs"
            exit 0
            ;;
        2)
            echo "Could not get temp directory file system features"
            exit 1
            ;;
        *)
            echo "Unexpected return code from grep - $?"
            exit 1
            ;;
        esac
        ;;
    esac
}

# function to kill the process with the given process id
kill_proc() {
    kill_pid=$1

    if [ "${kill_pid}" = "" ]
    then
        echo "kill_proc(): null pid: ignored"
        return
    fi

    if [ ${kill_pid} -le 0 ]
    then
        echo "kill_proc(): invalid pid: ${kill_pid}: ignored"
        return
    fi

    OS=`uname -s`
    case $OS in
    Windows_*)
        case ${SHELL_VERSION} in
        [1234567].* | 8.[12345].*)
            # when starting processes in the background, mks creates an
            # intervening between the parent process and the background
            # process. The pid acquired for background process as acquired
            # by the $! shell variable is actually the pid of the invervening
            # shell and not that of the background process. Sending a specific
            # signal to the intervening shell will only effects the intervening 
            # shell and not the background process, thus leaving the background
            # process running. The following code assumes that the pid passed
            # into this function as ${kill_pid}, was acquired by $!. Therefore,
            # in order to kill the background process, we must first find the
            # children of ${kill_pid} (should be only one child) and kill them.
        
            ps -o pid,ppid | grep "${kill_pid}$"
            children=`mks_children ${kill_pid}`
            echo "killing children of ${kill_pid}: ${children}"
            for child in ${children} ; do
                kill_proc_common ${child}
            done
            ;;
        *)
            # in mks 8.6 and later, the pid returned in $! is now the pid
            # of the background process and not that of the intervening shell.
            kill_proc_common ${kill_pid}
            ;;
        esac
        ;;
    *)
        kill_proc_common ${kill_pid}
        ;;
    esac
}

mks_children() {
    ppid=$1
    ps -o pid,ppid | grep "${ppid}$" | awk '
BEGIN	{ pids="" }
	{ pids = pids $1 " " }
END	{ print pids }'
}

kill_proc_common() {
   kpid=$1

    # verify that the process exists and we can signal it
    kill -0 ${kpid} 2>/dev/null
    if [ $? -ne 0 ]
    then
        echo "Could not signal >${kpid}<"
        echo "user id = `id`"
        echo "process information :"
        ps -l -p ${kpid}
        return
    fi

    kill -TERM ${kpid}		# hit it easy first
    if [ $? -eq 0 ]
    then
        sleep 2
        kill -0 ${kpid} 2>/dev/null
        # check if it's still hanging around
        if [ $? -eq 0 ]
        then
            # it's still lingering, now it it hard
            kill -KILL ${kpid} 2>/dev/null
            if [ $? -ne 0 ]
            then
                echo "Could not kill ${kpid}!"
            fi
        fi
    else
        echo "Error sending term signal to ${kpid}!"
    fi
}

# check to see if a port is free
checkPort() # port
{
    inuse=`netstat -a | egrep "\.$1"`
    if [ "${inuse}" = "" ] ; then
      echo "free"
    else
      echo "inuse"
    fi
}

# Get a free port, where port+1 is also free, return 0 when giving up
freePort()
{
  start=3000
  while [ ${start} -lt 3030 ] ; do
    port1=`expr ${start} '+' $$ '%' 1000`
    port2=`expr ${port1} '+' 1`
    if [ "`checkPort ${port1}`" = "inuse" \
         -o "`checkPort ${port2}`" = "inuse" ] ; then
      start=`expr ${start} '+' 1`
    else
      break
    fi
  done
  if [ "`checkPort ${port1}`" = "inuse" \
       -o "`checkPort ${port2}`" = "inuse" ] ; then
    port1="0"
  fi
  echo "${port1}"
}

# Flags used by all jstat calls in jdk/sun/tools/jstat/*.sh
#
# The awk scripts parsing jstat output expect it to be in en-us locale. 
# Especially, we must force '.' instead of ',' in numbers.
COMMON_JSTAT_FLAGS="-J-XX:+UsePerfData -J-Duser.language=en -J-Duser.country=en"

