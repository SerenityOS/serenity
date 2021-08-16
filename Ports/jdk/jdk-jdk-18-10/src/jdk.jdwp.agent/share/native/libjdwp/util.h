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

#ifndef JDWP_UTIL_H
#define JDWP_UTIL_H

#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdarg.h>

#ifdef LINUX
// Note. On Alpine Linux pthread.h includes calloc/malloc functions declaration.
// We need to include pthread.h before the following stdlib names poisoning.
#include <pthread.h>
#endif

#ifdef DEBUG
    /* Just to make sure these interfaces are not used here. */
    #undef free
    #define free(p) Do not use this interface.
    #undef malloc
    #define malloc(p) Do not use this interface.
    #undef calloc
    #define calloc(p) Do not use this interface.
    #undef realloc
    #define realloc(p) Do not use this interface.
    #undef strdup
    #define strdup(p) Do not use this interface.
#endif

#include "log_messages.h"
#include "vm_interface.h"
#include "JDWP.h"
#include "util_md.h"
#include "error_messages.h"
#include "debugInit.h"

/* Definition of a CommonRef tracked by the backend for the frontend */
typedef struct RefNode {
    jlong        seqNum;        /* ID of reference, also key for hash table */
    jobject      ref;           /* could be strong or weak */
    struct RefNode *next;       /* next RefNode* in bucket chain */
    jint         count;         /* count of references */
    unsigned     strongCount;   /* count of strong reference */
} RefNode;

/* Value of a NULL ID */
#define NULL_OBJECT_ID  ((jlong)0)

/*
 * Globals used throughout the back end
 */

typedef jint FrameNumber;

typedef struct {
    jvmtiEnv *jvmti;
    JavaVM   *jvm;
    volatile jboolean vmDead; /* Once VM is dead it stays that way - don't put in init */
    jboolean assertOn;
    jboolean assertFatal;
    jboolean doerrorexit;
    jboolean modifiedUtf8;
    jboolean quiet;

    /* Debug flags (bit mask) */
    int      debugflags;

    /* Possible debug flags */
    #define USE_ITERATE_THROUGH_HEAP 0X001

    char * options;

    jclass              classClass;
    jclass              threadClass;
    jclass              threadGroupClass;
    jclass              classLoaderClass;
    jclass              stringClass;
    jclass              systemClass;
    jmethodID           threadConstructor;
    jmethodID           threadSetDaemon;
    jmethodID           threadResume;
    jmethodID           systemGetProperty;
    jmethodID           setProperty;
    jthreadGroup        systemThreadGroup;
    jobject             agent_properties;

    jint                cachedJvmtiVersion;
    jvmtiCapabilities   cachedJvmtiCapabilities;
    jboolean            haveCachedJvmtiCapabilities;
    jvmtiEventCallbacks callbacks;

    /* Various property values we should grab on initialization */
    char* property_java_version;          /* UTF8 java.version */
    char* property_java_vm_name;          /* UTF8 java.vm.name */
    char* property_java_vm_info;          /* UTF8 java.vm.info */
    char* property_java_class_path;       /* UTF8 java.class.path */
    char* property_sun_boot_library_path; /* UTF8 sun.boot.library.path */
    char* property_path_separator;        /* UTF8 path.separator */
    char* property_user_dir;              /* UTF8 user.dir */

    unsigned log_flags;

    /* Common References static data */
    jrawMonitorID refLock;
    jlong         nextSeqNum;
    unsigned      pinAllCount;
    RefNode     **objectsByID;
    int           objectsByIDsize;
    int           objectsByIDcount;

    /* Indication that the agent has been loaded */
    jboolean isLoaded;

    /* Indication that VM_DEATH has been recieved and the JVMTI callbacks have been cleared. */
    volatile jboolean jvmtiCallBacksCleared;

} BackendGlobalData;

extern BackendGlobalData * gdata;

/*
 * Event Index for handlers
 */

