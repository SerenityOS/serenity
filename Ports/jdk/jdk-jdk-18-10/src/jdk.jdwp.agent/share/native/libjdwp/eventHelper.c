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
#include "outStream.h"
#include "eventHandler.h"
#include "threadControl.h"
#include "invoker.h"
#include "signature.h"


#define COMMAND_LOOP_THREAD_NAME "JDWP Event Helper Thread"

/*
 * Event helper thread command commandKinds
 */
#define COMMAND_REPORT_EVENT_COMPOSITE          1
#define COMMAND_REPORT_INVOKE_DONE              2
#define COMMAND_REPORT_VM_INIT                  3
#define COMMAND_SUSPEND_THREAD                  4

/*
 * Event helper thread command singleKinds
 */
#define COMMAND_SINGLE_EVENT                    11
#define COMMAND_SINGLE_UNLOAD                   12
#define COMMAND_SINGLE_FRAME_EVENT              13

typedef struct EventCommandSingle {
    jbyte suspendPolicy; /* NOTE: Must be the first field */
    jint id;
    EventInfo info;
} EventCommandSingle;

typedef struct UnloadCommandSingle {
    char *classSignature;
    jint id;
} UnloadCommandSingle;

typedef struct FrameEventCommandSingle {
    jbyte suspendPolicy; /* NOTE: Must be the first field */
    jint id;
    EventIndex ei;
    jthread thread;
    jclass clazz;
    jmethodID method;
    jlocation location;
    char typeKey;         /* Not used for method entry events */
                          /* If typeKey is 0, then no return value is needed */
    jvalue returnValue;   /* Not used for method entry events */
} FrameEventCommandSingle;

typedef struct CommandSingle {
    jint singleKind;
    union {
        EventCommandSingle eventCommand;
        UnloadCommandSingle unloadCommand;
        FrameEventCommandSingle frameEventCommand;
    } u;
} CommandSingle;

typedef struct ReportInvokeDoneCommand {
    jthread thread;
} ReportInvokeDoneCommand;

typedef struct ReportVMInitCommand {
    jbyte suspendPolicy; /* NOTE: Must be the first field */
    jthread thread;
} ReportVMInitCommand;

typedef struct SuspendThreadCommand {
    jthread thread;
} SuspendThreadCommand;

typedef struct ReportEventCompositeCommand {
    jbyte suspendPolicy; /* NOTE: Must be the first field */
    jint eventCount;
    CommandSingle singleCommand[1]; /* variable length */
} ReportEventCompositeCommand;

typedef struct HelperCommand {
    jint commandKind;
    jboolean done;
    jboolean waiting;
    jbyte sessionID;
    struct HelperCommand *next;
    union {
        /* NOTE: Each of the structs below must have the same first field */
        ReportEventCompositeCommand reportEventComposite;
        ReportInvokeDoneCommand     reportInvokeDone;
        ReportVMInitCommand         reportVMInit;
        SuspendThreadCommand        suspendThread;
    } u;
    /* composite array expand out, put nothing after */
} HelperCommand;

typedef struct {
    HelperCommand *head;
    HelperCommand *tail;
} CommandQueue;

static CommandQueue commandQueue;
static jrawMonitorID commandQueueLock;
static jrawMonitorID commandCompleteLock;
static jrawMonitorID blockCommandLoopLock;
static jrawMonitorID vmDeathLock;
static volatile jboolean commandLoopEnteredVmDeathLock = JNI_FALSE;

static jint maxQueueSize = 50 * 1024; /* TO DO: Make this configurable */
static jboolean holdEvents;
static jint currentQueueSize = 0;
static jint currentSessionID;

static void saveEventInfoRefs(JNIEnv *env, EventInfo *evinfo);
static void tossEventInfoRefs(JNIEnv *env, EventInfo *evinfo);

static jint
commandSize(HelperCommand *command)
{
    jint size = sizeof(HelperCommand);
    if (command->commandKind == COMMAND_REPORT_EVENT_COMPOSITE) {
        /*
         * One event is accounted for in the Helper Command. If there are
         * more, add to size here.
         */
        /*LINTED*/
        size += ((int)sizeof(CommandSingle) *
                     (command->u.reportEventComposite.eventCount - 1));
    }
    return size;
}

static void
freeCommand(HelperCommand *command)
{
    if ( command == NULL )
        return;
    jvmtiDeallocate(command);
}

