/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Converts a single string command line to the traditional argc, argv.
 * There are rules which govern the breaking of the arguments, and
 * these rules are embodied in the regression tests below, and duplicated
 * in the jdk regression tests.
 */

#include <assert.h>

#ifndef IDE_STANDALONE
#include "java.h"
#include "jni.h"
#include "jli_util.h"
#else /* IDE_STANDALONE */
// The defines we need for stand alone testing
#include <stdio.h>
#include <stdlib.h>
#include <Windows.h>
#define JNI_TRUE       TRUE
#define JNI_FALSE      FALSE
#define JLI_MemRealloc realloc
#define JLI_StringDup  _strdup
#define JLI_MemFree    free
#define jboolean       boolean
typedef struct  {
    char* arg;
    boolean has_wildcard;
} StdArg ;
#endif
static StdArg *stdargs;
static int    stdargc;

static int copyCh(USHORT ch, char* dest) {
    if (HIBYTE(ch) == 0) {
        *dest = (char)ch;
        return 1;
    } else {
        *((USHORT *)dest) = ch;
        return 2;
    }
}

static char* next_arg(char* cmdline, char* arg, jboolean* wildcard) {

    char* src = cmdline;
    char* dest = arg;
    jboolean separator = JNI_FALSE;
    int quotes = 0;
    int slashes = 0;

    // "prev"/"ch" may contain either a single byte, or a double byte
    // character encoded in CP_ACP.
    USHORT prev = 0;
    USHORT ch = 0;
    int i;
    jboolean done = JNI_FALSE;
    ptrdiff_t charLength;

    *wildcard = JNI_FALSE;
    while (!done) {
        charLength = CharNextExA(CP_ACP, src, 0) - src;
        if (charLength == 0) {
            break;
        } else if (charLength == 1) {
            ch = (USHORT)(UCHAR)src[0];
        } else {
            ch = ((USHORT *)src)[0];
        }

        switch (ch) {
        case L'"':
            if (separator) {
                done = JNI_TRUE;
                break;
            }
            if (prev == L'\\') {
                for (i = 1; i < slashes; i += 2) {
                    dest += copyCh(prev, dest);
                }
                if (slashes % 2 == 1) {
                    dest += copyCh(ch, dest);
                } else {
                    quotes++;
                }
            } else if (prev == L'"' && quotes % 2 == 0) {
                quotes++;
                dest += copyCh(ch, dest); // emit every other consecutive quote
            } else if (quotes == 0) {
                quotes++; // starting quote
            } else {
                quotes--; // matching quote
            }
            slashes = 0;
            break;

        case L'\\':
            slashes++;
            if (separator) {
                done = JNI_TRUE;
                separator = JNI_FALSE;
            }
            break;

        case L' ':
        case L'\t':
            if (prev == L'\\') {
                for (i = 0 ; i < slashes; i++) {
                    dest += copyCh(prev, dest);
                }
            }
            if (quotes % 2 == 1) {
                dest += copyCh(ch, dest);
            } else {
                separator = JNI_TRUE;
            }
            slashes = 0;
            break;

        case L'*':
        case L'?':
            if (separator) {
                done = JNI_TRUE;
                separator = JNI_FALSE;
                break;
            }
            if (quotes % 2 == 0) {
                *wildcard = JNI_TRUE;
            }
            if (prev == L'\\') {
                for (i = 0 ; i < slashes ; i++) {
                    dest += copyCh(prev, dest);
                }
            }
            dest += copyCh(ch, dest);
            slashes = 0;
            break;

        default:
            if (prev == L'\\') {
                for (i = 0 ; i < slashes ; i++) {
                    dest += copyCh(prev, dest);
                }
                dest += copyCh(ch, dest);
            } else if (separator) {
                done = JNI_TRUE;
            } else {
                dest += copyCh(ch, dest);
            }
            slashes = 0;
        }

        if (!done) {
            prev = ch;
            src += charLength;
        }
    }
    if (prev == L'\\') {
        for (i = 0; i < slashes; i++) {
            dest += copyCh(prev, dest);
        }
    }
    *dest = 0;
    return done ? src : NULL;
}

