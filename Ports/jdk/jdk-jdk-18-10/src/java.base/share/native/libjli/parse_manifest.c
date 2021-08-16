/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "jni.h"
#include "jli_util.h"

#include <zlib.h>
#include "manifest_info.h"

static char     *manifest;

static const char       *manifest_name = "META-INF/MANIFEST.MF";

/*
 * Inflate the manifest file (or any file for that matter).
 *
 *   fd:        File descriptor of the jar file.
 *   entry:     Contains the information necessary to perform the inflation
 *              (the compressed and uncompressed sizes and the offset in
 *              the file where the compressed data is located).
 *   size_out:  Returns the size of the inflated file.
 *
 * Upon success, it returns a pointer to a NUL-terminated malloc'd buffer
 * containing the inflated manifest file.  When the caller is done with it,
 * this buffer should be released by a call to free().  Upon failure,
 * returns NULL.
 */
static char *
inflate_file(int fd, zentry *entry, int *size_out)
{
    char        *in;
    char        *out;
    z_stream    zs;

    if (entry->csize == (size_t) -1 || entry->isize == (size_t) -1 )
        return (NULL);
    if (JLI_Lseek(fd, entry->offset, SEEK_SET) < (jlong)0)
        return (NULL);
    if ((in = malloc(entry->csize + 1)) == NULL)
        return (NULL);
    if ((size_t)(read(fd, in, (unsigned int)entry->csize)) != entry->csize) {
        free(in);
        return (NULL);
    }
    if (entry->how == STORED) {
        *(char *)((size_t)in + entry->csize) = '\0';
        if (size_out) {
            *size_out = (int)entry->csize;
        }
        return (in);
    } else if (entry->how == DEFLATED) {
        zs.zalloc = (alloc_func)Z_NULL;
        zs.zfree = (free_func)Z_NULL;
        zs.opaque = (voidpf)Z_NULL;
        zs.next_in = (Byte*)in;
        zs.avail_in = (uInt)entry->csize;
        if (inflateInit2(&zs, -MAX_WBITS) < 0) {
            free(in);
            return (NULL);
        }
        if ((out = malloc(entry->isize + 1)) == NULL) {
            free(in);
            return (NULL);
        }
        zs.next_out = (Byte*)out;
        zs.avail_out = (uInt)entry->isize;
        if (inflate(&zs, Z_PARTIAL_FLUSH) < 0) {
            free(in);
            free(out);
            return (NULL);
        }
        *(char *)((size_t)out + entry->isize) = '\0';
        free(in);
        if (inflateEnd(&zs) < 0) {
            free(out);
            return (NULL);
        }
        if (size_out) {
            *size_out = (int)entry->isize;
        }
        return (out);
    }
    free(in);
    return (NULL);
}

/*
 * Implementation notes:
 *
 * This is a zip format reader for seekable files, that tolerates
 * leading and trailing garbage, and tolerates having had internal
 * offsets adjusted for leading garbage (as with Info-Zip's zip -A).
 *
 * We find the end header by scanning backwards from the end of the
 * file for the end signature.  This may fail in the presence of
 * trailing garbage or a ZIP file comment that contains binary data.
 * Similarly, the ZIP64 end header may need to be located by scanning
 * backwards from the end header.  It may be misidentified, but this
 * is very unlikely to happen in practice without adversarial input.
 *
 * The zip file format is documented at:
 * https://www.pkware.com/documents/casestudies/APPNOTE.TXT
 *
 * TODO: more informative error messages
 */

/** Reads count bytes from fd at position pos into given buffer. */
static jboolean
readAt(int fd, jlong pos, unsigned int count, void *buf) {
    return (pos >= 0
            && JLI_Lseek(fd, pos, SEEK_SET) == pos
            && read(fd, buf, count) == (jlong) count);
}


/*
 * Tells whether given header values (obtained from either ZIP64 or
 * non-ZIP64 header) appear to be correct, by checking the first LOC
 * and CEN headers.
 */
