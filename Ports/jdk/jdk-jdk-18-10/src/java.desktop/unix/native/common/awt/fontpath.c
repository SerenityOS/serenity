/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

#if defined(__linux__)
#include <string.h>
#endif /* __linux__ */
#include <stdio.h>
#include <stdlib.h>
#include <strings.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>

#include <jni.h>
#include <jni_util.h>
#include <jvm_md.h>
#include <sizecalc.h>
#ifndef HEADLESS
#include <X11/Xlib.h>
#include <awt.h>
#else
/* locks ought to be included from awt.h */
#define AWT_LOCK()
#define AWT_UNLOCK()
#endif /* !HEADLESS */

#if defined(__linux__) && !defined(MAP_FAILED)
#define MAP_FAILED ((caddr_t)-1)
#endif

#ifndef HEADLESS
extern Display *awt_display;
#endif /* !HEADLESS */

#define FONTCONFIG_DLL_VERSIONED VERSIONED_JNI_LIB_NAME("fontconfig", "1")
#define FONTCONFIG_DLL JNI_LIB_NAME("fontconfig")

#define MAXFDIRS 512    /* Max number of directories that contain fonts */

#if defined( __linux__)
/* All the known interesting locations we have discovered on
 * various flavors of Linux
 */
static char *fullLinuxFontPath[] = {
    "/usr/X11R6/lib/X11/fonts/TrueType",  /* RH 7.1+ */
    "/usr/X11R6/lib/X11/fonts/truetype",  /* SuSE */
    "/usr/X11R6/lib/X11/fonts/tt",
    "/usr/X11R6/lib/X11/fonts/TTF",
    "/usr/X11R6/lib/X11/fonts/OTF",       /* RH 9.0 (but empty!) */
    "/usr/share/fonts/ja/TrueType",       /* RH 7.2+ */
    "/usr/share/fonts/truetype",
    "/usr/share/fonts/ko/TrueType",       /* RH 9.0 */
    "/usr/share/fonts/zh_CN/TrueType",    /* RH 9.0 */
    "/usr/share/fonts/zh_TW/TrueType",    /* RH 9.0 */
    "/var/lib/defoma/x-ttcidfont-conf.d/dirs/TrueType", /* Debian */
    "/usr/X11R6/lib/X11/fonts/Type1",
    "/usr/share/fonts/default/Type1",     /* RH 9.0 */
    NULL, /* terminates the list */
};
#elif defined(_AIX)
static char *fullAixFontPath[] = {
    "/usr/lpp/X11/lib/X11/fonts/Type1",    /* from X11.fnt.iso_T1  */
    "/usr/lpp/X11/lib/X11/fonts/TrueType", /* from X11.fnt.ucs.ttf */
    NULL, /* terminates the list */
};
#endif

static char **getFontConfigLocations();

typedef struct {
    const char *name[MAXFDIRS];
    int  num;
} fDirRecord, *fDirRecordPtr;

#ifndef HEADLESS

/*
 * Returns True if display is local, False of it's remote.
 */
jboolean isDisplayLocal(JNIEnv *env) {
    static jboolean isLocal = False;
    static jboolean isLocalSet = False;
    jboolean ret;

    if (! isLocalSet) {
      jclass geCls = (*env)->FindClass(env, "java/awt/GraphicsEnvironment");
      CHECK_NULL_RETURN(geCls, JNI_FALSE);
      jmethodID getLocalGE = (*env)->GetStaticMethodID(env, geCls,
                                                 "getLocalGraphicsEnvironment",
                                           "()Ljava/awt/GraphicsEnvironment;");
      CHECK_NULL_RETURN(getLocalGE, JNI_FALSE);
      jobject ge = (*env)->CallStaticObjectMethod(env, geCls, getLocalGE);
      JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);

      jclass sgeCls = (*env)->FindClass(env,
                                        "sun/java2d/SunGraphicsEnvironment");
      CHECK_NULL_RETURN(sgeCls, JNI_FALSE);
      if ((*env)->IsInstanceOf(env, ge, sgeCls)) {
        jmethodID isDisplayLocal = (*env)->GetMethodID(env, sgeCls,
                                                       "isDisplayLocal",
                                                       "()Z");
        JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);
        isLocal = (*env)->CallBooleanMethod(env, ge, isDisplayLocal);
        JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);
      } else {
        isLocal = True;
      }
      isLocalSet = True;
    }

    return isLocal;
}

static void AddFontsToX11FontPath ( fDirRecord *fDirP )
{
    char *onePath;
    int index, nPaths;
    int origNumPaths, length;
    int origIndex;
    int totalDirCount;
    char  **origFontPath;
    char  **tempFontPath;
    int doNotAppend;
    int *appendDirList;
    char **newFontPath;
    int err, compareLength;
    char fontDirPath[512];
    int dirFile;

    doNotAppend = 0;

    if ( fDirP->num == 0 ) return;

    appendDirList = SAFE_SIZE_ARRAY_ALLOC(malloc, fDirP->num, sizeof ( int ));
    if ( appendDirList == NULL ) {
      return;  /* if it fails we cannot do much */
    }

    origFontPath = XGetFontPath ( awt_display, &nPaths );

    totalDirCount = nPaths;
    origNumPaths = nPaths;
    tempFontPath = origFontPath;


    for (index = 0; index < fDirP->num; index++ ) {

        doNotAppend = 0;

        tempFontPath = origFontPath;
        for ( origIndex = 0; origIndex < nPaths; origIndex++ ) {

            onePath = *tempFontPath;

            compareLength = strlen ( onePath );
            if ( onePath[compareLength -1] == '/' )
              compareLength--;

            /* there is a slash at the end of every solaris X11 font path name */
            if ( strncmp ( onePath, fDirP->name[index], compareLength ) == 0 ) {
              doNotAppend = 1;
              break;
            }
            tempFontPath++;
        }

        appendDirList[index] = 0;
        if ( doNotAppend == 0 ) {
            snprintf(fontDirPath, sizeof(fontDirPath), "%s/fonts.dir", fDirP->name[index]);
            fontDirPath[sizeof(fontDirPath) - 1] = '\0';
            dirFile = open ( fontDirPath, O_RDONLY, 0 );
            if ( dirFile == -1 ) {
                doNotAppend = 1;
            } else {
               close ( dirFile );
               totalDirCount++;
               appendDirList[index] = 1;
            }
        }

    }

    /* if no changes are required do not bother to do a setfontpath */
    if ( totalDirCount == nPaths ) {
      free ( ( void *) appendDirList );
      XFreeFontPath ( origFontPath );
      return;
    }


    newFontPath = SAFE_SIZE_ARRAY_ALLOC(malloc, totalDirCount, sizeof(char *));
    /* if it fails free things and get out */
    if ( newFontPath == NULL ) {
      free ( ( void *) appendDirList );
      XFreeFontPath ( origFontPath );
      return;
    }

    for ( origIndex = 0; origIndex < nPaths; origIndex++ ) {
      onePath = origFontPath[origIndex];
      newFontPath[origIndex] = onePath;
    }

    /* now add the other font paths */

    for (index = 0; index < fDirP->num; index++ ) {

      if ( appendDirList[index] == 1 ) {

        /* printf ( "Appending %s\n", fDirP->name[index] ); */

        onePath = SAFE_SIZE_ARRAY_ALLOC(malloc, strlen (fDirP->name[index]) + 2, sizeof( char ) );
        if (onePath == NULL) {
            free ( ( void *) appendDirList );

            for ( index = origIndex; index < nPaths; index++ ) {
                free( newFontPath[index] );
            }

            free( ( void *) newFontPath);
            XFreeFontPath ( origFontPath );
            return;
        }
        strcpy ( onePath, fDirP->name[index] );
        strcat ( onePath, "/" );
        newFontPath[nPaths++] = onePath;
        /* printf ( "The path to be appended is %s\n", onePath ); */
      }
    }

    /*   printf ( "The dir count = %d\n", totalDirCount ); */
    free ( ( void *) appendDirList );

    XSetFontPath ( awt_display, newFontPath, totalDirCount );

        for ( index = origNumPaths; index < totalDirCount; index++ ) {
                free( newFontPath[index] );
    }

        free ( (void *) newFontPath );
    XFreeFontPath ( origFontPath );
    return;
}
#endif /* !HEADLESS */