JNIEXPORT int JNICALL
JLI_GetStdArgc() {
    return stdargc;
}

JNIEXPORT StdArg* JNICALL
JLI_GetStdArgs() {
    return stdargs;
}

JNIEXPORT void JNICALL
JLI_CmdToArgs(char* cmdline) {
    int nargs = 0;
    StdArg* argv = NULL;
    jboolean wildcard = JNI_FALSE;
    char* src = cmdline, *arg = NULL;
    JLI_List argsInFile;
    size_t i, cnt;

    JLI_List envArgs = JLI_List_new(1);
    if (JLI_AddArgsFromEnvVar(envArgs, JDK_JAVA_OPTIONS)) {
        // JLI_SetTraceLauncher is not called yet
        // Show _JAVA_OPTIONS content along with JDK_JAVA_OPTIONS to aid diagnosis
        if (getenv(JLDEBUG_ENV_ENTRY)) {
            char *tmp = getenv("_JAVA_OPTIONS");
            if (NULL != tmp) {
                JLI_ReportMessage(ARG_INFO_ENVVAR, "_JAVA_OPTIONS", tmp);
            }
        }
    }
    cnt = envArgs->size + 1;
    argv = JLI_MemAlloc(cnt * sizeof(StdArg));

    // allocate arg buffer with sufficient space to receive the largest arg
    arg = JLI_StringDup(cmdline);

    src = next_arg(src, arg, &wildcard);
    // first argument is the app name, do not preprocess and make sure remains first
    argv[0].arg = JLI_StringDup(arg);
    argv[0].has_wildcard = wildcard;
    nargs++;

    for (i = 1; i < cnt; i++) {
        argv[i].arg = envArgs->elements[i - 1];
        // wildcard is not supported in argfile
        argv[i].has_wildcard = JNI_FALSE;
        nargs++;
    }
    JLI_MemFree(envArgs->elements);
    JLI_MemFree(envArgs);

    assert ((size_t) nargs == cnt);
    *arg = '\0';

    // iterate through rest of command line
    while (src != NULL) {
        src = next_arg(src, arg, &wildcard);
        argsInFile = JLI_PreprocessArg(arg, JNI_TRUE);
        if (argsInFile != NULL) {
            // resize to accommodate another Arg
            cnt = argsInFile->size;
            argv = (StdArg*) JLI_MemRealloc(argv, (nargs + cnt) * sizeof(StdArg));
            for (i = 0; i < cnt; i++) {
                argv[nargs].arg = argsInFile->elements[i];
                // wildcard is not supported in argfile
                argv[nargs].has_wildcard = JNI_FALSE;
                nargs++;
            }
            // Shallow free, we reuse the string to avoid copy
            JLI_MemFree(argsInFile->elements);
            JLI_MemFree(argsInFile);
        } else {
            // resize to accommodate another Arg
            argv = (StdArg*) JLI_MemRealloc(argv, (nargs+1) * sizeof(StdArg));
            argv[nargs].arg = JLI_StringDup(arg);
            argv[nargs].has_wildcard = wildcard;
            *arg = '\0';
            nargs++;
        }
        *arg = '\0';
    }

    JLI_MemFree(arg);

    stdargc = nargs;
    stdargs = argv;
}

#ifdef IDE_STANDALONE
void doexit(int rv) {
    printf("Hit any key to quit\n");
    int c = getchar();
    exit(rv);
}

void doabort() {
    doexit(1);
}

class Vector {
public:
    char* cmdline;
    int argc;
    char* argv[10];
    boolean wildcard[10];
    boolean enabled;

    Vector(){}
    // Initialize our test vector with the program name, argv[0]
    // and the single string command line.
    Vector(char* pname, char* cline) {
        argv[0] = pname;
        wildcard[0] = FALSE;
        cmdline = cline;
        argc = 1;
        enabled = TRUE;
    }