static jboolean
is_valid_end_header(int fd, jlong endpos,
                    jlong censiz, jlong cenoff, jlong entries) {
    Byte cenhdr[CENHDR];
    Byte lochdr[LOCHDR];
    // Expected offset of the first central directory header
    jlong censtart = endpos - censiz;
    // Expected position within the file that offsets are relative to
    jlong base_offset = endpos - (censiz + cenoff);
    return censtart >= 0 && cenoff >= 0 &&
        (censiz == 0 ||
         // Validate first CEN and LOC header signatures.
         // Central directory must come directly before the end header.
         (readAt(fd, censtart, CENHDR, cenhdr)
          && CENSIG_AT(cenhdr)
          && readAt(fd, base_offset + CENOFF(cenhdr), LOCHDR, lochdr)
          && LOCSIG_AT(lochdr)
          && CENNAM(cenhdr) == LOCNAM(lochdr)));
}

/*
 * Tells whether p appears to be pointing at a valid ZIP64 end header.
 * Values censiz, cenoff, and entries are the corresponding values
 * from the non-ZIP64 end header.  We perform extra checks to avoid
 * misidentifying data from the last entry as a ZIP64 end header.
 */
static jboolean
is_zip64_endhdr(int fd, const Byte *p, jlong end64pos,
                jlong censiz, jlong cenoff, jlong entries) {
    if (ZIP64_ENDSIG_AT(p)) {
        jlong censiz64 = ZIP64_ENDSIZ(p);
        jlong cenoff64 = ZIP64_ENDOFF(p);
        jlong entries64 = ZIP64_ENDTOT(p);
        return (censiz64 == censiz || censiz == ZIP64_MAGICVAL)
            && (cenoff64 == cenoff || cenoff == ZIP64_MAGICVAL)
            && (entries64 == entries || entries == ZIP64_MAGICCOUNT)
            && is_valid_end_header(fd, end64pos, censiz64, cenoff64, entries64);
    }
    return JNI_FALSE;
}

/*
 * Given a non-ZIP64 end header located at endhdr and endpos, look for
 * an adjacent ZIP64 end header, finding the base offset and censtart
 * from the ZIP64 header if available, else from the non-ZIP64 header.
 * @return 0 if successful, -1 in case of failure
 */
static int
find_positions64(int fd, const Byte * const endhdr, const jlong endpos,
                 jlong* base_offset, jlong* censtart)
{
    jlong censiz = ENDSIZ(endhdr);
    jlong cenoff = ENDOFF(endhdr);
    jlong entries = ENDTOT(endhdr);
    jlong end64pos;
    Byte buf[ZIP64_ENDHDR + ZIP64_LOCHDR];
    if (censiz + cenoff != endpos
        && (end64pos = endpos - sizeof(buf)) >= (jlong)0
        && readAt(fd, end64pos, sizeof(buf), buf)
        && ZIP64_LOCSIG_AT(buf + ZIP64_ENDHDR)
        && (jlong) ZIP64_LOCDSK(buf + ZIP64_ENDHDR) == ENDDSK(endhdr)
        && (is_zip64_endhdr(fd, buf, end64pos, censiz, cenoff, entries)
            || // A variable sized "zip64 extensible data sector" ?
            ((end64pos = ZIP64_LOCOFF(buf + ZIP64_ENDHDR)) >= (jlong)0
             && readAt(fd, end64pos, ZIP64_ENDHDR, buf)
             && is_zip64_endhdr(fd, buf, end64pos, censiz, cenoff, entries)))
        ) {
        *censtart = end64pos - ZIP64_ENDSIZ(buf);
        *base_offset = *censtart - ZIP64_ENDOFF(buf);
    } else {
        if (!is_valid_end_header(fd, endpos, censiz, cenoff, entries))
            return -1;
        *censtart = endpos - censiz;
        *base_offset = *censtart - cenoff;
    }
    return 0;
}

