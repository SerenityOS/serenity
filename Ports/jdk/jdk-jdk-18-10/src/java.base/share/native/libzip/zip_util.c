/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Support for reading ZIP/JAR files.
 */

#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include <fcntl.h>
#include <limits.h>
#include <time.h>
#include <ctype.h>
#include <assert.h>

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"
#include "jvm.h"
#include "io_util.h"
#include "io_util_md.h"
#include "zip_util.h"
#include <zlib.h>

#ifdef _ALLBSD_SOURCE
#define off64_t off_t
#define mmap64 mmap
#endif

/* USE_MMAP means mmap the CEN & ENDHDR part of the zip file. */
#ifdef USE_MMAP
#include <sys/mman.h>
#endif

#define MAXREFS 0xFFFF  /* max number of open zip file references */

#define MCREATE()      JVM_RawMonitorCreate()
#define MLOCK(lock)    JVM_RawMonitorEnter(lock)
#define MUNLOCK(lock)  JVM_RawMonitorExit(lock)
#define MDESTROY(lock) JVM_RawMonitorDestroy(lock)

#define CENSIZE(cen) (CENHDR + CENNAM(cen) + CENEXT(cen) + CENCOM(cen))

static jzfile *zfiles = 0;      /* currently open zip files */
static void *zfiles_lock = 0;

static void freeCEN(jzfile *);

#ifndef PATH_MAX
#define PATH_MAX 1024
#endif

static jint INITIAL_META_COUNT = 2;   /* initial number of entries in meta name array */

/*
 * Declare library specific JNI_Onload entry if static build
 */
#ifdef STATIC_BUILD
DEF_STATIC_JNI_OnLoad
#endif

/*
 * The ZFILE_* functions exist to provide some platform-independence with
 * respect to file access needs.
 */

/*
 * Opens the named file for reading, returning a ZFILE.
 *
 * Compare this with winFileHandleOpen in windows/native/java/io/io_util_md.c.
 * This function does not take JNIEnv* and uses CreateFile (instead of
 * CreateFileW).  The expectation is that this function will be called only
 * from ZIP_Open_Generic, which in turn is used by the JVM, where we do not
 * need to concern ourselves with wide chars.
 */
static ZFILE
ZFILE_Open(const char *fname, int flags) {
#ifdef WIN32
    WCHAR *wfname, *wprefixed_fname;
    size_t fname_length;
    jlong fhandle;
    const DWORD access =
        (flags & O_RDWR)   ? (GENERIC_WRITE | GENERIC_READ) :
        (flags & O_WRONLY) ?  GENERIC_WRITE :
        GENERIC_READ;
    const DWORD sharing =
        FILE_SHARE_READ | FILE_SHARE_WRITE;
    const DWORD disposition =
        /* Note: O_TRUNC overrides O_CREAT */
        (flags & O_TRUNC) ? CREATE_ALWAYS :
        (flags & O_CREAT) ? OPEN_ALWAYS   :
        OPEN_EXISTING;
    const DWORD  maybeWriteThrough =
        (flags & (O_SYNC | O_DSYNC)) ?
        FILE_FLAG_WRITE_THROUGH :
        FILE_ATTRIBUTE_NORMAL;
    const DWORD maybeDeleteOnClose =
        (flags & O_TEMPORARY) ?
        FILE_FLAG_DELETE_ON_CLOSE :
        FILE_ATTRIBUTE_NORMAL;
    const DWORD flagsAndAttributes = maybeWriteThrough | maybeDeleteOnClose;

    fname_length = strlen(fname);
    if (fname_length < MAX_PATH) {
        return (jlong)CreateFile(
            fname,              /* path name in multibyte char */
            access,             /* Read and/or write permission */
            sharing,            /* File sharing flags */
            NULL,               /* Security attributes */
            disposition,        /* creation disposition */
            flagsAndAttributes, /* flags and attributes */
            NULL);
    } else {
        /* Get required buffer size to convert to Unicode */
        int wfname_len = MultiByteToWideChar(CP_ACP, MB_ERR_INVALID_CHARS,
                                             fname, -1, NULL, 0);
        if (wfname_len == 0) {
            return (jlong)INVALID_HANDLE_VALUE;
        }
        if ((wfname = (WCHAR*)malloc(wfname_len * sizeof(WCHAR))) == NULL) {
            return (jlong)INVALID_HANDLE_VALUE;
        }
        if (MultiByteToWideChar(CP_ACP, MB_ERR_INVALID_CHARS,
                                fname, -1, wfname, wfname_len) == 0) {
            free(wfname);
            return (jlong)INVALID_HANDLE_VALUE;
        }
        wprefixed_fname = getPrefixed(wfname, (int)fname_length);
        fhandle = (jlong)CreateFileW(
            wprefixed_fname,    /* Wide char path name */
            access,             /* Read and/or write permission */
            sharing,            /* File sharing flags */
            NULL,               /* Security attributes */
            disposition,        /* creation disposition */
            flagsAndAttributes, /* flags and attributes */
            NULL);
        free(wfname);
        free(wprefixed_fname);
        return fhandle;
    }
#else
    return open(fname, flags, 0);
#endif
}

/*
 * The io_util_md.h files do not provide IO_CLOSE, hence we use platform
 * specifics.
 */
static void
ZFILE_Close(ZFILE zfd) {
#ifdef WIN32
    CloseHandle((HANDLE) zfd);
#else
    close(zfd);
#endif
}

static int
ZFILE_read(ZFILE zfd, char *buf, jint nbytes) {
#ifdef WIN32
    return (int) IO_Read(zfd, buf, nbytes);
#else
    return read(zfd, buf, nbytes);
#endif
}

/*
 * Initialize zip file support. Return 0 if successful otherwise -1
 * if could not be initialized.
 */
static jint
InitializeZip()
{
    static jboolean inited = JNI_FALSE;

    // Initialize errno to 0.  It may be set later (e.g. during memory
    // allocation) but we can disregard previous values.
    errno = 0;

    if (inited)
        return 0;
    zfiles_lock = MCREATE();
    if (zfiles_lock == 0) {
        return -1;
    }
    inited = JNI_TRUE;

    return 0;
}

/*
 * Reads len bytes of data into buf.
 * Returns 0 if all bytes could be read, otherwise returns -1.
 */
static int
readFully(ZFILE zfd, void *buf, jlong len) {
  char *bp = (char *) buf;

  while (len > 0) {
        jlong limit = ((((jlong) 1) << 31) - 1);
        jint count = (len < limit) ?
            (jint) len :
            (jint) limit;
        jint n = ZFILE_read(zfd, bp, count);
        if (n > 0) {
            bp += n;
            len -= n;
        } else if (n == -1 && errno == EINTR) {
          /* Retry after EINTR (interrupted by signal). */
            continue;
        } else { /* EOF or IO error */
            return -1;
        }
    }
    return 0;
}

