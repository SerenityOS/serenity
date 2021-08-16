/*
 * Copyright (c) 1998, 1999, Oracle and/or its affiliates. All rights reserved.
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

#include <windows.h>
#include <string.h>
#include "sys.h"


int
dbgsysExec(char *cmdLine)
{
    PROCESS_INFORMATION pi;
    STARTUPINFO si;
    int ret;

    if (cmdLine == 0) {
        return SYS_ERR;
    }

    memset(&si, 0, sizeof(si));
    si.cb = sizeof(si);

    ret = CreateProcess(0,                /* executable name */
                        cmdLine,          /* command line */
                        0,                /* process security attribute */
                        0,                /* thread security attribute */
                        TRUE,             /* inherits system handles */
                        0,                /* normal attached process */
                        0,                /* environment block */
                        0,                /* inherits the current directory */
                        &si,              /* (in)  startup information */
                        &pi);             /* (out) process information */

    if (ret == 0) {
        return SYS_ERR;
    } else {
        return SYS_OK;
    }
}
