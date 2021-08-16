/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef D3DRenderQueue_h_Included
#define D3DRenderQueue_h_Included

#include "D3DContext.h"
#include "D3DSurfaceData.h"

/*
 * The following macros are used to pick values (of the specified type) off
 * the queue.
 */
#define NEXT_VAL(buf, type) (((type *)((buf) += sizeof(type)))[-1])
#define NEXT_BYTE(buf)      NEXT_VAL(buf, unsigned char)
#define NEXT_INT(buf)       NEXT_VAL(buf, jint)
#define NEXT_FLOAT(buf)     NEXT_VAL(buf, jfloat)
#define NEXT_BOOLEAN(buf)   (jboolean)NEXT_INT(buf)
#define NEXT_LONG(buf)      NEXT_VAL(buf, jlong)
#define NEXT_DOUBLE(buf)    NEXT_VAL(buf, jdouble)

/*
 * Increments a pointer (buf) by the given number of bytes.
 */
#define SKIP_BYTES(buf, numbytes) buf += (numbytes)

/*
 * Extracts a value at the given offset from the provided packed value.
 */
#define EXTRACT_VAL(packedval, offset, mask) \
    (((packedval) >> (offset)) & (mask))
#define EXTRACT_BYTE(packedval, offset) \
    (unsigned char)EXTRACT_VAL(packedval, offset, 0xff)
#define EXTRACT_BOOLEAN(packedval, offset) \
    (jboolean)EXTRACT_VAL(packedval, offset, 0x1)

D3DContext *D3DRQ_GetCurrentContext();
D3DSDOps *D3DRQ_GetCurrentDestination();
void D3DRQ_ResetCurrentContextAndDestination();
HRESULT D3DRQ_MarkLostIfNeeded(HRESULT res, D3DSDOps *d3dops);

#endif /* D3DRenderQueue_h_Included */
