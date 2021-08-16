/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "stepControl.h"
#include "eventHandler.h"
#include "eventHelper.h"
#include "threadControl.h"
#include "SDE.h"

static jrawMonitorID stepLock;

static jint
getFrameCount(jthread thread)
{
    jint count = 0;
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameCount)
                    (gdata->jvmti, thread, &count);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "getting frame count");
    }
    return count;
}

/*
 * Most enabling/disabling of JVMTI events happens implicitly through
 * the inserting and freeing of handlers for those events. Stepping is
 * different because requested steps are usually not identical to JVMTI steps.
 * They usually require multiple events step, and otherwise, before they
 * complete. While a step request is pending, we may need to temporarily
 * disable and re-enable stepping, but we can't just remove the handlers
 * because that would break the application's ability to remove the
 * events. So, for step events only, we directly enable and disable stepping.
 * This is safe because there can only ever be one pending step request
 * per thread.
 */
static void
enableStepping(jthread thread)
{
    jvmtiError error;

    LOG_STEP(("enableStepping: thread=%p", thread));

    error = threadControl_setEventMode(JVMTI_ENABLE, EI_SINGLE_STEP,
                                            thread);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "enabling single step");
    }
}

static void
disableStepping(jthread thread)
{
    jvmtiError error;

    LOG_STEP(("disableStepping: thread=%p", thread));

    error = threadControl_setEventMode(JVMTI_DISABLE, EI_SINGLE_STEP,
                                            thread);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "disabling single step");
    }
}

static jvmtiError
getFrameLocation(jthread thread,
        jclass *pclazz, jmethodID *pmethod, jlocation *plocation)
{
    jvmtiError error;

    *pclazz = NULL;
    *pmethod = NULL;
    *plocation = -1;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameLocation)
            (gdata->jvmti, thread, 0, pmethod, plocation);
    if (error == JVMTI_ERROR_NONE && *pmethod!=NULL ) {
        /* This also serves to verify that the methodID is valid */
        error = methodClass(*pmethod, pclazz);
    }
    return error;
}

static void
getLineNumberTable(jmethodID method, jint *pcount,
                jvmtiLineNumberEntry **ptable)
{
    jvmtiError error;

    *pcount = 0;
    *ptable = NULL;

    /* If the method is native or obsolete, don't even ask for the line table */
    if ( isMethodObsolete(method) || isMethodNative(method)) {
        return;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetLineNumberTable)
                (gdata->jvmti, method, pcount, ptable);
    if (error != JVMTI_ERROR_NONE) {
        *pcount = 0;
    }
}

static jint
findLineNumber(jthread thread, jlocation location,
               jvmtiLineNumberEntry *lines, jint count)
{
    jint line = -1;

    if (location != -1) {
        if (count > 0) {
            jint i;
            /* any preface before first line is assigned to first line */
            for (i=1; i<count; i++) {
                if (location < lines[i].start_location) {
                    break;
                }
            }
            line = lines[i-1].line_number;
        }
    }
    return line;
}

static jboolean
hasLineNumbers(jmethodID method)
{
    jint count;
    jvmtiLineNumberEntry *table;

    getLineNumberTable(method, &count, &table);
    if ( count == 0 ) {
        return JNI_FALSE;
    } else {
        jvmtiDeallocate(table);
    }
    return JNI_TRUE;
}