    // add our expected strings, the program name has already been
    // added so ignore that
    void add(char* arg, boolean w) {
        argv[argc] = arg;
        wildcard[argc] = w;
        argc++;
    }

    void disable() {
        enabled = FALSE;
    }

    // validate the returned arguments with the expected arguments, using the
    // new CmdToArgs method.
    bool check() {
        // "pgmname" rest of cmdline ie. pgmname + 2 double quotes + space + cmdline from windows
        char* cptr = (char*) malloc(strlen(argv[0]) + sizeof(char) * 3 + strlen(cmdline) + 1);
        _snprintf(cptr, MAX_PATH, "\"%s\" %s", argv[0], cmdline);
        JLI_CmdToArgs(cptr);
        free(cptr);
        StdArg *kargv = JLI_GetStdArgs();
        int     kargc = JLI_GetStdArgc();
        bool retval = true;
        printf("\n===========================\n");
        printf("cmdline=%s\n", cmdline);
        if (argc != kargc) {
            printf("*** argument count does not match\n");
            printme();
            printtest(kargc, kargv);
            doabort();
        }
        for (int i = 0 ; i < argc && retval == true ; i++) {
            if (strcmp(argv[i], kargv[i].arg) != 0) {
                printf("*** argument at [%d] don't match\n  got: %s\n  exp: %s\n",
                       i, kargv[i].arg, argv[i]);
                doabort();
            }
        }
        for (int i = 0 ; i < argc && retval == true ; i++) {
            if (wildcard[i] != kargv[i].has_wildcard) {
                printf("*** expansion flag at [%d] doesn't match\n  got: %d\n  exp: %d\n",
                       i, kargv[i].has_wildcard, wildcard[i]);
                doabort();
            }
        }
        for (int i = 0 ; i < kargc ; i++) {
            printf("k[%d]=%s\n", i, kargv[i].arg);
            printf(" [%d]=%s\n", i, argv[i]);
        }
        return retval;
    }
    void printtest(int kargc, StdArg* kargv) {
        for (int i = 0 ; i < kargc ; i++) {
            printf("k[%d]=%s\n", i, kargv[i].arg);
        }
    }
    void printme() {
        for (int i = 0 ; i < argc ; i++) {
            printf(" [%d]=%s\n", i, argv[i]);
        }
    }
};

void dotest(Vector** vectors) {
    Vector* v = vectors[0];
    for (int i = 0 ; v != NULL;) {
        if (v->enabled) {
            v->check();
        }
        v = vectors[++i];
    }
}