#ifndef HEADLESS
static char **getX11FontPath ()
{
    char **x11Path, **fontdirs;
    int i, pos, slen, nPaths, numDirs;

    x11Path = XGetFontPath (awt_display, &nPaths);

    /* This isn't ever going to be perfect: the font path may contain
     * much we aren't interested in, but the cost should be moderate
     * Exclude all directories that contain the strings "Speedo","/F3/",
     * "75dpi", "100dpi", "misc" or "bitmap", or don't begin with a "/",
     * the last of which should exclude font servers.
     * Also exclude the user specific ".gnome*" directories which
     * aren't going to contain the system fonts we need.
     * Hopefully we are left only with Type1 and TrueType directories.
     * It doesn't matter much if there are extraneous directories, it'll just
     * cost us a little wasted effort upstream.
     */
    fontdirs = (char**)calloc(nPaths+1, sizeof(char*));
    if (fontdirs == NULL) {
        return NULL;
    }
    pos = 0;
    for (i=0; i < nPaths; i++) {
        if (x11Path[i][0] != '/') {
            continue;
        }
        if (strstr(x11Path[i], "/75dpi") != NULL) {
            continue;
        }
        if (strstr(x11Path[i], "/100dpi") != NULL) {
            continue;
        }
        if (strstr(x11Path[i], "/misc") != NULL) {
            continue;
        }
        if (strstr(x11Path[i], "/Speedo") != NULL) {
            continue;
        }
        if (strstr(x11Path[i], ".gnome") != NULL) {
            continue;
        }
        fontdirs[pos] = strdup(x11Path[i]);
        slen = strlen(fontdirs[pos]);
        if (slen > 0 && fontdirs[pos][slen-1] == '/') {
            fontdirs[pos][slen-1] = '\0'; /* null out trailing "/"  */
        }
        pos++;
    }

    XFreeFontPath(x11Path);
    if (pos == 0) {
        free(fontdirs);
        fontdirs = NULL;
    }
    return fontdirs;
}


#endif /* !HEADLESS */

#if defined(__linux__)
/* from awt_LoadLibrary.c */
JNIEXPORT jboolean JNICALL AWTIsHeadless();
#endif

/* This eliminates duplicates, at a non-linear but acceptable cost
 * since the lists are expected to be reasonably short, and then
 * deletes references to non-existent directories, and returns
 * a single path consisting of unique font directories.
 */
static char* mergePaths(char **p1, char **p2, char **p3, jboolean noType1) {

    int len1=0, len2=0, len3=0, totalLen=0, numDirs=0,
        currLen, i, j, found, pathLen=0;
    char **ptr, **fontdirs;
    char *fontPath = NULL;

    if (p1 != NULL) {
        ptr = p1;
        while (*ptr++ != NULL) len1++;
    }
    if (p2 != NULL) {
        ptr = p2;

        while (*ptr++ != NULL) len2++;
    }
    if (p3 != NULL) {
        ptr = p3;
        while (*ptr++ != NULL) len3++;
    }
    totalLen = len1+len2+len3;
    fontdirs = (char**)calloc(totalLen, sizeof(char*));
    if (fontdirs == NULL) {
        return NULL;
    }

    for (i=0; i < len1; i++) {
        if (noType1 && strstr(p1[i], "Type1") != NULL) {
            continue;
        }
        fontdirs[numDirs++] = p1[i];
    }

    currLen = numDirs; /* only compare against previous path dirs */
    for (i=0; i < len2; i++) {
        if (noType1 && strstr(p2[i], "Type1") != NULL) {
            continue;
        }
        found = 0;
        for (j=0; j < currLen; j++) {
            if (strcmp(fontdirs[j], p2[i]) == 0) {
                found = 1;
                break;
            }
        }
        if (!found) {
           fontdirs[numDirs++] = p2[i];
        }
    }

    currLen = numDirs; /* only compare against previous path dirs */
    for (i=0; i < len3; i++) {
        if (noType1 && strstr(p3[i], "Type1") != NULL) {
            continue;
        }
        found = 0;
        for (j=0; j < currLen; j++) {
            if (strcmp(fontdirs[j], p3[i]) == 0) {
                found = 1;
                break;
            }
        }
        if (!found) {
           fontdirs[numDirs++] = p3[i];
        }
    }

    /* Now fontdirs contains unique dirs and numDirs records how many.
     * What we don't know is if they all exist. On reflection I think
     * this isn't an issue, so for now I will return all these locations,
     * converted to one string */
    for (i=0; i<numDirs; i++) {
        pathLen += (strlen(fontdirs[i]) + 1);
    }
    if (pathLen > 0 && (fontPath = malloc(pathLen))) {
        *fontPath = '\0';
        for (i = 0; i<numDirs; i++) {
            if (i != 0) {
                strcat(fontPath, ":");
            }
            strcat(fontPath, fontdirs[i]);
        }
    }
    free (fontdirs);

    return fontPath;
}

