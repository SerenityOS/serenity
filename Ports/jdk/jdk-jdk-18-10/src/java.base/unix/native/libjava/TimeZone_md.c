/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <stdio.h>
#include <strings.h>
#include <time.h>
#include <limits.h>
#include <errno.h>
#include <stddef.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <string.h>
#include <dirent.h>
#include <unistd.h>

#include "jvm.h"
#include "TimeZone_md.h"

static char *isFileIdentical(char* buf, size_t size, char *pathname);

#define SKIP_SPACE(p)   while (*p == ' ' || *p == '\t') p++;

#define RESTARTABLE(_cmd, _result) do { \
  do { \
    _result = _cmd; \
  } while((_result == -1) && (errno == EINTR)); \
} while(0)

#define fileopen        fopen
#define filegets        fgets
#define fileclose       fclose

#if defined(_ALLBSD_SOURCE)
#define stat64 stat
#define lstat64 lstat
#define fstat64 fstat
#endif

#if defined(__linux__) || defined(_ALLBSD_SOURCE)
static const char *ETC_TIMEZONE_FILE = "/etc/timezone";
static const char *ZONEINFO_DIR = "/usr/share/zoneinfo";
static const char *DEFAULT_ZONEINFO_FILE = "/etc/localtime";
#else
static const char *SYS_INIT_FILE = "/etc/default/init";
static const char *ZONEINFO_DIR = "/usr/share/lib/zoneinfo";
static const char *DEFAULT_ZONEINFO_FILE = "/usr/share/lib/zoneinfo/localtime";
#endif /* defined(__linux__) || defined(_ALLBSD_SOURCE) */

static const char popularZones[][4] = {"UTC", "GMT"};

#if defined(_AIX)
static const char *ETC_ENVIRONMENT_FILE = "/etc/environment";
#endif

#if defined(__linux__) || defined(MACOSX)

/*
 * Returns a pointer to the zone ID portion of the given zoneinfo file
 * name, or NULL if the given string doesn't contain "zoneinfo/".
 */
static char *
getZoneName(char *str)
{
    static const char *zidir = "zoneinfo/";

    char *pos = strstr((const char *)str, zidir);
    if (pos == NULL) {
        return NULL;
    }
    return pos + strlen(zidir);
}

/*
 * Returns a path name created from the given 'dir' and 'name' under
 * UNIX. This function allocates memory for the pathname calling
 * malloc(). NULL is returned if malloc() fails.
 */
static char *
getPathName(const char *dir, const char *name) {
    char *path;

    path = (char *) malloc(strlen(dir) + strlen(name) + 2);
    if (path == NULL) {
        return NULL;
    }
    return strcat(strcat(strcpy(path, dir), "/"), name);
}

/*
 * Scans the specified directory and its subdirectories to find a
 * zoneinfo file which has the same content as /etc/localtime on Linux
 * or /usr/share/lib/zoneinfo/localtime on Solaris given in 'buf'.
 * If file is symbolic link, then the contents it points to are in buf.
 * Returns a zone ID if found, otherwise, NULL is returned.
 */
static char *
findZoneinfoFile(char *buf, size_t size, const char *dir)
{
    DIR *dirp = NULL;
    struct dirent *dp = NULL;
    char *pathname = NULL;
    char *tz = NULL;
    int res;

    if (strcmp(dir, ZONEINFO_DIR) == 0) {
        /* fast path for 1st iteration */
        for (unsigned int i = 0; i < sizeof (popularZones) / sizeof (popularZones[0]); i++) {
            pathname = getPathName(dir, popularZones[i]);
            if (pathname == NULL) {
                continue;
            }
            tz = isFileIdentical(buf, size, pathname);
            free((void *) pathname);
            pathname = NULL;
            if (tz != NULL) {
                return tz;
            }
        }
    }

    dirp = opendir(dir);
    if (dirp == NULL) {
        return NULL;
    }

    while ((dp = readdir(dirp)) != NULL) {
        /*
         * Skip '.' and '..' (and possibly other .* files)
         */
        if (dp->d_name[0] == '.') {
            continue;
        }

        /*
         * Skip "ROC", "posixrules", and "localtime".
         */
        if ((strcmp(dp->d_name, "ROC") == 0)
            || (strcmp(dp->d_name, "posixrules") == 0)
            || (strcmp(dp->d_name, "localtime") == 0)) {
            continue;
        }

        pathname = getPathName(dir, dp->d_name);
        if (pathname == NULL) {
            break;
        }

        tz = isFileIdentical(buf, size, pathname);
        free((void *) pathname);
        pathname = NULL;
        if (tz != NULL) {
           break;
        }
    }

    if (dirp != NULL) {
        (void) closedir(dirp);
    }
    return tz;
}