static jvmtiError
initState(JNIEnv *env, jthread thread, StepRequest *step)
{
    jvmtiError error;

    /*
     * Initial values that may be changed below
     */
    step->fromLine = -1;
    step->fromNative = JNI_FALSE;
    step->frameExited = JNI_FALSE;
    step->fromStackDepth = getFrameCount(thread);

    if (step->fromStackDepth <= 0) {
        /*
         * If there are no stack frames, treat the step as though
         * from a native frame. This is most likely to occur at the
         * beginning of a debug session, right after the VM_INIT event,
         * so we need to do something intelligent.
         */
        step->fromNative = JNI_TRUE;
        return JVMTI_ERROR_NONE;
    }

    /*
     * Try to get a notification on frame pop. If we're in an opaque frame
     * we won't be able to, but we can use other methods to detect that
     * a native frame has exited.
     *
     * TO DO: explain the need for this notification.
     */
    error = JVMTI_FUNC_PTR(gdata->jvmti,NotifyFramePop)
                (gdata->jvmti, thread, 0);
    if (error == JVMTI_ERROR_OPAQUE_FRAME) {
        step->fromNative = JNI_TRUE;
        error = JVMTI_ERROR_NONE;
        /* continue without error */
    } else if (error == JVMTI_ERROR_DUPLICATE) {
        error = JVMTI_ERROR_NONE;
        /* Already being notified, continue without error */
    } else if (error != JVMTI_ERROR_NONE) {
        return error;
    }

    LOG_STEP(("initState(): frame=%d", step->fromStackDepth));

    /*
     * Note: we can't undo the frame pop notify, so
     * we'll just have to let the handler ignore it if
     * there are any errors below.
     */

    if (step->granularity == JDWP_STEP_SIZE(LINE) ) {

        LOG_STEP(("initState(): Begin line step"));

        WITH_LOCAL_REFS(env, 1) {

            jclass clazz;
            jmethodID method;
            jlocation location;

            error = getFrameLocation(thread, &clazz, &method, &location);
            if (error == JVMTI_ERROR_NONE) {
                /* Clear out previous line table only if we changed methods */
                if ( method != step->method ) {
                    step->lineEntryCount = 0;
                    if (step->lineEntries != NULL) {
                        jvmtiDeallocate(step->lineEntries);
                        step->lineEntries = NULL;
                    }
                    step->method = method;
                    getLineNumberTable(step->method,
                                 &step->lineEntryCount, &step->lineEntries);
                    if (step->lineEntryCount > 0) {
                        convertLineNumberTable(env, clazz,
                                &step->lineEntryCount, &step->lineEntries);
                    }
                }
                step->fromLine = findLineNumber(thread, location,
                                     step->lineEntries, step->lineEntryCount);
            }

        } END_WITH_LOCAL_REFS(env);

    }

    return error;
}

/*
 * TO DO: The step handlers (handleFrameChange and handleStep can
 * be broken down and made simpler now that we can install and de-install event
 * handlers.
 */
static void
handleFramePopEvent(JNIEnv *env, EventInfo *evinfo,
                    HandlerNode *node,
                    struct bag *eventBag)
{
    StepRequest *step;
    jthread thread = evinfo->thread;

    stepControl_lock();

    step = threadControl_getStepRequest(thread);
    if (step == NULL) {
        EXIT_ERROR(AGENT_ERROR_INVALID_THREAD, "getting step request");
    }

