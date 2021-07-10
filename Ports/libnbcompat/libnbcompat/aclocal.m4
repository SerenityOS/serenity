dnl $NetBSD: aclocal.m4,v 1.4 2006/03/01 16:47:54 joerg Exp $
dnl

dnl
dnl AC_MSG_TRY_COMPILE
dnl
dnl Written by Luke Mewburn <lukem@netbsd.org>
dnl
dnl Usage:
dnl	AC_MSG_TRY_COMPILE(Message, CacheVar, Includes, Code,
dnl			    ActionPass [,ActionFail] )
dnl
dnl effectively does:
dnl	AC_CACHE_CHECK(Message, CacheVar,
dnl		AC_TRY_COMPILE(Includes, Code, CacheVar = yes, CacheVar = no)
dnl		if CacheVar == yes
dnl			AC_MESSAGE_RESULT(yes)
dnl			ActionPass
dnl		else
dnl			AC_MESSAGE_RESULT(no)
dnl			ActionFail
dnl	)
dnl
AC_DEFUN(AC_MSG_TRY_COMPILE, [
	AC_CACHE_CHECK($1, $2, [
		AC_TRY_COMPILE([ $3 ], [ $4; ], [ $2=yes ], [ $2=no ])
	])
	if test "x[$]$2" = "xyes"; then
		$5
	else
		$6
		:
	fi
])

dnl
dnl AC_MSG_TRY_LINK
dnl
dnl Usage:
dnl	AC_MSG_TRY_LINK(Message, CacheVar, Includes, Code,
dnl			    ActionPass [,ActionFail] )
dnl
dnl as AC_MSG_TRY_COMPILE, but uses AC_TRY_LINK instead of AC_TRY_COMPILE
dnl
AC_DEFUN(AC_MSG_TRY_LINK, [
	AC_CACHE_CHECK($1, $2, [
		AC_TRY_LINK([ $3 ], [ $4; ], [ $2=yes ], [ $2=no ])
	])
	if test "x[$]$2" = "xyes"; then
		$5
	else
		$6
		:
	fi
])


dnl
dnl AC_LIBRARY_NET: #Id: net.m4,v 1.5 1997/11/09 21:36:54 jhawk Exp #
dnl
dnl Written by John Hawkinson <jhawk@mit.edu>. This code is in the Public
dnl Domain.
dnl
dnl This test is for network applications that need socket() and
dnl gethostbyname() -ish functions.  Under Solaris, those applications need to
dnl link with "-lsocket -lnsl".  Under IRIX, they should *not* link with
dnl "-lsocket" because libsocket.a breaks a number of things (for instance:
dnl gethostbyname() under IRIX 5.2, and snoop sockets under most versions of
dnl IRIX).
dnl 
dnl Unfortunately, many application developers are not aware of this, and
dnl mistakenly write tests that cause -lsocket to be used under IRIX.  It is
dnl also easy to write tests that cause -lnsl to be used under operating
dnl systems where neither are necessary (or useful), such as SunOS 4.1.4, which
dnl uses -lnsl for TLI.
dnl 
dnl This test exists so that every application developer does not test this in
dnl a different, and subtly broken fashion.
dnl 
dnl It has been argued that this test should be broken up into two separate
dnl tests, one for the resolver libraries, and one for the libraries necessary
dnl for using Sockets API. Unfortunately, the two are carefully intertwined and
dnl allowing the autoconf user to use them independently potentially results in
dnl unfortunate ordering dependencies -- as such, such component macros would
dnl have to carefully use indirection and be aware if the other components were
dnl executed. Since other autoconf macros do not go to this trouble, and almost
dnl no applications use sockets without the resolver, this complexity has not
dnl been implemented.
dnl
dnl The check for libresolv is in case you are attempting to link statically
dnl and happen to have a libresolv.a lying around (and no libnsl.a).
dnl
AC_DEFUN(AC_LIBRARY_NET, [
   # Most operating systems have gethostbyname() in the default searched
   # libraries (i.e. libc):
   AC_CHECK_FUNC(gethostbyname, ,
     # Some OSes (eg. Solaris) place it in libnsl:
     AC_CHECK_LIB(nsl, gethostbyname, , 
       # Some strange OSes (SINIX) have it in libsocket:
       AC_CHECK_LIB(socket, gethostbyname, ,
          # Unfortunately libsocket sometimes depends on libnsl.
          # AC_CHECK_LIB's API is essentially broken so the following
          # ugliness is necessary:
          AC_CHECK_LIB(socket, gethostbyname,
             LIBS="-lsocket -lnsl $LIBS",
               AC_CHECK_LIB(resolv, gethostbyname),
             -lnsl)
       )
     )
   )
  AC_CHECK_FUNC(socket, , AC_CHECK_LIB(socket, socket, ,
    AC_CHECK_LIB(socket, socket, LIBS="-lsocket -lnsl $LIBS", , -lnsl)))
  ])


dnl From heimdal sources
dnl Id: c-attribute.m4,v 1.5 2004/08/26 12:35:41 joda Exp 
dnl

dnl
dnl Test for __attribute__
dnl

AC_DEFUN([AC_C___ATTRIBUTE__], [
AC_MSG_CHECKING(for __attribute__)
AC_CACHE_VAL(ac_cv___attribute__, [
AC_COMPILE_IFELSE([AC_LANG_SOURCE([[#include <stdlib.h>
static void foo(void) __attribute__ ((noreturn));

static void
foo(void)
{
  exit(1);
}
]])],
[ac_cv___attribute__=yes],
[ac_cv___attribute__=no])])
if test "$ac_cv___attribute__" = "yes"; then
  AC_DEFINE(HAVE___ATTRIBUTE__, 1, [define if your compiler has __attribute__])
fi
AC_MSG_RESULT($ac_cv___attribute__)
])

