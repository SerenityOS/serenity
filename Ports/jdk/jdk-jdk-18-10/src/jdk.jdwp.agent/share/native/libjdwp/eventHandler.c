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
/*
 * eventHandler
 *
 * This module handles events as they come in directly from JVMTI
 * and also maps them to JDI events.  JDI events are those requested
 * at the JDI or JDWP level and seen on those levels.  Mapping is
 * one-to-many, a JVMTI event may map to several JDI events, or
 * to none.  Part of that mapping process is filteration, which
 * eventFilter sub-module handles.  A JDI EventRequest corresponds
 * to a HandlerNode and a JDI filter to the hidden HandlerNode data
 * used by eventFilter.  For example, if at the JDI level the user
 * executed:
 *
 *   EventRequestManager erm = vm.eventRequestManager();
 *   BreakpointRequest bp = erm.createBreakpointRequest();
 *   bp.enable();
 *   ClassPrepareRequest req = erm.createClassPrepareRequest();
 *   req.enable();
 *   req = erm.createClassPrepareRequest();
 *   req.addClassFilter("Foo*");
 *   req.enable();
 *
 * Three handlers would be created, the first with a LocationOnly
 * filter and the last with a ClassMatch  filter.
 * When a JVMTI class prepare event for "Foobar"
 * comes in, the second handler will create one JDI event, the
 * third handler will compare the class signature, and since
 * it matchs create a second event.  There may also be internal
 * events as there are in this case, one created by the front-end
 * and one by the back-end.
 *
 * Each event kind has a handler chain, which is a doublely linked
 * list of handlers for that kind of event.
 */
#include "util.h"
#include "eventHandler.h"
#include "eventHandlerRestricted.h"
#include "eventFilter.h"
#include "eventFilterRestricted.h"
#include "standardHandlers.h"
#include "threadControl.h"
#include "eventHelper.h"
#include "classTrack.h"
#include "commonRef.h"
#include "debugLoop.h"
#include "signature.h"

static HandlerID requestIdCounter;
static jbyte currentSessionID;

/* Counter of active callbacks and flag for vm_death */
static int      active_callbacks   = 0;
static jboolean vm_death_callback_active = JNI_FALSE;
static jrawMonitorID callbackLock;
static jrawMonitorID callbackBlock;

/* Macros to surround callback code (non-VM_DEATH callbacks).
 *   Note that this just keeps a count of the non-VM_DEATH callbacks that
 *   are currently active, it does not prevent these callbacks from
 *   operating in parallel. It's the VM_DEATH callback that will wait
 *   for all these callbacks to finish up, so that it can report the
 *   VM_DEATH in a clean state.
 *   If the VM_DEATH callback is active in the BEGIN macro then this
 *   callback just blocks until released by the VM_DEATH callback.
 *   If the VM_DEATH callback is active in the END macro, then this
 *   callback will notify the VM_DEATH callback if it's the last one,
 *   and then block until released by the VM_DEATH callback.
 *   Why block? These threads are often the threads of the Java program,
 *   not blocking might mean that a return would continue execution of
 *   some java thread in the middle of VM_DEATH, this seems troubled.
 *
 *   WARNING: No not 'return' or 'goto' out of the BEGIN_CALLBACK/END_CALLBACK
 *            block, this will mess up the count.
 */

#define BEGIN_CALLBACK()                                                \
{ /* BEGIN OF CALLBACK */                                               \
    jboolean bypass = JNI_TRUE;                                         \
    debugMonitorEnter(callbackLock); {                                  \
        if (vm_death_callback_active) {                                 \
            /* allow VM_DEATH callback to finish */                     \
            debugMonitorExit(callbackLock);                             \
            /* Now block because VM is about to die */                  \
            debugMonitorEnter(callbackBlock);                           \
            debugMonitorExit(callbackBlock);                            \
        } else {                                                        \
            active_callbacks++;                                         \
            bypass = JNI_FALSE;                                         \
            debugMonitorExit(callbackLock);                             \
        }                                                               \
    }                                                                   \
    if ( !bypass ) {                                                    \
        /* BODY OF CALLBACK CODE */

#define END_CALLBACK() /* Part of bypass if body */                     \
        debugMonitorEnter(callbackLock); {                              \
            active_callbacks--;                                         \
            if (active_callbacks < 0) {                                 \
                EXIT_ERROR(0, "Problems tracking active callbacks");    \
            }                                                           \
            if (vm_death_callback_active) {                             \
                if (active_callbacks == 0) {                            \
                    debugMonitorNotifyAll(callbackLock);                \
                }                                                       \
                /* allow VM_DEATH callback to finish */                 \
                debugMonitorExit(callbackLock);                         \
                /* Now block because VM is about to die */              \
                debugMonitorEnter(callbackBlock);                       \
                debugMonitorExit(callbackBlock);                        \
            } else {                                                    \
                debugMonitorExit(callbackLock);                         \
            }                                                           \
        }                                                               \
    }                                                                   \
} /* END OF CALLBACK */

/*
 * We are starting with a very simple locking scheme
 * for event handling.  All readers and writers of data in
 * the handlers[] chain must own this lock for the duration
 * of its use. If contention becomes a problem, we can:
 *
 * 1) create a lock per event type.
 * 2) move to a readers/writers approach where multiple threads
 * can access the chains simultaneously while reading (the
 * normal activity of an event callback).
 */
static jrawMonitorID handlerLock;

typedef struct HandlerChain_ {
    HandlerNode *first;
    /* add lock here */
} HandlerChain;

/*
 * This array maps event kinds to handler chains.
 * Protected by handlerLock.
 */

static HandlerChain __handlers[EI_max-EI_min+1];

/* Given a HandlerNode, these access our private data.
 */
#define PRIVATE_DATA(node) \
       (&(((EventHandlerRestricted_HandlerNode*)(void*)(node))->private_ehpd))

#define NEXT(node) (PRIVATE_DATA(node)->private_next)
#define PREV(node) (PRIVATE_DATA(node)->private_prev)
#define CHAIN(node) (PRIVATE_DATA(node)->private_chain)
#define HANDLER_FUNCTION(node) (PRIVATE_DATA(node)->private_handlerFunction)

static jclass getObjectClass(jobject object);
static jvmtiError freeHandler(HandlerNode *node);

static jvmtiError freeHandlerChain(HandlerChain *chain);

static HandlerChain *
getHandlerChain(EventIndex i)
{
    if ( i < EI_min || i > EI_max ) {
        EXIT_ERROR(AGENT_ERROR_INVALID_EVENT_TYPE,"bad index for handler");
    }
    return &(__handlers[i-EI_min]);
}

