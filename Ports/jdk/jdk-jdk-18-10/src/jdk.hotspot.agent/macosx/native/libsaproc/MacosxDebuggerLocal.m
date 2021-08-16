/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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
 *
 */

#include <objc/objc-runtime.h>
#import <Foundation/Foundation.h>

#include <jni.h>

#import <mach/mach.h>
#import <mach/mach_types.h>
#import <sys/sysctl.h>
#import <stdio.h>
#import <string.h>
#import <stdarg.h>
#import <stdlib.h>
#import <strings.h>
#import <dlfcn.h>
#import <limits.h>
#import <errno.h>
#import <sys/types.h>
#import <sys/ptrace.h>
#include "libproc_impl.h"

#if defined(amd64)
#include "sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext.h"
#elif defined(aarch64)
#include "sun_jvm_hotspot_debugger_aarch64_AARCH64ThreadContext.h"
#else
#error UNSUPPORTED_ARCH
#endif

static jfieldID symbolicatorID = 0; // set in _init0
static jfieldID taskID = 0; // set in _init0

static jfieldID p_ps_prochandle_ID = 0;
static jfieldID loadObjectList_ID = 0;
static jmethodID listAdd_ID = 0;

static jmethodID createClosestSymbol_ID = 0;
static jmethodID createLoadObject_ID = 0;
static jmethodID getJavaThreadsInfo_ID = 0;

// indicator if thread id (lwpid_t) was set
static bool _threads_filled = false;

// mach_exc_server defined in the generated mach_excServer.c
extern boolean_t mach_exc_server(mach_msg_header_t *input_msg_hdr,
                                 mach_msg_header_t *output_msg_hdr);

kern_return_t catch_mach_exception_raise(
  mach_port_t exception_port, mach_port_t thread,
  mach_port_t task, exception_type_t exception,
  mach_exception_data_t code,
  mach_msg_type_number_t code_cnt);

kern_return_t catch_mach_exception_raise_state(
  mach_port_t exception_port, exception_type_t exception,
  const mach_exception_data_t code, mach_msg_type_number_t code_cnt,
  int *flavor, const thread_state_t old_state,
  mach_msg_type_number_t old_state_cnt, thread_state_t new_state,
  mach_msg_type_number_t *new_state_cnt);

kern_return_t catch_mach_exception_raise_state_identity(
  mach_port_t exception_port, mach_port_t thread, mach_port_t task,
  exception_type_t exception, mach_exception_data_t code,
  mach_msg_type_number_t code_cnt, int *flavor, thread_state_t old_state,
  mach_msg_type_number_t old_state_cnt, thread_state_t new_state,
  mach_msg_type_number_t *new_state_cnt);

static struct exception_saved_state {
  exception_mask_t       saved_masks[EXC_TYPES_COUNT];
  mach_port_t            saved_ports[EXC_TYPES_COUNT];
  exception_behavior_t   saved_behaviors[EXC_TYPES_COUNT];
  thread_state_flavor_t  saved_flavors[EXC_TYPES_COUNT];
  mach_msg_type_number_t saved_exception_types_count;
} exception_saved_state;

static mach_port_t tgt_exception_port;

// Mirrors __Reply__mach_exception_raise_t generated in mach_excServer.c
static struct rep_msg {
  mach_msg_header_t header;
  NDR_record_t ndr;
  kern_return_t ret_code;
} rep_msg;

// Mirrors __Request__mach_exception_raise_t generated in mach_excServer.c
// with a large trailing pad to avoid MACH_MSG_RCV_TOO_LARGE
static struct exc_msg {
  mach_msg_header_t header;
  // start of the kernel processed data
  mach_msg_body_t msgh_body;
  mach_msg_port_descriptor_t thread;
  mach_msg_port_descriptor_t task;
  // end of the kernel processed data
  NDR_record_t ndr;
  exception_type_t exception;
  mach_msg_type_number_t code_cnt;
  mach_exception_data_t code; // an array of int64_t
  char pad[512];
} exc_msg;

static void putSymbolicator(JNIEnv *env, jobject this_obj, id symbolicator) {
  (*env)->SetLongField(env, this_obj, symbolicatorID, (jlong)(intptr_t)symbolicator);
}

static id getSymbolicator(JNIEnv *env, jobject this_obj) {
  jlong ptr = (*env)->GetLongField(env, this_obj, symbolicatorID);
  return (id)(intptr_t)ptr;
}

static void putTask(JNIEnv *env, jobject this_obj, task_t task) {
  (*env)->SetLongField(env, this_obj, taskID, (jlong)task);
}

static task_t getTask(JNIEnv *env, jobject this_obj) {
  jlong ptr = (*env)->GetLongField(env, this_obj, taskID);
  return (task_t)ptr;
}

#define CHECK_EXCEPTION_(value) if ((*env)->ExceptionOccurred(env)) { return value; }
#define CHECK_EXCEPTION if ((*env)->ExceptionOccurred(env)) { return;}
#define THROW_NEW_DEBUGGER_EXCEPTION_(str, value) { throw_new_debugger_exception(env, str); return value; }
#define THROW_NEW_DEBUGGER_EXCEPTION(str) { throw_new_debugger_exception(env, str); return;}
#define CHECK_EXCEPTION_CLEAR if ((*env)->ExceptionOccurred(env)) { (*env)->ExceptionClear(env); }
#define CHECK_EXCEPTION_CLEAR_VOID if ((*env)->ExceptionOccurred(env)) { (*env)->ExceptionClear(env); return; }
#define CHECK_EXCEPTION_CLEAR_(value) if ((*env)->ExceptionOccurred(env)) { (*env)->ExceptionClear(env); return value; }

static void throw_new_debugger_exception(JNIEnv* env, const char* errMsg) {
  jclass exceptionClass = (*env)->FindClass(env, "sun/jvm/hotspot/debugger/DebuggerException");
  CHECK_EXCEPTION;
  (*env)->ThrowNew(env, exceptionClass, errMsg);
}

static struct ps_prochandle* get_proc_handle(JNIEnv* env, jobject this_obj) {
  jlong ptr = (*env)->GetLongField(env, this_obj, p_ps_prochandle_ID);
  return (struct ps_prochandle*)(intptr_t)ptr;
}

