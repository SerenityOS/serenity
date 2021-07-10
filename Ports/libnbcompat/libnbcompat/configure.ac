dnl $NetBSD: configure.ac,v 1.88 2020/05/24 21:51:54 nia Exp $

dnl Process this file with autoconf to produce a configure script.
AC_PREREQ(2.52)
AC_INIT([libnbcompat], [20190708], [joerg@NetBSD.org])
AC_CONFIG_HEADER(nbcompat/config.h)
AC_ARG_PROGRAM

AC_CANONICAL_HOST
AC_GNU_SOURCE
CANONICAL_HOST=$host
AC_SUBST(CANONICAL_HOST)
AC_SUBST(INCLUDES)

# Checks for programs.
AC_PROG_MAKE_SET
AC_PROG_CC_C99
AC_PROG_INSTALL
AC_PROG_LN_S
AC_PROG_RANLIB
AC_PROG_AWK
AC_CHECK_PROG(AR, ar, ar)

AUTOCONF=${AUTOCONF-"$srcdir/missing --run autoconf"}
AC_SUBST(AUTOCONF)
AUTOHEADER=${AUTOHEADER-"$srcdir/missing --run autoheader"}
AC_SUBST(AUTOHEADER)

dnl Checks for libraries
AC_CHECK_LIB(util, fparseln)

dnl Checks for header files.
AC_HEADER_STDC
AC_HEADER_TIME
AC_HEADER_STDBOOL

AC_CHECK_HEADERS([alloca.h assert.h bsd/libutil.h ctype.h dirent.h endian.h \
	err.h errno.h fcntl.h fnmatch.h fts.h grp.h inttypes.h libutil.h \
	limits.h machine/endian.h ndir.h netdb.h paths.h pwd.h signal.h \
	stdarg.h stddef.h stdint.h stdio.h \
	stdlib.h string.h sys/byteorder.h sys/cdefs.h sys/dir.h sys/endian.h \
	sys/file.h sys/mkdev.h sys/ndir.h sys/param.h sys/socket.h sys/stat.h \
	sys/statfs.h sys/statvfs.h sys/time.h sys/ttycom.h sys/types.h \
	sys/uio.h sys/vfs.h sysexits.h termcap.h time.h tzfile.h unistd.h \
	util.h utime.h sys/mman.h
])
AC_CHECK_HEADERS([sys/mount.h], [:], [:], [
	AC_INCLUDES_DEFAULT
	#ifdef HAVE_SYS_PARAM_H
	#include <sys/param.h>
	#endif
])

dnl Always use our own glob().
AC_CHECK_HEADERS([glob.h])
AC_DEFINE(HAVE_NBCOMPAT_GLOB)
AH_TEMPLATE([HAVE_NBCOMPAT_GLOB], [
	Define to 1 if the `glob' function is built into the library.
])
AC_LIBOBJ(glob)

dnl Always use our own queue(3) implementation.
AC_CHECK_HEADERS([sys/queue.h])
AC_DEFINE(HAVE_NBCOMPAT_QUEUE)
AH_TEMPLATE([HAVE_NBCOMPAT_QUEUE], [
	Define to 1 if the `queue' macros are supplied by the library.
])

AC_CHECK_HEADERS([regex.h])
AC_CHECK_FUNC(regexec, [:], [
	AC_DEFINE(HAVE_NBCOMPAT_REGEX)
	AH_TEMPLATE([HAVE_NBCOMPAT_REGEX], [
		Define to 1 if the `regex' functions are built into the
		library.
        ])
	AC_LIBOBJ(regcomp)
	AC_LIBOBJ(regerror)
	AC_LIBOBJ(regexec)
	AC_LIBOBJ(regfree)
])

AC_MSG_TRY_LINK([for va_copy], pkg_cv_have_va_copy, [
#include <stdarg.h>
], [
	va_list ap, ap2;
	va_copy(ap2, ap);
	return 0;
],	AC_DEFINE(HAVE_VA_COPY)
	AH_TEMPLATE([HAVE_VA_COPY], [
		Define to 1 if the `va_copy' function is supported.
	]), [:]
])

