/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 * JVMTI agent used for run every test from the testbase in a special
 * debug mode. This mode is intended to be part of serviceability
 * reliability testing.
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <jvmti.h>

#include "nsk_tools.h"
#include "jni_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

static jvmtiEnv *jvmti = NULL; /* JVMTI env */
static jvmtiEventCallbacks callbacks;
static jrawMonitorID eventLock; /* raw monitor used for exclusive ownership of HotSwap function */

static volatile int debug_mode = 0; /* 0 - verbose mode off;
                                       1 - verbose mode on;
                                       2 - verbose mode on including all JVMTI events reporting,
                                           produces a huge number of messages */

/* stress level */
static volatile int stress_lev = 0; /* 0 - default mode: generation of all events except
                                                ExceptionCatch,
                                                MethodEntry/Exit, SingleStep;
                                       1 - generation of all events except
                                                MethodEntry/Exit,
                                                SingleStep;
                                       2 - generation of all events except
                                                SingleStep;
                                       3 - generation of all events, including
                                                ExceptionCatch,
                                                MethodEntry/Exit,
                                                SingleStep
                                     */

#define TRUE 1
#define FALSE 0

/**** the following is used for "postVM_DEATH" events watching ****/
static volatile int vm_death_occured = FALSE;
/************************************************/

/**** the following is used for HotSwap mode ****/

/* HotSwap modes:
 HOTSWAP_OFF                - default mode: HotSwap off;
 HOTSWAP_EVERY_METHOD_ENTRY - HotSwap tested class in every method entry event
                              of running test
 HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS - HotSwap tested class in every
                              method entry event of every class
 HOTSWAP_EVERY_SINGLE_STEP  - HotSwap tested class in every single step event
                              of running test
 HOTSWAP_EVERY_EXCEPTION    - HotSwap tested class in every exception event
                              of running test
 HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS - HotSwap tested class in every
                              exception event of every class
 */

#define HOTSWAP_OFF 0
#define HOTSWAP_EVERY_METHOD_ENTRY 2
#define HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS 20
#define HOTSWAP_EVERY_SINGLE_STEP 3
#define HOTSWAP_EVERY_EXCEPTION 4
#define HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS 40

static int hotswap = HOTSWAP_OFF;

typedef struct {   /* test class info */
    char *clazzsig;  /* class signature */
    jclass cls;      /* a class to be redefined */
    jint bCount;     /* number of bytes defining the class */
    jbyte *clsBytes; /* bytes defining the class */
    struct class_info *next;
} class_info;


static const char *shortTestName = NULL; /* name of the test without package prefix */
static jclass rasCls; /* reference to the auxiliary class RASagent used for HotSwap */
static class_info *clsInfo = NULL, *clsInfoFst = NULL;

static void lock(JNIEnv*);
static void unlock(JNIEnv*);
static jint allocClsInfo(JNIEnv*, char*, jclass);
static void deallocClsInfo(JNIEnv*);
static int findAndHotSwap(JNIEnv*, jclass);
static int doHotSwap(JNIEnv*, jclass, jint, jbyte*);
static void display(int, const char format[], ...);
static void clearJavaException(JNIEnv*);
static int enableEventsCaps();
static int addStressEvents();
static void getVerdict(JNIEnv*, const char *);
/************************************************/

/** callback functions **/
void JNICALL
Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thr, jmethodID method,
        jlocation loc) {

    display(1, "#### JVMTIagent: Breakpoint occurred ####\n");

    getVerdict(jni_env, "Breakpoint");
}

void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jclass class_beeing_redefined,
        jobject loader, const char* name, jobject protection_domain,
        jint class_data_len, const unsigned char* class_data,
        jint *new_class_data_len, unsigned char** new_class_data) {

    display(1, "#### JVMTIagent: ClassFileLoadHook occurred ####\n");

    getVerdict(jni_env, "ClassFileLoadHook");
}

void JNICALL
ClassLoad(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread, jclass klass) {
    char *cls_sig;
    jint clsByteCount;

    display((hotswap != HOTSWAP_OFF) ? 0 : 1,
        "#### JVMTIagent: ClassLoad occurred ####\n");

    getVerdict(jni_env, "ClassLoad");

    if (hotswap != HOTSWAP_OFF) {
        /* enter into a raw monitor for exclusive work with redefined class */
        lock(jni_env);
        display(0, "#### JVMTIagent: ClassLoad: >>>>>>>> entered the raw monitor \"eventLock\" ####\n");

        if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &cls_sig, /*&generic*/NULL)))
            jni_env->FatalError("JVMTIagent: failed to get class signature\n");
        else {
            if (shortTestName != NULL) {
                if (strstr((const char*) cls_sig, shortTestName) != NULL) {
                    display(0,
                            "#### JVMTIagent: found test class matched with \"%s\"\n"
                            "<JVMTIagent>\tsignature=%s\n",
                            shortTestName, cls_sig);
                    clsByteCount = allocClsInfo(jni_env, cls_sig, klass);
                    display(0, "#### JVMTIagent: %d bytes defining the class have been successfully loaded\n",
                            clsByteCount);
                }
            }
        }

        /* exit from the raw monitor */
        unlock(jni_env);
        display(0, "#### JVMTIagent: ClassLoad: <<<<<<<< exited from the raw monitor \"eventLock\" ####\n\n");
    }
}

