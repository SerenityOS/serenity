/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

// Disable CRT security warning against strcpy/strcat
#pragma warning(disable: 4996)

// this is source code windbg based SA debugger agent to debug
// Dr. Watson dump files and process snapshots.

#include "sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal.h"

#ifdef _M_IX86
  #include "sun_jvm_hotspot_debugger_x86_X86ThreadContext.h"
  #define NPRGREG sun_jvm_hotspot_debugger_x86_X86ThreadContext_NPRGREG
#elif _M_AMD64
  #include "sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext.h"
  #define NPRGREG sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext_NPRGREG
#elif _M_ARM64
  #include "sun_jvm_hotspot_debugger_aarch64_AARCH64ThreadContext.h"
  #define NPRGREG sun_jvm_hotspot_debugger_aarch64_AARCH64ThreadContext_NPRGREG
#else
  #error "SA windbg back-end is not supported for your cpu!"
#endif

#include <limits.h>
#include <windows.h>
#include <inttypes.h>

#define DEBUG_NO_IMPLEMENTATION
#include <dbgeng.h>
#include <dbghelp.h>


// Wrappers to simplify cleanup on errors.
namespace {

template <class T>
class AutoArrayPtr {
  T* m_ptr;
public:
  AutoArrayPtr(T* ptr) : m_ptr(ptr) {
  }

  ~AutoArrayPtr() {
    delete [] m_ptr;
  }

  operator T* () const {
    return m_ptr;
  }
};

// Manage COM 'auto' pointers to avoid multiple Release
// calls at every early (exception) returns.

template <class T>
class AutoCOMPtr {
  T* m_ptr;

public:
  AutoCOMPtr(T* ptr) : m_ptr(ptr) {
  }

  ~AutoCOMPtr() {
    if (m_ptr) {
      m_ptr->Release();
    }
  }

  T* operator->() const {
    return m_ptr;
  }
};

class AutoJavaString {
  JNIEnv* m_env;
  jstring m_str;
  const char* m_buf;

public:
  // check env->ExceptionOccurred() after ctor
  AutoJavaString(JNIEnv* env, jstring str)
    : m_env(env), m_str(str), m_buf(str == nullptr ? nullptr : env->GetStringUTFChars(str, nullptr)) {
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

class AutoJavaByteArray {
  JNIEnv* env;
  jbyteArray byteArray;
  jbyte* bytePtr;
  jint releaseMode;

public:
  // check env->ExceptionOccurred() after ctor
  AutoJavaByteArray(JNIEnv* env, jbyteArray byteArray, jint releaseMode = JNI_ABORT)
    : env(env), byteArray(byteArray), releaseMode(releaseMode),
      bytePtr(env->GetByteArrayElements(byteArray, nullptr)) {
  }

  ~AutoJavaByteArray() {
    if (bytePtr) {
      env->ReleaseByteArrayElements(byteArray, bytePtr, releaseMode);
    }
  }

  void setReleaseMode(jint mode) {
    releaseMode = mode;
  }

  operator jbyte* () const {
    return bytePtr;
  }
};

} // unnamed namespace


// field and method IDs we want here

static jfieldID imagePath_ID                    = 0;
static jfieldID symbolPath_ID                   = 0;
static jfieldID ptrIDebugClient_ID              = 0;
static jfieldID ptrIDebugControl_ID             = 0;
static jfieldID ptrIDebugDataSpaces_ID          = 0;
static jfieldID ptrIDebugOutputCallbacks_ID     = 0;
static jfieldID ptrIDebugAdvanced_ID            = 0;
static jfieldID ptrIDebugSymbols_ID             = 0;
static jfieldID ptrIDebugSystemObjects_ID       = 0;

static jmethodID addLoadObject_ID               = 0;
static jmethodID addThread_ID                   = 0;
static jmethodID createClosestSymbol_ID         = 0;
static jmethodID setThreadIntegerRegisterSet_ID = 0;

#define CHECK_EXCEPTION_(value) if (env->ExceptionOccurred()) { return value; }
#define CHECK_EXCEPTION if (env->ExceptionOccurred()) { return; }

#define THROW_NEW_DEBUGGER_EXCEPTION_(str, value) { \
                          throwNewDebuggerException(env, str); return value; }

#define THROW_NEW_DEBUGGER_EXCEPTION(str) { \
                          throwNewDebuggerException(env, str); return; }