#if defined(amd64)
    #define hsdb_thread_state_t     x86_thread_state64_t
    #define hsdb_float_state_t      x86_float_state64_t
    #define HSDB_THREAD_STATE       x86_THREAD_STATE64
    #define HSDB_FLOAT_STATE        x86_FLOAT_STATE64
    #define HSDB_THREAD_STATE_COUNT x86_THREAD_STATE64_COUNT
    #define HSDB_FLOAT_STATE_COUNT  x86_FLOAT_STATE64_COUNT
#elif defined(aarch64)
    #define hsdb_thread_state_t     arm_thread_state64_t
    #define hsdb_float_state_t      arm_neon_state64_t
    #define HSDB_THREAD_STATE       ARM_THREAD_STATE64
    #define HSDB_FLOAT_STATE        ARM_NEON_STATE64
    #define HSDB_THREAD_STATE_COUNT ARM_THREAD_STATE64_COUNT
    #define HSDB_FLOAT_STATE_COUNT  ARM_NEON_STATE64_COUNT
#else
    #error UNSUPPORTED_ARCH
#endif

/*
 * Class:     sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal
 * Method:    init0
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_init0(JNIEnv *env, jclass cls) {
  symbolicatorID = (*env)->GetFieldID(env, cls, "symbolicator", "J");
  CHECK_EXCEPTION;
  taskID = (*env)->GetFieldID(env, cls, "task", "J");
  CHECK_EXCEPTION;

  // for core file
  p_ps_prochandle_ID = (*env)->GetFieldID(env, cls, "p_ps_prochandle", "J");
  CHECK_EXCEPTION;
  loadObjectList_ID = (*env)->GetFieldID(env, cls, "loadObjectList", "Ljava/util/List;");
  CHECK_EXCEPTION;

  // methods we use
  createClosestSymbol_ID = (*env)->GetMethodID(env, cls, "createClosestSymbol",
                    "(Ljava/lang/String;J)Lsun/jvm/hotspot/debugger/cdbg/ClosestSymbol;");
  CHECK_EXCEPTION;
  createLoadObject_ID = (*env)->GetMethodID(env, cls, "createLoadObject",
                    "(Ljava/lang/String;JJ)Lsun/jvm/hotspot/debugger/cdbg/LoadObject;");
  CHECK_EXCEPTION;

  // java.util.List method we call
  jclass listClass = (*env)->FindClass(env, "java/util/List");
  CHECK_EXCEPTION;
  listAdd_ID = (*env)->GetMethodID(env, listClass, "add", "(Ljava/lang/Object;)Z");
  CHECK_EXCEPTION;
  getJavaThreadsInfo_ID = (*env)->GetMethodID(env, cls, "getJavaThreadsInfo",
                                                     "()[J");
  CHECK_EXCEPTION;

  init_libproc(getenv("LIBSAPROC_DEBUG") != NULL);
}

JNIEXPORT jint JNICALL Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_getAddressSize
  (JNIEnv *env, jclass cls)
{
#ifdef _LP64
  return 8;
#else
  #error UNSUPPORTED_ARCH
#endif
}

/** called by Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_lookupByName0 */
jlong lookupByNameIncore(
  JNIEnv *env, struct ps_prochandle *ph, jobject this_obj, jstring objectName, jstring symbolName)
{
  const char *objectName_cstr, *symbolName_cstr;
  jlong addr;
  jboolean isCopy;
  objectName_cstr = NULL;
  if (objectName != NULL) {
    objectName_cstr = (*env)->GetStringUTFChars(env, objectName, &isCopy);
    CHECK_EXCEPTION_(0);
  }
  symbolName_cstr = (*env)->GetStringUTFChars(env, symbolName, &isCopy);
  if ((*env)->ExceptionOccurred(env)) {
    if (objectName_cstr != NULL) {
      (*env)->ReleaseStringUTFChars(env, objectName, objectName_cstr);
    }
    return 0;
  }

  print_debug("look for %s \n", symbolName_cstr);
  addr = (jlong) lookup_symbol(ph, objectName_cstr, symbolName_cstr);

  if (objectName_cstr != NULL) {
    (*env)->ReleaseStringUTFChars(env, objectName, objectName_cstr);
  }
  (*env)->ReleaseStringUTFChars(env, symbolName, symbolName_cstr);
  return addr;
}

/* Create a pool and initiate a try block to catch any exception */
#define JNI_COCOA_ENTER(env) \
 NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init]; \
 @try {

/* Don't allow NSExceptions to escape to Java.
 * If there is a Java exception that has been thrown that should escape.
 * And ensure we drain the auto-release pool.
 */
#define JNI_COCOA_EXIT(env) \
 } \
 @catch (NSException *e) { \
     NSLog(@"%@", [e callStackSymbols]); \
 } \
 @finally { \
    [pool drain]; \
 };

static NSString* JavaStringToNSString(JNIEnv *env, jstring jstr) {

    if (jstr == NULL) {
        return NULL;
    }
    jsize len = (*env)->GetStringLength(env, jstr);
    const jchar *chars = (*env)->GetStringChars(env, jstr, NULL);
    if (chars == NULL) {
        return NULL;
    }
    NSString *result = [NSString stringWithCharacters:(UniChar *)chars length:len];
    (*env)->ReleaseStringChars(env, jstr, chars);
    return result;
}

/*
 * Class:     sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal
 * Method:    lookupByName0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_lookupByName0(
  JNIEnv *env, jobject this_obj,
  jstring objectName, jstring symbolName)
{
  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  if (ph != NULL && ph->core != NULL) {
    return lookupByNameIncore(env, ph, this_obj, objectName, symbolName);
  }

  jlong address = 0;

  JNI_COCOA_ENTER(env);

  NSString *symbolNameString = JavaStringToNSString(env, symbolName);

  print_debug("lookupInProcess called for %s\n", [symbolNameString UTF8String]);

  id symbolicator = getSymbolicator(env, this_obj);
  if (symbolicator != nil) {
    uint64_t (*dynamicCall)(id, SEL, NSString *) = (uint64_t (*)(id, SEL, NSString *))&objc_msgSend;
    address = (jlong) dynamicCall(symbolicator, @selector(addressForSymbol:), symbolNameString);
  }

  print_debug("address of symbol %s = %llx\n", [symbolNameString UTF8String], address);
  JNI_COCOA_EXIT(env);

  return address;
}

/*
 * Class:     sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal
 * Method:    lookupByAddress0
 * Signature: (J)Lsun/jvm/hotspot/debugger/cdbg/ClosestSymbol;
 */
