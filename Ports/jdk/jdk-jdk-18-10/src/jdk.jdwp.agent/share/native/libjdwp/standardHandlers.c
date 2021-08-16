/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * handlers
 *
 * The default event request handler functions
 */

#include "util.h"
#include "eventHandler.h"
#include "threadControl.h"
#include "eventHelper.h"
#include "classTrack.h"

#include "standardHandlers.h"

/* HandlerFunction - Invoked from event_callback() */
static void
handleClassPrepare(JNIEnv *env, EventInfo *evinfo,
                   HandlerNode *node,
                   struct bag *eventBag)
{
    jthread thread = evinfo->thread;

    /* We try hard to avoid class loads/prepares in debugger
     * threads, but it is still possible for them to happen (most
     * likely for exceptions that are thrown within JNI
     * methods). If such an event occurs, we must report it, but
     * we cannot suspend the debugger thread.
     *
     * 1) We report the thread as NULL because we don't want the
     *    application to get hold of a debugger thread object.
     * 2) We try to do the right thing wrt to suspending
     *    threads without suspending debugger threads. If the
     *    requested suspend policy is NONE, there's no problem. If
     *    the requested policy is ALL, we can just suspend all
     *    application threads without producing any surprising
     *    results by leaving the debugger thread running. However,
     *    if the requested policy is EVENT_THREAD, we are forced
     *    to do something different than requested. The most
     *    useful behavior is to suspend all application threads
     *    (just as if the policy was ALL). This allows the
     *    application to operate on the class before it gets into
     *    circulation and so it is preferable to the other
     *    alternative of suspending no threads.
     */
    if (threadControl_isDebugThread(thread)) {
        evinfo->thread = NULL;
        if (node->suspendPolicy == JDWP_SUSPEND_POLICY(EVENT_THREAD)) {
            node->suspendPolicy = JDWP_SUSPEND_POLICY(ALL);
        }
    }
    eventHelper_recordEvent(evinfo, node->handlerID,
                            node->suspendPolicy, eventBag);
}

/* HandlerFunction - Invoked from event_callback() */
static void
handleGarbageCollectionFinish(JNIEnv *env, EventInfo *evinfo,
                  HandlerNode *node,
                  struct bag *eventBag)
{
    JDI_ASSERT_MSG(JNI_FALSE, "Should never call handleGarbageCollectionFinish");
}

/* HandlerFunction - Invoked from event_callback() for METHOD_ENTRY and METHOD_EXIT. */
static void
handleFrameEvent(JNIEnv *env, EventInfo *evinfo,
                 HandlerNode *node,
                 struct bag *eventBag)
{
    /*
     * The frame id that comes with this event is very transient.
     * We can't send the frame to the helper thread because it
     * might be useless by the time the helper thread can use it
     * (if suspend policy is NONE). So, get the needed info from
     * the frame and then use a special command to the helper
     * thread.
     */

    jmethodID method;
    jlocation location;
    jvmtiError error;
    FrameNumber fnum = 0;
    jvalue returnValue;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameLocation)
            (gdata->jvmti, evinfo->thread, fnum, &method, &location);
    if (error != JVMTI_ERROR_NONE) {
        location = -1;
    }
    returnValue = evinfo->u.method_exit.return_value;

    eventHelper_recordFrameEvent(node->handlerID,
                                 node->suspendPolicy,
                                 evinfo->ei,
                                 evinfo->thread,
                                 evinfo->clazz,
                                 evinfo->method,
                                 location,
                                 node->needReturnValue,
                                 returnValue,
                                 eventBag);
}

/* HandlerFunction - Invoked from event_callback() */
static void
genericHandler(JNIEnv *env, EventInfo *evinfo,
               HandlerNode *node,
               struct bag *eventBag)
{
    eventHelper_recordEvent(evinfo, node->handlerID, node->suspendPolicy,
                            eventBag);
}

HandlerFunction
standardHandlers_defaultHandler(EventIndex ei)
{
    switch (ei) {
        case EI_BREAKPOINT:
        case EI_EXCEPTION:
        case EI_FIELD_ACCESS:
        case EI_FIELD_MODIFICATION:
        case EI_SINGLE_STEP:
        case EI_THREAD_START:
        case EI_THREAD_END:
        case EI_VM_DEATH:
        case EI_MONITOR_CONTENDED_ENTER:
        case EI_MONITOR_CONTENDED_ENTERED:
        case EI_MONITOR_WAIT:
        case EI_MONITOR_WAITED:
            return &genericHandler;

        case EI_CLASS_PREPARE:
            return &handleClassPrepare;

        case EI_GC_FINISH:
            return &handleGarbageCollectionFinish;

        case EI_METHOD_ENTRY:
        case EI_METHOD_EXIT:
            return &handleFrameEvent;

        default:
            /* This NULL will trigger a AGENT_ERROR_INVALID_EVENT_TYPE */
            return NULL;
    }
}

void
standardHandlers_onConnect(void)
{
    jboolean installed;

    /* always report VMDeath to a connected debugger */
    installed = (eventHandler_createPermanentInternal(
                        EI_VM_DEATH, genericHandler) != NULL);
    if (!installed) {
        EXIT_ERROR(AGENT_ERROR_INVALID_EVENT_TYPE,"Unable to install VM Death event handler");
    }
}

void
standardHandlers_onDisconnect(void)
{
}