/*
 * Reads len bytes of data from the specified offset into buf.
 * Returns 0 if all bytes could be read, otherwise returns -1.
 */
static int
readFullyAt(ZFILE zfd, void *buf, jlong len, jlong offset)
{
    if (IO_Lseek(zfd, offset, SEEK_SET) == -1) {
        return -1; /* lseek failure. */
    }

    return readFully(zfd, buf, len);
}

/*
 * Allocates a new zip file object for the specified file name.
 * Returns the zip file object or NULL if not enough memory.
 */
static jzfile *
allocZip(const char *name)
{
    jzfile *zip;
    if (((zip = calloc(1, sizeof(jzfile))) != NULL) &&
        ((zip->name = strdup(name))        != NULL) &&
        ((zip->lock = MCREATE())           != NULL)) {
        zip->zfd = -1;
        return zip;
    }

    if (zip != NULL) {
        free(zip->name);
        free(zip);
    }
    return NULL;
}

/*
 * Frees all native resources owned by the specified zip file object.
 */
static void
freeZip(jzfile *zip)
{
    /* First free any cached jzentry */
    ZIP_FreeEntry(zip,0);
    if (zip->lock != NULL) MDESTROY(zip->lock);
    free(zip->name);
    freeCEN(zip);

#ifdef USE_MMAP
    if (zip->usemmap) {
        if (zip->maddr != NULL)
            munmap((char *)zip->maddr, zip->mlen);
    } else
#endif
    {
        free(zip->cencache.data);
    }
    if (zip->comment != NULL)
        free(zip->comment);
    if (zip->zfd != -1) ZFILE_Close(zip->zfd);
    free(zip);
}

/* The END header is followed by a variable length comment of size < 64k. */
static const jlong END_MAXLEN = 0xFFFF + ENDHDR;

#define READBLOCKSZ 128

static jboolean verifyEND(jzfile *zip, jlong endpos, char *endbuf) {
    /* ENDSIG matched, however the size of file comment in it does not
       match the real size. One "common" cause for this problem is some
       "extra" bytes are padded at the end of the zipfile.
       Let's do some extra verification, we don't care about the performance
       in this situation.
     */
    jlong cenpos = endpos - ENDSIZ(endbuf);
    jlong locpos = cenpos - ENDOFF(endbuf);
    char buf[4];
    return (cenpos >= 0 &&
            locpos >= 0 &&
            readFullyAt(zip->zfd, buf, sizeof(buf), cenpos) != -1 &&
            CENSIG_AT(buf) &&
            readFullyAt(zip->zfd, buf, sizeof(buf), locpos) != -1 &&
            LOCSIG_AT(buf));
}

/*
 * Searches for end of central directory (END) header. The contents of
 * the END header will be read and placed in endbuf. Returns the file
 * position of the END header, otherwise returns -1 if the END header
 * was not found or an error occurred.
 */
static jlong
findEND(jzfile *zip, void *endbuf)
{
    char buf[READBLOCKSZ];
    jlong pos;
    const jlong len = zip->len;
    const ZFILE zfd = zip->zfd;
    const jlong minHDR = len - END_MAXLEN > 0 ? len - END_MAXLEN : 0;
    const jlong minPos = minHDR - (sizeof(buf)-ENDHDR);
    jint clen;

    for (pos = len - sizeof(buf); pos >= minPos; pos -= (sizeof(buf)-ENDHDR)) {

        int i;
        jlong off = 0;
        if (pos < 0) {
            /* Pretend there are some NUL bytes before start of file */
            off = -pos;
            memset(buf, '\0', (size_t)off);
        }

        if (readFullyAt(zfd, buf + off, sizeof(buf) - off,
                        pos + off) == -1) {
            return -1;  /* System error */
        }

        /* Now scan the block backwards for END header signature */
        for (i = sizeof(buf) - ENDHDR; i >= 0; i--) {
            if (buf[i+0] == 'P'    &&
                buf[i+1] == 'K'    &&
                buf[i+2] == '\005' &&
                buf[i+3] == '\006' &&
                ((pos + i + ENDHDR + ENDCOM(buf + i) == len)
                 || verifyEND(zip, pos + i, buf + i))) {
                /* Found END header */
                memcpy(endbuf, buf + i, ENDHDR);

                clen = ENDCOM(endbuf);
                if (clen != 0) {
                    zip->comment = malloc(clen + 1);
                    if (zip->comment == NULL) {
                        return -1;
                    }
                    if (readFullyAt(zfd, zip->comment, clen, pos + i + ENDHDR)
                        == -1) {
                        free(zip->comment);
                        zip->comment = NULL;
                        return -1;
                    }
                    zip->comment[clen] = '\0';
                    zip->clen = clen;
                }
                return pos + i;
            }
        }
    }

    return -1; /* END header not found */
}

/*
 * Searches for the ZIP64 end of central directory (END) header. The
 * contents of the ZIP64 END header will be read and placed in end64buf.
 * Returns the file position of the ZIP64 END header, otherwise returns
 * -1 if the END header was not found or an error occurred.
 *
 * The ZIP format specifies the "position" of each related record as
 *   ...
 *   [central directory]
 *   [zip64 end of central directory record]
 *   [zip64 end of central directory locator]
 *   [end of central directory record]
 *
 * The offset of zip64 end locator can be calculated from endpos as
 * "endpos - ZIP64_LOCHDR".
 * The "offset" of zip64 end record is stored in zip64 end locator.
 */
static jlong
findEND64(jzfile *zip, void *end64buf, jlong endpos)
{
    char loc64[ZIP64_LOCHDR];
    jlong end64pos;
    if (readFullyAt(zip->zfd, loc64, ZIP64_LOCHDR, endpos - ZIP64_LOCHDR) == -1) {
        return -1;    // end64 locator not found
    }
    end64pos = ZIP64_LOCOFF(loc64);
    if (readFullyAt(zip->zfd, end64buf, ZIP64_ENDHDR, end64pos) == -1) {
        return -1;    // end64 record not found
    }
    return end64pos;
}

/*
 * Returns a hash code value for a C-style NUL-terminated string.
 */
static unsigned int
hash(const char *s)
{
    int h = 0;
    while (*s != '\0')
        h = 31*h + *s++;
    return h;
}

/*
 * Returns a hash code value for a string of a specified length.
 */
static unsigned int
hashN(const char *s, int length)
{
    int h = 0;
    while (length-- > 0)
        h = 31*h + *s++;
    return h;
}