/*
 * The goal of this function is to find all "system" fonts which
 * are needed by the JRE to display text in supported locales etc, and
 * to support APIs which allow users to enumerate all system fonts and use
 * them from their Java applications.
 * The preferred mechanism is now using the new "fontconfig" library
 * This exists on newer versions of Linux and Solaris (S10 and above)
 * The library is dynamically located. The results are merged with
 * a set of "known" locations and with the X11 font path, if running in
 * a local X11 environment.
 * The hardwired paths are built into the JDK binary so as new font locations
 * are created on a host plaform for them to be located by the JRE they will
 * need to be added ito the host's font configuration database, typically
 * /etc/fonts/local.conf, and to ensure that directory contains a fonts.dir
 * NB: Fontconfig also depends heavily for performance on the host O/S
 * maintaining up to date caches.
 * This is consistent with the requirements of the desktop environments
 * on these OSes.
 * This also frees us from X11 APIs as JRE is required to function in
 * a "headless" mode where there is no Xserver.
 */
static char *getPlatformFontPathChars(JNIEnv *env, jboolean noType1, jboolean isX11) {

    char **fcdirs = NULL, **x11dirs = NULL, **knowndirs = NULL, *path = NULL;

    /* As of 1.5 we try to use fontconfig on both Solaris and Linux.
     * If its not available NULL is returned.
     */
    fcdirs = getFontConfigLocations();

#if defined(__linux__)
    knowndirs = fullLinuxFontPath;
#elif defined(_AIX)
    knowndirs = fullAixFontPath;
#endif
    /* REMIND: this code requires to be executed when the GraphicsEnvironment
     * is already initialised. That is always true, but if it were not so,
     * this code could throw an exception and the fontpath would fail to
     * be initialised.
     */
#ifndef HEADLESS
    if (isX11) { // The following only works in an x11 environment.
#if defined(__linux__)
    /* There's no headless build on linux ... */
    if (!AWTIsHeadless()) { /* .. so need to call a function to check */
#endif
      /* Using the X11 font path to locate font files is now a fallback
       * useful only if fontconfig failed, or is incomplete. So we could
       * remove this code completely and the consequences should be rare
       * and non-fatal. If this happens, then the calling Java code can
       * be modified to no longer require that the AWT lock (the X11GE)
       * be initialised prior to calling this code.
       */
    AWT_LOCK();
    if (isDisplayLocal(env)) {
        x11dirs = getX11FontPath();
    }
    AWT_UNLOCK();
#if defined(__linux__)
    }
#endif
    }
#endif /* !HEADLESS */
    path = mergePaths(fcdirs, x11dirs, knowndirs, noType1);
    if (fcdirs != NULL) {
        char **p = fcdirs;
        while (*p != NULL)  free(*p++);
        free(fcdirs);
    }

    if (x11dirs != NULL) {
        char **p = x11dirs;
        while (*p != NULL) free(*p++);
        free(x11dirs);
    }

    return path;
}

JNIEXPORT jstring JNICALL Java_sun_awt_FcFontManager_getFontPathNative
(JNIEnv *env, jobject thiz, jboolean noType1, jboolean isX11) {
    jstring ret;
    static char *ptr = NULL; /* retain result across calls */

    if (ptr == NULL) {
        ptr = getPlatformFontPathChars(env, noType1, isX11);
    }
    ret = (*env)->NewStringUTF(env, ptr);
    return ret;
}

#include <dlfcn.h>

#include <fontconfig/fontconfig.h>


static void* openFontConfig() {

    char *homeEnv;
    static char *homeEnvStr = "HOME="; /* must be static */
    void* libfontconfig = NULL;

    /* Private workaround to not use fontconfig library.
     * May be useful during testing/debugging
     */
    char *useFC = getenv("USE_J2D_FONTCONFIG");
    if (useFC != NULL && !strcmp(useFC, "no")) {
        return NULL;
    }

#if defined(_AIX)
    /* On AIX, fontconfig is not a standard package supported by IBM.
     * instead it has to be installed from the "AIX Toolbox for Linux Applications"
     * site http://www-03.ibm.com/systems/power/software/aix/linux/toolbox/alpha.html
     * and will be installed under /opt/freeware/lib/libfontconfig.a.
     * Notice that the archive contains the real 32- and 64-bit shared libraries.
     * We first try to load 'libfontconfig.so' from the default library path in the
     * case the user has installed a private version of the library and if that
     * doesn't succeed, we try the version from /opt/freeware/lib/libfontconfig.a
     */
    libfontconfig = dlopen("libfontconfig.so", RTLD_LOCAL|RTLD_LAZY);
    if (libfontconfig == NULL) {
        libfontconfig = dlopen("/opt/freeware/lib/libfontconfig.a(libfontconfig.so.1)", RTLD_MEMBER|RTLD_LOCAL|RTLD_LAZY);
        if (libfontconfig == NULL) {
            return NULL;
        }
    }
#else
    /* 64 bit sparc should pick up the right version from the lib path.
     * New features may be added to libfontconfig, this is expected to
     * be compatible with old features, but we may need to start
     * distinguishing the library version, to know whether to expect
     * certain symbols - and functionality - to be available.
     * Also add explicit search for .so.1 in case .so symlink doesn't exist.
     */
    libfontconfig = dlopen(FONTCONFIG_DLL_VERSIONED, RTLD_LOCAL|RTLD_LAZY);
    if (libfontconfig == NULL) {
        libfontconfig = dlopen(FONTCONFIG_DLL, RTLD_LOCAL|RTLD_LAZY);
        if (libfontconfig == NULL) {
            return NULL;
        }
    }
#endif

    /* Version 1.0 of libfontconfig crashes if HOME isn't defined in
     * the environment. This should generally never happen, but we can't
     * control it, and can't control the version of fontconfig, so iff
     * its not defined we set it to an empty value which is sufficient
     * to prevent a crash. I considered unsetting it before exit, but
     * it doesn't appear to work on Solaris, so I will leave it set.
     */
    homeEnv = getenv("HOME");
    if (homeEnv == NULL) {
        putenv(homeEnvStr);
    }

    return libfontconfig;
}

typedef void* (FcFiniFuncType)();

