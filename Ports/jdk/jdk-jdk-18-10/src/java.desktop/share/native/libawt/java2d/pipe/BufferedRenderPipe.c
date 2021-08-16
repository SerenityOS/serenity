/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include <jlong.h>
#include <jni_util.h>
#include "sun_java2d_pipe_BufferedRenderPipe.h"
#include "sun_java2d_pipe_BufferedOpCodes.h"
#include "SpanIterator.h"
#include "Trace.h"

/* The "header" consists of a jint opcode and a jint span count value */
#define INTS_PER_HEADER  2
#define BYTES_PER_HEADER 8

#define BYTES_PER_SPAN sun_java2d_pipe_BufferedRenderPipe_BYTES_PER_SPAN

JNIEXPORT jint JNICALL
Java_sun_java2d_pipe_BufferedRenderPipe_fillSpans
    (JNIEnv *env, jobject pipe,
     jobject rq, jlong buf,
     jint bpos, jint limit,
     jobject si, jlong pIterator,
     jint transx, jint transy)
{
    SpanIteratorFuncs *pFuncs = (SpanIteratorFuncs *)jlong_to_ptr(pIterator);
    void *srData;
    jint spanbox[4];
    jint spanCount = 0;
    jint remainingBytes, remainingSpans;
    unsigned char *bbuf;
    jint *ibuf;
    jint ipos;
    jboolean hasException;

    J2dTraceLn2(J2D_TRACE_INFO,
                "BufferedRenderPipe_fillSpans: bpos=%d limit=%d",
                bpos, limit);

    if (JNU_IsNull(env, rq)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "BufferedRenderPipe_fillSpans: rq is null");
        return bpos;
    }

    if (JNU_IsNull(env, si)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "BufferedRenderPipe_fillSpans: span iterator is null");
        return bpos;
    }

    if (pFuncs == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "BufferedRenderPipe_fillSpans: native iterator not supplied");
        return bpos;
    }

    bbuf = (unsigned char *)jlong_to_ptr(buf);
    if (bbuf == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "BufferedRenderPipe_fillSpans: cannot get direct buffer address");
        return bpos;
    }

    // adjust the int pointer to the current buffer position
    ibuf = (jint *)(bbuf + bpos);

    // start new operation
    ibuf[0] = sun_java2d_pipe_BufferedOpCodes_FILL_SPANS;
    ibuf[1] = 0; // placeholder for the span count

    // skip the opcode and span count
    ipos = INTS_PER_HEADER;
    bpos += BYTES_PER_HEADER; // skip the opcode and span count

    remainingBytes = limit - bpos;
    remainingSpans = remainingBytes / BYTES_PER_SPAN;

    srData = (*pFuncs->open)(env, si);
    while ((*pFuncs->nextSpan)(srData, spanbox)) {
        if (remainingSpans == 0) {
            // fill in span count
            ibuf[1] = spanCount;

            // flush the queue
            JNU_CallMethodByName(env, &hasException, rq, "flushNow", "(I)V", bpos);
            if (hasException) {
                break;
            }

            // now start a new operation
            ibuf = (jint *)bbuf;
            ibuf[0] = sun_java2d_pipe_BufferedOpCodes_FILL_SPANS;
            ibuf[1] = 0; // placeholder for the span count

            // skip the opcode and span count
            ipos = INTS_PER_HEADER;
            bpos = BYTES_PER_HEADER;

            // calculate new limits
            remainingBytes = limit - bpos;
            remainingSpans = remainingBytes / BYTES_PER_SPAN;
            spanCount = 0;
        }

        // enqueue span
        ibuf[ipos++] = spanbox[0] + transx; // x1
        ibuf[ipos++] = spanbox[1] + transy; // y1
        ibuf[ipos++] = spanbox[2] + transx; // x2
        ibuf[ipos++] = spanbox[3] + transy; // y2

        // update positions
        bpos += BYTES_PER_SPAN;
        spanCount++;
        remainingSpans--;
    }
    (*pFuncs->close)(env, srData);

    // fill in span count
    ibuf[1] = spanCount;

    // return the current byte position
    return bpos;
}