static unsigned int
hash_append(unsigned int hash, char c)
{
    return ((int)hash)*31 + c;
}

/*
 * Returns true if the specified entry's name begins with the string
 * "META-INF/" irrespective of case.
 */
static int
isMetaName(const char *name, int length)
{
    const char *s;
    if (length < (int)sizeof("META-INF/") - 1)
        return 0;
    for (s = "META-INF/"; *s != '\0'; s++) {
        char c = *name++;
        // Avoid toupper; it's locale-dependent
        if (c >= 'a' && c <= 'z') c += 'A' - 'a';
        if (*s != c)
            return 0;
    }
    return 1;
}

/*
 * Increases the capacity of zip->metanames.
 * Returns non-zero in case of allocation error.
 */
static int
growMetaNames(jzfile *zip)
{
    jint i;
    /* double the meta names array */
    const jint new_metacount = zip->metacount << 1;
    zip->metanames =
        realloc(zip->metanames, new_metacount * sizeof(zip->metanames[0]));
    if (zip->metanames == NULL) return -1;
    for (i = zip->metacount; i < new_metacount; i++)
        zip->metanames[i] = NULL;
    zip->metacurrent = zip->metacount;
    zip->metacount = new_metacount;
    return 0;
}

/*
 * Adds name to zip->metanames.
 * Returns non-zero in case of allocation error.
 */
static int
addMetaName(jzfile *zip, const char *name, int length)
{
    jint i;
    if (zip->metanames == NULL) {
      zip->metacount = INITIAL_META_COUNT;
      zip->metanames = calloc(zip->metacount, sizeof(zip->metanames[0]));
      if (zip->metanames == NULL) return -1;
      zip->metacurrent = 0;
    }

    i = zip->metacurrent;

    /* current meta name array isn't full yet. */
    if (i < zip->metacount) {
      zip->metanames[i] = (char *) malloc(length+1);
      if (zip->metanames[i] == NULL) return -1;
      memcpy(zip->metanames[i], name, length);
      zip->metanames[i][length] = '\0';
      zip->metacurrent++;
      return 0;
    }

    /* No free entries in zip->metanames? */
    if (growMetaNames(zip) != 0) return -1;
    return addMetaName(zip, name, length);
}

static void
freeMetaNames(jzfile *zip)
{
    if (zip->metanames) {
        jint i;
        for (i = 0; i < zip->metacount; i++)
            free(zip->metanames[i]);
        free(zip->metanames);
        zip->metanames = NULL;
    }
}

/* Free Zip data allocated by readCEN() */
static void
freeCEN(jzfile *zip)
{
    free(zip->entries); zip->entries = NULL;
    free(zip->table);   zip->table   = NULL;
    freeMetaNames(zip);
}

/*
 * Counts the number of CEN headers in a central directory extending
 * from BEG to END.  Might return a bogus answer if the zip file is
 * corrupt, but will not crash.
 */
static jint
countCENHeaders(unsigned char *beg, unsigned char *end)
{
    jint count = 0;
    ptrdiff_t i;
    for (i = 0; i + CENHDR <= end - beg; i += CENSIZE(beg + i))
        count++;
    return count;
}

#define ZIP_FORMAT_ERROR(message) \
if (1) { zip->msg = message; goto Catch; } else ((void)0)

/*
 * Reads zip file central directory. Returns the file position of first
 * CEN header, otherwise returns -1 if an error occurred. If zip->msg != NULL
 * then the error was a zip format error and zip->msg has the error text.
 * Always pass in -1 for knownTotal; it's used for a recursive call.
 */