/*
 * Finds the base offset and censtart of the zip file.
 *
 * @param fd file descriptor of the jar file
 * @param eb scratch buffer
 * @return 0 if successful, -1 in case of failure
 */
static int
find_positions(int fd, Byte *eb, jlong* base_offset, jlong* censtart)
{
    jlong   len;
    jlong   pos;
    jlong   flen;
    int     bytes;
    Byte    *cp;
    Byte    *endpos;
    Byte    *buffer;

    /*
     * 99.44% (or more) of the time, there will be no comment at the
     * end of the zip file.  Try reading just enough to read the END
     * record from the end of the file, at this time we should also
     * check to see if we have a ZIP64 archive.
     */
    if ((pos = JLI_Lseek(fd, -ENDHDR, SEEK_END)) < (jlong)0)
        return (-1);
    if (read(fd, eb, ENDHDR) < 0)
        return (-1);
    if (ENDSIG_AT(eb)) {
        return find_positions64(fd, eb, pos, base_offset, censtart);
    }

    /*
     * Shucky-Darn,... There is a comment at the end of the zip file.
     *
     * Allocate and fill a buffer with enough of the zip file
     * to meet the specification for a maximal comment length.
     */
    if ((flen = JLI_Lseek(fd, 0, SEEK_END)) < (jlong)0)
        return (-1);
    len = (flen < END_MAXLEN) ? flen : END_MAXLEN;
    if (JLI_Lseek(fd, -len, SEEK_END) < (jlong)0)
        return (-1);
    if ((buffer = malloc(END_MAXLEN)) == NULL)
        return (-1);

    /*
     * read() on windows takes an unsigned int for count. Casting len
     * to an unsigned int here is safe since it is guaranteed to be
     * less than END_MAXLEN.
     */
    if ((bytes = read(fd, buffer, (unsigned int)len)) < 0) {
        free(buffer);
        return (-1);
    }

    /*
     * Search backwards from the end of file stopping when the END header
     * signature is found.
     */
    endpos = &buffer[bytes];
    for (cp = &buffer[bytes - ENDHDR]; cp >= &buffer[0]; cp--)
        if (ENDSIG_AT(cp) && (cp + ENDHDR + ENDCOM(cp) == endpos)) {
            (void) memcpy(eb, cp, ENDHDR);
            free(buffer);
            pos = flen - (endpos - cp);
            return find_positions64(fd, eb, pos, base_offset, censtart);
        }
    free(buffer);
    return (-1);
}

#define BUFSIZE (3 * 65536 + CENHDR + SIGSIZ)
#define MINREAD 1024

/*
 * Locate the manifest file with the zip/jar file.
 *
 *      fd:     File descriptor of the jar file.
 *      entry:  To be populated with the information necessary to perform
 *              the inflation (the compressed and uncompressed sizes and
 *              the offset in the file where the compressed data is located).
 *
 * Returns zero upon success. Returns a negative value upon failure.
 *
 * The buffer for reading the Central Directory if the zip/jar file needs
 * to be large enough to accommodate the largest possible single record
 * and the signature of the next record which is:
 *
 *      3*2**16 + CENHDR + SIGSIZ
 *
 * Each of the three variable sized fields (name, comment and extension)
 * has a maximum possible size of 64k.
 *
 * Typically, only a small bit of this buffer is used with bytes shuffled
 * down to the beginning of the buffer.  It is one thing to allocate such
 * a large buffer and another thing to actually start faulting it in.
 *
 * In most cases, all that needs to be read are the first two entries in
 * a typical jar file (META-INF and META-INF/MANIFEST.MF). Keep this factoid
 * in mind when optimizing this code.
 */