typedef enum {
        EI_min                  =  1,

        EI_SINGLE_STEP          =  1,
        EI_BREAKPOINT           =  2,
        EI_FRAME_POP            =  3,
        EI_EXCEPTION            =  4,
        EI_THREAD_START         =  5,
        EI_THREAD_END           =  6,
        EI_CLASS_PREPARE        =  7,
        EI_GC_FINISH            =  8,
        EI_CLASS_LOAD           =  9,
        EI_FIELD_ACCESS         = 10,
        EI_FIELD_MODIFICATION   = 11,
        EI_EXCEPTION_CATCH      = 12,
        EI_METHOD_ENTRY         = 13,
        EI_METHOD_EXIT          = 14,
        EI_MONITOR_CONTENDED_ENTER = 15,
        EI_MONITOR_CONTENDED_ENTERED = 16,
        EI_MONITOR_WAIT         = 17,
        EI_MONITOR_WAITED       = 18,
        EI_VM_INIT              = 19,
        EI_VM_DEATH             = 20,
        EI_max                  = 20
} EventIndex;

/* Agent errors that might be in a jvmtiError for JDWP or internal.
 *    (Done this way so that compiler allows it's use as a jvmtiError)
 */
#define _AGENT_ERROR(x)                 ((jvmtiError)(JVMTI_ERROR_MAX+64+x))
#define AGENT_ERROR_INTERNAL                    _AGENT_ERROR(1)
#define AGENT_ERROR_VM_DEAD                     _AGENT_ERROR(2)
#define AGENT_ERROR_NO_JNI_ENV                  _AGENT_ERROR(3)
#define AGENT_ERROR_JNI_EXCEPTION               _AGENT_ERROR(4)
#define AGENT_ERROR_JVMTI_INTERNAL              _AGENT_ERROR(5)
#define AGENT_ERROR_JDWP_INTERNAL               _AGENT_ERROR(6)
#define AGENT_ERROR_NOT_CURRENT_FRAME           _AGENT_ERROR(7)
#define AGENT_ERROR_OUT_OF_MEMORY               _AGENT_ERROR(8)
#define AGENT_ERROR_INVALID_TAG                 _AGENT_ERROR(9)
#define AGENT_ERROR_ALREADY_INVOKING            _AGENT_ERROR(10)
#define AGENT_ERROR_INVALID_INDEX               _AGENT_ERROR(11)
#define AGENT_ERROR_INVALID_LENGTH              _AGENT_ERROR(12)
#define AGENT_ERROR_INVALID_STRING              _AGENT_ERROR(13)
#define AGENT_ERROR_INVALID_CLASS_LOADER        _AGENT_ERROR(14)
#define AGENT_ERROR_INVALID_ARRAY               _AGENT_ERROR(15)
#define AGENT_ERROR_TRANSPORT_LOAD              _AGENT_ERROR(16)
#define AGENT_ERROR_TRANSPORT_INIT              _AGENT_ERROR(17)
#define AGENT_ERROR_NATIVE_METHOD               _AGENT_ERROR(18)
#define AGENT_ERROR_INVALID_COUNT               _AGENT_ERROR(19)
#define AGENT_ERROR_INVALID_FRAMEID             _AGENT_ERROR(20)
#define AGENT_ERROR_NULL_POINTER                _AGENT_ERROR(21)
#define AGENT_ERROR_ILLEGAL_ARGUMENT            _AGENT_ERROR(22)
#define AGENT_ERROR_INVALID_THREAD              _AGENT_ERROR(23)
#define AGENT_ERROR_INVALID_EVENT_TYPE          _AGENT_ERROR(24)
#define AGENT_ERROR_INVALID_OBJECT              _AGENT_ERROR(25)
#define AGENT_ERROR_NO_MORE_FRAMES              _AGENT_ERROR(26)
#define AGENT_ERROR_INVALID_MODULE              _AGENT_ERROR(27)

/* Combined event information */