static void
enqueueCommand(HelperCommand *command,
               jboolean wait, jboolean reportingVMDeath)
{
    static jboolean vmDeathReported = JNI_FALSE;
    CommandQueue *queue = &commandQueue;
    jint size = commandSize(command);

    command->done = JNI_FALSE;
    command->waiting = wait;
    command->next = NULL;

    debugMonitorEnter(commandQueueLock);
    while (size + currentQueueSize > maxQueueSize) {
        debugMonitorWait(commandQueueLock);
    }
    log_debugee_location("enqueueCommand(): HelperCommand being processed", NULL, NULL, 0);
    if (vmDeathReported) {
        /* send no more events after VMDeath and don't wait */
        wait = JNI_FALSE;
    } else {
        currentQueueSize += size;

        if (queue->head == NULL) {
            queue->head = command;
        } else {
            queue->tail->next = command;
        }
        queue->tail = command;

        if (reportingVMDeath) {
            vmDeathReported = JNI_TRUE;
        }
    }
    debugMonitorNotifyAll(commandQueueLock);
    debugMonitorExit(commandQueueLock);

    if (wait) {
        debugMonitorEnter(commandCompleteLock);
        while (!command->done) {
            log_debugee_location("enqueueCommand(): HelperCommand wait", NULL, NULL, 0);
            debugMonitorWait(commandCompleteLock);
        }
        freeCommand(command);
        debugMonitorExit(commandCompleteLock);
    }
}

static void
completeCommand(HelperCommand *command)
{
    if (command->waiting) {
        debugMonitorEnter(commandCompleteLock);
        command->done = JNI_TRUE;
        log_debugee_location("completeCommand(): HelperCommand done waiting", NULL, NULL, 0);
        debugMonitorNotifyAll(commandCompleteLock);
        debugMonitorExit(commandCompleteLock);
    } else {
        freeCommand(command);
    }
}

static HelperCommand *
dequeueCommand(void)
{
    HelperCommand *command = NULL;
    CommandQueue *queue = &commandQueue;
    jint size;

    debugMonitorEnter(commandQueueLock);

    while (command == NULL) {
        while (holdEvents || (queue->head == NULL)) {
            debugMonitorWait(commandQueueLock);
        }

        JDI_ASSERT(queue->head);
        command = queue->head;
        queue->head = command->next;
        if (queue->tail == command) {
            queue->tail = NULL;
        }

        log_debugee_location("dequeueCommand(): command being dequeued", NULL, NULL, 0);

        size = commandSize(command);
        /*
         * Immediately close out any commands enqueued from
         * a dead VM or a previously attached debugger.
         */
        if (gdata->vmDead || command->sessionID != currentSessionID) {
            log_debugee_location("dequeueCommand(): command session removal", NULL, NULL, 0);
            completeCommand(command);
            command = NULL;
        }

        /*
         * There's room in the queue for more.
         */
        currentQueueSize -= size;
        debugMonitorNotifyAll(commandQueueLock);
    }

    debugMonitorExit(commandQueueLock);

    return command;
}

void eventHelper_holdEvents(void)
{
    debugMonitorEnter(commandQueueLock);
    holdEvents = JNI_TRUE;
    debugMonitorNotifyAll(commandQueueLock);
    debugMonitorExit(commandQueueLock);
}

void eventHelper_releaseEvents(void)
{
    debugMonitorEnter(commandQueueLock);
    holdEvents = JNI_FALSE;
    debugMonitorNotifyAll(commandQueueLock);
    debugMonitorExit(commandQueueLock);
}

static void
writeSingleStepEvent(JNIEnv *env, PacketOutputStream *out, EventInfo *evinfo)
{
    (void)outStream_writeObjectRef(env, out, evinfo->thread);
    writeCodeLocation(out, evinfo->clazz, evinfo->method, evinfo->location);
}

static void
writeBreakpointEvent(JNIEnv *env, PacketOutputStream *out, EventInfo *evinfo)
{
    (void)outStream_writeObjectRef(env, out, evinfo->thread);
    writeCodeLocation(out, evinfo->clazz, evinfo->method, evinfo->location);
}

static void
writeFieldAccessEvent(JNIEnv *env, PacketOutputStream *out, EventInfo *evinfo)
{
    jbyte fieldClassTag;

    fieldClassTag = referenceTypeTag(evinfo->u.field_access.field_clazz);

    (void)outStream_writeObjectRef(env, out, evinfo->thread);
    writeCodeLocation(out, evinfo->clazz, evinfo->method, evinfo->location);
    (void)outStream_writeByte(out, fieldClassTag);
    (void)outStream_writeObjectRef(env, out, evinfo->u.field_access.field_clazz);
    (void)outStream_writeFieldID(out, evinfo->u.field_access.field);
    (void)outStream_writeObjectTag(env, out, evinfo->object);
    (void)outStream_writeObjectRef(env, out, evinfo->object);
}

static void
writeFieldModificationEvent(JNIEnv *env, PacketOutputStream *out,
                            EventInfo *evinfo)
{
    jbyte fieldClassTag;

    fieldClassTag = referenceTypeTag(evinfo->u.field_modification.field_clazz);

    (void)outStream_writeObjectRef(env, out, evinfo->thread);
    writeCodeLocation(out, evinfo->clazz, evinfo->method, evinfo->location);
    (void)outStream_writeByte(out, fieldClassTag);
    (void)outStream_writeObjectRef(env, out, evinfo->u.field_modification.field_clazz);
    (void)outStream_writeFieldID(out, evinfo->u.field_modification.field);
    (void)outStream_writeObjectTag(env, out, evinfo->object);
    (void)outStream_writeObjectRef(env, out, evinfo->object);
    (void)outStream_writeValue(env, out, (jbyte)evinfo->u.field_modification.signature_type,
                         evinfo->u.field_modification.new_value);
}