static int
find_file(int fd, zentry *entry, const char *file_name)
{
    int     bytes;
    int     res;
    int     entry_size;
    int     read_size;

    /*
     * The (imaginary) position within the file relative to which
     * offsets within the zip file refer.  This is usually the
     * location of the first local header (the start of the zip data)
     * (which in turn is usually 0), but if the zip file has content
     * prepended, then it will be either 0 or the length of the
     * prepended content, depending on whether or not internal offsets
     * have been adjusted (via e.g. zip -A).  May be negative if
     * content is prepended, zip -A is run, then the prefix is
     * detached!
     */
    jlong   base_offset;

    /** The position within the file of the start of the central directory. */
    jlong   censtart;

    Byte    *p;
    Byte    *bp;
    Byte    *buffer;
    Byte    locbuf[LOCHDR];

    if ((buffer = (Byte*)malloc(BUFSIZE)) == NULL) {
        return(-1);
    }

    bp = buffer;

    if (find_positions(fd, bp, &base_offset, &censtart) == -1) {
        free(buffer);
        return -1;
    }
    if (JLI_Lseek(fd, censtart, SEEK_SET) < (jlong) 0) {
        free(buffer);
        return -1;
    }

    if ((bytes = read(fd, bp, MINREAD)) < 0) {
        free(buffer);
        return (-1);
    }
    p = bp;
    /*
     * Loop through the Central Directory Headers. Note that a valid zip/jar
     * must have an ENDHDR (with ENDSIG) after the Central Directory.
     */
    while (CENSIG_AT(p)) {

        /*
         * If a complete header isn't in the buffer, shift the contents
         * of the buffer down and refill the buffer.  Note that the check
         * for "bytes < CENHDR" must be made before the test for the entire
         * size of the header, because if bytes is less than CENHDR, the
         * actual size of the header can't be determined. The addition of
         * SIGSIZ guarantees that the next signature is also in the buffer
         * for proper loop termination.
         */
        if (bytes < CENHDR) {
            p = memmove(bp, p, bytes);
            if ((res = read(fd, bp + bytes, MINREAD)) <= 0) {
                free(buffer);
                return (-1);
            }
            bytes += res;
        }
        entry_size = CENHDR + CENNAM(p) + CENEXT(p) + CENCOM(p);
        if (bytes < entry_size + SIGSIZ) {
            if (p != bp)
                p = memmove(bp, p, bytes);
            read_size = entry_size - bytes + SIGSIZ;
            read_size = (read_size < MINREAD) ? MINREAD : read_size;
            if ((res = read(fd, bp + bytes,  read_size)) <= 0) {
                free(buffer);
                return (-1);
            }
            bytes += res;
        }

        /*
         * Check if the name is the droid we are looking for; the jar file
         * manifest.  If so, build the entry record from the data found in
         * the header located and return success.
         */
        if ((size_t)CENNAM(p) == JLI_StrLen(file_name) &&
          memcmp((p + CENHDR), file_name, JLI_StrLen(file_name)) == 0) {
            if (JLI_Lseek(fd, base_offset + CENOFF(p), SEEK_SET) < (jlong)0) {
                free(buffer);
                return (-1);
            }
            if (read(fd, locbuf, LOCHDR) < 0) {
                free(buffer);
                return (-1);
            }
            if (!LOCSIG_AT(locbuf)) {
                free(buffer);
                return (-1);
            }
            entry->isize = CENLEN(p);
            entry->csize = CENSIZ(p);
            entry->offset = base_offset + CENOFF(p) + LOCHDR +
                LOCNAM(locbuf) + LOCEXT(locbuf);
            entry->how = CENHOW(p);
            free(buffer);
            return (0);
        }

        /*
         * Point to the next entry and decrement the count of valid remaining
         * bytes.
         */
        bytes -= entry_size;
        p += entry_size;
    }
    free(buffer);
    return (-1);        /* Fell off the end the loop without a Manifest */
}

