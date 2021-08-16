/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>

#include "sysShmem.h"
#include "shmemBase.h"
#include "jdwpTransport.h"  /* for Packet, TransportCallback */

#if defined(_WIN32)
  #define PRId64 "I64d"
#endif

#define MIN(x,y) ((x)<(y)?(x):(y))

/*
 * This is the base shared memory transport implementation that is used
 * by both front-end transports (through com.sun.tools.jdi) and
 * back-end transports (through JDWP_OnLoad and the function tables
 * it requires). It supports multiple connections for the benefit of the
 * front-end client; the back end interface assumes only a single connection.
 */

#define MAX_IPC_PREFIX 50   /* user-specified or generated name for */
                            /* shared memory seg and prefix for other IPC */
#define MAX_IPC_SUFFIX 25   /* suffix to shmem name for other IPC names */
#define MAX_IPC_NAME   (MAX_IPC_PREFIX + MAX_IPC_SUFFIX)

#define MAX_GENERATION_RETRIES 20
#define SHARED_BUFFER_SIZE 5000

#define CHECK_ERROR(expr) do { \
                              jint error = (expr); \
                              if (error != SYS_OK) { \
                                  setLastError(error); \
                                  return error; \
                              } \
                          } while (0)

#define ENTER_CONNECTION(connection) \
        do { \
            InterlockedIncrement(&connection->refcount); \
            if (IS_STATE_CLOSED(connection->state)) { \
                setLastErrorMsg("stream closed"); \
                InterlockedDecrement(&connection->refcount); \
                return SYS_ERR; \
            } \
        } while (0)

#define LEAVE_CONNECTION(connection) \
        do { \
            InterlockedDecrement(&connection->refcount); \
        } while (0)

/*
 * The following assertions should hold anytime the stream's mutex is not held
 */
#define STREAM_INVARIANT(stream) \
        do { \
            SHMEM_ASSERT((stream->shared->readOffset < SHARED_BUFFER_SIZE) \
                         && (stream->shared->readOffset >= 0)); \
            SHMEM_ASSERT((stream->shared->writeOffset < SHARED_BUFFER_SIZE) \
                         && (stream->shared->writeOffset >= 0)); \
        } while (0)

/*
 * Transports are duplex, so carve the shared memory into "streams",
 * one used to send from client to server, the other vice versa.
 */
typedef struct SharedMemoryListener {
    char mutexName[MAX_IPC_NAME];
    char acceptEventName[MAX_IPC_NAME];
    char attachEventName[MAX_IPC_NAME];
    jboolean isListening;
    jboolean isAccepted;
    jlong acceptingPID;
    jlong attachingPID;
} SharedListener;

typedef struct SharedMemoryTransport {
    char name[MAX_IPC_PREFIX];
    sys_ipmutex_t mutex;
    sys_event_t acceptEvent;
    sys_event_t attachEvent;
    sys_shmem_t sharedMemory;
    SharedListener *shared;
} SharedMemoryTransport;

/*
 * Access must be syncronized.  Holds one shared
 * memory buffer and its state.
 */
typedef struct SharedStream {
    char mutexName[MAX_IPC_NAME];
    char hasDataEventName[MAX_IPC_NAME];
    char hasSpaceEventName[MAX_IPC_NAME];
    int readOffset;
    int writeOffset;
    jboolean isFull;
    jbyte buffer[SHARED_BUFFER_SIZE];
} SharedStream;

/*
 * The two shared streams: client to server and
 * server to client.
 */
typedef struct SharedMemory {
    SharedStream toClient;
    SharedStream toServer;
} SharedMemory;

/*
 * Local (to process) access to the shared memory
 * stream.  access to hasData and hasSpace synchronized
 * by OS.
 */
typedef struct Stream {
    sys_ipmutex_t mutex;
    sys_event_t hasData;
    sys_event_t hasSpace;
    SharedStream *shared;
    jint state;
} Stream;

/*
 * Values for Stream.state field above.
 */
#define STATE_CLOSED 0xDEAD
#define STATE_OPEN   (STATE_CLOSED -1)
/*
 * State checking macro. We compare against the STATE_OPEN value so
 * that STATE_CLOSED and any other value will be considered closed.
 * This catches a freed Stream as long as the memory page is still
 * valid. If the memory page is gone, then there is little that we
 * can do.
 */
#define IS_STATE_CLOSED(state) (state != STATE_OPEN)