AC_MSG_TRY_LINK([for __builtin_va_copy], pkg_cv_have___builtin_va_copy, [
#include <stdarg.h>
], [
	va_list ap, ap2;
	__builtin_va_copy(ap2, ap);
	return 0;
],	AC_DEFINE(HAVE___BUILTIN_VA_COPY)
	AH_TEMPLATE([HAVE___BUILTIN_VA_COPY], [
		Define to 1 if the `__builtin_va_copy' function is supported.
	]), [:]
])

AC_CHECK_HEADERS([md5.h], [
	AC_MSG_TRY_COMPILE([for MD5Init in md5.h], pkg_cv_have_md5init, [
		#include <sys/types.h>
		#include <md5.h>
	], [
		MD5_CTX *f;
		MD5Init(f);
	], [:], [
        	AC_DEFINE(HAVE_NBCOMPAT_MD5INIT)
		AH_TEMPLATE([HAVE_NBCOMPAT_MD5INIT], [
			Define to 1 if the `MD5Init' function is built into
			the library.
        	])
	])
])

AC_CHECK_HEADERS([md5.h rmd160.h sha1.h sha2.h])
AC_CHECK_FUNC(MD5File, [:], [
       	AC_DEFINE(HAVE_NBCOMPAT_MD5)
	AH_TEMPLATE([HAVE_NBCOMPAT_MD5], [
		Define to 1 if the `MD5*' functions are built into the
		library.
        ])
	AC_LIBOBJ(md5c)
	AC_LIBOBJ(md5hl)
])
AC_CHECK_FUNC(RMD160File, [:], [
       	AC_DEFINE(HAVE_NBCOMPAT_RMD160)
	AH_TEMPLATE([HAVE_NBCOMPAT_RMD160], [
		Define to 1 if the `RMD160*' functions are built into the
		library.
        ])
	AC_LIBOBJ(rmd160)
	AC_LIBOBJ(rmd160hl)
])
AC_CHECK_FUNC(SHA1File, [:], [
       	AC_DEFINE(HAVE_NBCOMPAT_SHA1)
	AH_TEMPLATE([HAVE_NBCOMPAT_SHA1], [
		Define to 1 if the `SHA1*' functions are built into the
		library.
        ])
	AC_LIBOBJ(sha1)
	AC_LIBOBJ(sha1hl)
])

AC_CHECK_FUNC(SHA512_File, [:], [
       	AC_DEFINE(HAVE_NBCOMPAT_SHA2)
	AH_TEMPLATE([HAVE_NBCOMPAT_SHA2], [
		Define to 1 if the `SHA2*' functions are built into the
		library.
        ])
	AC_LIBOBJ(sha2)
	AC_LIBOBJ(sha2hl)
])

case $host in
*-*-hpux*)
	AC_DEFINE(BROKEN_PREAD)
	AH_TEMPLATE([BROKEN_PREAD], [
	Define to 1 if the `pread' function is broken.
])
	AC_DEFINE(BROKEN_PWRITE)
	AH_TEMPLATE([BROKEN_PWRITE], [
	Define to 1 if the `pwrite' function is broken.
])
	;;
*-*-sco3.2v5*)
	AC_DEFINE(BROKEN_PREAD)
	AH_TEMPLATE([BROKEN_PREAD], [
	Define to 1 if the `pread' function is broken.
])
	AC_DEFINE(BROKEN_PWRITE)
	AH_TEMPLATE([BROKEN_PWRITE], [
	Define to 1 if the `pwrite' function is broken.
])
	;;
esac

AC_ARG_ENABLE([db],
	[AS_HELP_STRING([--enable-db], [include DB implementation])])
AC_SUBST(WITH_DB, $enable_db)
AC_CHECK_FUNCS(issetugid)

AC_ARG_ENABLE([bsd-getopt],
	[AS_HELP_STRING([--enable-bsd-getopt],
			[force use of BSDish getopt implementation])])

AC_CHECK_DECLS([optarg, optind, optreset], [:], [:], [
	#include <stdio.h>
	#include <stdlib.h>
	#include <unistd.h>
])