void JNICALL
ClassPrepare(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thr, jclass cls) {

    display(1, "#### JVMTIagent: ClassPrepare occurred ####\n");

    getVerdict(jni_env, "ClassPrepare");
}

void JNICALL
CompiledMethodLoad(jvmtiEnv *jvmti_env, jmethodID method, jint code_size,
        const void* code_addr,  jint map_length,
        const jvmtiAddrLocationMap* map, const void* compile_info) {

    display(1, "#### JVMTIagent: CompiledMethodLoad occurred ####\n");

    getVerdict(NULL, "CompiledMethodLoad");
}

void JNICALL
CompiledMethodUnload(jvmtiEnv *jvmti_env, jmethodID method,
        const void* code_addr) {

    display(1, "#### JVMTIagent: CompiledMethodUnload occurred ####\n");

    getVerdict(NULL, "CompiledMethodUnload");
}

void JNICALL
DataDumpRequest(jvmtiEnv *jvmti_env) {

    display(1, "#### JVMTIagent: DataDumpRequest occurred ####\n");

    getVerdict(NULL, "DataDumpRequest");
}

void JNICALL
DynamicCodeGenerated(jvmtiEnv *jvmti_env,
        const char* name,
        const void* address,
        jint length) {

    display(1, "#### JVMTIagent: DynamicCodeGenerated occurred ####\n");

    getVerdict(NULL, "DynamicCodeGenerated");
}

void JNICALL
Exception(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thr,
        jmethodID method, jlocation location, jobject exception,
        jmethodID catch_method, jlocation catch_location) {
    jclass decl_clazz;

    display((hotswap == HOTSWAP_EVERY_EXCEPTION ||
            hotswap == HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS) ? 0 : 1,
        "#### JVMTIagent: Exception occurred ####\n");

    getVerdict(jni_env, "Exception");

    if (hotswap == HOTSWAP_EVERY_EXCEPTION ||
            hotswap == HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(method, &decl_clazz)))
            jni_env->FatalError("JVMTIagent: failed to get method declaring class\n");

        if (findAndHotSwap(jni_env, decl_clazz) != 0)
            jni_env->FatalError("JVMTIagent: failed to hotswap class\n");
    }
}

void JNICALL
FieldAccess(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thr, jmethodID method,
        jlocation location, jclass field_klass, jobject obj, jfieldID field) {

    display(1, "#### JVMTIagent: FieldAccess occurred ####\n");

    getVerdict(jni_env, "FieldAccess");
}

void JNICALL
FieldModification(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thr, jmethodID method, jlocation location,
        jclass field_klass, jobject obj,
        jfieldID field, char sig, jvalue new_value) {

    display(1, "#### JVMTIagent: FieldModification occurred ####\n");

    getVerdict(jni_env, "FieldModification");
}

void JNICALL
FramePop(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thr, jmethodID method, jboolean wasPopedByException) {

    display(1, "#### JVMTIagent: FramePop occurred ####\n");

    getVerdict(jni_env, "FramePop");
}

void JNICALL
GarbageCollectionFinish(jvmtiEnv *jvmti_env) {

    display(1, "#### JVMTIagent: GarbageCollectionFinish occurred ####\n");

    getVerdict(NULL, "GarbageCollectionFinish");
}

void JNICALL
GarbageCollectionStart(jvmtiEnv *jvmti_env) {

    display(1, "#### JVMTIagent: GarbageCollectionStart occurred ####\n");

    getVerdict(NULL, "GarbageCollectionStart");
}

void JNICALL
MonitorContendedEnter(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thr,
        jobject obj) {

    display(1, "#### JVMTIagent: MonitorContendedEnter occurred ####\n");

    getVerdict(jni_env, "MonitorContendedEnter");
}

void JNICALL
MonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thr,
        jobject obj) {

    display(1, "#### JVMTIagent: MonitorContendedEntered occurred ####\n");

    getVerdict(jni_env, "MonitorContendedEntered");
}

void JNICALL
MonitorWait(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thr, jobject obj,
        jlong tout) {

    display(1, "#### JVMTIagent: MonitorWait occurred ####\n");

    getVerdict(jni_env, "MonitorWait");
}

