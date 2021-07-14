#! /bin/sh

# $NetBSD: bootstrap,v 1.300 2021/05/30 23:41:05 khorben Exp $
#
# Copyright (c) 2001-2011 Alistair Crooks <agc@NetBSD.org>
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
# IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
# OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
# IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
# NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
# THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
#set -x

# the following environment variables are honored:
# compiler/linker flags: CFLAGS, CPPFLAGS, LDFLAGS, LIBS
# tools: CP, GREP, ID, MKDIR, SH, TEST, TOUCH, XARGS

# The bootstrap program must be able to run with very limited shells.
# It must not use any of the following features:
#
# * the ${var%pattern} or ${var#pattern} expansions
# * the $(command) subshell

# Don't let the bootstrap program get confused by a pre-existing mk.conf
# file.
MAKECONF=/dev/null
export MAKECONF

# No vulnerability checks since there might be an incompatible checker installed
NO_PKGTOOLS_REQD_CHECK=yes
export NO_PKGTOOLS_REQD_CHECK

unprivileged=no

preserve_path=no

# where the building takes place
bootstrapdir=`dirname "$0"`
bootstrapdir=`cd "${bootstrapdir}" && pwd`
pkgsrcdir=`dirname "${bootstrapdir}"`
wrkdir="`pwd`/work"

usage="Usage: $0 "'
    [ --abi [32|64] ]
    [ --binary-kit <tarball> ]
    [ --compiler <compiler> ]
    [ --cwrappers <auto|yes|no> ]
    [ --full ]
    [ --gzip-binary-kit <tarball> ]
    [ --help ]
    [ --make-jobs <num> ]
    [ --mk-fragment <mk.conf> ]
    [ --pkgdbdir <pkgdbdir> ]
    [ --pkginfodir <pkginfodir> ]
    [ --pkgmandir <pkgmandir> ]
    [ --prefer-pkgsrc <list|yes|no> ]
    [ --prefer-native <list|yes|no> ]
    [ --prefix <prefix> ]
    [ --preserve-path ]
    [ --quiet ]
    [ --sysconfbase <sysconfbase> ]
    [ --sysconfdir <sysconfdir> ]
    [ --unprivileged | --ignore-user-check ]
    [ --varbase <varbase> ]
    [ --workdir <workdir> ]
'

# strip / for BSD/OS, strip - for HP-UX
opsys?=`uname -s | tr -d /-`

mkbinarykit_tar()
{
	# in case tar was built by bootstrap
	PATH="$prefix/bin:$PATH"; export PATH
	cd / && tar -hcf "${binary_kit}" .$prefix .$pkgdbdir .$etc_mk_conf
}

mkbinarykit_tgz()
{
	# in case tar was built by bootstrap
	PATH="$prefix/bin:$PATH"; export PATH
	cd / && tar -hcf - .$prefix .$pkgdbdir .$etc_mk_conf | gzip > "${binary_gzip_kit}"
}

die()
{
	echo >&2 "$*"
	exit 1
}

echo_msg()
{
	echo "===> $*"
}

# see if we're using gcc.  If so, set $compiler_is_gnu to '1'.
get_compiler()
{
	testcc="${CC}"
	# normally, we'd just use 'cc', but certain configure tools look
	# for gcc specifically, so we have to see if that comes first
	if [ -z "${testcc}" ]; then
		save_IFS="${IFS}"
		IFS=':'
		for dir in ${PATH}; do
			test -z "$dir" && dir=.
			if [ -x "$dir/gcc" ]; then
				testcc="$dir/gcc"
				break
			fi
		done
		IFS="${save_IFS}"
	fi

	# Clang compiler pretends to be GCC, so we have to check that
	cat >${wrkdir}/$$.c <<EOF
#ifdef __clang__
indeed
#endif
EOF
	compiler_is_clang=`${testcc:-cc} -E ${wrkdir}/$$.c 2>/dev/null | grep -c indeed`
	rm -f ${wrkdir}/$$.c

	cat >${wrkdir}/$$.c <<EOF
#ifdef __GNUC__
#ifndef __clang__
indeed
#endif
#endif
EOF
	compiler_is_gnu=`${testcc:-cc} -E ${wrkdir}/$$.c 2>/dev/null | grep -c indeed`
	rm -f ${wrkdir}/$$.c

}
get_abi()
{
	abi_opsys=$@

	if [ -n "$abi" ]; then
		case "$abi_opsys" in
		IRIX)
			die "ERROR: $abi_opsys has special ABI handling, --abi not supported (yet)."
			;;
		esac
	fi

	case "$abi_opsys" in
	IRIX)
		if [ `uname -r` -ge 6 ]; then
		abi=`sed -e 's/.*\(abi=\)\([on]*[36][24]\).*/\2/' /etc/compiler.defaults`
		isa=`sed -e 's/.*\(isa=mips\)\([1234]\).*/\2/' /etc/compiler.defaults`
		case "$abi" in
		o32)
			imakeopts="-DBuildO32 -DSgiISAo32=$isa"
			abi=""
			;;
		n32)	imakeopts="-DBuildN32 -DSgiISA32=$isa"
			abi="32"
			;;
		64 | n64)
			imakeopts="-DBuild64bit -DSgiISA64=$isa"
			abi="64"
			;;
		esac
		else # IRIX before 6
		abi=32
		fi
		;;
	esac
}

get_machine_arch_aix()
{
	_cpuid=`/usr/sbin/lsdev -C -c processor -S available | sed 1q | awk '{ print $1 }'`
	if /usr/sbin/lsattr -El $_cpuid | grep ' POWER' >/dev/null 2>&1; then
		echo rs6000
	else
		echo powerpc
	fi
}

get_machine_arch_darwin()
{
	case `uname -p` in
	arm)
		echo "aarch64"
		;;
	i386)
		# Returns "i386" or "x86_64" depending on CPU
		echo `uname -m`
		;;
	powerpc)
		echo "powerpc"
		;;
	esac
}

check_prog()
{
	_var="$1"; _name="$2"

	eval _tmp=\"\$$_var\"
	if [ "x$_tmp" != "x" ]; then
		# Variable is already set (by the user, for example)
		return 0
	fi

	for _d in `echo $PATH | tr ':' ' '`; do
		if [ -f "$_d/$_name" ] && [ -x "$_d/$_name" ]; then
			# Program found
			eval $_var=\""$_d/$_name"\"
			return 1
		fi
	done

	die "$_name not found in path."
}

opsys_finish()
{
	case "$opsys" in
	IRIX)
		if [ -n "$imakeopts" ]; then
			echo "IMAKEOPTS+=		$imakeopts" >> ${TARGET_MKCONF}
		fi
		if [ `uname -r` -lt 6 ]; then
		        echo_msg "Installing fake ldd script"
        		run_cmd "$install_sh -c -o $user -g $group -m 755 $pkgsrcdir/pkgtools/bootstrap-extras/files/fakeldd $prefix/sbin"
			need_extras=yes
			echo "LDD=			$prefix/sbin/fakeldd" >> ${TARGET_MKCONF}
		fi
		;;
	Haiku)
		need_extras=yes
		echo "LDD=			$prefix/sbin/fakeldd" >> ${TARGET_MKCONF}
		;;
	esac
}

is_root()
{
	if [ `uname -s` = "IRIX" ]; then
		if [ `uname -r` -lt 6  -a -z "$ID" ]; then
	# older version of IRIX have an id command with limited features
			if [ "`$idprog`" != "uid=0(root) gid=0(sys)" ]; then
				return 1
			fi
			return 0
		fi
	fi
	if [ `$idprog -u` != 0 ]; then
		return 1
	fi
	return 0
}