static void
writeExceptionEvent(JNIEnv *env, PacketOutputStream *out, EventInfo *evinfo)
{
    (void)outStream_writeObjectRef(env, out, evinfo->thread);
    writeCodeLocation(out, evinfo->clazz, evinfo->method, evinfo->location);
    (void)outStream_writeObjectTag(env, out, evinfo->object);
    (void)outStream_writeObjectRef(env, out, evinfo->object);
    writeCodeLocation(out, evinfo->u.exception.catch_clazz,
                      evinfo->u.exception.catch_method, evinfo->u.exception.catch_location);
}

static void
writeThreadEvent(JNIEnv *env, PacketOutputStream *out, EventInfo *evinfo)
{
    (void)outStream_writeObjectRef(env, out, evinfo->thread);
}

static void
writeMonitorEvent(JNIEnv *env, PacketOutputStream *out, EventInfo *evinfo)
{
    jclass klass;
    (void)outStream_writeObjectRef(env, out, evinfo->thread);
    (void)outStream_writeObjectTag(env, out, evinfo->object);
    (void)outStream_writeObjectRef(env, out, evinfo->object);
    if (evinfo->ei == EI_MONITOR_WAIT || evinfo->ei == EI_MONITOR_WAITED) {
        /* clazz of evinfo was set to class of monitor object for monitor wait event class filtering.
         * So get the method class to write location info.
         * See cbMonitorWait() and cbMonitorWaited() function in eventHandler.c.
         */
        klass=getMethodClass(gdata->jvmti, evinfo->method);
        writeCodeLocation(out, klass, evinfo->method, evinfo->location);
        if (evinfo->ei == EI_MONITOR_WAIT) {
            (void)outStream_writeLong(out, evinfo->u.monitor.timeout);
        } else  if (evinfo->ei == EI_MONITOR_WAITED) {
            (void)outStream_writeBoolean(out, evinfo->u.monitor.timed_out);
        }
        /* This runs in a command loop and this thread may not return to java.
         * So we need to delete the local ref created by jvmti GetMethodDeclaringClass.
         */
        JNI_FUNC_PTR(env,DeleteLocalRef)(env, klass);
    } else {
        writeCodeLocation(out, evinfo->clazz, evinfo->method, evinfo->location);
    }
}

static void
writeClassEvent(JNIEnv *env, PacketOutputStream *out, EventInfo *evinfo)
{
    jbyte classTag;
    jint status;
    char *signature = NULL;
    jvmtiError error;

    classTag = referenceTypeTag(evinfo->clazz);
    error = classSignature(evinfo->clazz, &signature, NULL);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"signature");
    }
    status = classStatus(evinfo->clazz);

    (void)outStream_writeObjectRef(env, out, evinfo->thread);
    (void)outStream_writeByte(out, classTag);
    (void)outStream_writeObjectRef(env, out, evinfo->clazz);
    (void)outStream_writeString(out, signature);
    (void)outStream_writeInt(out, map2jdwpClassStatus(status));
    jvmtiDeallocate(signature);
}

static void
writeVMDeathEvent(JNIEnv *env, PacketOutputStream *out, EventInfo *evinfo)
{
}

static void
handleEventCommandSingle(JNIEnv *env, PacketOutputStream *out,
                           EventCommandSingle *command)
{
    EventInfo *evinfo = &command->info;

    (void)outStream_writeByte(out, eventIndex2jdwp(evinfo->ei));
    (void)outStream_writeInt(out, command->id);

    switch (evinfo->ei) {
        case EI_SINGLE_STEP:
            writeSingleStepEvent(env, out, evinfo);
            break;
        case EI_BREAKPOINT:
            writeBreakpointEvent(env, out, evinfo);
            break;
        case EI_FIELD_ACCESS:
            writeFieldAccessEvent(env, out, evinfo);
            break;
        case EI_FIELD_MODIFICATION:
            writeFieldModificationEvent(env, out, evinfo);
            break;
        case EI_EXCEPTION:
            writeExceptionEvent(env, out, evinfo);
            break;
        case EI_THREAD_START:
        case EI_THREAD_END:
            writeThreadEvent(env, out, evinfo);
            break;
        case EI_CLASS_LOAD:
        case EI_CLASS_PREPARE:
            writeClassEvent(env, out, evinfo);
            break;
        case EI_MONITOR_CONTENDED_ENTER:
        case EI_MONITOR_CONTENDED_ENTERED:
        case EI_MONITOR_WAIT:
        case EI_MONITOR_WAITED:
            writeMonitorEvent(env, out, evinfo);
            break;
        case EI_VM_DEATH:
            writeVMDeathEvent(env, out, evinfo);
            break;
        default:
            EXIT_ERROR(AGENT_ERROR_INVALID_EVENT_TYPE,"unknown event index");
            break;
    }
    tossEventInfoRefs(env, evinfo);
}

