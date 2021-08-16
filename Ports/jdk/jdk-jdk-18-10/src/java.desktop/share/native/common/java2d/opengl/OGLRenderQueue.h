/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OGLRenderQueue_h_Included
#define OGLRenderQueue_h_Included

#include "OGLContext.h"
#include "OGLSurfaceData.h"

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

/*
 * Parameter used by the RESET_PREVIOUS_OP() convenience macro, which
 * indicates that any "open" state (such as an unmatched glBegin() or
 * glEnable(GL_TEXTURE_2D)) should be completed before the following operation
 * is performed.  SET_SURFACES is an example of an operation that needs to
 * call RESET_PREVIOUS_OP() before completing the surface change operation.
 */
#define OGL_STATE_RESET  -1

/*
 * Parameter passed to the CHECK_PREVIOUS_OP() macro to indicate that the
 * following operation represents a "simple" state change.  A simple state
 * change is one that is allowed to occur within a series of texturing
 * operations; in other words, this type of state change can occur without
 * first calling glDisable(GL_TEXTURE_2D).  An example of such an operation
 * is SET_RECT_CLIP.
 */
#define OGL_STATE_CHANGE -2

/*
 * Parameter passed to the CHECK_PREVIOUS_OP() macro to indicate that the
 * following operation represents an operation that uses an alpha mask,
 * such as OGLMaskFill and OGLTR_DrawGrayscaleGlyphNoCache().
 */
#define OGL_STATE_MASK_OP -3

/*
 * Parameter passed to the CHECK_PREVIOUS_OP() macro to indicate that the
 * following operation represents an operation that uses the glyph cache,
 * such as OGLTR_DrawGrayscaleGlyphViaCache().
 */
#define OGL_STATE_GLYPH_OP -4

/*
 * Parameter passed to the CHECK_PREVIOUS_OP() macro to indicate that the
 * following operation represents an operation that renders a
 * parallelogram via a fragment program (see OGLRenderer).
 */
#define OGL_STATE_PGRAM_OP -5

/*
 * Initializes the "previous operation" state to its default value.
 */
#define INIT_PREVIOUS_OP() previousOp = OGL_STATE_RESET

/*
 * These macros now simply delegate to the CheckPreviousOp() method.
 */
#define CHECK_PREVIOUS_OP(op) OGLRenderQueue_CheckPreviousOp(op)
#define RESET_PREVIOUS_OP() CHECK_PREVIOUS_OP(OGL_STATE_RESET)

/*
 * The following macros allow the caller to return (or continue) if the
 * provided value is NULL.  (The strange else clause is included below to
 * allow for a trailing ';' after RETURN/CONTINUE_IF_NULL() invocations.)
 */
#define ACT_IF_NULL(ACTION, value)         \
    if ((value) == NULL) {                 \
        J2dTraceLn1(J2D_TRACE_ERROR,       \
                    "%s is null", #value); \
        ACTION;                            \
    } else do { } while (0)
#define RETURN_IF_NULL(value)   ACT_IF_NULL(return, value)
#define CONTINUE_IF_NULL(value) ACT_IF_NULL(continue, value)

/*
 * Exports.
 */
extern jint previousOp;

OGLContext *OGLRenderQueue_GetCurrentContext();
OGLSDOps *OGLRenderQueue_GetCurrentDestination();
void OGLRenderQueue_CheckPreviousOp(jint op);

#endif /* OGLRenderQueue_h_Included */