static void
insert(HandlerChain *chain, HandlerNode *node)
{
    HandlerNode *oldHead = chain->first;
    NEXT(node) = oldHead;
    PREV(node) = NULL;
    CHAIN(node) = chain;
    if (oldHead != NULL) {
        PREV(oldHead) = node;
    }
    chain->first = node;
}

static HandlerNode *
findInChain(HandlerChain *chain, HandlerID handlerID)
{
    HandlerNode *node = chain->first;
    while (node != NULL) {
        if (node->handlerID == handlerID) {
            return node;
        }
        node = NEXT(node);
    }
    return NULL;
}

static HandlerNode *
find(EventIndex ei, HandlerID handlerID)
{
    return findInChain(getHandlerChain(ei), handlerID);
}

/**
 * Deinsert.  Safe for non-inserted nodes.
 */
static void
deinsert(HandlerNode *node)
{
    HandlerChain *chain = CHAIN(node);

    if (chain == NULL) {
        return;
    }
    if (chain->first == node) {
        chain->first = NEXT(node);
    }
    if (NEXT(node) != NULL) {
        PREV(NEXT(node)) = PREV(node);
    }
    if (PREV(node) != NULL) {
        NEXT(PREV(node)) = NEXT(node);
    }
    CHAIN(node) = NULL;
}

jboolean
eventHandlerRestricted_iterator(EventIndex ei,
                              IteratorFunction func, void *arg)
{
    HandlerChain *chain;
    HandlerNode *node;
    JNIEnv *env;

    chain = getHandlerChain(ei);
    node = chain->first;
    env = getEnv();

    if ( func == NULL ) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL,"iterator function NULL");
    }

    while (node != NULL) {
        if (((func)(env, node, arg))) {
            return JNI_TRUE;
        }
        node = NEXT(node);
    }
    return JNI_FALSE;
}

/* BREAKPOINT, METHOD_ENTRY and SINGLE_STEP events are covered by
 * the co-location of events policy. Of these three co-located
 * events, METHOD_ENTRY is always reported first and BREAKPOINT
 * is always reported last. Here are the possible combinations and
 * their order:
 *
 * (p1) METHOD_ENTRY, BREAKPOINT (existing)
 * (p2) METHOD_ENTRY, BREAKPOINT (new)
 * (p1) METHOD_ENTRY, SINGLE_STEP
 * (p1) METHOD_ENTRY, SINGLE_STEP, BREAKPOINT (existing)
 * (p1/p2) METHOD_ENTRY, SINGLE_STEP, BREAKPOINT (new)
 * (p1) SINGLE_STEP, BREAKPOINT (existing)
 * (p2) SINGLE_STEP, BREAKPOINT (new)
 *
 * BREAKPOINT (existing) indicates a BREAKPOINT that is set before
 * the other co-located event is posted. BREAKPOINT (new) indicates
 * a BREAKPOINT that is set after the other co-located event is
 * posted and before the thread has resumed execution.
 *
 * Co-location of events policy used to be implemented via
 * temporary BREAKPOINTs along with deferring the reporting of
 * non-BREAKPOINT co-located events, but the temporary BREAKPOINTs
 * caused performance problems on VMs where setting or clearing
 * BREAKPOINTs is expensive, e.g., HotSpot.
 *
 * The policy is now implemented in two phases. Phase 1: when a
 * METHOD_ENTRY or SINGLE_STEP event is received, if there is an
 * existing co-located BREAKPOINT, then the current event is
 * deferred. When the BREAKPOINT event is processed, the event
 * bag will contain the deferred METHOD_ENTRY and/or SINGLE_STEP
 * events along with the BREAKPOINT event. For a METHOD_ENTRY
 * event where there is not an existing co-located BREAKPOINT,
 * if SINGLE_STEP events are also enabled for the thread, then
 * the METHOD_ENTRY event is deferred. When the SINGLE_STEP event
 * is processed, the event bag will also contain the deferred
 * METHOD_ENTRY event. This covers each of the combinations
 * marked with 'p1' above.
 *
 * Phase 2: if there is no existing co-located BREAKPOINT, then the
 * location information for the METHOD_ENTRY or SINGLE_STEP event
 * is recorded in the ThreadNode. If the next event for the thread
 * is a co-located BREAKPOINT, then the first BREAKPOINT event will
 * be skipped since it cannot be delivered in the same event set.
 * This covers each of the combinations marked with 'p2' above.
 *
 * For the combination marked p1/p2, part of the case is handled
 * during phase 1 and the rest is handled during phase 2.
 *
 * The recording of information in the ThreadNode is handled in
 * this routine. The special handling of the next event for the
 * thread is handled in skipEventReport().
 */

static jboolean
deferEventReport(JNIEnv *env, jthread thread,
            EventIndex ei, jclass clazz, jmethodID method, jlocation location)
{
    jboolean deferring = JNI_FALSE;

    switch (ei) {
        case EI_METHOD_ENTRY:
            if (!isMethodNative(method)) {
                jvmtiError error;
                jlocation start;
                jlocation end;
                error = methodLocation(method, &start, &end);
                if (error == JVMTI_ERROR_NONE) {
                    deferring = isBreakpointSet(clazz, method, start) ||
                                threadControl_getInstructionStepMode(thread)
                                    == JVMTI_ENABLE;
                    if (!deferring) {
                        threadControl_saveCLEInfo(env, thread, ei,
                                                  clazz, method, start);
                    }
                }
            }
            break;
        case EI_SINGLE_STEP:
            deferring = isBreakpointSet(clazz, method, location);
            if (!deferring) {
                threadControl_saveCLEInfo(env, thread, ei,
                                          clazz, method, location);
            }
            break;
        default:
            break;
    }
    /* TO DO: Once JVMTI supports a way to know if we're
     * at the end of a method, we should check here for
     * break and step events which precede a method exit
     * event.
     */
    return deferring;
}

/* Handle phase 2 of the co-located events policy. See detailed
 * comments in deferEventReport() above.
 */
static jboolean
skipEventReport(JNIEnv *env, jthread thread, EventIndex ei,
                        jclass clazz, jmethodID method, jlocation location)
{
    jboolean skipping = JNI_FALSE;

    if (ei == EI_BREAKPOINT) {
        if (threadControl_cmpCLEInfo(env, thread, clazz, method, location)) {
            LOG_MISC(("Co-located breakpoint event found: "
                "%s,thread=%p,clazz=%p,method=%p,location=%d",
                eventText(ei), thread, clazz, method, location));
            skipping = JNI_TRUE;
        }
    }

    threadControl_clearCLEInfo(env, thread);

    return skipping;
}