static void
handleUnloadCommandSingle(JNIEnv* env, PacketOutputStream *out,
                           UnloadCommandSingle *command)
{
    (void)outStream_writeByte(out, JDWP_EVENT(CLASS_UNLOAD));
    (void)outStream_writeInt(out, command->id);
    (void)outStream_writeString(out, command->classSignature);
    jvmtiDeallocate(command->classSignature);
    command->classSignature = NULL;
}

static void
handleFrameEventCommandSingle(JNIEnv* env, PacketOutputStream *out,
                              FrameEventCommandSingle *command)
{
    if (command->typeKey) {
        (void)outStream_writeByte(out, JDWP_EVENT(METHOD_EXIT_WITH_RETURN_VALUE));
    } else {
        (void)outStream_writeByte(out, eventIndex2jdwp(command->ei));
    }
    (void)outStream_writeInt(out, command->id);
    (void)outStream_writeObjectRef(env, out, command->thread);
    writeCodeLocation(out, command->clazz, command->method, command->location);
    if (command->typeKey) {
        (void)outStream_writeValue(env, out, command->typeKey, command->returnValue);
        if (isReferenceTag(command->typeKey) &&
            command->returnValue.l != NULL) {
            tossGlobalRef(env, &(command->returnValue.l));
        }
    }
    tossGlobalRef(env, &(command->thread));
    tossGlobalRef(env, &(command->clazz));
}

static void
suspendWithInvokeEnabled(jbyte policy, jthread thread)
{
    invoker_enableInvokeRequests(thread);

    if (policy == JDWP_SUSPEND_POLICY(ALL)) {
        (void)threadControl_suspendAll();
    } else {
        (void)threadControl_suspendThread(thread, JNI_FALSE);
    }
}

static void
handleReportEventCompositeCommand(JNIEnv *env,
                                  ReportEventCompositeCommand *recc)
{
    PacketOutputStream out;
    jint count = recc->eventCount;
    jint i;

    if (recc->suspendPolicy != JDWP_SUSPEND_POLICY(NONE)) {
        /* must determine thread to interrupt before writing */
        /* since writing destroys it */
        jthread thread = NULL;
        for (i = 0; i < count; i++) {
            CommandSingle *single = &(recc->singleCommand[i]);
            switch (single->singleKind) {
                case COMMAND_SINGLE_EVENT:
                    thread = single->u.eventCommand.info.thread;
                    break;
                case COMMAND_SINGLE_FRAME_EVENT:
                    thread = single->u.frameEventCommand.thread;
                    break;
            }
            if (thread != NULL) {
                break;
            }
        }

        if (thread == NULL) {
            (void)threadControl_suspendAll();
        } else {
            suspendWithInvokeEnabled(recc->suspendPolicy, thread);
        }
    }

    outStream_initCommand(&out, uniqueID(), 0x0,
                          JDWP_COMMAND_SET(Event),
                          JDWP_COMMAND(Event, Composite));
    (void)outStream_writeByte(&out, recc->suspendPolicy);
    (void)outStream_writeInt(&out, count);

    for (i = 0; i < count; i++) {
        CommandSingle *single = &(recc->singleCommand[i]);
        switch (single->singleKind) {
            case COMMAND_SINGLE_EVENT:
                handleEventCommandSingle(env, &out,
                                         &single->u.eventCommand);
                break;
            case COMMAND_SINGLE_UNLOAD:
                handleUnloadCommandSingle(env, &out,
                                          &single->u.unloadCommand);
                break;
            case COMMAND_SINGLE_FRAME_EVENT:
                handleFrameEventCommandSingle(env, &out,
                                              &single->u.frameEventCommand);
                break;
        }
    }

    outStream_sendCommand(&out);
    outStream_destroy(&out);
}

static void
handleReportInvokeDoneCommand(JNIEnv* env, ReportInvokeDoneCommand *command)
{
    invoker_completeInvokeRequest(command->thread);
    tossGlobalRef(env, &(command->thread));
}

static void
handleReportVMInitCommand(JNIEnv* env, ReportVMInitCommand *command)
{
    PacketOutputStream out;

    if (command->suspendPolicy == JDWP_SUSPEND_POLICY(ALL)) {
        (void)threadControl_suspendAll();
    } else if (command->suspendPolicy == JDWP_SUSPEND_POLICY(EVENT_THREAD)) {
        (void)threadControl_suspendThread(command->thread, JNI_FALSE);
    }

    outStream_initCommand(&out, uniqueID(), 0x0,
                          JDWP_COMMAND_SET(Event),
                          JDWP_COMMAND(Event, Composite));
    (void)outStream_writeByte(&out, command->suspendPolicy);
    (void)outStream_writeInt(&out, 1);   /* Always one component */
    (void)outStream_writeByte(&out, JDWP_EVENT(VM_INIT));
    (void)outStream_writeInt(&out, 0);    /* Not in response to an event req. */

    (void)outStream_writeObjectRef(env, &out, command->thread);

    outStream_sendCommand(&out);
    outStream_destroy(&out);
    /* Why aren't we tossing this: tossGlobalRef(env, &(command->thread)); */
}