JNIEXPORT jobject JNICALL Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_lookupByAddress0
  (JNIEnv *env, jobject this_obj, jlong addr) {
  uintptr_t offset;
  const char* sym = NULL;
  jstring sym_string;

  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  if (ph != NULL && ph->core != NULL) {
    sym = symbol_for_pc(ph, (uintptr_t) addr, &offset);
    if (sym == NULL) return 0;
    sym_string = (*env)->NewStringUTF(env, sym);
    CHECK_EXCEPTION_(0);
    return (*env)->CallObjectMethod(env, this_obj, createClosestSymbol_ID,
                                                sym_string, (jlong)offset);
  }
  return 0;
}

/** called from Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_readBytesFromProcess0 */
jbyteArray readBytesFromCore(
  JNIEnv *env, struct ps_prochandle *ph, jobject this_obj, jlong addr, jlong numBytes)
{
  jboolean isCopy;
  jbyteArray array;
  jbyte *bufPtr;
  ps_err_e err;

  array = (*env)->NewByteArray(env, numBytes);
  CHECK_EXCEPTION_(0);
  bufPtr = (*env)->GetByteArrayElements(env, array, &isCopy);
  CHECK_EXCEPTION_(0);

  err = ps_pread(ph, (psaddr_t) (uintptr_t)addr, bufPtr, numBytes);
  (*env)->ReleaseByteArrayElements(env, array, bufPtr, 0);
  return (err == PS_OK)? array : 0;
}

/*
 * Class:     sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal
 * Method:    readBytesFromProcess0
 * Signature: (JJ)Lsun/jvm/hotspot/debugger/ReadResult;
 */
JNIEXPORT jbyteArray JNICALL
Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_readBytesFromProcess0(
  JNIEnv *env, jobject this_obj,
  jlong addr, jlong numBytes)
{
  print_debug("readBytesFromProcess called. addr = %llx numBytes = %lld\n", addr, numBytes);

  // must allocate storage instead of using former parameter buf
  jbyteArray array;

  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  if (ph != NULL && ph->core != NULL) {
    return readBytesFromCore(env, ph, this_obj, addr, numBytes);
  }

  array = (*env)->NewByteArray(env, numBytes);
  CHECK_EXCEPTION_(0);

  unsigned long alignedAddress;
  unsigned long alignedLength = 0;
  kern_return_t result;
  vm_offset_t *pages;
  int *mapped;
  long pageCount;
  uint byteCount;
  int i;
  unsigned long remaining;

  alignedAddress = trunc_page(addr);
  if (addr != alignedAddress) {
    alignedLength += addr - alignedAddress;
  }
  alignedLength = round_page(numBytes);
  pageCount = alignedLength/vm_page_size;

  // Allocate storage for pages and flags.
  pages = malloc(pageCount * sizeof(vm_offset_t));
  if (pages == NULL) {
    (*env)->DeleteLocalRef(env, array);
    return NULL;
  }
  mapped = calloc(pageCount, sizeof(int));
  if (mapped == NULL) {
    (*env)->DeleteLocalRef(env, array);
    free(pages);
    return NULL;
  }

  task_t gTask = getTask(env, this_obj);
  // Try to read each of the pages.
  for (i = 0; i < pageCount; i++) {
    result = vm_read(gTask, alignedAddress + i*vm_page_size, vm_page_size,
                     &pages[i], &byteCount);
    mapped[i] = (result == KERN_SUCCESS);
    // assume all failures are unmapped pages
  }

  print_debug("%ld pages\n", pageCount);

  remaining = numBytes;

  for (i = 0; i < pageCount; i++) {
    unsigned long len = vm_page_size;
    unsigned long start = 0;

    if (i == 0) {
      start = addr - alignedAddress;
      len = vm_page_size - start;
    }

    if (i == (pageCount - 1)) {
      len = remaining;
    }

    if (mapped[i]) {
      print_debug("page %d mapped (len %ld start %ld)\n", i, len, start);
      (*env)->SetByteArrayRegion(env, array, 0, len, ((jbyte *) pages[i] + start));
      vm_deallocate(mach_task_self(), pages[i], vm_page_size);
    }

    remaining -= len;
  }

  free (pages);
  free (mapped);
  return array;
}

/** Only used for core file reading, set thread_id for threads which is got after core file parsed.
  * Thread context is available in Mach-O core file but thread id is not. We can get thread id
  * from Threads which store all java threads information when they are created. Here we can identify
  * them as java threads by checking if a thread's rsp or rbp within a java thread's stack.
  * Note Macosx uses unique_thread_id which is different from other platforms though printed ids
  * are still pthread id. Function BsdDebuggerLocal.getJavaThreadsInfo returns an array of long
  * integers to host all java threads' id, stack_start, stack_end as:
  * [uid0, stack_start0, stack_end0, uid1, stack_start1, stack_end1, ...]
  *
  * The work cannot be done at init0 since Threads is not available yet(VM not initialized yet).
  * This function should be called only once if succeeded
  */
bool fill_java_threads(JNIEnv* env, jobject this_obj, struct ps_prochandle* ph) {
  int n = 0, i = 0, j;
  struct reg regs;

  jlongArray thrinfos = (*env)->CallObjectMethod(env, this_obj, getJavaThreadsInfo_ID);
  CHECK_EXCEPTION_(false);
  int len = (int)(*env)->GetArrayLength(env, thrinfos);
  uint64_t* cinfos = (uint64_t *)(*env)->GetLongArrayElements(env, thrinfos, NULL);
  CHECK_EXCEPTION_(false);
  n = get_num_threads(ph);
  print_debug("fill_java_threads called, num_of_thread = %d\n", n);
  for (i = 0; i < n; i++) {
    if (!get_nth_lwp_regs(ph, i, &regs)) {
      print_debug("Could not get regs of thread %d, already set!\n", i);
      (*env)->ReleaseLongArrayElements(env, thrinfos, (jlong*)cinfos, 0);
      return false;
    }
    for (j = 0; j < len; j += 3) {
      lwpid_t  uid = cinfos[j];
      uint64_t beg = cinfos[j + 1];
      uint64_t end = cinfos[j + 2];
#if defined(amd64)
      if ((regs.r_rsp < end && regs.r_rsp >= beg) ||
          (regs.r_rbp < end && regs.r_rbp >= beg)) {
        set_lwp_id(ph, i, uid);
        break;
      }
#elif defined(aarch64)
      if ((regs.r_sp < end && regs.r_sp >= beg) ||
          (regs.r_fp < end && regs.r_fp >= beg)) {
        set_lwp_id(ph, i, uid);
        break;
      }
#else
#error UNSUPPORTED_ARCH
#endif
    }
  }
  (*env)->ReleaseLongArrayElements(env, thrinfos, (jlong*)cinfos, 0);
  CHECK_EXCEPTION_(false);
  return true;
}

