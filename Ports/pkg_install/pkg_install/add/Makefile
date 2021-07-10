# $NetBSD: Makefile.in,v 1.34 2020/10/28 16:52:43 maya Exp $

srcdir=		.

prefix=		/usr/local
exec_prefix=	${prefix}
sbindir=	${exec_prefix}/sbin
mandir=		${datarootdir}/man
datarootdir=	${prefix}/share

man1dir=	$(mandir)/man1
cat1dir=	$(mandir)/cat1

CC=		i686-pc-serenity-gcc
CCLD=		$(CC)
CPPFLAGS=	 -I. -I$(srcdir) -I../lib
DEFS=		-DHAVE_CONFIG_H -DOPSYS_NAME=\"$(OPSYS)\" -DPKGSRC_MACHINE_ARCH=\"$(PKGSRC_MACHINE_ARCH)\" -DBINDIR=\"$(sbindir)\"
CFLAGS=		-g -O2
LDFLAGS=	 -L../lib

BOOTSTRAP=	yes
SSL_SUPPORT=	

.if empty(BOOTSTRAP)
LIBS=		-linstall -lnetpgpverify -larchive -lbz2 -lz -lfetch
.if !empty(SSL_SUPPORT)
LIBS+=		-lssl -lcrypto
CPPFLAGS+=	-DHAVE_SSL
.endif
LIBS+=		-lnbcompat
.else
LIBS=		-linstall ${STATIC_LIBARCHIVE} ${STATIC_LIBARCHIVE_LDADD} -lnbcompat
CPPFLAGS+=	-DBOOTSTRAP
.endif

INSTALL=	/opt/pkg/bin/ginstall -c

PROG=		pkg_add

OBJS=	main.o perform.o

all: $(PROG)

.c.o:
	$(CC) $(DEFS) $(CPPFLAGS) $(CFLAGS) -c $<

$(PROG): $(OBJS)
	$(CCLD) $(LDFLAGS) -o $@ $(OBJS) $(LIBS)

clean:
	rm -f $(OBJS) $(PROG)

install:
	$(INSTALL) -m 755 -d ${DESTDIR}$(sbindir)
	$(INSTALL) -m 755 -d ${DESTDIR}$(man1dir)
	$(INSTALL) $(PROG) ${DESTDIR}$(sbindir)/$(PROG)
	$(INSTALL) -m 444 $(PROG).1 ${DESTDIR}$(man1dir)/$(PROG).1
.if defined(MANINSTALL) && !empty(MANINSTALL:Mcatinstall)
	$(INSTALL) -m 755 -d ${DESTDIR}$(cat1dir)
.  if defined(CATMAN_SECTION_SUFFIX) && !empty(CATMAN_SECTION_SUFFIX:M[Yy][Ee][Ss])
	$(INSTALL) -m 444 $(PROG).cat ${DESTDIR}$(cat1dir)/$(PROG).1
.  else
	$(INSTALL) -m 444 $(PROG).cat ${DESTDIR}$(cat1dir)/$(PROG).0
.  endif
.endif
