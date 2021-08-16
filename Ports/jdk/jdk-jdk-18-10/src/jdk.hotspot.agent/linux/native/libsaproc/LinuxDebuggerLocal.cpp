/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2021, NTT DATA.
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

#include <jni.h>
#include "libproc.h"
#include "proc_service.h"

#include <elf.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <cxxabi.h>

#if defined(x86_64) && !defined(amd64)
#define amd64 1
#endif

#if defined(i386) && !defined(i586)
#define i586 1
#endif

#ifdef i586
#include "sun_jvm_hotspot_debugger_x86_X86ThreadContext.h"
#endif

#ifdef amd64
#include "sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext.h"
#endif

#if defined(ppc64) || defined(ppc64le)
#include "sun_jvm_hotspot_debugger_ppc64_PPC64ThreadContext.h"
#endif

#ifdef aarch64
#include "sun_jvm_hotspot_debugger_aarch64_AARCH64ThreadContext.h"
#endif

class AutoJavaString {
  JNIEnv* m_env;
  jstring m_str;
  const char* m_buf;

public:
  // check env->ExceptionOccurred() after ctor
  AutoJavaString(JNIEnv* env, jstring str)
    : m_env(env), m_str(str), m_buf(str == NULL ? NULL : env->GetStringUTFChars(str, NULL)) {
  }

  ~AutoJavaString() {
    if (m_buf) {
      m_env->ReleaseStringUTFChars(m_str, m_buf);
    }
  }

  operator const char* () const {
    return m_buf;
  }
};

static jfieldID p_ps_prochandle_ID = 0;
static jfieldID threadList_ID = 0;
static jfieldID loadObjectList_ID = 0;

static jmethodID createClosestSymbol_ID = 0;
static jmethodID createLoadObject_ID = 0;
static jmethodID getThreadForThreadId_ID = 0;
static jmethodID listAdd_ID = 0;

/*
 * SA_ALTROOT environment variable.
 * This memory holds env string for putenv(3).
 */
static char *saaltroot = NULL;

#define CHECK_EXCEPTION_(value) if (env->ExceptionOccurred()) { return value; }
#define CHECK_EXCEPTION if (env->ExceptionOccurred()) { return;}
#define THROW_NEW_DEBUGGER_EXCEPTION_(str, value) { throw_new_debugger_exception(env, str); return value; }
#define THROW_NEW_DEBUGGER_EXCEPTION(str) { throw_new_debugger_exception(env, str); return;}

void throw_new_debugger_exception(JNIEnv* env, const char* errMsg) {
  jclass clazz;
  clazz = env->FindClass("sun/jvm/hotspot/debugger/DebuggerException");
  CHECK_EXCEPTION;
  env->ThrowNew(clazz, errMsg);
}

