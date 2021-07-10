# $NetBSD: Makefile.inc,v 1.1 2008/10/10 00:21:43 joerg Exp $

.PATH: ${.PARSEDIR}/btree ${.PARSEDIR}/db ${.PARSEDIR}/hash \
    ${.PARSEDIR}/mpool ${.PARSEDIR}/recno

CPPFLAGS+=-D__DBINTERFACE_PRIVATE

OBJS+=	bt_close.o bt_conv.o bt_debug.o bt_delete.o bt_get.o bt_open.o \
	bt_overflow.o bt_page.o bt_put.o bt_search.o bt_seq.o bt_split.o \
	bt_utils.o \
	db.o \
	hash.o hash_bigkey.o hash_buf.o hash_func.o hash_log2.o hash_page.o \
	ndbm.o ndbmdatum.o \
	mpool.o rec_close.o rec_delete.o rec_get.o rec_open.o rec_put.o \
	rec_search.o rec_seq.o rec_utils.o

INCS+=		nbcompat/db.h