    if (step->pending) {
        /*
         * Note: current depth is reported as *before* the pending frame
         * pop.
         */
        jint currentDepth;
        jint fromDepth;
        jint afterPopDepth;

        currentDepth = getFrameCount(thread);
        fromDepth = step->fromStackDepth;
        afterPopDepth = currentDepth-1;

        LOG_STEP(("handleFramePopEvent: BEGIN fromDepth=%d, currentDepth=%d",
                        fromDepth, currentDepth));

        /*
         * If we are exiting the original stepping frame, record that
         * fact here. Once the next step event comes in, we can safely
         * stop stepping there.
         */
        if (fromDepth > afterPopDepth ) {
            step->frameExited = JNI_TRUE;
        }

        if (step->depth == JDWP_STEP_DEPTH(OVER)) {
            /*
             * Either
             * 1) the original stepping frame is about to be popped
             *    [fromDepth == currentDepth]. Re-enable stepping to
             *    reach a point where we can stop.
             * 2) a method called from the stepping frame has returned
             *    (during which we had stepping disabled)
             *    [fromDepth == currentDepth - 1]. Re-enable stepping
             *    so that we can continue instructions steps in the
             *    original stepping frame.
             * 3) a method further down the call chain has notified
             *    of a frame pop [fromDepth < currentDepth - 1]. This
             *    *might* represent case (2) above if the stepping frame
             *    was calling a native method which in turn called a
             *    java method. If so, we must enable stepping to
             *    ensure that we get control back after the intervening
             *    native frame is popped (you can't get frame pop
             *    notifications on native frames). If the native caller
             *    calls another Java method before returning,
             *    stepping will be diabled again and another frame pop
             *    will be awaited.
             *
             *    If it turns out that this is not case (2) with native
             *    methods, then the enabled stepping is benign and
             *    will be disabled again on the next step event.
             *
             * Note that the condition not covered above,
             * [fromDepth > currentDepth] shouldn't happen since it means
             * that too many frames have been popped. For robustness,
             * we enable stepping in that case too, so that the errant
             * step-over can be stopped.
             *
             */
            LOG_STEP(("handleFramePopEvent: starting singlestep, depth==OVER"));
            enableStepping(thread);
        } else if (step->depth == JDWP_STEP_DEPTH(OUT) &&
                   fromDepth > afterPopDepth) {
            /*
             * The original stepping frame is about to be popped. Step
             * until we reach the next safe place to stop.
             */
            LOG_STEP(("handleFramePopEvent: starting singlestep, depth==OUT && fromDepth > afterPopDepth (%d>%d)",fromDepth, afterPopDepth));
            enableStepping(thread);
        } else if (step->methodEnterHandlerNode != NULL) {
            /* We installed a method entry event handler as part of a step into operation. */
            JDI_ASSERT(step->depth == JDWP_STEP_DEPTH(INTO));
            if (fromDepth >= afterPopDepth) {
                /*
                 * We've popped back to the original stepping frame without finding a place to stop.
                 * Resume stepping in the original frame.
                 */
                LOG_STEP(("handleFramePopEvent: starting singlestep, have methodEnter handler && depth==INTO && fromDepth >= afterPopDepth (%d>=%d)", fromDepth, afterPopDepth));
                enableStepping(thread);
                (void)eventHandler_free(step->methodEnterHandlerNode);
                step->methodEnterHandlerNode = NULL;
            } else {
                LOG_STEP(("handleFramePopEvent: starting singlestep, have methodEnter handler && depth==INTO && fromDepth < afterPopDepth (%d<%d)", fromDepth, afterPopDepth));
            }
        }
        LOG_STEP(("handleFramePopEvent: finished"));
    }

    stepControl_unlock();
}

static void
handleExceptionCatchEvent(JNIEnv *env, EventInfo *evinfo,
                          HandlerNode *node,
                          struct bag *eventBag)
{
    StepRequest *step;
    jthread thread = evinfo->thread;

    stepControl_lock();

    step = threadControl_getStepRequest(thread);
    if (step == NULL) {
        EXIT_ERROR(AGENT_ERROR_INVALID_THREAD, "getting step request");
    }

    if (step->pending) {
        /*
         *  Determine where we are on the call stack relative to where
         *  we started.
         */
        jint currentDepth = getFrameCount(thread);
        jint fromDepth = step->fromStackDepth;

        LOG_STEP(("handleExceptionCatchEvent: fromDepth=%d, currentDepth=%d",
                        fromDepth, currentDepth));

        /*
         * If we are exiting the original stepping frame, record that
         * fact here. Once the next step event comes in, we can safely
         * stop stepping there.
         */
        if (fromDepth > currentDepth) {
            step->frameExited = JNI_TRUE;
        }

        if (step->depth == JDWP_STEP_DEPTH(OVER) &&
            fromDepth >= currentDepth) {
            /*
             * Either the original stepping frame is done,
             * or a called method has returned (during which we had stepping
             * disabled). In either case we must resume stepping.
             */
            enableStepping(thread);
        } else if (step->depth == JDWP_STEP_DEPTH(OUT) &&
                   fromDepth > currentDepth) {
            /*
             * The original stepping frame is done. Step
             * until we reach the next safe place to stop.
             */
            enableStepping(thread);
        } else if (step->methodEnterHandlerNode != NULL &&
                   fromDepth >= currentDepth) {
            /*
             * We installed a method entry event handler as part of a
             * step into operation. We've popped back to the original
             * stepping frame or higher without finding a place to stop.
             * Resume stepping in the original frame.
             */
            enableStepping(thread);
            (void)eventHandler_free(step->methodEnterHandlerNode);
            step->methodEnterHandlerNode = NULL;
        }
    }

