/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "EventRequestImpl.h"
#include "eventHandler.h"
#include "inStream.h"
#include "outStream.h"
#include "stepControl.h"

/**
 * Take JDWP "modifiers" (which are JDI explicit filters, like
 * addCountFilter(), and implicit filters, like the LocationOnly
 * filter that goes with breakpoints) and add them as filters
 * (eventFilter) to the HandlerNode (eventHandler).
 */
static jdwpError
readAndSetFilters(JNIEnv *env, PacketInputStream *in, HandlerNode *node,
                  jint filterCount)
{
    int i;
    jdwpError serror = JDWP_ERROR(NONE);

    for (i = 0; i < filterCount; ++i) {

        jbyte modifier;

        modifier = inStream_readByte(in);
        if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
            break;

        switch (modifier) {

            case JDWP_REQUEST_MODIFIER(Conditional): {
                jint exprID;
                exprID = inStream_readInt(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setConditionalFilter(node, i, exprID));
                break;
            }

            case JDWP_REQUEST_MODIFIER(Count): {
                jint count;
                count = inStream_readInt(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setCountFilter(node, i, count));
                break;
            }

            case JDWP_REQUEST_MODIFIER(ThreadOnly): {
                jthread thread;
                thread = inStream_readThreadRef(env, in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setThreadOnlyFilter(node, i, thread));
                break;
            }

            case JDWP_REQUEST_MODIFIER(LocationOnly): {
                jbyte tag;
                jclass clazz;
                jmethodID method;
                jlocation location;
                tag = inStream_readByte(in); /* not currently used */
                tag = tag; /* To shut up lint */
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                clazz = inStream_readClassRef(env, in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                method = inStream_readMethodID(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                location = inStream_readLocation(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setLocationOnlyFilter(node, i, clazz, method, location));
                break;
            }

            case JDWP_REQUEST_MODIFIER(FieldOnly): {
                jclass clazz;
                jfieldID field;
                clazz = inStream_readClassRef(env, in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                field = inStream_readFieldID(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setFieldOnlyFilter(node, i, clazz, field));
                break;
            }

            case JDWP_REQUEST_MODIFIER(ClassOnly): {
                jclass clazz;
                clazz = inStream_readClassRef(env, in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setClassOnlyFilter(node, i, clazz));
                break;
            }

            case JDWP_REQUEST_MODIFIER(ExceptionOnly): {
                jclass exception;
                jboolean caught;
                jboolean uncaught;
                exception = inStream_readClassRef(env, in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                caught = inStream_readBoolean(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                uncaught = inStream_readBoolean(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setExceptionOnlyFilter(node, i,
                                             exception, caught, uncaught));
                break;
            }

            case JDWP_REQUEST_MODIFIER(InstanceOnly): {
                jobject instance;
                instance = inStream_readObjectRef(env, in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setInstanceOnlyFilter(node, i, instance));
                break;
            }

            case JDWP_REQUEST_MODIFIER(ClassMatch): {
                char *pattern;
                pattern = inStream_readString(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setClassMatchFilter(node, i,
                                                                pattern));
                break;
            }

            case JDWP_REQUEST_MODIFIER(ClassExclude): {
                char *pattern;
                pattern = inStream_readString(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                        eventFilter_setClassExcludeFilter(node, i, pattern));
                break;
            }
            case JDWP_REQUEST_MODIFIER(Step): {
                jthread thread;
                jint size;
                jint depth;
                thread = inStream_readThreadRef(env, in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                size = inStream_readInt(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                depth = inStream_readInt(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) )
                    break;
                serror = map2jdwpError(
                      eventFilter_setStepFilter(node, i, thread, size, depth));
                break;
            }
            case JDWP_REQUEST_MODIFIER(SourceNameMatch): {
                char *sourceNamePattern;
                sourceNamePattern = inStream_readString(in);
                if ( (serror = inStream_error(in)) != JDWP_ERROR(NONE) ) {
                    break;
                }
                serror = map2jdwpError(
                        eventFilter_setSourceNameMatchFilter(node, i, sourceNamePattern));
                break;
            }

            default:
                serror = JDWP_ERROR(ILLEGAL_ARGUMENT);
                break;
        }
        if ( serror != JDWP_ERROR(NONE) )
            break;
    }
    return serror;
}

/**
 * This is the back-end implementation for enabling
 * (what are at the JDI level) EventRequests.
 *
 * Allocate the event request handler (eventHandler).
 * Add any filters (explicit or implicit).
 * Install the handler.
 * Return the handlerID which is used to map subsequent
 * events to the EventRequest that created it.
 */
static jboolean
setCommand(PacketInputStream *in, PacketOutputStream *out)
{
    jdwpError serror;
    HandlerNode *node;
    HandlerID requestID = -1;
    jdwpEvent eventType;
    jbyte suspendPolicy;
    jint filterCount;
    EventIndex ei;

    node = NULL;
    eventType = inStream_readByte(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    suspendPolicy = inStream_readByte(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    filterCount = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    ei = jdwp2EventIndex(eventType);
    if (ei == 0) {
        outStream_setError(out, JDWP_ERROR(INVALID_EVENT_TYPE));
        return JNI_TRUE;
    }

    if (ei == EI_VM_INIT) {
        /*
         * VM is already initialized so there's no need to install a handler
         * for this event. However we need to allocate a requestID to send in
         * the reply to the debugger.
         */
        serror = JDWP_ERROR(NONE);
        requestID = eventHandler_allocHandlerID();
    } else {
        node = eventHandler_alloc(filterCount, ei, suspendPolicy);
        if (node == NULL) {
            outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
            return JNI_TRUE;
        }
        if (eventType == JDWP_EVENT(METHOD_EXIT_WITH_RETURN_VALUE)) {
            node->needReturnValue = 1;
        } else {
            node->needReturnValue = 0;
        }
        serror = readAndSetFilters(getEnv(), in, node, filterCount);
        if (serror == JDWP_ERROR(NONE)) {
            jvmtiError error;
            error = eventHandler_installExternal(node);
            serror = map2jdwpError(error);
            if (serror == JDWP_ERROR(NONE)) {
                requestID = node->handlerID;
            }
        }
    }

    if (serror == JDWP_ERROR(NONE)) {
        (void)outStream_writeInt(out, requestID);
    } else {
        (void)eventHandler_free(node);
        outStream_setError(out, serror);
    }

    return JNI_TRUE;
}

/**
 * This is the back-end implementation for disabling
 * (what are at the JDI level) EventRequests.
 */
static jboolean
clearCommand(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jdwpEvent eventType;
    HandlerID handlerID;
    EventIndex ei;

    eventType = inStream_readByte(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    handlerID = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    ei = jdwp2EventIndex(eventType);
    if (ei == 0) {
        /* NOTE: Clear command not yet spec'ed to return INVALID_EVENT_TYPE */
        outStream_setError(out, JDWP_ERROR(INVALID_EVENT_TYPE));
        return JNI_TRUE;
    }

    error = eventHandler_freeByID(ei, handlerID);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }

    return JNI_TRUE;
}

static jboolean
clearAllBreakpoints(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;

    error = eventHandler_freeAll(EI_BREAKPOINT);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}

Command EventRequest_Commands[] = {
    {setCommand, "SetCommand"},
    {clearCommand, "ClearCommand"},
    {clearAllBreakpoints, "ClearAllBreakpoints"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(EventRequest)
