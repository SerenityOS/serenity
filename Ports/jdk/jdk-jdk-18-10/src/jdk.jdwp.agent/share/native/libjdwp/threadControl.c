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
#include "eventHandler.h"
#include "threadControl.h"
#include "commonRef.h"
#include "eventHelper.h"
#include "stepControl.h"
#include "invoker.h"
#include "bag.h"

#define HANDLING_EVENT(node) ((node)->current_ei != 0)

/*
 * Collection of info for properly handling co-located events.
 * If the ei field is non-zero, then one of the possible
 * co-located events has been posted and the other fields describe
 * the event's location.
 *
 * See comment above deferEventReport() for an explanation of co-located events.
 */
typedef struct CoLocatedEventInfo_ {
    EventIndex ei;
    jclass    clazz;
    jmethodID method;
    jlocation location;
} CoLocatedEventInfo;

/**
 * The main data structure in threadControl is the ThreadNode.
 * This is a per-thread structure that is allocated on the
 * first event that occurs in a thread. It is freed after the
 * thread's thread end event has completed processing. The
 * structure contains state information on its thread including
 * suspend counts. It also acts as a repository for other
 * per-thread state such as the current method invocation or
 * current step.
 *
 * suspendCount is the number of outstanding suspends
 * from the debugger. suspends from the app itself are
 * not included in this count.
 */
typedef struct ThreadNode {
    jthread thread;
    unsigned int toBeResumed : 1;      /* true if this thread was successfully suspended. */
    unsigned int pendingInterrupt : 1; /* true if thread is interrupted while handling an event. */
    unsigned int isDebugThread : 1;    /* true if this is one of our debug agent threads. */
    unsigned int suspendOnStart : 1;   /* true for new threads if we are currently in a VM.suspend(). */
    unsigned int isStarted : 1;        /* THREAD_START or VIRTUAL_THREAD_SCHEDULED event received. */
    unsigned int popFrameEvent : 1;
    unsigned int popFrameProceed : 1;
    unsigned int popFrameThread : 1;
    EventIndex current_ei; /* Used to determine if we are currently handling an event on this thread. */
    jobject pendingStop;   /* Object we are throwing to stop the thread (ThreadReferenceImpl.stop). */
    jint suspendCount;
    jint resumeFrameDepth; /* !=0 => This thread is in a call to Thread.resume() */
    jvmtiEventMode instructionStepMode;
    StepRequest currentStep;
    InvokeRequest currentInvoke;
    struct bag *eventBag;       /* Accumulation of JDWP events to be sent as a reply. */
    CoLocatedEventInfo cleInfo; /* See comment above deferEventReport() for an explanation. */
    struct ThreadNode *next;
    struct ThreadNode *prev;
    jlong frameGeneration;    /* used to generate a unique frameID. Incremented whenever existing frameID
                                 needs to be invalidated, such as when the thread is resumed. */
    struct ThreadList *list;  /* Tells us what list this thread is in. */
#ifdef DEBUG_THREADNAME
    char name[256];
#endif
} ThreadNode;

static jint suspendAllCount;

typedef struct ThreadList {
    ThreadNode *first;
} ThreadList;

/*
 * popFrameEventLock is used to notify that the event has been received
 */
static jrawMonitorID popFrameEventLock = NULL;

/*
 * popFrameProceedLock is used to assure that the event thread is
 * re-suspended immediately after the event is acknowledged.
 */
static jrawMonitorID popFrameProceedLock = NULL;

static jrawMonitorID threadLock;
static jlocation resumeLocation;
static HandlerNode *breakpointHandlerNode;
static HandlerNode *framePopHandlerNode;
static HandlerNode *catchHandlerNode;

static jvmtiError threadControl_removeDebugThread(jthread thread);

/*
 * Threads which have issued thread start events and not yet issued thread
 * end events are maintained in the "runningThreads" list. All other threads known
 * to this module are kept in the "otherThreads" list.
 */
static ThreadList runningThreads;
static ThreadList otherThreads;

#define MAX_DEBUG_THREADS 10
static int debugThreadCount;
static jthread debugThreads[MAX_DEBUG_THREADS];

typedef struct DeferredEventMode {
    EventIndex ei;
    jvmtiEventMode mode;
    jthread thread;
    struct DeferredEventMode *next;
} DeferredEventMode;

typedef struct {
    DeferredEventMode *first;
    DeferredEventMode *last;
} DeferredEventModeList;

static DeferredEventModeList deferredEventModes;

#ifdef DEBUG
static void dumpThreadList(ThreadList *list);
static void dumpThread(ThreadNode *node);
#endif

static jint
getStackDepth(jthread thread)
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

/* Get the state of the thread direct from JVMTI */
static jvmtiError
threadState(jthread thread, jint *pstate)
{
    *pstate = 0;
    return JVMTI_FUNC_PTR(gdata->jvmti,GetThreadState)
                        (gdata->jvmti, thread, pstate);
}

/* Set TLS on a specific jthread to the ThreadNode* */
static void
setThreadLocalStorage(jthread thread, ThreadNode *node)
{
    jvmtiError  error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,SetThreadLocalStorage)
            (gdata->jvmti, thread, (void*)node);
    if ( error == JVMTI_ERROR_THREAD_NOT_ALIVE && node == NULL) {
        /* Just return. This can happen when clearing the TLS. */
        return;
    } else if ( error != JVMTI_ERROR_NONE ) {
        /* The jthread object must be valid, so this must be a fatal error */
        EXIT_ERROR(error, "cannot set thread local storage");
    }
}

/* Get TLS on a specific jthread, which is the ThreadNode* */
static ThreadNode *
getThreadLocalStorage(jthread thread)
{
    jvmtiError  error;
    ThreadNode *node;

    node = NULL;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetThreadLocalStorage)
            (gdata->jvmti, thread, (void**)&node);
    if ( error == JVMTI_ERROR_THREAD_NOT_ALIVE ) {
        /* Just return NULL, thread hasn't started yet */
        return NULL;
    } else if ( error != JVMTI_ERROR_NONE ) {
        /* The jthread object must be valid, so this must be a fatal error */
        EXIT_ERROR(error, "cannot get thread local storage");
    }
    return node;
}

/* Search list for nodes that don't have TLS set and match this thread.
 *   It assumed that this logic is never dealing with terminated threads,
 *   since the ThreadEnd events always delete the ThreadNode while the
 *   jthread is still alive.  So we can only look at the ThreadNode's that
 *   have never had their TLS set, making the search much faster.
 *   But keep in mind, this kind of search should rarely be needed.
 */
static ThreadNode *
nonTlsSearch(JNIEnv *env, ThreadList *list, jthread thread)
{
    ThreadNode *node;

    for (node = list->first; node != NULL; node = node->next) {
        if (isSameObject(env, node->thread, thread)) {
            break;
        }
    }
    return node;
}

/*
 * These functions maintain the linked list of currently running threads.
 * All assume that the threadLock is held before calling.
 */

/*
 * Search for a thread on the list. If list==NULL, search all lists.
 */
static ThreadNode *
findThread(ThreadList *list, jthread thread)
{
    ThreadNode *node;

    /* Get thread local storage for quick thread -> node access */
    node = getThreadLocalStorage(thread);

    if ( node == NULL ) {
        /*
         * If the thread was not yet started when the ThreadNode was created, then it
         * got added to the otherThreads list and its thread local storage was not set.
         * Search for it in the otherThreads list.
         */
        if ( list == NULL || list == &otherThreads ) {
            node = nonTlsSearch(getEnv(), &otherThreads, thread);
        }
        /*
         * Normally we can assume that a thread with no TLS will never be in the runningThreads
         * list. This is because we always set the TLS when adding to runningThreads.
         * However, when a thread exits, its TLS is automatically cleared. Normally this
         * is not a problem because the debug agent will first get a THREAD_END event,
         * and that will cause the thread to be removed from runningThreads, thus we
         * avoid this situation of having a thread in runningThreads, but with no TLS.
         *
         * However... there is one exception to this. While handling VM_DEATH, the first thing
         * the debug agent does is clear all the callbacks. This means we will no longer
         * get THREAD_END events as threads exit. This means we might find threads on
         * runningThreads with no TLS during VM_DEATH. Essentially the THREAD_END that
         * would normally have resulted in removing the thread from runningThreads is
         * missed, so the thread remains on runningThreads.
         *
         * The end result of all this is that if the TLS lookup failed, we still need to check
         * if the thread is on runningThreads, but only if JVMTI callbacks have been cleared.
         * Otherwise the thread should not be on the runningThreads.
         */
        if ( !gdata->jvmtiCallBacksCleared ) {
            /* The thread better not be on runningThreads if the TLS lookup failed. */
            JDI_ASSERT(!nonTlsSearch(getEnv(), &runningThreads, thread));
        } else {
            /*
             * Search the runningThreads list. The TLS lookup may have failed because the
             * thread has terminated, but we never got the THREAD_END event.
             */
            if ( node == NULL ) {
                if ( list == NULL || list == &runningThreads ) {
                    node = nonTlsSearch(getEnv(), &runningThreads, thread);
                }
            }
        }
    }

    /* If a list is supplied, only return ones in this list */
    if ( node != NULL && list != NULL && node->list != list ) {
        return NULL;
    }
    return node;
}