static void
reportEvents(JNIEnv *env, jbyte sessionID, jthread thread, EventIndex ei,
             jclass clazz, jmethodID method, jlocation location,
             struct bag *eventBag)
{
    jbyte suspendPolicy;
    jboolean invoking;

    if (bagSize(eventBag) < 1) {
        return;
    }

    /*
     * Never report events before initialization completes
     */
    if (!debugInit_isInitComplete()) {
        return;
    }

    /*
     * Check to see if we should skip reporting this event due to
     * co-location of events policy.
     */
    if (thread != NULL &&
           skipEventReport(env, thread, ei, clazz, method, location)) {
        LOG_MISC(("event report being skipped: "
            "ei=%s,thread=%p,clazz=%p,method=%p,location=%d",
            eventText(ei), thread, clazz, method, location));
        bagDeleteAll(eventBag);
        return;
    }

    /* We delay the reporting of some events so that they can be
     * properly grouped into event sets with upcoming events. If
     * the reporting is to be deferred, the event commands remain
     * in the event bag until a subsequent event occurs.  Event is
     * NULL for synthetic events (e.g. unload).
     */
    if (thread == NULL
         || !deferEventReport(env, thread, ei,
                        clazz, method, location)) {
        struct bag *completedBag = bagDup(eventBag);
        bagDeleteAll(eventBag);
        if (completedBag == NULL) {
            /*
             * TO DO: Report, but don't terminate?
             */
            return;
        } else {
            suspendPolicy = eventHelper_reportEvents(sessionID, completedBag);
            if (thread != NULL && suspendPolicy != JDWP_SUSPEND_POLICY(NONE)) {
                do {
                    /* The events have been reported and this
                     * thread is about to continue, but it may
                     * have been started up just to perform a
                     * requested method invocation. If so, we do
                     * the invoke now and then stop again waiting
                     * for another continue. By then another
                     * invoke request can be in place, so there is
                     * a loop around this code.
                     */
                    invoking = invoker_doInvoke(thread);
                    if (invoking) {
                        eventHelper_reportInvokeDone(sessionID, thread);
                    }
                } while (invoking);
            }
            bagDestroyBag(completedBag);
        }
    }
}

/* A bagEnumerateFunction.  Create a synthetic class unload event
 * for every class no longer present.  Analogous to event_callback
 * combined with a handler in a unload specific (no event
 * structure) kind of way.
 */
static jboolean
synthesizeUnloadEvent(void *signatureVoid, void *envVoid)
{
    JNIEnv *env = (JNIEnv *)envVoid;
    char *signature = *(char **)signatureVoid;
    char *classname;
    HandlerNode *node;
    jbyte eventSessionID = currentSessionID;
    struct bag *eventBag = eventHelper_createEventBag();

    /* TO DO: Report null error, but don't die */
    JDI_ASSERT(eventBag != NULL);

    /* Signature needs to last, so convert extra copy to
     * classname
     */
    classname = jvmtiAllocate((int)strlen(signature)+1);
    (void)strcpy(classname, signature);
    convertSignatureToClassname(classname);

    debugMonitorEnter(handlerLock);

    node = getHandlerChain(EI_GC_FINISH)->first;
    while (node != NULL) {
        /* save next so handlers can remove themselves */
        HandlerNode *next = NEXT(node);
        jboolean shouldDelete;

        if (eventFilterRestricted_passesUnloadFilter(env, classname,
                                                     node,
                                                     &shouldDelete)) {
            /* There may be multiple handlers, the signature will
             * be freed when the event helper thread has written
             * it.  So each event needs a separate allocation.
             */
            char *durableSignature = jvmtiAllocate((int)strlen(signature)+1);
            (void)strcpy(durableSignature, signature);

            eventHelper_recordClassUnload(node->handlerID,
                                          durableSignature,
                                          eventBag);
        }
        if (shouldDelete) {
            /* We can safely free the node now that we are done
             * using it.
             */
            (void)freeHandler(node);
        }
        node = next;
    }

    debugMonitorExit(handlerLock);

    if (eventBag != NULL) {
        reportEvents(env, eventSessionID, (jthread)NULL, 0,
                            (jclass)NULL, (jmethodID)NULL, 0, eventBag);

        /*
         * bag was created locally, destroy it here.
         */
        bagDestroyBag(eventBag);
    }

    jvmtiDeallocate(signature);
    jvmtiDeallocate(classname);

    return JNI_TRUE;
}

/* Garbage Collection Happened */
static unsigned int garbageCollected = 0;

/*
 * The JVMTI generic event callback. Each event is passed to a sequence of
 * handlers in a chain until the chain ends or one handler
 * consumes the event.
 */