static void
handleSuspendThreadCommand(JNIEnv* env, SuspendThreadCommand *command)
{
    /*
     * For the moment, there's  nothing that can be done with the
     * return code, so we don't check it here.
     */
    (void)threadControl_suspendThread(command->thread, JNI_TRUE);
    tossGlobalRef(env, &(command->thread));
}

static void
handleCommand(JNIEnv *env, HelperCommand *command)
{
    switch (command->commandKind) {
        case COMMAND_REPORT_EVENT_COMPOSITE:
            handleReportEventCompositeCommand(env,
                                        &command->u.reportEventComposite);
            break;
        case COMMAND_REPORT_INVOKE_DONE:
            handleReportInvokeDoneCommand(env, &command->u.reportInvokeDone);
            break;
        case COMMAND_REPORT_VM_INIT:
            handleReportVMInitCommand(env, &command->u.reportVMInit);
            break;
        case COMMAND_SUSPEND_THREAD:
            handleSuspendThreadCommand(env, &command->u.suspendThread);
            break;
        default:
            EXIT_ERROR(AGENT_ERROR_INVALID_EVENT_TYPE,"Event Helper Command");
            break;
    }
}

/*
 * There was an assumption that only one event with a suspend-all
 * policy could be processed by commandLoop() at one time. It was
 * assumed that native thread suspension from the first suspend-all
 * event would prevent the second suspend-all event from making it
 * into the command queue. For the Classic VM, this was a reasonable
 * assumption. However, in HotSpot all thread suspension requires a
 * VM operation and VM operations take time.
 *
 * The solution is to add a mechanism to prevent commandLoop() from
 * processing more than one event with a suspend-all policy. This is
 * accomplished by forcing commandLoop() to wait for either
 * ThreadReferenceImpl.c: resume() or VirtualMachineImpl.c: resume()
 * when an event with a suspend-all policy has been completed.
 */
static jboolean blockCommandLoop = JNI_FALSE;

/*
 * We wait for either ThreadReferenceImpl.c: resume() or
 * VirtualMachineImpl.c: resume() to be called.
 */
static void
doBlockCommandLoop(void) {
    debugMonitorEnter(blockCommandLoopLock);
    while (blockCommandLoop == JNI_TRUE) {
        debugMonitorWait(blockCommandLoopLock);
    }
    debugMonitorExit(blockCommandLoopLock);
}

/*
 * If the command that we are about to execute has a suspend-all
 * policy, then prepare for either ThreadReferenceImpl.c: resume()
 * or VirtualMachineImpl.c: resume() to be called.
 */
static jboolean
needBlockCommandLoop(HelperCommand *cmd) {
    if (cmd->commandKind == COMMAND_REPORT_EVENT_COMPOSITE
    && cmd->u.reportEventComposite.suspendPolicy == JDWP_SUSPEND_POLICY(ALL)) {
        debugMonitorEnter(blockCommandLoopLock);
        blockCommandLoop = JNI_TRUE;
        debugMonitorExit(blockCommandLoopLock);

        return JNI_TRUE;
    }

    return JNI_FALSE;
}

/*
 * Used by either ThreadReferenceImpl.c: resume() or
 * VirtualMachineImpl.c: resume() to resume commandLoop().
 */
void
unblockCommandLoop(void) {
    debugMonitorEnter(blockCommandLoopLock);
    blockCommandLoop = JNI_FALSE;
    debugMonitorNotifyAll(blockCommandLoopLock);
    debugMonitorExit(blockCommandLoopLock);
}

/*
 * The event helper thread. Dequeues commands and processes them.
 */
static void JNICALL
commandLoop(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg)
{
    LOG_MISC(("Begin command loop thread"));

    while (JNI_TRUE) {
        HelperCommand *command = dequeueCommand();
        if (command != NULL) {
            /*
             * Setup for a potential doBlockCommand() call before calling
             * handleCommand() to prevent any races.
             */
            jboolean doBlock = needBlockCommandLoop(command);
            debugMonitorEnter(vmDeathLock);
            commandLoopEnteredVmDeathLock = JNI_TRUE;
            if (!gdata->vmDead) {
                log_debugee_location("commandLoop(): command being handled", NULL, NULL, 0);
                handleCommand(jni_env, command);
            }
            completeCommand(command);
            debugMonitorExit(vmDeathLock);
            commandLoopEnteredVmDeathLock = JNI_FALSE;
            /* if we just finished a suspend-all cmd, then we block here */
            if (doBlock) {
                doBlockCommandLoop();
            }
        }
    }
    /* This loop never ends, even as connections come and go with server=y */
}

void
eventHelper_initialize(jbyte sessionID)
{
    jvmtiStartFunction func;

    currentSessionID = sessionID;
    holdEvents = JNI_FALSE;
    commandQueue.head = NULL;
    commandQueue.tail = NULL;

    commandQueueLock = debugMonitorCreate("JDWP Event Helper Queue Monitor");
    commandCompleteLock = debugMonitorCreate("JDWP Event Helper Completion Monitor");
    blockCommandLoopLock = debugMonitorCreate("JDWP Event Block CommandLoop Monitor");
    vmDeathLock = debugMonitorCreate("JDWP VM_DEATH CommandLoop Monitor");

    /* Start the event handler thread */
    func = &commandLoop;
    (void)spawnNewThread(func, NULL, COMMAND_LOOP_THREAD_NAME);
}