#define MAXV 128
int main(int argc, char* argv[]) {

    int n;
    for (n=1; n < argc; n++) {
        printf("%d %s\n", n, argv[n]);
    }
    if (n > 1) {
        JLI_CmdToArgs(GetCommandLine());
        for (n = 0; n < stdargc; n++) {
            printf(" [%d]=%s\n", n, stdargs[n].arg);
            printf(" [%d]=%s\n", n, stdargs[n].has_wildcard ? "TRUE" : "FALSE");
        }
        doexit(0);
    }

    Vector *vectors[MAXV];

    memset(vectors, 0, sizeof(vectors));
    int i = 0;
    Vector* v = new Vector(argv[0], "abcd");
    v->add("abcd", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"a b c d\"");
    v->add("a b c d", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "a\"b c d\"e");
    v->add("ab c de", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "ab\\\"cd");
    v->add("ab\"cd", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"a b c d\\\\\"");
    v->add("a b c d\\", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "ab\\\\\\\"cd");
    v->add("ab\\\"cd", FALSE);
    // v->disable();
    vectors[i++] = v;


    // Windows tests
    v = new Vector(argv[0], "a\\\\\\c");
    v->add("a\\\\\\c", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"a\\\\\\d\"");
    v->add("a\\\\\\d", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"a b c\" d e");
    v->add("a b c", FALSE);
    v->add("d", FALSE);
    v->add("e", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"ab\\\"c\"  \"\\\\\"  d");
    v->add("ab\"c", FALSE);
    v->add("\\", FALSE);
    v->add("d", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "a\\\\\\c d\"e f\"g h");
    v->add("a\\\\\\c", FALSE);
    v->add("de fg", FALSE);
    v->add("h", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "a\\\\\\\"b c d");
    v->add("a\\\"b", FALSE); // XXX "a\\\\\\\"b"
    v->add("c", FALSE);
    v->add("d", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "a\\\\\\\\\"g c\" d e"); // XXX "a\\\\\\\\\"b c\" d e"
    v->add("a\\\\\g c", FALSE); // XXX "a\\\\\\\\\"b c"
    v->add("d", FALSE);
    v->add("e", FALSE);
    // v->disable();
    vectors[i++] = v;


    // Additional tests
    v = new Vector(argv[0], "\"a b c\"\"");
    v->add("a b c\"", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"\"a b c\"\"");
    v->add("a", FALSE);
    v->add("b", FALSE);
    v->add("c", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"\"\"a b c\"\"\"");
    v->add("\"a b c\"", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"\"\"\"a b c\"\"\"\"");
    v->add("\"a", FALSE);
    v->add("b", FALSE);
    v->add("c\"", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"\"\"\"\"a b c\"\"\"\"\"");
    v->add("\"\"a b c\"\"", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"C:\\TEST A\\\\\"");
    v->add("C:\\TEST A\\", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"\"C:\\TEST A\\\\\"\"");
    v->add("C:\\TEST", FALSE);
    v->add("A\\", FALSE);
    // v->disable();
    vectors[i++] = v;


    // test if a wildcard is present
    v = new Vector(argv[0], "abc*def");
    v->add("abc*def", TRUE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"abc*def\"");
    v->add("abc*def", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "*.abc");
    v->add("*.abc", TRUE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"*.abc\"");
    v->add("*.abc", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "x.???");
    v->add("x.???", TRUE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\"x.???\"");
    v->add("x.???", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "Debug\\*");
    v->add("Debug\\*", TRUE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "Debug\\f?a");
    v->add("Debug\\f?a", TRUE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "Debug\\?a.java");
    v->add("Debug\\?a.java", TRUE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "foo *.noexts");
    v->add("foo", FALSE);
    v->add("*.noexts", TRUE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "X\\Y\\Z");
    v->add("X\\Y\\Z", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "\\X\\Y\\Z");
    v->add("\\X\\Y\\Z", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "a b");
    v->add("a", FALSE);
    v->add("b", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "a\tb");
    v->add("a", FALSE);
    v->add("b", FALSE);
    // v->disable();
    vectors[i++] = v;


    v = new Vector(argv[0], "a \t b");
    v->add("a", FALSE);
    v->add("b", FALSE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], "*\\");
    v->add("*\\", TRUE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], "*/");
    v->add("*/", TRUE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], ".\\*");
    v->add(".\\*", TRUE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], "./*");
    v->add("./*", TRUE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], ".\\*");
    v->add(".\\*", TRUE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], ".//*");
    v->add(".//*", TRUE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], "..\\..\\*");
    v->add("..\\..\\*", TRUE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], "../../*");
    v->add("../../*", TRUE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], "..\\..\\");
    v->add("..\\..\\", FALSE);
    // v->disable();
    vectors[i++] = v;

    v = new Vector(argv[0], "../../");
    v->add("../../", FALSE);
    // v->disable();
    vectors[i++] = v;

    v= new Vector(argv[0], "a b\\\\ d");
    v->add("a", FALSE);
    v->add("b\\\\", FALSE);
    v->add("d", FALSE);
    vectors[i++] = v;

    v= new Vector(argv[0], "\\\\?");
    v->add("\\\\?", TRUE);
    vectors[i++] = v;

    v= new Vector(argv[0], "\\\\*");
    v->add("\\\\*", TRUE);
    vectors[i++] = v;

    dotest(vectors);
    printf("All tests pass [%d]\n", i);
    doexit(0);
}
#endif /* IDE_STANDALONE */