static void closeFontConfig(void* libfontconfig, jboolean fcFini) {

  /* NB FcFini is not in (eg) the Solaris 10 version of fontconfig. Its not
   * clear if this means we are really leaking resources in those cases
   * but it seems we should call this function when its available.
   * But since the Swing GTK code may be still accessing the lib, its probably
   * safest for now to just let this "leak" rather than potentially
   * concurrently free global data still in use by other code.
   */
#if 0
    if (fcFini) { /* release resources */
        FcFiniFuncType FcFini = (FcFiniFuncType)dlsym(libfontconfig, "FcFini");

        if (FcFini != NULL) {
            (*FcFini)();
        }
    }
#endif
    dlclose(libfontconfig);
}

typedef FcConfig* (*FcInitLoadConfigFuncType)();
typedef FcPattern* (*FcPatternBuildFuncType)(FcPattern *orig, ...);
typedef FcObjectSet* (*FcObjectSetFuncType)(const char *first, ...);
typedef FcFontSet* (*FcFontListFuncType)(FcConfig *config,
                                         FcPattern *p,
                                         FcObjectSet *os);
typedef FcResult (*FcPatternGetBoolFuncType)(const FcPattern *p,
                                               const char *object,
                                               int n,
                                               FcBool *b);
typedef FcResult (*FcPatternGetIntegerFuncType)(const FcPattern *p,
                                                const char *object,
                                                int n,
                                                int *i);
typedef FcResult (*FcPatternGetStringFuncType)(const FcPattern *p,
                                               const char *object,
                                               int n,
                                               FcChar8 ** s);
typedef FcChar8* (*FcStrDirnameFuncType)(const FcChar8 *file);
typedef void (*FcPatternDestroyFuncType)(FcPattern *p);
typedef void (*FcFontSetDestroyFuncType)(FcFontSet *s);
typedef FcPattern* (*FcNameParseFuncType)(const FcChar8 *name);
typedef FcBool (*FcPatternAddStringFuncType)(FcPattern *p,
                                             const char *object,
                                             const FcChar8 *s);
typedef void (*FcDefaultSubstituteFuncType)(FcPattern *p);
typedef FcBool (*FcConfigSubstituteFuncType)(FcConfig *config,
                                             FcPattern *p,
                                             FcMatchKind kind);
typedef FcPattern* (*FcFontMatchFuncType)(FcConfig *config,
                                          FcPattern *p,
                                          FcResult *result);
typedef FcFontSet* (*FcFontSetCreateFuncType)();
typedef FcBool (*FcFontSetAddFuncType)(FcFontSet *s, FcPattern *font);

typedef FcResult (*FcPatternGetCharSetFuncType)(FcPattern *p,
                                                const char *object,
                                                int n,
                                                FcCharSet **c);
typedef FcFontSet* (*FcFontSortFuncType)(FcConfig *config,
                                         FcPattern *p,
                                         FcBool trim,
                                         FcCharSet **csp,
                                         FcResult *result);
typedef FcCharSet* (*FcCharSetUnionFuncType)(const FcCharSet *a,
                                             const FcCharSet *b);
typedef FcChar32 (*FcCharSetSubtractCountFuncType)(const FcCharSet *a,
                                                   const FcCharSet *b);

typedef int (*FcGetVersionFuncType)();

typedef FcStrList* (*FcConfigGetCacheDirsFuncType)(FcConfig *config);
typedef FcChar8* (*FcStrListNextFuncType)(FcStrList *list);
typedef FcChar8* (*FcStrListDoneFuncType)(FcStrList *list);

static char **getFontConfigLocations() {

    char **fontdirs;
    int numdirs = 0;
    FcInitLoadConfigFuncType FcInitLoadConfig;
    FcPatternBuildFuncType FcPatternBuild;
    FcObjectSetFuncType FcObjectSetBuild;
    FcFontListFuncType FcFontList;
    FcPatternGetStringFuncType FcPatternGetString;
    FcStrDirnameFuncType FcStrDirname;
    FcPatternDestroyFuncType FcPatternDestroy;
    FcFontSetDestroyFuncType FcFontSetDestroy;

    FcConfig *fontconfig;
    FcPattern *pattern;
    FcObjectSet *objset;
    FcFontSet *fontSet;
    FcStrList *strList;
    FcChar8 *str;
    int i, f, found, len=0;
    char **fontPath;

    void* libfontconfig = openFontConfig();

    if (libfontconfig == NULL) {
        return NULL;
    }

    FcPatternBuild     =
        (FcPatternBuildFuncType)dlsym(libfontconfig, "FcPatternBuild");
    FcObjectSetBuild   =
        (FcObjectSetFuncType)dlsym(libfontconfig, "FcObjectSetBuild");
    FcFontList         =
        (FcFontListFuncType)dlsym(libfontconfig, "FcFontList");
    FcPatternGetString =
        (FcPatternGetStringFuncType)dlsym(libfontconfig, "FcPatternGetString");
    FcStrDirname       =
        (FcStrDirnameFuncType)dlsym(libfontconfig, "FcStrDirname");
    FcPatternDestroy   =
        (FcPatternDestroyFuncType)dlsym(libfontconfig, "FcPatternDestroy");
    FcFontSetDestroy   =
        (FcFontSetDestroyFuncType)dlsym(libfontconfig, "FcFontSetDestroy");

    if (FcPatternBuild     == NULL ||
        FcObjectSetBuild   == NULL ||
        FcPatternGetString == NULL ||
        FcFontList         == NULL ||
        FcStrDirname       == NULL ||
        FcPatternDestroy   == NULL ||
        FcFontSetDestroy   == NULL) { /* problem with the library: return. */
        closeFontConfig(libfontconfig, JNI_FALSE);
        return NULL;
    }

    /* Make calls into the fontconfig library to build a search for
     * outline fonts, and to get the set of full file paths from the matches.
     * This set is returned from the call to FcFontList(..)
     * We allocate an array of char* pointers sufficient to hold all
     * the matches + 1 extra which ensures there will be a NULL after all
     * valid entries.
     * We call FcStrDirname strip the file name from the path, and
     * check if we have yet seen this directory. If not we add a pointer to
     * it into our array of char*. Note that FcStrDirname returns newly
     * allocated storage so we can use this in the return char** value.
     * Finally we clean up, freeing allocated resources, and return the
     * array of unique directories.
     */
    pattern = (*FcPatternBuild)(NULL, FC_OUTLINE, FcTypeBool, FcTrue, NULL);
    objset = (*FcObjectSetBuild)(FC_FILE, NULL);
    fontSet = (*FcFontList)(NULL, pattern, objset);
    if (fontSet == NULL) {
        /* FcFontList() may return NULL if fonts are not installed. */
        fontdirs = NULL;
    } else {
        fontdirs = (char**)calloc(fontSet->nfont+1, sizeof(char*));
        if (fontdirs == NULL) {
            (*FcFontSetDestroy)(fontSet);
            goto cleanup;
        }
        for (f=0; f < fontSet->nfont; f++) {
            FcChar8 *file;
            FcChar8 *dir;
            if ((*FcPatternGetString)(fontSet->fonts[f], FC_FILE, 0, &file) ==
                                      FcResultMatch) {
                dir = (*FcStrDirname)(file);
                found = 0;
                for (i=0;i<numdirs; i++) {
                    if (strcmp(fontdirs[i], (char*)dir) == 0) {
                        found = 1;
                        break;
                    }
                }
                if (!found) {
                    fontdirs[numdirs++] = (char*)dir;
                } else {
                    free((char*)dir);
                }
            }
        }
        /* Free fontset if one was returned */
        (*FcFontSetDestroy)(fontSet);
    }

cleanup:
    /* Free memory and close the ".so" */
    (*FcPatternDestroy)(pattern);
    closeFontConfig(libfontconfig, JNI_TRUE);
    return fontdirs;
}