typedef struct SharedMemoryConnection {
    char name[MAX_IPC_NAME];
    SharedMemory *shared;
    sys_shmem_t sharedMemory;
    Stream incoming;
    Stream outgoing;
    sys_process_t otherProcess;
    sys_event_t shutdown;           /* signalled to indicate shutdown */
    volatile DWORD32 refcount;
    jint state;
} SharedMemoryConnection;

static jdwpTransportCallback *callback;
static JavaVM *jvm;
static int tlsIndex;

typedef jint (*CreateFunc)(char *name, void *arg);

/*
 * Set the per-thread error message (if not already set)
 */
static void
setLastErrorMsg(char *newmsg) {
    char *msg;

    msg = (char *)sysTlsGet(tlsIndex);
    if (msg == NULL) {
        msg = (*callback->alloc)((int)strlen(newmsg)+1);
        if (msg != NULL) {
           strcpy(msg, newmsg);
        }
        sysTlsPut(tlsIndex, (void *)msg);
    }
}

/*
 * Clear last per-thread error message
 */
static void
clearLastError() {
    char* msg = (char *)sysTlsGet(tlsIndex);
    if (msg != NULL) {
        (*callback->free)(msg);
        sysTlsPut(tlsIndex, NULL);
    }
}

/*
 * Set the per-thread error message to the textual representation
 * of the last system error (if not already set)
 */
static void
setLastError(jint error) {
    char buf[128];

    switch (error) {
        case SYS_OK      : return;      /* no-op */
        case SYS_DIED    : strcpy(buf, "Other process terminated"); break;
        case SYS_TIMEOUT : strcpy(buf, "Timed out"); break;
        default          : sysGetLastError(buf, sizeof(buf));
    }
    setLastErrorMsg(buf);
}

jint
shmemBase_initialize(JavaVM *vm, jdwpTransportCallback *cbPtr)
{
    jvm = vm;
    callback = cbPtr;
    tlsIndex = sysTlsAlloc();
    return SYS_OK;
}

static jint
createWithGeneratedName(char *prefix, char *nameBuffer, CreateFunc func, void *arg)
{
    jint error;
    jint i = 0;

    do {
        strcpy(nameBuffer, prefix);
        if (i > 0) {
            char buf[10];
            sprintf(buf, ".%d", i+1);
            strcat(nameBuffer, buf);
        }
        error = func(nameBuffer, arg);
        i++;
    } while ((error == SYS_INUSE) && (i < MAX_GENERATION_RETRIES));

    if (error != SYS_OK) {
        setLastError(error);
    }

    return error;
}

typedef struct SharedMemoryArg {
    jint size;
    sys_shmem_t memory;
    void *start;
} SharedMemoryArg;

static jint
createSharedMem(char *name, void *ptr)
{
    SharedMemoryArg *arg = ptr;
    return sysSharedMemCreate(name, arg->size, &arg->memory, &arg->start);
}

static jint
createMutex(char *name, void *arg)
{
    sys_ipmutex_t *retArg = arg;
    return sysIPMutexCreate(name, retArg);
}

/*
 * Creates named or unnamed event that is automatically reset
 * (in other words, no need to reset event after it has signalled
 * a thread).
 */
static jint
createEvent(char *name, void *arg)
{
    sys_event_t *retArg = arg;
    return sysEventCreate(name, retArg, JNI_FALSE);
}

#define ADD_OFFSET(o1, o2) ((o1 + o2) % SHARED_BUFFER_SIZE)
#define FULL(stream) (stream->shared->isFull)
#define EMPTY(stream) ((stream->shared->writeOffset == stream->shared->readOffset) \
                       && !stream->shared->isFull)

static jint
leaveMutex(Stream *stream)
{
    return sysIPMutexExit(stream->mutex);
}

/* enter the stream's mutex and (optionally) check for a closed stream */
static jint
enterMutex(Stream *stream, sys_event_t event)
{
    jint ret = sysIPMutexEnter(stream->mutex, event);
    if (ret != SYS_OK) {
        if (IS_STATE_CLOSED(stream->state)) {
            setLastErrorMsg("stream closed");
        }
        return ret;
    }
    if (IS_STATE_CLOSED(stream->state)) {
        setLastErrorMsg("stream closed");
        (void)leaveMutex(stream);
        return SYS_ERR;
    }
    return SYS_OK;
}

/*
 * Enter/exit with stream mutex held.
 * On error, does not hold the stream mutex.
 */
