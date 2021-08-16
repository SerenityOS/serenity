/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

typedef void (WINAPI * LogStringCallbackFP) (const char * lpString);
extern LogStringCallbackFP g_LogStringCallback;

#define LINE_BUFSIZE 1024
#define LARGE_BUFSIZE 5120
#define HUGE_BUFSIZE 20480

/*
 * returns formatted date and time
 */
char *getTimeAndDate();

/*
 * displays a message in a dialog and writes the message to a logfile
 */
void displayAndLog(HWND hDlg, int nIDDlgItem, FILE *logfile, char *msg, ...);

/*
 * writes a text string to a logfile
 */
void logString(FILE *logfile, char *msg, ...);

/**
 * returns accessibility information for an AccessibleContext
 */
char *getAccessibleInfo(long vmID, AccessibleContext ac, char *buffer, int bufsize);

/**
 * returns accessibility information at the specified coordinates in an AccessibleContext
 */
char *getAccessibleInfo(long vmID, AccessibleContext ac, int x, int y, char *buffer, int bufsize);