/* These are copied from sun.awt.SunHints.
 * Consider initialising them as ints using JNI for more robustness.
 */
#define TEXT_AA_OFF 1
#define TEXT_AA_ON  2
#define TEXT_AA_LCD_HRGB 4
#define TEXT_AA_LCD_HBGR 5
#define TEXT_AA_LCD_VRGB 6
#define TEXT_AA_LCD_VBGR 7

JNIEXPORT jint JNICALL
Java_sun_font_FontConfigManager_getFontConfigAASettings
(JNIEnv *env, jclass obj, jstring localeStr, jstring fcNameStr) {

    FcNameParseFuncType FcNameParse;
    FcPatternAddStringFuncType FcPatternAddString;
    FcConfigSubstituteFuncType FcConfigSubstitute;
    FcDefaultSubstituteFuncType  FcDefaultSubstitute;
    FcFontMatchFuncType FcFontMatch;
    FcPatternGetBoolFuncType FcPatternGetBool;
    FcPatternGetIntegerFuncType FcPatternGetInteger;
    FcPatternDestroyFuncType FcPatternDestroy;

    FcPattern *pattern, *matchPattern;
    FcResult result;
    FcBool antialias = FcFalse;
    int rgba = 0;
    const char *locale=NULL, *fcName=NULL;
    void* libfontconfig;

    if (fcNameStr == NULL || localeStr == NULL) {
        return -1;
    }

    fcName = (*env)->GetStringUTFChars(env, fcNameStr, 0);
    if (fcName == NULL) {
        return -1;
    }
    locale = (*env)->GetStringUTFChars(env, localeStr, 0);

    if ((libfontconfig = openFontConfig()) == NULL) {
        (*env)->ReleaseStringUTFChars(env, fcNameStr, (const char*)fcName);
        if (locale) {
            (*env)->ReleaseStringUTFChars(env, localeStr,(const char*)locale);
        }
        return -1;
    }

    FcNameParse = (FcNameParseFuncType)dlsym(libfontconfig, "FcNameParse");
    FcPatternAddString =
        (FcPatternAddStringFuncType)dlsym(libfontconfig, "FcPatternAddString");
    FcConfigSubstitute =
        (FcConfigSubstituteFuncType)dlsym(libfontconfig, "FcConfigSubstitute");
    FcDefaultSubstitute = (FcDefaultSubstituteFuncType)
        dlsym(libfontconfig, "FcDefaultSubstitute");
    FcFontMatch = (FcFontMatchFuncType)dlsym(libfontconfig, "FcFontMatch");
    FcPatternGetBool = (FcPatternGetBoolFuncType)
        dlsym(libfontconfig, "FcPatternGetBool");
    FcPatternGetInteger = (FcPatternGetIntegerFuncType)
        dlsym(libfontconfig, "FcPatternGetInteger");
    FcPatternDestroy =
        (FcPatternDestroyFuncType)dlsym(libfontconfig, "FcPatternDestroy");

    if (FcNameParse          == NULL ||
        FcPatternAddString   == NULL ||
        FcConfigSubstitute   == NULL ||
        FcDefaultSubstitute  == NULL ||
        FcFontMatch          == NULL ||
        FcPatternGetBool     == NULL ||
        FcPatternGetInteger  == NULL ||
        FcPatternDestroy     == NULL) { /* problem with the library: return. */

        (*env)->ReleaseStringUTFChars(env, fcNameStr, (const char*)fcName);
        if (locale) {
            (*env)->ReleaseStringUTFChars(env, localeStr,(const char*)locale);
        }
        closeFontConfig(libfontconfig, JNI_FALSE);
        return -1;
    }


    pattern = (*FcNameParse)((FcChar8 *)fcName);
    if (locale != NULL) {
        (*FcPatternAddString)(pattern, FC_LANG, (unsigned char*)locale);
    }
    (*FcConfigSubstitute)(NULL, pattern, FcMatchPattern);
    (*FcDefaultSubstitute)(pattern);
    matchPattern = (*FcFontMatch)(NULL, pattern, &result);
    /* Perhaps should call FcFontRenderPrepare() here as some pattern
     * elements might change as a result of that call, but I'm not seeing
     * any difference in testing.
     */
    if (matchPattern) {
        (*FcPatternGetBool)(matchPattern, FC_ANTIALIAS, 0, &antialias);
        (*FcPatternGetInteger)(matchPattern, FC_RGBA, 0, &rgba);
        (*FcPatternDestroy)(matchPattern);
    }
    (*FcPatternDestroy)(pattern);

    (*env)->ReleaseStringUTFChars(env, fcNameStr, (const char*)fcName);
    if (locale) {
        (*env)->ReleaseStringUTFChars(env, localeStr, (const char*)locale);
    }
    closeFontConfig(libfontconfig, JNI_TRUE);

    if (antialias == FcFalse) {
        return TEXT_AA_OFF;
    } else if (rgba <= FC_RGBA_UNKNOWN || rgba >= FC_RGBA_NONE) {
        return TEXT_AA_ON;
    } else {
        switch (rgba) {
        case FC_RGBA_RGB : return TEXT_AA_LCD_HRGB;
        case FC_RGBA_BGR : return TEXT_AA_LCD_HBGR;
        case FC_RGBA_VRGB : return TEXT_AA_LCD_VRGB;
        case FC_RGBA_VBGR : return TEXT_AA_LCD_VBGR;
        default : return TEXT_AA_LCD_HRGB; // should not get here.
        }
    }
}