/* For core file only, called from
 * Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_getThreadIntegerRegisterSet0
 */
jlongArray getThreadIntegerRegisterSetFromCore(JNIEnv *env, jobject this_obj, long lwp_id, struct ps_prochandle* ph) {
  if (!_threads_filled)  {
    if (!fill_java_threads(env, this_obj, ph)) {
      throw_new_debugger_exception(env, "Failed to fill in threads");
      return 0;
    } else {
      _threads_filled = true;
    }
  }

  struct reg gregs;
  jboolean isCopy;
  jlongArray array;
  jlong *regs;

  if (get_lwp_regs(ph, lwp_id, &gregs) != true) {
    THROW_NEW_DEBUGGER_EXCEPTION_("get_thread_regs failed for a lwp", 0);
  }

#undef NPRGREG
#undef REG_INDEX
#if defined(amd64)
#define NPRGREG sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext_NPRGREG
#define REG_INDEX(reg) sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext_##reg
#elif defined(aarch64)
#define NPRGREG sun_jvm_hotspot_debugger_aarch64_AARCH64ThreadContext_NPRGREG
#define REG_INDEX(reg) sun_jvm_hotspot_debugger_aarch64_AARCH64ThreadContext_##reg
#else
#error UNSUPPORTED_ARCH
#endif

  array = (*env)->NewLongArray(env, NPRGREG);
  CHECK_EXCEPTION_(0);
  regs = (*env)->GetLongArrayElements(env, array, &isCopy);

#if defined(amd64)

  regs[REG_INDEX(R15)] = gregs.r_r15;
  regs[REG_INDEX(R14)] = gregs.r_r14;
  regs[REG_INDEX(R13)] = gregs.r_r13;
  regs[REG_INDEX(R12)] = gregs.r_r12;
  regs[REG_INDEX(RBP)] = gregs.r_rbp;
  regs[REG_INDEX(RBX)] = gregs.r_rbx;
  regs[REG_INDEX(R11)] = gregs.r_r11;
  regs[REG_INDEX(R10)] = gregs.r_r10;
  regs[REG_INDEX(R9)]  = gregs.r_r9;
  regs[REG_INDEX(R8)]  = gregs.r_r8;
  regs[REG_INDEX(RAX)] = gregs.r_rax;
  regs[REG_INDEX(RCX)] = gregs.r_rcx;
  regs[REG_INDEX(RDX)] = gregs.r_rdx;
  regs[REG_INDEX(RSI)] = gregs.r_rsi;
  regs[REG_INDEX(RDI)] = gregs.r_rdi;
  regs[REG_INDEX(RIP)] = gregs.r_rip;
  regs[REG_INDEX(CS)]  = gregs.r_cs;
  regs[REG_INDEX(RSP)] = gregs.r_rsp;
  regs[REG_INDEX(SS)]  = gregs.r_ss;
  regs[REG_INDEX(FSBASE)] = 0;
  regs[REG_INDEX(GSBASE)] = 0;
  regs[REG_INDEX(DS)] = gregs.r_ds;
  regs[REG_INDEX(ES)] = gregs.r_es;
  regs[REG_INDEX(FS)] = gregs.r_fs;
  regs[REG_INDEX(GS)] = gregs.r_gs;
  regs[REG_INDEX(TRAPNO)] = gregs.r_trapno;
  regs[REG_INDEX(RFL)]    = gregs.r_rflags;

#elif defined(aarch64)

  regs[REG_INDEX(R0)] = gregs.r_r0;
  regs[REG_INDEX(R1)] = gregs.r_r1;
  regs[REG_INDEX(R2)] = gregs.r_r2;
  regs[REG_INDEX(R3)] = gregs.r_r3;
  regs[REG_INDEX(R4)] = gregs.r_r4;
  regs[REG_INDEX(R5)] = gregs.r_r5;
  regs[REG_INDEX(R6)] = gregs.r_r6;
  regs[REG_INDEX(R7)] = gregs.r_r7;
  regs[REG_INDEX(R8)] = gregs.r_r8;
  regs[REG_INDEX(R9)] = gregs.r_r9;
  regs[REG_INDEX(R10)] = gregs.r_r10;
  regs[REG_INDEX(R11)] = gregs.r_r11;
  regs[REG_INDEX(R12)] = gregs.r_r12;
  regs[REG_INDEX(R13)] = gregs.r_r13;
  regs[REG_INDEX(R14)] = gregs.r_r14;
  regs[REG_INDEX(R15)] = gregs.r_r15;
  regs[REG_INDEX(R16)] = gregs.r_r16;
  regs[REG_INDEX(R17)] = gregs.r_r17;
  regs[REG_INDEX(R18)] = gregs.r_r18;
  regs[REG_INDEX(R19)] = gregs.r_r19;
  regs[REG_INDEX(R20)] = gregs.r_r20;
  regs[REG_INDEX(R21)] = gregs.r_r21;
  regs[REG_INDEX(R22)] = gregs.r_r22;
  regs[REG_INDEX(R23)] = gregs.r_r23;
  regs[REG_INDEX(R24)] = gregs.r_r24;
  regs[REG_INDEX(R25)] = gregs.r_r25;
  regs[REG_INDEX(R26)] = gregs.r_r26;
  regs[REG_INDEX(R27)] = gregs.r_r27;
  regs[REG_INDEX(R28)] = gregs.r_r28;
  regs[REG_INDEX(FP)] = gregs.r_fp;
  regs[REG_INDEX(LR)] = gregs.r_lr;
  regs[REG_INDEX(SP)] = gregs.r_sp;
  regs[REG_INDEX(PC)] = gregs.r_pc;

#else
#error UNSUPPORTED_ARCH
#endif

  (*env)->ReleaseLongArrayElements(env, array, regs, JNI_COMMIT);
  return array;
}