static jlong
readCEN(jzfile *zip, jint knownTotal)
{
    /* Following are unsigned 32-bit */
    jlong endpos, end64pos, cenpos, cenlen, cenoff;
    /* Following are unsigned 16-bit */
    jint total, tablelen, i, j;
    unsigned char *cenbuf = NULL;
    unsigned char *cenend;
    unsigned char *cp;
#ifdef USE_MMAP
    static jlong pagesize;
    jlong offset;
#endif
    unsigned char endbuf[ENDHDR];
    jint endhdrlen = ENDHDR;
    jzcell *entries;
    jint *table;

    /* Clear previous zip error */
    zip->msg = NULL;
    /* Get position of END header */
    if ((endpos = findEND(zip, endbuf)) == -1)
        return -1; /* no END header or system error */

    if (endpos == 0) return 0;  /* only END header present */

    freeCEN(zip);
   /* Get position and length of central directory */
    cenlen = ENDSIZ(endbuf);
    cenoff = ENDOFF(endbuf);
    total  = ENDTOT(endbuf);
    if (cenlen == ZIP64_MAGICVAL || cenoff == ZIP64_MAGICVAL ||
        total == ZIP64_MAGICCOUNT) {
        unsigned char end64buf[ZIP64_ENDHDR];
        if ((end64pos = findEND64(zip, end64buf, endpos)) != -1) {
            cenlen = ZIP64_ENDSIZ(end64buf);
            cenoff = ZIP64_ENDOFF(end64buf);
            total = (jint)ZIP64_ENDTOT(end64buf);
            endpos = end64pos;
            endhdrlen = ZIP64_ENDHDR;
        }
    }

    if (cenlen > endpos) {
        ZIP_FORMAT_ERROR("invalid END header (bad central directory size)");
    }
    cenpos = endpos - cenlen;

    /* Get position of first local file (LOC) header, taking into
     * account that there may be a stub prefixed to the zip file. */
    zip->locpos = cenpos - cenoff;
    if (zip->locpos < 0) {
        ZIP_FORMAT_ERROR("invalid END header (bad central directory offset)");
    }
#ifdef USE_MMAP
    if (zip->usemmap) {
      /* On Solaris & Linux prior to JDK 6, we used to mmap the whole jar file to
       * read the jar file contents. However, this greatly increased the perceived
       * footprint numbers because the mmap'ed pages were adding into the totals shown
       * by 'ps' and 'top'. We switched to mmaping only the central directory of jar
       * file while calling 'read' to read the rest of jar file. Here are a list of
       * reasons apart from above of why we are doing so:
       * 1. Greatly reduces mmap overhead after startup complete;
       * 2. Avoids dual path code maintainance;
       * 3. Greatly reduces risk of address space (not virtual memory) exhaustion.
       */
        if (pagesize == 0) {
            pagesize = (jlong)sysconf(_SC_PAGESIZE);
            if (pagesize == 0) goto Catch;
        }
        if (cenpos > pagesize) {
            offset = cenpos & ~(pagesize - 1);
        } else {
            offset = 0;
        }
        /* When we are not calling recursively, knownTotal is -1. */
        if (knownTotal == -1) {
            void* mappedAddr;
            /* Mmap the CEN and END part only. We have to figure
               out the page size in order to make offset to be multiples of
               page size.
            */
            zip->mlen = cenpos - offset + cenlen + endhdrlen;
            zip->offset = offset;
            mappedAddr = mmap64(0, zip->mlen, PROT_READ, MAP_SHARED, zip->zfd, (off64_t) offset);
            zip->maddr = (mappedAddr == (void*) MAP_FAILED) ? NULL :
                (unsigned char*)mappedAddr;

            if (zip->maddr == NULL) {
                jio_fprintf(stderr, "mmap failed for CEN and END part of zip file\n");
                goto Catch;
            }
        }
        cenbuf = zip->maddr + cenpos - offset;
    } else
#endif
    {
        if ((cenbuf = malloc((size_t) cenlen)) == NULL ||
            (readFullyAt(zip->zfd, cenbuf, cenlen, cenpos) == -1))
        goto Catch;
    }

    cenend = cenbuf + cenlen;

    /* Initialize zip file data structures based on the total number
     * of central directory entries as stored in ENDTOT.  Since this
     * is a 2-byte field, but we (and other zip implementations)
     * support approx. 2**31 entries, we do not trust ENDTOT, but
     * treat it only as a strong hint.  When we call ourselves
     * recursively, knownTotal will have the "true" value.
     *
     * Keep this path alive even with the Zip64 END support added, just
     * for zip files that have more than 0xffff entries but don't have
     * the Zip64 enabled.
     */
    total = (knownTotal != -1) ? knownTotal : total;
    entries  = zip->entries  = calloc(total, sizeof(entries[0]));
    tablelen = zip->tablelen = ((total/2) | 1); // Odd -> fewer collisions
    table    = zip->table    = malloc(tablelen * sizeof(table[0]));
    /* According to ISO C it is perfectly legal for malloc to return zero
     * if called with a zero argument. We check this for 'entries' but not
     * for 'table' because 'tablelen' can't be zero (see computation above). */
    if ((entries == NULL && total != 0) || table == NULL) goto Catch;
    for (j = 0; j < tablelen; j++)
        table[j] = ZIP_ENDCHAIN;

    /* Iterate through the entries in the central directory */
    for (i = 0, cp = cenbuf; cp <= cenend - CENHDR; i++, cp += CENSIZE(cp)) {
        /* Following are unsigned 16-bit */
        jint method, nlen;
        unsigned int hsh;

        if (i >= total) {
            /* This will only happen if the zip file has an incorrect
             * ENDTOT field, which usually means it contains more than
             * 65535 entries. */
            cenpos = readCEN(zip, countCENHeaders(cenbuf, cenend));
            goto Finally;
        }

        method = CENHOW(cp);
        nlen   = CENNAM(cp);

        if (!CENSIG_AT(cp)) {
            ZIP_FORMAT_ERROR("invalid CEN header (bad signature)");
        }
        if (CENFLG(cp) & 1) {
            ZIP_FORMAT_ERROR("invalid CEN header (encrypted entry)");
        }
        if (method != STORED && method != DEFLATED) {
            ZIP_FORMAT_ERROR("invalid CEN header (bad compression method)");
        }
        if (cp + CENHDR + nlen > cenend) {
            ZIP_FORMAT_ERROR("invalid CEN header (bad header size)");
        }
        /* if the entry is metadata add it to our metadata names */
        if (isMetaName((char *)cp+CENHDR, nlen))
            if (addMetaName(zip, (char *)cp+CENHDR, nlen) != 0)
                goto Catch;

        /* Record the CEN offset and the name hash in our hash cell. */
        entries[i].cenpos = cenpos + (cp - cenbuf);
        entries[i].hash = hashN((char *)cp+CENHDR, nlen);

        /* Add the entry to the hash table */
        hsh = entries[i].hash % tablelen;
        entries[i].next = table[hsh];
        table[hsh] = i;
    }
    if (cp != cenend) {
        ZIP_FORMAT_ERROR("invalid CEN header (bad header size)");
    }
    zip->total = i;
    goto Finally;

 Catch:
    freeCEN(zip);
    cenpos = -1;

 Finally:
#ifdef USE_MMAP
    if (!zip->usemmap)
#endif
        free(cenbuf);

    return cenpos;
}

/*
 * Opens a zip file with the specified mode. Returns the jzfile object
 * or NULL if an error occurred. If a zip error occurred then *pmsg will
 * be set to the error message text if pmsg != 0. Otherwise, *pmsg will be
 * set to NULL. Caller is responsible to free the error message.
 */
jzfile *
ZIP_Open_Generic(const char *name, char **pmsg, int mode, jlong lastModified)
{
    jzfile *zip = NULL;

    /* Clear zip error message */
    if (pmsg != NULL) {
        *pmsg = NULL;
    }

    zip = ZIP_Get_From_Cache(name, pmsg, lastModified);

    if (zip == NULL && pmsg != NULL && *pmsg == NULL) {
        ZFILE zfd = ZFILE_Open(name, mode);
        zip = ZIP_Put_In_Cache(name, zfd, pmsg, lastModified);
    }
    return zip;
}

/*
 * Returns the jzfile corresponding to the given file name from the cache of
 * zip files, or NULL if the file is not in the cache.  If the name is longer
 * than PATH_MAX or a zip error occurred then *pmsg will be set to the error
 * message text if pmsg != 0. Otherwise, *pmsg will be set to NULL. Caller
 * is responsible to free the error message.
 */
jzfile *
ZIP_Get_From_Cache(const char *name, char **pmsg, jlong lastModified)
{
    char buf[PATH_MAX];
    jzfile *zip;

    if (InitializeZip()) {
        return NULL;
    }

    /* Clear zip error message */
    if (pmsg != 0) {
        *pmsg = NULL;
    }

    if (strlen(name) >= PATH_MAX) {
        if (pmsg) {
            *pmsg = strdup("zip file name too long");
        }
        return NULL;
    }
    strcpy(buf, name);
    JVM_NativePath(buf);
    name = buf;

    MLOCK(zfiles_lock);
    for (zip = zfiles; zip != NULL; zip = zip->next) {
        if (strcmp(name, zip->name) == 0
            && (zip->lastModified == lastModified || zip->lastModified == 0)
            && zip->refs < MAXREFS) {
            zip->refs++;
            break;
        }
    }
    MUNLOCK(zfiles_lock);
    return zip;
}

/*
 * Reads data from the given file descriptor to create a jzfile, puts the
 * jzfile in a cache, and returns that jzfile.  Returns NULL in case of error.
 * If a zip error occurs, then *pmsg will be set to the error message text if
 * pmsg != 0. Otherwise, *pmsg will be set to NULL. Caller is responsible to
 * free the error message.
 */