typedef struct {

    EventIndex  ei;
    jthread     thread;
    jclass      clazz;
    jmethodID   method;
    jlocation   location;
    jobject     object; /* possibly an exception or user object */

    union {

        /* ei = EI_FIELD_ACCESS */
        struct {
            jclass      field_clazz;
            jfieldID    field;
        } field_access;

        /* ei = EI_FIELD_MODIFICATION */
        struct {
            jclass      field_clazz;
            jfieldID    field;
            char        signature_type;
            jvalue      new_value;
        } field_modification;

        /* ei = EI_EXCEPTION */
        struct {
            jclass      catch_clazz;
            jmethodID   catch_method;
            jlocation   catch_location;
        } exception;

        /* ei = EI_METHOD_EXIT */
        struct {
            jvalue      return_value;
        } method_exit;

        /* For monitor wait events */
        union {
            /* ei = EI_MONITOR_WAIT */
            jlong timeout;
            /* ei = EI_MONITOR_WAITED */
            jboolean timed_out;
        } monitor;
    } u;

} EventInfo;

/* Structure to hold dynamic array of objects */
typedef struct ObjectBatch {
    jobject *objects;
    jint     count;
} ObjectBatch;

/*
 * Modifier flags for classes, fields, methods
 */
#define MOD_PUBLIC       0x0001     /* visible to everyone */
#define MOD_PRIVATE      0x0002     /* visible only to the defining class */
#define MOD_PROTECTED    0x0004     /* visible to subclasses */
#define MOD_STATIC       0x0008     /* instance variable is static */
#define MOD_FINAL        0x0010     /* no further subclassing, overriding */
#define MOD_SYNCHRONIZED 0x0020     /* wrap method call in monitor lock */
#define MOD_VOLATILE     0x0040     /* can cache in registers */
#define MOD_TRANSIENT    0x0080     /* not persistant */
#define MOD_NATIVE       0x0100     /* implemented in C */
#define MOD_INTERFACE    0x0200     /* class is an interface */
#define MOD_ABSTRACT     0x0400     /* no definition provided */
/*
 * Additional modifiers not defined as such in the JVM spec
 */
#define MOD_SYNTHETIC    0xf0000000  /* not in source code */

/*
 * util funcs
 */
void util_initialize(JNIEnv *env);
void util_reset(void);

struct PacketInputStream;
struct PacketOutputStream;

jint uniqueID(void);
jbyte referenceTypeTag(jclass clazz);
jbyte specificTypeKey(JNIEnv *env, jobject object);
jboolean isObjectTag(jbyte tag);
jvmtiError spawnNewThread(jvmtiStartFunction func, void *arg, char *name);
void writeCodeLocation(struct PacketOutputStream *out, jclass clazz,
                       jmethodID method, jlocation location);

jvmtiError classInstances(jclass klass, ObjectBatch *instances, int maxInstances);
jvmtiError classInstanceCounts(jint classCount, jclass *classes, jlong *counts);
jvmtiError objectReferrers(jobject obj, ObjectBatch *referrers, int maxObjects);

/*
 * Command handling helpers shared among multiple command sets
 */
int filterDebugThreads(jthread *threads, int count);


void sharedGetFieldValues(struct PacketInputStream *in,
                          struct PacketOutputStream *out,
                          jboolean isStatic);
jboolean sharedInvoke(struct PacketInputStream *in,
                      struct PacketOutputStream *out);

jvmtiError fieldSignature(jclass, jfieldID, char **, char **, char **);
jvmtiError fieldModifiers(jclass, jfieldID, jint *);
jvmtiError methodSignature(jmethodID, char **, char **, char **);
jvmtiError methodReturnType(jmethodID, char *);
jvmtiError methodModifiers(jmethodID, jint *);
jvmtiError methodClass(jmethodID, jclass *);
jvmtiError methodLocation(jmethodID, jlocation*, jlocation*);
jvmtiError classLoader(jclass, jobject *);

/*
 * Thin wrappers on top of JNI
 */