static jint
waitForSpace(SharedMemoryConnection *connection, Stream *stream)
{
    jint error = SYS_OK;

    /* Assumes mutex is held on call */
    while ((error == SYS_OK) && FULL(stream)) {
        CHECK_ERROR(leaveMutex(stream));
        error = sysEventWait(connection->otherProcess, stream->hasSpace, 0);
        if (error == SYS_OK) {
            CHECK_ERROR(enterMutex(stream, connection->shutdown));
        } else {
            setLastError(error);
        }
    }
    return error;
}

static jint
signalSpace(Stream *stream)
{
    return sysEventSignal(stream->hasSpace);
}

/*
 * Enter/exit with stream mutex held.
 * On error, does not hold the stream mutex.
 */
static jint
waitForData(SharedMemoryConnection *connection, Stream *stream)
{
    jint error = SYS_OK;

    /* Assumes mutex is held on call */
    while ((error == SYS_OK) && EMPTY(stream)) {
        CHECK_ERROR(leaveMutex(stream));
        error = sysEventWait(connection->otherProcess, stream->hasData, 0);
        if (error == SYS_OK) {
            CHECK_ERROR(enterMutex(stream, connection->shutdown));
        } else {
            setLastError(error);
        }
    }
    return error;
}

static jint
signalData(Stream *stream)
{
    return sysEventSignal(stream->hasData);
}


static jint
closeStream(Stream *stream, jboolean linger, volatile DWORD32 *refcount)
{
    /*
     * Lock stream during close - ignore shutdown event as we are
     * closing down and shutdown should be signalled.
     */
    CHECK_ERROR(enterMutex(stream, NULL));

    /* mark the stream as closed */
    stream->state = STATE_CLOSED;
    /* wake up waitForData() if it is in sysEventWait() */
    sysEventSignal(stream->hasData);
    /* wake up waitForSpace() if it is in sysEventWait() */
    sysEventSignal(stream->hasSpace);

    /*
     * If linger requested then give the stream a few seconds to
     * drain before closing it.
     */
    if (linger) {
        int attempts = 10;
        while (!EMPTY(stream) && attempts>0) {
            CHECK_ERROR(leaveMutex(stream));
            sysSleep(200);
            CHECK_ERROR(enterMutex(stream, NULL));
            attempts--;
        }
    }

    CHECK_ERROR(leaveMutex(stream));

    /* Attempt to close resources */
    int attempts = 10;
    while (attempts > 0) {
        if (*refcount == 0) {
            sysEventClose(stream->hasData);
            sysEventClose(stream->hasSpace);
            sysIPMutexClose(stream->mutex);
            return SYS_OK;
        }
        sysSleep(200);
        attempts--;
    }
    return SYS_ERR;
}

/*
 * Server creates stream.
 */
static int
createStream(char *name, Stream *stream)
{
    jint error;
    char objectName[MAX_IPC_NAME];

    sprintf(objectName, "%s.mutex", name);
    error = createWithGeneratedName(objectName, stream->shared->mutexName,
                                    createMutex, &stream->mutex);
    if (error != SYS_OK) {
        return error;
    }

    sprintf(objectName, "%s.hasData", name);
    error = createWithGeneratedName(objectName, stream->shared->hasDataEventName,
                                    createEvent, &stream->hasData);
    if (error != SYS_OK) {
        sysIPMutexClose(stream->mutex);
        return error;
    }

    sprintf(objectName, "%s.hasSpace", name);
    error = createWithGeneratedName(objectName, stream->shared->hasSpaceEventName,
                                    createEvent, &stream->hasSpace);
    if (error != SYS_OK) {
        sysIPMutexClose(stream->mutex);
        sysEventClose(stream->hasData);
        return error;
    }

    stream->shared->readOffset = 0;
    stream->shared->writeOffset = 0;
    stream->shared->isFull = JNI_FALSE;
    stream->state = STATE_OPEN;
    return SYS_OK;
}


/*
 * Initialization for the stream opened by the other process
 */
static int
openStream(Stream *stream)
{
    jint error;

    CHECK_ERROR(sysIPMutexOpen(stream->shared->mutexName, &stream->mutex));

    error = sysEventOpen(stream->shared->hasDataEventName,
                             &stream->hasData);
    if (error != SYS_OK) {
        setLastError(error);
        sysIPMutexClose(stream->mutex);
        return error;
    }

    error = sysEventOpen(stream->shared->hasSpaceEventName,
                             &stream->hasSpace);
    if (error != SYS_OK) {
        setLastError(error);
        sysIPMutexClose(stream->mutex);
        sysEventClose(stream->hasData);
        return error;
    }

    stream->state = STATE_OPEN;

    return SYS_OK;
}