static void throwNewDebuggerException(JNIEnv* env, const char* errMsg) {
  jclass clazz = env->FindClass("sun/jvm/hotspot/debugger/DebuggerException");
  CHECK_EXCEPTION;
  env->ThrowNew(clazz, errMsg);
}

// Verifies COM call result is S_OK, throws DebuggerException and exits otherwise.
// Note: other success results (like S_FALSE) are considered errors.
#define COM_VERIFY_OK_(v, str, retValue) \
  do { \
    const HRESULT hr = (v); \
    if (hr != S_OK) { \
      AutoArrayPtr<char> errmsg(new char[strlen(str) + 32]); \
      if (errmsg == nullptr) { \
        THROW_NEW_DEBUGGER_EXCEPTION_(str, retValue); \
      } else { \
        sprintf(errmsg, "%s (hr: 0x%08X)", str, hr); \
        THROW_NEW_DEBUGGER_EXCEPTION_(errmsg, retValue); \
      } \
    } \
  } while (false)

/*
 * Class:     sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal_initIDs
      (JNIEnv *env, jclass clazz) {
  imagePath_ID = env->GetStaticFieldID(clazz, "imagePath", "Ljava/lang/String;");
  CHECK_EXCEPTION;

  symbolPath_ID = env->GetStaticFieldID(clazz, "symbolPath", "Ljava/lang/String;");
  CHECK_EXCEPTION;

  ptrIDebugClient_ID = env->GetFieldID(clazz, "ptrIDebugClient", "J");
  CHECK_EXCEPTION;

  ptrIDebugControl_ID = env->GetFieldID(clazz, "ptrIDebugControl", "J");
  CHECK_EXCEPTION;

  ptrIDebugDataSpaces_ID = env->GetFieldID(clazz, "ptrIDebugDataSpaces", "J");
  CHECK_EXCEPTION;

  ptrIDebugOutputCallbacks_ID = env->GetFieldID(clazz, "ptrIDebugOutputCallbacks", "J");
  CHECK_EXCEPTION;

  ptrIDebugAdvanced_ID = env->GetFieldID(clazz, "ptrIDebugAdvanced", "J");
  CHECK_EXCEPTION;

  ptrIDebugSymbols_ID = env->GetFieldID(clazz, "ptrIDebugSymbols", "J");
  CHECK_EXCEPTION;

  ptrIDebugSystemObjects_ID = env->GetFieldID(clazz, "ptrIDebugSystemObjects", "J");
  CHECK_EXCEPTION;

  addLoadObject_ID = env->GetMethodID(clazz, "addLoadObject", "(Ljava/lang/String;JJ)V");
  CHECK_EXCEPTION;

  addThread_ID = env->GetMethodID(clazz, "addThread", "(J)V");
  CHECK_EXCEPTION;

  createClosestSymbol_ID = env->GetMethodID(clazz, "createClosestSymbol",
                            "(Ljava/lang/String;J)Lsun/jvm/hotspot/debugger/cdbg/ClosestSymbol;");
  CHECK_EXCEPTION;

  setThreadIntegerRegisterSet_ID = env->GetMethodID(clazz,
                                         "setThreadIntegerRegisterSet", "(J[J)V");
  CHECK_EXCEPTION;
}

// class for IDebugOutputCallbacks

class SAOutputCallbacks : public IDebugOutputCallbacks {
  LONG  m_refCount;
  char* m_msgBuffer;

public:
  SAOutputCallbacks() : m_refCount(1), m_msgBuffer(nullptr) {
  }

  ~SAOutputCallbacks() {
    clearBuffer();
  }

  const char* getBuffer() const {
    return m_msgBuffer;
  }

  void clearBuffer() {
    if (m_msgBuffer) {
      free(m_msgBuffer);
      m_msgBuffer = 0;
    }
  }

  STDMETHOD_(ULONG, AddRef)(THIS);
  STDMETHOD_(ULONG, Release)(THIS);
  STDMETHOD(QueryInterface)(THIS_
                            IN REFIID interfaceId,
                            OUT PVOID* ppInterface);
  STDMETHOD(Output)(THIS_
                    IN ULONG mask,
                    IN PCSTR msg);
};

STDMETHODIMP_(ULONG) SAOutputCallbacks::AddRef(THIS) {
  return InterlockedIncrement(&m_refCount);
}

STDMETHODIMP_(ULONG) SAOutputCallbacks::Release(THIS) {
  LONG retVal = InterlockedDecrement(&m_refCount);
  if (retVal == 0) {
    delete this;
  }
  return retVal;
}

STDMETHODIMP SAOutputCallbacks::QueryInterface(THIS_
                                          IN REFIID interfaceId,
                                          OUT PVOID* ppInterface) {
  *ppInterface = nullptr;
  if (IsEqualIID(interfaceId, __uuidof(IUnknown)) ||
      IsEqualIID(interfaceId, __uuidof(IDebugOutputCallbacks))) {
    *ppInterface = static_cast<IDebugOutputCallbacks*>(this);
  } else {
    return E_NOINTERFACE;
  }
  AddRef();
  return S_OK;
}

STDMETHODIMP SAOutputCallbacks::Output(THIS_
                                       IN ULONG mask,
                                       IN PCSTR msg) {
  size_t len = strlen(msg) + 1;
  if (m_msgBuffer == 0) {
    m_msgBuffer = (char*) malloc(len);
    if (m_msgBuffer == 0) {
      fprintf(stderr, "out of memory debugger output!\n");
      return S_FALSE;
    }
    strcpy(m_msgBuffer, msg);
  } else {
    char* newBuffer = (char*)realloc(m_msgBuffer, len + strlen(m_msgBuffer));
    if (newBuffer == nullptr) {
      // old m_msgBuffer buffer is still valid
      fprintf(stderr, "out of memory debugger output!\n");
      return S_FALSE;
    }
    m_msgBuffer = newBuffer;
    strcat(m_msgBuffer, msg);
  }
  return S_OK;
}

static bool getWindbgInterfaces(JNIEnv* env, jobject obj) {
  // get windbg interfaces ..

  IDebugClient* ptrIDebugClient = 0;
  COM_VERIFY_OK_(DebugCreate(__uuidof(IDebugClient), (PVOID*) &ptrIDebugClient),
                 "Windbg Error: not able to create IDebugClient object!", false);
  env->SetLongField(obj, ptrIDebugClient_ID, (jlong) ptrIDebugClient);

  IDebugControl* ptrIDebugControl = 0;
  COM_VERIFY_OK_(ptrIDebugClient->QueryInterface(
                    __uuidof(IDebugControl), (PVOID*) &ptrIDebugControl),
                 "Windbg Error: not able to get IDebugControl", false);
  env->SetLongField(obj, ptrIDebugControl_ID, (jlong) ptrIDebugControl);

  IDebugDataSpaces* ptrIDebugDataSpaces = 0;
  COM_VERIFY_OK_(ptrIDebugClient->QueryInterface(
                    __uuidof(IDebugDataSpaces), (PVOID*) &ptrIDebugDataSpaces),
                 "Windbg Error: not able to get IDebugDataSpaces object!", false);
  env->SetLongField(obj, ptrIDebugDataSpaces_ID, (jlong) ptrIDebugDataSpaces);

  SAOutputCallbacks* ptrIDebugOutputCallbacks = new SAOutputCallbacks();
  env->SetLongField(obj, ptrIDebugOutputCallbacks_ID, (jlong) ptrIDebugOutputCallbacks);
  CHECK_EXCEPTION_(false);

  IDebugAdvanced* ptrIDebugAdvanced = 0;
  COM_VERIFY_OK_(ptrIDebugClient->QueryInterface(
                    __uuidof(IDebugAdvanced), (PVOID*) &ptrIDebugAdvanced),
                 "Windbg Error: not able to get IDebugAdvanced object!", false);
  env->SetLongField(obj, ptrIDebugAdvanced_ID, (jlong) ptrIDebugAdvanced);

  IDebugSymbols* ptrIDebugSymbols = 0;
  COM_VERIFY_OK_(ptrIDebugClient->QueryInterface(
                    __uuidof(IDebugSymbols), (PVOID*) &ptrIDebugSymbols),
                 "Windbg Error: not able to get IDebugSymbols object!", false);
  env->SetLongField(obj, ptrIDebugSymbols_ID, (jlong) ptrIDebugSymbols);

  IDebugSystemObjects* ptrIDebugSystemObjects = 0;
  COM_VERIFY_OK_(ptrIDebugClient->QueryInterface(
                    __uuidof(IDebugSystemObjects), (PVOID*) &ptrIDebugSystemObjects),
                 "Windbg Error: not able to get IDebugSystemObjects object!", false);
  env->SetLongField(obj, ptrIDebugSystemObjects_ID, (jlong) ptrIDebugSystemObjects);

  return true;
}

static bool setImageAndSymbolPath(JNIEnv* env, jobject obj) {
  jclass clazz = env->GetObjectClass(obj);
  CHECK_EXCEPTION_(false);
  jstring path;

  path = (jstring) env->GetStaticObjectField(clazz, imagePath_ID);
  CHECK_EXCEPTION_(false);
  if (path == nullptr) {
     THROW_NEW_DEBUGGER_EXCEPTION_("Windbg Error: not able to get imagePath field ID!", false);
  }
  AutoJavaString imagePath(env, path);
  CHECK_EXCEPTION_(false);

  path = (jstring) env->GetStaticObjectField(clazz, symbolPath_ID);
  CHECK_EXCEPTION_(false);
  if (path == nullptr) {
     THROW_NEW_DEBUGGER_EXCEPTION_("Windbg Error: not able to get symbolPath field ID!", false);
  }
  AutoJavaString symbolPath(env, path);
  CHECK_EXCEPTION_(false);

  IDebugSymbols* ptrIDebugSymbols = (IDebugSymbols*)env->GetLongField(obj, ptrIDebugSymbols_ID);
  CHECK_EXCEPTION_(false);

  ptrIDebugSymbols->SetImagePath(imagePath);
  ptrIDebugSymbols->SetSymbolPath(symbolPath);
  return true;
}

static HRESULT WaitForEvent(IDebugControl *ptrIDebugControl) {
  HRESULT hr = ptrIDebugControl->WaitForEvent(DEBUG_WAIT_DEFAULT, INFINITE);
  // see JDK-8204994: sometimes WaitForEvent fails with E_ACCESSDENIED,
  // but succeeds on 2nd call.
  // To minimize possible noise retry 3 times.
  for (int i = 0; hr == E_ACCESSDENIED && i < 3; i++) {
    // yield current thread use of a processor (short delay).
    SwitchToThread();
    hr = ptrIDebugControl->WaitForEvent(DEBUG_WAIT_DEFAULT, INFINITE);
  }
  return hr;
}

static bool openDumpFile(JNIEnv* env, jobject obj, jstring coreFileName) {
  // open the dump file
  AutoJavaString coreFile(env, coreFileName);
  CHECK_EXCEPTION_(false);
  if (!setImageAndSymbolPath(env, obj)) {
     return false;
  }

  IDebugClient* ptrIDebugClient = (IDebugClient*)env->GetLongField(obj, ptrIDebugClient_ID);
  CHECK_EXCEPTION_(false);
  COM_VERIFY_OK_(ptrIDebugClient->OpenDumpFile(coreFile),
                 "Windbg Error: OpenDumpFile failed!", false);

  IDebugControl* ptrIDebugControl = (IDebugControl*)env->GetLongField(obj, ptrIDebugControl_ID);
  CHECK_EXCEPTION_(false);
  COM_VERIFY_OK_(WaitForEvent(ptrIDebugControl),
                 "Windbg Error: WaitForEvent failed!", false);

  return true;
}


static bool attachToProcess(JNIEnv* env, jobject obj, jint pid) {
  if (!setImageAndSymbolPath(env, obj)) {
     return false;
  }
  IDebugClient* ptrIDebugClient = (IDebugClient*)env->GetLongField(obj, ptrIDebugClient_ID);
  CHECK_EXCEPTION_(false);

  /***********************************************************************************

     We are attaching to a process in 'read-only' mode. i.e., we do not want to
     put breakpoints, suspend/resume threads etc. For read-only JDI and HSDB kind of
     usage this should suffice.

     Please refer to DEBUG_ATTACH_NONINVASIVE mode source comments from dbgeng.h.
     In this mode, debug engine does not call DebugActiveProrcess. i.e., we are not
     actually debugging at all. We can safely 'detach' from the process anytime
     we want and debuggee process is left as is on all Windows variants.

     This also makes JDI-on-SA installation/usage simpler because with this we would
     not need a tool like ServiceInstaller from http://www.kcmultimedia.com/smaster.

  ***********************************************************************************/


  COM_VERIFY_OK_(ptrIDebugClient->AttachProcess(0, pid, DEBUG_ATTACH_NONINVASIVE),
                 "Windbg Error: AttachProcess failed!", false);

  IDebugControl* ptrIDebugControl = (IDebugControl*) env->GetLongField(obj,
                                                     ptrIDebugControl_ID);
  CHECK_EXCEPTION_(false);
  COM_VERIFY_OK_(WaitForEvent(ptrIDebugControl),
                 "Windbg Error: WaitForEvent failed!", false);

  return true;
}