void JNICALL
MonitorWaited(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
        jthread thr, jobject obj, jboolean timed_out) {

    display(1, "#### JVMTIagent: MonitorWaited occurred ####\n");

    getVerdict(jni_env, "MonitorWaited");
}

void JNICALL
NativeMethodBind(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
        jmethodID method, void *addr, void **new_addr) {

    display(1, "#### JVMTIagent: NativeMethodBind occurred ####\n");

    getVerdict(jni_env, "NativeMethodBind");
}

void JNICALL
ObjectFree(jvmtiEnv *jvmti_env, jlong tag) {

    display(1, "#### JVMTIagent: ObjectFree occurred ####\n");

    getVerdict(NULL, "ObjectFree");
}

void JNICALL
ThreadEnd(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread) {

    display(1, "#### JVMTIagent: ThreadEnd occurred ####\n");

    getVerdict(jni_env, "ThreadEnd");
}

void JNICALL
ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread) {

    display(1, "#### JVMTIagent: ThreadStart occurred ####\n");

    getVerdict(jni_env, "ThreadStart");
}

void JNICALL
VMDeath(jvmtiEnv *jvmti_env, JNIEnv *jni_env) {
    vm_death_occured = TRUE;

    display(0, "#### JVMTIagent: VMDeath occurred ####\n");

    if (hotswap != HOTSWAP_OFF) {
        deallocClsInfo(jni_env);
        display(0, "#### JVMTIagent: allocated memory was successfully freed ####\n");
    }
}

void JNICALL
VMInit(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thr) {

    display(0, "#### JVMTIagent: VMInit occurred ####\n");

    getVerdict(jni_env, "VMInit");
}

void JNICALL
VMStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env) {

    display(0, "#### JVMTIagent: VMStart occurred ####\n");

    getVerdict(jni_env, "VMStart");
}

JNIEXPORT void JNICALL
VMObjectAlloc(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
        jobject object, jclass object_klass, jlong size) {

    display(1, "#### JVMTIagent: VMObjectAlloc occurred ####\n");

    getVerdict(jni_env, "VMObjectAlloc");
}

void JNICALL
SingleStep(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
        jmethodID method, jlocation location) {
    jclass decl_clazz;

    display((hotswap == HOTSWAP_EVERY_SINGLE_STEP) ? 0 : 1,
        "#### JVMTIagent: SingleStep occurred ####\n");

    getVerdict(jni_env, "SingleStep");

    if (hotswap == HOTSWAP_EVERY_SINGLE_STEP) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(method, &decl_clazz)))
            jni_env->FatalError("JVMTIagent: failed to get method declaring class\n");

        if (findAndHotSwap(jni_env, decl_clazz) != 0)
            jni_env->FatalError("JVMTIagent: failed to hotswap class\n");
    }
}

void JNICALL
MethodEntry(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thr, jmethodID method) {
    jclass decl_clazz;

    display((hotswap == HOTSWAP_EVERY_METHOD_ENTRY ||
            hotswap == HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS) ? 0 : 1,
        "#### JVMTIagent: MethodEntry occurred ####\n");

    getVerdict(jni_env, "MethodEntry");

    if (hotswap == HOTSWAP_EVERY_METHOD_ENTRY ||
            hotswap == HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(method, &decl_clazz)))
            jni_env->FatalError("JVMTIagent: failed to get method declaring class\n");

        if (findAndHotSwap(jni_env, decl_clazz) != 0)
            jni_env->FatalError("JVMTIagent: failed to hotswap class\n");
    }
}

void JNICALL
MethodExit(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thr, jmethodID method,
        jboolean was_poped_by_exc, jvalue return_value) {

    display(1, "#### JVMTIagent: MethodExit occurred ####\n");

    getVerdict(jni_env, "MethodExit");
}

void JNICALL
ExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thr,
        jmethodID method, jlocation location, jobject exception) {
    jclass decl_clazz;

    display((hotswap == HOTSWAP_EVERY_EXCEPTION ||
            hotswap == HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS) ? 0 : 1,
        "#### JVMTIagent: ExceptionCatch occurred ####\n");

    getVerdict(jni_env, "ExceptionCatch");

    if (hotswap == HOTSWAP_EVERY_EXCEPTION ||
            hotswap == HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(method, &decl_clazz)))
            jni_env->FatalError("JVMTIagent: failed to get method declaring class\n");

        if (findAndHotSwap(jni_env, decl_clazz) != 0)
            jni_env->FatalError("JVMTIagent: failed to hotswap class\n");
    }
}
/************************/