jzfile *
ZIP_Put_In_Cache(const char *name, ZFILE zfd, char **pmsg, jlong lastModified)
{
    return ZIP_Put_In_Cache0(name, zfd, pmsg, lastModified, JNI_TRUE);
}

jzfile *
ZIP_Put_In_Cache0(const char *name, ZFILE zfd, char **pmsg, jlong lastModified,
                 jboolean usemmap)
{
    char errbuf[256];
    jlong len;
    jzfile *zip;

    if ((zip = allocZip(name)) == NULL) {
        return NULL;
    }

#ifdef USE_MMAP
    zip->usemmap = usemmap;
#endif
    zip->refs = 1;
    zip->lastModified = lastModified;

    if (zfd == -1) {
        if (pmsg && getLastErrorString(errbuf, sizeof(errbuf)) > 0)
            *pmsg = strdup(errbuf);
        freeZip(zip);
        return NULL;
    }

    // Assumption, zfd refers to start of file. Trivially, reuse errbuf.
    if (readFully(zfd, errbuf, 4) != -1) {  // errors will be handled later
        zip->locsig = LOCSIG_AT(errbuf) ? JNI_TRUE : JNI_FALSE;
    }

    len = zip->len = IO_Lseek(zfd, 0, SEEK_END);
    if (len <= 0) {
        if (len == 0) { /* zip file is empty */
            if (pmsg) {
                *pmsg = strdup("zip file is empty");
            }
        } else { /* error */
            if (pmsg && getLastErrorString(errbuf, sizeof(errbuf)) > 0)
                *pmsg = strdup(errbuf);
        }
        ZFILE_Close(zfd);
        freeZip(zip);
        return NULL;
    }

    zip->zfd = zfd;
    if (readCEN(zip, -1) < 0) {
        /* An error occurred while trying to read the zip file */
        if (pmsg != 0) {
            /* Set the zip error message */
            if (zip->msg != NULL)
                *pmsg = strdup(zip->msg);
        }
        freeZip(zip);
        return NULL;
    }
    MLOCK(zfiles_lock);
    zip->next = zfiles;
    zfiles = zip;
    MUNLOCK(zfiles_lock);

    return zip;
}

/*
 * Opens a zip file for reading. Returns the jzfile object or NULL
 * if an error occurred. If a zip error occurred then *msg will be
 * set to the error message text if msg != 0. Otherwise, *msg will be
 * set to NULL. Caller doesn't need to free the error message.
 */
JNIEXPORT jzfile *
ZIP_Open(const char *name, char **pmsg)
{
    jzfile *file = ZIP_Open_Generic(name, pmsg, O_RDONLY, 0);
    if (file == NULL && pmsg != NULL && *pmsg != NULL) {
        free(*pmsg);
        *pmsg = "Zip file open error";
    }
    return file;
}

/*
 * Closes the specified zip file object.
 */
JNIEXPORT void
ZIP_Close(jzfile *zip)
{
    MLOCK(zfiles_lock);
    if (--zip->refs > 0) {
        /* Still more references so just return */
        MUNLOCK(zfiles_lock);
        return;
    }
    /* No other references so close the file and remove from list */
    if (zfiles == zip) {
        zfiles = zfiles->next;
    } else {
        jzfile *zp;
        for (zp = zfiles; zp->next != 0; zp = zp->next) {
            if (zp->next == zip) {
                zp->next = zip->next;
                break;
            }
        }
    }
    MUNLOCK(zfiles_lock);
    freeZip(zip);
    return;
}

/* Empirically, most CEN headers are smaller than this. */
#define AMPLE_CEN_HEADER_SIZE 160

/* A good buffer size when we want to read CEN headers sequentially. */
#define CENCACHE_PAGESIZE 8192

static char *
readCENHeader(jzfile *zip, jlong cenpos, jint bufsize)
{
    jint censize;
    ZFILE zfd = zip->zfd;
    char *cen;
    if (bufsize > zip->len - cenpos)
        bufsize = (jint)(zip->len - cenpos);
    if ((cen = malloc(bufsize)) == NULL)       goto Catch;
    if (readFullyAt(zfd, cen, bufsize, cenpos) == -1)     goto Catch;
    censize = CENSIZE(cen);
    if (censize <= bufsize) return cen;
    if ((cen = realloc(cen, censize)) == NULL)              goto Catch;
    if (readFully(zfd, cen+bufsize, censize-bufsize) == -1) goto Catch;
    return cen;

 Catch:
    free(cen);
    return NULL;
}

static char *
sequentialAccessReadCENHeader(jzfile *zip, jlong cenpos)
{
    cencache *cache = &zip->cencache;
    char *cen;
    if (cache->data != NULL
        && (cenpos >= cache->pos)
        && (cenpos + CENHDR <= cache->pos + CENCACHE_PAGESIZE))
    {
        cen = cache->data + cenpos - cache->pos;
        if (cenpos + CENSIZE(cen) <= cache->pos + CENCACHE_PAGESIZE)
            /* A cache hit */
            return cen;
    }

    if ((cen = readCENHeader(zip, cenpos, CENCACHE_PAGESIZE)) == NULL)
        return NULL;
    free(cache->data);
    cache->data = cen;
    cache->pos  = cenpos;
    return cen;
}

typedef enum { ACCESS_RANDOM, ACCESS_SEQUENTIAL } AccessHint;

/*
 * Return a new initialized jzentry corresponding to a given hash cell.
 * In case of error, returns NULL.
 * We already sanity-checked all the CEN headers for ZIP format errors
 * in readCEN(), so we don't check them again here.
 * The ZIP lock should be held here.
 */