static bool addLoadObjects(JNIEnv* env, jobject obj) {
  IDebugSymbols* ptrIDebugSymbols = (IDebugSymbols*) env->GetLongField(obj,
                                                      ptrIDebugSymbols_ID);
  CHECK_EXCEPTION_(false);
  ULONG loaded = 0, unloaded = 0;
  COM_VERIFY_OK_(ptrIDebugSymbols->GetNumberModules(&loaded, &unloaded),
                 "Windbg Error: GetNumberModules failed!", false);

  AutoArrayPtr<DEBUG_MODULE_PARAMETERS> params(new DEBUG_MODULE_PARAMETERS[loaded]);

  if (params == nullptr) {
      THROW_NEW_DEBUGGER_EXCEPTION_("out of memory to allocate debug module params!", false);
  }

  COM_VERIFY_OK_(ptrIDebugSymbols->GetModuleParameters(loaded, nullptr, 0, params),
                 "Windbg Error: GetModuleParameters failed!", false);

  for (int u = 0; u < (int)loaded; u++) {
    TCHAR imageName[MAX_PATH];
    COM_VERIFY_OK_(ptrIDebugSymbols->GetModuleNames(DEBUG_ANY_ID, params[u].Base,
                                                    imageName, MAX_PATH, nullptr, nullptr,
                                                    0, nullptr, nullptr, 0, nullptr),
                   "Windbg Error: GetModuleNames failed!", false);

    jstring strName = env->NewStringUTF(imageName);
    CHECK_EXCEPTION_(false);
    env->CallVoidMethod(obj, addLoadObject_ID, strName, (jlong) params[u].Size,
                        (jlong) params[u].Base);
    CHECK_EXCEPTION_(false);
    env->DeleteLocalRef(strName);
  }

  return true;
}