/* Remove a ThreadNode from a ThreadList */
static void
removeNode(ThreadList *list, ThreadNode *node)
{
    ThreadNode *prev;
    ThreadNode *next;

    prev = node->prev;
    next = node->next;
    if ( prev != NULL ) {
        prev->next = next;
    }
    if ( next != NULL ) {
        next->prev = prev;
    }
    if ( prev == NULL ) {
        list->first = next;
    }
    node->next = NULL;
    node->prev = NULL;
    node->list = NULL;
}

/* Add a ThreadNode to a ThreadList */
static void
addNode(ThreadList *list, ThreadNode *node)
{
    node->next = NULL;
    node->prev = NULL;
    node->list = NULL;
    if ( list->first == NULL ) {
        list->first = node;
    } else {
        list->first->prev = node;
        node->next = list->first;
        list->first = node;
    }
    node->list = list;
}

static ThreadNode *
insertThread(JNIEnv *env, ThreadList *list, jthread thread)
{
    ThreadNode *node;
    struct bag *eventBag;

    node = findThread(list, thread);
    if (node == NULL) {
        node = jvmtiAllocate(sizeof(*node));
        if (node == NULL) {
            EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"thread table entry");
            return NULL;
        }
        (void)memset(node, 0, sizeof(*node));
        eventBag = eventHelper_createEventBag();
        if (eventBag == NULL) {
            jvmtiDeallocate(node);
            EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"thread table entry");
            return NULL;
        }

        /*
         * Init all flags false, all refs NULL, all counts 0
         */

        saveGlobalRef(env, thread, &(node->thread));
        if (node->thread == NULL) {
            jvmtiDeallocate(node);
            bagDestroyBag(eventBag);
            EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"thread table entry");
            return NULL;
        }
        /*
         * Remember if it is a debug thread
         */
        if (threadControl_isDebugThread(node->thread)) {
            node->isDebugThread = JNI_TRUE;
        } else if (suspendAllCount > 0){
            /*
             * If there is a pending suspendAll, all new threads should
             * be initialized as if they were suspended by the suspendAll,
             * and the thread will need to be suspended when it starts.
             */
            node->suspendCount = suspendAllCount;
            node->suspendOnStart = JNI_TRUE;
        }
        node->current_ei = 0;
        node->instructionStepMode = JVMTI_DISABLE;
        node->eventBag = eventBag;
        addNode(list, node);

#ifdef DEBUG_THREADNAME
        {
            /* Set the thread name */
            jvmtiThreadInfo info;
            jvmtiError error;

            memset(&info, 0, sizeof(info));
            error = JVMTI_FUNC_PTR(gdata->jvmti,GetThreadInfo)
                    (gdata->jvmti, node->thread, &info);
            if (info.name != NULL) {
                strncpy(node->name, info.name, sizeof(node->name) - 1);
                jvmtiDeallocate(info.name);
            }
        }
#endif

        /* Set thread local storage for quick thread -> node access.
         *   Threads that are not yet started do not allow setting of TLS. These
         *   threads go on the otherThreads list and have their TLS set
         *   when moved to the runningThreads list. findThread() knows to look
         *   on otherThreads when the TLS lookup fails.
         */
        if (list != &otherThreads) {
            setThreadLocalStorage(node->thread, (void*)node);
        }
    }

    return node;
}

static void
clearThread(JNIEnv *env, ThreadNode *node)
{
    if (node->pendingStop != NULL) {
        tossGlobalRef(env, &(node->pendingStop));
    }
    stepControl_clearRequest(node->thread, &node->currentStep);
    if (node->isDebugThread) {
        (void)threadControl_removeDebugThread(node->thread);
    }
    /* Clear out TLS on this thread (just a cleanup action) */
    setThreadLocalStorage(node->thread, NULL);
    tossGlobalRef(env, &(node->thread));
    bagDestroyBag(node->eventBag);
    jvmtiDeallocate(node);
}

static void
removeThread(JNIEnv *env, ThreadList *list, jthread thread)
{
    ThreadNode *node;

    node = findThread(list, thread);
    if (node != NULL) {
        removeNode(list, node);
        clearThread(env, node);
    }
}

static void
removeResumed(JNIEnv *env, ThreadList *list)
{
    ThreadNode *node;

    node = list->first;
    while (node != NULL) {
        ThreadNode *temp = node->next;
        if (node->suspendCount == 0) {
            removeThread(env, list, node->thread);
        }
        node = temp;
    }
}

static void
moveNode(ThreadList *source, ThreadList *dest, ThreadNode *node)
{
    removeNode(source, node);
    JDI_ASSERT(findThread(dest, node->thread) == NULL);
    addNode(dest, node);
}

typedef jvmtiError (*ThreadEnumerateFunction)(JNIEnv *, ThreadNode *, void *);

static jvmtiError
enumerateOverThreadList(JNIEnv *env, ThreadList *list,
                        ThreadEnumerateFunction function, void *arg)
{
    ThreadNode *node;
    jvmtiError error = JVMTI_ERROR_NONE;

    for (node = list->first; node != NULL; node = node->next) {
        error = (*function)(env, node, arg);
        if ( error != JVMTI_ERROR_NONE ) {
            break;
        }
    }
    return error;
}

static void
insertEventMode(DeferredEventModeList *list, DeferredEventMode *eventMode)
{
    if (list->last != NULL) {
        list->last->next = eventMode;
    } else {
        list->first = eventMode;
    }
    list->last = eventMode;
}

static void
removeEventMode(DeferredEventModeList *list, DeferredEventMode *eventMode, DeferredEventMode *prev)
{
    if (prev == NULL) {
        list->first = eventMode->next;
    } else {
        prev->next = eventMode->next;
    }
    if (eventMode->next == NULL) {
        list->last = prev;
    }
}

static jvmtiError
addDeferredEventMode(JNIEnv *env, jvmtiEventMode mode, EventIndex ei, jthread thread)
{
    DeferredEventMode *eventMode;

    /*LINTED*/
    eventMode = jvmtiAllocate((jint)sizeof(DeferredEventMode));
    if (eventMode == NULL) {
        return AGENT_ERROR_OUT_OF_MEMORY;
    }
    eventMode->thread = NULL;
    saveGlobalRef(env, thread, &(eventMode->thread));
    eventMode->mode = mode;
    eventMode->ei = ei;
    eventMode->next = NULL;
    insertEventMode(&deferredEventModes, eventMode);
    return JVMTI_ERROR_NONE;
}

static void
freeDeferredEventModes(JNIEnv *env)
{
    DeferredEventMode *eventMode;
    eventMode = deferredEventModes.first;
    while (eventMode != NULL) {
        DeferredEventMode *next;
        next = eventMode->next;
        tossGlobalRef(env, &(eventMode->thread));
        jvmtiDeallocate(eventMode);
        eventMode = next;
    }
    deferredEventModes.first = NULL;
    deferredEventModes.last = NULL;
}

static jvmtiError
threadSetEventNotificationMode(ThreadNode *node,
        jvmtiEventMode mode, EventIndex ei, jthread thread)
{
    jvmtiError error;

    /* record single step mode */
    if (ei == EI_SINGLE_STEP) {
        node->instructionStepMode = mode;
    }
    error = JVMTI_FUNC_PTR(gdata->jvmti,SetEventNotificationMode)
        (gdata->jvmti, mode, eventIndex2jvmti(ei), thread);
    return error;
}

static void
processDeferredEventModes(JNIEnv *env, jthread thread, ThreadNode *node)
{
    jvmtiError error;
    DeferredEventMode *eventMode;
    DeferredEventMode *prev;

    prev = NULL;
    eventMode = deferredEventModes.first;
    while (eventMode != NULL) {
        DeferredEventMode *next = eventMode->next;
        if (isSameObject(env, thread, eventMode->thread)) {
            error = threadSetEventNotificationMode(node,
                    eventMode->mode, eventMode->ei, eventMode->thread);
            if (error != JVMTI_ERROR_NONE) {
                EXIT_ERROR(error, "cannot process deferred thread event notifications at thread start");
            }
            removeEventMode(&deferredEventModes, eventMode, prev);
            tossGlobalRef(env, &(eventMode->thread));
            jvmtiDeallocate(eventMode);
        } else {
            prev = eventMode;
        }
        eventMode = next;
    }
}

static void
getLocks(void)
{
    /*
     * Anything which might be locked as part of the handling of
     * a JVMTI event (which means: might be locked by an application
     * thread) needs to be grabbed here. This allows thread control
     * code to safely suspend and resume the application threads
     * while ensuring they don't hold a critical lock.
     */

    eventHandler_lock();
    invoker_lock();
    eventHelper_lock();
    stepControl_lock();
    commonRef_lock();
    debugMonitorEnter(threadLock);

}