/*
 * Lookup the thread_t that corresponds to the given thread_id.
 * The thread_id should be the result from calling thread_info() with THREAD_IDENTIFIER_INFO
 * and reading the m_ident_info.thread_id returned.
 * The returned thread_t is the mach send right to the kernel port for the corresponding thread.
 *
 * We cannot simply use the OSThread._thread_id field in the JVM. This is set to ::mach_thread_self()
 * in the VM, but that thread port is not valid for a remote debugger to access the thread.
 */
thread_t
lookupThreadFromThreadId(task_t task, jlong thread_id) {
  print_debug("lookupThreadFromThreadId thread_id=0x%llx\n", thread_id);

  thread_array_t thread_list = NULL;
  mach_msg_type_number_t thread_list_count = 0;
  thread_t result_thread = 0;
  int i;

  // get the list of all the send rights
  kern_return_t result = task_threads(task, &thread_list, &thread_list_count);
  if (result != KERN_SUCCESS) {
    print_debug("task_threads returned 0x%x\n", result);
    return 0;
  }

  for(i = 0 ; i < thread_list_count; i++) {
    thread_identifier_info_data_t m_ident_info;
    mach_msg_type_number_t count = THREAD_IDENTIFIER_INFO_COUNT;

    // get the THREAD_IDENTIFIER_INFO for the send right
    result = thread_info(thread_list[i], THREAD_IDENTIFIER_INFO, (thread_info_t) &m_ident_info, &count);
    if (result != KERN_SUCCESS) {
      print_debug("thread_info returned 0x%x\n", result);
      break;
    }

    // if this is the one we're looking for, return the send right
    if (thread_id == m_ident_info.thread_id)
    {
      result_thread = thread_list[i];
      break;
    }
  }

  vm_size_t thread_list_size = (vm_size_t) (thread_list_count * sizeof (thread_t));
  vm_deallocate(mach_task_self(), (vm_address_t) thread_list, thread_list_count);

  return result_thread;
}


/*
 * Class:     sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal
 * Method:    getThreadIntegerRegisterSet0
 * Signature: (J)[J
 */
JNIEXPORT jlongArray JNICALL
Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_getThreadIntegerRegisterSet0(
  JNIEnv *env, jobject this_obj,
  jlong thread_id)
{
  print_debug("getThreadIntegerRegisterSet0 called\n");

  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  if (ph != NULL && ph->core != NULL) {
    return getThreadIntegerRegisterSetFromCore(env, this_obj, thread_id, ph);
  }

  kern_return_t result;
  thread_t tid;
  mach_msg_type_number_t count = HSDB_THREAD_STATE_COUNT;
  hsdb_thread_state_t state;
  jlongArray registerArray;
  jlong *primitiveArray;
  task_t gTask = getTask(env, this_obj);

  tid = lookupThreadFromThreadId(gTask, thread_id);

  result = thread_get_state(tid, HSDB_THREAD_STATE, (thread_state_t)&state, &count);

  if (result != KERN_SUCCESS) {
    // This is not considered fatal. Unlike on Linux and Windows, we haven't seen a
    // failure to get thread registers, but if it were to fail the response should
    // be the same. By ignoring this error and returning NULL, stacking walking code
    // will get null registers and fallback to using the "last java frame" if setup.
    fprintf(stdout, "WARNING: getThreadIntegerRegisterSet0: thread_get_state failed (%d) for thread (%d)\n",
            result, tid);
    fflush(stdout);
    return NULL;
  }

#undef NPRGREG
#if defined(amd64)
#define NPRGREG sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext_NPRGREG
#elif defined(aarch64)
#define NPRGREG sun_jvm_hotspot_debugger_aarch64_AARCH64ThreadContext_NPRGREG
#else
#error UNSUPPORTED_ARCH
#endif

  // 64 bit
  print_debug("Getting threads for a 64-bit process\n");
  registerArray = (*env)->NewLongArray(env, NPRGREG);
  CHECK_EXCEPTION_(0);
  primitiveArray = (*env)->GetLongArrayElements(env, registerArray, NULL);

#if defined(amd64)

  primitiveArray[REG_INDEX(R15)] = state.__r15;
  primitiveArray[REG_INDEX(R14)] = state.__r14;
  primitiveArray[REG_INDEX(R13)] = state.__r13;
  primitiveArray[REG_INDEX(R12)] = state.__r12;
  primitiveArray[REG_INDEX(R11)] = state.__r11;
  primitiveArray[REG_INDEX(R10)] = state.__r10;
  primitiveArray[REG_INDEX(R9)]  = state.__r9;
  primitiveArray[REG_INDEX(R8)]  = state.__r8;
  primitiveArray[REG_INDEX(RDI)] = state.__rdi;
  primitiveArray[REG_INDEX(RSI)] = state.__rsi;
  primitiveArray[REG_INDEX(RBP)] = state.__rbp;
  primitiveArray[REG_INDEX(RBX)] = state.__rbx;
  primitiveArray[REG_INDEX(RDX)] = state.__rdx;
  primitiveArray[REG_INDEX(RCX)] = state.__rcx;
  primitiveArray[REG_INDEX(RAX)] = state.__rax;
  primitiveArray[REG_INDEX(TRAPNO)] = 0;            // trapno, not used
  primitiveArray[REG_INDEX(ERR)]    = 0;            // err, not used
  primitiveArray[REG_INDEX(RIP)] = state.__rip;
  primitiveArray[REG_INDEX(CS)]  = state.__cs;
  primitiveArray[REG_INDEX(RFL)] = state.__rflags;
  primitiveArray[REG_INDEX(RSP)] = state.__rsp;
  primitiveArray[REG_INDEX(SS)] = 0;                // We don't have SS
  primitiveArray[REG_INDEX(FS)] = state.__fs;
  primitiveArray[REG_INDEX(GS)] = state.__gs;
  primitiveArray[REG_INDEX(ES)] = 0;
  primitiveArray[REG_INDEX(DS)] = 0;
  primitiveArray[REG_INDEX(FSBASE)] = 0;
  primitiveArray[REG_INDEX(GSBASE)] = 0;

#elif defined(aarch64)

  primitiveArray[REG_INDEX(R0)] = state.__x[0];
  primitiveArray[REG_INDEX(R1)] = state.__x[1];
  primitiveArray[REG_INDEX(R2)] = state.__x[2];
  primitiveArray[REG_INDEX(R3)] = state.__x[3];
  primitiveArray[REG_INDEX(R4)] = state.__x[4];
  primitiveArray[REG_INDEX(R5)] = state.__x[5];
  primitiveArray[REG_INDEX(R6)] = state.__x[6];
  primitiveArray[REG_INDEX(R7)] = state.__x[7];
  primitiveArray[REG_INDEX(R8)] = state.__x[8];
  primitiveArray[REG_INDEX(R9)] = state.__x[9];
  primitiveArray[REG_INDEX(R10)] = state.__x[10];
  primitiveArray[REG_INDEX(R11)] = state.__x[11];
  primitiveArray[REG_INDEX(R12)] = state.__x[12];
  primitiveArray[REG_INDEX(R13)] = state.__x[13];
  primitiveArray[REG_INDEX(R14)] = state.__x[14];
  primitiveArray[REG_INDEX(R15)] = state.__x[15];
  primitiveArray[REG_INDEX(R16)] = state.__x[16];
  primitiveArray[REG_INDEX(R17)] = state.__x[17];
  primitiveArray[REG_INDEX(R18)] = state.__x[18];
  primitiveArray[REG_INDEX(R19)] = state.__x[19];
  primitiveArray[REG_INDEX(R20)] = state.__x[20];
  primitiveArray[REG_INDEX(R21)] = state.__x[21];
  primitiveArray[REG_INDEX(R22)] = state.__x[22];
  primitiveArray[REG_INDEX(R23)] = state.__x[23];
  primitiveArray[REG_INDEX(R24)] = state.__x[24];
  primitiveArray[REG_INDEX(R25)] = state.__x[25];
  primitiveArray[REG_INDEX(R26)] = state.__x[26];
  primitiveArray[REG_INDEX(R27)] = state.__x[27];
  primitiveArray[REG_INDEX(R28)] = state.__x[28];
  primitiveArray[REG_INDEX(FP)] = state.__fp;
  primitiveArray[REG_INDEX(LR)] = state.__lr;
  primitiveArray[REG_INDEX(SP)] = state.__sp;
  primitiveArray[REG_INDEX(PC)] = state.__pc;

#else
#error UNSUPPORTED_ARCH
#endif

  print_debug("set registers\n");

  (*env)->ReleaseLongArrayElements(env, registerArray, primitiveArray, 0);
  return registerArray;
}