# run a command, abort if it fails
run_cmd()
{
	echo_msg "running: $*"
	eval "$@"
	ret=$?
        if [ $ret -ne 0 ]; then
		echo_msg "exited with status $ret"
		die "aborted."
	fi
}

# Some versions of mkdir (notably SunOS) bail out too easily, so use the
# install-sh wrapper instead.
mkdir_p()
{
	for dir in "$@"; do
		run_cmd "$install_sh -d -o $user -g $group $dir"
	done
}

mkdir_p_early()
{
	[ -d "$1" ] && return 0
	mkdir -p "$1" 2> /dev/null && return 0
	parent=`dirname "$1"`
	mkdir_p_early "$parent"
	if [ ! -d "$1" ] && mkdir "$1"; then
		echo_msg "mkdir $1 exited with status $?"
		die "aborted."
	fi
	return 0
}

copy_src()
{
	_src="$1"; _dst="$2"
	if [ ! -d $wrkdir/$_dst ]; then
		mkdir_p $wrkdir/$_dst
	fi
	$cpprog -r $_src/* $wrkdir/$_dst
	if [ -f $wrkdir/$_dst/config.guess ]; then
		$cpprog $pkgsrcdir/mk/gnu-config/config.guess $wrkdir/$_dst/
	fi
	if [ -f $wrkdir/$_dst/config.sub ]; then
		$cpprog $pkgsrcdir/mk/gnu-config/config.sub $wrkdir/$_dst/
	fi
}

get_optarg()
{
	expr "x$1" : "x[^=]*=\\(.*\\)"
}

# The --prefer-pkgsrc and --prefer native options require an argument, 
# but our manual getopt parser is unable to detect the difference 
# between a valid argument and the next getopt option (or no option 
# at all if it's the last).  Ensure that the argument does not begin 
# with "-" or is empty in case the user forgets to provide one.
checkarg_missing()
{
	case $1 in
	-*|"")
		die "ERROR: $2 takes yes, no or a list"
		;;
	esac
}

checkarg_sane_absolute_path()
{
	case "$1" in
	"")	;; # the default value will be used.
	*[!-A-Za-z0-9_./]*)
		die "ERROR: Invalid characters in path $1 (from $2)." ;;
	*/)	die "ERROR: The argument to $2 must not end in /." ;;
	*//* | */. | */./* | */.. | */../*)
		die "ERROR: The path $1 (from $2) must be canonical." ;;
	/*)	checkarg_no_symlink_path "$1" "$2" ;;
	*)	die "ERROR: The argument to $2 must be an absolute path." ;;
	esac
}

checkarg_no_symlink_path()
{
	_dir=$1
	while [ ! -d "$_dir" ]; do
		_dir=`dirname "$_dir"`
		[ "$_dir" ] || _dir="/"
	done

	_realdir=`cd "$_dir" && exec pwd`
	[ "$_realdir" = "$_dir" ] && return

	die "ERROR: The path $1 (from $2) must not contain symlinks.

	Given path   : $1
	Resolved path: $_realdir${1##${_dir}}

	Several packages assume that the given path of $2 stays the same
	when symlinks are resolved. When that assumption fails, they will:

	* not find some include files or libraries during the build phase
	  since the files from dependencies are not installed in
	  \${WRKDIR}/.buildlink.

	* install their files into the wrong path inside \${WRKDIR}/.destdir,
	  which will fail the PLIST check during the install phase."
}

checkarg_sane_relative_path() {
	case "$1" in
	"")	;; # the default value will be used.
	*[!-A-Za-z0-9_./]*)
		die "ERROR: Invalid characters in path $1 (from $2)." ;;
	/*)	die "ERROR: The argument to $2 must be a relative path." ;;
	*)	;;
	esac
}

bootstrap_sh=${SH-/bin/sh}
bootstrap_sh_set=${SH+set}

case "$bootstrap_sh" in
/*)
	;;
*)
	die "ERROR: The variable SH must contain an absolute path"
	;;
esac

if [ -n "$PKG_PATH" ]; then
	die "ERROR: Please unset PKG_PATH before running bootstrap."
fi

build_start=`date`
echo_msg "bootstrap command: $0 $*"
echo_msg "bootstrap started: $build_start"

# ensure system locations are empty; we will set them later when we know
# whether they will be system wide or user specific
prefix=
pkgdbdir=
pkginfodir=
pkgmandir=
sysconfbase=
sysconfdir=
varbase=

compiler=""
cwrappers=auto
full=no
make_jobs=1
mk_fragment=
quiet=no

# Set these variables so that we can test whether they have been
# correctly enabled by the user and not left empty
prefer_native=unset
prefer_pkgsrc=unset

while [ $# -gt 0 ]; do
	case $1 in
	--workdir=*)	wrkdir=`get_optarg "$1"` ;;
	--workdir)	wrkdir="$2"; shift ;;
	--prefix=*)	prefix=`get_optarg "$1"` ;;
	--prefix)	prefix="$2"; shift ;;
	--pkgdbdir=*)	pkgdbdir=`get_optarg "$1"` ;;
	--pkgdbdir)	pkgdbdir="$2"; shift ;;
	--pkginfodir=*)	pkginfodir=`get_optarg "$1"` ;;
	--pkginfodir)	pkginfodir="$2"; shift ;;
	--pkgmandir=*)	pkgmandir=`get_optarg "$1"` ;;
	--pkgmandir)	pkgmandir="$2"; shift ;;
	--sysconfbase=*)sysconfbase=`get_optarg "$1"` ;;
	--sysconfbase)	sysconfbase="$2"; shift ;;
	--sysconfdir=*)	sysconfdir=`get_optarg "$1"` ;;
	--sysconfdir)	sysconfdir="$2"; shift ;;
	--varbase=*)	varbase=`get_optarg "$1"` ;;
	--varbase)	varbase="$2"; shift ;;
	--compiler=*)	compiler=`get_optarg "$1"` ;;
	--compiler)	compiler="$2"; shift ;;
	--abi=*)	abi=`get_optarg "$1"` ;;
	--abi)		abi="$2"; shift ;;
	--cwrappers=*)	cwrappers=`get_optarg "$1"` ;;
	--cwrappers)	cwrappers="$2"; shift ;;
	--unprivileged | --ignore-user-check) unprivileged=yes ;;
	--prefer-pkgsrc=*)
			prefer_pkgsrc=`get_optarg "$1"` ;;
	--prefer-pkgsrc)
			prefer_pkgsrc="$2"; shift ;;
	--prefer-native=*)
			prefer_native=`get_optarg "$1"` ;;
	--prefer-native)
			prefer_native="$2"; shift ;;
	--preserve-path) preserve_path=yes ;;
	--mk-fragment=*)
			mk_fragment=`get_optarg "$1"` ;;
	--mk-fragment)
			mk_fragment="$2"; shift ;;
	--binary-kit=*)
			binary_kit=`get_optarg "$1"` ;;
	--binary-kit)
			binary_kit="$2"; shift ;;
	--gzip-binary-kit=*)
			binary_gzip_kit=`get_optarg "$1"` ;;
	--gzip-binary-kit)
			binary_gzip_kit="$2"; shift ;;
	--make-jobs=*)	make_jobs=`get_optarg "$1"` ;;
	--make-jobs)	make_jobs="$2"; shift ;;
	--full)		full=yes ;;
	--quiet)	quiet=yes ;;
	--help)		echo "$usage"; exit ;;
	-h)		echo "$usage"; exit ;;
	-*)		echo "${0##*/}: unknown option \"$1\"" 1>&2
			echo "$usage" 1>&2; exit 1 ;;
	esac
	shift