static void
releaseLocks(void)
{
    debugMonitorExit(threadLock);
    commonRef_unlock();
    stepControl_unlock();
    eventHelper_unlock();
    invoker_unlock();
    eventHandler_unlock();
}

void
threadControl_initialize(void)
{
    jlocation unused;
    jvmtiError error;

    suspendAllCount = 0;
    runningThreads.first = NULL;
    otherThreads.first = NULL;
    debugThreadCount = 0;
    threadLock = debugMonitorCreate("JDWP Thread Lock");
    if (gdata->threadClass==NULL) {
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER, "no java.lang.thread class");
    }
    if (gdata->threadResume==0) {
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER, "cannot resume thread");
    }
    /* Get the java.lang.Thread.resume() method beginning location */
    error = methodLocation(gdata->threadResume, &resumeLocation, &unused);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "getting method location");
    }
}

static jthread
getResumee(jthread resumingThread)
{
    jthread resumee = NULL;
    jvmtiError error;
    jobject object;
    FrameNumber fnum = 0;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalObject)
                    (gdata->jvmti, resumingThread, fnum, 0, &object);
    if (error == JVMTI_ERROR_NONE) {
        resumee = object;
    }
    return resumee;
}


static jboolean
pendingAppResume(jboolean includeSuspended)
{
    ThreadList *list;
    ThreadNode *node;

    list = &runningThreads;
    node = list->first;
    while (node != NULL) {
        if (node->resumeFrameDepth > 0) {
            if (includeSuspended) {
                return JNI_TRUE;
            } else {
                jvmtiError error;
                jint       state;

                error = threadState(node->thread, &state);
                if (error != JVMTI_ERROR_NONE) {
                    EXIT_ERROR(error, "getting thread state");
                }
                if (!(state & JVMTI_THREAD_STATE_SUSPENDED)) {
                    return JNI_TRUE;
                }
            }
        }
        node = node->next;
    }
    return JNI_FALSE;
}

static void
notifyAppResumeComplete(void)
{
    debugMonitorNotifyAll(threadLock);
    if (!pendingAppResume(JNI_TRUE)) {
        if (framePopHandlerNode != NULL) {
            (void)eventHandler_free(framePopHandlerNode);
            framePopHandlerNode = NULL;
        }
        if (catchHandlerNode != NULL) {
            (void)eventHandler_free(catchHandlerNode);
            catchHandlerNode = NULL;
        }
    }
}

/*
 * Event handler for FRAME_POP and EXCEPTION_CATCH when in Thread.resume()
 * so we can detect its completion.
 */
static void
handleAppResumeCompletion(JNIEnv *env, EventInfo *evinfo,
                          HandlerNode *handlerNode,
                          struct bag *eventBag)
{
    ThreadNode *node;
    jthread     thread;

    thread = evinfo->thread;

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if (node != NULL) {
        if (node->resumeFrameDepth > 0) {
            jint compareDepth = getStackDepth(thread);
            if (evinfo->ei == EI_FRAME_POP) {
                compareDepth--;
            }
            if (compareDepth < node->resumeFrameDepth) {
                node->resumeFrameDepth = 0;
                notifyAppResumeComplete();
            }
        }
    }

    debugMonitorExit(threadLock);
}

static void
blockOnDebuggerSuspend(jthread thread)
{
    ThreadNode *node;

    node = findThread(NULL, thread);
    if (node != NULL) {
        while (node && node->suspendCount > 0) {
            debugMonitorWait(threadLock);
            node = findThread(NULL, thread);
        }
    }
}

static void
trackAppResume(jthread thread)
{
    jvmtiError  error;
    FrameNumber fnum;
    ThreadNode *node;

    fnum = 0;
    node = findThread(&runningThreads, thread);
    if (node != NULL) {
        JDI_ASSERT(node->resumeFrameDepth == 0);
        error = JVMTI_FUNC_PTR(gdata->jvmti,NotifyFramePop)
                        (gdata->jvmti, thread, fnum);
        if (error == JVMTI_ERROR_NONE) {
            jint frameDepth = getStackDepth(thread);
            if ((frameDepth > 0) && (framePopHandlerNode == NULL)) {
                framePopHandlerNode = eventHandler_createInternalThreadOnly(
                                           EI_FRAME_POP,
                                           handleAppResumeCompletion,
                                           thread);
                catchHandlerNode = eventHandler_createInternalThreadOnly(
                                           EI_EXCEPTION_CATCH,
                                           handleAppResumeCompletion,
                                           thread);
                if ((framePopHandlerNode == NULL) ||
                    (catchHandlerNode == NULL)) {
                    (void)eventHandler_free(framePopHandlerNode);
                    framePopHandlerNode = NULL;
                    (void)eventHandler_free(catchHandlerNode);
                    catchHandlerNode = NULL;
                }
            }
            if ((framePopHandlerNode != NULL) &&
                (catchHandlerNode != NULL) &&
                (frameDepth > 0)) {
                node->resumeFrameDepth = frameDepth;
            }
        }
    }
}

/* Global breakpoint handler for Thread.resume() */
static void
handleAppResumeBreakpoint(JNIEnv *env, EventInfo *evinfo,
                          HandlerNode *handlerNode,
                          struct bag *eventBag)
{
    jthread resumer = evinfo->thread;
    jthread resumee = getResumee(resumer);

    debugMonitorEnter(threadLock);
    if (resumee != NULL) {
        /*
         * Hold up any attempt to resume as long as the debugger
         * has suspended the resumee.
         */
        blockOnDebuggerSuspend(resumee);
    }

    if (resumer != NULL) {
        /*
         * Track the resuming thread by marking it as being within
         * a resume and by setting up for notification on
         * a frame pop or exception. We won't allow the debugger
         * to suspend threads while any thread is within a
         * call to resume. This (along with the block above)
         * ensures that when the debugger
         * suspends a thread it will remain suspended.
         */
        trackAppResume(resumer);
    }

    debugMonitorExit(threadLock);
}

void
threadControl_onConnect(void)
{
    breakpointHandlerNode = eventHandler_createInternalBreakpoint(
                 handleAppResumeBreakpoint, NULL,
                 gdata->threadClass, gdata->threadResume, resumeLocation);
}

void
threadControl_onDisconnect(void)
{
    if (breakpointHandlerNode != NULL) {
        (void)eventHandler_free(breakpointHandlerNode);
        breakpointHandlerNode = NULL;
    }
    if (framePopHandlerNode != NULL) {
        (void)eventHandler_free(framePopHandlerNode);
        framePopHandlerNode = NULL;
    }
    if (catchHandlerNode != NULL) {
        (void)eventHandler_free(catchHandlerNode);
        catchHandlerNode = NULL;
    }
}

void
threadControl_onHook(void)
{
    /*
     * As soon as the event hook is in place, we need to initialize
     * the thread list with already-existing threads. The threadLock
     * has been held since initialize, so we don't need to worry about
     * insertions or deletions from the event handlers while we do this
     */
    JNIEnv *env;

    env = getEnv();

    /*
     * Prevent any event processing until OnHook has been called
     */
    debugMonitorEnter(threadLock);

    WITH_LOCAL_REFS(env, 1) {

        jint threadCount;
        jthread *threads;

        threads = allThreads(&threadCount);
        if (threads == NULL) {
            EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"thread table");
        } else {

            int i;

            for (i = 0; i < threadCount; i++) {
                ThreadNode *node;
                jthread thread = threads[i];
                node = insertThread(env, &runningThreads, thread);

                /*
                 * This is a tiny bit risky. We have to assume that the
                 * pre-existing threads have been started because we
                 * can't rely on a thread start event for them. The chances
                 * of a problem related to this are pretty slim though, and
                 * there's really no choice because without setting this flag
                 * there is no way to enable stepping and other events on
                 * the threads that already exist (e.g. the finalizer thread).
                 */
                node->isStarted = JNI_TRUE;
            }
            jvmtiDeallocate(threads);
        }

    } END_WITH_LOCAL_REFS(env)

    debugMonitorExit(threadLock);
}

static jvmtiError
commonSuspendByNode(ThreadNode *node)
{
    jvmtiError error;

    LOG_MISC(("thread=%p suspended", node->thread));
    error = JVMTI_FUNC_PTR(gdata->jvmti,SuspendThread)
                (gdata->jvmti, node->thread);

    /*
     * Mark for resume only if suspend succeeded
     */
    if (error == JVMTI_ERROR_NONE) {
        node->toBeResumed = JNI_TRUE;
    }

    /*
     * If the thread was suspended by another app thread,
     * do nothing and report no error (we won't resume it later).
     */
     if (error == JVMTI_ERROR_THREAD_SUSPENDED) {
        error = JVMTI_ERROR_NONE;
     }

     return error;
}

