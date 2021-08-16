/*
 * Copyright (c) 1995, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * Prototypes for zip file support
 */

#ifndef _ZIP_H_
#define _ZIP_H_

#include "jni.h"

/*
 * Header signatures
 */
#define PKZIP_SIGNATURE_AT(p, b2, b3) \
  (((p)[0] == 'P') & ((p)[1] == 'K') & ((p)[2] == b2) & ((p)[3] == b3))
#define CENSIG_AT(p)       PKZIP_SIGNATURE_AT(p, 1, 2)
#define LOCSIG_AT(p)       PKZIP_SIGNATURE_AT(p, 3, 4)
#define ENDSIG_AT(p)       PKZIP_SIGNATURE_AT(p, 5, 6)
#define EXTSIG_AT(p)       PKZIP_SIGNATURE_AT(p, 7, 8)
#define ZIP64_ENDSIG_AT(p) PKZIP_SIGNATURE_AT(p, 6, 6)
#define ZIP64_LOCSIG_AT(p) PKZIP_SIGNATURE_AT(p, 6, 7)

/*
 * Header sizes including signatures
 */

#define LOCHDR 30
#define EXTHDR 16
#define CENHDR 46
#define ENDHDR 22

#define ZIP64_ENDHDR 56       // ZIP64 end header size
#define ZIP64_LOCHDR 20       // ZIP64 end loc header size
#define ZIP64_EXTHDR 24       // EXT header size
#define ZIP64_EXTID   1       // Extra field Zip64 header ID

#define ZIP64_MAGICVAL 0xffffffffLL
#define ZIP64_MAGICCOUNT 0xffff


/*
 * Header field access macros
 */
#define CH(b, n) (((unsigned char *)(b))[n])
#define SH(b, n) (CH(b, n) | (CH(b, n+1) << 8))
#define LG(b, n) ((SH(b, n) | (SH(b, n+2) << 16)) &0xffffffffUL)
#define LL(b, n) (((jlong)LG(b, n)) | (((jlong)LG(b, n+4)) << 32))
#define GETSIG(b) LG(b, 0)

/*
 * Macros for getting local file (LOC) header fields
 */
#define LOCVER(b) SH(b, 4)          /* version needed to extract */
#define LOCFLG(b) SH(b, 6)          /* general purpose bit flags */
#define LOCHOW(b) SH(b, 8)          /* compression method */
#define LOCTIM(b) LG(b, 10)         /* modification time */
#define LOCCRC(b) LG(b, 14)         /* crc of uncompressed data */
#define LOCSIZ(b) LG(b, 18)         /* compressed data size */
#define LOCLEN(b) LG(b, 22)         /* uncompressed data size */
#define LOCNAM(b) SH(b, 26)         /* filename length */
#define LOCEXT(b) SH(b, 28)         /* extra field length */

/*
 * Macros for getting extra local (EXT) header fields
 */
#define EXTCRC(b) LG(b, 4)          /* crc of uncompressed data */
#define EXTSIZ(b) LG(b, 8)          /* compressed size */
#define EXTLEN(b) LG(b, 12)         /* uncompressed size */

/*
 * Macros for getting central directory header (CEN) fields
 */
#define CENVEM(b) SH(b, 4)          /* version made by */
#define CENVER(b) SH(b, 6)          /* version needed to extract */
#define CENFLG(b) SH(b, 8)          /* general purpose bit flags */
#define CENHOW(b) SH(b, 10)         /* compression method */
#define CENTIM(b) LG(b, 12)         /* modification time */
#define CENCRC(b) LG(b, 16)         /* crc of uncompressed data */
#define CENSIZ(b) LG(b, 20)         /* compressed size */
#define CENLEN(b) LG(b, 24)         /* uncompressed size */
#define CENNAM(b) SH(b, 28)         /* length of filename */
#define CENEXT(b) SH(b, 30)         /* length of extra field */
#define CENCOM(b) SH(b, 32)         /* file comment length */
#define CENDSK(b) SH(b, 34)         /* disk number start */
#define CENATT(b) SH(b, 36)         /* internal file attributes */
#define CENATX(b) LG(b, 38)         /* external file attributes */
#define CENOFF(b) LG(b, 42)         /* offset of local header */

/*
 * Macros for getting end of central directory header (END) fields
 */
#define ENDSUB(b) SH(b, 8)          /* number of entries on this disk */
#define ENDTOT(b) SH(b, 10)         /* total number of entries */
#define ENDSIZ(b) LG(b, 12)         /* central directory size */
#define ENDOFF(b) LG(b, 16)         /* central directory offset */
#define ENDCOM(b) SH(b, 20)         /* size of zip file comment */

/*
 * Macros for getting Zip64 end of central directory header fields
 */
#define ZIP64_ENDLEN(b) LL(b, 4)      /* size of zip64 end of central dir */
#define ZIP64_ENDVEM(b) SH(b, 12)     /* version made by */
#define ZIP64_ENDVER(b) SH(b, 14)     /* version needed to extract */
#define ZIP64_ENDNMD(b) LG(b, 16)     /* number of this disk */
#define ZIP64_ENDDSK(b) LG(b, 20)     /* disk number of start */
#define ZIP64_ENDTOD(b) LL(b, 24)     /* total number of entries on this disk */
#define ZIP64_ENDTOT(b) LL(b, 32)     /* total number of entries */
#define ZIP64_ENDSIZ(b) LL(b, 40)     /* central directory size in bytes */
#define ZIP64_ENDOFF(b) LL(b, 48)     /* offset of first CEN header */

/*
 * Macros for getting Zip64 end of central directory locator fields
 */