done

checkarg_sane_absolute_path "$pkgdbdir" "--pkgdbdir"
checkarg_sane_absolute_path "$sysconfbase" "--sysconfbase"
checkarg_sane_absolute_path "$sysconfdir" "--sysconfdir"
checkarg_sane_absolute_path "$varbase" "--varbase"
checkarg_sane_relative_path "$pkginfodir" "--pkginfodir"
checkarg_sane_relative_path "$pkgmandir" "--pkgmandir"
checkarg_sane_absolute_path "$wrkdir" "--workdir"
checkarg_missing "$prefer_pkgsrc" "--prefer-pkgsrc" 
checkarg_missing "$prefer_native" "--prefer-native" 


# set defaults for system locations if not already set by the user
wrkobjdir=${wrkdir}/pkgsrc
if [ "$unprivileged" = "yes" ]; then
	[ -z "$prefix" ] && prefix=${HOME}/pkg
elif [ -z "$prefix" -o "$prefix" = "/usr/pkg" ]; then
	prefix=/usr/pkg
	[ -z "$sysconfbase" ] && sysconfbase=/etc
	[ -z "$varbase" ] && varbase=/var
fi
checkarg_sane_absolute_path "$prefix" "--prefix"

[ -z "$varbase" ] && varbase=${prefix}/var
[ -z "$pkgdbdir" ] && pkgdbdir=${prefix}/pkgdb

if [ "$prefix" = "/usr" ]; then
	[ -z "$pkginfodir" ] && pkginfodir=share/info
	[ -z "$pkgmandir" ] && pkgmandir=share/man
	[ -z "$sysconfbase" ] && sysconfbase=/etc
else
	[ -z "$pkginfodir" ] && pkginfodir=info
	[ -z "$pkgmandir" ] && pkgmandir=man
	[ -z "$sysconfbase" ] && sysconfbase=${prefix}/etc
fi
infodir=${prefix}/${pkginfodir}
mandir=${prefix}/${pkgmandir}
[ -z "$sysconfdir" ] && sysconfdir=${prefix}/etc

if [ "x$preserve_path" != "xyes" ]; then
	PATH="$PATH:/sbin:/usr/sbin"
fi

if [ "$prefer_native" = "unset" ]; then
	prefer_native=
fi
if [ "$prefer_pkgsrc" = "unset" ]; then
	prefer_pkgsrc=
fi

configure_cross_args=--host=${HOST}
overpath=""
root_user=root
bmakexargs=
need_awk=no
need_bsd_install=no
need_extras=no
need_sed=no
need_xargs=no
set_opsys=no
use_bsdinstall=

case "$opsys" in
AIX)
	root_group=system
	need_bsd_install=yes
	need_awk=yes
	need_sed=yes
	need_ksh=yes
	need_fixed_strip=yes
	machine_arch=`get_machine_arch_aix`
	;;
Bitrig)
	root_group=wheel
	machine_arch=`arch -s`
	check_compiler=yes
	;;
CYGWIN_*)
	is_root () {
		if id -nG | grep -q 'Administrators'; then
			return 0
		fi
		return 1
	}
	root_user=Administrators
	root_group=Administrators
	opsys=`uname -o`
	need_sed=yes
	machine_arch=`uname -m`
	# only used for unprivileged builds.
	whoamiprog='id -u'
	groupsprog='id -g'
	;;
Darwin)
	root_group=wheel
	machine_arch=`get_machine_arch_darwin`
	CC=${CC:-"cc -isystem /usr/include"}; export CC
	check_compiler=yes
	osrev=`uname -r`

	# Combine major.minor product version for simpler numerical tests.
	macos_version=`sw_vers -productVersion | \
	    awk -F. '{ printf("%02d%02d", $1, $2) }'`

	# Newer native sed does not support multibyte correctly.
	if [ $macos_version -ge 1008 ]; then
		need_awk=yes
		need_sed=yes
	fi

	# Avoid system shells on macOS versions that enable System Integrity
	# Protection (SIP) as it affects packages that rely on variables such
	# as LD_LIBRARY_PATH.  SIP unsets any variables that may affect
	# security when using system binaries, i.e. /bin/*sh, but using a
	# non-system shell is unaffected, at least for now.
	if [ $macos_version -ge 1011 ]; then
		need_mksh=yes
	fi

	unset osrev macos_version
	;;
DragonFly)
	root_group=wheel
	check_prog tarprog tar
	machine_arch=`uname -p`
	;;
FreeBSD)
	root_group=wheel
	machine_arch=`uname -p`
	check_compiler=yes
	;;
FreeMiNT)
	root_group=root
	machine_arch=m68k
	;;
GNUkFreeBSD)
	root_group=root
	need_awk=yes
	machine_arch=`uname -m`
	;;
Haiku)
	root_user=`id -un`
	root_group=root
	case `uname -m` in
	BeMac)
		machine_arch=powerpc
		;;
	BePC)
		machine_arch=i386
		;;
	*)
		machine_arch=`uname -p`
		;;
	esac
	;;
HPUX)
	root_group=sys
	need_bsd_install=yes
	need_awk=yes
	need_sed=yes
	machine_arch=`uname -m | sed 's/^9000.*$/hppa/'`
	;;
Interix)
	is_root () {
		if id -G | grep -q 131616; then
			return 0
		fi
		return 1
	}
	mkdir_p () {
		mkdir -p "$@" # allows umask to take effect
	}
	default_install_mode=0775
	root_user=`id -un`
	root_group=+Administrators
	case `uname -r` in
	3.* | 5.*)
		need_bsd_install=yes
		need_awk=yes
		need_sed=yes
		need_xargs=yes
		;;
	esac
	# only used for unprivileged builds
	groupsprog="id -gn"
	# for bootstrap only; pkgsrc uses CPPFLAGS
	CC="gcc -D_ALL_SOURCE"; export CC
	ac_cv_header_poll_h=no; export ac_cv_header_poll_h
	ac_cv_func_poll=no; export ac_cv_func_poll
	;;
IRIX*)
	if [ -d "/usr/freeware/bin" ]; then
		overpath="/usr/freeware/bin:$overpath"
	fi
	if [ -d "/usr/bsd" ]; then
		overpath="/usr/bsd:$overpath"
	fi
	if [ -d "/usr/bsd/bin" ]; then
		overpath="/usr/bsd/bin:$overpath"
	fi
	root_group=sys
	need_bsd_install=yes
	get_abi "IRIX"
	opsys=IRIX
	need_awk=yes
	need_sed=yes
	set_opsys=yes
	machine_arch=mipseb
	check_compiler=yes
	if [ `uname -r` -lt 6 ]; then
# IRIX 5's mkdir bails out with an error when trying to create with the -p
# option an already existing directory
		need_mkdir=yes
	fi
	;;
