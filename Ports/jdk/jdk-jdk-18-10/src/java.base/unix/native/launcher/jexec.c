/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * jexec for J2SE
 *
 * jexec is used by the system to allow execution of JAR files.
 *    Essentially jexec needs to run java and
 *    needs to be a native ISA executable (not a shell script), although
 *    this native ISA executable requirement was a mistake that will be fixed.
 *    (<ISA> is sparc or i386 or amd64).
 *
 *    When you execute a jar file, jexec is executed by the system as follows:
 *      /usr/java/jre/lib/<ISA>/jexec -jar JARFILENAME
 *    so this just needs to be turned into:
 *      /usr/java/jre/bin/java -jar JARFILENAME
 *
 * Solaris systems (new 7's and all 8's) will be looking for jexec at:
 *      /usr/java/jre/lib/<ISA>/jexec
 * Older systems may need to add this to their /etc/system file:
 *      set javaexec:jexec="/usr/java/jre/lib/<ISA>/jexec"
 *     and reboot the machine for this to work.
 *
 * This source should be compiled as:
 *      cc -o jexec jexec.c
 *
 * And jexec should be placed at the following location of the installation:
 *      <INSTALLATIONDIR>/jre/lib/<ISA>/jexec  (for Solaris)
 *      <INSTALLATIONDIR>/lib/jexec            (for Linux)
 *
 * NOTE: Unless <INSTALLATIONDIR> is the "default" JDK on the system
 *       (i.e. /usr/java -> <INSTALLATIONDIR>), this jexec will not be
 *       found.  The 1.2 java is only the default on Solaris 8 and
 *       on systems where the 1.2 packages were installed and no 1.1
 *       java was found.
 *
 * NOTE: You must use 1.2 jar to build your jar files. The system
 *       doesn't seem to pick up 1.1 jar files.
 *
 * NOTE: We don't need to set LD_LIBRARY_PATH here, even though we
 *       are running the actual java binary because the java binary will
 *       look for it's libraries through it's own runpath, which uses
 *       $ORIGIN.
 *
 * NOTE: This jexec should NOT have any special .so library needs because
 *       it appears that this executable will NOT get the $ORIGIN of jexec
 *       but the $ORIGIN of the jar file being executed. Be careful to keep
 *       this program simple and with no .so dependencies.
 */

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <limits.h>
#include <errno.h>
#ifdef __linux__
#  include <sys/types.h>
#  include <sys/stat.h>
#  include <fcntl.h>
#  include "jni.h"
#  include "manifest_info.h"
#endif

static const int CRAZY_EXEC = ENOEXEC;
static const int BAD_MAGIC  = ENOEXEC;

static const char * BAD_EXEC_MSG     = "jexec failed";
static const char * CRAZY_EXEC_MSG   = "missing args";
static const char * MISSING_JAVA_MSG = "can't locate java";
static const char * BAD_ARG_MSG      = "incorrect number of arguments";
static const char * MEM_FAILED_MSG   = "memory allocation failed";
#ifdef __linux__
static const char * BAD_PATHNAME_MSG = "invalid path";
static const char * BAD_FILE_MSG     = "invalid file";
static const char * BAD_MAGIC_MSG    = "invalid file (bad magic number)";
#endif
static const char * UNKNOWN_ERROR    = "unknown error";

/* Define a constant that represents the number of directories to pop off the
 * current location to find the java binary */
#ifdef __linux__
static const int RELATIVE_DEPTH = 2;
#else /* Solaris */
static const int RELATIVE_DEPTH = 3;
#endif

/* path to java after popping */
static const char * BIN_PATH = "/bin/java";

/* flag used when running JAR files */
static const char * JAR_FLAG = "-jar";


#ifdef __linux__
/* largest possible size for a local file header */
static const size_t CHUNK_SIZE = 65535;

/* smallest possible size for a local file header */
static const ssize_t MIN_SIZE = LOCHDR + 1 + 4;
#endif


int main(int argc, const char * argv[]);
void errorExit(int error, const char * message);
int getJavaPath(const char * path, char * buf, int depth);
#ifdef __linux__
const char * isJar(const char * path);
#endif


/*
 * This is the main entry point.  This program (jexec) will attempt to execute
 * a JAR file by finding the Java program (java), relative to its own location.
 * The exact location of the Java program depends on the platform, i.e.
 *
 *      <INSTALLATIONDIR>/jre/lib/<ISA>/jexec  (for Solaris)
 *      <INSTALLATIONDIR>/lib/jexec            (for Linux JDK)
 *
 * Once the Java program is found, this program copies any remaining arguments
 * into another array, which is then used to exec the Java program.
 *
 * On Linux this program does some additional steps.  When copying the array of
 * args, it is necessary to insert the "-jar" flag between arg[0], the program
 * name, and the original arg[1], which is presumed to be a path to a JAR file.
 * It is also necessary to verify that the original arg[1] really is a JAR file.
 * (These steps are unnecessary on Solaris because they are taken care of by
 * the kernel.)
 */
