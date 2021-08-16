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

#include "util.h"
#include "FrameID.h"
#include "threadControl.h"

/* FrameID: */

/* ------------------------------------------------------------ */
/* | thread frame generation (48 bits)| frame number (16 bits)| */
/* ------------------------------------------------------------ */

#define FNUM_BWIDTH 16
#define FNUM_BMASK ((1<<FNUM_BWIDTH)-1)

FrameID
createFrameID(jthread thread, FrameNumber fnum)
{
    FrameID frame;
    jlong frameGeneration;

    frameGeneration = threadControl_getFrameGeneration(thread);
    frame = (frameGeneration<<FNUM_BWIDTH) | (jlong)fnum;
    return frame;
}

FrameNumber
getFrameNumber(FrameID frame)
{
    /*LINTED*/
    return (FrameNumber)(((jint)frame) & FNUM_BMASK);
}

jdwpError
validateFrameID(jthread thread, FrameID frame)
{
    jlong frameGeneration;

    frameGeneration = threadControl_getFrameGeneration(thread);
    if ( frameGeneration != (frame>>FNUM_BWIDTH)  ) {
        return JDWP_ERROR(INVALID_FRAMEID);
    }
    return JDWP_ERROR(NONE);
}