/*
 * Class:     sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal
 * Method:    translateTID0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_sun_jvm_hotspot_debugger_macosx_MacOSXDebuggerLocal_translateTID0(
  JNIEnv *env, jobject this_obj, jint tid)
{
  print_debug("translateTID0 called on tid = 0x%x\n", (int)tid);

  kern_return_t result;
  thread_t foreign_tid, usable_tid;
  mach_msg_type_name_t type;

  foreign_tid = tid;

  task_t gTask = getTask(env, this_obj);
  result = mach_port_extract_right(gTask, foreign_tid,
                                   MACH_MSG_TYPE_COPY_SEND,
                                   &usable_tid, &type);
  if (result != KERN_SUCCESS)
    return -1;

  print_debug("translateTID0: 0x%x -> 0x%x\n", foreign_tid, usable_tid);

  return (jint) usable_tid;
}

// attach to a process/thread specified by "pid"
static bool ptrace_attach(pid_t pid) {
  errno = 0;
  ptrace(PT_ATTACHEXC, pid, 0, 0);

  if (errno != 0) {
    print_error("ptrace_attach: ptrace(PT_ATTACHEXC,...) failed: %s", strerror(errno));
    return false;
  }
  return true;
}

kern_return_t catch_mach_exception_raise(
  mach_port_t exception_port, mach_port_t thread_port, mach_port_t task_port,
  exception_type_t exception_type, mach_exception_data_t codes,
  mach_msg_type_number_t num_codes) {

  print_debug("catch_mach_exception_raise: Exception port = %d thread_port = %d "
              "task port %d exc type = %d num_codes %d\n",
              exception_port, thread_port, task_port, exception_type, num_codes);

  // This message should denote a Unix soft signal, with
  // 1. the exception type = EXC_SOFTWARE
  // 2. codes[0] (which is the code) = EXC_SOFT_SIGNAL
  // 3. codes[1] (which is the sub-code) = SIGSTOP
  if (!(exception_type == EXC_SOFTWARE &&
        codes[0] == EXC_SOFT_SIGNAL    &&
        codes[num_codes -1] == SIGSTOP)) {
    print_error("catch_mach_exception_raise: Message doesn't denote a Unix "
                "soft signal. exception_type = %d, codes[0] = %d, "
                "codes[num_codes -1] = %d, num_codes = %d\n",
                exception_type, codes[0], codes[num_codes - 1], num_codes);
    return MACH_RCV_INVALID_TYPE;
  }
  return KERN_SUCCESS;
}

kern_return_t catch_mach_exception_raise_state(
  mach_port_t exception_port, exception_type_t exception, const mach_exception_data_t code,
  mach_msg_type_number_t code_cnt, int *flavor, const thread_state_t old_state,
  mach_msg_type_number_t old_state_cnt, thread_state_t new_state,
  mach_msg_type_number_t *new_state_cnt) {
  return MACH_RCV_INVALID_TYPE;
}


kern_return_t catch_mach_exception_raise_state_identity(
  mach_port_t exception_port, mach_port_t thread, mach_port_t task,
  exception_type_t exception, mach_exception_data_t code,
  mach_msg_type_number_t code_cnt, int *flavor,
  thread_state_t old_state, mach_msg_type_number_t old_state_cnt,
  thread_state_t new_state, mach_msg_type_number_t *new_state_cnt) {
  return MACH_RCV_INVALID_TYPE;
}

// wait to receive an exception message
static bool wait_for_exception() {
  kern_return_t result;

  result = mach_msg(&exc_msg.header,
                    MACH_RCV_MSG,
                    0,
                    sizeof(exc_msg),
                    tgt_exception_port,
                    MACH_MSG_TIMEOUT_NONE,
                    MACH_PORT_NULL);

  if (result != MACH_MSG_SUCCESS) {
    print_error("attach: wait_for_exception: mach_msg() failed: '%s' (%d)\n",
                mach_error_string(result), result);
    return false;
  }

  if (mach_exc_server(&exc_msg.header, &rep_msg.header) == false ||
      rep_msg.ret_code != KERN_SUCCESS) {
    print_error("attach: wait_for_exception: mach_exc_server failure\n");
    if (rep_msg.ret_code != KERN_SUCCESS) {
      print_error("catch_mach_exception_raise() failed '%s' (%d)\n",
                  mach_error_string(rep_msg.ret_code), rep_msg.ret_code);
    }
    return false;
  }

  print_debug("reply msg from mach_exc_server: (msg->{bits = %#x, size = %u, "
              "remote_port = %#x, local_port = %#x, reserved = 0x%x, id = 0x%x},)",
              rep_msg.header.msgh_bits, rep_msg.header.msgh_size,
              rep_msg.header.msgh_remote_port, rep_msg.header.msgh_local_port,
              rep_msg.header.msgh_reserved, rep_msg.header.msgh_id);

  return true;
}

/*
 * Class:     sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal
 * Method:    attach0
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_attach0__I(
  JNIEnv *env, jobject this_obj, jint jpid)
{
  print_debug("attach0 called for jpid=%d\n", (int)jpid);

  JNI_COCOA_ENTER(env);

  kern_return_t result;
  task_t gTask = 0;

  result = task_for_pid(mach_task_self(), jpid, &gTask);
  if (result != KERN_SUCCESS) {
    print_error("attach: task_for_pid(%d) failed: '%s' (%d)\n", (int)jpid, mach_error_string(result), result);
    THROW_NEW_DEBUGGER_EXCEPTION(
      "Can't attach to the process. Could be caused by an incorrect pid or lack of privileges.");
  }
  putTask(env, this_obj, gTask);

  // Allocate an exception port.
  result = mach_port_allocate(mach_task_self(),
                              MACH_PORT_RIGHT_RECEIVE,
                              &tgt_exception_port);
  if (result != KERN_SUCCESS) {
    print_error("attach: mach_port_allocate() for tgt_exception_port failed: '%s' (%d)\n",
                mach_error_string(result), result);
    THROW_NEW_DEBUGGER_EXCEPTION(
      "Can't attach to the process. Couldn't allocate an exception port.");
  }

  // Enable the new exception port to send messages.
  result = mach_port_insert_right (mach_task_self(),
                                   tgt_exception_port,
                                   tgt_exception_port,
                                   MACH_MSG_TYPE_MAKE_SEND);
  if (result != KERN_SUCCESS) {
    print_error("attach: mach_port_insert_right() failed for port 0x%x: '%s' (%d)\n",
                tgt_exception_port, mach_error_string(result), result);
    THROW_NEW_DEBUGGER_EXCEPTION(
      "Can't attach to the process. Couldn't add send privileges to the exception port.");
  }

  // Save the existing original exception ports registered with the target
  // process (for later restoration while detaching from the process).
  result = task_get_exception_ports(gTask,
                                    EXC_MASK_ALL,
                                    exception_saved_state.saved_masks,
                                    &exception_saved_state.saved_exception_types_count,
                                    exception_saved_state.saved_ports,
                                    exception_saved_state.saved_behaviors,
                                    exception_saved_state.saved_flavors);

  if (result != KERN_SUCCESS) {
    print_error("attach: task_get_exception_ports() failed: '%s' (%d)\n",
                mach_error_string(result), result);
    THROW_NEW_DEBUGGER_EXCEPTION(
      "Can't attach to the process. Could not get the target exception ports.");
  }

  // register the exception port to be used for all future exceptions with the
  // target process.
  result = task_set_exception_ports(gTask,
                                    EXC_MASK_ALL,
                                    tgt_exception_port,
                                    EXCEPTION_DEFAULT | MACH_EXCEPTION_CODES,
                                    THREAD_STATE_NONE);

  if (result != KERN_SUCCESS) {
    print_error("attach: task_set_exception_ports() failed -- port 0x%x: '%s' (%d)\n",
                tgt_exception_port, mach_error_string(result), result);
    mach_port_deallocate(mach_task_self(), gTask);
    mach_port_deallocate(mach_task_self(), tgt_exception_port);
    THROW_NEW_DEBUGGER_EXCEPTION(
      "Can't attach to the process. Could not register the exception port "
      "with the target process.");
  }

  // use ptrace to stop the process
  // on os x, ptrace only needs to be called on the process, not the individual threads
  if (ptrace_attach(jpid) != true) {
    print_error("attach: ptrace failure in attaching to %d\n", (int)jpid);
    mach_port_deallocate(mach_task_self(), gTask);
    mach_port_deallocate(mach_task_self(), tgt_exception_port);
    THROW_NEW_DEBUGGER_EXCEPTION("Can't ptrace attach to the process");
  }

  if (wait_for_exception() != true) {
    mach_port_deallocate(mach_task_self(), gTask);
    mach_port_deallocate(mach_task_self(), tgt_exception_port);
    THROW_NEW_DEBUGGER_EXCEPTION(
      "Can't attach to the process. Issues with reception of the exception message.");
  }

  // suspend all the threads in the task
  result = task_suspend(gTask);
  if (result != KERN_SUCCESS) {
    print_error("attach: task_suspend() failed: '%s' (%d)\n",
                mach_error_string(result), result);
    mach_port_deallocate(mach_task_self(), gTask);
    mach_port_deallocate(mach_task_self(), tgt_exception_port);
    THROW_NEW_DEBUGGER_EXCEPTION("Can't attach. Unable to suspend all the threads in the task.");
  }

  id symbolicator = nil;
  id jrsSymbolicator = objc_lookUpClass("JRSSymbolicator");
  if (jrsSymbolicator != nil) {
    id (*dynamicCall)(id, SEL, pid_t) = (id (*)(id, SEL, pid_t))&objc_msgSend;
    symbolicator = dynamicCall(jrsSymbolicator, @selector(symbolicatorForPid:), (pid_t)jpid);
  }
  if (symbolicator != nil) {
    CFRetain(symbolicator); // pin symbolicator while in java heap
  }

  putSymbolicator(env, this_obj, symbolicator);
  if (symbolicator == nil) {
    mach_port_deallocate(mach_task_self(), gTask);
    mach_port_deallocate(mach_task_self(), tgt_exception_port);
    THROW_NEW_DEBUGGER_EXCEPTION("Can't attach symbolicator to the process");
  }

  JNI_COCOA_EXIT(env);
}

/** For core file,
    called from Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_attach0__Ljava_lang_String_2Ljava_lang_String_2 */