/********************************************************************/

static SharedMemoryConnection *
allocConnection(void)
{
    /*
     * TO DO: Track all allocated connections for clean shutdown?
     */
    SharedMemoryConnection *conn = (*callback->alloc)(sizeof(SharedMemoryConnection));
    if (conn != NULL) {
        memset(conn, 0, sizeof(SharedMemoryConnection));
    }
    conn->state = STATE_OPEN;
    return conn;
}

static void
freeConnection(SharedMemoryConnection *connection)
{
    (*callback->free)(connection);
}

static void
closeConnection(SharedMemoryConnection *connection)
{
    /* mark the connection as closed */
    connection->state = STATE_CLOSED;

    /*
     * Signal all threads accessing this connection that we are
     * shutting down.
     */
    if (connection->shutdown) {
        sysEventSignal(connection->shutdown);
    }

    Stream * stream = &connection->outgoing;
    if (stream->state == STATE_OPEN) {
        (void)closeStream(stream, JNI_TRUE, &connection->refcount);
    }
    stream = &connection->incoming;
    if (stream->state == STATE_OPEN) {
        (void)closeStream(stream, JNI_FALSE, &connection->refcount);
    }

    if (connection->refcount == 0) {
        if (connection->sharedMemory) {
            sysSharedMemClose(connection->sharedMemory, connection->shared);
        }
        if (connection->otherProcess) {
            sysProcessClose(connection->otherProcess);
        }
        if (connection->shutdown) {
            sysEventClose(connection->shutdown);
        }
    }
}


/*
 * For client: connect to the shared memory.  Open incoming and
 * outgoing streams.
 */
static jint
openConnection(SharedMemoryTransport *transport, jlong otherPID,
               SharedMemoryConnection **connectionPtr)
{
    jint error;

    SharedMemoryConnection *connection = allocConnection();
    if (connection == NULL) {
        return SYS_NOMEM;
    }

    sprintf(connection->name, "%s.%" PRId64, transport->name, sysProcessGetID());
    error = sysSharedMemOpen(connection->name, &connection->sharedMemory,
                             &connection->shared);
    if (error != SYS_OK) {
        freeConnection(connection);
        return error;
    }

    /* This process is the client */
    connection->incoming.shared = &connection->shared->toClient;
    connection->outgoing.shared = &connection->shared->toServer;

    error = openStream(&connection->incoming);
    if (error != SYS_OK) {
        closeConnection(connection);
        freeConnection(connection);
        return error;
    }

    error = openStream(&connection->outgoing);
    if (error != SYS_OK) {
        closeConnection(connection);
        freeConnection(connection);
        return error;
    }

    error = sysProcessOpen(otherPID, &connection->otherProcess);
    if (error != SYS_OK) {
        setLastError(error);
        closeConnection(connection);
        freeConnection(connection);
        return error;
    }

    /*
     * Create an event that signals that the connection is shutting
     * down. The event is unnamed as it's process local, and is
     * manually reset (so that signalling the event will signal
     * all threads waiting on it).
     */
    error = sysEventCreate(NULL, &connection->shutdown, JNI_TRUE);
    if (error != SYS_OK) {
        setLastError(error);
        closeConnection(connection);
        freeConnection(connection);
        return error;
    }

    *connectionPtr = connection;
    return SYS_OK;
}

/*
 * For server: create the shared memory.  Create incoming and
 * outgoing streams.
 */
static jint
createConnection(SharedMemoryTransport *transport, jlong otherPID,
                 SharedMemoryConnection **connectionPtr)
{
    jint error;
    char streamName[MAX_IPC_NAME];

    SharedMemoryConnection *connection = allocConnection();
    if (connection == NULL) {
        return SYS_NOMEM;
    }

    sprintf(connection->name, "%s.%" PRId64, transport->name, otherPID);
    error = sysSharedMemCreate(connection->name, sizeof(SharedMemory),
                               &connection->sharedMemory, &connection->shared);
    if (error != SYS_OK) {
        freeConnection(connection);
        return error;
    }

    memset(connection->shared, 0, sizeof(SharedMemory));

    /* This process is the server */
    connection->incoming.shared = &connection->shared->toServer;
    connection->outgoing.shared = &connection->shared->toClient;

    strcpy(streamName, connection->name);
    strcat(streamName, ".ctos");
    error = createStream(streamName, &connection->incoming);
    if (error != SYS_OK) {
        closeConnection(connection);
        freeConnection(connection);
        return error;
    }

    strcpy(streamName, connection->name);
    strcat(streamName, ".stoc");
    error = createStream(streamName, &connection->outgoing);
    if (error != SYS_OK) {
        closeConnection(connection);
        freeConnection(connection);
        return error;
    }

    error = sysProcessOpen(otherPID, &connection->otherProcess);
    if (error != SYS_OK) {
        setLastError(error);
        closeConnection(connection);
        freeConnection(connection);
        return error;
    }

    /*
     * Create an event that signals that the connection is shutting
     * down. The event is unnamed as it's process local, and is
     * manually reset (so that a signalling the event will signal
     * all threads waiting on it).
     */
    error = sysEventCreate(NULL, &connection->shutdown, JNI_TRUE);
    if (error != SYS_OK) {
        setLastError(error);
        closeConnection(connection);
        freeConnection(connection);
        return error;
    }

    *connectionPtr = connection;
    return SYS_OK;
}