struct ps_prochandle* get_proc_handle(JNIEnv* env, jobject this_obj) {
  jlong ptr = env->GetLongField(this_obj, p_ps_prochandle_ID);
  return (struct ps_prochandle*)(intptr_t)ptr;
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    init0
 * Signature: ()V
 */
extern "C"
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_init0
  (JNIEnv *env, jclass cls) {
  jclass listClass;

  if (init_libproc(getenv("LIBSAPROC_DEBUG") != NULL) != true) {
     THROW_NEW_DEBUGGER_EXCEPTION("can't initialize libproc");
  }

  // fields we use
  p_ps_prochandle_ID = env->GetFieldID(cls, "p_ps_prochandle", "J");
  CHECK_EXCEPTION;
  threadList_ID = env->GetFieldID(cls, "threadList", "Ljava/util/List;");
  CHECK_EXCEPTION;
  loadObjectList_ID = env->GetFieldID(cls, "loadObjectList", "Ljava/util/List;");
  CHECK_EXCEPTION;

  // methods we use
  createClosestSymbol_ID = env->GetMethodID(cls, "createClosestSymbol",
                    "(Ljava/lang/String;J)Lsun/jvm/hotspot/debugger/cdbg/ClosestSymbol;");
  CHECK_EXCEPTION;
  createLoadObject_ID = env->GetMethodID(cls, "createLoadObject",
                    "(Ljava/lang/String;JJ)Lsun/jvm/hotspot/debugger/cdbg/LoadObject;");
  CHECK_EXCEPTION;
  getThreadForThreadId_ID = env->GetMethodID(cls, "getThreadForThreadId",
                                                     "(J)Lsun/jvm/hotspot/debugger/ThreadProxy;");
  CHECK_EXCEPTION;
  // java.util.List method we call
  listClass = env->FindClass("java/util/List");
  CHECK_EXCEPTION;
  listAdd_ID = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
  CHECK_EXCEPTION;
}

extern "C"
JNIEXPORT jint JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_getAddressSize
  (JNIEnv *env, jclass cls)
{
#ifdef _LP64
 return 8;
#else
 return 4;
#endif

}


static void fillThreadsAndLoadObjects(JNIEnv* env, jobject this_obj, struct ps_prochandle* ph) {
  int n = 0, i = 0;

  // add threads
  n = get_num_threads(ph);
  for (i = 0; i < n; i++) {
    jobject thread;
    jobject threadList;
    lwpid_t lwpid;

    lwpid = get_lwp_id(ph, i);
    thread = env->CallObjectMethod(this_obj, getThreadForThreadId_ID, (jlong)lwpid);
    CHECK_EXCEPTION;
    threadList = env->GetObjectField(this_obj, threadList_ID);
    CHECK_EXCEPTION;
    env->CallBooleanMethod(threadList, listAdd_ID, thread);
    CHECK_EXCEPTION;
    env->DeleteLocalRef(thread);
    env->DeleteLocalRef(threadList);
  }

  // add load objects
  n = get_num_libs(ph);
  for (i = 0; i < n; i++) {
     uintptr_t base, memsz;
     const char* name;
     jobject loadObject;
     jobject loadObjectList;
     jstring str;

     get_lib_addr_range(ph, i, &base, &memsz);
     name = get_lib_name(ph, i);

     str = env->NewStringUTF(name);
     CHECK_EXCEPTION;
     loadObject = env->CallObjectMethod(this_obj, createLoadObject_ID, str, (jlong)memsz, (jlong)base);
     CHECK_EXCEPTION;
     loadObjectList = env->GetObjectField(this_obj, loadObjectList_ID);
     CHECK_EXCEPTION;
     env->CallBooleanMethod(loadObjectList, listAdd_ID, loadObject);
     CHECK_EXCEPTION;
     env->DeleteLocalRef(str);
     env->DeleteLocalRef(loadObject);
     env->DeleteLocalRef(loadObjectList);
  }
}


/*
 * Verify that a named ELF binary file (core or executable) has the same
 * bitness as ourselves.
 * Throw an exception if there is a mismatch or other problem.
 *
 * If we proceed using a mismatched debugger/debuggee, the best to hope
 * for is a missing symbol, the worst is a crash searching for debug symbols.
 */
void verifyBitness(JNIEnv *env, const char *binaryName) {
  int fd = open(binaryName, O_RDONLY);
  if (fd < 0) {
    THROW_NEW_DEBUGGER_EXCEPTION("cannot open binary file");
  }
  unsigned char elf_ident[EI_NIDENT];
  int i = read(fd, &elf_ident, sizeof(elf_ident));
  close(fd);

  if (i < 0) {
    THROW_NEW_DEBUGGER_EXCEPTION("cannot read binary file");
  }
#ifndef _LP64
  if (elf_ident[EI_CLASS] == ELFCLASS64) {
    THROW_NEW_DEBUGGER_EXCEPTION("debuggee is 64 bit, use 64-bit java for debugger");
  }
#else
  if (elf_ident[EI_CLASS] != ELFCLASS64) {
    THROW_NEW_DEBUGGER_EXCEPTION("debuggee is 32 bit, use 32 bit java for debugger");
  }
#endif
}


/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    setSAAltRoot0
 * Signature: (Ljava/lang/String;)V
 */
extern "C"
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_setSAAltRoot0
  (JNIEnv *env, jobject this_obj, jstring altroot) {
  if (saaltroot != NULL) {
    free(saaltroot);
  }
  const char *path = env->GetStringUTFChars(altroot, NULL);
  if (path == NULL) { return; }
  /*
   * `saaltroot` is used for putenv().
   * So we need to keep this memory.
   */
  static const char *PREFIX = "SA_ALTROOT=";
  size_t len = strlen(PREFIX) + strlen(path) + 1;
  saaltroot = (char *)malloc(len);
  snprintf(saaltroot, len, "%s%s", PREFIX, path);
  putenv(saaltroot);
  env->ReleaseStringUTFChars(altroot, path);
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    attach0
 * Signature: (I)V
 */
extern "C"
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_attach0__I
  (JNIEnv *env, jobject this_obj, jint jpid) {

  // For bitness checking, locate binary at /proc/jpid/exe
  char buf[PATH_MAX];
  snprintf((char *) &buf, PATH_MAX, "/proc/%d/exe", jpid);
  verifyBitness(env, (char *) &buf);
  CHECK_EXCEPTION;

  char err_buf[200];
  struct ps_prochandle* ph;
  if ((ph = Pgrab(jpid, err_buf, sizeof(err_buf))) == NULL) {
    char msg[230];
    snprintf(msg, sizeof(msg), "Can't attach to the process: %s", err_buf);
    THROW_NEW_DEBUGGER_EXCEPTION(msg);
  }
  env->SetLongField(this_obj, p_ps_prochandle_ID, (jlong)(intptr_t)ph);
  fillThreadsAndLoadObjects(env, this_obj, ph);
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    attach0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
extern "C"
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_attach0__Ljava_lang_String_2Ljava_lang_String_2
  (JNIEnv *env, jobject this_obj, jstring execName, jstring coreName) {
  struct ps_prochandle* ph;
  AutoJavaString execName_cstr(env, execName);
  CHECK_EXCEPTION;
  AutoJavaString coreName_cstr(env, coreName);
  CHECK_EXCEPTION;

  verifyBitness(env, execName_cstr);
  CHECK_EXCEPTION;

  if ( (ph = Pgrab_core(execName_cstr, coreName_cstr)) == NULL) {
    THROW_NEW_DEBUGGER_EXCEPTION("Can't attach to the core file");
  }
  env->SetLongField(this_obj, p_ps_prochandle_ID, (jlong)(intptr_t)ph);
  fillThreadsAndLoadObjects(env, this_obj, ph);
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    detach0
 * Signature: ()V
 */
extern "C"
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_detach0
  (JNIEnv *env, jobject this_obj) {
  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  if (ph != NULL) {
     Prelease(ph);
  }
  if (saaltroot != NULL) {
    free(saaltroot);
    saaltroot = NULL;
  }
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    lookupByName0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)J
 */
extern "C"
JNIEXPORT jlong JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_lookupByName0
  (JNIEnv *env, jobject this_obj, jstring objectName, jstring symbolName) {
  jlong addr;
  jboolean isCopy;
  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  // Note, objectName is ignored, and may in fact be NULL.
  // lookup_symbol will always search all objects/libs
  AutoJavaString objectName_cstr(env, objectName);
  CHECK_EXCEPTION_(0);
  AutoJavaString symbolName_cstr(env, symbolName);
  CHECK_EXCEPTION_(0);

  addr = (jlong) lookup_symbol(ph, NULL, symbolName_cstr);
  return addr;
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    lookupByAddress0
 * Signature: (J)Lsun/jvm/hotspot/debugger/cdbg/ClosestSymbol;
 */
extern "C"
JNIEXPORT jobject JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_lookupByAddress0
  (JNIEnv *env, jobject this_obj, jlong addr) {
  uintptr_t offset;
  jobject obj;
  jstring str;
  const char* sym = NULL;

  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  sym = symbol_for_pc(ph, (uintptr_t) addr, &offset);
  if (sym == NULL) return 0;
  str = env->NewStringUTF(sym);
  CHECK_EXCEPTION_(NULL);
  obj = env->CallObjectMethod(this_obj, createClosestSymbol_ID, str, (jlong)offset);
  CHECK_EXCEPTION_(NULL);
  return obj;
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    readBytesFromProcess0
 * Signature: (JJ)Lsun/jvm/hotspot/debugger/ReadResult;
 */
extern "C"
JNIEXPORT jbyteArray JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_readBytesFromProcess0
  (JNIEnv *env, jobject this_obj, jlong addr, jlong numBytes) {

  jboolean isCopy;
  jbyteArray array;
  jbyte *bufPtr;
  ps_err_e err;

  array = env->NewByteArray(numBytes);
  CHECK_EXCEPTION_(0);
  bufPtr = env->GetByteArrayElements(array, &isCopy);
  CHECK_EXCEPTION_(0);

  err = ps_pdread(get_proc_handle(env, this_obj), (psaddr_t) (uintptr_t)addr, bufPtr, numBytes);
  env->ReleaseByteArrayElements(array, bufPtr, 0);
  return (err == PS_OK)? array : 0;
}

#if defined(i586) || defined(amd64) || defined(ppc64) || defined(ppc64le) || defined(aarch64)
extern "C"
JNIEXPORT jlongArray JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_getThreadIntegerRegisterSet0
  (JNIEnv *env, jobject this_obj, jint lwp_id) {

  struct user_regs_struct gregs;
  jboolean isCopy;
  jlongArray array;
  jlong *regs;
  int i;

  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  if (get_lwp_regs(ph, lwp_id, &gregs) != true) {
    // This is not considered fatal and does happen on occassion, usually with an
    // ESRCH error. The root cause is not fully understood, but by ignoring this error
    // and returning NULL, stacking walking code will get null registers and fallback
    // to using the "last java frame" if setup.
    fprintf(stdout, "WARNING: getThreadIntegerRegisterSet0: get_lwp_regs failed for lwp (%d)\n", lwp_id);
    fflush(stdout);
    return NULL;
  }

#undef NPRGREG
#ifdef i586
#define NPRGREG sun_jvm_hotspot_debugger_x86_X86ThreadContext_NPRGREG
#endif
#ifdef amd64
#define NPRGREG sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext_NPRGREG
#endif
#ifdef aarch64
#define NPRGREG sun_jvm_hotspot_debugger_aarch64_AARCH64ThreadContext_NPRGREG
#endif
#if defined(ppc64) || defined(ppc64le)
#define NPRGREG sun_jvm_hotspot_debugger_ppc64_PPC64ThreadContext_NPRGREG
#endif


  array = env->NewLongArray(NPRGREG);
  CHECK_EXCEPTION_(0);
  regs = env->GetLongArrayElements(array, &isCopy);

#undef REG_INDEX

#ifdef i586
#define REG_INDEX(reg) sun_jvm_hotspot_debugger_x86_X86ThreadContext_##reg

  regs[REG_INDEX(GS)]  = (uintptr_t) gregs.xgs;
  regs[REG_INDEX(FS)]  = (uintptr_t) gregs.xfs;
  regs[REG_INDEX(ES)]  = (uintptr_t) gregs.xes;
  regs[REG_INDEX(DS)]  = (uintptr_t) gregs.xds;
  regs[REG_INDEX(EDI)] = (uintptr_t) gregs.edi;
  regs[REG_INDEX(ESI)] = (uintptr_t) gregs.esi;
  regs[REG_INDEX(FP)] = (uintptr_t) gregs.ebp;
  regs[REG_INDEX(SP)] = (uintptr_t) gregs.esp;
  regs[REG_INDEX(EBX)] = (uintptr_t) gregs.ebx;
  regs[REG_INDEX(EDX)] = (uintptr_t) gregs.edx;
  regs[REG_INDEX(ECX)] = (uintptr_t) gregs.ecx;
  regs[REG_INDEX(EAX)] = (uintptr_t) gregs.eax;
  regs[REG_INDEX(PC)] = (uintptr_t) gregs.eip;
  regs[REG_INDEX(CS)]  = (uintptr_t) gregs.xcs;
  regs[REG_INDEX(SS)]  = (uintptr_t) gregs.xss;

#endif /* i586 */

#ifdef amd64
#define REG_INDEX(reg) sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext_##reg

  regs[REG_INDEX(R15)] = gregs.r15;
  regs[REG_INDEX(R14)] = gregs.r14;
  regs[REG_INDEX(R13)] = gregs.r13;
  regs[REG_INDEX(R12)] = gregs.r12;
  regs[REG_INDEX(RBP)] = gregs.rbp;
  regs[REG_INDEX(RBX)] = gregs.rbx;
  regs[REG_INDEX(R11)] = gregs.r11;
  regs[REG_INDEX(R10)] = gregs.r10;
  regs[REG_INDEX(R9)] = gregs.r9;
  regs[REG_INDEX(R8)] = gregs.r8;
  regs[REG_INDEX(RAX)] = gregs.rax;
  regs[REG_INDEX(RCX)] = gregs.rcx;
  regs[REG_INDEX(RDX)] = gregs.rdx;
  regs[REG_INDEX(RSI)] = gregs.rsi;
  regs[REG_INDEX(RDI)] = gregs.rdi;
  regs[REG_INDEX(RIP)] = gregs.rip;
  regs[REG_INDEX(CS)] = gregs.cs;
  regs[REG_INDEX(RSP)] = gregs.rsp;
  regs[REG_INDEX(SS)] = gregs.ss;
  regs[REG_INDEX(FSBASE)] = gregs.fs_base;
  regs[REG_INDEX(GSBASE)] = gregs.gs_base;
  regs[REG_INDEX(DS)] = gregs.ds;
  regs[REG_INDEX(ES)] = gregs.es;
  regs[REG_INDEX(FS)] = gregs.fs;
  regs[REG_INDEX(GS)] = gregs.gs;

#endif /* amd64 */

#if defined(aarch64)

#define REG_INDEX(reg) sun_jvm_hotspot_debugger_aarch64_AARCH64ThreadContext_##reg

  {
    int i;
    for (i = 0; i < 31; i++)
      regs[i] = gregs.regs[i];
    regs[REG_INDEX(SP)] = gregs.sp;
    regs[REG_INDEX(PC)] = gregs.pc;
  }
#endif /* aarch64 */

#if defined(ppc64) || defined(ppc64le)
#define REG_INDEX(reg) sun_jvm_hotspot_debugger_ppc64_PPC64ThreadContext_##reg

  regs[REG_INDEX(LR)] = gregs.link;
  regs[REG_INDEX(NIP)] = gregs.nip;
  regs[REG_INDEX(R0)]  = gregs.gpr[0];
  regs[REG_INDEX(R1)]  = gregs.gpr[1];
  regs[REG_INDEX(R2)]  = gregs.gpr[2];
  regs[REG_INDEX(R3)]  = gregs.gpr[3];
  regs[REG_INDEX(R4)]  = gregs.gpr[4];
  regs[REG_INDEX(R5)]  = gregs.gpr[5];
  regs[REG_INDEX(R6)]  = gregs.gpr[6];
  regs[REG_INDEX(R7)]  = gregs.gpr[7];
  regs[REG_INDEX(R8)]  = gregs.gpr[8];
  regs[REG_INDEX(R9)]  = gregs.gpr[9];
  regs[REG_INDEX(R10)] = gregs.gpr[10];
  regs[REG_INDEX(R11)] = gregs.gpr[11];
  regs[REG_INDEX(R12)] = gregs.gpr[12];
  regs[REG_INDEX(R13)] = gregs.gpr[13];
  regs[REG_INDEX(R14)] = gregs.gpr[14];
  regs[REG_INDEX(R15)] = gregs.gpr[15];
  regs[REG_INDEX(R16)] = gregs.gpr[16];
  regs[REG_INDEX(R17)] = gregs.gpr[17];
  regs[REG_INDEX(R18)] = gregs.gpr[18];
  regs[REG_INDEX(R19)] = gregs.gpr[19];
  regs[REG_INDEX(R20)] = gregs.gpr[20];
  regs[REG_INDEX(R21)] = gregs.gpr[21];
  regs[REG_INDEX(R22)] = gregs.gpr[22];
  regs[REG_INDEX(R23)] = gregs.gpr[23];
  regs[REG_INDEX(R24)] = gregs.gpr[24];
  regs[REG_INDEX(R25)] = gregs.gpr[25];
  regs[REG_INDEX(R26)] = gregs.gpr[26];
  regs[REG_INDEX(R27)] = gregs.gpr[27];
  regs[REG_INDEX(R28)] = gregs.gpr[28];
  regs[REG_INDEX(R29)] = gregs.gpr[29];
  regs[REG_INDEX(R30)] = gregs.gpr[30];
  regs[REG_INDEX(R31)] = gregs.gpr[31];

#endif

  env->ReleaseLongArrayElements(array, regs, JNI_COMMIT);
  return array;
}
#endif

/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    demangle
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
extern "C"
JNIEXPORT jstring JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_demangle
  (JNIEnv *env, jobject this_obj, jstring jsym) {
  int status;
  jstring result = NULL;

  const char *sym = env->GetStringUTFChars(jsym, NULL);
  if (sym == NULL) {
    THROW_NEW_DEBUGGER_EXCEPTION_("Error getting symbol string", NULL);
  }
  char *demangled = abi::__cxa_demangle(sym, NULL, 0, &status);
  env->ReleaseStringUTFChars(jsym, sym);
  if ((demangled != NULL) && (status == 0)) {
    result = env->NewStringUTF(demangled);
    free(demangled);
  } else if (status == -2) { // not C++ ABI mangling rules - maybe C style
    result = jsym;
  } else {
    THROW_NEW_DEBUGGER_EXCEPTION_("Could not demangle", NULL);
  }

  return result;
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal
 * Method:    findLibPtrByAddress0
 * Signature: (J)J
 */
extern "C"
JNIEXPORT jlong JNICALL Java_sun_jvm_hotspot_debugger_linux_LinuxDebuggerLocal_findLibPtrByAddress0
  (JNIEnv *env, jobject this_obj, jlong pc) {
  struct ps_prochandle* ph = get_proc_handle(env, this_obj);
  return reinterpret_cast<jlong>(find_lib_by_address(ph, pc));
}
