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

/* Error message and general message handling functions. */

/* NOTE: We assume that most strings passed around this library are
 *       UTF-8 (modified or standard) and not platform encoding.
 *       Before sending any strings to the "system" (e.g. OS system
 *       calls, or system input/output functions like fprintf) we need
 *       to make sure that the strings are transformed from UTF-8 to
 *       the platform encoding accepted by the system.
 *       UTF-8 and most encodings have simple ASCII or ISO-Latin
 *       characters as a subset, so in most cases the strings really
 *       don't need to be converted, but we don't know that easily.
 *       Parts of messages can be non-ASCII in some cases, so they may
 *       include classnames, methodnames, signatures, or other pieces
 *       that could contain non-ASCII characters, either from JNI or
 *       JVMTI (which both return modified UTF-8 strings).
 *       (It's possible that the platform encoding IS UTF-8, but we
 *       assume not, just to be safe).
 *
 */

#include <stdarg.h>
#include <errno.h>

#include "util.h"
#include "utf_util.h"
#include "proc_md.h"

/* Maximum number of bytes in a message, including the trailing zero.
 * Do not print very long messages as they could be truncated.
 * Use at most one pathname per message. NOTE, we use MAXPATHLEN*2
 * in case each character in the pathname takes 2 bytes.
 */
#define MAX_MESSAGE_BUF MAXPATHLEN*2+512

/* Print message in platform encoding (assume all input is UTF-8 safe)
 *    NOTE: This function is at the lowest level of the call tree.
 *          Do not use the ERROR* macros here.
 */
static void
vprint_message(FILE *fp, const char *prefix, const char *suffix,
               const char *format, va_list ap)
{
    jbyte  utf8buf[MAX_MESSAGE_BUF];
    int    len;
    char   pbuf[MAX_MESSAGE_BUF];

    /* Fill buffer with single UTF-8 string */
    (void)vsnprintf((char*)utf8buf, sizeof(utf8buf), format, ap);
    utf8buf[sizeof(utf8buf) - 1] = 0;
    len = (int)strlen((char*)utf8buf);

    /* Convert to platform encoding (ignore errors, dangerous area) */
    (void)utf8ToPlatform(utf8buf, len, pbuf, (int)sizeof(pbuf));

    (void)fprintf(fp, "%s%s%s", prefix, pbuf, suffix);
}

/* Print message in platform encoding (assume all input is UTF-8 safe)
 *    NOTE: This function is at the lowest level of the call tree.
 *          Do not use the ERROR* macros here.
 */
void
print_message(FILE *fp, const char *prefix,  const char *suffix,
              const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    vprint_message(fp, prefix, suffix, format, ap);
    va_end(ap);
}

/* Generate error message */
void
error_message(const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    vprint_message(stderr, "ERROR: ", "\n", format, ap);
    va_end(ap);
    if ( gdata->doerrorexit ) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL,"Requested errorexit=y exit()");
    }
}

/* Print plain message to stdout. */
void
tty_message(const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    vprint_message(stdout, "", "\n", format, ap);
    va_end(ap);
    (void)fflush(stdout);
}

/* Print assertion error message to stderr. */
void
jdiAssertionFailed(char *fileName, int lineNumber, char *msg)
{
    LOG_MISC(("ASSERT FAILED: %s : %d - %s\n", fileName, lineNumber, msg));
    print_message(stderr, "ASSERT FAILED: ", "\n",
        "%s : %d - %s", fileName, lineNumber, msg);
    if (gdata && gdata->assertFatal) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL,"Assertion Failed");
    }
}

/* Macro for case on switch, returns string for name. */
#define CASE_RETURN_TEXT(name) case name: return #name;