JNIEXPORT jint JNICALL
Java_sun_font_FontConfigManager_getFontConfigVersion
    (JNIEnv *env, jclass obj) {

    void* libfontconfig;
    FcGetVersionFuncType FcGetVersion;
    int version = 0;

    if ((libfontconfig = openFontConfig()) == NULL) {
        return 0;
    }

    FcGetVersion = (FcGetVersionFuncType)dlsym(libfontconfig, "FcGetVersion");

    if (FcGetVersion == NULL) {
        closeFontConfig(libfontconfig, JNI_FALSE);
        return 0;
    }
    version = (*FcGetVersion)();
    closeFontConfig(libfontconfig, JNI_FALSE);

    return version;
}


JNIEXPORT void JNICALL
Java_sun_font_FontConfigManager_getFontConfig
(JNIEnv *env, jclass obj, jstring localeStr, jobject fcInfoObj,
 jobjectArray fcCompFontArray,  jboolean includeFallbacks) {

    FcNameParseFuncType FcNameParse;
    FcPatternAddStringFuncType FcPatternAddString;
    FcConfigSubstituteFuncType FcConfigSubstitute;
    FcDefaultSubstituteFuncType  FcDefaultSubstitute;
    FcFontMatchFuncType FcFontMatch;
    FcPatternGetStringFuncType FcPatternGetString;
    FcPatternDestroyFuncType FcPatternDestroy;
    FcPatternGetCharSetFuncType FcPatternGetCharSet;
    FcFontSortFuncType FcFontSort;
    FcFontSetDestroyFuncType FcFontSetDestroy;
    FcCharSetUnionFuncType FcCharSetUnion;
    FcCharSetSubtractCountFuncType FcCharSetSubtractCount;
    FcGetVersionFuncType FcGetVersion;
    FcConfigGetCacheDirsFuncType FcConfigGetCacheDirs;
    FcStrListNextFuncType FcStrListNext;
    FcStrListDoneFuncType FcStrListDone;

    int i, arrlen;
    jobject fcCompFontObj;
    jstring fcNameStr, jstr;
    const char *locale, *fcName;
    FcPattern *pattern;
    FcResult result;
    void* libfontconfig;
    jfieldID fcNameID, fcFirstFontID, fcAllFontsID, fcVersionID, fcCacheDirsID;
    jfieldID familyNameID, styleNameID, fullNameID, fontFileID;
    jmethodID fcFontCons;
    char* debugMinGlyphsStr = getenv("J2D_DEBUG_MIN_GLYPHS");
    jclass fcInfoClass;
    jclass fcCompFontClass;
    jclass fcFontClass;

    CHECK_NULL(fcInfoObj);
    CHECK_NULL(fcCompFontArray);

    fcInfoClass =
        (*env)->FindClass(env, "sun/font/FontConfigManager$FontConfigInfo");
    CHECK_NULL(fcInfoClass);
    fcCompFontClass =
        (*env)->FindClass(env, "sun/font/FontConfigManager$FcCompFont");
    CHECK_NULL(fcCompFontClass);
    fcFontClass =
         (*env)->FindClass(env, "sun/font/FontConfigManager$FontConfigFont");
    CHECK_NULL(fcFontClass);


    CHECK_NULL(fcVersionID = (*env)->GetFieldID(env, fcInfoClass, "fcVersion", "I"));
    CHECK_NULL(fcCacheDirsID = (*env)->GetFieldID(env, fcInfoClass, "cacheDirs",
                                                  "[Ljava/lang/String;"));
    CHECK_NULL(fcNameID = (*env)->GetFieldID(env, fcCompFontClass,
                                             "fcName", "Ljava/lang/String;"));
    CHECK_NULL(fcFirstFontID = (*env)->GetFieldID(env, fcCompFontClass, "firstFont",
                                        "Lsun/font/FontConfigManager$FontConfigFont;"));
    CHECK_NULL(fcAllFontsID = (*env)->GetFieldID(env, fcCompFontClass, "allFonts",
                                        "[Lsun/font/FontConfigManager$FontConfigFont;"));
    CHECK_NULL(fcFontCons = (*env)->GetMethodID(env, fcFontClass, "<init>", "()V"));
    CHECK_NULL(familyNameID = (*env)->GetFieldID(env, fcFontClass,
                                      "familyName", "Ljava/lang/String;"));
    CHECK_NULL(styleNameID = (*env)->GetFieldID(env, fcFontClass,
                                    "styleStr", "Ljava/lang/String;"));
    CHECK_NULL(fullNameID = (*env)->GetFieldID(env, fcFontClass,
                                    "fullName", "Ljava/lang/String;"));
    CHECK_NULL(fontFileID = (*env)->GetFieldID(env, fcFontClass,
                                    "fontFile", "Ljava/lang/String;"));

    if ((libfontconfig = openFontConfig()) == NULL) {
        return;
    }

    FcNameParse = (FcNameParseFuncType)dlsym(libfontconfig, "FcNameParse");
    FcPatternAddString =
        (FcPatternAddStringFuncType)dlsym(libfontconfig, "FcPatternAddString");
    FcConfigSubstitute =
        (FcConfigSubstituteFuncType)dlsym(libfontconfig, "FcConfigSubstitute");
    FcDefaultSubstitute = (FcDefaultSubstituteFuncType)
        dlsym(libfontconfig, "FcDefaultSubstitute");
    FcFontMatch = (FcFontMatchFuncType)dlsym(libfontconfig, "FcFontMatch");
    FcPatternGetString =
        (FcPatternGetStringFuncType)dlsym(libfontconfig, "FcPatternGetString");
    FcPatternDestroy =
        (FcPatternDestroyFuncType)dlsym(libfontconfig, "FcPatternDestroy");
    FcPatternGetCharSet =
        (FcPatternGetCharSetFuncType)dlsym(libfontconfig,
                                           "FcPatternGetCharSet");
    FcFontSort =
        (FcFontSortFuncType)dlsym(libfontconfig, "FcFontSort");
    FcFontSetDestroy =
        (FcFontSetDestroyFuncType)dlsym(libfontconfig, "FcFontSetDestroy");
    FcCharSetUnion =
        (FcCharSetUnionFuncType)dlsym(libfontconfig, "FcCharSetUnion");
    FcCharSetSubtractCount =
        (FcCharSetSubtractCountFuncType)dlsym(libfontconfig,
                                              "FcCharSetSubtractCount");
    FcGetVersion = (FcGetVersionFuncType)dlsym(libfontconfig, "FcGetVersion");

    if (FcNameParse          == NULL ||
        FcPatternAddString   == NULL ||
        FcConfigSubstitute   == NULL ||
        FcDefaultSubstitute  == NULL ||
        FcFontMatch          == NULL ||
        FcPatternGetString   == NULL ||
        FcPatternDestroy     == NULL ||
        FcPatternGetCharSet  == NULL ||
        FcFontSetDestroy     == NULL ||
        FcCharSetUnion       == NULL ||
        FcGetVersion         == NULL ||
        FcCharSetSubtractCount == NULL) {/* problem with the library: return.*/
        closeFontConfig(libfontconfig, JNI_FALSE);
        return;
    }

    (*env)->SetIntField(env, fcInfoObj, fcVersionID, (*FcGetVersion)());

    /* Optionally get the cache dir locations. This isn't
     * available until v 2.4.x, but this is OK since on those later versions
     * we can check the time stamps on the cache dirs to see if we
     * are out of date. There are a couple of assumptions here. First
     * that the time stamp on the directory changes when the contents are
     * updated. Secondly that the locations don't change. The latter is
     * most likely if a new version of fontconfig is installed, but we also
     * invalidate the cache if we detect that. Arguably even that is "rare",
     * and most likely is tied to an OS upgrade which gets a new file anyway.
     */
    FcConfigGetCacheDirs =
        (FcConfigGetCacheDirsFuncType)dlsym(libfontconfig,
                                            "FcConfigGetCacheDirs");
    FcStrListNext =
        (FcStrListNextFuncType)dlsym(libfontconfig, "FcStrListNext");
    FcStrListDone =
        (FcStrListDoneFuncType)dlsym(libfontconfig, "FcStrListDone");
    if (FcStrListNext != NULL && FcStrListDone != NULL &&
        FcConfigGetCacheDirs != NULL) {

        FcStrList* cacheDirs;
        FcChar8* cacheDir;
        int cnt = 0;
        jobject cacheDirArray =
            (*env)->GetObjectField(env, fcInfoObj, fcCacheDirsID);
        int max = (*env)->GetArrayLength(env, cacheDirArray);

        cacheDirs = (*FcConfigGetCacheDirs)(NULL);
        if (cacheDirs != NULL) {
            while ((cnt < max) && (cacheDir = (*FcStrListNext)(cacheDirs))) {
                jstr = (*env)->NewStringUTF(env, (const char*)cacheDir);
                JNU_CHECK_EXCEPTION(env);

                (*env)->SetObjectArrayElement(env, cacheDirArray, cnt++, jstr);
                (*env)->DeleteLocalRef(env, jstr);
            }
            (*FcStrListDone)(cacheDirs);
        }
    }

    locale = (*env)->GetStringUTFChars(env, localeStr, 0);
    if (locale == NULL) {
        (*env)->ExceptionClear(env);
        JNU_ThrowOutOfMemoryError(env, "Could not create locale");
        return;
    }

    arrlen = (*env)->GetArrayLength(env, fcCompFontArray);
    for (i=0; i<arrlen; i++) {
        FcFontSet* fontset;
        int fn, j, fontCount, nfonts;
        unsigned int minGlyphs;
        FcChar8 **family, **styleStr, **fullname, **file;
        jarray fcFontArr = NULL;
        FcCharSet *unionCharset = NULL;

        fcCompFontObj = (*env)->GetObjectArrayElement(env, fcCompFontArray, i);
        fcNameStr =
            (jstring)((*env)->GetObjectField(env, fcCompFontObj, fcNameID));
        fcName = (*env)->GetStringUTFChars(env, fcNameStr, 0);
        if (fcName == NULL) {
            (*env)->DeleteLocalRef(env, fcCompFontObj);
            (*env)->DeleteLocalRef(env, fcNameStr);
            continue;
        }
        pattern = (*FcNameParse)((FcChar8 *)fcName);
        (*env)->ReleaseStringUTFChars(env, fcNameStr, (const char*)fcName);
        (*env)->DeleteLocalRef(env, fcNameStr);
        if (pattern == NULL) {
            closeFontConfig(libfontconfig, JNI_FALSE);
            if (locale) {
                (*env)->ReleaseStringUTFChars(env, localeStr, (const char*)locale);
            }
            return;
        }

        /* locale may not usually be necessary as fontconfig appears to apply
         * this anyway based on the user's environment. However we want
         * to use the value of the JDK startup locale so this should take
         * care of it.
         */
        if (locale != NULL) {
            (*FcPatternAddString)(pattern, FC_LANG, (unsigned char*)locale);
        }
        (*FcConfigSubstitute)(NULL, pattern, FcMatchPattern);
        (*FcDefaultSubstitute)(pattern);
        fontset = (*FcFontSort)(NULL, pattern, FcTrue, NULL, &result);
        if (fontset == NULL) {
            (*FcPatternDestroy)(pattern);
            closeFontConfig(libfontconfig, JNI_FALSE);
            if (locale) {
                (*env)->ReleaseStringUTFChars(env, localeStr, (const char*)locale);
            }
            return;
        }

        /* fontconfig returned us "nfonts". If we are just getting the
         * first font, we set nfont to zero. Otherwise we use "nfonts".
         * Next create separate C arrrays of length nfonts for family file etc.
         * Inspect the returned fonts and the ones we like (adds enough glyphs)
         * are added to the arrays and we increment 'fontCount'.
         */
        nfonts = fontset->nfont;
        family   = (FcChar8**)calloc(nfonts, sizeof(FcChar8*));
        styleStr = (FcChar8**)calloc(nfonts, sizeof(FcChar8*));
        fullname = (FcChar8**)calloc(nfonts, sizeof(FcChar8*));
        file     = (FcChar8**)calloc(nfonts, sizeof(FcChar8*));
        if (family == NULL || styleStr == NULL ||
            fullname == NULL || file == NULL) {
            if (family != NULL) {
                free(family);
            }
            if (styleStr != NULL) {
                free(styleStr);
            }
            if (fullname != NULL) {
                free(fullname);
            }
            if (file != NULL) {
                free(file);
            }
            (*FcPatternDestroy)(pattern);
            (*FcFontSetDestroy)(fontset);
            closeFontConfig(libfontconfig, JNI_FALSE);
            if (locale) {
                (*env)->ReleaseStringUTFChars(env, localeStr, (const char*)locale);
            }
            return;
        }
        fontCount = 0;
        minGlyphs = 20;
        if (debugMinGlyphsStr != NULL) {
            int val = minGlyphs;
            sscanf(debugMinGlyphsStr, "%5d", &val);
            if (val >= 0 && val <= 65536) {
                minGlyphs = val;
            }
        }

        for (j=0; j<nfonts; j++) {
            FcPattern *fontPattern = fontset->fonts[j];
            FcChar8 *fontformat;
            FcCharSet *charset = NULL;

            fontformat = NULL;
            (*FcPatternGetString)(fontPattern, FC_FONTFORMAT, 0, &fontformat);
            /* We only want TrueType fonts but some Linuxes still depend
             * on Type 1 fonts for some Locale support, so we'll allow
             * them there.
             */
            if (fontformat != NULL
                && (strcmp((char*)fontformat, "TrueType") != 0)
#if defined(__linux__) || defined(_AIX)
                && (strcmp((char*)fontformat, "Type 1") != 0)
                && (strcmp((char*)fontformat, "CFF") != 0)
#endif
             ) {
                continue;
            }
            result = (*FcPatternGetCharSet)(fontPattern,
                                            FC_CHARSET, 0, &charset);
            if (result != FcResultMatch) {
                free(family);
                free(fullname);
                free(styleStr);
                free(file);
                (*FcPatternDestroy)(pattern);
                (*FcFontSetDestroy)(fontset);
                closeFontConfig(libfontconfig, JNI_FALSE);
                if (locale) {
                    (*env)->ReleaseStringUTFChars(env, localeStr, (const char*)locale);
                }
                return;
            }

            /* We don't want 20 or 30 fonts, so once we hit 10 fonts,
             * then require that they really be adding value. Too many
             * adversely affects load time for minimal value-add.
             * This is still likely far more than we've had in the past.
             */
            if (j==10) {
                minGlyphs = 50;
            }
            if (unionCharset == NULL) {
                unionCharset = charset;
            } else {
                if ((*FcCharSetSubtractCount)(charset, unionCharset)
                    > minGlyphs) {
                    unionCharset = (* FcCharSetUnion)(unionCharset, charset);
                } else {
                    continue;
                }
            }

            fontCount++; // found a font we will use.
            (*FcPatternGetString)(fontPattern, FC_FILE, 0, &file[j]);
            (*FcPatternGetString)(fontPattern, FC_FAMILY, 0, &family[j]);
            (*FcPatternGetString)(fontPattern, FC_STYLE, 0, &styleStr[j]);
            (*FcPatternGetString)(fontPattern, FC_FULLNAME, 0, &fullname[j]);
            if (!includeFallbacks) {
                break;
            }
            if (fontCount == 254) {
                break; // CompositeFont will only use up to 254 slots from here.
            }
        }

        /* Once we get here 'fontCount' is the number of returned fonts
         * we actually want to use, so we create 'fcFontArr' of that length.
         * The non-null entries of "family[]" etc are those fonts.
         * Then loop again over all nfonts adding just those non-null ones
         * to 'fcFontArr'. If its null (we didn't want the font)
         * then we don't enter the main body.
         * So we should never get more than 'fontCount' entries.
         */
        if (includeFallbacks) {
            fcFontArr =
                (*env)->NewObjectArray(env, fontCount, fcFontClass, NULL);
            if (IS_NULL(fcFontArr)) {
                free(family);
                free(fullname);
                free(styleStr);
                free(file);
                (*FcPatternDestroy)(pattern);
                (*FcFontSetDestroy)(fontset);
                closeFontConfig(libfontconfig, JNI_FALSE);
                if (locale) {
                    (*env)->ReleaseStringUTFChars(env, localeStr, (const char*)locale);
                }
                return;
            }
            (*env)->SetObjectField(env,fcCompFontObj, fcAllFontsID, fcFontArr);
        }
        fn=0;

        for (j=0;j<nfonts;j++) {
            if (family[j] != NULL) {
                jobject fcFont =
                    (*env)->NewObject(env, fcFontClass, fcFontCons);
                if (IS_NULL(fcFont)) break;
                jstr = (*env)->NewStringUTF(env, (const char*)family[j]);
                if (IS_NULL(jstr)) break;
                (*env)->SetObjectField(env, fcFont, familyNameID, jstr);
                (*env)->DeleteLocalRef(env, jstr);
                if (file[j] != NULL) {
                    jstr = (*env)->NewStringUTF(env, (const char*)file[j]);
                    if (IS_NULL(jstr)) break;
                    (*env)->SetObjectField(env, fcFont, fontFileID, jstr);
                    (*env)->DeleteLocalRef(env, jstr);
                }
                if (styleStr[j] != NULL) {
                    jstr = (*env)->NewStringUTF(env, (const char*)styleStr[j]);
                    if (IS_NULL(jstr)) break;
                    (*env)->SetObjectField(env, fcFont, styleNameID, jstr);
                    (*env)->DeleteLocalRef(env, jstr);
                }
                if (fullname[j] != NULL) {
                    jstr = (*env)->NewStringUTF(env, (const char*)fullname[j]);
                    if (IS_NULL(jstr)) break;
                    (*env)->SetObjectField(env, fcFont, fullNameID, jstr);
                    (*env)->DeleteLocalRef(env, jstr);
                }
                if (fn==0) {
                    (*env)->SetObjectField(env, fcCompFontObj,
                                           fcFirstFontID, fcFont);
                }
                if (includeFallbacks) {
                    (*env)->SetObjectArrayElement(env, fcFontArr, fn++,fcFont);
                } else {
                    (*env)->DeleteLocalRef(env, fcFont);
                    break;
                }
                (*env)->DeleteLocalRef(env, fcFont);
            }
        }
        if (includeFallbacks) {
            (*env)->DeleteLocalRef(env, fcFontArr);
        }
        (*env)->DeleteLocalRef(env, fcCompFontObj);
        (*FcFontSetDestroy)(fontset);
        (*FcPatternDestroy)(pattern);
        free(family);
        free(styleStr);
        free(fullname);
        free(file);
    }

    /* release resources and close the ".so" */

    if (locale) {
        (*env)->ReleaseStringUTFChars(env, localeStr, (const char*)locale);
    }
    closeFontConfig(libfontconfig, JNI_TRUE);
}