JNIEnv *getEnv(void);
jboolean isClass(jobject object);
jboolean isThread(jobject object);
jboolean isThreadGroup(jobject object);
jboolean isString(jobject object);
jboolean isClassLoader(jobject object);
jboolean isArray(jobject object);

/*
 * Thin wrappers on top of JVMTI
 */
jvmtiError jvmtiGetCapabilities(jvmtiCapabilities *caps);
jint jvmtiMajorVersion(void);
jint jvmtiMinorVersion(void);
jint jvmtiMicroVersion(void);
jvmtiError getSourceDebugExtension(jclass clazz, char **extensionPtr);

jrawMonitorID debugMonitorCreate(char *name);
void debugMonitorEnter(jrawMonitorID theLock);
void debugMonitorExit(jrawMonitorID theLock);
void debugMonitorWait(jrawMonitorID theLock);
void debugMonitorTimedWait(jrawMonitorID theLock, jlong millis);
void debugMonitorNotify(jrawMonitorID theLock);
void debugMonitorNotifyAll(jrawMonitorID theLock);
void debugMonitorDestroy(jrawMonitorID theLock);

jthread *allThreads(jint *count);

void threadGroupInfo(jthreadGroup, jvmtiThreadGroupInfo *info);

jclass findClass(JNIEnv *env, const char * name);
jmethodID getMethod(JNIEnv *env, jclass clazz, const char * name, const char *signature);
char *getModuleName(jclass);
char *getClassname(jclass);
jvmtiError classSignature(jclass, char**, char**);
jint classStatus(jclass);
void writeGenericSignature(struct PacketOutputStream *, char *);
jboolean isMethodNative(jmethodID);
jboolean isMethodObsolete(jmethodID);
jvmtiError isMethodSynthetic(jmethodID, jboolean*);
jvmtiError isFieldSynthetic(jclass, jfieldID, jboolean*);

jboolean isSameObject(JNIEnv *env, jobject o1, jobject o2);

jint objectHashCode(jobject);

jvmtiError allInterfaces(jclass clazz, jclass **ppinterfaces, jint *count);
jvmtiError allLoadedClasses(jclass **ppclasses, jint *count);
jvmtiError allClassLoaderClasses(jobject loader, jclass **ppclasses, jint *count);
jvmtiError allNestedClasses(jclass clazz, jclass **ppnested, jint *pcount);

void setAgentPropertyValue(JNIEnv *env, char *propertyName, char* propertyValue);

void *jvmtiAllocate(jint numBytes);
void jvmtiDeallocate(void *buffer);

void             eventIndexInit(void);
#ifdef DEBUG
char*            eventIndex2EventName(EventIndex ei);
#endif
jdwpEvent        eventIndex2jdwp(EventIndex i);
jvmtiEvent       eventIndex2jvmti(EventIndex i);
EventIndex       jdwp2EventIndex(jdwpEvent eventType);
EventIndex       jvmti2EventIndex(jvmtiEvent kind);

jvmtiError       map2jvmtiError(jdwpError);
jdwpError        map2jdwpError(jvmtiError);
jdwpThreadStatus map2jdwpThreadStatus(jint state);
jint             map2jdwpSuspendStatus(jint state);
jint             map2jdwpClassStatus(jint);

void log_debugee_location(const char *func,
                jthread thread, jmethodID method, jlocation location);

/*
 * Local Reference management. The two macros below are used
 * throughout the back end whenever space for JNI local references
 * is needed in the current frame.
 */

void createLocalRefSpace(JNIEnv *env, jint capacity);

#define WITH_LOCAL_REFS(env, number) \
    createLocalRefSpace(env, number); \
    { /* BEGINNING OF WITH SCOPE */

#define END_WITH_LOCAL_REFS(env) \
        JNI_FUNC_PTR(env,PopLocalFrame)(env, NULL); \
    } /* END OF WITH SCOPE */

void saveGlobalRef(JNIEnv *env, jobject obj, jobject *pobj);
void tossGlobalRef(JNIEnv *env, jobject *pobj);

jvmtiEnv* getSpecialJvmti(void);

#endif