/* Mapping of JVMTI errors to their name */
const char *
jvmtiErrorText(jvmtiError error)
{
    switch ((int)error) {
        CASE_RETURN_TEXT(JVMTI_ERROR_NONE)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_THREAD)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_THREAD_GROUP)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_PRIORITY)
        CASE_RETURN_TEXT(JVMTI_ERROR_THREAD_NOT_SUSPENDED)
        CASE_RETURN_TEXT(JVMTI_ERROR_THREAD_SUSPENDED)
        CASE_RETURN_TEXT(JVMTI_ERROR_THREAD_NOT_ALIVE)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_OBJECT)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_CLASS)
        CASE_RETURN_TEXT(JVMTI_ERROR_CLASS_NOT_PREPARED)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_METHODID)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_LOCATION)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_FIELDID)
        CASE_RETURN_TEXT(JVMTI_ERROR_NO_MORE_FRAMES)
        CASE_RETURN_TEXT(JVMTI_ERROR_OPAQUE_FRAME)
        CASE_RETURN_TEXT(JVMTI_ERROR_TYPE_MISMATCH)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_SLOT)
        CASE_RETURN_TEXT(JVMTI_ERROR_DUPLICATE)
        CASE_RETURN_TEXT(JVMTI_ERROR_NOT_FOUND)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_MONITOR)
        CASE_RETURN_TEXT(JVMTI_ERROR_NOT_MONITOR_OWNER)
        CASE_RETURN_TEXT(JVMTI_ERROR_INTERRUPT)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_CLASS_FORMAT)
        CASE_RETURN_TEXT(JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION)
        CASE_RETURN_TEXT(JVMTI_ERROR_FAILS_VERIFICATION)
        CASE_RETURN_TEXT(JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED)
        CASE_RETURN_TEXT(JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_TYPESTATE)
        CASE_RETURN_TEXT(JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED)
        CASE_RETURN_TEXT(JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED)
        CASE_RETURN_TEXT(JVMTI_ERROR_UNSUPPORTED_VERSION)
        CASE_RETURN_TEXT(JVMTI_ERROR_NAMES_DONT_MATCH)
        CASE_RETURN_TEXT(JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED)
        CASE_RETURN_TEXT(JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED)
        CASE_RETURN_TEXT(JVMTI_ERROR_NOT_AVAILABLE)
        CASE_RETURN_TEXT(JVMTI_ERROR_MUST_POSSESS_CAPABILITY)
        CASE_RETURN_TEXT(JVMTI_ERROR_NULL_POINTER)
        CASE_RETURN_TEXT(JVMTI_ERROR_ABSENT_INFORMATION)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_EVENT_TYPE)
        CASE_RETURN_TEXT(JVMTI_ERROR_ILLEGAL_ARGUMENT)
        CASE_RETURN_TEXT(JVMTI_ERROR_OUT_OF_MEMORY)
        CASE_RETURN_TEXT(JVMTI_ERROR_ACCESS_DENIED)
        CASE_RETURN_TEXT(JVMTI_ERROR_WRONG_PHASE)
        CASE_RETURN_TEXT(JVMTI_ERROR_INTERNAL)
        CASE_RETURN_TEXT(JVMTI_ERROR_UNATTACHED_THREAD)
        CASE_RETURN_TEXT(JVMTI_ERROR_INVALID_ENVIRONMENT)

        CASE_RETURN_TEXT(AGENT_ERROR_INTERNAL)
        CASE_RETURN_TEXT(AGENT_ERROR_VM_DEAD)
        CASE_RETURN_TEXT(AGENT_ERROR_NO_JNI_ENV)
        CASE_RETURN_TEXT(AGENT_ERROR_JNI_EXCEPTION)
        CASE_RETURN_TEXT(AGENT_ERROR_JVMTI_INTERNAL)
        CASE_RETURN_TEXT(AGENT_ERROR_JDWP_INTERNAL)
        CASE_RETURN_TEXT(AGENT_ERROR_NOT_CURRENT_FRAME)
        CASE_RETURN_TEXT(AGENT_ERROR_OUT_OF_MEMORY)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_TAG)
        CASE_RETURN_TEXT(AGENT_ERROR_ALREADY_INVOKING)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_INDEX)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_LENGTH)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_STRING)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_CLASS_LOADER)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_ARRAY)
        CASE_RETURN_TEXT(AGENT_ERROR_TRANSPORT_LOAD)
        CASE_RETURN_TEXT(AGENT_ERROR_TRANSPORT_INIT)
        CASE_RETURN_TEXT(AGENT_ERROR_NATIVE_METHOD)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_COUNT)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_FRAMEID)
        CASE_RETURN_TEXT(AGENT_ERROR_NULL_POINTER)
        CASE_RETURN_TEXT(AGENT_ERROR_ILLEGAL_ARGUMENT)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_THREAD)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_EVENT_TYPE)
        CASE_RETURN_TEXT(AGENT_ERROR_INVALID_OBJECT)
        CASE_RETURN_TEXT(AGENT_ERROR_NO_MORE_FRAMES)

        default: return  "ERROR_unknown";
    }
}

const char *
eventText(int i)
{
    switch ( i ) {
        CASE_RETURN_TEXT(EI_SINGLE_STEP)
        CASE_RETURN_TEXT(EI_BREAKPOINT)
        CASE_RETURN_TEXT(EI_FRAME_POP)
        CASE_RETURN_TEXT(EI_EXCEPTION)
        CASE_RETURN_TEXT(EI_THREAD_START)
        CASE_RETURN_TEXT(EI_THREAD_END)
        CASE_RETURN_TEXT(EI_CLASS_PREPARE)
        CASE_RETURN_TEXT(EI_CLASS_LOAD)
        CASE_RETURN_TEXT(EI_FIELD_ACCESS)
        CASE_RETURN_TEXT(EI_FIELD_MODIFICATION)
        CASE_RETURN_TEXT(EI_EXCEPTION_CATCH)
        CASE_RETURN_TEXT(EI_METHOD_ENTRY)
        CASE_RETURN_TEXT(EI_METHOD_EXIT)
        CASE_RETURN_TEXT(EI_VM_INIT)
        CASE_RETURN_TEXT(EI_VM_DEATH)
        CASE_RETURN_TEXT(EI_GC_FINISH)
        default: return "EVENT_unknown";
    }
}

/* Macro for case on switch, returns string for name. */
#define CASE_RETURN_JDWP_ERROR_TEXT(name) case JDWP_ERROR(name): return #name;