int main(int argc, const char * argv[]) {
    /* We need to exec the original arguments using java, instead of jexec.
     * Also, for Linux, it is necessary to add the "-jar" argument between
     * the new arg[0], and the old arg[1].  To do this we will create a new
     * args array. */
    char          java[PATH_MAX + 1];    /* path to java binary  */
    const char ** nargv = NULL;          /* new args array       */
    int           nargc = 0;             /* new args array count */
    int           argi  = 0;             /* index into old array */
    size_t        alen  = 0;             /* length of new array */

    /* Make sure we have something to work with */
    if ((argc < 1) || (argv == NULL)) {
        /* Shouldn't happen... */
        errorExit(CRAZY_EXEC, CRAZY_EXEC_MSG);
    }

    /* Get the path to the java binary, which is in a known position relative
     * to our current position, which is in argv[0]. */
    if (getJavaPath(argv[argi++], java, RELATIVE_DEPTH) != 0) {
        errorExit(errno, MISSING_JAVA_MSG);
    }
    alen = (argc + 2) * (sizeof (const char *));
    if (alen <= 0 || alen > INT_MAX / sizeof(char *)) {
        errorExit(errno, BAD_ARG_MSG);
    }
    nargv = (const char **) malloc(alen);
    if (nargv == NULL) {
        errorExit(errno, MEM_FAILED_MSG);
    }
    nargv[nargc++] = java;

#ifdef __linux__
    /* The "-jar" flag is already in the original args list on Solaris,
     * so it only needs to be added on Linux. */
    nargv[nargc++] = JAR_FLAG;
#endif

    if (argc >= 2) {
        const char * jarfile = argv[argi++];
        const char * message = NULL;

#ifdef __linux__
        /* On Linux we also need to make sure argv[1] is really a JAR
         * file (this will also resolve any symlinks, which helps). */
        char jarPath[PATH_MAX + 1];

        if (realpath(jarfile, jarPath) == NULL) {
            errorExit(errno, BAD_PATHNAME_MSG);
        }

        message = isJar(jarPath);
        if (message != NULL) {
            errorExit(errno, message);
        }

        jarfile = jarPath;
#endif
        /* the next argument is the path to the JAR file */
        nargv[nargc++] = jarfile;
    }

    /* finally copy any remaining arguments */
    while (argi < argc) {
        nargv[nargc++] = argv[argi++];
    }

    /* finally add one last terminating null */
    nargv[nargc++] = NULL;

    /* It's time to exec the java binary with the new arguments.  It
     * is possible that we've reached this point without actually
     * having a JAR file argument (i.e. if argc < 2), but we still
     * want to exec the java binary, since that will take care of
     * displaying the correct usage. */
    execv(java, (char * const *) nargv);

    /* If the exec worked, this process would have been replaced
     * by the new process.  So any code reached beyond this point
     * implies an error in the exec. */
    free(nargv);
    errorExit(errno, BAD_EXEC_MSG);
    return 0; // keep the compiler happy
}


/*
 * Exit the application by setting errno, and writing a message.
 *
 * Parameters:
 *     error   - errno is set to this value, and it is used to exit.
 *     message - the message to write.
 */
void errorExit(int error, const char * message) {
    if (error != 0) {
        errno = error;
        perror((message != NULL) ? message : UNKNOWN_ERROR);
    }

    exit((error == 0) ? 0 : 1);
}


/*
 * Get the path to the java binary that should be relative to the current path.
 *
 * Parameters:
 *     path  - the input path that the java binary that should be relative to.
 *     buf   - a buffer of size PATH_MAX or greater that the java path is
 *             copied to.
 *     depth - the number of names to trim off the current path, including the
 *             name of this program.
 *
 * Returns:
 *     This function returns 0 on success; otherwise it returns the value of
 *     errno.
 */
int getJavaPath(const char * path, char * buf, int depth) {
    int result = 0;

    /* Get the full path to this program.  Depending on whether this is Solaris
     * or Linux, this will be something like,
     *
     *     <FOO>/jre/lib/<ISA>/jexec  (for Solaris)
     *     <FOO>/lib/jexec            (for Linux)
     */
    if (realpath(path, buf) != NULL) {
        int count = 0;

        /* Pop off the filename, and then subdirectories for each level of
         * depth */
        for (count = 0; count < depth; count++) {
            *(strrchr(buf, '/')) = '\0';
        }

        /* Append the relative location of java, creating something like,
         *
         *     <FOO>/jre/bin/java  (for Solaris)
         *     <FOO>/bin/java      (for Linux)
         */
        strcat(buf, BIN_PATH);
    }
    else {
        /* Failed to get the path */
        result = errno;
    }

    return (result);
}


#ifdef __linux__
/*
 * Check if the given file is a JAR file.
 *
 * Parameters:
 *     path  - the path to the file to check for JAR magic.
 *
 * Returns:
 *     This function return NULL on success.  Otherwise, errno is set, and it
 *     returns a message that indicates what caused the failure.
 */
const char * isJar(const char * path) {
    const char * result = BAD_FILE_MSG;

    int fd = open(path, O_RDONLY);
    if (fd != -1) {
        unsigned char buf[CHUNK_SIZE];

        ssize_t count = read(fd, buf, CHUNK_SIZE);
        if (count >= MIN_SIZE) {
            result = BAD_MAGIC_MSG;

            // be sure the file is at least a ZIP file
            if (LOCSIG_AT(buf)) {

                off_t flen  = LOCNAM(buf);
                off_t xlen  = LOCEXT(buf);
                off_t start = LOCHDR + flen;
                off_t end   = start  + xlen;

                if (end <= count) {
                    end -= 4; // make sure there are 4 bytes to read at start
                    while (start <= end) {
                        off_t xhid  = SH(buf, start);
                        off_t xdlen = SH(buf, start + 2);

                        start += 4 + xdlen;
                        if (xhid == 0xcafe) {
                            // found the JAR magic
                            result = NULL;
                            break;
                        }
                    }
                }
            }
        }

        if (result != NULL) {
            errno = BAD_MAGIC;
        }

        close (fd);
    }

    return (result);
}
#endif