/*
 * Deferred suspends happen when the suspend is attempted on a thread
 * that is not started. Bookkeeping (suspendCount,etc.)
 * is handled by the original request, and once the thread actually
 * starts, an actual suspend is attempted. This function does the
 * deferred suspend without changing the bookkeeping that is already
 * in place.
 */
static jint
deferredSuspendThreadByNode(ThreadNode *node)
{
    jvmtiError error;

    error = JVMTI_ERROR_NONE;
    if (node->isDebugThread) {
        /* Ignore requests for suspending debugger threads */
        return JVMTI_ERROR_NONE;
    }

    /*
     * Do the actual suspend only if a subsequent resume hasn't
     * made it irrelevant.
     */
    if (node->suspendCount > 0) {
        error = commonSuspendByNode(node);

        /*
         * Attempt to clean up from any error by decrementing the
         * suspend count. This compensates for the increment that
         * happens when suspendOnStart is set to true.
         */
        if (error != JVMTI_ERROR_NONE) {
            node->suspendCount--;
        }
    }

    node->suspendOnStart = JNI_FALSE;

    debugMonitorNotifyAll(threadLock);

    return error;
}

static jvmtiError
suspendThreadByNode(ThreadNode *node)
{
    jvmtiError error = JVMTI_ERROR_NONE;
    if (node->isDebugThread) {
        /* Ignore requests for suspending debugger threads */
        return JVMTI_ERROR_NONE;
    }

    /*
     * Just increment the suspend count if we are waiting
     * for a deferred suspend.
     */
    if (node->suspendOnStart) {
        node->suspendCount++;
        return JVMTI_ERROR_NONE;
    }

    if (node->suspendCount == 0) {
        error = commonSuspendByNode(node);

        if (error == JVMTI_ERROR_THREAD_NOT_ALIVE) {
            /*
             * This error means that the thread is either a zombie or not yet
             * started. In either case, we ignore the error. If the thread
             * is a zombie, suspend/resume are no-ops. If the thread is not
             * started, it will be suspended for real during the processing
             * of its thread start event.
             */
            node->suspendOnStart = JNI_TRUE;
            error = JVMTI_ERROR_NONE;
        }
    }

    if (error == JVMTI_ERROR_NONE) {
        node->suspendCount++;
    }

    debugMonitorNotifyAll(threadLock);

    return error;
}

static jvmtiError
resumeThreadByNode(ThreadNode *node)
{
    jvmtiError error = JVMTI_ERROR_NONE;

    if (node->isDebugThread) {
        /* never suspended by debugger => don't ever try to resume */
        return JVMTI_ERROR_NONE;
    }
    if (node->suspendCount > 0) {
        node->suspendCount--;
        debugMonitorNotifyAll(threadLock);
        if ((node->suspendCount == 0) && node->toBeResumed &&
            !node->suspendOnStart) {
            LOG_MISC(("thread=%p resumed", node->thread));
            error = JVMTI_FUNC_PTR(gdata->jvmti,ResumeThread)
                        (gdata->jvmti, node->thread);
            node->frameGeneration++; /* Increment on each resume */
            node->toBeResumed = JNI_FALSE;
            if (error == JVMTI_ERROR_THREAD_NOT_ALIVE && !node->isStarted) {
                /*
                 * We successfully "suspended" this thread, but
                 * we never received a THREAD_START event for it.
                 * Since the thread never ran, we can ignore our
                 * failure to resume the thread.
                 */
                error = JVMTI_ERROR_NONE;
            }
        }
    }

    return error;
}

/*
 * Functions which respond to user requests to suspend/resume
 * threads.
 * Suspends and resumes add and subtract from a count respectively.
 * The thread is only suspended when the count goes from 0 to 1 and
 * resumed only when the count goes from 1 to 0.
 *
 * These functions suspend and resume application threads
 * without changing the
 * state of threads that were already suspended beforehand.
 * They must not be called from an application thread because
 * that thread may be suspended somewhere in the  middle of things.
 */
static void
preSuspend(void)
{
    getLocks();                     /* Avoid debugger deadlocks */

    /*
     * Delay any suspend while a call to java.lang.Thread.resume is in
     * progress (not including those in suspended threads). The wait is
     * timed because the threads suspended through
     * java.lang.Thread.suspend won't result in a notify even though
     * it may change the result of pendingAppResume()
     */
    while (pendingAppResume(JNI_FALSE)) {
        /*
         * This is ugly but we need to release the locks from getLocks
         * or else the notify will never happen. The locks must be
         * released and reacquired in the right order. else deadlocks
         * can happen. It is possible that, during this dance, the
         * notify will be missed, but since the wait needs to be timed
         * anyway, it won't be a disaster. Note that this code will
         * execute only on very rare occasions anyway.
         */
        releaseLocks();

        debugMonitorEnter(threadLock);
        debugMonitorTimedWait(threadLock, 1000);
        debugMonitorExit(threadLock);

        getLocks();
    }
}

static void
postSuspend(void)
{
    releaseLocks();
}

/*
 * This function must be called after preSuspend and before postSuspend.
 */
static jvmtiError
commonSuspend(JNIEnv *env, jthread thread, jboolean deferred)
{
    ThreadNode *node;

    /*
     * If the thread is not between its start and end events, we should
     * still suspend it. To keep track of things, add the thread
     * to a separate list of threads so that we'll resume it later.
     */
    node = findThread(&runningThreads, thread);
#if 0
    tty_message("commonSuspend: node(%p) suspendCount(%d) %s", node, node->suspendCount, node->name);
#endif
    if (node == NULL) {
        node = insertThread(env, &otherThreads, thread);
    }

    if ( deferred ) {
        return deferredSuspendThreadByNode(node);
    } else {
        return suspendThreadByNode(node);
    }
}


static jvmtiError
resumeCopyHelper(JNIEnv *env, ThreadNode *node, void *arg)
{
    if (node->isDebugThread) {
        /* never suspended by debugger => don't ever try to resume */
        return JVMTI_ERROR_NONE;
    }

    if (node->suspendCount > 1) {
        node->suspendCount--;
        /* nested suspend so just undo one level */
        return JVMTI_ERROR_NONE;
    }

    /*
     * This thread was marked for suspension since its THREAD_START
     * event came in during a suspendAll, but the helper hasn't
     * completed the job yet. We decrement the count so the helper
     * won't suspend this thread after we are done with the resumeAll.
     * Another case to be handled here is when the debugger suspends
     * the thread while the app has it suspended. In this case,
     * the toBeResumed flag has been cleared indicating that
     * the thread should not be resumed when the debugger does a resume.
     * In this case, we also have to decrement the suspend count.
     * If we don't then when the app resumes the thread and our Thread.resume
     * bkpt handler is called, blockOnDebuggerSuspend will not resume
     * the thread because suspendCount will be 1 meaning that the
     * debugger has the thread suspended.  See bug 6224859.
     */
    if (node->suspendCount == 1 && (!node->toBeResumed || node->suspendOnStart)) {
        node->suspendCount--;
        return JVMTI_ERROR_NONE;
    }

    if (arg == NULL) {
        /* nothing to hard resume so we're done */
        return JVMTI_ERROR_NONE;
    }

    /*
     * This is tricky. A suspendCount of 1 and toBeResumed means that
     * JVM/DI SuspendThread() or JVM/DI SuspendThreadList() was called
     * on this thread. The check for !suspendOnStart is paranoia that
     * we inherited from resumeThreadByNode().
     */
    if (node->suspendCount == 1 && node->toBeResumed && !node->suspendOnStart) {
        jthread **listPtr = (jthread **)arg;

        **listPtr = node->thread;
        (*listPtr)++;
    }
    return JVMTI_ERROR_NONE;
}


static jvmtiError
resumeCountHelper(JNIEnv *env, ThreadNode *node, void *arg)
{
    if (node->isDebugThread) {
        /* never suspended by debugger => don't ever try to resume */
        return JVMTI_ERROR_NONE;
    }

    /*
     * This is tricky. A suspendCount of 1 and toBeResumed means that
     * JVM/DI SuspendThread() or JVM/DI SuspendThreadList() was called
     * on this thread. The check for !suspendOnStart is paranoia that
     * we inherited from resumeThreadByNode().
     */
    if (node->suspendCount == 1 && node->toBeResumed && !node->suspendOnStart) {
        jint *counter = (jint *)arg;

        (*counter)++;
    }
    return JVMTI_ERROR_NONE;
}

static void *
newArray(jint length, size_t nbytes)
{
    void *ptr;
    ptr = jvmtiAllocate(length*(jint)nbytes);
    if ( ptr != NULL ) {
        (void)memset(ptr, 0, length*nbytes);
    }
    return ptr;
}

static void
deleteArray(void *ptr)
{
    jvmtiDeallocate(ptr);
}

