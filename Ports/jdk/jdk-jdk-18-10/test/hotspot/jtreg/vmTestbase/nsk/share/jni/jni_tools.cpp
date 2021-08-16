/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
#include <stdarg.h>
#include <string.h>
#include <ctype.h>

/*************************************************************/
#if (defined(WIN32) || defined(_WIN32))
#include <windows.h>
#else
#include <unistd.h>
#include <sys/time.h>
#endif
/*************************************************************/

#include "jni.h"

/*************************************************************/

#include "nsk_tools.h"
#include "jni_tools.h"

/*************************************************************/

extern "C" {

/*************************************************************/

int nsk_jni_check_exception(JNIEnv* jni, const char file[], int line)
{
    jthrowable throwable;

    NSK_TRACE(throwable = jni->ExceptionOccurred());
    if (throwable != NULL) {
        nsk_lcomplain(file, line, "Exception in JNI call (cleared):\n");
        NSK_TRACE(jni->ExceptionDescribe());
        NSK_TRACE(jni->ExceptionClear());
        return NSK_TRUE;
    }
    return NSK_FALSE;
}

int nsk_jni_lverify(int positive, JNIEnv* jni, int status,
                        const char file[], int line, const char format[], ...)
{
    int failure=0;
    int negative = !positive;
    va_list ap;
    va_start(ap,format);

    nsk_lvtrace(NSK_TRACE_AFTER,file,line,format,ap);
    if (status == negative) {
        nsk_lvcomplain(file,line,format,ap);
        nsk_printf("#   verified JNI assertion is FALSE\n");
        failure=1;
    }

    failure = nsk_jni_check_exception(jni, file, line) || failure;

    va_end(ap);
    return !failure;
}

int nsk_jni_lverify_void(JNIEnv* jni, const char file[], int line,
                            const char format[], ...)
{
    int failure=0;
    va_list ap;
    va_start(ap,format);

    nsk_lvtrace(NSK_TRACE_AFTER,file,line,format,ap);
    failure = nsk_jni_check_exception(jni, file, line);

    if (failure)
        nsk_lvcomplain(file,line,format,ap);

    va_end(ap);
    return !failure;
}

char *jlong_to_string(jlong value, char *string) {
    char buffer[32];
    char *pbuf, *pstr;

    pstr = string;
    if (value == 0) {
        *pstr++ = '0';
    } else {
        if (value < 0) {
            *pstr++ = '-';
            value = -value;
        }
        pbuf = buffer;
        while (value != 0) {
            *pbuf++ = '0' + (char)(value % 10);
            value = value / 10;
        }
        while (pbuf != buffer) {
            *pstr++ = *--pbuf;
        }
    }
    *pstr = '\0';

    return string;
}

char *julong_to_string(julong value, char *string) {
    char buffer[32];
    char *pbuf, *pstr;

    pstr = string;
    if (value == 0) {
        *pstr++ = '0';
    } else {
        pbuf = buffer;
        while (value != 0) {
            *pbuf++ = '0' + (char)(value % 10);
            value = value / 10;
        }
        while (pbuf != buffer) {
            *pstr++ = *--pbuf;
        }
    }
    *pstr = '\0';

    return string;
}

void mssleep(long millis) {
#if (defined(WIN32) || defined(_WIN32))
   Sleep(millis);
#else
   /* Using select for portable sleep */
   /* Not using usleep because of it's possible interaction with SIGALRM */
   struct timeval timeout;
   timeout.tv_sec = millis / 1000;
   timeout.tv_usec = (millis % 1000) * 1000;
   select(0, NULL, NULL, NULL, &timeout);
#endif
}

void
jni_print_vmargs(JavaVMInitArgs vmargs)
{
   int i = 0;

   printf("JavaVMInitArgs:\n");
   printf(" version = %d\n", vmargs.version);
   printf(" ignoreUnrecognized = %d\n", vmargs.ignoreUnrecognized);

   printf(" vmargs.nOptions = %d\n", vmargs.nOptions);
   for (i = 0; i < vmargs.nOptions; i++) {
      printf("   options[%d].optionString = %s\n", i, vmargs.options[i].optionString);
      printf("   options[%d].extraInfo = %p\n", i, vmargs.options[i].extraInfo);
   }
}

JavaVMOption*
jni_create_vmoptions(int size, char *args[], int argsCnt)
{
   int i;
   JavaVMOption *options = NULL;

   if (size <= 0)
      return options;

   options = (JavaVMOption*)calloc(size, sizeof(JavaVMOption));

   for (i=0; i<argsCnt; i++)
      options[i].optionString = args[i];

   return options;
}

/*************************************************************/

}