/********************************************************************/

static SharedMemoryTransport *
allocTransport(void)
{
    /*
     * TO DO: Track all allocated transports for clean shutdown?
     */
    return (*callback->alloc)(sizeof(SharedMemoryTransport));
}

static void
freeTransport(SharedMemoryTransport *transport)
{
    (*callback->free)(transport);
}

static void
closeTransport(SharedMemoryTransport *transport)
{
    sysIPMutexClose(transport->mutex);
    sysEventClose(transport->acceptEvent);
    sysEventClose(transport->attachEvent);
    sysSharedMemClose(transport->sharedMemory, transport->shared);
    freeTransport(transport);
}

static int
openTransport(const char *address, SharedMemoryTransport **transportPtr)
{
    jint error;
    SharedMemoryTransport *transport;

    transport = allocTransport();
    if (transport == NULL) {
        return SYS_NOMEM;
    }
    memset(transport, 0, sizeof(*transport));

    if (strlen(address) >= MAX_IPC_PREFIX) {
        char buf[128];
        sprintf(buf, "Error: address strings longer than %d characters are invalid\n", MAX_IPC_PREFIX);
        setLastErrorMsg(buf);
        closeTransport(transport);
        return SYS_ERR;
    }

    error = sysSharedMemOpen(address, &transport->sharedMemory, &transport->shared);
    if (error != SYS_OK) {
        setLastError(error);
        closeTransport(transport);
        return error;
    }
    strcpy(transport->name, address);

    error = sysIPMutexOpen(transport->shared->mutexName, &transport->mutex);
    if (error != SYS_OK) {
        setLastError(error);
        closeTransport(transport);
        return error;
    }

    error = sysEventOpen(transport->shared->acceptEventName,
                             &transport->acceptEvent);
    if (error != SYS_OK) {
        setLastError(error);
        closeTransport(transport);
        return error;
    }

    error = sysEventOpen(transport->shared->attachEventName,
                             &transport->attachEvent);
    if (error != SYS_OK) {
        setLastError(error);
        closeTransport(transport);
        return error;
    }

    *transportPtr = transport;
    return SYS_OK;
}

static jint
createTransport(const char *address, SharedMemoryTransport **transportPtr)
{
    SharedMemoryTransport *transport;
    jint error;
    char objectName[MAX_IPC_NAME];

    transport = allocTransport();
    if (transport == NULL) {
        return SYS_NOMEM;
    }
    memset(transport, 0, sizeof(*transport));

    if ((address == NULL) || (address[0] == '\0')) {
        SharedMemoryArg arg;
        arg.size = sizeof(SharedListener);
        error = createWithGeneratedName("javadebug", transport->name,
                                        createSharedMem, &arg);
        transport->shared = arg.start;
        transport->sharedMemory = arg.memory;
    } else {
        if (strlen(address) >= MAX_IPC_PREFIX) {
            char buf[128];
            sprintf(buf, "Error: address strings longer than %d characters are invalid\n", MAX_IPC_PREFIX);
            setLastErrorMsg(buf);
            closeTransport(transport);
            return SYS_ERR;
        }
        strcpy(transport->name, address);
        error = sysSharedMemCreate(address, sizeof(SharedListener),
                                   &transport->sharedMemory, &transport->shared);
    }
    if (error != SYS_OK) {
        setLastError(error);
        closeTransport(transport);
        return error;
    }

    memset(transport->shared, 0, sizeof(SharedListener));
    transport->shared->acceptingPID = sysProcessGetID();

    sprintf(objectName, "%s.mutex", transport->name);
    error = createWithGeneratedName(objectName, transport->shared->mutexName,
                                    createMutex, &transport->mutex);
    if (error != SYS_OK) {
        closeTransport(transport);
        return error;
    }

    sprintf(objectName, "%s.accept", transport->name);
    error = createWithGeneratedName(objectName, transport->shared->acceptEventName,
                                    createEvent, &transport->acceptEvent);
    if (error != SYS_OK) {
        closeTransport(transport);
        return error;
    }

    sprintf(objectName, "%s.attach", transport->name);
    error = createWithGeneratedName(objectName, transport->shared->attachEventName,
                                    createEvent, &transport->attachEvent);
    if (error != SYS_OK) {
        closeTransport(transport);
        return error;
    }

    *transportPtr = transport;
    return SYS_OK;
}