#define ZIP64_LOCDSK(b) LG(b, 4)      /* disk number start */
#define ZIP64_LOCOFF(b) LL(b, 8)      /* offset of zip64 end */
#define ZIP64_LOCTOT(b) LG(b, 16)     /* total number of disks */

/*
 * Supported compression methods
 */
#define STORED      0
#define DEFLATED    8

/*
 * Support for reading ZIP/JAR files. Some things worth noting:
 *
 * - Zip file entries larger than 2**32 bytes are not supported.
 * - jzentry time and crc fields are signed even though they really
 *   represent unsigned quantities.
 * - If csize is zero then the entry is uncompressed.
 * - If extra != 0 then the first two bytes are the length of the extra
 *   data in intel byte order.
 * - If pos <= 0 then it is the position of entry LOC header.
 *   If pos > 0 then it is the position of entry data.
 *   pos should not be accessed directly, but only by ZIP_GetEntryDataOffset.
 * - entry name may include embedded null character, use nlen for length
 */

typedef struct jzentry {  /* Zip file entry */
    char *name;           /* entry name */
    jlong time;           /* modification time */
    jlong size;           /* size of uncompressed data */
    jlong csize;          /* size of compressed data (zero if uncompressed) */
    jint crc;             /* crc of uncompressed data */
    char *comment;        /* optional zip file comment */
    jbyte *extra;         /* optional extra data */
    jlong pos;            /* position of LOC header or entry data */
    jint flag;            /* general purpose flag */
    jint nlen;            /* length of the entry name */
} jzentry;

/*
 * In-memory hash table cell.
 * In a typical system we have a *lot* of these, as we have one for
 * every entry in every active JAR.
 * Note that in order to save space we don't keep the name in memory,
 * but merely remember a 32 bit hash.
 */
typedef struct jzcell {
    unsigned int hash;    /* 32 bit hashcode on name */
    unsigned int next;    /* hash chain: index into jzfile->entries */
    jlong cenpos;         /* Offset of central directory file header */
} jzcell;

typedef struct cencache {
    char *data;           /* A cached page of CEN headers */
    jlong pos;            /* file offset of data */
} cencache;

/*
 * Use ZFILE to represent access to a file in a platform-indepenent
 * fashion.
 */
#ifdef WIN32
#define ZFILE jlong
#else
#define ZFILE int
#endif

/*
 * Descriptor for a ZIP file.
 */
typedef struct jzfile {   /* Zip file */
    char *name;           /* zip file name */
    jint refs;            /* number of active references */
    jlong len;            /* length (in bytes) of zip file */
#ifdef USE_MMAP
    unsigned char *maddr; /* beginning address of the CEN & ENDHDR */
    jlong mlen;           /* length (in bytes) mmaped */
    jlong offset;         /* offset of the mmapped region from the
                             start of the file. */
    jboolean usemmap;     /* if mmap is used. */
#endif
    jboolean locsig;      /* if zip file starts with LOCSIG */
    cencache cencache;    /* CEN header cache */
    ZFILE zfd;            /* open file descriptor */
    void *lock;           /* read lock */
    char *comment;        /* zip file comment */
    jint clen;            /* length of the zip file comment */
    char *msg;            /* zip error message */
    jzcell *entries;      /* array of hash cells */
    jint total;           /* total number of entries */
    jint *table;          /* Hash chain heads: indexes into entries */
    jint tablelen;        /* number of hash heads */
    struct jzfile *next;  /* next zip file in search list */
    jzentry *cache;       /* we cache the most recently freed jzentry */
    /* Information on metadata names in META-INF directory */
    char **metanames;     /* array of meta names (may have null names) */
    jint metacurrent;     /* the next empty slot in metanames array */
    jint metacount;       /* number of slots in metanames array */
    jlong lastModified;   /* last modified time */
    jlong locpos;         /* position of first LOC header (usually 0) */
} jzfile;

/*
 * Index representing end of hash chain
 */
#define ZIP_ENDCHAIN ((jint)-1)

JNIEXPORT jzentry *
ZIP_FindEntry(jzfile *zip, char *name, jint *sizeP, jint *nameLenP);

JNIEXPORT jboolean
ZIP_ReadEntry(jzfile *zip, jzentry *entry, unsigned char *buf, char *entrynm);

JNIEXPORT jzentry *
ZIP_GetNextEntry(jzfile *zip, jint n);

JNIEXPORT jzfile *
ZIP_Open(const char *name, char **pmsg);

jzfile *
ZIP_Open_Generic(const char *name, char **pmsg, int mode, jlong lastModified);

jzfile *
ZIP_Get_From_Cache(const char *name, char **pmsg, jlong lastModified);

jzfile *
ZIP_Put_In_Cache(const char *name, ZFILE zfd, char **pmsg, jlong lastModified);

jzfile *
ZIP_Put_In_Cache0(const char *name, ZFILE zfd, char **pmsg, jlong lastModified, jboolean usemmap);

JNIEXPORT void
ZIP_Close(jzfile *zip);

jzentry *
ZIP_GetEntry(jzfile *zip, char *name, jint ulen);
void
ZIP_Lock(jzfile *zip);
void
ZIP_Unlock(jzfile *zip);
jint
ZIP_Read(jzfile *zip, jzentry *entry, jlong pos, void *buf, jint len);
void
ZIP_FreeEntry(jzfile *zip, jzentry *ze);
jlong ZIP_GetEntryDataOffset(jzfile *zip, jzentry *entry);
jzentry * ZIP_GetEntry2(jzfile *zip, char *name, jint ulen, jboolean addSlash);

JNIEXPORT jboolean
ZIP_InflateFully(void *inBuf, jlong inLen, void *outBuf, jlong outLen, char **pmsg);

#endif /* !_ZIP_H_ */