Linux)
	if [ -f /etc/ssdlinux_version ]; then
		root_group=wheel
	else
		root_group=root
	fi
	# Debian/Ubuntu's awk is mawk, and mawk does not understand
	# some regexp used in pkgsrc/mk.
	if [ -f /etc/debian_version ]; then
		need_awk=yes
	# Arch uses gawk 5 that breaks some regexps. It doesn't provide pax
	# anymore.
	elif [ -f /etc/arch-release ]; then
		need_awk=yes
		need_pax=yes
	elif grep -sq '^CHROMEOS_RELEASE_NAME' /etc/lsb-release; then
		need_awk=yes
		need_sed=yes
	fi
	machine_arch=`uname -m`
	# Override machine_arch where required.
	case "$machine_arch" in
	i?86)		machine_arch=i386 ;;
	ppc64le)	machine_arch=powerpc64le ;;
	esac
	;;
MidnightBSD)
	root_group=wheel
	machine_arch=`uname -p`
	check_compiler=yes
	;;
Minix)
	root_group=wheel
	machine_arch=`uname -p`
	check_compiler=yes
	;;	
MirBSD)
	root_group=wheel
	need_pax=yes
	machine_arch=`arch -s`
	# there is no /usr/bin/cc, so use mgcc if unset
	test -n "$CC" || { CC=mgcc; export CC; }
	# get some variables from the native make if unset
	for var in CFLAGS CPPFLAGS LDFLAGS; do
		# check if variable is already set
		eval _tmp=\"\$$var\"
		[ "x$_tmp" != x ] && continue
		# ask the native make (EXPERIMENTAL = don't add -Werror)
		# the -I${.CURDIR} dance is to prevent junk in CPPFLAGS
		_tmp=`printf '%s\nall:\n\t@%s %%s %s=${%s:M*:Q:Q}\n%s\n%s\n' \
		    $var'+=-I${.CURDIR}' printf $var $var':S/-I${.CURDIR}//' \
		    EXPERIMENTAL=yes '.include <bsd.prog.mk>' | \
		    (unset MAKECONF; /usr/bin/make -f - all 2>/dev/null) | \
		    sed 's/^x//'`
		eval $_tmp
		eval export $var
	done
	;;
NetBSD)
	root_group=wheel
	machine_arch=`uname -p`
	;;
OpenBSD)
	root_group=wheel
	machine_arch=`arch -s`
	CC=${CC:-cc}; export CC
	check_compiler=yes
	;;
OSF1)
	root_group=system
	need_bsd_install=yes
	need_awk=yes
	need_sed=yes
	need_mksh=yes
	machine_arch=`uname -p`
	;;
QNX)
	root_group=root
	need_bsd_install=yes
	need_awk=yes
	need_sed=yes
	groupsprog="id -gn"
	whoamiprog="id -un"
	machine_arch=`uname -p | sed -e 's/x86/i386/'`
	# libarchive checks for "rm -f" with no arguments being accepted. This
	# does not work on QNX. Interestingly, libarchive doesn't seem to
	# actually need it during the build.
	export ACCEPT_INFERIOR_RM_PROGRAM=yes
	;;
SCO_SV)
	root_group=root
	need_awk=yes
	need_bsd_install=yes
	need_sed=yes
	whoamiprog='id -u'
	groupsprog='id -g'
	# /bin/sh under OpenServer 5.0.7/3.2 breaks bmake tests.
	#bmakexargs="$bmakexargs --with-defshell=/bin/ksh"
	;;
SunOS)
	root_group=root
	need_bsd_install=yes
	use_bsdinstall=yes
	if [ ! -x "/usr/gnu/bin/awk" ]; then
		need_awk=yes
	fi
	if [ ! -x "/usr/gnu/bin/sed" ]; then
		need_sed=yes
	fi
	if [ -x "/usr/bin/bash" ]; then
		bootstrap_sh=${SH:-/usr/bin/bash}
		bootstrap_sh_set=set
	else
		need_mksh=yes
	fi
	idprog="/usr/xpg4/bin/id"
	groupsprog="${idprog} -gn"
	whoamiprog="${idprog} -un"
	machine_arch=`isainfo -k`
	check_compiler=yes
	check_ssp=yes
	;;
UnixWare)
	root_group=sys
	BSTRAP_ENV="INSTALL=/usr/ucb/install $BSTRAP_ENV"
	need_mkdir=yes
	need_awk=yes
	need_sed=yes
	whoamiprog=/usr/ucb/whoami
	CC="gcc -DUNIXWARE"; export CC
	;;
Serenity)
	root_group=wheel
	need_bsd_install=yes
	need_sed=yes
	need_awk=yes
	need_mksh=no
	need_ksh=no
	;;
*)
	echo "This platform ($opsys) is untried - good luck, and thanks for using pkgsrc"
	root_group=wheel
	need_bsd_install=yes
	need_awk=yes
	need_sed=yes
	;;
esac