static void fillLoadObjects(JNIEnv* env, jobject this_obj, struct ps_prochandle* ph) {
  int n = 0, i = 0;
  jobject loadObjectList;

  loadObjectList = (*env)->GetObjectField(env, this_obj, loadObjectList_ID);
  CHECK_EXCEPTION;

  // add load objects
  n = get_num_libs(ph);
  for (i = 0; i < n; i++) {
     uintptr_t base, memsz;
     const char* name;
     jobject loadObject;
     jstring nameString;

     get_lib_addr_range(ph, i, &base, &memsz);
     name = get_lib_name(ph, i);
     nameString = (*env)->NewStringUTF(env, name);
     CHECK_EXCEPTION;
     loadObject = (*env)->CallObjectMethod(env, this_obj, createLoadObject_ID,
                                            nameString, (jlong)memsz, (jlong)base);
     CHECK_EXCEPTION;
     (*env)->CallBooleanMethod(env, loadObjectList, listAdd_ID, loadObject);
     CHECK_EXCEPTION;
     (*env)->DeleteLocalRef(env, nameString);
     (*env)->DeleteLocalRef(env, loadObject);
  }
}

/*
 * Class:     sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal
 * Method:    attach0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_attach0__Ljava_lang_String_2Ljava_lang_String_2(
  JNIEnv *env, jobject this_obj, jstring execName, jstring coreName)
{
  const char *execName_cstr;
  const char *coreName_cstr;
  jboolean isCopy;
  struct ps_prochandle* ph;

  execName_cstr = (*env)->GetStringUTFChars(env, execName, &isCopy);
  CHECK_EXCEPTION;
  coreName_cstr = (*env)->GetStringUTFChars(env, coreName, &isCopy);
  if ((*env)->ExceptionOccurred(env)) {
    (*env)->ReleaseStringUTFChars(env, execName, execName_cstr);
    return;
  }

  print_debug("attach: %s %s\n", execName_cstr, coreName_cstr);

  if ( (ph = Pgrab_core(execName_cstr, coreName_cstr)) == NULL) {
    (*env)->ReleaseStringUTFChars(env, execName, execName_cstr);
    (*env)->ReleaseStringUTFChars(env, coreName, coreName_cstr);
    THROW_NEW_DEBUGGER_EXCEPTION("Can't attach to the core file");
  }
  (*env)->SetLongField(env, this_obj, p_ps_prochandle_ID, (jlong)(intptr_t)ph);
  (*env)->ReleaseStringUTFChars(env, execName, execName_cstr);
  (*env)->ReleaseStringUTFChars(env, coreName, coreName_cstr);
  fillLoadObjects(env, this_obj, ph);
}

static void detach_cleanup(task_t gTask, JNIEnv *env, jobject this_obj, bool throw_exception) {
  mach_port_deallocate(mach_task_self(), tgt_exception_port);
  mach_port_deallocate(mach_task_self(), gTask);

  id symbolicator = getSymbolicator(env, this_obj);
  if (symbolicator != nil) {
    CFRelease(symbolicator);
  }
  if (throw_exception) {
    THROW_NEW_DEBUGGER_EXCEPTION("Cannot detach.");
  }
}

/*
 * Class:     sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal
 * Method:    detach0
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_jvm_hotspot_debugger_bsd_BsdDebuggerLocal_detach0(
  JNIEnv *env, jobject this_obj)
{
  print_debug("detach0 called\n");
  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  if (ph != NULL && ph->core != NULL) {
     Prelease(ph);
     return;
  }

  JNI_COCOA_ENTER(env);

  task_t gTask = getTask(env, this_obj);
  kern_return_t k_res = 0;

  // Restore the pre-saved original exception ports registered with the target process
  for (uint32_t i = 0; i < exception_saved_state.saved_exception_types_count; ++i) {
    k_res = task_set_exception_ports(gTask,
                                     exception_saved_state.saved_masks[i],
                                     exception_saved_state.saved_ports[i],
                                     exception_saved_state.saved_behaviors[i],
                                     exception_saved_state.saved_flavors[i]);
    if (k_res != KERN_SUCCESS) {
      print_error("detach: task_set_exception_ports failed with %d while "
                  "restoring the target exception ports.\n", k_res);
      detach_cleanup(gTask, env, this_obj, true);
    }
  }

  // detach from the ptraced process causing it to resume execution
  int pid;
  k_res = pid_for_task(gTask, &pid);
  if (k_res != KERN_SUCCESS) {
    print_error("detach: pid_for_task(%d) failed (%d)\n", pid, k_res);
    detach_cleanup(gTask, env, this_obj, true);
  }
  else {
    errno = 0;
    ptrace(PT_DETACH, pid, (caddr_t)1, 0);
    if (errno != 0) {
      print_error("detach: ptrace(PT_DETACH,...) failed: %s", strerror(errno));
      detach_cleanup(gTask, env, this_obj, true);
    }
  }

  // reply to the previous exception message
  k_res = mach_msg(&rep_msg.header,
                   MACH_SEND_MSG| MACH_SEND_INTERRUPT,
                   rep_msg.header.msgh_size,
                   0,
                   MACH_PORT_NULL,
                   MACH_MSG_TIMEOUT_NONE,
                   MACH_PORT_NULL);
  if (k_res != MACH_MSG_SUCCESS) {
    print_error("detach: mach_msg() for replying to pending exceptions failed: '%s' (%d)\n",
                 mach_error_string(k_res), k_res);
    detach_cleanup(gTask, env, this_obj, true);
  }

  detach_cleanup(gTask, env, this_obj, false);

  JNI_COCOA_EXIT(env);
}
