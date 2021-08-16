/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _MANIFEST_INFO_H
#define _MANIFEST_INFO_H

#include <sys/types.h>
#include "jni.h"

/*
 * Zip file header signatures
 */
#define SIGSIZ 4                    /* size of all header signatures */

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
#define ENDNMD(b) SH(b, 4)          /* number of this disk */
#define ENDDSK(b) SH(b, 6)          /* disk number of start */
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
 * A comment of maximum length of 64kb can follow the END record. This
 * is the furthest the END record can be from the end of the file.
 */
#define END_MAXLEN      (0xFFFF + ENDHDR)

/*
 * Supported compression methods.
 */
#define STORED      0
#define DEFLATED    8

/*
 * Information from the CEN entry to inflate a file.
 */
typedef struct zentry { /* Zip file entry */
    size_t      isize;  /* size of inflated data */
    size_t      csize;  /* size of compressed data (zero if uncompressed) */
    jlong       offset; /* position of compressed data */
    int         how;    /* compression method (if any) */
} zentry;

/*
 * Information returned from the Manifest file by the ParseManifest() routine.
 * Certainly (much) more could be returned, but this is the information
 * currently of interest to the C based Java utilities (particularly the
 * Java launcher).
 */
typedef struct manifest_info {  /* Interesting fields from the Manifest */
    char        *manifest_version;      /* Manifest-Version string */
    char        *main_class;            /* Main-Class entry */
    char        *jre_version;           /* Appropriate J2SE release spec */
    char        jre_restrict_search;    /* Restricted JRE search */
    char        *splashscreen_image_file_name; /* splashscreen image file */
} manifest_info;

/*
 * Attribute closure to provide to manifest_iterate.
 */
typedef void (*attribute_closure)(const char *name, const char *value,
        void *user_data);

/*
 * Function prototypes.
 */
int     JLI_ParseManifest(char *jarfile, manifest_info *info);
void    *JLI_JarUnpackFile(const char *jarfile, const char *filename,
                int *size);
void    JLI_FreeManifest(void);

JNIEXPORT int JNICALL
JLI_ManifestIterate(const char *jarfile, attribute_closure ac,
                void *user_data);

#endif  /* _MANIFEST_INFO_H */