static void lock(JNIEnv *jni_env) {
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(eventLock)))
        jni_env->FatalError("JVMTIagent: failed to enter a raw monitor\n");
}

static void unlock(JNIEnv *jni_env) {
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(eventLock)))
        jni_env->FatalError("JVMTIagent: failed to exit a raw monitor\n");
}

JNIEXPORT jint JNICALL
Java_nsk_share_RASagent_setHotSwapMode(JNIEnv *jni_env, jclass cls,
        jboolean vrb, jint level, jstring shortName) {
    jvmtiCapabilities capabil;
    jmethodID mid = NULL;

    if (jvmti == NULL) {
        printf("ERROR(%s,%d): JVMTIagent was not properly loaded: JVMTI env = NULL\n",
               __FILE__, __LINE__);
        return 1;
    }

    /* get supported JVMTI capabilities */
    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&capabil)))
        jni_env->FatalError("JVMTIagent: failed to get capabilities\n");
    if (capabil.can_redefine_classes != 1) { /* ???????????? */
        printf("ERROR: JVMTIagent: Class File Redefinition (HotSwap) is not implemented in this VM\n");
        return 1;
    }

    if (vrb == JNI_TRUE && debug_mode == 0)
        debug_mode = 1;

    hotswap = level;
    switch (hotswap) {
        case HOTSWAP_OFF:
            display(0, "#### JVMTIagent: hotswap mode off ####\n");
            return 0;
        case HOTSWAP_EVERY_METHOD_ENTRY:
            stress_lev = 2;
            display(0,
                    "#### JVMTIagent: hotswapping class in every method entry event enabled ####\n"
                    "<JVMTIagent>\tHotSwap stress level: %d\n",
                    stress_lev);
            break;
        case HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS:
            stress_lev = 2;
            display(0,
                    "#### JVMTIagent: hotswapping class in every method entry event for every class enabled ####\n"
                    "<JVMTIagent>\tHotSwap stress level: %d\n",
                    stress_lev);
            break;
        case HOTSWAP_EVERY_SINGLE_STEP:
            stress_lev = 3;
            display(0,
                    "#### JVMTIagent: hotswapping class in every single step event enabled ####\n"
                    "<JVMTIagent>\tHotSwap stress level: %d\n",
                    stress_lev);
            break;
        case HOTSWAP_EVERY_EXCEPTION:
            stress_lev = 4;
            display(0,
                    "#### JVMTIagent: hotswapping class in every exception event enabled ####\n"
                    "<JVMTIagent>\tHotSwap stress level: %d\n",
                    stress_lev);
            break;
        case HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS:
            stress_lev = 40;
            display(0,
                    "#### JVMTIagent: hotswapping class in every exception event for every class enabled ####\n"
                    "<JVMTIagent>\tHotSwap stress level: %d\n",
                    stress_lev);
            break;
        default:
            printf("ERROR(%s,%d): JVMTIagent: unknown value of HotSwap stress level: \"%d\"\n",
                __FILE__,__LINE__,hotswap);
            return 1;
    }

    if (!NSK_JNI_VERIFY(jni_env, (shortTestName = jni_env->GetStringUTFChars(shortName, NULL)) != NULL)) {
        printf("ERROR: JVMTIagent: unable to get UTF-8 characters of the string\n");
        return 1;
    }
    display(0, "#### JVMTIagent: short name of current test is \"%s\"\n",
        shortTestName);

    if (!NSK_JNI_VERIFY(jni_env, (rasCls = jni_env->NewGlobalRef(cls)) != NULL)) {
        printf("ERROR JVMTIagent: unable to create a new global reference of the class \"RASagent\"\n");
        return 1;
    }

    if (addStressEvents() != 0) {
        printf("ERROR(%s,%d): JVMTIagent terminated abnormally! ####\n",
            __FILE__,__LINE__);
        return 1;
    }

    return 0;
}