our_getopt_long=no
AC_CHECK_HEADERS([getopt.h],
	[ AC_CHECK_TYPES([struct option], [:],
	    [ our_getopt_long=yes ], [#include <getopt.h>])],
	[ our_getopt_long=yes ]
)

if test "$enable_bsd_getopt" = yes; then
	if test "$ac_cv_have_decl_optreset" = no; then
		AC_MSG_NOTICE([Using included getopt and getopt_long])
		AC_LIBOBJ(getopt)
		our_getopt_long=yes
	fi
fi

if test "$our_getopt_long" = "yes"; then
	AC_LIBOBJ(getopt_long)
fi

dnl Check that this vis.h has *vis()-related functions because Solaris'
dnl vis.h doesn't.  Also, only consider the header found if it defines
dnl all of the functions that we need.
dnl
pkg_use_nbcompat_vis=yes
AC_CHECK_HEADERS([vis.h], [
	AC_MSG_TRY_COMPILE([for VIS_WHITE in vis.h], pkg_cv_have_vis_white, [
		#include <sys/types.h>
		#include <vis.h>
	], [ int X = VIS_WHITE; ], [
		AC_CHECK_FUNCS([strunvis strvis strsvis svis unvis vis])
		AC_CHECK_FUNCS([strunvis strvis strsvis svis unvis vis],
			[pkg_use_nbcompat_vis=no],
			[pkg_use_nbcompat_vis=yes; break])
	])
])

if test $pkg_use_nbcompat_vis = yes; then
	AC_DEFINE(HAVE_NBCOMPAT_VIS)
	AH_TEMPLATE([HAVE_NBCOMPAT_VIS], [
		Define to 1 if the `vis' function is built into the library.
	])
	AC_LIBOBJ(vis)
	AC_LIBOBJ(unvis)
fi

dnl Checks for typedefs, structures, and compiler characteristics.
AC_C___ATTRIBUTE__
AC_TYPE_SIZE_T
AC_TYPE_UINTPTR_T
AC_TYPE_INT8_T
AC_TYPE_INT16_T
AC_TYPE_INT32_T
AC_TYPE_INT64_T
AC_TYPE_UINT8_T
AC_TYPE_UINT16_T
AC_TYPE_UINT32_T
AC_TYPE_UINT64_T
AC_CHECK_TYPES([long long, long double],[],[],
[#if STDC_HEADERS
#include <stdlib.h>
#include <stddef.h>
#endif
#if HAVE_INTTYPES_H
#include <inttypes.h>
#endif
#if HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif
#if HAVE_SYS_BITYPES_H
#include <sys/bitypes.h>
#endif])
AC_CHECK_TYPES([fsid_t],[],[],
[#if HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif
#if HAVE_SYS_PARAM_H
#include <sys/param.h>
#endif
#if HAVE_SYS_MOUNT_H
#include <sys/mount.h>
#endif
#if HAVE_SYS_VFS_H
#include <sys/vfs.h>
#endif])
AC_C_BIGENDIAN
AC_CHECK_SIZEOF(off_t, 0)

# Checks for library functions.
AC_FUNC_STRERROR_R
AC_FUNC_VFORK

AC_CHECK_DECLS([snprintf(char *, size_t, const char *, ...)],
            [], [AC_LIBOBJ(snprintf)], [#include <stdio.h>])
AC_CHECK_DECLS([asprintf(char **, const char *, ...)],
            [], [AC_LIBOBJ(asprintf)],
            [#define _GNU_SOURCE
             #include <stdio.h>])
AC_CHECK_DECLS([strdup(const char *)],
            [], [AC_LIBOBJ(strdup)], [#include <string.h>])

case $host_os in
darwin*)
	# NetBSD PR 38488: warnx(3) escapes certain characters like \n
	ac_cv_func_warn=no
	;;
esac

AC_CHECK_DECLS([isblank(int)], [], [AC_LIBOBJ(isblank)], [#include <ctype.h>])

AC_REPLACE_FUNCS([err fgetln fnmatch fparseln getdelim getenv \
	getline lchflags lchmod lchown lutimes mkdtemp mkstemp setenv \
	setgroupent setpassent setprogname shquote statvfs \
	strerror strlcat strlcpy strmode strsep strtoll unsetenv usleep \
	utimes warn
])

AC_SEARCH_LIBS(socket, [socket network])
AC_CHECK_LIB(nsl, gethostbyname)

need_getaddrinfo=no
AC_CHECK_TYPES([socklen_t], [], [], [[#ifdef HAVE_SYS_TYPES_H
# include <sys/types.h>
#endif

#include <sys/socket.h>
]])
AC_CHECK_TYPES([struct addrinfo], [], [], [[#include <netdb.h>]])
AC_CHECK_TYPES([struct in6_addr], [], [], [[#ifdef HAVE_SYS_TYPES_H
# include <sys/types.h>
#endif
#include <netinet/in.h>
]])
AC_CHECK_TYPES([struct sockaddr_in6], [], [], [[#ifdef HAVE_SYS_TYPES_H
# include <sys/types.h>
#endif
#include <netinet/in.h>
]])
AC_CHECK_TYPES([struct sockaddr_storage], [], [], [[#ifdef HAVE_SYS_TYPES_H
# include <sys/types.h>
#endif
#include <sys/socket.h>
]])
dnl Assume that freeaddrinfo is only missing if getaddrinfo is also missing
AC_CHECK_FUNCS([getnameinfo gai_strerror getaddrinfo], [:],
    [need_getaddrinfo=yes])
if test $need_getaddrinfo = yes; then
	AC_LIBOBJ(getaddrinfo)
fi

need_findenv=no
AC_CHECK_FUNCS([getenv setenv unsetenv], [:], [need_findenv=yes])
if test $need_findenv = yes; then
	AC_LIBOBJ(findenv)
fi

need_gettemp=no
AC_CHECK_FUNCS([mkdtemp mkstemp], [:], [ need_gettemp=yes ])
if test $need_gettemp = yes; then
	AC_LIBOBJ(gettemp)
fi

AC_CHECK_FUNCS([setlocale])

need_setmode=no
AC_CHECK_FUNCS([getmode setmode], [:], [need_setmode=yes])

if test $need_setmode = yes; then
	AC_LIBOBJ(setmode)
fi

need_strnlen=no
AC_CHECK_FUNCS([strnlen], [:], [need_strnlen=yes])

if test $need_strnlen = yes; then
        AC_LIBOBJ(strnlen)
fi

AC_CHECK_DECLS([endgrent, endpwent, setpassent])

need_pwcache=no
AC_CHECK_FUNCS([gid_from_group group_from_gid pwcache_groupdb \
	pwcache_userdb uid_from_user user_from_uid \
], [:], [need_pwcache=yes])

if test $need_pwcache = yes; then
	AC_LIBOBJ(pwcache)
fi

AC_CHECK_FUNC(fts_open, [:], [
	AC_DEFINE(HAVE_NBCOMPAT_FTS, 1)
	AH_TEMPLATE([HAVE_NBCOMPAT_FTS], [
		Define to 1 if the `fts_*' functions are built into the
		library.
	])
	AC_LIBOBJ(fts)
])

AC_CHECK_FUNC(timegm, [:], [AC_LIBOBJ(timegm)])

# Handle Darwin 7 having a poll() compatibility function.  Be
# conservative -- if we don't find one of <poll.h> or <sys/poll.h>, then
# use our own poll() implementation.
#
pkg_cv_have_poll_h=no
AC_CHECK_HEADERS([poll.h sys/poll.h])
AC_CHECK_HEADERS([poll.h sys/poll.h], [pkg_cv_have_poll_h=yes])

pkg_use_nbcompat_poll=yes
if test $pkg_cv_have_poll_h = yes; then
	pkg_use_nbcompat_poll=no
fi

if test $pkg_cv_have_poll_h = yes -o test $pkg_cv_have_sys_poll_h = yes; then
	AC_CHECK_FUNC(poll, [
        	AC_MSG_TRY_COMPILE([if poll() is implemented natively],
                pkg_cv_native_poll,
		[ #if HAVE_POLL_H
		  #include <poll.h>
		  #endif
		  #if HAVE_SYS_POLL_H
		  #include <sys/poll.h>
		  #endif
		],
                [ #ifdef _POLL_EMUL_H_
		  #error "poll() is emulated"
	 	  #endif
		],
                [pkg_use_nbcompat_poll=no],
		[pkg_use_nbcompat_poll=yes])
	])
fi

if test $pkg_use_nbcompat_poll = yes; then
	AC_DEFINE(HAVE_NBCOMPAT_POLL, 1)
	AH_TEMPLATE([HAVE_NBCOMPAT_POLL], [
		Define to 1 if the `poll' function is built into the library.
	])
	AC_LIBOBJ(poll)
fi

AC_CHECK_FUNCS([getpassphrase getpgrp select])

AC_CHECK_FUNC(getpgrp, [AC_FUNC_GETPGRP])

AC_CHECK_MEMBERS([struct statfs.f_flags], [:], [:],
	[ #include <sys/statfs.h> ])
AC_CHECK_MEMBERS([struct statfs.f_fsize], [:], [:],
	[ #include <sys/statfs.h> ])
AC_CHECK_MEMBERS([struct statfs.f_iosize], [:], [:],
	[ #include <sys/statfs.h> ])
AC_CHECK_MEMBERS([struct dirent.d_namlen, DIR.dd_fd], [:], [:],
	[ #include <sys/types.h>
	  #if HAVE_DIRENT_H
	  # include <dirent.h>
	  #else
	  # define dirent direct
	  # if HAVE_SYS_NDIR_H
	  #  include <sys/ndir.h>
	  # endif
	  # if HAVE_SYS_DIR_H
	  #  include <sys/dir.h>
	  # endif
	  # if HAVE_NDIR_H
	  #  include <ndir.h>
	  # endif
	  #endif ])

AC_MSG_CHECKING([for dirfd])
AC_TRY_LINK(
  [ #include <sys/types.h>
    #if HAVE_DIRENT_H
    # include <dirent.h>
    #else
    # define dirent direct
    # if HAVE_SYS_NDIR_H
    #  include <sys/ndir.h>
    # endif
    # if HAVE_SYS_DIR_H
    #  include <sys/dir.h>
    # endif
    # if HAVE_NDIR_H
    #  include <ndir.h>
    # endif
    #endif
    int dummy_var;
  ], [ dummy_var = dirfd((DIR *)0) ],
  [
	AC_MSG_RESULT(yes)
	AC_DEFINE([HAVE_DIRFD], [1], [Define if dirfd is either a function or a macro.])
  ], [	AC_MSG_RESULT(no) ]

)

if test $ac_cv_type_long_long = yes; then

dnl		We assume that if sprintf() supports %lld or %qd,
dnl		then all of *printf() does. If not, disable long long
dnl		support because we don't know how to display it.

	AC_MSG_CHECKING(*printf() support for %lld)
	can_printf_longlong=no
	AC_TRY_RUN([
		#include <stdio.h>
		int main() {
			char buf[100];
			sprintf(buf, "%lld", 4294967300LL);
			return (strcmp(buf, "4294967300"));
		}
	], [
		AC_MSG_RESULT(yes)
		can_printf_longlong=yes
	], [
		AC_MSG_RESULT(no)
	], [:])

	if test $can_printf_longlong != yes; then
		AC_MSG_CHECKING(*printf() support for %qd)
		AC_TRY_RUN([
			#include <stdio.h>
			int main() {
				char buf[100];
				sprintf(buf, "%qd", 4294967300LL);
				return (strcmp(buf, "4294967300"));
			}
		], [
			AC_MSG_RESULT(yes)
			can_printf_longlong=yes
			AC_DEFINE(HAVE_PRINTF_QD, 1)
			AH_TEMPLATE([HAVE_PRINTF_QD], [
				Define to 1 if *printf() uses %qd to print
				`long long' (otherwise uses %lld).
			])
		], [
			AC_MSG_RESULT(no)
		], [:])
	fi

	if test $can_printf_longlong = yes; then
		AC_DEFINE(HAVE_WORKING_LONG_LONG, 1)
		AH_TEMPLATE([HAVE_WORKING_LONG_LONG], [
			Define to 1 if `long long' is supported and
			sizeof(off_t) >= 8.
		])
		AC_REPLACE_FUNCS(strtoll)
	fi

fi


dnl	Check if vsnprintf returns the number of bytes that would have been
dnl	written, had the buffer been large enough.

AC_MSG_CHECKING(if vsnprintf is standards compliant)
have_std_vsnprintf=no
AC_TRY_RUN([
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
void dotest(char *fmt, ...) {
	char buf[3];
	va_list ap;
	va_start(ap, fmt);
	exit(snprintf(buf, 3, fmt, ap) == 4 ? 0 : 1);
}
int main() {
	dotest("test");
}
], [
	AC_MSG_RESULT(yes)
	have_std_vsnprintf=yes
], [
	AC_MSG_RESULT(no)
], [:])

if test $have_std_vsnprintf = no; then
	AC_LIBOBJ(snprintf)
fi

AH_BOTTOM([
/* NBCOMPAT template section follows. */

#ifndef HAVE___ATTRIBUTE__
# define __attribute__(x)
#endif
])

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