static jzentry *
newEntry(jzfile *zip, jzcell *zc, AccessHint accessHint)
{
    jlong locoff;
    jint nlen, elen, clen;
    jzentry *ze;
    char *cen;

    if ((ze = (jzentry *) malloc(sizeof(jzentry))) == NULL) return NULL;
    ze->name    = NULL;
    ze->extra   = NULL;
    ze->comment = NULL;

#ifdef USE_MMAP
    if (zip->usemmap) {
        cen = (char*) zip->maddr + zc->cenpos - zip->offset;
    } else
#endif
    {
        if (accessHint == ACCESS_RANDOM)
            cen = readCENHeader(zip, zc->cenpos, AMPLE_CEN_HEADER_SIZE);
        else
            cen = sequentialAccessReadCENHeader(zip, zc->cenpos);
        if (cen == NULL) goto Catch;
    }

    nlen      = CENNAM(cen);
    elen      = CENEXT(cen);
    clen      = CENCOM(cen);
    ze->time  = CENTIM(cen);
    ze->size  = CENLEN(cen);
    ze->csize = (CENHOW(cen) == STORED) ? 0 : CENSIZ(cen);
    ze->crc   = CENCRC(cen);
    locoff    = CENOFF(cen);
    ze->pos   = -(zip->locpos + locoff);
    ze->flag  = CENFLG(cen);

    if ((ze->name = malloc(nlen + 1)) == NULL) goto Catch;
    memcpy(ze->name, cen + CENHDR, nlen);
    ze->name[nlen] = '\0';
    ze->nlen = nlen;
    if (elen > 0) {
        char *extra = cen + CENHDR + nlen;

        /* This entry has "extra" data */
        if ((ze->extra = malloc(elen + 2)) == NULL) goto Catch;
        ze->extra[0] = (unsigned char) elen;
        ze->extra[1] = (unsigned char) (elen >> 8);
        memcpy(ze->extra+2, extra, elen);
        if (ze->csize == ZIP64_MAGICVAL || ze->size == ZIP64_MAGICVAL ||
            locoff == ZIP64_MAGICVAL) {
            jint off = 0;
            while ((off + 4) < elen) {    // spec: HeaderID+DataSize+Data
                jint sz = SH(extra, off + 2);
                if (SH(extra, off) == ZIP64_EXTID) {
                    off += 4;
                    if (ze->size == ZIP64_MAGICVAL) {
                        // if invalid zip64 extra fields, just skip
                        if (sz < 8 || (off + 8) > elen)
                            break;
                        ze->size = LL(extra, off);
                        sz -= 8;
                        off += 8;
                    }
                    if (ze->csize == ZIP64_MAGICVAL) {
                        if (sz < 8 || (off + 8) > elen)
                            break;
                        ze->csize = LL(extra, off);
                        sz -= 8;
                        off += 8;
                    }
                    if (locoff == ZIP64_MAGICVAL) {
                        if (sz < 8 || (off + 8) > elen)
                            break;
                        ze->pos = -(zip->locpos +  LL(extra, off));
                        sz -= 8;
                        off += 8;
                    }
                    break;
                }
                off += (sz + 4);
            }
        }
    }

    if (clen > 0) {
        /* This entry has a comment */
        if ((ze->comment = malloc(clen + 1)) == NULL) goto Catch;
        memcpy(ze->comment, cen + CENHDR + nlen + elen, clen);
        ze->comment[clen] = '\0';
    }
    goto Finally;

 Catch:
    free(ze->name);
    free(ze->extra);
    free(ze->comment);
    free(ze);
    ze = NULL;

 Finally:
#ifdef USE_MMAP
    if (!zip->usemmap)
#endif
        if (cen != NULL && accessHint == ACCESS_RANDOM) free(cen);
    return ze;
}

/*
 * Free the given jzentry.
 * In fact we maintain a one-entry cache of the most recently used
 * jzentry for each zip.  This optimizes a common access pattern.
 */

void
ZIP_FreeEntry(jzfile *jz, jzentry *ze)
{
    jzentry *last;
    ZIP_Lock(jz);
    last = jz->cache;
    jz->cache = ze;
    ZIP_Unlock(jz);
    if (last != NULL) {
        /* Free the previously cached jzentry */
        free(last->name);
        if (last->extra)   free(last->extra);
        if (last->comment) free(last->comment);
        free(last);
    }
}

/*
 * Returns the zip entry corresponding to the specified name, or
 * NULL if not found.
 */
jzentry *
ZIP_GetEntry(jzfile *zip, char *name, jint ulen)
{
    if (ulen == 0) {
        return ZIP_GetEntry2(zip, name, (jint)strlen(name), JNI_FALSE);
    }
    return ZIP_GetEntry2(zip, name, ulen, JNI_TRUE);
}

jboolean equals(char* name1, int len1, char* name2, int len2) {
    if (len1 != len2) {
        return JNI_FALSE;
    }
    while (len1-- > 0) {
        if (*name1++ != *name2++) {
            return JNI_FALSE;
        }
    }
    return JNI_TRUE;
}

/*
 * Returns the zip entry corresponding to the specified name, or
 * NULL if not found.
 * This method supports embedded null character in "name", use ulen
 * for the length of "name".
 */
jzentry *
ZIP_GetEntry2(jzfile *zip, char *name, jint ulen, jboolean addSlash)
{
    unsigned int hsh = hashN(name, ulen);
    jint idx;
    jzentry *ze = 0;

    ZIP_Lock(zip);
    if (zip->total == 0) {
        goto Finally;
    }

    idx = zip->table[hsh % zip->tablelen];

    /*
     * This while loop is an optimization where a double lookup
     * for name and name+/ is being performed. The name char
     * array has enough room at the end to try again with a
     * slash appended if the first table lookup does not succeed.
     */
    while(1) {

        /* Check the cached entry first */
        ze = zip->cache;
        if (ze && equals(ze->name, ze->nlen, name, ulen)) {
            /* Cache hit!  Remove and return the cached entry. */
            zip->cache = 0;
            ZIP_Unlock(zip);
            return ze;
        }
        ze = 0;

        /*
         * Search down the target hash chain for a cell whose
         * 32 bit hash matches the hashed name.
         */
        while (idx != ZIP_ENDCHAIN) {
            jzcell *zc = &zip->entries[idx];

            if (zc->hash == hsh) {
                /*
                 * OK, we've found a ZIP entry whose 32 bit hashcode
                 * matches the name we're looking for.  Try to read
                 * its entry information from the CEN.  If the CEN
                 * name matches the name we're looking for, we're
                 * done.
                 * If the names don't match (which should be very rare)
                 * we keep searching.
                 */
                ze = newEntry(zip, zc, ACCESS_RANDOM);
                if (ze && equals(ze->name, ze->nlen, name, ulen)) {
                    break;
                }
                if (ze != 0) {
                    /* We need to release the lock across the free call */
                    ZIP_Unlock(zip);
                    ZIP_FreeEntry(zip, ze);
                    ZIP_Lock(zip);
                }
                ze = 0;
            }
            idx = zc->next;
        }

        /* Entry found, return it */
        if (ze != 0) {
            break;
        }

        /* If no need to try appending slash, we are done */
        if (!addSlash) {
            break;
        }

        /* Slash is already there? */
        if (ulen > 0 && name[ulen - 1] == '/') {
            break;
        }

        /* Add slash and try once more */
        name[ulen++] = '/';
        name[ulen] = '\0';
        hsh = hash_append(hsh, '/');
        idx = zip->table[hsh % zip->tablelen];
        addSlash = JNI_FALSE;
    }

Finally:
    ZIP_Unlock(zip);
    return ze;
}