static jint allocClsInfo(JNIEnv *jni_env, char *cls_sig, jclass clazz) {
    class_info *_clsInfo = NULL;
    jmethodID mid = NULL;
    jbyteArray classBytes;
    jboolean isCopy;

    _clsInfo = (class_info*) malloc(sizeof(class_info));
    if (_clsInfo == NULL)
        jni_env->FatalError("JVMTIagent: cannot allocate memory for class_info\n");

    /* fill the structure class_info */
    _clsInfo->clazzsig = cls_sig;

    if (!NSK_JNI_VERIFY(jni_env, ((*_clsInfo).cls = jni_env->NewGlobalRef(clazz)) != NULL)) {
        printf("ERROR: JVMTIagent: unable to create a new global reference of class \"%s\"\n",
            _clsInfo->clazzsig);
        free(_clsInfo);
        deallocClsInfo(jni_env);
        jni_env->FatalError("JVMTIagent: unable to create a new global reference of class\n");
    }

    if (!NSK_JNI_VERIFY(jni_env, (mid =
        jni_env->GetStaticMethodID(rasCls, "loadFromClassFile", "(Ljava/lang/String;)[B")) != NULL))
        jni_env->FatalError("JVMTIagent: unable to get ID of the method \"loadFromClassFile\"\n");

    classBytes = (jbyteArray) jni_env->CallStaticObjectMethod(rasCls, mid, jni_env->NewStringUTF(cls_sig));

    clearJavaException(jni_env);

    (*_clsInfo).bCount = jni_env->GetArrayLength(classBytes);

    (*_clsInfo).clsBytes =
        jni_env->GetByteArrayElements(classBytes, &isCopy);

    _clsInfo->next = NULL;

    if (clsInfo != NULL) {
        clsInfo->next = (struct class_info*) _clsInfo;
    }
    else {
        clsInfoFst = _clsInfo;
    }
    clsInfo = _clsInfo;

    return (*_clsInfo).bCount;
}

static void deallocClsInfo(JNIEnv *jni_env) {
    class_info *clsInfoCurr = clsInfoFst;

    NSK_TRACE(jni_env->DeleteGlobalRef(rasCls));

    while (clsInfoCurr != NULL) {
        class_info *_clsInfo = clsInfoCurr;

        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) clsInfoCurr->clazzsig)))
            jni_env->FatalError("JVMTIagent: failed to deallocate memory for clazzsig\n");

        NSK_TRACE(jni_env->DeleteGlobalRef(clsInfoCurr->cls));

        clsInfoCurr = (class_info*) clsInfoCurr->next;

        free(_clsInfo);
    }
    /* fix for 4756585: indicate that stucture class_info is empty now */
    clsInfoFst = NULL;
}

static int findAndHotSwap(JNIEnv *jni_env, jclass clazz) {
    int ret_code = 0;
    char *clazzsig = NULL;
    class_info *clsInfoCurr = clsInfoFst;

    display(1, "\n#### JVMTIagent: findAndHotSwap: obtaining class signature of class to be hotswap ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(clazz, &clazzsig, /*&generic*/NULL)))
        jni_env->FatalError("JVMTIagent: findAndHotSwap: failed to get class signature\n");
    else {
        display(1, "#### JVMTIagent: findAndHotSwap: ... class signature obtained: \"%s\"\n",
            clazzsig);

        /* enter into a raw monitor for exclusive work with redefined class */
        lock(jni_env);
        display(0, "#### JVMTIagent: findAndHotSwap: >>>>>>>> entered the raw monitor \"eventLock\" ####\n");

        while (clsInfoCurr != NULL) {
            if (hotswap == HOTSWAP_EVERY_METHOD_ENTRY_FOR_EVERY_CLASS ||
                    hotswap == HOTSWAP_EVERY_EXCEPTION_FOR_EVERY_CLASS) {
                display(1, "\n#### JVMTIagent: findAndHotSwap: going to hotswap tested class \"%s\" during execution of class \"%s\" ...\n",
                    clsInfoCurr->clazzsig, clazzsig);
                if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) clazzsig)))
                    jni_env->FatalError("JVMTIagent: findAndHotSwap: failed to deallocate memory for clazzsig\n");

                if (doHotSwap(jni_env, clsInfoCurr->cls,
                        clsInfoCurr->bCount, clsInfoCurr->clsBytes) != 0) {
                    ret_code = 1;
                    break;
                }
            }
            else {
                if (strcmp(clazzsig, clsInfoCurr->clazzsig) == 0) {
                    display(0, "\n#### JVMTIagent: findAndHotSwap: tested class found \"%s\" ...\n",
                        clazzsig);

                    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) clazzsig)))
                        jni_env->FatalError("JVMTIagent: findAndHotSwap: failed to deallocate memory for clazzsig\n");

                    display(0, "\n#### JVMTIagent: findAndHotSwap: going to hotswap tested class \"%s\" ...\n",
                        clsInfoCurr->clazzsig);
                    if (doHotSwap(jni_env, clsInfoCurr->cls,
                            clsInfoCurr->bCount, clsInfoCurr->clsBytes) != 0) {
                        ret_code = 1;
                        break;
                    }
                }
            }

            clsInfoCurr = (class_info*) clsInfoCurr->next;
        }

        /* exit raw monitor */
        unlock(jni_env);
        display(0, "#### JVMTIagent: findAndHotSwap: <<<<<<<< exited from the raw monitor \"eventLock\" ####\n\n");
    }

    return ret_code;
}

