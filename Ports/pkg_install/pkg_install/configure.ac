dnl $NetBSD: configure.ac,v 1.44 2020/12/02 10:22:39 wiz Exp $
dnl Process this file with autoconf to produce a configure script.
AC_PREREQ(2.52)
AC_INIT([pkg_install], [20200701], [joerg@NetBSD.org])
AC_CONFIG_SRCDIR([lib/plist.c])
AC_CONFIG_HEADER(lib/config.h)

AC_CANONICAL_HOST
CANONICAL_HOST=$host
AC_SUBST(CANONICAL_HOST)
AC_SUBST(INCLUDES)

# Checks for programs.
AC_PROG_MAKE_SET
AC_PROG_CC
AC_PROG_INSTALL
AC_PROG_LN_S
AC_PROG_RANLIB
AC_CHECK_PROG(AR, ar, ar)

AC_PATH_PROG(CHMOD, chmod)
AC_PATH_PROG(CMP, cmp)
AC_PATH_PROG(CP, cp)
AC_PATH_PROG(ENV, env)
AC_PATH_PROG(FIND, find)
AC_PATH_PROG(GREP, grep)
AC_PATH_PROG(LN, ln)
AC_PATH_PROG(MKDIR, mkdir)
AC_PATH_PROG(RMDIR, rmdir)
AC_PATH_PROG(RM, rm)
AC_PATH_PROG(SED, sed)
AC_PATH_PROG(SORT, sort)
AC_PATH_PROG(TOUCH, touch)

AUTOCONF=${AUTOCONF-"$srcdir/missing --run autoconf"}
AC_SUBST(AUTOCONF)
AUTOHEADER=${AUTOHEADER-"$srcdir/missing --run autoheader"}
AC_SUBST(AUTOHEADER)

dnl Set the default pkg dbdir
AC_ARG_WITH(pkgdbdir,
[  --with-pkgdbdir=DIR     Where to put the pkg database (PREFIX/pkgdb)],
[ pkgdbdir="$with_pkgdbdir" ],
[ pkgdbdir="${prefix}/pkgdb" ])
AC_SUBST(pkgdbdir)

AC_ARG_WITH(ssl,
[ --with-ssl		Enable OpenSSL based signature support],
[ ssl_support=yes ],
[ ssl_support= ])
AC_SUBST(ssl_support)

AC_ARG_ENABLE([bootstrap],
    [AS_HELP_STRING([--enable-bootstrap], [build minimal version of pkg_install])],
    [bootstrap=yes], [bootstrap=])

AC_SUBST(bootstrap)

dnl Checks for large file support.
AC_SYS_LARGEFILE

dnl Checks for libraries.
AC_CHECK_LIB(db, __db185_open, , AC_SEARCH_LIBS(dbopen, [db db1]))

dnl Checks for header files.
AC_HEADER_STDC
AC_CHECK_HEADERS([assert.h ctype.h dirent.h err.h errno.h fcntl.h \
	fnmatch.h glob.h grp.h inttypes.h limits.h pwd.h signal.h \
	stdarg.h stdio.h stdlib.h string.h time.h unistd.h vis.h])
AC_CHECK_HEADERS([sys/cdefs.h sys/file.h sys/ioctl.h sys/queue.h \
	sys/stat.h sys/time.h sys/types.h sys/utsname.h sys/wait.h])

# Checks for library functions.
AC_CHECK_FUNCS([vfork])

AC_SEARCH_LIBS([gethostbyname], [nsl network])
AC_SEARCH_LIBS([socket],
               [socket],
               [],
               [AC_CHECK_LIB([socket],
                             [socket],
                             [LIBS="-lsocket -lnsl $LIBS"],
                             [],
                             [-lnsl])])

dnl Check for types
AC_TYPE_UINT8_T
AC_TYPE_UINT16_T
AC_TYPE_UINT32_T
AC_TYPE_UINT64_T

need_priu64=no
AC_MSG_CHECKING([for a working PRIu64])
AC_LANG_PUSH([C])
AC_RUN_IFELSE(
	[AC_LANG_PROGRAM([[
		#include <stdio.h>
		#ifdef HAVE_INTTYPES_H
		#include <inttypes.h>
		#endif
		]],
		[[
		char *x;
		#if !defined(PRIu64)
			return -1;
		#else
		x = PRIu64;
		if( x[0] == '%' ) {
			return -1;
		} else {
			return 0;
		}
		#endif
		]])],
	[
		# program worked correctly
		AC_MSG_RESULT([yes])
	],
	[
		# program failed
		AC_MSG_RESULT([no])
		AC_DEFINE([NEED_PRI_MACRO], [1], [Defined when PRIu64 is missing or broken])
		need_priu64=yes
	],
	[
		# we are cross compiling
		AC_MSG_RESULT([unable to check when crosscompiling])
		AC_DEFINE([NEED_PRI_MACRO], [1], [Defined when PRIu64 is missing or broken])
		need_priu64=yes
	])
AC_LANG_POP([C])
AC_CHECK_SIZEOF(int)
AC_CHECK_SIZEOF(long)
AC_CHECK_SIZEOF(long long)
AC_CHECK_SIZEOF(size_t, [#include <stdlib.h>])

case $host in
*-*-bsdi*|*-*-hpux*|*-*-osf*)
	AC_DEFINE(MISSING_SIZE_T_SUPPORT)
	AH_TEMPLATE([MISSING_SIZE_T_SUPPORT], [
	Define to 1 if the `z' modifider for printf is missing.
])
	;;
esac

case $host in
*-*-cygwin*|*-*-dragonfly* |*-*-freebsd*|*-*-linux*)
	AC_DEFINE([NUMERIC_VERSION_ONLY], [1], [Defined when to retain only the numeric OS version])
	;;
esac

AH_BOTTOM(
#if !HAVE_VFORK
# define vfork fork
#endif

#ifndef MISSING_SIZE_T_SUPPORT
#  define PRIzu "zu"
#elif SIZEOF_SIZE_T == SIZEOF_INT
#  define PRIzu "u"
#elif SIZEOF_SIZE_T == SIZEOF_LONG
#  define PRIzu "lu"
#elif SIZEOF_SIZE_T == SIZEOF_LONG_LONG
#  define PRIzu "llu"
#else
#  errror "Unknown size_t size"
#endif
)

SYSCONFDIR="$sysconfdir"
AC_SUBST(SYSCONFDIR)
PKG_DBDIR="$pkgdbdir"
AC_SUBST(PKG_DBDIR)

AC_CONFIG_FILES([Makefile add/Makefile admin/Makefile \
	create/Makefile delete/Makefile info/Makefile \
	lib/Makefile lib/pkg_install.conf.5 lib/pkg_install.conf.cat])
AC_OUTPUT