/*
 * This function must be called with the threadLock held.
 *
 * Two facts conspire to make this routine complicated:
 *
 * 1) the VM doesn't support nested external suspend
 * 2) the original resumeAll code structure doesn't retrieve the
 *    entire thread list from JVMTI so we use the runningThreads
 *    list and two helpers to get the job done.
 *
 * Because we hold the threadLock, state seen by resumeCountHelper()
 * is the same state seen in resumeCopyHelper(). resumeCountHelper()
 * just counts up the number of threads to be hard resumed.
 * resumeCopyHelper() does the accounting for nested suspends and
 * special cases and, finally, populates the list of hard resume
 * threads to be passed to ResumeThreadList().
 *
 * At first glance, you might think that the accounting could be done
 * in resumeCountHelper(), but then resumeCopyHelper() would see
 * "post-resume" state in the accounting values (suspendCount and
 * toBeResumed) and would not be able to distinguish between a thread
 * that needs a hard resume versus a thread that is already running.
 */
static jvmtiError
commonResumeList(JNIEnv *env)
{
    jvmtiError   error;
    jint         i;
    jint         reqCnt;
    jthread     *reqList;
    jthread     *reqPtr;
    jvmtiError  *results;

    reqCnt = 0;

    /* count number of threads to hard resume */
    (void) enumerateOverThreadList(env, &runningThreads, resumeCountHelper,
                                   &reqCnt);
    if (reqCnt == 0) {
        /* nothing to hard resume so do just the accounting part */
        (void) enumerateOverThreadList(env, &runningThreads, resumeCopyHelper,
                                       NULL);
        return JVMTI_ERROR_NONE;
    }

    /*LINTED*/
    reqList = newArray(reqCnt, sizeof(jthread));
    if (reqList == NULL) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"resume request list");
    }
    /*LINTED*/
    results = newArray(reqCnt, sizeof(jvmtiError));
    if (results == NULL) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"resume list");
    }

    /* copy the jthread values for threads to hard resume */
    reqPtr = reqList;
    (void) enumerateOverThreadList(env, &runningThreads, resumeCopyHelper,
                                   &reqPtr);

    error = JVMTI_FUNC_PTR(gdata->jvmti,ResumeThreadList)
                (gdata->jvmti, reqCnt, reqList, results);
    for (i = 0; i < reqCnt; i++) {
        ThreadNode *node;

        node = findThread(&runningThreads, reqList[i]);
        if (node == NULL) {
            EXIT_ERROR(AGENT_ERROR_INVALID_THREAD,"missing entry in running thread table");
        }
        LOG_MISC(("thread=%p resumed as part of list", node->thread));

        /*
         * resumeThreadByNode() assumes that JVM/DI ResumeThread()
         * always works and does all the accounting updates. We do
         * the same here. We also don't clear the error.
         */
        node->suspendCount--;
        node->toBeResumed = JNI_FALSE;
        node->frameGeneration++; /* Increment on each resume */
    }
    deleteArray(results);
    deleteArray(reqList);

    debugMonitorNotifyAll(threadLock);

    return error;
}


/*
 * This function must be called after preSuspend and before postSuspend.
 */
static jvmtiError
commonSuspendList(JNIEnv *env, jint initCount, jthread *initList)
{
    jvmtiError  error;
    jint        i;
    jint        reqCnt;
    jthread    *reqList;

    error   = JVMTI_ERROR_NONE;
    reqCnt  = 0;
    reqList = newArray(initCount, sizeof(jthread));
    if (reqList == NULL) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"request list");
    }

    /*
     * Go through the initial list and see if we have anything to suspend.
     */
    for (i = 0; i < initCount; i++) {
        ThreadNode *node;

        /*
         * If the thread is not between its start and end events, we should
         * still suspend it. To keep track of things, add the thread
         * to a separate list of threads so that we'll resume it later.
         */
        node = findThread(&runningThreads, initList[i]);
        if (node == NULL) {
            node = insertThread(env, &otherThreads, initList[i]);
        }

        if (node->isDebugThread) {
            /* Ignore requests for suspending debugger threads */
            continue;
        }

        /*
         * Just increment the suspend count if we are waiting
         * for a deferred suspend or if this is a nested suspend.
         */
        if (node->suspendOnStart || node->suspendCount > 0) {
            node->suspendCount++;
            continue;
        }

        if (node->suspendCount == 0) {
            /* thread is not suspended yet so put it on the request list */
            reqList[reqCnt++] = initList[i];
        }
    }

    if (reqCnt > 0) {
        jvmtiError *results = newArray(reqCnt, sizeof(jvmtiError));

        if (results == NULL) {
            EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"suspend list results");
        }

        /*
         * We have something to suspend so try to do it.
         */
        error = JVMTI_FUNC_PTR(gdata->jvmti,SuspendThreadList)
                        (gdata->jvmti, reqCnt, reqList, results);
        for (i = 0; i < reqCnt; i++) {
            ThreadNode *node;

            node = findThread(NULL, reqList[i]);
            if (node == NULL) {
                EXIT_ERROR(AGENT_ERROR_INVALID_THREAD,"missing entry in thread tables");
            }
            LOG_MISC(("thread=%p suspended as part of list", node->thread));

            if (results[i] == JVMTI_ERROR_NONE) {
                /* thread was suspended as requested */
                node->toBeResumed = JNI_TRUE;
            } else if (results[i] == JVMTI_ERROR_THREAD_SUSPENDED) {
                /*
                 * If the thread was suspended by another app thread,
                 * do nothing and report no error (we won't resume it later).
                 */
                results[i] = JVMTI_ERROR_NONE;
            } else if (results[i] == JVMTI_ERROR_THREAD_NOT_ALIVE) {
                /*
                 * This error means that the suspend request failed
                 * because the thread is either a zombie or not yet
                 * started. In either case, we ignore the error. If the
                 * thread is a zombie, suspend/resume are no-ops. If the
                 * thread is not started, it will be suspended for real
                 * during the processing of its thread start event.
                 */
                node->suspendOnStart = JNI_TRUE;
                results[i] = JVMTI_ERROR_NONE;
            }

            /* count real, app and deferred (suspendOnStart) suspensions */
            if (results[i] == JVMTI_ERROR_NONE) {
                node->suspendCount++;
            }
        }
        deleteArray(results);
    }
    deleteArray(reqList);

    debugMonitorNotifyAll(threadLock);

    return error;
}

static jvmtiError
commonResume(jthread thread)
{
    jvmtiError  error;
    ThreadNode *node;

    /*
     * The thread is normally between its start and end events, but if
     * not, check the auxiliary list used by threadControl_suspendThread.
     */
    node = findThread(NULL, thread);
#if 0
    tty_message("commonResume: node(%p) suspendCount(%d) %s", node, node->suspendCount, node->name);
#endif

    /*
     * If the node is in neither list, the debugger never suspended
     * this thread, so do nothing.
     */
    error = JVMTI_ERROR_NONE;
    if (node != NULL) {
        error = resumeThreadByNode(node);
    }
    return error;
}


jvmtiError
threadControl_suspendThread(jthread thread, jboolean deferred)
{
    jvmtiError error;
    JNIEnv    *env;

    env = getEnv();

    log_debugee_location("threadControl_suspendThread()", thread, NULL, 0);

    preSuspend();
    error = commonSuspend(env, thread, deferred);
    postSuspend();

    return error;
}

jvmtiError
threadControl_resumeThread(jthread thread, jboolean do_unblock)
{
    jvmtiError error;
    JNIEnv    *env;

    env = getEnv();

    log_debugee_location("threadControl_resumeThread()", thread, NULL, 0);

    eventHandler_lock(); /* for proper lock order */
    debugMonitorEnter(threadLock);
    error = commonResume(thread);
    removeResumed(env, &otherThreads);
    debugMonitorExit(threadLock);
    eventHandler_unlock();

    if (do_unblock) {
        /* let eventHelper.c: commandLoop() know we resumed one thread */
        unblockCommandLoop();
    }

    return error;
}

jvmtiError
threadControl_suspendCount(jthread thread, jint *count)
{
    jvmtiError  error;
    ThreadNode *node;

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if (node == NULL) {
        node = findThread(&otherThreads, thread);
    }

    error = JVMTI_ERROR_NONE;
    if (node != NULL) {
        *count = node->suspendCount;
    } else {
        /*
         * If the node is in neither list, the debugger never suspended
         * this thread, so the suspend count is 0.
         */
        *count = 0;
    }

    debugMonitorExit(threadLock);

    return error;
}