static bool addThreads(JNIEnv* env, jobject obj) {
  IDebugSystemObjects* ptrIDebugSystemObjects = (IDebugSystemObjects*) env->GetLongField(obj,
                                                      ptrIDebugSystemObjects_ID);
  CHECK_EXCEPTION_(false);

  ULONG numThreads = 0;
  COM_VERIFY_OK_(ptrIDebugSystemObjects->GetNumberThreads(&numThreads),
                 "Windbg Error: GetNumberThreads failed!", false);

  AutoArrayPtr<ULONG> ptrSysThreadIds(new ULONG[numThreads]);

  if (ptrSysThreadIds == nullptr) {
     THROW_NEW_DEBUGGER_EXCEPTION_("out of memory to allocate thread ids!", false);
  }

  AutoArrayPtr<ULONG> ptrThreadIds(new ULONG[numThreads]);

  if (ptrThreadIds == nullptr) {
     THROW_NEW_DEBUGGER_EXCEPTION_("out of memory to allocate thread ids!", false);
  }

  COM_VERIFY_OK_(ptrIDebugSystemObjects->GetThreadIdsByIndex(0, numThreads,
                                      ptrThreadIds, ptrSysThreadIds),
                 "Windbg Error: GetThreadIdsByIndex failed!", false);


  IDebugAdvanced* ptrIDebugAdvanced = (IDebugAdvanced*) env->GetLongField(obj,
                                                      ptrIDebugAdvanced_ID);
  CHECK_EXCEPTION_(false);

  // for each thread, get register context and save it.
  for (ULONG t = 0; t < numThreads; t++) {
    COM_VERIFY_OK_(ptrIDebugSystemObjects->SetCurrentThreadId(ptrThreadIds[t]),
                   "Windbg Error: SetCurrentThread failed!", false);

    jlongArray regs = env->NewLongArray(NPRGREG);
    CHECK_EXCEPTION_(false);

    jlong* ptrRegs = env->GetLongArrayElements(regs, nullptr);
    CHECK_EXCEPTION_(false);

    // copy register values from the CONTEXT struct
    CONTEXT context;
    memset(&context, 0, sizeof(CONTEXT));

#undef REG_INDEX
#ifdef _M_IX86
    #define REG_INDEX(x) sun_jvm_hotspot_debugger_x86_X86ThreadContext_##x

    context.ContextFlags = CONTEXT_FULL | CONTEXT_DEBUG_REGISTERS;
    ptrIDebugAdvanced->GetThreadContext(&context, sizeof(CONTEXT));

    ptrRegs[REG_INDEX(GS)]  = context.SegGs;
    ptrRegs[REG_INDEX(FS)]  = context.SegFs;
    ptrRegs[REG_INDEX(ES)]  = context.SegEs;
    ptrRegs[REG_INDEX(DS)]  = context.SegDs;

    ptrRegs[REG_INDEX(EDI)] = context.Edi;
    ptrRegs[REG_INDEX(ESI)] = context.Esi;
    ptrRegs[REG_INDEX(EBX)] = context.Ebx;
    ptrRegs[REG_INDEX(EDX)] = context.Edx;
    ptrRegs[REG_INDEX(ECX)] = context.Ecx;
    ptrRegs[REG_INDEX(EAX)] = context.Eax;

    ptrRegs[REG_INDEX(FP)] = context.Ebp;
    ptrRegs[REG_INDEX(PC)] = context.Eip;
    ptrRegs[REG_INDEX(CS)]  = context.SegCs;
    ptrRegs[REG_INDEX(EFL)] = context.EFlags;
    ptrRegs[REG_INDEX(SP)] = context.Esp;
    ptrRegs[REG_INDEX(SS)]  = context.SegSs;

    ptrRegs[REG_INDEX(DR0)] = context.Dr0;
    ptrRegs[REG_INDEX(DR1)] = context.Dr1;
    ptrRegs[REG_INDEX(DR2)] = context.Dr2;
    ptrRegs[REG_INDEX(DR3)] = context.Dr3;
    ptrRegs[REG_INDEX(DR6)] = context.Dr6;
    ptrRegs[REG_INDEX(DR7)] = context.Dr7;

#elif _M_AMD64
    #define REG_INDEX(x) sun_jvm_hotspot_debugger_amd64_AMD64ThreadContext_##x

    context.ContextFlags = CONTEXT_FULL | CONTEXT_DEBUG_REGISTERS;
    ptrIDebugAdvanced->GetThreadContext(&context, sizeof(CONTEXT));

    // Segment Registers and processor flags
    ptrRegs[REG_INDEX(CS)]  = context.SegCs;
    ptrRegs[REG_INDEX(DS)]  = context.SegDs;
    ptrRegs[REG_INDEX(ES)]  = context.SegEs;
    ptrRegs[REG_INDEX(FS)]  = context.SegFs;
    ptrRegs[REG_INDEX(GS)]  = context.SegGs;
    ptrRegs[REG_INDEX(SS)]  = context.SegSs;
    ptrRegs[REG_INDEX(RFL)] = context.EFlags;

    // Integer registers
    ptrRegs[REG_INDEX(RDI)] = context.Rdi;
    ptrRegs[REG_INDEX(RSI)] = context.Rsi;
    ptrRegs[REG_INDEX(RAX)] = context.Rax;
    ptrRegs[REG_INDEX(RCX)] = context.Rcx;
    ptrRegs[REG_INDEX(RDX)] = context.Rdx;
    ptrRegs[REG_INDEX(RBX)] = context.Rbx;
    ptrRegs[REG_INDEX(RBP)] = context.Rbp;
    ptrRegs[REG_INDEX(RSP)] = context.Rsp;

    ptrRegs[REG_INDEX(R8)]  = context.R8;
    ptrRegs[REG_INDEX(R9)]  = context.R9;
    ptrRegs[REG_INDEX(R10)] = context.R10;
    ptrRegs[REG_INDEX(R11)] = context.R11;
    ptrRegs[REG_INDEX(R12)] = context.R12;
    ptrRegs[REG_INDEX(R13)] = context.R13;
    ptrRegs[REG_INDEX(R14)] = context.R14;
    ptrRegs[REG_INDEX(R15)] = context.R15;

    // Program counter
    ptrRegs[REG_INDEX(RIP)] = context.Rip;
#endif

    env->ReleaseLongArrayElements(regs, ptrRegs, JNI_COMMIT);
    CHECK_EXCEPTION_(false);

    env->CallVoidMethod(obj, setThreadIntegerRegisterSet_ID, (jlong)ptrThreadIds[t], regs);
    CHECK_EXCEPTION_(false);
    env->DeleteLocalRef(regs);

    ULONG sysId;
    COM_VERIFY_OK_(ptrIDebugSystemObjects->GetCurrentThreadSystemId(&sysId),
                   "Windbg Error: GetCurrentThreadSystemId failed!", false);

    env->CallVoidMethod(obj, addThread_ID, (jlong) sysId);
    CHECK_EXCEPTION_(false);
  }

  return true;
}