static int doHotSwap(JNIEnv *jni_env, jclass redefCls, jint bCount,
        jbyte *classBytes) {
    jvmtiClassDefinition classDef;

    /* fill the structure jvmtiClassDefinition */
    classDef.klass = redefCls;
    classDef.class_byte_count = bCount;
    classDef.class_bytes = (unsigned char*) classBytes;

    display(0,
            "#### JVMTIagent: >>>>>>>> Invoke RedefineClasses():\n"
            "<JVMTIagent>\tnew class byte count=%d\n",
            classDef.class_byte_count);
    if (!NSK_JVMTI_VERIFY(jvmti->RedefineClasses(1, &classDef)))
        return 1;

    display(0, "#### JVMTIagent: <<<<<<<< RedefineClasses() is successfully done ####\n");

    return 0;
}

static int addStressEvents() {
    static int stepEventSet = JNI_FALSE;
    static int methodsEventSet = JNI_FALSE;
    static int excCatchEventSet = JNI_FALSE;

    if (stress_lev >= 3) {
        /* SingleStep events */
        if (stepEventSet == JNI_FALSE) { /* don't set the event twice */
            display(0, "#### JVMTIagent: setting SingleStep events ...\n");

            callbacks.SingleStep = &SingleStep;

            if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, NULL)))
                return JNI_ERR;

            stepEventSet = JNI_TRUE;

            display(0, "#### JVMTIagent: ... setting SingleStep events done\n");
        }
    }

    if (stress_lev >= 2) {
        /* MethodEntry/Exit events */
        if (methodsEventSet == JNI_FALSE) { /* don't set the event twice */
            display(0, "#### JVMTIagent: setting MethodEntry events ...\n");

            callbacks.MethodEntry = &MethodEntry;

            if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, NULL)))
                return JNI_ERR;

            display(0, "#### JVMTIagent: ... setting MethodEntry events done\n");

            /* MethodExit events */
            display(0, "#### JVMTIagent: setting MethodExit events ...\n");

            callbacks.MethodExit = &MethodExit;

            if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, NULL)))
                return JNI_ERR;

            display(0, "#### JVMTIagent: ... setting MethodExit events done\n");

            methodsEventSet = JNI_TRUE;
        }
    }

    if (stress_lev >= 1) {
        /* ExceptionCatch events */
        if (excCatchEventSet == JNI_FALSE) { /* don't set the event twice */
            display(0, "#### JVMTIagent: setting ExceptionCatch events ...\n");

            callbacks.ExceptionCatch = &ExceptionCatch;

            if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION_CATCH, NULL)))
                return JNI_ERR;

            excCatchEventSet = JNI_TRUE;

            display(0, "#### JVMTIagent: ... setting ExceptionCatch events done\n");
        }
    }

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;
    else
        return 0;
}