static void
event_callback(JNIEnv *env, EventInfo *evinfo)
{
    struct bag *eventBag;
    jbyte eventSessionID = currentSessionID; /* session could change */
    jthrowable currentException;
    jthread thread;
    EventIndex ei = evinfo->ei;

    LOG_MISC(("event_callback(): ei=%s", eventText(ei)));
    log_debugee_location("event_callback()", evinfo->thread, evinfo->method, evinfo->location);

    /* We want to preserve any current exception that might get
     * wiped out during event handling (e.g. JNI calls). We have
     * to rely on space for the local reference on the current
     * frame because doing a PushLocalFrame here might itself
     * generate an exception.
     */
    currentException = JNI_FUNC_PTR(env,ExceptionOccurred)(env);
    JNI_FUNC_PTR(env,ExceptionClear)(env);

    /* See if a garbage collection finish event happened earlier.
     *
     * Note: The "if" is an optimization to avoid entering the lock on every
     *       event; garbageCollected may be zapped before we enter
     *       the lock but then this just becomes one big no-op.
     */
    if ( garbageCollected > 0 ) {
        struct bag *unloadedSignatures = NULL;

        /* We want to compact the hash table of all
         * objects sent to the front end by removing objects that have
         * been collected.
         */
        commonRef_compact();

        /* We also need to simulate the class unload events. */

        debugMonitorEnter(handlerLock);

        /* Clear garbage collection counter */
        garbageCollected = 0;

        /* Analyze which class unloads occurred */
        unloadedSignatures = classTrack_processUnloads(env);

        debugMonitorExit(handlerLock);

        /* Generate the synthetic class unload events and/or just cleanup.  */
        if ( unloadedSignatures != NULL ) {
            (void)bagEnumerateOver(unloadedSignatures, synthesizeUnloadEvent,
                             (void *)env);
            bagDestroyBag(unloadedSignatures);
        }
    }

    thread = evinfo->thread;
    if (thread != NULL) {
        /*
         * Record the fact that we're entering an event
         * handler so that thread operations (status, interrupt,
         * stop) can be done correctly and so that thread
         * resources can be allocated.  This must be done before
         * grabbing any locks.
         */
        eventBag = threadControl_onEventHandlerEntry(eventSessionID, evinfo, currentException);
        if ( eventBag == NULL ) {
            jboolean invoking;
            do {
                /* The event has been 'handled' and this
                 * thread is about to continue, but it may
                 * have been started up just to perform a
                 * requested method invocation. If so, we do
                 * the invoke now and then stop again waiting
                 * for another continue. By then another
                 * invoke request can be in place, so there is
                 * a loop around this code.
                 */
                invoking = invoker_doInvoke(thread);
                if (invoking) {
                    eventHelper_reportInvokeDone(eventSessionID, thread);
                }
            } while (invoking);
            return; /* Do nothing, event was consumed */
        }
    } else {
        eventBag = eventHelper_createEventBag();
        if (eventBag == NULL) {
            /*
             * TO DO: Report, but don't die
             */
            eventBag = NULL;  /* to shut up lint */
        }
    }

    debugMonitorEnter(handlerLock);
    {
        HandlerNode *node;
        char        *classname;

        /* We must keep track of all classes prepared to know what's unloaded */
        if (evinfo->ei == EI_CLASS_PREPARE) {
            classTrack_addPreparedClass(env, evinfo->clazz);
        }

        node = getHandlerChain(evinfo->ei)->first;
        classname = getClassname(evinfo->clazz);

        while (node != NULL) {
            /* save next so handlers can remove themselves */
            HandlerNode *next = NEXT(node);
            jboolean shouldDelete;

            if (eventFilterRestricted_passesFilter(env, classname,
                                                   evinfo, node,
                                                   &shouldDelete)) {
                HandlerFunction func;

                func = HANDLER_FUNCTION(node);
                if ( func == NULL ) {
                    EXIT_ERROR(AGENT_ERROR_INTERNAL,"handler function NULL");
                }
                (*func)(env, evinfo, node, eventBag);
            }
            if (shouldDelete) {
                /* We can safely free the node now that we are done
                 * using it.
                 */
                (void)freeHandler(node);
            }
            node = next;
        }
        jvmtiDeallocate(classname);
    }
    debugMonitorExit(handlerLock);

    if (eventBag != NULL) {
        reportEvents(env, eventSessionID, thread, evinfo->ei,
                evinfo->clazz, evinfo->method, evinfo->location, eventBag);
    }

    /* we are continuing after VMDeathEvent - now we are dead */
    if (evinfo->ei == EI_VM_DEATH) {
        gdata->vmDead = JNI_TRUE;
    }

    /*
     * If the bag was created locally, destroy it here.
     */
    if (thread == NULL) {
        bagDestroyBag(eventBag);
    }

    /* Always restore any exception that was set beforehand.  If
     * there is a pending async exception, StopThread will be
     * called from threadControl_onEventHandlerExit immediately
     * below.  Depending on VM implementation and state, the async
     * exception might immediately overwrite the currentException,
     * or it might be delayed until later.  */
    if (currentException != NULL) {
        JNI_FUNC_PTR(env,Throw)(env, currentException);
    } else {
        JNI_FUNC_PTR(env,ExceptionClear)(env);
    }

    /*
     * Release thread resources and perform any delayed operations.
     */
    if (thread != NULL) {
        threadControl_onEventHandlerExit(evinfo->ei, thread, eventBag);
    }
}

/* Returns a local ref to the declaring class for an object. */
static jclass
getObjectClass(jobject object)
{
    jclass clazz;
    JNIEnv *env = getEnv();

    clazz = JNI_FUNC_PTR(env,GetObjectClass)(env, object);

    return clazz;
}

/* Returns a local ref to the declaring class for a method, or NULL. */
jclass
getMethodClass(jvmtiEnv *jvmti_env, jmethodID method)
{
    jclass clazz = NULL;
    jvmtiError error;

    if ( method == NULL ) {
        return NULL;
    }
    error = methodClass(method, &clazz);
    if ( error != JVMTI_ERROR_NONE ) {
        EXIT_ERROR(error,"Can't get jclass for a methodID, invalid?");
        return NULL;
    }
    return clazz;
}