jint
shmemBase_listen(const char *address, SharedMemoryTransport **transportPtr)
{
    int error;

    clearLastError();

    error = createTransport(address, transportPtr);
    if (error == SYS_OK) {
        (*transportPtr)->shared->isListening = JNI_TRUE;
    }
    return error;
}


jint
shmemBase_accept(SharedMemoryTransport *transport,
                 long timeout,
                 SharedMemoryConnection **connectionPtr)
{
    jint error;
    SharedMemoryConnection *connection;

    clearLastError();

    CHECK_ERROR(sysEventWait(NULL, transport->attachEvent, timeout));

    error = createConnection(transport, transport->shared->attachingPID,
                             &connection);
    if (error != SYS_OK) {
        /*
         * Reject the attacher
         */
        transport->shared->isAccepted = JNI_FALSE;
        sysEventSignal(transport->acceptEvent);

        return error;
    }

    transport->shared->isAccepted = JNI_TRUE;
    error = sysEventSignal(transport->acceptEvent);
    if (error != SYS_OK) {
        /*
         * No real point trying to reject it.
         */
        closeConnection(connection);
        freeConnection(connection);
        return error;
    }

    *connectionPtr = connection;
    return SYS_OK;
}

static jint
doAttach(SharedMemoryTransport *transport, long timeout)
{
    transport->shared->attachingPID = sysProcessGetID();
    CHECK_ERROR(sysEventSignal(transport->attachEvent));
    CHECK_ERROR(sysEventWait(NULL, transport->acceptEvent, timeout));
    return SYS_OK;
}

jint
shmemBase_attach(const char *addressString, long timeout, SharedMemoryConnection **connectionPtr)
{
    int error;
    SharedMemoryTransport *transport;
    jlong acceptingPID;

    clearLastError();

    error = openTransport(addressString, &transport);
    if (error != SYS_OK) {
        return error;
    }

    /* lock transport - no additional event to wait on as no connection yet */
    error = sysIPMutexEnter(transport->mutex, NULL);
    if (error != SYS_OK) {
        setLastError(error);
        closeTransport(transport);
        return error;
    }

    if (transport->shared->isListening) {
        error = doAttach(transport, timeout);
        if (error == SYS_OK) {
            acceptingPID = transport->shared->acceptingPID;
        }
    } else {
        /* Not listening: error */
        error = SYS_ERR;
    }

    sysIPMutexExit(transport->mutex);
    if (error != SYS_OK) {
        closeTransport(transport);
        return error;
    }

    error = openConnection(transport, acceptingPID, connectionPtr);

    closeTransport(transport);

    return error;
}




void
shmemBase_closeConnection(SharedMemoryConnection *connection)
{
    clearLastError();
    closeConnection(connection);
    /*
     * Ideally we should free the connection structure. However,
     * since the connection has already being published, other
     * threads may still be accessing it. In particular, refcount
     * and state fields could be accessed at any time even after
     * closing the connection. On Win32 this means we leak 140
     * bytes. This memory will be reclaimed at process exit.
     *
     * In general reference counting should exist externally to
     * the object being managed so that it can be freed. If we
     * want to free SharedMemoryConnection, one alternative could
     * be to define a new struct X and move all those fields there
     * except refcount and state. We would have a pointer to a
     * dynamically allocated X from SharedMemoryConnection. Then
     * if refcount is 0 we could also free X. This would leak
     * 12 bytes instead of 140.
     *
     * freeConnection(connection);
     *
     */
}

void
shmemBase_closeTransport(SharedMemoryTransport *transport)
{
    clearLastError();
    closeTransport(transport);
}

