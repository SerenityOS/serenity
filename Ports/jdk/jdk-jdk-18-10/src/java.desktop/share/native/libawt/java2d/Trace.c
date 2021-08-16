/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include "Trace.h"

static int j2dTraceLevel = J2D_TRACE_INVALID;
static FILE *j2dTraceFile = NULL;

JNIEXPORT void JNICALL
J2dTraceImpl(int level, jboolean cr, const char *string, ...)
{
    va_list args;
    if (j2dTraceLevel < J2D_TRACE_OFF) {
        J2dTraceInit();
    }
    if (level <= j2dTraceLevel) {
        if (cr) {
            switch (level) {
            case J2D_TRACE_ERROR:
                fprintf(j2dTraceFile, "[E] ");
                break;
            case J2D_TRACE_WARNING:
                fprintf(j2dTraceFile, "[W] ");
                break;
            case J2D_TRACE_INFO:
                fprintf(j2dTraceFile, "[I] ");
                break;
            case J2D_TRACE_VERBOSE:
                fprintf(j2dTraceFile, "[V] ");
                break;
            case J2D_TRACE_VERBOSE2:
                fprintf(j2dTraceFile, "[X] ");
                break;
            default:
                break;
            }
        }

        va_start(args, string);
        vfprintf(j2dTraceFile, string, args);
        va_end(args);

        if (cr) {
            fprintf(j2dTraceFile, "\n");
        }
        fflush(j2dTraceFile);
    }
}

JNIEXPORT void JNICALL
J2dTraceInit()
{
    char *j2dTraceLevelString = getenv("J2D_TRACE_LEVEL");
    char *j2dTraceFileName;
    j2dTraceLevel = J2D_TRACE_OFF;
    if (j2dTraceLevelString) {
        int traceLevelTmp = -1;
        int args = sscanf(j2dTraceLevelString, "%d", &traceLevelTmp);
        if (args > 0 &&
            traceLevelTmp > J2D_TRACE_INVALID &&
            traceLevelTmp < J2D_TRACE_MAX)
        {
            j2dTraceLevel = traceLevelTmp;
        }
    }
    j2dTraceFileName = getenv("J2D_TRACE_FILE");
    if (j2dTraceFileName) {
        j2dTraceFile = fopen(j2dTraceFileName, "w");
        if (!j2dTraceFile) {
            printf("[E]: Error opening trace file %s\n", j2dTraceFileName);
        }
    }
    if (!j2dTraceFile) {
        j2dTraceFile = stdout;
    }
}