/*
 * Checks if the file pointed to by pathname matches
 * the data contents in buf.
 * Returns a representation of the timezone file name
 * if file match is found, otherwise NULL.
 */
static char *
isFileIdentical(char *buf, size_t size, char *pathname)
{
    char *possibleMatch = NULL;
    struct stat64 statbuf;
    char *dbuf = NULL;
    int fd = -1;
    int res;

    RESTARTABLE(stat64(pathname, &statbuf), res);
    if (res == -1) {
        return NULL;
    }

    if (S_ISDIR(statbuf.st_mode)) {
        possibleMatch  = findZoneinfoFile(buf, size, pathname);
    } else if (S_ISREG(statbuf.st_mode) && (size_t)statbuf.st_size == size) {
        dbuf = (char *) malloc(size);
        if (dbuf == NULL) {
            return NULL;
        }
        RESTARTABLE(open(pathname, O_RDONLY), fd);
        if (fd == -1) {
            goto freedata;
        }
        RESTARTABLE(read(fd, dbuf, size), res);
        if (res != (ssize_t) size) {
            goto freedata;
        }
        if (memcmp(buf, dbuf, size) == 0) {
            possibleMatch = getZoneName(pathname);
            if (possibleMatch != NULL) {
                possibleMatch = strdup(possibleMatch);
            }
        }
        freedata:
        free((void *) dbuf);
        (void) close(fd);
    }
    return possibleMatch;
}

/*
 * Performs Linux specific mapping and returns a zone ID
 * if found. Otherwise, NULL is returned.
 */
static char *
getPlatformTimeZoneID()
{
    struct stat64 statbuf;
    char *tz = NULL;
    FILE *fp;
    int fd;
    char *buf;
    size_t size;
    int res;

#if defined(__linux__)
    /*
     * Try reading the /etc/timezone file for Debian distros. There's
     * no spec of the file format available. This parsing assumes that
     * there's one line of an Olson tzid followed by a '\n', no
     * leading or trailing spaces, no comments.
     */
    if ((fp = fopen(ETC_TIMEZONE_FILE, "r")) != NULL) {
        char line[256];

        if (fgets(line, sizeof(line), fp) != NULL) {
            char *p = strchr(line, '\n');
            if (p != NULL) {
                *p = '\0';
            }
            if (strlen(line) > 0) {
                tz = strdup(line);
            }
        }
        (void) fclose(fp);
        if (tz != NULL) {
            return tz;
        }
    }
#endif /* defined(__linux__) */

    /*
     * Next, try /etc/localtime to find the zone ID.
     */
    RESTARTABLE(lstat64(DEFAULT_ZONEINFO_FILE, &statbuf), res);
    if (res == -1) {
        return NULL;
    }

    /*
     * If it's a symlink, get the link name and its zone ID part. (The
     * older versions of timeconfig created a symlink as described in
     * the Red Hat man page. It was changed in 1999 to create a copy
     * of a zoneinfo file. It's no longer possible to get the zone ID
     * from /etc/localtime.)
     */
    if (S_ISLNK(statbuf.st_mode)) {
        char linkbuf[PATH_MAX+1];
        int len;

        if ((len = readlink(DEFAULT_ZONEINFO_FILE, linkbuf, sizeof(linkbuf)-1)) == -1) {
            jio_fprintf(stderr, (const char *) "can't get a symlink of %s\n",
                        DEFAULT_ZONEINFO_FILE);
            return NULL;
        }
        linkbuf[len] = '\0';
        tz = getZoneName(linkbuf);
        if (tz != NULL) {
            tz = strdup(tz);
            return tz;
        }
    }

    /*
     * If it's a regular file, we need to find out the same zoneinfo file
     * that has been copied as /etc/localtime.
     * If initial symbolic link resolution failed, we should treat target
     * file as a regular file.
     */
    RESTARTABLE(open(DEFAULT_ZONEINFO_FILE, O_RDONLY), fd);
    if (fd == -1) {
        return NULL;
    }

    RESTARTABLE(fstat64(fd, &statbuf), res);
    if (res == -1) {
        (void) close(fd);
        return NULL;
    }
    size = (size_t) statbuf.st_size;
    buf = (char *) malloc(size);
    if (buf == NULL) {
        (void) close(fd);
        return NULL;
    }

    RESTARTABLE(read(fd, buf, size), res);
    if (res != (ssize_t) size) {
        (void) close(fd);
        free((void *) buf);
        return NULL;
    }
    (void) close(fd);

    tz = findZoneinfoFile(buf, size, ZONEINFO_DIR);
    free((void *) buf);
    return tz;
}