/*
 * Returns the n'th (starting at zero) zip file entry, or NULL if the
 * specified index was out of range.
 */
JNIEXPORT jzentry *
ZIP_GetNextEntry(jzfile *zip, jint n)
{
    jzentry *result;
    if (n < 0 || n >= zip->total) {
        return 0;
    }
    ZIP_Lock(zip);
    result = newEntry(zip, &zip->entries[n], ACCESS_SEQUENTIAL);
    ZIP_Unlock(zip);
    return result;
}

/*
 * Locks the specified zip file for reading.
 */
void
ZIP_Lock(jzfile *zip)
{
    MLOCK(zip->lock);
}

/*
 * Unlocks the specified zip file.
 */
void
ZIP_Unlock(jzfile *zip)
{
    MUNLOCK(zip->lock);
}

/*
 * Returns the offset of the entry data within the zip file.
 * Returns -1 if an error occurred, in which case zip->msg will
 * contain the error text.
 */
jlong
ZIP_GetEntryDataOffset(jzfile *zip, jzentry *entry)
{
    /* The Zip file spec explicitly allows the LOC extra data size to
     * be different from the CEN extra data size, although the JDK
     * never creates such zip files.  Since we cannot trust the CEN
     * extra data size, we need to read the LOC to determine the entry
     * data offset.  We do this lazily to avoid touching the virtual
     * memory page containing the LOC when initializing jzentry
     * objects.  (This speeds up javac by a factor of 10 when the JDK
     * is installed on a very slow filesystem.)
     */
    if (entry->pos <= 0) {
        unsigned char loc[LOCHDR];
        if (readFullyAt(zip->zfd, loc, LOCHDR, -(entry->pos)) == -1) {
            zip->msg = "error reading zip file";
            return -1;
        }
        if (!LOCSIG_AT(loc)) {
            zip->msg = "invalid LOC header (bad signature)";
            return -1;
        }
        entry->pos = (- entry->pos) + LOCHDR + LOCNAM(loc) + LOCEXT(loc);
    }
    return entry->pos;
}

/*
 * Reads bytes from the specified zip entry. Assumes that the zip
 * file had been previously locked with ZIP_Lock(). Returns the
 * number of bytes read, or -1 if an error occurred. If zip->msg != 0
 * then a zip error occurred and zip->msg contains the error text.
 *
 * The current implementation does not support reading an entry that
 * has the size bigger than 2**32 bytes in ONE invocation.
 */
jint
ZIP_Read(jzfile *zip, jzentry *entry, jlong pos, void *buf, jint len)
{
    jlong entry_size;
    jlong start;

    if (zip == 0) {
        return -1;
    }

    /* Clear previous zip error */
    zip->msg = NULL;

    if (entry == 0) {
        zip->msg = "ZIP_Read: jzentry is NULL";
        return -1;
    }

    entry_size = (entry->csize != 0) ? entry->csize : entry->size;

    /* Check specified position */
    if (pos < 0 || pos > entry_size - 1) {
        zip->msg = "ZIP_Read: specified offset out of range";
        return -1;
    }

    /* Check specified length */
    if (len <= 0)
        return 0;
    if (len > entry_size - pos)
        len = (jint)(entry_size - pos);

    /* Get file offset to start reading data */
    start = ZIP_GetEntryDataOffset(zip, entry);
    if (start < 0)
        return -1;
    start += pos;

    if (start + len > zip->len) {
        zip->msg = "ZIP_Read: corrupt zip file: invalid entry size";
        return -1;
    }

    if (readFullyAt(zip->zfd, buf, len, start) == -1) {
        zip->msg = "ZIP_Read: error reading zip file";
        return -1;
    }
    return len;
}


/* The maximum size of a stack-allocated buffer.
 */
#define BUF_SIZE 4096

/*
 * This function is used by the runtime system to load compressed entries
 * from ZIP/JAR files specified in the class path. It is defined here
 * so that it can be dynamically loaded by the runtime if the zip library
 * is found.
 *
 * The current implementation does not support reading an entry that
 * has the size bigger than 2**32 bytes in ONE invocation.
 */
jboolean
InflateFully(jzfile *zip, jzentry *entry, void *buf, char **msg)
{
    z_stream strm;
    char tmp[BUF_SIZE];
    jlong pos = 0;
    jlong count = entry->csize;

    *msg = 0; /* Reset error message */

    if (count == 0) {
        *msg = "inflateFully: entry not compressed";
        return JNI_FALSE;
    }

    memset(&strm, 0, sizeof(z_stream));
    if (inflateInit2(&strm, -MAX_WBITS) != Z_OK) {
        *msg = strm.msg;
        return JNI_FALSE;
    }

    strm.next_out = buf;
    strm.avail_out = (uInt)entry->size;

    while (count > 0) {
        jint n = count > (jlong)sizeof(tmp) ? (jint)sizeof(tmp) : (jint)count;
        ZIP_Lock(zip);
        n = ZIP_Read(zip, entry, pos, tmp, n);
        ZIP_Unlock(zip);
        if (n <= 0) {
            if (n == 0) {
                *msg = "inflateFully: Unexpected end of file";
            }
            inflateEnd(&strm);
            return JNI_FALSE;
        }
        pos += n;
        count -= n;
        strm.next_in = (Bytef *)tmp;
        strm.avail_in = n;
        do {
            switch (inflate(&strm, Z_PARTIAL_FLUSH)) {
            case Z_OK:
                break;
            case Z_STREAM_END:
                if (count != 0 || strm.total_out != (uInt)entry->size) {
                    *msg = "inflateFully: Unexpected end of stream";
                    inflateEnd(&strm);
                    return JNI_FALSE;
                }
                break;
            default:
                break;
            }
        } while (strm.avail_in > 0);
    }

    inflateEnd(&strm);
    return JNI_TRUE;
}

/*
 * The current implementation does not support reading an entry that
 * has the size bigger than 2**32 bytes in ONE invocation.
 */
JNIEXPORT jzentry *
ZIP_FindEntry(jzfile *zip, char *name, jint *sizeP, jint *nameLenP)
{
    jzentry *entry = ZIP_GetEntry(zip, name, 0);
    if (entry) {
        *sizeP = (jint)entry->size;
        *nameLenP = (jint)strlen(entry->name);
    }
    return entry;
}

/*
 * Reads a zip file entry into the specified byte array
 * When the method completes, it releases the jzentry.
 * Note: this is called from the separately delivered VM (hotspot/classic)
 * so we have to be careful to maintain the expected behaviour.
 */