static jint
shmemBase_sendByte_internal(SharedMemoryConnection *connection, jbyte data)
{
    Stream *stream = &connection->outgoing;
    SharedStream *shared = stream->shared;
    int offset;

    clearLastError();

    CHECK_ERROR(enterMutex(stream, connection->shutdown));
    CHECK_ERROR(waitForSpace(connection, stream));
    SHMEM_ASSERT(!FULL(stream));
    offset = shared->writeOffset;
    shared->buffer[offset] = data;
    shared->writeOffset = ADD_OFFSET(offset, 1);
    shared->isFull = (shared->readOffset == shared->writeOffset);

    STREAM_INVARIANT(stream);
    CHECK_ERROR(leaveMutex(stream));

    CHECK_ERROR(signalData(stream));

    return SYS_OK;
}

jint
shmemBase_sendByte(SharedMemoryConnection *connection, jbyte data)
{
    ENTER_CONNECTION(connection);
    jint rc = shmemBase_sendByte_internal(connection, data);
    LEAVE_CONNECTION(connection);
    return rc;
}

static jint
shmemBase_receiveByte_internal(SharedMemoryConnection *connection, jbyte *data)
{
    Stream *stream = &connection->incoming;
    SharedStream *shared = stream->shared;
    int offset;

    clearLastError();

    CHECK_ERROR(enterMutex(stream, connection->shutdown));
    CHECK_ERROR(waitForData(connection, stream));
    SHMEM_ASSERT(!EMPTY(stream));
    offset = shared->readOffset;
    *data = shared->buffer[offset];
    shared->readOffset = ADD_OFFSET(offset, 1);
    shared->isFull = JNI_FALSE;

    STREAM_INVARIANT(stream);
    CHECK_ERROR(leaveMutex(stream));

    CHECK_ERROR(signalSpace(stream));

    return SYS_OK;
}

jint
shmemBase_receiveByte(SharedMemoryConnection *connection, jbyte *data)
{
    ENTER_CONNECTION(connection);
    jint rc = shmemBase_receiveByte_internal(connection, data);
    LEAVE_CONNECTION(connection);
    return rc;
}

static jint
sendBytes(SharedMemoryConnection *connection, const void *bytes, jint length)
{
    Stream *stream = &connection->outgoing;
    SharedStream *shared = stream->shared;
    jint fragmentStart;
    jint fragmentLength;
    jint index = 0;
    jint maxLength;

    clearLastError();

    CHECK_ERROR(enterMutex(stream, connection->shutdown));
    while (index < length) {
        CHECK_ERROR(waitForSpace(connection, stream));
        SHMEM_ASSERT(!FULL(stream));

        fragmentStart = shared->writeOffset;

        if (fragmentStart < shared->readOffset) {
            maxLength = shared->readOffset - fragmentStart;
        } else {
            maxLength = SHARED_BUFFER_SIZE - fragmentStart;
        }
        fragmentLength = MIN(maxLength, length - index);
        memcpy(shared->buffer + fragmentStart, (jbyte *)bytes + index, fragmentLength);
        shared->writeOffset = ADD_OFFSET(fragmentStart, fragmentLength);
        index += fragmentLength;

        shared->isFull = (shared->readOffset == shared->writeOffset);

        STREAM_INVARIANT(stream);
        CHECK_ERROR(signalData(stream));

    }
    CHECK_ERROR(leaveMutex(stream));

    return SYS_OK;
}


/*
 * Send packet header followed by data.
 */
static jint
shmemBase_sendPacket_internal(SharedMemoryConnection *connection, const jdwpPacket *packet)
{
    jint data_length;

    clearLastError();

    CHECK_ERROR(sendBytes(connection, &packet->type.cmd.id, sizeof(jint)));
    CHECK_ERROR(sendBytes(connection, &packet->type.cmd.flags, sizeof(jbyte)));

    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        CHECK_ERROR(sendBytes(connection, &packet->type.reply.errorCode, sizeof(jshort)));
    } else {
        CHECK_ERROR(sendBytes(connection, &packet->type.cmd.cmdSet, sizeof(jbyte)));
        CHECK_ERROR(sendBytes(connection, &packet->type.cmd.cmd, sizeof(jbyte)));
    }

    data_length = packet->type.cmd.len - JDWP_HEADER_SIZE;
    SHMEM_GUARANTEE(data_length >= 0);
    CHECK_ERROR(sendBytes(connection, &data_length, sizeof(jint)));

    if (data_length > 0) {
        CHECK_ERROR(sendBytes(connection, packet->type.cmd.data, data_length));
    }

    return SYS_OK;
}