#elif defined(_AIX)

static char *
getPlatformTimeZoneID()
{
    FILE *fp;
    char *tz = NULL;
    char *tz_key = "TZ=";
    char line[256];
    size_t tz_key_len = strlen(tz_key);

    if ((fp = fopen(ETC_ENVIRONMENT_FILE, "r")) != NULL) {
        while (fgets(line, sizeof(line), fp) != NULL) {
            char *p = strchr(line, '\n');
            if (p != NULL) {
                *p = '\0';
            }
            if (0 == strncmp(line, tz_key, tz_key_len)) {
                tz = strdup(line + tz_key_len);
                break;
            }
        }
        (void) fclose(fp);
    }

    return tz;
}

static char *
mapPlatformToJavaTimezone(const char *java_home_dir, const char *tz) {
    FILE *tzmapf;
    char mapfilename[PATH_MAX + 1];
    char line[256];
    int linecount = 0;
    char *tz_buf = NULL;
    char *temp_tz = NULL;
    char *javatz = NULL;
    size_t tz_len = 0;

    /* On AIX, the TZ environment variable may end with a comma
     * followed by modifier fields. These are ignored here. */
    temp_tz = strchr(tz, ',');
    tz_len = (temp_tz == NULL) ? strlen(tz) : temp_tz - tz;
    tz_buf = (char *)malloc(tz_len + 1);
    memcpy(tz_buf, tz, tz_len);
    tz_buf[tz_len] = 0;

    /* Open tzmappings file, with buffer overrun check */
    if ((strlen(java_home_dir) + 15) > PATH_MAX) {
        jio_fprintf(stderr, "Path %s/lib/tzmappings exceeds maximum path length\n", java_home_dir);
        goto tzerr;
    }
    strcpy(mapfilename, java_home_dir);
    strcat(mapfilename, "/lib/tzmappings");
    if ((tzmapf = fopen(mapfilename, "r")) == NULL) {
        jio_fprintf(stderr, "can't open %s\n", mapfilename);
        goto tzerr;
    }

    while (fgets(line, sizeof(line), tzmapf) != NULL) {
        char *p = line;
        char *sol = line;
        char *java;
        int result;

        linecount++;
        /*
         * Skip comments and blank lines
         */
        if (*p == '#' || *p == '\n') {
            continue;
        }

        /*
         * Get the first field, platform zone ID
         */
        while (*p != '\0' && *p != '\t') {
            p++;
        }
        if (*p == '\0') {
            /* mapping table is broken! */
            jio_fprintf(stderr, "tzmappings: Illegal format at near line %d.\n", linecount);
            break;
        }

        *p++ = '\0';
        if ((result = strncmp(tz_buf, sol, tz_len)) == 0) {
            /*
             * If this is the current platform zone ID,
             * take the Java time zone ID (2nd field).
             */
            java = p;
            while (*p != '\0' && *p != '\n') {
                p++;
            }

            if (*p == '\0') {
                /* mapping table is broken! */
                jio_fprintf(stderr, "tzmappings: Illegal format at line %d.\n", linecount);
                break;
            }

            *p = '\0';
            javatz = strdup(java);
            break;
        } else if (result < 0) {
            break;
        }
    }
    (void) fclose(tzmapf);

tzerr:
    if (tz_buf != NULL ) {
        free((void *) tz_buf);
    }

    if (javatz == NULL) {
        return getGMTOffsetID();
    }

    return javatz;
}