void
eventHelper_reset(jbyte newSessionID)
{
    debugMonitorEnter(commandQueueLock);
    currentSessionID = newSessionID;
    holdEvents = JNI_FALSE;
    debugMonitorNotifyAll(commandQueueLock);
    debugMonitorExit(commandQueueLock);
    unblockCommandLoop();
}

/*
 * Provide a means for threadControl to ensure that crucial locks are not
 * held by suspended threads.
 */
void
eventHelper_lock(void)
{
    debugMonitorEnter(commandQueueLock);
    debugMonitorEnter(commandCompleteLock);
}

void
eventHelper_unlock(void)
{
    debugMonitorExit(commandCompleteLock);
    debugMonitorExit(commandQueueLock);
}

void commandLoop_exitVmDeathLockOnError()
{
    const char* MSG_BASE = "exitVmDeathLockOnError: error in JVMTI %s: %d\n";
    jthread cur_thread = NULL;
    jvmtiThreadInfo thread_info;
    jvmtiError err = JVMTI_ERROR_NONE;

    err = JVMTI_FUNC_PTR(gdata->jvmti, GetCurrentThread)
              (gdata->jvmti, &cur_thread);
    if (err != JVMTI_ERROR_NONE) {
        LOG_ERROR((MSG_BASE, "GetCurrentThread", err));
        return;
    }

    err = JVMTI_FUNC_PTR(gdata->jvmti, GetThreadInfo)
              (gdata->jvmti, cur_thread, &thread_info);
    if (err != JVMTI_ERROR_NONE) {
        LOG_ERROR((MSG_BASE, "GetThreadInfo", err));
        return;
    }
    if (strcmp(thread_info.name, COMMAND_LOOP_THREAD_NAME) != 0) {
        return;
    }
    if (commandLoopEnteredVmDeathLock == JNI_TRUE) {
        debugMonitorExit(vmDeathLock);
        commandLoopEnteredVmDeathLock = JNI_FALSE;
    }
}

void
commandLoop_sync(void)
{
    debugMonitorEnter(vmDeathLock);
    debugMonitorExit(vmDeathLock);
}

/* Change all references to global in the EventInfo struct */
static void
saveEventInfoRefs(JNIEnv *env, EventInfo *evinfo)
{
    jthread *pthread;
    jclass *pclazz;
    jobject *pobject;
    jthread thread;
    jclass clazz;
    jobject object;
    char sig;

    JNI_FUNC_PTR(env,ExceptionClear)(env);

    if ( evinfo->thread != NULL ) {
        pthread = &(evinfo->thread);
        thread = *pthread;
        *pthread = NULL;
        saveGlobalRef(env, thread, pthread);
    }
    if ( evinfo->clazz != NULL ) {
        pclazz = &(evinfo->clazz);
        clazz = *pclazz;
        *pclazz = NULL;
        saveGlobalRef(env, clazz, pclazz);
    }
    if ( evinfo->object != NULL ) {
        pobject = &(evinfo->object);
        object = *pobject;
        *pobject = NULL;
        saveGlobalRef(env, object, pobject);
    }

    switch (evinfo->ei) {
        case EI_FIELD_MODIFICATION:
            if ( evinfo->u.field_modification.field_clazz != NULL ) {
                pclazz = &(evinfo->u.field_modification.field_clazz);
                clazz = *pclazz;
                *pclazz = NULL;
                saveGlobalRef(env, clazz, pclazz);
            }
            sig = evinfo->u.field_modification.signature_type;
            if (isReferenceTag(sig)) {
                if ( evinfo->u.field_modification.new_value.l != NULL ) {
                    pobject = &(evinfo->u.field_modification.new_value.l);
                    object = *pobject;
                    *pobject = NULL;
                    saveGlobalRef(env, object, pobject);
                }
            }
            break;
        case EI_FIELD_ACCESS:
            if ( evinfo->u.field_access.field_clazz != NULL ) {
                pclazz = &(evinfo->u.field_access.field_clazz);
                clazz = *pclazz;
                *pclazz = NULL;
                saveGlobalRef(env, clazz, pclazz);
            }
            break;
        case EI_EXCEPTION:
            if ( evinfo->u.exception.catch_clazz != NULL ) {
                pclazz = &(evinfo->u.exception.catch_clazz);
                clazz = *pclazz;
                *pclazz = NULL;
                saveGlobalRef(env, clazz, pclazz);
            }
            break;
        default:
            break;
    }

    if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
        EXIT_ERROR(AGENT_ERROR_INVALID_EVENT_TYPE,"ExceptionOccurred");
    }
}