    stepControl_unlock();
}

static void
handleMethodEnterEvent(JNIEnv *env, EventInfo *evinfo,
                       HandlerNode *node,
                       struct bag *eventBag)
{
    StepRequest *step;
    jthread thread;

    thread = evinfo->thread;

    stepControl_lock();

    step = threadControl_getStepRequest(thread);
    if (step == NULL) {
        EXIT_ERROR(AGENT_ERROR_INVALID_THREAD, "getting step request");
    }

    if (step->pending) {
        jclass    clazz;
        jmethodID method;
        char     *classname;

        LOG_STEP(("handleMethodEnterEvent: thread=%p", thread));

        clazz     = evinfo->clazz;
        method    = evinfo->method;
        classname = getClassname(clazz);

        /*
         * This handler is relevant only to step into
         */
        JDI_ASSERT(step->depth == JDWP_STEP_DEPTH(INTO));

        if (    (!eventFilter_predictFiltering(step->stepHandlerNode,
                                               clazz, classname))
             && (   step->granularity != JDWP_STEP_SIZE(LINE)
                 || hasLineNumbers(method) ) ) {
            /*
             * We've found a suitable method in which to stop. Step
             * until we reach the next safe location to complete the step->,
             * and we can get rid of the method entry handler.
             */
            enableStepping(thread);
            if ( step->methodEnterHandlerNode != NULL ) {
                (void)eventHandler_free(step->methodEnterHandlerNode);
                step->methodEnterHandlerNode = NULL;
            }
        }
        jvmtiDeallocate(classname);
        classname = NULL;
    }

    stepControl_unlock();
}

static void
completeStep(JNIEnv *env, jthread thread, StepRequest *step)
{
    jvmtiError error;

    /*
     * We've completed a step; reset state for the next one, if any
     */

    LOG_STEP(("completeStep: thread=%p", thread));

    if (step->methodEnterHandlerNode != NULL) {
        (void)eventHandler_free(step->methodEnterHandlerNode);
        step->methodEnterHandlerNode = NULL;
    }

    error = initState(env, thread, step);
    if (error != JVMTI_ERROR_NONE) {
        /*
         * None of the initState errors should happen after one step
         * has successfully completed.
         */
        EXIT_ERROR(error, "initializing step state");
    }
}