static int enableEventsCaps() {
    jvmtiCapabilities caps;

    memset(&caps, 0, sizeof(jvmtiCapabilities));

    /* add all capabilities */
    caps.can_redefine_classes = 1;
    caps.can_generate_breakpoint_events = 1;
    caps.can_generate_all_class_hook_events = 1;
    caps.can_generate_single_step_events = 1;
    caps.can_generate_method_entry_events = 1;
    caps.can_generate_method_exit_events = 1;
    caps.can_generate_exception_events = 1;
    caps.can_generate_compiled_method_load_events = 1;
    caps.can_generate_field_access_events = 1;
    caps.can_generate_field_modification_events = 1;
    caps.can_generate_frame_pop_events = 1;
    caps.can_generate_garbage_collection_events = 1;
    caps.can_generate_monitor_events = 1;
    caps.can_generate_native_method_bind_events = 1;
    caps.can_generate_object_free_events = 1;
    caps.can_generate_vm_object_alloc_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    /* Breakpoint events */
    display(0, "#### JVMTIagent: setting Breakpoint events ...\n");

    callbacks.Breakpoint = &Breakpoint;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_BREAKPOINT, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting Breakpoint events done\n");

    /* ClassFileLoadHook events */
    display(0, "#### JVMTIagent: setting ClassFileLoadHook events ...\n");

    callbacks.ClassFileLoadHook = &ClassFileLoadHook;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting ClassFileLoadHook events done\n");

    /* ClassLoad events */
    display(0, "#### JVMTIagent: setting ClassLoad events ...\n");

    callbacks.ClassLoad = &ClassLoad;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting ClassLoad events done\n");

    /* ClassPrepare events */
    display(0, "#### JVMTIagent: setting ClassPrepare events ...\n");

    callbacks.ClassPrepare = &ClassPrepare;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting ClassPrepare events done\n");

    /* CompiledMethodLoad events */
    display(0, "#### JVMTIagent: setting CompiledMethodLoad events ...\n");

    callbacks.CompiledMethodLoad = &CompiledMethodLoad;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting CompiledMethodLoad events done\n");

    /* CompiledMethodUnload events */
    display(0, "#### JVMTIagent: setting CompiledMethodUnload events ...\n");

    callbacks.CompiledMethodUnload = &CompiledMethodUnload;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_UNLOAD, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting CompiledMethodUnload events done\n");

    /* DataDumpRequest events */
    display(0, "#### JVMTIagent: setting DataDumpRequest events ...\n");

    callbacks.DataDumpRequest = &DataDumpRequest;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DATA_DUMP_REQUEST, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting DataDumpRequest events done\n");

    /* DynamicCodeGenerated events */
    display(0, "#### JVMTIagent: setting DynamicCodeGenerated events ...\n");

    callbacks.DynamicCodeGenerated = &DynamicCodeGenerated;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DYNAMIC_CODE_GENERATED, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting DynamicCodeGenerated events done\n");

    /* Exception events */
    display(0, "#### JVMTIagent: setting Exception events ...\n");

    callbacks.Exception = &Exception;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting Exception events done\n");

    /* FieldAccess events */
    display(0, "#### JVMTIagent: setting FieldAccess events ...\n");

    callbacks.FieldAccess = &FieldAccess;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_ACCESS, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting FieldAccess events done\n");

    /* FieldModification events */
    display(0, "#### JVMTIagent: setting FieldModification events ...\n");

    callbacks.FieldModification = &FieldModification;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_MODIFICATION, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting FieldModification events done\n");

    /* FramePop events */
    display(0, "#### JVMTIagent: setting FramePop events ...\n");

    callbacks.FramePop = &FramePop;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FRAME_POP, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting FramePop events done\n");

    /* GarbageCollectionFinish events */
    display(0, "#### JVMTIagent: setting GarbageCollectionFinish events ...\n");

    callbacks.GarbageCollectionFinish = &GarbageCollectionFinish;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting GarbageCollectionFinish events done\n");

    /* GarbageCollectionStart events */
    display(0, "#### JVMTIagent: setting GarbageCollectionStart events ...\n");

    callbacks.GarbageCollectionStart = &GarbageCollectionStart;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_START, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting GarbageCollectionStart events done\n");

    /* MonitorContendedEnter events */
    display(0, "#### JVMTIagent: setting MonitorContendedEnter events ...\n");

    callbacks.MonitorContendedEnter = &MonitorContendedEnter;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting MonitorContendedEnter events done\n");

    /* MonitorContendedEntered events */
    display(0, "#### JVMTIagent: setting MonitorContendedEntered events ...\n");

    callbacks.MonitorContendedEntered = &MonitorContendedEntered;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting MonitorContendedEntered events done\n");

    /* MonitorWait events */
    display(0, "#### JVMTIagent: setting MonitorWait events ...\n");

    callbacks.MonitorWait = &MonitorWait;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAIT, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting MonitorWait events done\n");

    /* MonitorWaited events */
    display(0, "#### JVMTIagent: setting MonitorWaited events ...\n");

    callbacks.MonitorWaited = &MonitorWaited;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAITED, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting MonitorWaited events done\n");

    /* NativeMethodBind events */
    display(0, "#### JVMTIagent: setting NativeMethodBind events ...\n");

    callbacks.NativeMethodBind = &NativeMethodBind;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting NativeMethodBind events done\n");

    /* ObjectFree events */
    display(0, "#### JVMTIagent: setting ObjectFree events ...\n");

    callbacks.ObjectFree = &ObjectFree;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting ObjectFree events done\n");

    /* ThreadEnd events */
    display(0, "#### JVMTIagent: setting ThreadEnd events ...\n");

    callbacks.ThreadEnd = &ThreadEnd;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting ThreadEnd events done\n");

    /* ThreadStart events */
    display(0, "#### JVMTIagent: setting ThreadStart events ...\n");

    callbacks.ThreadStart = &ThreadStart;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting ThreadStart events done\n");

    /* VMDeath events */
    display(0, "#### JVMTIagent: setting VMDeath events ...\n");

    callbacks.VMDeath = &VMDeath;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting VMDeath events done\n");

    /* VMInit events */
    display(0, "#### JVMTIagent: setting VMInit events ...\n");

    callbacks.VMInit = &VMInit;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting VMInit events done\n");

    /* VMStart events */
    display(0, "#### JVMTIagent: setting VMStart events ...\n");

    callbacks.VMStart = &VMStart;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting VMStart events done\n");

    /* VMObjectAlloc events */
    display(0, "#### JVMTIagent: setting VMObjectAlloc events ...\n");

    callbacks.VMObjectAlloc = &VMObjectAlloc;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, NULL)))
        return JNI_ERR;
    display(0, "#### JVMTIagent: ... setting VMObjectAlloc events done\n");

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    return 0;
}