static void
tossEventInfoRefs(JNIEnv *env, EventInfo *evinfo)
{
    char sig;
    if ( evinfo->thread != NULL ) {
        tossGlobalRef(env, &(evinfo->thread));
    }
    if ( evinfo->clazz != NULL ) {
        tossGlobalRef(env, &(evinfo->clazz));
    }
    if ( evinfo->object != NULL ) {
        tossGlobalRef(env, &(evinfo->object));
    }
    switch (evinfo->ei) {
        case EI_FIELD_MODIFICATION:
            if ( evinfo->u.field_modification.field_clazz != NULL ) {
                tossGlobalRef(env, &(evinfo->u.field_modification.field_clazz));
            }
            sig = evinfo->u.field_modification.signature_type;
            if (isReferenceTag(sig)) {
                if ( evinfo->u.field_modification.new_value.l != NULL ) {
                    tossGlobalRef(env, &(evinfo->u.field_modification.new_value.l));
                }
            }
            break;
        case EI_FIELD_ACCESS:
            if ( evinfo->u.field_access.field_clazz != NULL ) {
                tossGlobalRef(env, &(evinfo->u.field_access.field_clazz));
            }
            break;
        case EI_EXCEPTION:
            if ( evinfo->u.exception.catch_clazz != NULL ) {
                tossGlobalRef(env, &(evinfo->u.exception.catch_clazz));
            }
            break;
        default:
            break;
    }
}

struct bag *
eventHelper_createEventBag(void)
{
    return bagCreateBag(sizeof(CommandSingle), 5 /* events */ );
}

/* Return the combined suspend policy for the event set
 */
static jboolean
enumForCombinedSuspendPolicy(void *cv, void *arg)
{
    CommandSingle *command = cv;
    jbyte thisPolicy;
    jbyte *policy = arg;

    switch(command->singleKind) {
        case COMMAND_SINGLE_EVENT:
            thisPolicy = command->u.eventCommand.suspendPolicy;
            break;
        case COMMAND_SINGLE_FRAME_EVENT:
            thisPolicy = command->u.frameEventCommand.suspendPolicy;
            break;
        default:
            thisPolicy = JDWP_SUSPEND_POLICY(NONE);
    }
    /* Expand running policy value if this policy demands it */
    if (*policy == JDWP_SUSPEND_POLICY(NONE)) {
        *policy = thisPolicy;
    } else if (*policy == JDWP_SUSPEND_POLICY(EVENT_THREAD)) {
        *policy = (thisPolicy == JDWP_SUSPEND_POLICY(ALL))?
                        thisPolicy : *policy;
    }

    /* Short circuit if we reached maximal suspend policy */
    if (*policy == JDWP_SUSPEND_POLICY(ALL)) {
        return JNI_FALSE;
    } else {
        return JNI_TRUE;
    }
}

/* Determine whether we are reporting VM death
 */
static jboolean
enumForVMDeath(void *cv, void *arg)
{
    CommandSingle *command = cv;
    jboolean *reportingVMDeath = arg;

    if (command->singleKind == COMMAND_SINGLE_EVENT) {
        if (command->u.eventCommand.info.ei == EI_VM_DEATH) {
            *reportingVMDeath = JNI_TRUE;
            return JNI_FALSE;
        }
    }
    return JNI_TRUE;
}

struct singleTracker {
    ReportEventCompositeCommand *recc;
    int index;
};

static jboolean
enumForCopyingSingles(void *command, void *tv)
{
    struct singleTracker *tracker = (struct singleTracker *)tv;
    (void)memcpy(&tracker->recc->singleCommand[tracker->index++],
           command,
           sizeof(CommandSingle));
    return JNI_TRUE;
}

jbyte
eventHelper_reportEvents(jbyte sessionID, struct bag *eventBag)
{
    int size = bagSize(eventBag);
    jbyte suspendPolicy = JDWP_SUSPEND_POLICY(NONE);
    jboolean reportingVMDeath = JNI_FALSE;
    jboolean wait;
    int command_size;

    HelperCommand *command;
    ReportEventCompositeCommand *recc;
    struct singleTracker tracker;

    if (size == 0) {
        return suspendPolicy;
    }
    (void)bagEnumerateOver(eventBag, enumForCombinedSuspendPolicy, &suspendPolicy);
    (void)bagEnumerateOver(eventBag, enumForVMDeath, &reportingVMDeath);

    /*LINTED*/
    command_size = (int)(sizeof(HelperCommand) +
                         sizeof(CommandSingle)*(size-1));
    command = jvmtiAllocate(command_size);
    (void)memset(command, 0, command_size);
    command->commandKind = COMMAND_REPORT_EVENT_COMPOSITE;
    command->sessionID = sessionID;
    recc = &command->u.reportEventComposite;
    recc->suspendPolicy = suspendPolicy;
    recc->eventCount = size;
    tracker.recc = recc;
    tracker.index = 0;
    (void)bagEnumerateOver(eventBag, enumForCopyingSingles, &tracker);

    /*
     * We must wait if this thread (the event thread) is to be
     * suspended or if the VM is about to die. (Waiting in the latter
     * case ensures that we get the event out before the process dies.)
     */
    wait = (jboolean)((suspendPolicy != JDWP_SUSPEND_POLICY(NONE)) ||
                      reportingVMDeath);
    enqueueCommand(command, wait, reportingVMDeath);
    return suspendPolicy;
}