static jboolean
contains(JNIEnv *env, jthread *list, jint count, jthread item)
{
    int i;

    for (i = 0; i < count; i++) {
        if (isSameObject(env, list[i], item)) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}


typedef struct {
    jthread *list;
    jint count;
} SuspendAllArg;

static jvmtiError
suspendAllHelper(JNIEnv *env, ThreadNode *node, void *arg)
{
    SuspendAllArg *saArg = (SuspendAllArg *)arg;
    jvmtiError error = JVMTI_ERROR_NONE;
    jthread *list = saArg->list;
    jint count = saArg->count;
    if (!contains(env, list, count, node->thread)) {
        error = commonSuspend(env, node->thread, JNI_FALSE);
    }
    return error;
}

jvmtiError
threadControl_suspendAll(void)
{
    jvmtiError error;
    JNIEnv    *env;
#if 0
    tty_message("threadControl_suspendAll: suspendAllCount(%d)", suspendAllCount);
#endif

    env = getEnv();

    log_debugee_location("threadControl_suspendAll()", NULL, NULL, 0);

    preSuspend();

    /*
     * Get a list of all threads and suspend them.
     */
    WITH_LOCAL_REFS(env, 1) {

        jthread *threads;
        jint count;

        threads = allThreads(&count);
        if (threads == NULL) {
            error = AGENT_ERROR_OUT_OF_MEMORY;
            goto err;
        }
        error = commonSuspendList(env, count, threads);
        if (error != JVMTI_ERROR_NONE) {
            goto err;
        }

        /*
         * Update the suspend count of any threads not yet (or no longer)
         * in the thread list above.
         */
        {
            SuspendAllArg arg;
            arg.list = threads;
            arg.count = count;
            error = enumerateOverThreadList(env, &otherThreads,
                                            suspendAllHelper, &arg);
        }

        if (error == JVMTI_ERROR_NONE) {
            /*
             * Pin all objects to prevent objects from being
             * garbage collected while the VM is suspended.
             */
            commonRef_pinAll();

            suspendAllCount++;
        }

    err:
        jvmtiDeallocate(threads);

    } END_WITH_LOCAL_REFS(env)

    postSuspend();

    return error;
}

static jvmtiError
resumeHelper(JNIEnv *env, ThreadNode *node, void *ignored)
{
    /*
     * Since this helper is called with the threadLock held, we
     * don't need to recheck to see if the node is still on one
     * of the two thread lists.
     */
    return resumeThreadByNode(node);
}

jvmtiError
threadControl_resumeAll(void)
{
    jvmtiError error;
    JNIEnv    *env;
#if 0
    tty_message("threadControl_resumeAll: suspendAllCount(%d)", suspendAllCount);
#endif

    env = getEnv();

    log_debugee_location("threadControl_resumeAll()", NULL, NULL, 0);

    eventHandler_lock(); /* for proper lock order */
    debugMonitorEnter(threadLock);

    /*
     * Resume only those threads that the debugger has suspended. All
     * such threads must have a node in one of the thread lists, so there's
     * no need to get the whole thread list from JVMTI (unlike
     * suspendAll).
     */
    error = commonResumeList(env);
    if ((error == JVMTI_ERROR_NONE) && (otherThreads.first != NULL)) {
        error = enumerateOverThreadList(env, &otherThreads,
                                        resumeHelper, NULL);
        removeResumed(env, &otherThreads);
    }

    if (suspendAllCount > 0) {
        /*
         * Unpin all objects.
         */
        commonRef_unpinAll();

        suspendAllCount--;
    }

    debugMonitorExit(threadLock);
    eventHandler_unlock();
    /* let eventHelper.c: commandLoop() know we are resuming */
    unblockCommandLoop();

    return error;
}


StepRequest *
threadControl_getStepRequest(jthread thread)
{
    ThreadNode  *node;
    StepRequest *step;

    step = NULL;

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if (node != NULL) {
        step = &node->currentStep;
    }

    debugMonitorExit(threadLock);

    return step;
}

InvokeRequest *
threadControl_getInvokeRequest(jthread thread)
{
    ThreadNode    *node;
    InvokeRequest *request;

    request = NULL;

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if (node != NULL) {
         request = &node->currentInvoke;
    }

    debugMonitorExit(threadLock);

    return request;
}

jvmtiError
threadControl_addDebugThread(jthread thread)
{
    jvmtiError error;

    debugMonitorEnter(threadLock);
    if (debugThreadCount >= MAX_DEBUG_THREADS) {
        error = AGENT_ERROR_OUT_OF_MEMORY;
    } else {
        JNIEnv    *env;

        env = getEnv();
        debugThreads[debugThreadCount] = NULL;
        saveGlobalRef(env, thread, &(debugThreads[debugThreadCount]));
        if (debugThreads[debugThreadCount] == NULL) {
            error = AGENT_ERROR_OUT_OF_MEMORY;
        } else {
            debugThreadCount++;
            error = JVMTI_ERROR_NONE;
        }
    }
    debugMonitorExit(threadLock);
    return error;
}

static jvmtiError
threadControl_removeDebugThread(jthread thread)
{
    jvmtiError error;
    JNIEnv    *env;
    int        i;

    error = AGENT_ERROR_INVALID_THREAD;
    env   = getEnv();

    debugMonitorEnter(threadLock);
    for (i = 0; i< debugThreadCount; i++) {
        if (isSameObject(env, thread, debugThreads[i])) {
            int j;

            tossGlobalRef(env, &(debugThreads[i]));
            for (j = i+1; j < debugThreadCount; j++) {
                debugThreads[j-1] = debugThreads[j];
            }
            debugThreadCount--;
            error = JVMTI_ERROR_NONE;
            break;
        }
    }
    debugMonitorExit(threadLock);
    return error;
}

jboolean
threadControl_isDebugThread(jthread thread)
{
    int      i;
    jboolean rc;
    JNIEnv  *env;

    rc  = JNI_FALSE;
    env = getEnv();

    debugMonitorEnter(threadLock);
    for (i = 0; i < debugThreadCount; i++) {
        if (isSameObject(env, thread, debugThreads[i])) {
            rc = JNI_TRUE;
            break;
        }
    }
    debugMonitorExit(threadLock);
    return rc;
}

static void
initLocks(void)
{
    if (popFrameEventLock == NULL) {
        popFrameEventLock = debugMonitorCreate("JDWP PopFrame Event Lock");
        popFrameProceedLock = debugMonitorCreate("JDWP PopFrame Proceed Lock");
    }
}

static jboolean
getPopFrameThread(jthread thread)
{
    jboolean popFrameThread;

    debugMonitorEnter(threadLock);
    {
        ThreadNode *node;

        node = findThread(NULL, thread);
        if (node == NULL) {
            popFrameThread = JNI_FALSE;
        } else {
            popFrameThread = node->popFrameThread;
        }
    }
    debugMonitorExit(threadLock);

    return popFrameThread;
}

static void
setPopFrameThread(jthread thread, jboolean value)
{
    debugMonitorEnter(threadLock);
    {
        ThreadNode *node;

        node = findThread(NULL, thread);
        if (node == NULL) {
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"entry in thread table");
        } else {
            node->popFrameThread = value;
        }
    }
    debugMonitorExit(threadLock);
}

static jboolean
getPopFrameEvent(jthread thread)
{
    jboolean popFrameEvent;

    debugMonitorEnter(threadLock);
    {
        ThreadNode *node;

        node = findThread(NULL, thread);
        if (node == NULL) {
            popFrameEvent = JNI_FALSE;
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"entry in thread table");
        } else {
            popFrameEvent = node->popFrameEvent;
        }
    }
    debugMonitorExit(threadLock);

    return popFrameEvent;
}

static void
setPopFrameEvent(jthread thread, jboolean value)
{
    debugMonitorEnter(threadLock);
    {
        ThreadNode *node;

        node = findThread(NULL, thread);
        if (node == NULL) {
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"entry in thread table");
        } else {
            node->popFrameEvent = value;
            node->frameGeneration++; /* Increment on each resume */
        }
    }
    debugMonitorExit(threadLock);
}

static jboolean
getPopFrameProceed(jthread thread)
{
    jboolean popFrameProceed;

    debugMonitorEnter(threadLock);
    {
        ThreadNode *node;

        node = findThread(NULL, thread);
        if (node == NULL) {
            popFrameProceed = JNI_FALSE;
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"entry in thread table");
        } else {
            popFrameProceed = node->popFrameProceed;
        }
    }
    debugMonitorExit(threadLock);

    return popFrameProceed;
}

static void
setPopFrameProceed(jthread thread, jboolean value)
{
    debugMonitorEnter(threadLock);
    {
        ThreadNode *node;

        node = findThread(NULL, thread);
        if (node == NULL) {
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"entry in thread table");
        } else {
            node->popFrameProceed = value;
        }
    }
    debugMonitorExit(threadLock);
}

/**
 * Special event handler for events on the popped thread
 * that occur during the pop operation.
 */
static void
popFrameCompleteEvent(jthread thread)
{
      debugMonitorEnter(popFrameProceedLock);
      {
          /* notify that we got the event */
          debugMonitorEnter(popFrameEventLock);
          {
              setPopFrameEvent(thread, JNI_TRUE);
              debugMonitorNotify(popFrameEventLock);
          }
          debugMonitorExit(popFrameEventLock);

          /* make sure we get suspended again */
          setPopFrameProceed(thread, JNI_FALSE);
          while (getPopFrameProceed(thread) == JNI_FALSE) {
              debugMonitorWait(popFrameProceedLock);
          }
      }
      debugMonitorExit(popFrameProceedLock);
}