# Fixup MACHINE_ARCH to use canonical pkgsrc variants, and support multiarch
# systems via --abi, setting a default $abi based on MACHINE_ARCH if not set.
#
case "$machine_arch/$abi" in
# "amd64" translates to "x86_64", defaults to 64-bit
amd64/32)	abi=32	machine_arch=i386	;;
amd64/*)	abi=64	machine_arch=x86_64	;;
# XXX: hppa untested
hppa/64)	abi=64	machine_arch=hppa64	;;
hppa/*)		abi=32	machine_arch=hppa	;;
hppa64/32)	abi=32	machine_arch=hppa	;;
hppa64/*)	abi=64	machine_arch=hppa64	;;
# "i386" can support 64-bit, e.g. SunOS, defaults to 32-bit.
i386/64)	abi=64	machine_arch=x86_64	;;
i386/*)		abi=32	machine_arch=i386	;;
# XXX: powerpc untested
powerpc/64)	abi=64	machine_arch=powerpc64	;;
powerpc/*)	abi=32	machine_arch=powerpc	;;
powerpc64/32)	abi=32	machine_arch=powerpc	;;
powerpc64/*)	abi=64	machine_arch=powerpc64	;;
powerpc64le/*)	abi=64	machine_arch=powerpc64le;;
# sparc/sparcv9 support 32-bit/64-bit, default to native.
sparcv9/32)	abi=32	machine_arch=sparc	;;
sparcv9/*)	abi=64	machine_arch=sparc64	;;
sparc/64)	abi=64	machine_arch=sparc64	;;
sparc/*)	abi=32	machine_arch=sparc	;;
# x86_64 supports 32-bit/64-bit, defaults to 64-bit.
x86_64/32)	abi=32	machine_arch=i386	;;
x86_64/*)	abi=64	machine_arch=x86_64	;;
esac

# If "--full" is specified, then install all of the platform-independent
# bootstrap software.
#
case "$full" in
yes)
	need_bsd_install=yes
	need_awk=yes
	need_sed=yes
	need_mksh=yes
	;;
esac

# mksh and ksh are mutually exclusive, prefer mksh while we transition to it.
case "$need_mksh" in
yes)	need_ksh=no ;;
esac

case "$quiet" in
yes)
	configure_quiet_flags="--quiet"
	make_quiet_flags="-s"
	;;
no)
	configure_quiet_flags=""
	make_quiet_flags=""
esac

test ${make_jobs} -gt 0 2>/dev/null
if [ $? -ne 0 ]; then
	die "ERROR: --make-jobs must be a positive integer argument"
fi

# export MACHINE_ARCH and OPSYS for bmake and pkg_install.
MACHINE_ARCH=${machine_arch}; export MACHINE_ARCH
OPSYS=${opsys}; export OPSYS

if [ "x$preserve_path" != "xyes" ]; then
	PATH="$overpath:$PATH"
fi

check_prog awkprog awk
check_prog chmodprog chmod
if [ -n "$CP" ]; then
	cpprog="$CP"
else
	check_prog cpprog cp
fi
if [ -n "$ID" ]; then
	idprog="$ID"
else
	check_prog idprog id
fi
check_prog groupsprog groups
check_prog lnprog ln
check_prog lsprog ls
check_prog rmdirprog rmdir
check_prog sedprog sed
check_prog shprog sh
check_prog whoamiprog whoami

if [ -d "${wrkdir}" ] || [ -f "${wrkdir}" ]; then
	die "\"${wrkdir}\" already exists, please remove it or use --workdir"
fi

if [ -f "${prefix}/share/mk/sys.mk" ]; then
	echo "host prefix found!"
	#die "\"${prefix}\" already in use, remove it or use a different --prefix"
fi

if [ -d "${pkgdbdir}/bootstrap-mk-files-"* ]; then
	echo "host pkgdbdir found!"
	#die "\"${pkgdbdir}\" already in use, remove it or use a different --pkgdbdir"
fi

mkdir_p_early ${wrkdir}
if touch ${wrkdir}/.writeable; then
	:
else
	die "\"${wrkdir}\" is not writeable. Try $0 -h"
fi
echo "Working directory is: ${wrkdir}"

if [ "$compiler" = "" ] && [ x"$check_compiler" = x"yes" ]; then
	get_compiler
	# Clang pretends to be GCC, so we have to check it first.
	if [ $compiler_is_clang -gt 0 ]; then
		compiler="clang"
	elif [ $compiler_is_gnu -gt 0 ]; then
		compiler="gcc"
	else
		case "$opsys" in
		IRIX)
			if [ `uname -r` -ge 6 ]; then
				compiler="mipspro"
			else
				compiler="ido"
			fi
			;;
		SunOS)	compiler="sunpro"
			;;
		esac
	fi
fi

case "$compiler" in
clang|gcc)
	test -n "$CC" || CC=$compiler
	;;
*)
	test -n "$CC" || CC=cc
	;;
esac

has_ssp_support() {
	mkdir_p_early ${wrkdir}/tmp
	echo 'int main(void){return 0;}' > ${wrkdir}/tmp/ssp.c
	${CC:-cc} -fstack-protector-strong -o ${wrkdir}/tmp/ssp ${wrkdir}/tmp/ssp.c >/dev/null 2>&1

	if [ $? -eq 0 ]; then
		echo yes
	else
		echo no
	fi
}

if [ "$has_ssp" = "" ] && [ x"$check_ssp" = x"yes" ]; then
	has_ssp=`has_ssp_support`
fi

mkdir_p_early ${wrkdir}/bin

# build install-sh
run_cmd "$sedprog -e 's|@DEFAULT_INSTALL_MODE@|'${default_install_mode-0755}'|' $pkgsrcdir/sysutils/install-sh/files/install-sh.in > $wrkdir/bin/install-sh"
run_cmd "$chmodprog +x $wrkdir/bin/install-sh"
install_sh="$shprog $wrkdir/bin/install-sh"

if [ $unprivileged = "yes" ]; then
	user=`$whoamiprog`
	group=`$groupsprog | $awkprog '{print $1}'`
	echo_msg "building as unprivileged user $user/$group"

	# force bmake install target to use $user and $group
	echo "BINOWN=$user
BINGRP=$group
LIBOWN=$user
LIBGRP=$group
MANOWN=$user
MANGRP=$group" > ${wrkdir}/Makefile.inc
elif is_root; then
	user=$root_user
	group=$root_group
else
	die "You must be either root to install bootstrap-pkgsrc or use the --unprivileged option."
fi

# export the proper environment
PATH=$prefix/bin:$prefix/sbin:${PATH}; export PATH
if [ -d /usr/ccs/bin -a -x /usr/ccs/bin/make ]; then
	PATH=${PATH}:/usr/ccs/bin; export PATH
fi
PKG_DBDIR=$pkgdbdir; export PKG_DBDIR
LOCALBASE=$prefix; export LOCALBASE
VARBASE=$varbase; export VARBASE
if [ x"$has_ssp" = x"no" ] && [ x"$check_ssp" = x"yes" ]; then
_OPSYS_SUPPORTS_SSP=no; export _OPSYS_SUPPORTS_SSP
fi

# set up an example mk.conf file
TARGET_MKCONF=${wrkdir}/mk.conf.example
echo_msg "Creating default mk.conf in ${wrkdir}"
echo "# Example ${sysconfdir}/mk.conf file produced by bootstrap-pkgsrc" > ${TARGET_MKCONF}
echo "# `date`" >> ${TARGET_MKCONF}
echo "" >> ${TARGET_MKCONF}
echo ".ifdef BSD_PKG_MK	# begin pkgsrc settings" >> ${TARGET_MKCONF}
echo "" >> ${TARGET_MKCONF}

# IRIX64 needs to be set to IRIX, for example
if [ "$set_opsys" = "yes" ]; then
	echo "OPSYS=			$opsys" >> ${TARGET_MKCONF}
fi

if [ -n "$abi" ]; then
	echo "ABI=			$abi" >> ${TARGET_MKCONF}
fi
if [ "$compiler" != "" ]; then
	echo "PKGSRC_COMPILER=	$compiler" >> ${TARGET_MKCONF}
fi
case "$compiler" in
sunpro)
	echo "CC=			cc"        >> ${TARGET_MKCONF}
	echo "CXX=			CC"        >> ${TARGET_MKCONF}
	echo "CPP=			\${CC} -E" >> ${TARGET_MKCONF}
	;;
clang)
	echo "CC=			clang"     >> ${TARGET_MKCONF}
	echo "CXX=			clang++"   >> ${TARGET_MKCONF}
	echo "CPP=			\${CC} -E" >> ${TARGET_MKCONF}
	if [ -z "$CLANGBASE" -a -f "/usr/bin/clang" ]; then
		CLANGBASE="/usr"
	fi
	if [ -n "$CLANGBASE" -o -f "/bin/clang" ]; then
		echo "CLANGBASE=		$CLANGBASE" >> ${TARGET_MKCONF}
	fi
	;;
esac
if [ -n "$GCCBASE" ]; then
	echo "GCCBASE=		$GCCBASE" >> ${TARGET_MKCONF}
fi
if [ -n "$SUNWSPROBASE" ]; then
	echo "SUNWSPROBASE=		$SUNWSPROBASE" >> ${TARGET_MKCONF}
fi
echo "" >> ${TARGET_MKCONF}

if [ x"$has_ssp" = x"no" ] && [ x"$check_ssp" = x"yes" ]; then
	echo "_OPSYS_SUPPORTS_SSP=	no" >> ${TARGET_MKCONF}
fi

# enable unprivileged builds if not root
if [ "$unprivileged" = "yes" ]; then
	echo "UNPRIVILEGED=		yes" >> ${TARGET_MKCONF}
fi

# save environment in example mk.conf
echo "PKG_DBDIR=		$pkgdbdir" >> ${TARGET_MKCONF}
echo "LOCALBASE=		$prefix" >> ${TARGET_MKCONF}
if [ "${sysconfbase}" != "/etc" ]; then
echo "SYSCONFBASE=		$sysconfbase" >> ${TARGET_MKCONF}
fi
echo "VARBASE=		$varbase" >> ${TARGET_MKCONF}
if [ "${sysconfdir}" != "${prefix}/etc" ]; then
	echo "PKG_SYSCONFBASE=	$sysconfdir" >> ${TARGET_MKCONF}
fi
echo "PKG_TOOLS_BIN=		$prefix/sbin" >> ${TARGET_MKCONF}
echo "PKGINFODIR=		$pkginfodir" >> ${TARGET_MKCONF}
echo "PKGMANDIR=		$pkgmandir" >> ${TARGET_MKCONF}
echo "" >> ${TARGET_MKCONF}

case $opsys in
Linux)
	# Default to PREFER_PKGSRC=yes unless user specifies --prefer-native=yes
	# Linux systems likely have software from other packages managers
	# like yum or apt that can leak into pkgsrc and cause issues as they
	# age, undergo ABI changes, or get added/removed behind our backs.
	# Let pkgsrc maintain all dependencies to avoid these problems.
	# The exception here is Elbrus 2000, for which a lot of patches have
	# not been upstreamed, so that it is better to use native.
	if [ -z "$prefer_pkgsrc" ] && [ "$prefer_native" != "yes" ] && \
	    [ "$machine_arch" != "e2k" ]; then
		prefer_pkgsrc="yes"
	fi
	;;
esac

if [ -n "$prefer_pkgsrc" ]; then
	echo "# WARNING: Changing PREFER_* after bootstrap will require rebuilding all" >> ${TARGET_MKCONF}
	echo "# packages with a dependency that switched between native/pkgsrc." >> ${TARGET_MKCONF}
	echo "PREFER_PKGSRC=		$prefer_pkgsrc" >> ${TARGET_MKCONF}
	echo "" >> ${TARGET_MKCONF}
fi
if [ -n "$prefer_native" ]; then
	echo "PREFER_NATIVE=		$prefer_native" >> ${TARGET_MKCONF}
	echo "" >> ${TARGET_MKCONF}
fi

BOOTSTRAP_MKCONF=${wrkdir}/mk.conf
cp ${TARGET_MKCONF} ${BOOTSTRAP_MKCONF}

case "$cwrappers" in
yes|no)
	echo "USE_CWRAPPERS=		$cwrappers" >> ${TARGET_MKCONF}
	echo "USE_CWRAPPERS=		$cwrappers" >> ${BOOTSTRAP_MKCONF}
	echo "" >> ${TARGET_MKCONF}
	;;
esac

# On all Debian GNU/kFreeBSD 7, /bin/sh is a symlink to /bin/dash, and
# use /bin/bash.
# Only needed by legacy wrappers.
if [ "$opsys" = "GNUkFreeBSD" -a "$bootstrap_sh_set" != "set" ]; then
	echo "TOOLS_PLATFORM.sh?=		/bin/bash	# instead of /bin/sh" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.sh?=		/bin/bash	# instead of /bin/sh" >> ${BOOTSTRAP_MKCONF}
	echo $opsys
fi

# sbin is used by pkg_install, share/mk by bootstrap-mk-files
mkdir_p $wrkdir/sbin $wrkdir/share/mk
mkdir_p_early ${wrkdir}

if [ "$need_bsd_install" = "yes" ]; then
	BSTRAP_ENV="INSTALL='$prefix/bin/install-sh -c' $BSTRAP_ENV"
	if [ "$use_bsdinstall" = "yes" ]; then
		echo "TOOLS_PLATFORM.install?=	$prefix/bin/bsdinstall" >> ${TARGET_MKCONF}
	else
		echo "TOOLS_PLATFORM.install?=	$prefix/bin/install-sh" >> ${TARGET_MKCONF}
	fi
	echo "TOOLS_PLATFORM.install?=	$wrkdir/bin/install-sh" >> ${BOOTSTRAP_MKCONF}
fi

if [ "$need_fixed_strip" = "yes" ] ; then
	echo_msg "Installing fixed strip script"
	run_cmd "$install_sh -c -o $user -g $group -m 755 $pkgsrcdir/pkgtools/bootstrap-extras/files/strip-sh $wrkdir/bin/strip"
	echo "TOOLS_PLATFORM.strip?=		$prefix/bin/strip" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.strip?=		$wrkdir/bin/strip" >> ${BOOTSTRAP_MKCONF}
	need_extras=yes
fi

if [ "$need_mkdir" = "yes" -a -z "$MKDIR" ]; then
	echo_msg "Installing fixed mkdir script \"mkdir-sh\""
	run_cmd "$install_sh -c -o $user -g $group -m 755 $pkgsrcdir/pkgtools/bootstrap-extras/files/mkdir-sh $wrkdir/bin/mkdir-sh"
	echo "TOOLS_PLATFORM.mkdir?=		$prefix/bin/mkdir-sh -p" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.mkdir?=		$wrkdir/bin/mkdir-sh -p" >> ${BOOTSTRAP_MKCONF}
	need_extras=yes
fi

if [ "$need_xargs" = "yes" ]; then
	echo_msg "Installing fixed xargs script"
	run_cmd "$install_sh -c -o $user -g $group -m 755 $pkgsrcdir/pkgtools/bootstrap-extras/files/xargs-sh $wrkdir/bin/xargs"
	echo "TOOLS_PLATFORM.xargs?=		$prefix/bin/xargs" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.xargs?=		$wrkdir/bin/xargs" >> ${BOOTSTRAP_MKCONF}
	need_extras=yes
fi

echo_msg "Bootstrapping mk-files"
run_cmd "(cd ${pkgsrcdir}/pkgtools/bootstrap-mk-files/files && env CP=${cpprog} \
 OPSYS=${opsys} MK_DST=${wrkdir}/share/mk ROOT_GROUP=${root_group} \
ROOT_USER=${root_user} SED=${sedprog} SYSCONFDIR=${sysconfdir} \
$shprog ./bootstrap.sh)"

if [ -n "${bootstrap_sh_set}" ]; then
	bmakexargs="${bmakexargs} --with-defshell=${bootstrap_sh}"
fi

bootstrap_bmake() {
	echo_msg "Bootstrapping bmake"
	copy_src $pkgsrcdir/devel/bmake/files bmake
	run_cmd "cp $pkgsrcdir/../files/config.sub $wrkdir/bmake/"
	run_cmd "(cd $wrkdir/bmake && $shprog configure $configure_quiet_flags --prefix=$wrkdir --with-default-sys-path=$wrkdir/share/mk --with-machine-arch=${machine_arch} $bmakexargs $configure_cross_args CFLAGS=-DNO_REGEX)"
	#run_cmd "(cd $wrkdir/bmake && echo \"#define NO_REGEX\" >> config.h)"
	run_cmd "(cd $wrkdir/bmake && $shprog make-bootstrap.sh)"
	run_cmd "$install_sh -c -o $user -g $group -m 755 $wrkdir/bmake/bmake $wrkdir/bin/bmake"
}
#bootstrap_bmake #already provided by the bmake port

#bmake="$wrkdir/bin/bmake"
bmake=$(which bmake) # use host system bmake

# build libnbcompat
echo_msg "Building libnbcompat"
echo_msg $bmake
copy_src $pkgsrcdir/pkgtools/libnbcompat/files libnbcompat
run_cmd "cp $pkgsrcdir/../files/config.sub $wrkdir/libnbcompat/" # overwrite with serenity config.sub
run_cmd "(cd $wrkdir/libnbcompat; $shprog ./configure $configure_quiet_flags -C --prefix=$prefix --infodir=$infodir --mandir=$mandir --sysconfdir=$sysconfdir --enable-bsd-getopt --enable-db $configure_cross_args && $bmake $make_quiet_flags -j$make_jobs)"

# bootstrap mksh if necessary
case "$need_mksh" in
yes)	echo_msg "Bootstrapping mksh"
	copy_src $pkgsrcdir/shells/mksh/files mksh
	run_cmd "(cd $wrkdir/mksh && env $BSTRAP_ENV CC=\"${CC}\" $shprog Build.sh -r)"
	run_cmd "$install_sh -c -o $user -g $group -m 755 $wrkdir/mksh/mksh $wrkdir/bin/mksh"
	echo "TOOLS_PLATFORM.sh?=               $prefix/bin/mksh" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.sh?=               ${SHELL}" >> ${BOOTSTRAP_MKCONF}
	echo_msg "Rebootstrapping bmake for mksh"
	bmakexargs="$bmakexargs --with-defshell=$wrkdir/bin/mksh"
	bootstrap_bmake
	;;
esac

# bootstrap ksh if necessary
case "$need_ksh" in
yes)	echo_msg "Bootstrapping ksh"
	copy_src $pkgsrcdir/shells/pdksh/files ksh
	run_cmd "(cd $wrkdir/ksh && env $BSTRAP_ENV $shprog ./configure $configure_quiet_flags --prefix=$prefix --infodir=$infodir --mandir=$mandir --sysconfdir=$sysconfdir $configure_cross_args && $bmake $make_quiet_flags -j$make_jobs)"
	run_cmd "$install_sh -c -o $user -g $group -m 755 $wrkdir/ksh/ksh $wrkdir/bin/pdksh"
	echo "TOOLS_PLATFORM.sh?=		$prefix/bin/pdksh" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.sh?=		$wrkdir/bin/pdksh" >> ${BOOTSTRAP_MKCONF}
	echo "TOOLS_PLATFORM.ksh?=		$prefix/bin/pdksh" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.ksh?=		$wrkdir/bin/pdksh" >> ${BOOTSTRAP_MKCONF}
# Now rebootstrap bmake for ksh
	echo_msg "Rebootstrapping bmake for ksh"
	bmakexargs="$bmakexargs --with-defshell=$wrkdir/bin/pdksh"
	bootstrap_bmake
	;;
esac

# bootstrap awk if necessary
case "$need_awk" in
yes)	echo_msg "Bootstrapping awk"
	copy_src $pkgsrcdir/lang/nawk/files awk
	run_cmd "(cd $wrkdir/awk && $bmake $make_quiet_flags -j$make_jobs -f Makefile CC=\"${CC}\" CFLAGS=\"${CFLAGS}\" CPPFLAGS=\"-DFOPEN_MAX=4096\")"
	run_cmd "$install_sh -c -o $user -g $group -m 755 $wrkdir/awk/a.out $wrkdir/bin/nawk"
	echo "TOOLS_PLATFORM.awk?=		$prefix/bin/nawk" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.awk?=		nawk" >> ${BOOTSTRAP_MKCONF}
	;;
esac

# bootstrap sed if necessary
case "$need_sed" in
yes)	echo_msg "Bootstrapping sed"
	copy_src $pkgsrcdir/textproc/nbsed/files sed
	run_cmd "cp $pkgsrcdir/../files/config.sub $wrkdir/sed/"
	run_cmd "(cd $wrkdir/sed; env $BSTRAP_ENV CPPFLAGS='$CPPFLAGS -I../libnbcompat' LDFLAGS='$LDFLAGS -L../libnbcompat' LIBS='$LIBS -lnbcompat' $shprog ./configure $configure_quiet_flags -C --prefix=$prefix --infodir=$infodir --mandir=$mandir --sysconfdir=$sysconfdir --program-transform-name='s,sed,nbsed,' $configure_cross_args && $bmake $make_quiet_flags -j$make_jobs)"
	run_cmd "$install_sh -c -o $user -g $group -m 755 $wrkdir/sed/sed $wrkdir/bin/sed"
	echo "TOOLS_PLATFORM.sed?=		$prefix/bin/nbsed" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.sed?=		$wrkdir/bin/sed" >> ${BOOTSTRAP_MKCONF}
	;;
esac

# bootstrap pkg_install
extra_libarchive_depends() {
	$sedprog -n -e 's/Libs.private: //p' $wrkdir/libarchive/build/pkgconfig/libarchive.pc
}

echo_msg "Bootstrapping pkgtools"
copy_src $pkgsrcdir/archivers/libarchive/files libarchive
run_cmd "cp $pkgsrcdir/../files/config.sub $wrkdir/libarchive/build/autoconf/"
run_cmd "(cd $wrkdir/libarchive; env $BSTRAP_ENV \
$shprog ./configure $configure_quiet_flags --enable-static --disable-shared \
--disable-bsdtar --disable-bsdcpio --disable-posix-regex-lib --disable-xattr \
--disable-maintainer-mode --disable-acl --without-zlib --without-bz2lib \
--without-iconv --without-lzma --without-lzo2 --without-lz4 \
--without-nettle --without-openssl --without-xml2 --without-expat --without-zstd $configure_cross_args \
MAKE=$bmake && $bmake $make_quiet_flags -j$make_jobs)"
copy_src $pkgsrcdir/pkgtools/pkg_install/files pkg_install
run_cmd "cp $pkgsrcdir/../files/config.sub $wrkdir/pkg_install"
run_cmd "(cd $wrkdir/pkg_install; env $BSTRAP_ENV \
CPPFLAGS='$CPPFLAGS -I${wrkdir}/libnbcompat -I${wrkdir}/libarchive/libarchive' \
LDFLAGS='$LDFLAGS -L${wrkdir}/libnbcompat' \
LIBS='$LIBS -lnbcompat' $shprog ./configure $configure_quiet_flags -C \
--enable-bootstrap --prefix=$prefix --sysconfdir=$sysconfdir \
--with-pkgdbdir=$pkgdbdir --infodir=$infodir \
--mandir=$mandir $pkg_install_args $configure_cross_args && \
STATIC_LIBARCHIVE=$wrkdir/libarchive/.libs/libarchive.a \
STATIC_LIBARCHIVE_LDADD=`extra_libarchive_depends` \
PKGSRC_MACHINE_ARCH="$machine_arch" $bmake $make_quiet_flags -j$make_jobs)"
run_cmd "$install_sh -c -o $user -g $group -m 755 $wrkdir/pkg_install/add/pkg_add $wrkdir/sbin/pkg_add"
run_cmd "$install_sh -c -o $user -g $group -m 755 $wrkdir/pkg_install/admin/pkg_admin $wrkdir/sbin/pkg_admin"
run_cmd "$install_sh -c -o $user -g $group -m 755 $wrkdir/pkg_install/create/pkg_create $wrkdir/sbin/pkg_create"
run_cmd "$install_sh -c -o $user -g $group -m 755 $wrkdir/pkg_install/info/pkg_info $wrkdir/sbin/pkg_info"
echo "PKG_ADD_CMD?=			pkg_add" >> ${BOOTSTRAP_MKCONF}
echo "PKG_ADMIN_CMD?=			pkg_admin" >> ${BOOTSTRAP_MKCONF}
echo "PKG_CREATE_CMD?=			pkg_create" >> ${BOOTSTRAP_MKCONF}
echo "PKG_INFO_CMD?=			pkg_info" >> ${BOOTSTRAP_MKCONF}

MAKECONF=$wrkdir/mk.conf
export MAKECONF

if [ "$bootstrap_sh_set" = "set" ]; then
	echo "TOOLS_PLATFORM.sh?=		${bootstrap_sh}" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.sh?=		${bootstrap_sh}" >> ${BOOTSTRAP_MKCONF}
fi

# preserve compiler and tool environment variables settings
if test -n "$CP"; then
	echo "TOOLS_PLATFORM.cp?=		$CP" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.cp?=		$CP" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$GREP"; then
	echo "TOOLS_PLATFORM.grep?=		$GREP" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.grep?=		$GREP" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$EGREP"; then
	echo "TOOLS_PLATFORM.egrep?=		$EGREP" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.egrep?=		$EGREP" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$FGREP"; then
	echo "TOOLS_PLATFORM.fgrep?=		$FGREP" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.fgrep?=		$FGREP" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$ID"; then
	echo "TOOLS_PLATFORM.id?=		$ID" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.id?=		$ID" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$MKDIR"; then
	echo "TOOLS_PLATFORM.mkdir?=		$MKDIR" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.mkdir?=		$MKDIR" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$TEST"; then
	echo "TOOLS_PLATFORM.test?=		$TEST" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.test?=		$TEST" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$TOUCH"; then
	echo "TOOLS_PLATFORM.touch?=		$TOUCH" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.touch?=		$TOUCH" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$XARGS"; then
	echo "TOOLS_PLATFORM.xargs?=		$XARGS" >> ${TARGET_MKCONF}
	echo "TOOLS_PLATFORM.xargs?=		$XARGS" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$CFLAGS"; then
	echo "CFLAGS+=		$CFLAGS" >> ${TARGET_MKCONF}
	echo "DBG=			# prevent DBG from adding default optimizer flags" >> ${TARGET_MKCONF}
	echo "DBG=			# prevent DBG from adding default optimizer flags" >> ${BOOTSTRAP_MKCONF}
fi
if test -n "$CPPFLAGS"; then
	echo "CPPFLAGS+=		$CPPFLAGS" >> ${TARGET_MKCONF}
fi
if test -n "$LDFLAGS"; then
	echo "LDFLAGS+=		$LDFLAGS" >> ${TARGET_MKCONF}
fi
if test -n "$LIBS"; then
	echo "LIBS+=		$LIBS" >> ${TARGET_MKCONF}
fi

# opsys specific fiddling
opsys_finish

echo "WRKOBJDIR=		${wrkdir}/wrk" >> ${BOOTSTRAP_MKCONF}

echo "" >> ${TARGET_MKCONF}
echo "" >> ${BOOTSTRAP_MKCONF}
if test -n "${mk_fragment}"; then
	cat "${mk_fragment}" >> ${TARGET_MKCONF}
	echo "" >> ${TARGET_MKCONF}
fi
echo ".endif			# end pkgsrc settings" >> ${TARGET_MKCONF}
echo ".endif			# end pkgsrc settings" >> ${BOOTSTRAP_MKCONF}

# build and register packages
# usage: build_package <packagedirectory>
build_package() {
	run_cmd "(cd $pkgsrcdir/$1 && $bmake $make_quiet_flags MAKE_JOBS=${make_jobs} PKG_COMPRESSION=none -DPKG_PRESERVE PKGSRC_KEEP_BIN_PKGS=no MAKECONF=${BOOTSTRAP_MKCONF} MKDIR=\"mkdir -p\" SED=sed install)"
}
build_package_nopreserve() {
	run_cmd "(cd $pkgsrcdir/$1 && $bmake $make_quiet_flags MAKE_JOBS=${make_jobs} PKG_COMPRESSION=none PKGSRC_KEEP_BIN_PKGS=no MAKECONF=${BOOTSTRAP_MKCONF} install)"
}

#
# cwrappers is special, we don't want to set it as a BOOTSTRAP_PKG but must
# build it (if required) without -DPKG_PRESERVE set so that it can be deleted.
#
export PWD_CMD=pwd
export ID=id
export ECHO=echo
export SED=sed
echo "TOOLS_IGNORE.sh=		yes" >> ${BOOTSTRAP_MKCONF}
echo "PKG_DBDIR=	${DESTDIR}$pkgdbdir" >> ${BOOTSTRAP_MKCONF}
echo "LOCALBASE=	${DESTDIR}$localbase" >> ${BOOTSTRAP_MKCONF}
echo "LOCALBASE=	${DESTDIR}$prefix" >> ${BOOTSTRAP_MKCONF}
echo "SYSCONFBASE=	${DESTDIR}$prefix/etc" >> ${BOOTSTRAP_MKCONF}
echo "VARBASE=		${DESTDIR}$prefix/var" >> ${BOOTSTRAP_MKCONF}
echo "PKG_TOOLS_BIN=" >> ${BOOTSTRAP_MKCONF}

exit 0
use_cwrappers=`(cd $pkgsrcdir/devel/bmake && $bmake show-var VARNAME=_USE_CWRAPPERS)`
case "$use_cwrappers" in
yes)
	build_package_nopreserve "pkgtools/cwrappers"
	;;
esac
#
# Please make sure that the following packages and
# only the following packages set BOOTSTRAP_PKG=yes.
#
echo_msg "Installing packages"
build_package "pkgtools/bootstrap-mk-files"
case "$need_bsd_install" in
yes)
	if [ "$use_bsdinstall" = "yes" ]; then
		build_package "sysutils/bsdinstall"
	else
		build_package "sysutils/install-sh"
	fi
	;;
esac
case "$need_mksh" in
yes)	build_package "shells/mksh";;
esac
case "$need_ksh" in
yes)	build_package "shells/pdksh";;
esac
build_package "devel/bmake"
case "$need_awk" in
yes)	build_package "lang/nawk";;
esac
case "$need_sed" in
yes)	build_package "textproc/nbsed";;
esac
case "$need_extras" in
yes)	build_package "pkgtools/bootstrap-extras";;
esac
case "$need_pax" in
yes)    build_package "archivers/pax"
esac
build_package "pkgtools/pkg_install"

etc_mk_conf="$sysconfdir/mk.conf"

# Install the example mk.conf so that it is used, but only if it doesn't
# exist yet. This can happen with non-default sysconfdir settings.
mkdir_p "$sysconfdir"
if [ ! -f "$etc_mk_conf" ]; then
	cp "$TARGET_MKCONF" "$etc_mk_conf"
	TARGET_MKCONF="$etc_mk_conf"
fi

[ -n "${binary_kit}" ] && mkbinarykit_tar
[ -n "${binary_gzip_kit}" ] && mkbinarykit_tgz

hline="==========================================================================="
echo ""
echo "$hline"
echo ""
echo "Please remember to add $prefix/bin to your PATH environment variable"
echo "and $mandir to your MANPATH environment variable, if necessary."
echo ""
echo "An example mk.conf file with the settings you provided to \"bootstrap\""
echo "has been created for you. It can be found in:"
echo ""
echo "      ${TARGET_MKCONF}"
echo ""
if [ "$TARGET_MKCONF" != "$etc_mk_conf" ]; then
	echo "Please copy it to $etc_mk_conf to use it."
	echo ""
fi
echo "You can find extensive documentation of the NetBSD Packages Collection"
echo "in $pkgsrcdir/doc/pkgsrc.txt."
echo ""
echo "Thank you for using pkgsrc!"
echo ""
echo "$hline"
echo ""

echo_msg "bootstrap started: $build_start"
echo_msg "bootstrap ended:   `date`"

exit 0
