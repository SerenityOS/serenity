/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _Included_PathConsumer2D
#define _Included_PathConsumer2D

/* For forward referencing - struct defined below. */
struct _PathConsumerVec;

/*
 * Note on Error Conditions:
 * The following functions all return true on an error condition which
 * precludes any further processing.  The module calling these functions
 * should cease the operation and invoke its own error handling.
 * The return value is the only indication of the error, no exceptions
 * should be thrown by the consumer - the caller is solely responsible
 * for reporting the error/exception.
 * The most common cause of failure is an allocation failure so a
 * true return code could be reported as an "out of memory" error
 * if so desired.
 * No cleanup of the native consumer is required upon either a successful
 * completion of the path or upon an error return.  Such cleanup will
 * be handled elsewhere via other mechanisms (finalization, try/finally,
 * etc.)
 */

/* See GeneralPath.moveTo - returns true on error condition. */
typedef jboolean (MoveToFunc)(struct _PathConsumerVec *pVec,
                              jfloat x0, jfloat y0);
/* See GeneralPath.lineTo - returns true on error condition. */
typedef jboolean (LineToFunc)(struct _PathConsumerVec *pVec,
                              jfloat x1, jfloat y1);
/* See GeneralPath.quadTo - returns true on error condition. */
typedef jboolean (QuadToFunc)(struct _PathConsumerVec *pVec,
                              jfloat xm, jfloat ym,
                              jfloat x1, jfloat y1);
/* See GeneralPath.curveTo - returns true on error condition. */
typedef jboolean (CubicToFunc)(struct _PathConsumerVec *pVec,
                               jfloat xm0, jfloat ym0,
                               jfloat xm1, jfloat ym1,
                               jfloat x1, jfloat y1);
/* See GeneralPath.closePath - returns true on error condition. */
typedef jboolean (ClosePathFunc)(struct _PathConsumerVec *pVec);

/*
 * This function must be called after the last segment of the last
 * subpath is sent to the above methods.  No further calls should
 * be made to any of the PathConsumerVec functions subsequently.
 */
typedef jboolean (PathDoneFunc)(struct _PathConsumerVec *pVec);

/*
 * This structure defines the list of function pointers for implementations
 * of the above specified functions.  A pointer to this structure is also
 * handed to each function as its first parameter.  If the implementation
 * needs private context-specific data then it can be stored adjacent to
 * the PathConsumerVec structure in the same allocated storage.
 */
typedef struct _PathConsumerVec {
    MoveToFunc     *moveTo;
    LineToFunc     *lineTo;
    QuadToFunc     *quadTo;
    CubicToFunc    *cubicTo;
    ClosePathFunc  *closePath;
    PathDoneFunc   *pathDone;
} PathConsumerVec;

#endif