#endif /* defined(_AIX) */

/*
 * findJavaTZ_md() maps platform time zone ID to Java time zone ID
 * using <java_home>/lib/tzmappings. If the TZ value is not found, it
 * trys some libc implementation dependent mappings. If it still
 * can't map to a Java time zone ID, it falls back to the GMT+/-hh:mm
 * form.
 */
/*ARGSUSED1*/
char *
findJavaTZ_md(const char *java_home_dir)
{
    char *tz;
    char *javatz = NULL;
    char *freetz = NULL;

    tz = getenv("TZ");

    if (tz == NULL || *tz == '\0') {
        tz = getPlatformTimeZoneID();
        freetz = tz;
    }

    if (tz != NULL) {
        /* Ignore preceding ':' */
        if (*tz == ':') {
            tz++;
        }
#if defined(__linux__)
        /* Ignore "posix/" prefix on Linux. */
        if (strncmp(tz, "posix/", 6) == 0) {
            tz += 6;
        }
#endif

#if defined(_AIX)
        /* On AIX do the platform to Java mapping. */
        javatz = mapPlatformToJavaTimezone(java_home_dir, tz);
        if (freetz != NULL) {
            free((void *) freetz);
        }
#else
        if (freetz == NULL) {
            /* strdup if we are still working on getenv result. */
            javatz = strdup(tz);
        } else if (freetz != tz) {
            /* strdup and free the old buffer, if we moved the pointer. */
            javatz = strdup(tz);
            free((void *) freetz);
        } else {
            /* we are good if we already work on a freshly allocated buffer. */
            javatz = tz;
        }
#endif
    }

    return javatz;
}

/**
 * Returns a GMT-offset-based zone ID. (e.g., "GMT-08:00")
 */

#if defined(MACOSX)

char *
getGMTOffsetID()
{
    time_t offset;
    char sign, buf[32];
    struct tm local_tm;
    time_t clock;

    clock = time(NULL);
    if (localtime_r(&clock, &local_tm) == NULL) {
        return strdup("GMT");
    }
    offset = (time_t)local_tm.tm_gmtoff;
    if (offset == 0) {
        return strdup("GMT");
    }
    if (offset > 0) {
        sign = '+';
    } else {
        offset = -offset;
        sign = '-';
    }
    sprintf(buf, (const char *)"GMT%c%02d:%02d",
            sign, (int)(offset/3600), (int)((offset%3600)/60));
    return strdup(buf);
}

#else

char *
getGMTOffsetID()
{
    time_t offset;
    char sign, buf[32];
    offset = timezone;

    if (offset == 0) {
        return strdup("GMT");
    }

    /* Note that the time offset direction is opposite. */
    if (offset > 0) {
        sign = '-';
    } else {
        offset = -offset;
        sign = '+';
    }
    sprintf(buf, (const char *)"GMT%c%02d:%02d",
            sign, (int)(offset/3600), (int)((offset%3600)/60));
    return strdup(buf);
}
#endif /* MACOSX */