/**
 * Pop one frame off the stack of thread.
 * popFrameEventLock is already held
 */
static jvmtiError
popOneFrame(jthread thread)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,PopFrame)(gdata->jvmti, thread);
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }

    /* resume the popped thread so that the pop occurs and so we */
    /* will get the event (step or method entry) after the pop */
    LOG_MISC(("thread=%p resumed in popOneFrame", thread));
    error = JVMTI_FUNC_PTR(gdata->jvmti,ResumeThread)(gdata->jvmti, thread);
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }

    /* wait for the event to occur */
    setPopFrameEvent(thread, JNI_FALSE);
    while (getPopFrameEvent(thread) == JNI_FALSE) {
        debugMonitorWait(popFrameEventLock);
    }

    /* make sure not to suspend until the popped thread is on the wait */
    debugMonitorEnter(popFrameProceedLock);
    {
        /* return popped thread to suspended state */
        LOG_MISC(("thread=%p suspended in popOneFrame", thread));
        error = JVMTI_FUNC_PTR(gdata->jvmti,SuspendThread)(gdata->jvmti, thread);

        /* notify popped thread so it can proceed when resumed */
        setPopFrameProceed(thread, JNI_TRUE);
        debugMonitorNotify(popFrameProceedLock);
    }
    debugMonitorExit(popFrameProceedLock);

    return error;
}

/**
 * pop frames of the stack of 'thread' until 'frame' is popped.
 */
jvmtiError
threadControl_popFrames(jthread thread, FrameNumber fnum)
{
    jvmtiError error;
    jvmtiEventMode prevStepMode;
    jint framesPopped = 0;
    jint popCount;
    jboolean prevInvokeRequestMode;

    log_debugee_location("threadControl_popFrames()", thread, NULL, 0);

    initLocks();

    /* compute the number of frames to pop */
    popCount = fnum+1;
    if (popCount < 1) {
        return AGENT_ERROR_NO_MORE_FRAMES;
    }

    /* enable instruction level single step, but first note prev value */
    prevStepMode = threadControl_getInstructionStepMode(thread);

    /*
     * Fix bug 6517249.  The pop processing will disable invokes,
     * so remember if invokes are enabled now and restore
     * that state after we finish popping.
     */
    prevInvokeRequestMode = invoker_isEnabled(thread);

    error = threadControl_setEventMode(JVMTI_ENABLE,
                                       EI_SINGLE_STEP, thread);
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }

    /* Inform eventHandler logic we are in a popFrame for this thread */
    debugMonitorEnter(popFrameEventLock);
    {
        setPopFrameThread(thread, JNI_TRUE);
        /* pop frames using single step */
        while (framesPopped++ < popCount) {
            error = popOneFrame(thread);
            if (error != JVMTI_ERROR_NONE) {
                break;
            }
        }
        setPopFrameThread(thread, JNI_FALSE);
    }
    debugMonitorExit(popFrameEventLock);

    /*  Reset StepRequest info (fromLine and stackDepth) after popframes
     *  only if stepping is enabled.
     */
    if (prevStepMode == JVMTI_ENABLE) {
        stepControl_resetRequest(thread);
    }

    if (prevInvokeRequestMode) {
        invoker_enableInvokeRequests(thread);
    }

    /* restore state */
    (void)threadControl_setEventMode(prevStepMode,
                               EI_SINGLE_STEP, thread);

    return error;
}

/* Check to see if any events are being consumed by a popFrame(). */
static jboolean
checkForPopFrameEvents(JNIEnv *env, EventIndex ei, jthread thread)
{
    if ( getPopFrameThread(thread) ) {
        switch (ei) {
            case EI_THREAD_START:
                /* Excuse me? */
                EXIT_ERROR(AGENT_ERROR_INTERNAL, "thread start during pop frame");
                break;
            case EI_THREAD_END:
                /* Thread wants to end? let it. */
                setPopFrameThread(thread, JNI_FALSE);
                popFrameCompleteEvent(thread);
                break;
            case EI_SINGLE_STEP:
                /* This is an event we requested to mark the */
                /*        completion of the pop frame */
                popFrameCompleteEvent(thread);
                return JNI_TRUE;
            case EI_BREAKPOINT:
            case EI_EXCEPTION:
            case EI_FIELD_ACCESS:
            case EI_FIELD_MODIFICATION:
            case EI_METHOD_ENTRY:
            case EI_METHOD_EXIT:
                /* Tell event handler to assume event has been consumed. */
                return JNI_TRUE;
            default:
                break;
        }
    }
    /* Pretend we were never called */
    return JNI_FALSE;
}

struct bag *
threadControl_onEventHandlerEntry(jbyte sessionID, EventInfo *evinfo, jobject currentException)
{
    ThreadNode *node;
    JNIEnv     *env;
    struct bag *eventBag;
    jthread     threadToSuspend;
    jboolean    consumed;
    EventIndex  ei = evinfo->ei;
    jthread     thread = evinfo->thread;

    env             = getEnv();
    threadToSuspend = NULL;

    log_debugee_location("threadControl_onEventHandlerEntry()", thread, NULL, 0);

    /* Events during pop commands may need to be ignored here. */
    consumed = checkForPopFrameEvents(env, ei, thread);
    if ( consumed ) {
        /* Always restore any exception (see below). */
        if (currentException != NULL) {
            JNI_FUNC_PTR(env,Throw)(env, currentException);
        } else {
            JNI_FUNC_PTR(env,ExceptionClear)(env);
        }
        return NULL;
    }

    debugMonitorEnter(threadLock);

    /*
     * Check the list of unknown threads maintained by suspend
     * and resume. If this thread is currently present in the
     * list, it should be
     * moved to the runningThreads list, since it is a
     * well-known thread now.
     */
    node = findThread(&otherThreads, thread);
    if (node != NULL) {
        moveNode(&otherThreads, &runningThreads, node);
        /* Now that we know the thread has started, we can set its TLS.*/
        setThreadLocalStorage(thread, (void*)node);
    } else {
        /*
         * Get a thread node for the reporting thread. For thread start
         * events, or if this event precedes a thread start event,
         * the thread node may need to be created.
         *
         * It is possible for certain events (notably method entry/exit)
         * to precede thread start for some VM implementations.
         */
        node = insertThread(env, &runningThreads, thread);
    }

    if (ei == EI_THREAD_START) {
        node->isStarted = JNI_TRUE;
        processDeferredEventModes(env, thread, node);
    }

    node->current_ei = ei;
    eventBag = node->eventBag;
    if (node->suspendOnStart) {
        threadToSuspend = node->thread;
    }
    debugMonitorExit(threadLock);

    if (threadToSuspend != NULL) {
        /*
         * An attempt was made to suspend this thread before it started.
         * We must suspend it now, before it starts to run. This must
         * be done with no locks held.
         */
        eventHelper_suspendThread(sessionID, threadToSuspend);
    }

    return eventBag;
}

static void
doPendingTasks(JNIEnv *env, ThreadNode *node)
{
    /*
     * Take care of any pending interrupts/stops, and clear out
     * info on pending interrupts/stops.
     */
    if (node->pendingInterrupt) {
        JVMTI_FUNC_PTR(gdata->jvmti,InterruptThread)
                        (gdata->jvmti, node->thread);
        /*
         * TO DO: Log error
         */
        node->pendingInterrupt = JNI_FALSE;
    }

    if (node->pendingStop != NULL) {
        JVMTI_FUNC_PTR(gdata->jvmti,StopThread)
                        (gdata->jvmti, node->thread, node->pendingStop);
        /*
         * TO DO: Log error
         */
        tossGlobalRef(env, &(node->pendingStop));
    }
}

void
threadControl_onEventHandlerExit(EventIndex ei, jthread thread,
                                 struct bag *eventBag)
{
    ThreadNode *node;

    log_debugee_location("threadControl_onEventHandlerExit()", thread, NULL, 0);

    if (ei == EI_THREAD_END) {
        eventHandler_lock(); /* for proper lock order */
    }
    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if (node == NULL) {
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"thread list corrupted");
    } else {
        JNIEnv *env;

        env = getEnv();
        if (ei == EI_THREAD_END) {
            jboolean inResume = (node->resumeFrameDepth > 0);
            removeThread(env, &runningThreads, thread);
            node = NULL;   /* has been freed */

            /*
             * Clean up mechanism used to detect end of
             * resume.
             */
            if (inResume) {
                notifyAppResumeComplete();
            }
        } else {
            /* No point in doing this if the thread is about to die.*/
            doPendingTasks(env, node);
            node->eventBag = eventBag;
            node->current_ei = 0;
        }
    }

    debugMonitorExit(threadLock);
    if (ei == EI_THREAD_END) {
        eventHandler_unlock();
    }
}