/*
 * Class:     sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal
 * Method:    attach0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal_attach0__Ljava_lang_String_2Ljava_lang_String_2
  (JNIEnv *env, jobject obj, jstring execName, jstring coreFileName) {

  if (!getWindbgInterfaces(env, obj)) {
     return;
  }

  if (!openDumpFile(env, obj, coreFileName)) {
     return;
  }

  if (!addLoadObjects(env, obj)) {
     return;
  }

  if (!addThreads(env, obj)) {
     return;
  }
}

/*
 * Class:     sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal
 * Method:    attach0
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal_attach0__I
  (JNIEnv *env, jobject obj, jint pid) {

  if (!getWindbgInterfaces(env, obj)) {
     return;
  }

  if (!attachToProcess(env, obj, pid)) {
     return;
  }

  if (!addLoadObjects(env, obj)) {
     return;
  }

  if (!addThreads(env, obj)) {
     return;
  }
}


#define RELEASE(fieldID) \
  do { \
    IUnknown* ptr = (IUnknown*)env->GetLongField(obj, fieldID); \
    CHECK_EXCEPTION_(false); \
    if (ptr) { \
      ptr->Release(); \
    } \
  } while (false)

static bool releaseWindbgInterfaces(JNIEnv* env, jobject obj) {
  RELEASE(ptrIDebugDataSpaces_ID);
  RELEASE(ptrIDebugOutputCallbacks_ID);
  RELEASE(ptrIDebugAdvanced_ID);
  RELEASE(ptrIDebugSymbols_ID);
  RELEASE(ptrIDebugSystemObjects_ID);
  RELEASE(ptrIDebugControl_ID);
  RELEASE(ptrIDebugClient_ID);

  return true;
}

/*
 * Class:     sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal
 * Method:    detach0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal_detach0
    (JNIEnv *env, jobject obj) {
  IDebugClient* ptrIDebugClient = (IDebugClient*) env->GetLongField(obj, ptrIDebugClient_ID);
  CHECK_EXCEPTION;
  ptrIDebugClient->DetachProcesses();
  releaseWindbgInterfaces(env, obj);
}


/*
 * Class:     sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal
 * Method:    readBytesFromProcess0
 * Signature: (JJ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal_readBytesFromProcess0
    (JNIEnv *env, jobject obj, jlong address, jlong numBytes) {
  jbyteArray byteArray = env->NewByteArray((jsize)numBytes);
  CHECK_EXCEPTION_(0);

  AutoJavaByteArray arrayBytes(env, byteArray);
  CHECK_EXCEPTION_(0);

  IDebugDataSpaces* ptrIDebugDataSpaces = (IDebugDataSpaces*) env->GetLongField(obj,
                                                       ptrIDebugDataSpaces_ID);
  CHECK_EXCEPTION_(0);

  ULONG bytesRead;
  const HRESULT hr = ptrIDebugDataSpaces->ReadVirtual((ULONG64)address, arrayBytes,
                                                      (ULONG)numBytes, &bytesRead);
  if (hr != S_OK || bytesRead != numBytes) {
     return 0;
  }

  arrayBytes.setReleaseMode(0);

  return byteArray;
}

/*
 * Class:     sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal
 * Method:    getThreadIdFromSysId0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal_getThreadIdFromSysId0
    (JNIEnv *env, jobject obj, jlong sysId) {
  IDebugSystemObjects* ptrIDebugSystemObjects = (IDebugSystemObjects*) env->GetLongField(obj,
                                                    ptrIDebugSystemObjects_ID);
  CHECK_EXCEPTION_(0);

  ULONG id = 0;
  HRESULT hr = ptrIDebugSystemObjects->GetThreadIdBySystemId((ULONG)sysId, &id);
  if (hr != S_OK) {
    // This is not considered fatal and does happen on occassion, usually with an
    // 0x80004002 "No such interface supported". The root cause is not fully understood,
    // but by ignoring this error and returning NULL, stacking walking code will get
    // null registers and fallback to using the "last java frame" if setup.
   printf("WARNING: GetThreadIdBySystemId failed with 0x%x for sysId (%" PRIu64 ")\n",
           hr, sysId);
    return -1;
  }
  return (jlong) id;
}

/*
 * Class:     sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal
 * Method:    consoleExecuteCommand0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal_consoleExecuteCommand0
    (JNIEnv *env, jobject obj, jstring cmd) {
  AutoJavaString command(env, cmd);
  CHECK_EXCEPTION_(0);

  IDebugClient* ptrIDebugClient = (IDebugClient*) env->GetLongField(obj, ptrIDebugClient_ID);
  CHECK_EXCEPTION_(0);

  IDebugClient*  tmpClientPtr = 0;
  COM_VERIFY_OK_(ptrIDebugClient->CreateClient(&tmpClientPtr),
                 "Windbg Error: CreateClient failed!", 0);
  AutoCOMPtr<IDebugClient> tmpClient(tmpClientPtr);

  IDebugControl* tmpControlPtr = 0;
  COM_VERIFY_OK_(tmpClient->QueryInterface(__uuidof(IDebugControl), (PVOID*) &tmpControlPtr),
                 "Windbg Error: QueryInterface (IDebugControl) failed", 0);
  AutoCOMPtr<IDebugControl> tmpControl(tmpControlPtr);

  SAOutputCallbacks* saOutputCallbacks = (SAOutputCallbacks*) env->GetLongField(obj,
                                                                   ptrIDebugOutputCallbacks_ID);
  CHECK_EXCEPTION_(0);

  saOutputCallbacks->clearBuffer();

  COM_VERIFY_OK_(tmpClient->SetOutputCallbacks(saOutputCallbacks),
                 "Windbg Error: SetOutputCallbacks failed!", 0);

  tmpControl->Execute(DEBUG_OUTPUT_VERBOSE, command, DEBUG_EXECUTE_DEFAULT);

  const char* output = saOutputCallbacks->getBuffer();
  if (output == 0) {
     output = "";
  }

  jstring res = env->NewStringUTF(output);
  saOutputCallbacks->clearBuffer();
  return res;
}

/*
 * Class:     sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal
 * Method:    lookupByName0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)J
 */