jint
shmemBase_sendPacket(SharedMemoryConnection *connection, const jdwpPacket *packet)
{
    ENTER_CONNECTION(connection);
    jint rc = shmemBase_sendPacket_internal(connection, packet);
    LEAVE_CONNECTION(connection);
    return rc;
}

static jint
receiveBytes(SharedMemoryConnection *connection, void *bytes, jint length)
{
    Stream *stream = &connection->incoming;
    SharedStream *shared = stream->shared;
    jint fragmentStart;
    jint fragmentLength;
    jint index = 0;
    jint maxLength;

    clearLastError();

    CHECK_ERROR(enterMutex(stream, connection->shutdown));
    while (index < length) {
        CHECK_ERROR(waitForData(connection, stream));
        SHMEM_ASSERT(!EMPTY(stream));

        fragmentStart = shared->readOffset;
        if (fragmentStart < shared->writeOffset) {
            maxLength = shared->writeOffset - fragmentStart;
        } else {
            maxLength = SHARED_BUFFER_SIZE - fragmentStart;
        }
        fragmentLength = MIN(maxLength, length - index);
        memcpy((jbyte *)bytes + index, shared->buffer + fragmentStart, fragmentLength);
        shared->readOffset = ADD_OFFSET(fragmentStart, fragmentLength);
        index += fragmentLength;

        shared->isFull = JNI_FALSE;

        STREAM_INVARIANT(stream);
        CHECK_ERROR(signalSpace(stream));
    }
    CHECK_ERROR(leaveMutex(stream));

    return SYS_OK;
}

/*
 * Read packet header and insert into packet structure.
 * Allocate space for the data and fill it in.
 */
static jint
shmemBase_receivePacket_internal(SharedMemoryConnection *connection, jdwpPacket *packet)
{
    jint data_length;
    jint error;

    clearLastError();

    CHECK_ERROR(receiveBytes(connection, &packet->type.cmd.id, sizeof(jint)));
    CHECK_ERROR(receiveBytes(connection, &packet->type.cmd.flags, sizeof(jbyte)));

    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        CHECK_ERROR(receiveBytes(connection, &packet->type.reply.errorCode, sizeof(jshort)));
    } else {
        CHECK_ERROR(receiveBytes(connection, &packet->type.cmd.cmdSet, sizeof(jbyte)));
        CHECK_ERROR(receiveBytes(connection, &packet->type.cmd.cmd, sizeof(jbyte)));
    }

    CHECK_ERROR(receiveBytes(connection, &data_length, sizeof(jint)));

    if (data_length < 0) {
        return SYS_ERR;
    } else if (data_length == 0) {
        packet->type.cmd.len = JDWP_HEADER_SIZE;
        packet->type.cmd.data = NULL;
    } else {
        packet->type.cmd.len = data_length + JDWP_HEADER_SIZE;
        packet->type.cmd.data = (*callback->alloc)(data_length);
        if (packet->type.cmd.data == NULL) {
            return SYS_ERR;
        }

        error = receiveBytes(connection, packet->type.cmd.data, data_length);
        if (error != SYS_OK) {
            (*callback->free)(packet->type.cmd.data);
            return error;
        }
    }

    return SYS_OK;
}

jint
shmemBase_receivePacket(SharedMemoryConnection *connection, jdwpPacket *packet)
{
    ENTER_CONNECTION(connection);
    jint rc = shmemBase_receivePacket_internal(connection, packet);
    LEAVE_CONNECTION(connection);
    return rc;
}

jint
shmemBase_name(struct SharedMemoryTransport *transport, char **name)
{
    *name = transport->name;
    return SYS_OK;
}

jint
shmemBase_getlasterror(char *msg, jint size) {
    char *errstr = (char *)sysTlsGet(tlsIndex);
    if (errstr != NULL) {
        strcpy(msg, errstr);
        return SYS_OK;
    } else {
        return SYS_ERR;
    }
}


void
exitTransportWithError(char *message, char *fileName,
                       char *date, int lineNumber)
{
    JNIEnv *env;
    jint error;
    char buffer[500];

    sprintf(buffer, "Shared Memory Transport \"%s\" (%s), line %d: %s\n",
            fileName, date, lineNumber, message);
    error = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2);
    if (error != JNI_OK) {
        /*
         * We're forced into a direct call to exit()
         */
        fprintf(stderr, "%s", buffer);
        exit(-1);
    } else {
        (*env)->FatalError(env, buffer);
    }
}