/*
 * Parse a Manifest file header entry into a distinct "name" and "value".
 * Continuation lines are joined into a single "value". The documented
 * syntax for a header entry is:
 *
 *      header: name ":" value
 *
 *      name: alphanum *headerchar
 *
 *      value: SPACE *otherchar newline *continuation
 *
 *      continuation: SPACE *otherchar newline
 *
 *      newline: CR LF | LF | CR (not followed by LF)
 *
 *      alphanum: {"A"-"Z"} | {"a"-"z"} | {"0"-"9"}
 *
 *      headerchar: alphanum | "-" | "_"
 *
 *      otherchar: any UTF-8 character except NUL, CR and LF
 *
 * Note that a manifest file may be composed of multiple sections,
 * each of which may contain multiple headers.
 *
 *      section: *header +newline
 *
 *      nonempty-section: +header +newline
 *
 * (Note that the point of "nonempty-section" is unclear, because it isn't
 * referenced elsewhere in the full specification for the Manifest file.)
 *
 * Arguments:
 *      lp      pointer to a character pointer which points to the start
 *              of a valid header.
 *      name    pointer to a character pointer which will be set to point
 *              to the name portion of the header (nul terminated).
 *      value   pointer to a character pointer which will be set to point
 *              to the value portion of the header (nul terminated).
 *
 * Returns:
 *    1 Successful parsing of an NV pair.  lp is updated to point to the
 *      next character after the terminating newline in the string
 *      representing the Manifest file. name and value are updated to
 *      point to the strings parsed.
 *    0 A valid end of section indicator was encountered.  lp, name, and
 *      value are not modified.
 *   -1 lp does not point to a valid header. Upon return, the values of
 *      lp, name, and value are undefined.
 */
static int
parse_nv_pair(char **lp, char **name, char **value)
{
    char    *nl;
    char    *cp;

    /*
     * End of the section - return 0. The end of section condition is
     * indicated by either encountering a blank line or the end of the
     * Manifest "string" (EOF).
     */
    if (**lp == '\0' || **lp == '\n' || **lp == '\r')
        return (0);

    /*
     * Getting to here, indicates that *lp points to an "otherchar".
     * Turn the "header" into a string on its own.
     */
    nl = JLI_StrPBrk(*lp, "\n\r");
    if (nl == NULL) {
        nl = JLI_StrChr(*lp, (int)'\0');
    } else {
        cp = nl;                        /* For merging continuation lines */
        if (*nl == '\r' && *(nl+1) == '\n')
            *nl++ = '\0';
        *nl++ = '\0';

        /*
         * Process any "continuation" line(s), by making them part of the
         * "header" line. Yes, I know that we are "undoing" the NULs we
         * just placed here, but continuation lines are the fairly rare
         * case, so we shouldn't unnecessarily complicate the code above.
         *
         * Note that an entire continuation line is processed each iteration
         * through the outer while loop.
         */
        while (*nl == ' ') {
            nl++;                       /* First character to be moved */
            while (*nl != '\n' && *nl != '\r' && *nl != '\0')
                *cp++ = *nl++;          /* Shift string */
            if (*nl == '\0')
                return (-1);            /* Error: newline required */
            *cp = '\0';
            if (*nl == '\r' && *(nl+1) == '\n')
                *nl++ = '\0';
            *nl++ = '\0';
        }
    }

    /*
     * Separate the name from the value;
     */
    cp = JLI_StrChr(*lp, (int)':');
    if (cp == NULL)
        return (-1);
    *cp++ = '\0';               /* The colon terminates the name */
    if (*cp != ' ')
        return (-1);
    *cp++ = '\0';               /* Eat the required space */
    *name = *lp;
    *value = cp;
    *lp = nl;
    return (1);
}

/*
 * Read the manifest from the specified jar file and fill in the manifest_info
 * structure with the information found within.
 *
 * Error returns are as follows:
 *    0 Success
 *   -1 Unable to open jarfile
 *   -2 Error accessing the manifest from within the jarfile (most likely
 *      a manifest is not present, or this isn't a valid zip/jar file).
 */