static void clearJavaException(JNIEnv* jni_env) {
    if (jni_env->ExceptionOccurred()) {

        jni_env->ExceptionDescribe();
        jni_env->ExceptionClear();

        jni_env->FatalError("JVMTIagent: exception occurred in java code, aborting\n");
    }
}

static int get_tok(char **src, char *buf, int buflen, char sep) {
    int i;
    char *p = *src;
    for (i = 0; i < buflen; i++) {
        if (p[i] == 0 || p[i] == sep) {
            buf[i] = 0;
            if (p[i] == sep) {
                i++;
            }
            *src += i;
            return i;
        }
        buf[i] = p[i];
    }
    /* overflow */
    return 0;
}

static void doSetup(char *str) {
    if (str == 0)
        str = (char*) "";

    if ((strcmp(str, "help")) == 0) {
        printf("#### JVMTIagent usage: -agentlib:JVMTIagent[=[help]|[=[verbose]|[verbose2],[stress0|stress1|stress2|stress3]]]\n");
        printf("####      where: help\tprint this message\n");
        printf("####             verbose\tturn verbose mode on\n");
        printf("####             verbose2\tturn extended verbose mode on (including reporting JVMTI events)\n");
        printf("####             stress0, or empty value\tturn stress level 0 on (default mode):\n");
        printf("####                   enable event generation except ExceptionCatch, MethodEntry/Exit, SingleStep\n");
        printf("####             stress1\tturn stress level 1 on:\n");
        printf("####                   enable generation of ExceptionCatch events\n");
        printf("####             stress2\tturn stress level 2 on:\n");
        printf("####                   enable generation of ExceptionCatch,\n");
        printf("####                                        MethodEntry/Exit events\n");
        printf("####             stress3\tturn stress level 3 on:\n");
        printf("####                   enable generation of ExceptionCatch,\n");
        printf("####                                        MethodEntry/Exit,\n");
        printf("####                                        SingleStep events\n");
        exit(1);
    }

    while (*str) {
        char buf[1000];

        if (!get_tok(&str, buf, sizeof(buf), ',')) {
            printf("ERROR: JVMTIagent: bad option: \"%s\"!\n", str);
            exit(1);
        }
        if ((strcmp(buf, "verbose")) == 0) {
            printf("#### JVMTIagent: turned verbose mode on ####\n");
            debug_mode = 1;
        }
        if ((strcmp(buf, "verbose2")) == 0) {
            printf("#### JVMTIagent: turned extended verbose mode on ####\n");
            debug_mode = 2;
        }
        if ((strcmp(buf, "stress0")) == 0) {
            if (debug_mode > 0)
                printf("#### JVMTIagent: turned stress level 0 on ####\n");
            stress_lev = 0;
        }
        if ((strcmp(buf, "stress1")) == 0) {
            if (debug_mode > 0)
                printf("#### JVMTIagent: turned stress level 1 on ####\n");
            stress_lev = 1;
        }
        if ((strcmp(buf, "stress2")) == 0) {
            if (debug_mode > 0)
                printf("#### JVMTIagent: turned stress level 2 on ####\n");
            stress_lev = 2;
        }
        if ((strcmp(buf, "stress3")) == 0) {
            if (debug_mode > 0)
                printf("#### JVMTIagent: turned stress level 3 on ####\n");
            stress_lev = 3;
        }
    }
}

static void getVerdict(JNIEnv *jni_env, const char *evnt) {
    char error_msg[80];

    if (vm_death_occured == TRUE) {
        sprintf(error_msg, "JVMTIagent: getVerdict: %s event occured after VMDeath",
            evnt);

        if (jni_env == NULL) { /* some event callbacks have no pointer to jni */
            printf("ERROR: %s\n", error_msg);
            exit(97);
        }
        else
            jni_env->FatalError(error_msg);
    }
}

static void display(int level, const char format[], ...) {
    va_list ar;

    if (debug_mode > level) {
        va_start(ar, format);
        vprintf(format, ar);
        va_end(ar);
    }
}

/* agent procedure */
static void JNICALL
agentProc(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg) {
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    doSetup(options);

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_event_lock", &eventLock)))
        return JNI_ERR;

    if (enableEventsCaps() == 0 && addStressEvents() == 0) {
        display(0, "#### JVMTIagent: all events were successfully enabled and capabilities/events callbacks set ####\n\n");
    } else {
        printf("ERROR(%s,%d): JVMTIagent terminated abnormally! ####\n",
            __FILE__,__LINE__);
        return JNI_ERR;
    }

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

}