/* Returns JDWP flavored status and status flags. */
jvmtiError
threadControl_applicationThreadStatus(jthread thread,
                        jdwpThreadStatus *pstatus, jint *statusFlags)
{
    ThreadNode *node;
    jvmtiError  error;
    jint        state;

    log_debugee_location("threadControl_applicationThreadStatus()", thread, NULL, 0);

    debugMonitorEnter(threadLock);

    error = threadState(thread, &state);
    *pstatus = map2jdwpThreadStatus(state);
    *statusFlags = map2jdwpSuspendStatus(state);

    if (error == JVMTI_ERROR_NONE) {
        node = findThread(&runningThreads, thread);
        if ((node != NULL) && HANDLING_EVENT(node)) {
            /*
             * While processing an event, an application thread is always
             * considered to be running even if its handler happens to be
             * cond waiting on an internal debugger monitor, etc.
             *
             * Leave suspend status untouched since it is not possible
             * to distinguish debugger suspends from app suspends.
             */
            *pstatus = JDWP_THREAD_STATUS(RUNNING);
        }
    }

    debugMonitorExit(threadLock);

    return error;
}

jvmtiError
threadControl_interrupt(jthread thread)
{
    ThreadNode *node;
    jvmtiError  error;

    error = JVMTI_ERROR_NONE;

    log_debugee_location("threadControl_interrupt()", thread, NULL, 0);

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if ((node == NULL) || !HANDLING_EVENT(node)) {
        error = JVMTI_FUNC_PTR(gdata->jvmti,InterruptThread)
                        (gdata->jvmti, thread);
    } else {
        /*
         * Hold any interrupts until after the event is processed.
         */
        node->pendingInterrupt = JNI_TRUE;
    }

    debugMonitorExit(threadLock);

    return error;
}

void
threadControl_clearCLEInfo(JNIEnv *env, jthread thread)
{
    ThreadNode *node;

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if (node != NULL) {
        node->cleInfo.ei = 0;
        if (node->cleInfo.clazz != NULL) {
            tossGlobalRef(env, &(node->cleInfo.clazz));
        }
    }

    debugMonitorExit(threadLock);
}

jboolean
threadControl_cmpCLEInfo(JNIEnv *env, jthread thread, jclass clazz,
                         jmethodID method, jlocation location)
{
    ThreadNode *node;
    jboolean    result;

    result = JNI_FALSE;

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if (node != NULL && node->cleInfo.ei != 0 &&
        node->cleInfo.method == method &&
        node->cleInfo.location == location &&
        (isSameObject(env, node->cleInfo.clazz, clazz))) {
        result = JNI_TRUE; /* we have a match */
    }

    debugMonitorExit(threadLock);

    return result;
}

void
threadControl_saveCLEInfo(JNIEnv *env, jthread thread, EventIndex ei,
                          jclass clazz, jmethodID method, jlocation location)
{
    ThreadNode *node;

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if (node != NULL) {
        node->cleInfo.ei = ei;
        /* Create a class ref that will live beyond */
        /* the end of this call */
        saveGlobalRef(env, clazz, &(node->cleInfo.clazz));
        /* if returned clazz is NULL, we just won't match */
        node->cleInfo.method    = method;
        node->cleInfo.location  = location;
    }

    debugMonitorExit(threadLock);
}

void
threadControl_setPendingInterrupt(jthread thread)
{
    ThreadNode *node;

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if (node != NULL) {
        node->pendingInterrupt = JNI_TRUE;
    }

    debugMonitorExit(threadLock);
}

jvmtiError
threadControl_stop(jthread thread, jobject throwable)
{
    ThreadNode *node;
    jvmtiError  error;

    error = JVMTI_ERROR_NONE;

    log_debugee_location("threadControl_stop()", thread, NULL, 0);

    debugMonitorEnter(threadLock);

    node = findThread(&runningThreads, thread);
    if ((node == NULL) || !HANDLING_EVENT(node)) {
        error = JVMTI_FUNC_PTR(gdata->jvmti,StopThread)
                        (gdata->jvmti, thread, throwable);
    } else {
        JNIEnv *env;

        /*
         * Hold any stops until after the event is processed.
         */
        env = getEnv();
        saveGlobalRef(env, throwable, &(node->pendingStop));
    }

    debugMonitorExit(threadLock);

    return error;
}

static jvmtiError
detachHelper(JNIEnv *env, ThreadNode *node, void *arg)
{
    invoker_detach(&node->currentInvoke);
    return JVMTI_ERROR_NONE;
}

void
threadControl_detachInvokes(void)
{
    JNIEnv *env;

    env = getEnv();
    invoker_lock(); /* for proper lock order */
    debugMonitorEnter(threadLock);
    (void)enumerateOverThreadList(env, &runningThreads, detachHelper, NULL);
    debugMonitorExit(threadLock);
    invoker_unlock();
}

static jvmtiError
resetHelper(JNIEnv *env, ThreadNode *node, void *arg)
{
    if (node->toBeResumed) {
        LOG_MISC(("thread=%p resumed", node->thread));
        (void)JVMTI_FUNC_PTR(gdata->jvmti,ResumeThread)(gdata->jvmti, node->thread);
        node->frameGeneration++; /* Increment on each resume */
    }
    stepControl_clearRequest(node->thread, &node->currentStep);
    node->toBeResumed = JNI_FALSE;
    node->suspendCount = 0;
    node->suspendOnStart = JNI_FALSE;

    return JVMTI_ERROR_NONE;
}

void
threadControl_reset(void)
{
    JNIEnv *env;

    env = getEnv();
    eventHandler_lock(); /* for proper lock order */
    debugMonitorEnter(threadLock);
    (void)enumerateOverThreadList(env, &runningThreads, resetHelper, NULL);
    (void)enumerateOverThreadList(env, &otherThreads, resetHelper, NULL);

    removeResumed(env, &otherThreads);

    freeDeferredEventModes(env);

    suspendAllCount = 0;

    /* Everything should have been resumed */
    JDI_ASSERT(otherThreads.first == NULL);

    debugMonitorExit(threadLock);
    eventHandler_unlock();
}

jvmtiEventMode
threadControl_getInstructionStepMode(jthread thread)
{
    ThreadNode    *node;
    jvmtiEventMode mode;

    mode = JVMTI_DISABLE;

    debugMonitorEnter(threadLock);
    node = findThread(&runningThreads, thread);
    if (node != NULL) {
        mode = node->instructionStepMode;
    }
    debugMonitorExit(threadLock);
    return mode;
}

jvmtiError
threadControl_setEventMode(jvmtiEventMode mode, EventIndex ei, jthread thread)
{
    jvmtiError error;

    /* Global event */
    if ( thread == NULL ) {
        error = JVMTI_FUNC_PTR(gdata->jvmti,SetEventNotificationMode)
                    (gdata->jvmti, mode, eventIndex2jvmti(ei), thread);
    } else {
        /* Thread event */
        ThreadNode *node;

        debugMonitorEnter(threadLock);
        {
            node = findThread(&runningThreads, thread);
            if ((node == NULL) || (!node->isStarted)) {
                JNIEnv *env;

                env = getEnv();
                error = addDeferredEventMode(env, mode, ei, thread);
            } else {
                error = threadSetEventNotificationMode(node,
                        mode, ei, thread);
            }
        }
        debugMonitorExit(threadLock);

    }
    return error;
}

/*
 * Returns the current thread, if the thread has generated at least
 * one event, and has not generated a thread end event.
 */
jthread
threadControl_currentThread(void)
{
    jthread thread;

    debugMonitorEnter(threadLock);
    {
        ThreadNode *node;

        node = findThread(&runningThreads, NULL);
        thread = (node == NULL) ? NULL : node->thread;
    }
    debugMonitorExit(threadLock);

    return thread;
}

jlong
threadControl_getFrameGeneration(jthread thread)
{
    jlong frameGeneration = -1;

    debugMonitorEnter(threadLock);
    {
        ThreadNode *node;

        node = findThread(NULL, thread);

        if (node != NULL) {
            frameGeneration = node->frameGeneration;
        }
    }
    debugMonitorExit(threadLock);

    return frameGeneration;
}

/***** debugging *****/

#ifdef DEBUG

void
threadControl_dumpAllThreads()
{
    tty_message("Dumping runningThreads:\n");
    dumpThreadList(&runningThreads);
    tty_message("Dumping otherThreads:\n");
    dumpThreadList(&otherThreads);
}

static void
dumpThreadList(ThreadList *list)
{
    ThreadNode *node;
    for (node = list->first; node != NULL; node = node->next) {
        if (!node->isDebugThread) {
            dumpThread(node);
        }
    }
}

static void
dumpThread(ThreadNode *node) {
    tty_message("  Thread: node = %p, jthread = %p", node, node->thread);
#ifdef DEBUG_THREADNAME
    tty_message("\tname: %s", node->name);
#endif
    // More fields can be printed here when needed. The amount of output is intentionlly
    // kept small so it doesn't generate too much output.
    tty_message("\tsuspendCount: %d", node->suspendCount);
}

#endif /* DEBUG */