jboolean
stepControl_handleStep(JNIEnv *env, jthread thread,
                       jclass clazz, jmethodID method)
{
    jboolean completed = JNI_FALSE;
    StepRequest *step;
    jint currentDepth;
    jint fromDepth;
    jvmtiError error;
    char *classname;

    classname = NULL;
    stepControl_lock();

    step = threadControl_getStepRequest(thread);
    if (step == NULL) {
        EXIT_ERROR(AGENT_ERROR_INVALID_THREAD, "getting step request");
    }

    /*
     * If no step is currently pending, ignore the event
     */
    if (!step->pending) {
        goto done;
    }

    LOG_STEP(("stepControl_handleStep: thread=%p", thread));

    /*
     * We never filter step into instruction. It's always over on the
     * first step event.
     */
    if (step->depth == JDWP_STEP_DEPTH(INTO) &&
        step->granularity == JDWP_STEP_SIZE(MIN)) {
        completed = JNI_TRUE;
        LOG_STEP(("stepControl_handleStep: completed, into min"));
        goto done;
    }

    /*
     * If we have left the method in which
     * stepping started, the step is always complete.
     */
    if (step->frameExited) {
        completed = JNI_TRUE;
        LOG_STEP(("stepControl_handleStep: completed, frame exited"));
        goto done;
    }

    /*
     *  Determine where we are on the call stack relative to where
     *  we started.
     */
    currentDepth = getFrameCount(thread);
    fromDepth = step->fromStackDepth;

    if (fromDepth > currentDepth) {
        /*
         * We have returned from the caller. There are cases where
         * we don't get frame pop notifications
         * (e.g. stepping from opaque frames), and that's when
         * this code will be reached. Complete the step->
         */
        completed = JNI_TRUE;
        LOG_STEP(("stepControl_handleStep: completed, fromDepth>currentDepth(%d>%d)", fromDepth, currentDepth));
    } else if (fromDepth < currentDepth) {
        /* We have dropped into a called method. */
        if (   step->depth == JDWP_STEP_DEPTH(INTO)
            && (!eventFilter_predictFiltering(step->stepHandlerNode, clazz,
                                          (classname = getClassname(clazz))))
            && hasLineNumbers(method) ) {

            /* Stepped into a method with lines, so we're done */
            completed = JNI_TRUE;
            LOG_STEP(("stepControl_handleStep: completed, fromDepth<currentDepth(%d<%d) and into method with lines", fromDepth, currentDepth));
        } else {
            /*
             * We need to continue, but don't want the overhead of step
             * events from this method. So, we disable stepping and
             * enable a frame pop. If we're stepping into, we also
             * enable method enter events because a called frame may be
             * where we want to stop.
             */
            disableStepping(thread);

            if (step->depth == JDWP_STEP_DEPTH(INTO)) {
                step->methodEnterHandlerNode =
                    eventHandler_createInternalThreadOnly(
                                       EI_METHOD_ENTRY,
                                       handleMethodEnterEvent, thread);
                if (step->methodEnterHandlerNode == NULL) {
                    EXIT_ERROR(AGENT_ERROR_INVALID_EVENT_TYPE,
                                "installing event method enter handler");
                }
            }
            LOG_STEP(("stepControl_handleStep: NotifyFramePop (fromDepth=%d currentDepth=%d)",
                      fromDepth, currentDepth));

            error = JVMTI_FUNC_PTR(gdata->jvmti,NotifyFramePop)
                        (gdata->jvmti, thread, 0);
            if (error == JVMTI_ERROR_DUPLICATE) {
                error = JVMTI_ERROR_NONE;
            } else if (error != JVMTI_ERROR_NONE) {
                EXIT_ERROR(error, "setting up notify frame pop");
            }
        }
        jvmtiDeallocate(classname);
        classname = NULL;
    } else {
        /*
         * We are at the same stack depth where stepping started.
         * Instruction steps are complete at this point. For line
         * steps we must check to see whether we've moved to a
         * different line.
         */
        if (step->granularity == JDWP_STEP_SIZE(MIN)) {
            completed = JNI_TRUE;
            LOG_STEP(("stepControl_handleStep: completed, fromDepth==currentDepth(%d) and min", fromDepth));
        } else {
            if (step->fromLine != -1) {
                jint line = -1;
                jlocation location;
                jmethodID method;
                WITH_LOCAL_REFS(env, 1) {
                    jclass clazz;
                    error = getFrameLocation(thread,
                                        &clazz, &method, &location);
                    if ( isMethodObsolete(method)) {
                        method = NULL;
                        location = -1;
                    }
                    if (error != JVMTI_ERROR_NONE || location == -1) {
                        EXIT_ERROR(error, "getting frame location");
                    }
                    if ( method == step->method ) {
                        LOG_STEP(("stepControl_handleStep: checking line location"));
                        log_debugee_location("stepControl_handleStep: checking line loc",
                                thread, method, location);
                        line = findLineNumber(thread, location,
                                      step->lineEntries, step->lineEntryCount);
                    }
                    if (line != step->fromLine) {
                        completed = JNI_TRUE;
                        LOG_STEP(("stepControl_handleStep: completed, fromDepth==currentDepth(%d) and different line", fromDepth));
                    }
                } END_WITH_LOCAL_REFS(env);
            } else {
                /*
                 * This is a rare case. We have stepped from a location
                 * inside a native method to a location within a Java
                 * method at the same stack depth. This means that
                 * the original native method returned to another
                 * native method which, in turn, invoked a Java method.
                 *
                 * Since the original frame was  native, we were unable
                 * to ask for a frame pop event, and, thus, could not
                 * set the step->frameExited flag when the original
                 * method was done. Instead we end up here
                 * and act just as though the frameExited flag was set
                 * and complete the step immediately.
                 */
                completed = JNI_TRUE;
                LOG_STEP(("stepControl_handleStep: completed, fromDepth==currentDepth(%d) and no line", fromDepth));
            }
        }
        LOG_STEP(("stepControl_handleStep: finished"));
    }
done:
    if (completed) {
        completeStep(env, thread, step);
    }
    stepControl_unlock();
    return completed;
}