void
eventHelper_recordEvent(EventInfo *evinfo, jint id, jbyte suspendPolicy,
                        struct bag *eventBag)
{
    JNIEnv *env = getEnv();
    CommandSingle *command = bagAdd(eventBag);
    if (command == NULL) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"bagAdd(eventBag)");
    }

    command->singleKind = COMMAND_SINGLE_EVENT;
    command->u.eventCommand.suspendPolicy = suspendPolicy;
    command->u.eventCommand.id = id;

    /*
     * Copy the event into the command so that it can be used
     * asynchronously by the event helper thread.
     */
    (void)memcpy(&command->u.eventCommand.info, evinfo, sizeof(*evinfo));
    saveEventInfoRefs(env, &command->u.eventCommand.info);
}

void
eventHelper_recordClassUnload(jint id, char *signature, struct bag *eventBag)
{
    CommandSingle *command = bagAdd(eventBag);
    if (command == NULL) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"bagAdd(eventBag)");
    }
    command->singleKind = COMMAND_SINGLE_UNLOAD;
    command->u.unloadCommand.id = id;
    command->u.unloadCommand.classSignature = signature;
}

void
eventHelper_recordFrameEvent(jint id, jbyte suspendPolicy, EventIndex ei,
                             jthread thread, jclass clazz,
                             jmethodID method, jlocation location,
                             int needReturnValue,
                             jvalue returnValue,
                             struct bag *eventBag)
{
    JNIEnv *env = getEnv();
    FrameEventCommandSingle *frameCommand;
    CommandSingle *command = bagAdd(eventBag);
    jvmtiError err = JVMTI_ERROR_NONE;
    if (command == NULL) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"bagAdd(eventBag)");
    }

    command->singleKind = COMMAND_SINGLE_FRAME_EVENT;
    frameCommand = &command->u.frameEventCommand;
    frameCommand->suspendPolicy = suspendPolicy;
    frameCommand->id = id;
    frameCommand->ei = ei;
    saveGlobalRef(env, thread, &(frameCommand->thread));
    saveGlobalRef(env, clazz, &(frameCommand->clazz));
    frameCommand->method = method;
    frameCommand->location = location;
    if (needReturnValue) {
        err = methodReturnType(method, &frameCommand->typeKey);
        JDI_ASSERT(err == JVMTI_ERROR_NONE);

        /*
         * V or B C D F I J S Z L <classname> ;    [ ComponentType
         */
        if (isReferenceTag(frameCommand->typeKey) &&
            returnValue.l != NULL) {
            saveGlobalRef(env, returnValue.l, &(frameCommand->returnValue.l));
        } else {
            frameCommand->returnValue = returnValue;
        }
    } else {
      /* This is not a JDWP METHOD_EXIT_WITH_RETURN_VALUE request,
       * so signal this by setting typeKey = 0 which is not
       * a legal typekey.
       */
       frameCommand->typeKey = 0;
    }
}

void
eventHelper_reportInvokeDone(jbyte sessionID, jthread thread)
{
    JNIEnv *env = getEnv();
    HelperCommand *command = jvmtiAllocate(sizeof(*command));
    if (command == NULL) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"HelperCommand");
    }
    (void)memset(command, 0, sizeof(*command));
    command->commandKind = COMMAND_REPORT_INVOKE_DONE;
    command->sessionID = sessionID;
    saveGlobalRef(env, thread, &(command->u.reportInvokeDone.thread));
    enqueueCommand(command, JNI_TRUE, JNI_FALSE);
}

/*
 * This, currently, cannot go through the normal event handling code
 * because the JVMTI event does not contain a thread.
 */
void
eventHelper_reportVMInit(JNIEnv *env, jbyte sessionID, jthread thread, jbyte suspendPolicy)
{
    HelperCommand *command = jvmtiAllocate(sizeof(*command));
    if (command == NULL) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"HelperCommmand");
    }
    (void)memset(command, 0, sizeof(*command));
    command->commandKind = COMMAND_REPORT_VM_INIT;
    command->sessionID = sessionID;
    saveGlobalRef(env, thread, &(command->u.reportVMInit.thread));
    command->u.reportVMInit.suspendPolicy = suspendPolicy;
    enqueueCommand(command, JNI_TRUE, JNI_FALSE);
}

void
eventHelper_suspendThread(jbyte sessionID, jthread thread)
{
    JNIEnv *env = getEnv();
    HelperCommand *command = jvmtiAllocate(sizeof(*command));
    if (command == NULL) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"HelperCommmand");
    }
    (void)memset(command, 0, sizeof(*command));
    command->commandKind = COMMAND_SUSPEND_THREAD;
    command->sessionID = sessionID;
    saveGlobalRef(env, thread, &(command->u.suspendThread.thread));
    enqueueCommand(command, JNI_TRUE, JNI_FALSE);
}