JNIEXPORT jlong JNICALL Java_sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal_lookupByName0
    (JNIEnv *env, jobject obj, jstring objName, jstring sym) {
  IDebugSymbols* ptrIDebugSymbols = (IDebugSymbols*)env->GetLongField(obj, ptrIDebugSymbols_ID);
  CHECK_EXCEPTION_(0);

  AutoJavaString name(env, sym);
  CHECK_EXCEPTION_(0);

  ULONG64 offset = 0L;
  if (strstr(name, "::") != 0) {
    ptrIDebugSymbols->AddSymbolOptions(SYMOPT_UNDNAME);
  } else {
    ptrIDebugSymbols->RemoveSymbolOptions(SYMOPT_UNDNAME);
  }
  if (ptrIDebugSymbols->GetOffsetByName(name, &offset) != S_OK) {
    return (jlong) 0;
  }
  return (jlong) offset;
}

#define SYMBOL_BUFSIZE 512
/*
 * Class:     sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal
 * Method:    lookupByAddress0
 * Signature: (J)Lsun/jvm/hotspot/debugger/cdbg/ClosestSymbol;
 */
JNIEXPORT jobject JNICALL Java_sun_jvm_hotspot_debugger_windbg_WindbgDebuggerLocal_lookupByAddress0
    (JNIEnv *env, jobject obj, jlong address) {
  IDebugSymbols* ptrIDebugSymbols = (IDebugSymbols*) env->GetLongField(obj, ptrIDebugSymbols_ID);
  CHECK_EXCEPTION_(0);

  ULONG64 disp = 0L;
  char buf[SYMBOL_BUFSIZE];
  memset(buf, 0, sizeof(buf));

  if (ptrIDebugSymbols->GetNameByOffset(address, buf, sizeof(buf), 0, &disp) != S_OK) {
    return 0;
  }

  jstring sym = env->NewStringUTF(buf);
  CHECK_EXCEPTION_(0);
  jobject res = env->CallObjectMethod(obj, createClosestSymbol_ID, sym, disp);
  CHECK_EXCEPTION_(0);
  return res;
}
