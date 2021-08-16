/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * A class to manage AccessBridge debugging
 */

#ifndef __AccessBridgeDebug_H__
#define __AccessBridgeDebug_H__

#include <crtdbg.h>
#include <windows.h>

#ifdef DEBUG
#define DEBUGGING_ON
#define SEND_TO_OUTPUT_DEBUG_STRING
//#define JAVA_DEBUGGING_ON
#endif

#ifdef DEBUGGING_ON
#define DEBUG_CODE(x) x
#else
#define DEBUG_CODE(x) /* */
#endif

#ifdef __cplusplus
extern "C" {
#endif

    char *printError(char *msg);
    void PrintDebugString(char *msg, ...);
    void PrintJavaDebugString(char *msg, ...);
    void wPrintJavaDebugString(wchar_t *msg, ...);
    void wPrintDebugString(wchar_t *msg, ...);
    void initializeFileLogger(char * fileName);
    void finalizeFileLogger();

#ifdef __cplusplus
}
#endif


#endif