JNIEXPORT jboolean
ZIP_ReadEntry(jzfile *zip, jzentry *entry, unsigned char *buf, char *entryname)
{
    char *msg;
    char tmpbuf[1024];

    if (entry == 0) {
        jio_fprintf(stderr, "jzentry was invalid");
        return JNI_FALSE;
    }

    strcpy(entryname, entry->name);
    if (entry->csize == 0) {
        /* Entry is stored */
        jlong pos = 0;
        jlong size = entry->size;
        while (pos < size) {
            jint n;
            jlong limit = ((((jlong) 1) << 31) - 1);
            jint count = (size - pos < limit) ?
                /* These casts suppress a VC++ Internal Compiler Error */
                (jint) (size - pos) :
                (jint) limit;
            ZIP_Lock(zip);
            n = ZIP_Read(zip, entry, pos, buf, count);
            msg = zip->msg;
            ZIP_Unlock(zip);
            if (n == -1) {
                if (msg == 0) {
                    getErrorString(errno, tmpbuf, sizeof(tmpbuf));
                    msg = tmpbuf;
                }
                jio_fprintf(stderr, "%s: %s\n", zip->name, msg);
                return JNI_FALSE;
            }
            buf += n;
            pos += n;
        }
    } else {
        /* Entry is compressed */
        int ok = InflateFully(zip, entry, buf, &msg);
        if (!ok) {
            if ((msg == NULL) || (*msg == 0)) {
                msg = zip->msg;
            }
            if (msg == 0) {
                getErrorString(errno, tmpbuf, sizeof(tmpbuf));
                msg = tmpbuf;
            }
            jio_fprintf(stderr, "%s: %s\n", zip->name, msg);
            return JNI_FALSE;
        }
    }

    ZIP_FreeEntry(zip, entry);

    return JNI_TRUE;
}

JNIEXPORT jboolean
ZIP_InflateFully(void *inBuf, jlong inLen, void *outBuf, jlong outLen, char **pmsg)
{
    z_stream strm;
    int i = 0;
    memset(&strm, 0, sizeof(z_stream));

    *pmsg = 0; /* Reset error message */

    if (inflateInit2(&strm, MAX_WBITS) != Z_OK) {
        *pmsg = strm.msg;
        return JNI_FALSE;
    }

    strm.next_out = (Bytef *) outBuf;
    strm.avail_out = (uInt)outLen;
    strm.next_in = (Bytef *) inBuf;
    strm.avail_in = (uInt)inLen;

    do {
        switch (inflate(&strm, Z_PARTIAL_FLUSH)) {
            case Z_OK:
                break;
            case Z_STREAM_END:
                if (strm.total_out != (uInt)outLen) {
                    *pmsg = "INFLATER_inflateFully: Unexpected end of stream";
                    inflateEnd(&strm);
                    return JNI_FALSE;
                }
                break;
            case Z_DATA_ERROR:
                *pmsg = "INFLATER_inflateFully: Compressed data corrupted";
                inflateEnd(&strm);
                return JNI_FALSE;
            case Z_MEM_ERROR:
                *pmsg = "INFLATER_inflateFully: out of memory";
                inflateEnd(&strm);
                return JNI_FALSE;
            default:
                *pmsg = "INFLATER_inflateFully: internal error";
                inflateEnd(&strm);
                return JNI_FALSE;
        }
    } while (strm.avail_in > 0);

    inflateEnd(&strm);
    return JNI_TRUE;
}

static voidpf tracking_zlib_alloc(voidpf opaque, uInt items, uInt size) {
  size_t* needed = (size_t*) opaque;
  *needed += (size_t) items * (size_t) size;
  return (voidpf) calloc((size_t) items, (size_t) size);
}

static void tracking_zlib_free(voidpf opaque, voidpf address) {
  free((void*) address);
}

static voidpf zlib_block_alloc(voidpf opaque, uInt items, uInt size) {
  char** range = (char**) opaque;
  voidpf result = NULL;
  size_t needed = (size_t) items * (size_t) size;

  if (range[1] - range[0] >= (ptrdiff_t) needed) {
    result = (voidpf) range[0];
    range[0] += needed;
  }

  return result;
}

static void zlib_block_free(voidpf opaque, voidpf address) {
  /* Nothing to do. */
}

static char const* deflateInit2Wrapper(z_stream* strm, int level) {
  int err = deflateInit2(strm, level >= 0 && level <= 9 ? level : Z_DEFAULT_COMPRESSION,
                         Z_DEFLATED, 31, 8, Z_DEFAULT_STRATEGY);
  if (err == Z_MEM_ERROR) {
    return "Out of memory in deflateInit2";
  }

  if (err != Z_OK) {
    return "Internal error in deflateInit2";
  }

  return NULL;
}

JNIEXPORT char const*
ZIP_GZip_InitParams(size_t inLen, size_t* outLen, size_t* tmpLen, int level) {
  z_stream strm;
  *tmpLen = 0;
  char const* errorMsg;

  memset(&strm, 0, sizeof(z_stream));
  strm.zalloc = tracking_zlib_alloc;
  strm.zfree = tracking_zlib_free;
  strm.opaque = (voidpf) tmpLen;

  errorMsg = deflateInit2Wrapper(&strm, level);

  if (errorMsg == NULL) {
    *outLen = (size_t) deflateBound(&strm, (uLong) inLen);
    deflateEnd(&strm);
  }

  return errorMsg;
}

JNIEXPORT size_t
ZIP_GZip_Fully(char* inBuf, size_t inLen, char* outBuf, size_t outLen, char* tmp, size_t tmpLen,
               int level, char* comment, char const** pmsg) {
  z_stream strm;
  gz_header hdr;
  int err;
  char* block[] = {tmp, tmpLen + tmp};
  size_t result = 0;

  memset(&strm, 0, sizeof(z_stream));
  strm.zalloc = zlib_block_alloc;
  strm.zfree = zlib_block_free;
  strm.opaque = (voidpf) block;

  *pmsg = deflateInit2Wrapper(&strm, level);

  if (*pmsg == NULL) {
    strm.next_out = (Bytef *) outBuf;
    strm.avail_out = (uInt) outLen;
    strm.next_in = (Bytef *) inBuf;
    strm.avail_in = (uInt) inLen;

    if (comment != NULL) {
      memset(&hdr, 0, sizeof(hdr));
      hdr.comment = (Bytef*) comment;
      deflateSetHeader(&strm, &hdr);
    }

    err = deflate(&strm, Z_FINISH);

    if (err == Z_OK || err == Z_BUF_ERROR) {
      *pmsg = "Buffer too small";
    } else if (err != Z_STREAM_END) {
      *pmsg = "Intern deflate error";
    } else {
      result = (size_t) strm.total_out;
    }

    deflateEnd(&strm);
  }

  return result;
}
