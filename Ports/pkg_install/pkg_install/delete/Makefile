# $NetBSD: Makefile.in,v 1.24 2015/09/07 09:06:05 jperkin Exp $

srcdir=		.

prefix=		/usr/local
exec_prefix=	${prefix}
sbindir=	${exec_prefix}/sbin
mandir=		${datarootdir}/man
datarootdir=	${prefix}/share

man1dir=	$(mandir)/man1
cat1dir=	$(mandir)/cat1

SSL_SUPPORT=	

CC=		i686-pc-serenity-gcc
CCLD=		$(CC)
LIBS=		-linstall -lnetpgpverify -larchive -lbz2 -lz -lfetch -lnbcompat

.if !empty(SSL_SUPPORT)
LIBS+=		-lssl -lcrypto
.endif

CPPFLAGS=	 -I. -I$(srcdir) -I../lib -DBINDIR=\"$(sbindir)\"
DEFS=		-DHAVE_CONFIG_H
CFLAGS=		-g -O2
LDFLAGS=	 -L../lib

INSTALL=	/opt/pkg/bin/ginstall -c

PROG=		pkg_delete

OBJS=		pkg_delete.o

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