int
JLI_ParseManifest(char *jarfile, manifest_info *info)
{
    int     fd;
    zentry  entry;
    char    *lp;
    char    *name;
    char    *value;
    int     rc;
    char    *splashscreen_name = NULL;

    if ((fd = JLI_Open(jarfile, O_RDONLY
#ifdef O_LARGEFILE
        | O_LARGEFILE /* large file mode */
#endif
#ifdef O_BINARY
        | O_BINARY /* use binary mode on windows */
#endif
        )) == -1) {
        return (-1);
    }
    info->manifest_version = NULL;
    info->main_class = NULL;
    info->jre_version = NULL;
    info->jre_restrict_search = 0;
    info->splashscreen_image_file_name = NULL;
    if ((rc = find_file(fd, &entry, manifest_name)) != 0) {
        close(fd);
        return (-2);
    }
    manifest = inflate_file(fd, &entry, NULL);
    if (manifest == NULL) {
        close(fd);
        return (-2);
    }
    lp = manifest;
    while ((rc = parse_nv_pair(&lp, &name, &value)) > 0) {
        if (JLI_StrCaseCmp(name, "Manifest-Version") == 0) {
            info->manifest_version = value;
        } else if (JLI_StrCaseCmp(name, "Main-Class") == 0) {
            info->main_class = value;
        } else if (JLI_StrCaseCmp(name, "JRE-Version") == 0) {
            /*
             * Manifest specification overridden by command line option
             * so we will silently override there with no specification.
             */
            info->jre_version = 0;
        } else if (JLI_StrCaseCmp(name, "Splashscreen-Image") == 0) {
            info->splashscreen_image_file_name = value;
        }
    }
    close(fd);
    if (rc == 0)
        return (0);
    else
        return (-2);
}

/*
 * Opens the jar file and unpacks the specified file from its contents.
 * Returns NULL on failure.
 */
void *
JLI_JarUnpackFile(const char *jarfile, const char *filename, int *size) {
    int     fd;
    zentry  entry;
    void    *data = NULL;

    if ((fd = JLI_Open(jarfile, O_RDONLY
#ifdef O_LARGEFILE
        | O_LARGEFILE /* large file mode */
#endif
#ifdef O_BINARY
        | O_BINARY /* use binary mode on windows */
#endif
        )) == -1) {
        return NULL;
    }
    if (find_file(fd, &entry, filename) == 0) {
        data = inflate_file(fd, &entry, size);
    }
    close(fd);
    return (data);
}

/*
 * Specialized "free" function.
 */
void
JLI_FreeManifest()
{
    if (manifest)
        free(manifest);
}

/*
 * Iterate over the manifest of the specified jar file and invoke the provided
 * closure function for each attribute encountered.
 *
 * Error returns are as follows:
 *    0 Success
 *   -1 Unable to open jarfile
 *   -2 Error accessing the manifest from within the jarfile (most likely
 *      this means a manifest is not present, or it isn't a valid zip/jar file).
 */
JNIEXPORT int JNICALL
JLI_ManifestIterate(const char *jarfile, attribute_closure ac, void *user_data)
{
    int     fd;
    zentry  entry;
    char    *mp;        /* manifest pointer */
    char    *lp;        /* pointer into manifest, updated during iteration */
    char    *name;
    char    *value;
    int     rc;

    if ((fd = JLI_Open(jarfile, O_RDONLY
#ifdef O_LARGEFILE
        | O_LARGEFILE /* large file mode */
#endif
#ifdef O_BINARY
        | O_BINARY /* use binary mode on windows */
#endif
        )) == -1) {
        return (-1);
    }

    if ((rc = find_file(fd, &entry, manifest_name)) != 0) {
        close(fd);
        return (-2);
    }

    mp = inflate_file(fd, &entry, NULL);
    if (mp == NULL) {
        close(fd);
        return (-2);
    }

    lp = mp;
    while ((rc = parse_nv_pair(&lp, &name, &value)) > 0) {
        (*ac)(name, value, user_data);
    }
    free(mp);
    close(fd);
    return (rc == 0) ? 0 : -2;
}
