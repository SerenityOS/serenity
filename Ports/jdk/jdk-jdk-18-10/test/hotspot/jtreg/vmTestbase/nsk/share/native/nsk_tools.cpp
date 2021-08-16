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

#include <stdio.h>
#include <stdarg.h>
#include <assert.h>
#include <ctype.h>
#include <string.h>
#include <errno.h>


/*************************************************************/

#include "nsk_tools.h"

/*************************************************************/

extern "C" {

/*************************************************************/

static struct {
    int verbose;
    int tracing;
    int nComplains;
} nsk_context = { NSK_FALSE, NSK_TRACE_NONE, 0 };

void nsk_setVerboseMode(int verbose) {
    nsk_context.verbose = verbose;
}

int  nsk_getVerboseMode() {
    return nsk_context.verbose;
}

void nsk_setTraceMode(int mode) {
    nsk_context.tracing = mode;
}

int  nsk_getTraceMode() {
    return nsk_context.tracing;
}

/*************************************************************/

static const char* file_basename(const char* fullname) {
    const char* p;
    const char* base = fullname;;

    if (fullname == NULL)
        return NULL;

    for (p = fullname; *p != '\0'; p++) {
        if (*p == '/' || *p == '\\')
            base = p + 1;
    }
    return base;
}

/*************************************************************/

void nsk_display(const char format[], ...) {
    va_list ap;
    va_start(ap,format);
    nsk_lvdisplay(NULL,0,format,ap);
    va_end(ap);
}

void nsk_ldisplay(const char file[], int line, const char format[], ...) {
    va_list ap;
    va_start(ap,format);
    nsk_lvdisplay(file,line,format,ap);
    va_end(ap);
}

void nsk_vdisplay(const char format[], va_list ap) {
    nsk_lvdisplay(NULL,0,format,ap);
}

void nsk_lvdisplay(const char file[], int line, const char format[], va_list ap)
{
    if (!nsk_context.verbose)
        return;
    if (file != NULL)
        (void) nsk_printf("- %s, %d: ",file_basename(file),line);
    (void) nsk_vprintf(format,ap);
}

/*************************************************************/

void nsk_complain(const char format[], ...) {
    va_list ap;
    va_start(ap,format);
    nsk_lvcomplain(NULL,0,format,ap);
    va_end(ap);
}

void nsk_lcomplain(const char file[], int line, const char format[], ...)
{
    va_list ap;
    va_start(ap,format);
    nsk_lvcomplain(file,line,format,ap);
    va_end(ap);
}

void nsk_vcomplain(const char format[], va_list ap) {
    nsk_lvcomplain(NULL,0,format,ap);
}

void nsk_lvcomplain(const char file[], int line,
    const char format[], va_list ap)
{
    char msg_buf[1024];
    nsk_context.nComplains++;
    if (!nsk_context.verbose) {
        if (nsk_context.nComplains > NSK_MAX_COMPLAINS_NON_VERBOSE) {
            return;
        }

        if (nsk_context.nComplains == NSK_MAX_COMPLAINS_NON_VERBOSE) {
            nsk_printf("# ...\n"
                       "# ERROR: too many complains, giving up to save disk space (CR 6341460)\n"
                       "# Please rerun the test with -verbose option to listen to the entire song\n");
            return;
        }
    }

    // Generate the message into a temp buffer since we can't call vfprintf on it twice,
    // and also may need to modify a copy of the message slightly.
    (void) vsnprintf(msg_buf, sizeof(msg_buf), format, ap);

    // Print a fake exception with the error for failure analysis.
    // Do this only for the first complaint.
    if (nsk_context.nComplains == 1) {
      char msg_buf2[sizeof(msg_buf)];
      char* nl_ptr;
      strncpy(msg_buf2, msg_buf, sizeof(msg_buf2));
      // Only include up to the 1st newline in the exception's error message.
      nl_ptr = strchr(msg_buf2, '\n');
      if (nl_ptr != NULL) {
        nl_ptr++;       // Skip past the newline char.
        *nl_ptr = '\0'; // Terminate the string after the newline char.
      } else if (strlen(msg_buf2) != 0) {
        msg_buf2[strlen(msg_buf2)-1] = '\n'; // Make sure we have a newline char at the end.
      }
      (void) nsk_printf("The following fake exception stacktrace is for failure analysis. \n");
      (void) nsk_printf("nsk.share.Fake_Exception_for_RULE_Creation: ");
      if (file != NULL) {
        (void) nsk_printf("(%s:%d) ", file_basename(file), line);
      }
      (void) nsk_printf(msg_buf2);
      (void) nsk_printf("\tat nsk_lvcomplain(%s:%d)\n", file_basename(__FILE__), __LINE__);
    }

    if (file != NULL) {
        (void) nsk_printf("# ERROR: %s, %d: ", file_basename(file), line);
    } else {
        (void) nsk_printf("# ERROR: ");
    }
    (void) nsk_printf(msg_buf);
}

/*************************************************************/

void nsk_ltrace(int mode, const char file[], int line, const char format[], ...) {
    va_list ap;
    va_start(ap,format);
    nsk_lvtrace(mode,file,line,format,ap);
    va_end(ap);
}

void nsk_lvtrace(int mode, const char file[], int line, const char format[], va_list ap)
{
    if ((nsk_context.tracing & mode) == 0) {
        return;
    }

    {
        const char* prefix;
        switch (mode) {
            case NSK_TRACE_BEFORE:
                prefix = ">>";
                break;
            case NSK_TRACE_AFTER:
                prefix = "<<";
                break;
            default:
                prefix = "..";
                break;
        }

        (void) nsk_printf("- %s, %d: %s ",file_basename(file),line,prefix);
        (void) nsk_vprintf(format,ap);
    }
}

/*************************************************************/

int nsk_lverify(int value, const char file[], int line, const char format[], ...)
{
    int fail=0;
    va_list ap;
    va_start(ap,format);
    nsk_lvtrace(NSK_TRACE_AFTER,file,line,format,ap);
    if (!value) {
        nsk_lvcomplain(file,line,format,ap);
        nsk_printf("#   verified assertion is FALSE\n");
        fail=1;
    };
    va_end(ap);
    return !fail;
}

/*************************************************************/

int nsk_vprintf(const char format[], va_list ap) {
    int x = vfprintf(stdout,format,ap);
    int err = fflush(stdout);
    if (err != 0) {
      printf("stdout: fflush failed - err=%d errno=%d x=%d\n", err, errno, x);
      fprintf(stderr, "stderr: fflush failed - err=%d errno=%d x=%d\n", err, errno, x);
    }
    assert(err == 0);
    return x;
}

int nsk_printf(const char format[], ...) {
    int x;
    va_list ap;
    va_start(ap,format);
    x = nsk_vprintf(format,ap);
    va_end(ap);
    return x;
}

/*************************************************************/

#define MAX_HEX_COLUMNS  255

void nsk_printHexBytes(const char indent[], int columns,
                                    size_t size, const unsigned char bytes[]) {
    char hex[MAX_HEX_COLUMNS * 3 + 1];
    char ascii[MAX_HEX_COLUMNS + 1];
    char buf[16];

    size_t i;

    if (size <= 0 || bytes == NULL)
        return;

    for (i = 0; i < size; i += columns) {
        int j;

        hex[0] = '\0';
        ascii[0] = '\0';

        for (j = 0; j < columns && (i + j) < size; j++) {
            unsigned int b = (unsigned int)bytes[i + j] & 0xFF;
            char ch = (char)bytes[i + j];

            if (!(isascii(ch) && isprint(ch))) ch = '.';
            sprintf(buf, " %02X", b);
            strcat(hex, buf);
            ascii[j] = ch;
        }

        ascii[j] = '\0';
        if (j < columns) {
            for (; j < columns; j++) {
                strcat(hex, "   ");
            }
        }

        nsk_printf("%s0x%08X:  %s    %s\n", indent, (int)i, hex, ascii);
    }
}

/*************************************************************/

const char* nsk_null_string(const char* str) {
    return (str == NULL)? "<NULL>" : str;
}

/*************************************************************/

}