/* Event callback for JVMTI_EVENT_SINGLE_STEP */
static void JNICALL
cbSingleStep(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jmethodID method, jlocation location)
{
    EventInfo info;

    LOG_CB(("cbSingleStep: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_SINGLE_STEP;
        info.thread     = thread;
        info.clazz      = getMethodClass(jvmti_env, method);
        info.method     = method;
        info.location   = location;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbSingleStep"));
}

/* Event callback for JVMTI_EVENT_BREAKPOINT */
static void JNICALL
cbBreakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jmethodID method, jlocation location)
{
    EventInfo info;

    LOG_CB(("cbBreakpoint: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_BREAKPOINT;
        info.thread     = thread;
        info.clazz      = getMethodClass(jvmti_env, method);
        info.method     = method;
        info.location   = location;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbBreakpoint"));
}

/* Event callback for JVMTI_EVENT_FRAME_POP */
static void JNICALL
cbFramePop(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jmethodID method,
                        jboolean wasPoppedByException)
{
    EventInfo info;

    /* JDWP does not return these events when popped due to an exception. */
    if ( wasPoppedByException ) {
        return;
    }

    LOG_CB(("cbFramePop: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_FRAME_POP;
        info.thread     = thread;
        info.clazz      = getMethodClass(jvmti_env, method);
        info.method     = method;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbFramePop"));
}

/* Event callback for JVMTI_EVENT_EXCEPTION */
static void JNICALL
cbException(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jmethodID method,
                        jlocation location, jobject exception,
                        jmethodID catch_method, jlocation catch_location)
{
    EventInfo info;

    LOG_CB(("cbException: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei                         = EI_EXCEPTION;
        info.thread                     = thread;
        info.clazz                      = getMethodClass(jvmti_env, method);
        info.method                     = method;
        info.location                   = location;
        info.object                     = exception;
        info.u.exception.catch_clazz    = getMethodClass(jvmti_env, catch_method);
        info.u.exception.catch_method   = catch_method;
        info.u.exception.catch_location = catch_location;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbException"));
}

/* Event callback for JVMTI_EVENT_THREAD_START */
static void JNICALL
cbThreadStart(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread)
{
    EventInfo info;

    LOG_CB(("cbThreadStart: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_THREAD_START;
        info.thread     = thread;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbThreadStart"));
}

/* Event callback for JVMTI_EVENT_THREAD_END */
static void JNICALL
cbThreadEnd(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread)
{
    EventInfo info;

    LOG_CB(("cbThreadEnd: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_THREAD_END;
        info.thread     = thread;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbThreadEnd"));
}

/* Event callback for JVMTI_EVENT_CLASS_PREPARE */
static void JNICALL
cbClassPrepare(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jclass klass)
{
    EventInfo info;

    LOG_CB(("cbClassPrepare: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_CLASS_PREPARE;
        info.thread     = thread;
        info.clazz      = klass;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbClassPrepare"));
}

/* Event callback for JVMTI_EVENT_GARBAGE_COLLECTION_FINISH */
static void JNICALL
cbGarbageCollectionFinish(jvmtiEnv *jvmti_env)
{
    LOG_CB(("cbGarbageCollectionFinish"));
    ++garbageCollected;
    LOG_MISC(("END cbGarbageCollectionFinish"));
}

/* Event callback for JVMTI_EVENT_CLASS_LOAD */
static void JNICALL
cbClassLoad(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jclass klass)
{
    EventInfo info;

    LOG_CB(("cbClassLoad: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_CLASS_LOAD;
        info.thread     = thread;
        info.clazz      = klass;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbClassLoad"));
}

/* Event callback for JVMTI_EVENT_FIELD_ACCESS */
static void JNICALL
cbFieldAccess(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jmethodID method,
                        jlocation location, jclass field_klass,
                        jobject object, jfieldID field)
{
    EventInfo info;

    LOG_CB(("cbFieldAccess: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei                         = EI_FIELD_ACCESS;
        info.thread                     = thread;
        info.clazz                      = getMethodClass(jvmti_env, method);
        info.method                     = method;
        info.location                   = location;
        info.u.field_access.field_clazz = field_klass;
        info.object                     = object;
        info.u.field_access.field       = field;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbFieldAccess"));
}

/* Event callback for JVMTI_EVENT_FIELD_MODIFICATION */
static void JNICALL
cbFieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method,
        jlocation location, jclass field_klass, jobject object, jfieldID field,
        char signature_type, jvalue new_value)
{
    EventInfo info;

    LOG_CB(("cbFieldModification: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei                                 = EI_FIELD_MODIFICATION;
        info.thread                             = thread;
        info.clazz                              = getMethodClass(jvmti_env, method);
        info.method                             = method;
        info.location                           = location;
        info.u.field_modification.field         = field;
        info.u.field_modification.field_clazz   = field_klass;
        info.object                             = object;
        info.u.field_modification.signature_type= signature_type;
        info.u.field_modification.new_value     = new_value;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbFieldModification"));
}

/* Event callback for JVMTI_EVENT_EXCEPTION_CATCH */
static void JNICALL
cbExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jlocation location, jobject exception)
{
    EventInfo info;

    LOG_CB(("cbExceptionCatch: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_EXCEPTION_CATCH;
        info.thread     = thread;
        info.clazz      = getMethodClass(jvmti_env, method);
        info.method     = method;
        info.location   = location;
        info.object     = exception;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbExceptionCatch"));
}

/* Event callback for JVMTI_EVENT_METHOD_ENTRY */
static void JNICALL
cbMethodEntry(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jmethodID method)
{
    EventInfo info;

    LOG_CB(("cbMethodEntry: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_METHOD_ENTRY;
        info.thread     = thread;
        info.clazz      = getMethodClass(jvmti_env, method);
        info.method     = method;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbMethodEntry"));
}

/* Event callback for JVMTI_EVENT_METHOD_EXIT */
static void JNICALL
cbMethodExit(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jmethodID method,
                        jboolean wasPoppedByException, jvalue return_value)
{
    EventInfo info;

    /* JDWP does not return these events when popped due to an exception. */
    if ( wasPoppedByException ) {
        return;
    }

    LOG_CB(("cbMethodExit: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_METHOD_EXIT;
        info.thread     = thread;
        info.clazz      = getMethodClass(jvmti_env, method);
        info.method     = method;
        info.u.method_exit.return_value = return_value;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbMethodExit"));
}

/* Event callback for JVMTI_EVENT_MONITOR_CONTENDED_ENTER */
static void JNICALL
cbMonitorContendedEnter(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jobject object)
{
    EventInfo info;
    jvmtiError error;
    jmethodID  method;
    jlocation  location;

    LOG_CB(("cbMonitorContendedEnter: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_MONITOR_CONTENDED_ENTER;
        info.thread     = thread;
        info.object     = object;
        /* get current location of contended monitor enter */
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameLocation)
                (gdata->jvmti, thread, 0, &method, &location);
        if (error == JVMTI_ERROR_NONE) {
            info.location = location;
            info.method   = method;
            info.clazz    = getMethodClass(jvmti_env, method);
        } else {
            info.location = -1;
        }
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbMonitorContendedEnter"));
}

/* Event callback for JVMTI_EVENT_MONITOR_CONTENDED_ENTERED */
static void JNICALL
cbMonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jobject object)
{
    EventInfo info;
    jvmtiError error;
    jmethodID  method;
    jlocation  location;

    LOG_CB(("cbMonitorContendedEntered: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_MONITOR_CONTENDED_ENTERED;
        info.thread     = thread;
        info.object     = object;
        /* get current location of contended monitor enter */
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameLocation)
                (gdata->jvmti, thread, 0, &method, &location);
        if (error == JVMTI_ERROR_NONE) {
            info.location = location;
            info.method   = method;
            info.clazz    = getMethodClass(jvmti_env, method);
        } else {
            info.location = -1;
        }
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbMonitorContendedEntered"));
}

/* Event callback for JVMTI_EVENT_MONITOR_WAIT */
static void JNICALL
cbMonitorWait(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jobject object,
                        jlong timeout)
{
    EventInfo info;
    jvmtiError error;
    jmethodID  method;
    jlocation  location;

    LOG_CB(("cbMonitorWait: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_MONITOR_WAIT;
        info.thread     = thread;
        info.object     = object;
        /* The info.clazz is used for both class filtering and for location info.
         * For monitor wait event the class filtering is done for class of monitor
         * object. So here info.clazz is set to class of monitor object here and it
         * is reset to class of method before writing location info.
         * See writeMonitorEvent in eventHelper.c
         */
        info.clazz      = getObjectClass(object);
        info.u.monitor.timeout = timeout;

        /* get location of monitor wait() method. */
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameLocation)
                (gdata->jvmti, thread, 0, &method, &location);
        if (error == JVMTI_ERROR_NONE) {
            info.location = location;
            info.method   = method;
        } else {
            info.location = -1;
        }
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbMonitorWait"));
}

/* Event callback for JVMTI_EVENT_MONITOR_WAIT */
static void JNICALL
cbMonitorWaited(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jthread thread, jobject object,
                        jboolean timed_out)
{
    EventInfo info;
    jvmtiError error;
    jmethodID  method;
    jlocation  location;

    LOG_CB(("cbMonitorWaited: thread=%p", thread));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_MONITOR_WAITED;
        info.thread     = thread;
        info.object     = object;
        /* The info.clazz is used for both class filtering and for location info.
         * For monitor waited event the class filtering is done for class of monitor
         * object. So here info.clazz is set to class of monitor object here and it
         * is reset to class of method before writing location info.
         * See writeMonitorEvent in eventHelper.c
         */
        info.clazz      = getObjectClass(object);
        info.u.monitor.timed_out = timed_out;

        /* get location of monitor wait() method */
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameLocation)
                (gdata->jvmti, thread, 0, &method, &location);
        if (error == JVMTI_ERROR_NONE) {
            info.location = location;
            info.method   = method;
        } else {
            info.location = -1;
        }
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbMonitorWaited"));
}

/* Event callback for JVMTI_EVENT_VM_INIT */
static void JNICALL
cbVMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread)
{
    EventInfo info;

    LOG_CB(("cbVMInit"));

    BEGIN_CALLBACK() {
        (void)memset(&info,0,sizeof(info));
        info.ei         = EI_VM_INIT;
        info.thread     = thread;
        event_callback(env, &info);
    } END_CALLBACK();

    LOG_MISC(("END cbVMInit"));
}

/* Event callback for JVMTI_EVENT_VM_DEATH */
static void JNICALL
cbVMDeath(jvmtiEnv *jvmti_env, JNIEnv *env)
{
    jvmtiError error;
    EventInfo info;
    LOG_CB(("cbVMDeath"));

    /* Setting this flag is needed by findThread(). It's ok to set it before
       the callbacks are cleared.*/
    gdata->jvmtiCallBacksCleared = JNI_TRUE;

    /* Clear out ALL callbacks at this time, we don't want any more. */
    /*    This should prevent any new BEGIN_CALLBACK() calls. */
    (void)memset(&(gdata->callbacks),0,sizeof(gdata->callbacks));
    error = JVMTI_FUNC_PTR(gdata->jvmti,SetEventCallbacks)
                (gdata->jvmti, &(gdata->callbacks), sizeof(gdata->callbacks));
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"Can't clear event callbacks on vm death");
    }

    /* Now that no new callbacks will be made, we need to wait for the ones
     *   that are still active to complete.
     *   The BEGIN_CALLBACK/END_CALLBACK macros implement the VM_DEATH
     *   callback protocol. Once the callback table is cleared (above),
     *   we can have callback threads in different stages:
     *   1) after callback function entry and before BEGIN_CALLBACK
     *      macro; we catch these threads with callbackBlock in the
     *      BEGIN_CALLBACK macro
     *   2) after BEGIN_CALLBACK macro and before END_CALLBACK macro; we
     *      catch these threads with callbackBlock in the END_CALLBACK
     *      macro
     *   3) after END_CALLBACK macro; these threads have made it past
     *      callbackBlock and callbackLock and don't count as active
     *
     *   Since some of the callback threads could be blocked or suspended
     *   we will resume all threads suspended by the debugger for a short
     *   time to flush out all callbacks. Note that the callback threads
     *   will block from returning to the VM in both macros. Some threads
     *   not associated with callbacks, but suspended by the debugger may
     *   continue on, but not for long.
     *   Once the last callback finishes, it will notify this thread and
     *   we fall out of the loop below and actually process the VM_DEATH
     *   event.
     */
    debugMonitorEnter(callbackBlock); {
        debugMonitorEnter(callbackLock); {
            vm_death_callback_active = JNI_TRUE;
            (void)threadControl_resumeAll();
            while (active_callbacks > 0) {
                /* wait for active CALLBACKs to check in (and block) */
                debugMonitorWait(callbackLock);
            }
        } debugMonitorExit(callbackLock);

        /* Only now should we actually process the VM death event */
        (void)memset(&info,0,sizeof(info));
        info.ei                 = EI_VM_DEATH;
        event_callback(env, &info);

        /* Here we unblock all the callbacks and let them return to the
         *   VM.  It's not clear this is necessary, but leaving threads
         *   blocked doesn't seem like a good idea. They don't have much
         *   life left anyway.
         */
    } debugMonitorExit(callbackBlock);

    /*
     * The VM will die soon after the completion of this callback -
     * we synchronize with both the command loop and the debug loop
     * for a more orderly shutdown.
     */
    commandLoop_sync();
    debugLoop_sync();

    LOG_MISC(("END cbVMDeath"));
}

/**
 * Delete this handler (do not delete permanent handlers):
 * Deinsert handler from active list,
 * make it inactive, and free it's memory
 * Assumes handlerLock held.
 */
static jvmtiError
freeHandler(HandlerNode *node) {
    jvmtiError error = JVMTI_ERROR_NONE;

    /* deinsert the handler node before disableEvents() to make
     * sure the event will be disabled when no other event
     * handlers are installed.
     */
    if (node != NULL && (!node->permanent)) {
        deinsert(node);
        error = eventFilterRestricted_deinstall(node);
        jvmtiDeallocate(node);
    }

    return error;
}

/**
 * Delete all the handlers on this chain (do not delete permanent handlers).
 * Assumes handlerLock held.
 */
static jvmtiError
freeHandlerChain(HandlerChain *chain)
{
    HandlerNode *node;
    jvmtiError   error;

    error = JVMTI_ERROR_NONE;
    node  = chain->first;
    while ( node != NULL ) {
        HandlerNode *next;
        jvmtiError   singleError;

        next = NEXT(node);
        singleError = freeHandler(node);
        if ( singleError != JVMTI_ERROR_NONE ) {
            error = singleError;
        }
        node = next;
    }
    return error;
}

/**
 * Deinsert and free all memory.  Safe for non-inserted nodes.
 */
jvmtiError
eventHandler_free(HandlerNode *node)
{
    jvmtiError error;

    debugMonitorEnter(handlerLock);

    error = freeHandler(node);

    debugMonitorExit(handlerLock);

    return error;
}

/**
 * Free all handlers of this kind created by the JDWP client,
 * that is, doesn't free handlers internally created by back-end.
 */
jvmtiError
eventHandler_freeAll(EventIndex ei)
{
    jvmtiError error = JVMTI_ERROR_NONE;
    HandlerNode *node;

    debugMonitorEnter(handlerLock);
    node = getHandlerChain(ei)->first;
    while (node != NULL) {
        HandlerNode *next = NEXT(node);    /* allows node removal */
        if (node->handlerID != 0) {        /* don't free internal handlers */
            error = freeHandler(node);
            if (error != JVMTI_ERROR_NONE) {
                break;
            }
        }
        node = next;
    }
    debugMonitorExit(handlerLock);
    return error;
}

/***
 * Delete all breakpoints on "clazz".
 */
void
eventHandler_freeClassBreakpoints(jclass clazz)
{
    HandlerNode *node;
    JNIEnv *env = getEnv();

    debugMonitorEnter(handlerLock);
    node = getHandlerChain(EI_BREAKPOINT)->first;
    while (node != NULL) {
        HandlerNode *next = NEXT(node); /* allows node removal */
        if (eventFilterRestricted_isBreakpointInClass(env, clazz,
                                                      node)) {
            (void)freeHandler(node);
        }
        node = next;
    }
    debugMonitorExit(handlerLock);
}

jvmtiError
eventHandler_freeByID(EventIndex ei, HandlerID handlerID)
{
    jvmtiError error;
    HandlerNode *node;

    debugMonitorEnter(handlerLock);
    node = find(ei, handlerID);
    if (node != NULL) {
        error = freeHandler(node);
    } else {
        /* already freed */
        error = JVMTI_ERROR_NONE;
    }
    debugMonitorExit(handlerLock);
    return error;
}

void
eventHandler_initialize(jbyte sessionID)
{
    jvmtiError error;
    jint i;

    requestIdCounter = 1;
    currentSessionID = sessionID;

    /* This is for BEGIN_CALLBACK/END_CALLBACK handling, make sure this
     *   is done while none of these callbacks are active.
     */
    active_callbacks = 0;
    vm_death_callback_active = JNI_FALSE;
    callbackLock = debugMonitorCreate("JDWP Callback Lock");
    callbackBlock = debugMonitorCreate("JDWP Callback Block");

    handlerLock = debugMonitorCreate("JDWP Event Handler Lock");

    for (i = EI_min; i <= EI_max; ++i) {
        getHandlerChain(i)->first = NULL;
    }

    /*
     * Permanently enabled some events.
     */
    error = threadControl_setEventMode(JVMTI_ENABLE,
                                      EI_VM_INIT, NULL);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"Can't enable vm init events");
    }
    error = threadControl_setEventMode(JVMTI_ENABLE,
                                      EI_VM_DEATH, NULL);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"Can't enable vm death events");
    }
    error = threadControl_setEventMode(JVMTI_ENABLE,
                                      EI_THREAD_START, NULL);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"Can't enable thread start events");
    }
    error = threadControl_setEventMode(JVMTI_ENABLE,
                                       EI_THREAD_END, NULL);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"Can't enable thread end events");
    }
    error = threadControl_setEventMode(JVMTI_ENABLE,
                                       EI_CLASS_PREPARE, NULL);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"Can't enable class prepare events");
    }
    error = threadControl_setEventMode(JVMTI_ENABLE,
                                       EI_GC_FINISH, NULL);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"Can't enable garbage collection finish events");
    }

    (void)memset(&(gdata->callbacks),0,sizeof(gdata->callbacks));
    /* Event callback for JVMTI_EVENT_SINGLE_STEP */
    gdata->callbacks.SingleStep                 = &cbSingleStep;
    /* Event callback for JVMTI_EVENT_BREAKPOINT */
    gdata->callbacks.Breakpoint                 = &cbBreakpoint;
    /* Event callback for JVMTI_EVENT_FRAME_POP */
    gdata->callbacks.FramePop                   = &cbFramePop;
    /* Event callback for JVMTI_EVENT_EXCEPTION */
    gdata->callbacks.Exception                  = &cbException;
    /* Event callback for JVMTI_EVENT_THREAD_START */
    gdata->callbacks.ThreadStart                = &cbThreadStart;
    /* Event callback for JVMTI_EVENT_THREAD_END */
    gdata->callbacks.ThreadEnd                  = &cbThreadEnd;
    /* Event callback for JVMTI_EVENT_CLASS_PREPARE */
    gdata->callbacks.ClassPrepare               = &cbClassPrepare;
    /* Event callback for JVMTI_EVENT_CLASS_LOAD */
    gdata->callbacks.ClassLoad                  = &cbClassLoad;
    /* Event callback for JVMTI_EVENT_FIELD_ACCESS */
    gdata->callbacks.FieldAccess                = &cbFieldAccess;
    /* Event callback for JVMTI_EVENT_FIELD_MODIFICATION */
    gdata->callbacks.FieldModification          = &cbFieldModification;
    /* Event callback for JVMTI_EVENT_EXCEPTION_CATCH */
    gdata->callbacks.ExceptionCatch             = &cbExceptionCatch;
    /* Event callback for JVMTI_EVENT_METHOD_ENTRY */
    gdata->callbacks.MethodEntry                = &cbMethodEntry;
    /* Event callback for JVMTI_EVENT_METHOD_EXIT */
    gdata->callbacks.MethodExit                 = &cbMethodExit;
    /* Event callback for JVMTI_EVENT_MONITOR_CONTENDED_ENTER */
    gdata->callbacks.MonitorContendedEnter      = &cbMonitorContendedEnter;
    /* Event callback for JVMTI_EVENT_MONITOR_CONTENDED_ENTERED */
    gdata->callbacks.MonitorContendedEntered    = &cbMonitorContendedEntered;
    /* Event callback for JVMTI_EVENT_MONITOR_WAIT */
    gdata->callbacks.MonitorWait                = &cbMonitorWait;
    /* Event callback for JVMTI_EVENT_MONITOR_WAITED */
    gdata->callbacks.MonitorWaited              = &cbMonitorWaited;
    /* Event callback for JVMTI_EVENT_VM_INIT */
    gdata->callbacks.VMInit                     = &cbVMInit;
    /* Event callback for JVMTI_EVENT_VM_DEATH */
    gdata->callbacks.VMDeath                    = &cbVMDeath;
    /* Event callback for JVMTI_EVENT_GARBAGE_COLLECTION_FINISH */
    gdata->callbacks.GarbageCollectionFinish    = &cbGarbageCollectionFinish;

    error = JVMTI_FUNC_PTR(gdata->jvmti,SetEventCallbacks)
                (gdata->jvmti, &(gdata->callbacks), sizeof(gdata->callbacks));
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"Can't set event callbacks");
    }

    /* Notify other modules that the event callbacks are in place */
    threadControl_onHook();

    /* Get the event helper thread initialized */
    eventHelper_initialize(sessionID);
}

void
eventHandler_reset(jbyte sessionID)
{
    int i;

    debugMonitorEnter(handlerLock);

    /* We must do this first so that if any invokes complete,
     * there will be no attempt to send them to the front
     * end. Waiting for threadControl_reset leaves a window where
     * the invoke completions can sneak through.
     */
    threadControl_detachInvokes();

    /* Reset the event helper thread, purging all queued and
     * in-process commands.
     */
    eventHelper_reset(sessionID);

    /* delete all handlers */
    for (i = EI_min; i <= EI_max; i++) {
        (void)freeHandlerChain(getHandlerChain(i));
    }

    requestIdCounter = 1;
    currentSessionID = sessionID;

    debugMonitorExit(handlerLock);
}

void
eventHandler_lock(void)
{
    debugMonitorEnter(handlerLock);
}

void
eventHandler_unlock(void)
{
    debugMonitorExit(handlerLock);
}

/***** handler creation *****/

HandlerNode *
eventHandler_alloc(jint filterCount, EventIndex ei, jbyte suspendPolicy)
{
    HandlerNode *node = eventFilterRestricted_alloc(filterCount);

    if (node != NULL) {
        node->ei = ei;
        node->suspendPolicy = suspendPolicy;
        node->permanent = JNI_FALSE;
    }

    return node;
}


HandlerID
eventHandler_allocHandlerID(void)
{
    jint handlerID;
    debugMonitorEnter(handlerLock);
    handlerID = ++requestIdCounter;
    debugMonitorExit(handlerLock);
    return handlerID;
}


static jvmtiError
installHandler(HandlerNode *node,
              HandlerFunction func,
              jboolean external)
{
    jvmtiError error;

    if ( func == NULL ) {
        return AGENT_ERROR_INVALID_EVENT_TYPE;
    }

    debugMonitorEnter(handlerLock);

    HANDLER_FUNCTION(node) = func;

    node->handlerID = external? ++requestIdCounter : 0;
    error = eventFilterRestricted_install(node);
    if (node->ei == EI_GC_FINISH) {
        classTrack_activate(getEnv());
    }
    if (error == JVMTI_ERROR_NONE) {
        insert(getHandlerChain(node->ei), node);
    }

    debugMonitorExit(handlerLock);

    return error;
}

static HandlerNode *
createInternal(EventIndex ei, HandlerFunction func,
               jthread thread, jclass clazz, jmethodID method,
               jlocation location, jboolean permanent)
{
    jint index = 0;
    jvmtiError error = JVMTI_ERROR_NONE;
    HandlerNode *node;

    /*
     * Start with necessary allocations
     */
    node = eventHandler_alloc(
        ((thread == NULL)? 0 : 1) + ((clazz == NULL)? 0 : 1),
        ei, JDWP_SUSPEND_POLICY(NONE));
    if (node == NULL) {
        return NULL;
    }

    node->permanent = permanent;

    if (thread != NULL) {
        error = eventFilter_setThreadOnlyFilter(node, index++, thread);
    }

    if ((error == JVMTI_ERROR_NONE) && (clazz != NULL)) {
        error = eventFilter_setLocationOnlyFilter(node, index++, clazz,
                                                  method, location);
    }
    /*
     * Create the new handler node
     */
    error = installHandler(node, func, JNI_FALSE);

    if (error != JVMTI_ERROR_NONE) {
        (void)eventHandler_free(node);
        node = NULL;
    }
    return node;
}

HandlerNode *
eventHandler_createPermanentInternal(EventIndex ei, HandlerFunction func)
{
    return createInternal(ei, func, NULL,
                          NULL, NULL, 0, JNI_TRUE);
}

HandlerNode *
eventHandler_createInternalThreadOnly(EventIndex ei,
                                      HandlerFunction func,
                                      jthread thread)
{
    return createInternal(ei, func, thread,
                          NULL, NULL, 0, JNI_FALSE);
}

HandlerNode *
eventHandler_createInternalBreakpoint(HandlerFunction func,
                                      jthread thread,
                                      jclass clazz,
                                      jmethodID method,
                                      jlocation location)
{
    return createInternal(EI_BREAKPOINT, func, thread,
                          clazz, method, location, JNI_FALSE);
}

jvmtiError
eventHandler_installExternal(HandlerNode *node)
{
    return installHandler(node,
                          standardHandlers_defaultHandler(node->ei),
                          JNI_TRUE);
}

/***** debugging *****/

#ifdef DEBUG

void
eventHandler_dumpAllHandlers(jboolean dumpPermanent)
{
    int ei;
    for (ei = EI_min; ei <= EI_max; ++ei) {
        eventHandler_dumpHandlers(ei, dumpPermanent);
    }
}

void
eventHandler_dumpHandlers(EventIndex ei, jboolean dumpPermanent)
{
  HandlerNode *nextNode;
  nextNode = getHandlerChain(ei)->first;
  if (nextNode != NULL) {
      tty_message("\nHandlers for %s(%d)", eventIndex2EventName(ei), ei);
      while (nextNode != NULL) {
          HandlerNode *node = nextNode;
          nextNode = NEXT(node);

          if (node->permanent && !dumpPermanent) {
              continue; // ignore permanent handlers
          }

          tty_message("node(%p) handlerID(%d) suspendPolicy(%d) permanent(%d)",
                      node, node->handlerID, node->suspendPolicy, node->permanent);
          eventFilter_dumpHandlerFilters(node);
      }
  }
}

void
eventHandler_dumpHandler(HandlerNode *node)
{
    tty_message("Handler for %s(%d)\n", eventIndex2EventName(node->ei), node->ei);
    eventFilter_dumpHandlerFilters(node);
}

#endif /* DEBUG */