void
stepControl_initialize(void)
{
    stepLock = debugMonitorCreate("JDWP Step Handler Lock");
}

void
stepControl_reset(void)
{
}

/*
 * Reset step control request stack depth and line number.
 */
void
stepControl_resetRequest(jthread thread)
{

    StepRequest *step;
    jvmtiError error;

    LOG_STEP(("stepControl_resetRequest: thread=%p", thread));

    stepControl_lock();

    step = threadControl_getStepRequest(thread);

    if (step != NULL) {
        JNIEnv *env;
        env = getEnv();
        error = initState(env, thread, step);
        if (error != JVMTI_ERROR_NONE) {
            EXIT_ERROR(error, "initializing step state");
        }
    } else {
        EXIT_ERROR(AGENT_ERROR_INVALID_THREAD, "getting step request");
    }

    stepControl_unlock();
}

static void
initEvents(jthread thread, StepRequest *step)
{
    /* Need to install frame pop handler and exception catch handler when
     * single-stepping is enabled (i.e. step-into or step-over/step-out
     * when fromStackDepth > 0).
     */
    if (step->depth == JDWP_STEP_DEPTH(INTO) || step->fromStackDepth > 0) {
        /*
         * TO DO: These might be able to applied more selectively to
         * boost performance.
         */
        step->catchHandlerNode = eventHandler_createInternalThreadOnly(
                                     EI_EXCEPTION_CATCH,
                                     handleExceptionCatchEvent,
                                     thread);
        JDI_ASSERT(step->framePopHandlerNode == NULL);
        step->framePopHandlerNode = eventHandler_createInternalThreadOnly(
                                        EI_FRAME_POP,
                                        handleFramePopEvent,
                                        thread);

        if (step->catchHandlerNode == NULL ||
            step->framePopHandlerNode == NULL) {
            EXIT_ERROR(AGENT_ERROR_INVALID_EVENT_TYPE,
                        "installing step event handlers");
        }

    }
    /*
     * Initially enable stepping:
     * 1) For step into, always
     * 2) For step over, unless right after the VM_INIT.
     *    Enable stepping for STEP_MIN or STEP_LINE with or without line numbers.
     *    If the class is redefined then non EMCP methods may not have line
     *    number info. So enable line stepping for non line number so that it
     *    behaves like STEP_MIN/STEP_OVER.
     * 3) For step out, only if stepping from native, except right after VM_INIT
     *
     * (right after VM_INIT, a step->over or out is identical to running
     * forever)
     */
    switch (step->depth) {
        case JDWP_STEP_DEPTH(INTO):
            enableStepping(thread);
            break;
        case JDWP_STEP_DEPTH(OVER):
            if (step->fromStackDepth > 0 && !step->fromNative ) {
              enableStepping(thread);
            }
            break;
        case JDWP_STEP_DEPTH(OUT):
            if (step->fromNative &&
                (step->fromStackDepth > 0)) {
                enableStepping(thread);
            }
            break;
        default:
            JDI_ASSERT(JNI_FALSE);
    }
}