const char *
jdwpErrorText(jdwpError serror)
{
    switch ( serror ) {
        CASE_RETURN_JDWP_ERROR_TEXT(NONE)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_THREAD)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_THREAD_GROUP)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_PRIORITY)
        CASE_RETURN_JDWP_ERROR_TEXT(THREAD_NOT_SUSPENDED)
        CASE_RETURN_JDWP_ERROR_TEXT(THREAD_SUSPENDED)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_OBJECT)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_CLASS)
        CASE_RETURN_JDWP_ERROR_TEXT(CLASS_NOT_PREPARED)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_METHODID)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_LOCATION)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_FIELDID)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_FRAMEID)
        CASE_RETURN_JDWP_ERROR_TEXT(NO_MORE_FRAMES)
        CASE_RETURN_JDWP_ERROR_TEXT(OPAQUE_FRAME)
        CASE_RETURN_JDWP_ERROR_TEXT(NOT_CURRENT_FRAME)
        CASE_RETURN_JDWP_ERROR_TEXT(TYPE_MISMATCH)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_SLOT)
        CASE_RETURN_JDWP_ERROR_TEXT(DUPLICATE)
        CASE_RETURN_JDWP_ERROR_TEXT(NOT_FOUND)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_MONITOR)
        CASE_RETURN_JDWP_ERROR_TEXT(NOT_MONITOR_OWNER)
        CASE_RETURN_JDWP_ERROR_TEXT(INTERRUPT)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_CLASS_FORMAT)
        CASE_RETURN_JDWP_ERROR_TEXT(CIRCULAR_CLASS_DEFINITION)
        CASE_RETURN_JDWP_ERROR_TEXT(FAILS_VERIFICATION)
        CASE_RETURN_JDWP_ERROR_TEXT(ADD_METHOD_NOT_IMPLEMENTED)
        CASE_RETURN_JDWP_ERROR_TEXT(SCHEMA_CHANGE_NOT_IMPLEMENTED)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_TYPESTATE)
        CASE_RETURN_JDWP_ERROR_TEXT(HIERARCHY_CHANGE_NOT_IMPLEMENTED)
        CASE_RETURN_JDWP_ERROR_TEXT(DELETE_METHOD_NOT_IMPLEMENTED)
        CASE_RETURN_JDWP_ERROR_TEXT(UNSUPPORTED_VERSION)
        CASE_RETURN_JDWP_ERROR_TEXT(NAMES_DONT_MATCH)
        CASE_RETURN_JDWP_ERROR_TEXT(CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED)
        CASE_RETURN_JDWP_ERROR_TEXT(METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED)
        CASE_RETURN_JDWP_ERROR_TEXT(CLASS_ATTRIBUTE_CHANGE_NOT_IMPLEMENTED)
        CASE_RETURN_JDWP_ERROR_TEXT(NOT_IMPLEMENTED)
        CASE_RETURN_JDWP_ERROR_TEXT(NULL_POINTER)
        CASE_RETURN_JDWP_ERROR_TEXT(ABSENT_INFORMATION)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_EVENT_TYPE)
        CASE_RETURN_JDWP_ERROR_TEXT(ILLEGAL_ARGUMENT)
        CASE_RETURN_JDWP_ERROR_TEXT(OUT_OF_MEMORY)
        CASE_RETURN_JDWP_ERROR_TEXT(ACCESS_DENIED)
        CASE_RETURN_JDWP_ERROR_TEXT(VM_DEAD)
        CASE_RETURN_JDWP_ERROR_TEXT(INTERNAL)
        CASE_RETURN_JDWP_ERROR_TEXT(UNATTACHED_THREAD)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_TAG)
        CASE_RETURN_JDWP_ERROR_TEXT(ALREADY_INVOKING)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_INDEX)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_LENGTH)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_STRING)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_CLASS_LOADER)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_ARRAY)
        CASE_RETURN_JDWP_ERROR_TEXT(TRANSPORT_LOAD)
        CASE_RETURN_JDWP_ERROR_TEXT(TRANSPORT_INIT)
        CASE_RETURN_JDWP_ERROR_TEXT(NATIVE_METHOD)
        CASE_RETURN_JDWP_ERROR_TEXT(INVALID_COUNT)
        default: return "JDWP_ERROR_unknown";
    }
}

static int p = 1;

void
do_pause(void)
{
    THREAD_T tid = GET_THREAD_ID();
    PID_T pid    = GETPID();
    int timeleft = 600; /* 10 minutes max */
    int interval = 10;  /* 10 second message check */

    /*LINTED*/
    tty_message("DEBUGGING: JDWP pause for PID %d, THREAD %d (0x%x)",
                    /*LINTED*/
                    (int)(intptr_t)pid, (int)(intptr_t)tid, (int)(intptr_t)tid);
    while ( p && timeleft > 0 ) {
        (void)sleep(interval); /* 'assign p = 0;' to get out of loop */
        timeleft -= interval;
    }
    if ( timeleft <= 0 ) {
        tty_message("DEBUGGING: JDWP pause got tired of waiting and gave up.");
    }
}