jvmtiError
stepControl_beginStep(JNIEnv *env, jthread thread, jint size, jint depth,
                      HandlerNode *node)
{
    StepRequest *step;
    jvmtiError error;
    jvmtiError error2;

    LOG_STEP(("stepControl_beginStep: thread=%p,size=%d,depth=%d",
              thread, size, depth));

    eventHandler_lock(); /* for proper lock order */
    stepControl_lock();

    step = threadControl_getStepRequest(thread);
    if (step == NULL) {
        error = AGENT_ERROR_INVALID_THREAD;
        /* Normally not getting a StepRequest struct pointer is a fatal error
         *   but on a beginStep, we just return an error code.
         */
    } else {
        /*
         * In case the thread isn't already suspended, do it again.
         */
        error = threadControl_suspendThread(thread, JNI_FALSE);
        if (error == JVMTI_ERROR_NONE) {
            /*
             * Overwrite any currently executing step.
             */
            step->granularity = size;
            step->depth = depth;
            step->catchHandlerNode = NULL;
            step->framePopHandlerNode = NULL;
            step->methodEnterHandlerNode = NULL;
            step->stepHandlerNode = node;
            error = initState(env, thread, step);
            if (error == JVMTI_ERROR_NONE) {
                initEvents(thread, step);
            }
            /* false means it is not okay to unblock the commandLoop thread */
            error2 = threadControl_resumeThread(thread, JNI_FALSE);
            if (error2 != JVMTI_ERROR_NONE && error == JVMTI_ERROR_NONE) {
                error = error2;
            }

            /*
             * If everything went ok, indicate a step is pending.
             */
            if (error == JVMTI_ERROR_NONE) {
                step->pending = JNI_TRUE;
            }
        } else {
            EXIT_ERROR(error, "stepControl_beginStep: cannot suspend thread");
        }
    }

    stepControl_unlock();
    eventHandler_unlock();

    return error;
}


static void
clearStep(jthread thread, StepRequest *step)
{
    if (step->pending) {

        disableStepping(thread);
        if ( step->catchHandlerNode != NULL ) {
            (void)eventHandler_free(step->catchHandlerNode);
            step->catchHandlerNode = NULL;
        }
        if ( step->framePopHandlerNode!= NULL ) {
            (void)eventHandler_free(step->framePopHandlerNode);
            step->framePopHandlerNode = NULL;
        }
        if ( step->methodEnterHandlerNode != NULL ) {
            (void)eventHandler_free(step->methodEnterHandlerNode);
            step->methodEnterHandlerNode = NULL;
        }
        step->pending = JNI_FALSE;

        /*
         * Warning: Do not clear step->method, step->lineEntryCount,
         *          or step->lineEntries here, they will likely
         *          be needed on the next step.
         */

    }
}

jvmtiError
stepControl_endStep(jthread thread)
{
    StepRequest *step;
    jvmtiError error;

    LOG_STEP(("stepControl_endStep: thread=%p", thread));

    eventHandler_lock(); /* for proper lock order */
    stepControl_lock();

    step = threadControl_getStepRequest(thread);
    if (step != NULL) {
        clearStep(thread, step);
        error = JVMTI_ERROR_NONE;
    } else {
        /* If the stepRequest can't be gotten, then this thread no longer
         *   exists, just return, don't die here, this is normal at
         *   termination time. Return JVMTI_ERROR_NONE so the thread Ref
         *   can be tossed.
         */
         error = JVMTI_ERROR_NONE;
    }

    stepControl_unlock();
    eventHandler_unlock();

    return error;
}

void
stepControl_clearRequest(jthread thread, StepRequest *step)
{
    LOG_STEP(("stepControl_clearRequest: thread=%p", thread));
    clearStep(thread, step);
}

void
stepControl_lock(void)
{
    debugMonitorEnter(stepLock);
}

void
stepControl_unlock(void)
{
    debugMonitorExit(stepLock);
}
