/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * The Toolkit class has two functions: it instantiates the AWT
 * ToolkitPeer's native methods, and provides the DLL's core functions.
 *
 * There are two ways this DLL can be used: either as a dynamically-
 * loaded Java native library from the interpreter, or by a Windows-
 * specific app.  The first manner requires that the Toolkit provide
 * all support needed so the app can function as a first-class Windows
 * app, while the second assumes that the app will provide that
 * functionality.  Which mode this DLL functions in is determined by
 * which initialization paradigm is used. If the Toolkit is constructed
 * normally, then the Toolkit will have its own pump. If it is explicitly
 * initialized for an embedded environment (via a static method on
 * sun.awt.windows.WToolkit), then it will rely on an external message
 * pump.
 *
 * The most basic functionality needed is a Windows message pump (also
 * known as a message loop).  When an Java app is started as a console
 * app by the interpreter, the Toolkit needs to provide that message
 * pump if the AWT is dynamically loaded.
 */

#ifndef AWT_TOOLKIT_H
#define AWT_TOOLKIT_H

#include "awt.h"
#include "awtmsg.h"
#include "Trace.h"

#include "sun_awt_windows_WToolkit.h"

class AwtObject;
class AwtDialog;
class AwtDropTarget;

typedef VOID (CALLBACK* IDLEPROC)(VOID);
typedef BOOL (CALLBACK* PEEKMESSAGEPROC)(MSG&);

// Struct for _WInputMethod_enable|disableNativeIME method
struct EnableNativeIMEStruct {
    jobject self;
    jobject peer;
    jint context;
    jboolean useNativeCompWindow;
};

/*
 * class JNILocalFrame
 * Push/PopLocalFrame helper
 */
class JNILocalFrame {
  public:
    INLINE JNILocalFrame(JNIEnv *env, int size) {
        m_env = env;
        int result = m_env->PushLocalFrame(size);
        if (result < 0) {
            DASSERT(FALSE);
            throw std::bad_alloc();
        }
    }
    INLINE ~JNILocalFrame() { m_env->PopLocalFrame(NULL); }
  private:
    JNIEnv* m_env;
};

/*
 * class CriticalSection
 * ~~~~~ ~~~~~~~~~~~~~~~~
 * Lightweight intra-process thread synchronization. Can only be used with
 * other critical sections, and only within the same process.
 */
class CriticalSection {
  public:
    INLINE  CriticalSection() { ::InitializeCriticalSection(&rep); }
    INLINE ~CriticalSection() { ::DeleteCriticalSection(&rep); }

    class Lock {
      public:
        INLINE Lock(const CriticalSection& cs) : critSec(cs) {
            (const_cast<CriticalSection &>(critSec)).Enter();
        }
        INLINE ~Lock() {
            (const_cast<CriticalSection &>(critSec)).Leave();
        }
      private:
        const CriticalSection& critSec;
    };
    friend class Lock;

  private:
    CRITICAL_SECTION rep;

    CriticalSection(const CriticalSection&);
    const CriticalSection& operator =(const CriticalSection&);

  public:
    virtual void Enter() {
        ::EnterCriticalSection(&rep);
    }
    virtual BOOL TryEnter() {
        return ::TryEnterCriticalSection(&rep);
    }
    virtual void Leave() {
        ::LeaveCriticalSection(&rep);
    }
};

// Macros for using CriticalSection objects that help trace
// lock/unlock actions

#define CRITICAL_SECTION_ENTER(cs) { \
    J2dTraceLn4(J2D_TRACE_VERBOSE2, \
                "CS.Wait:  tid, cs, file, line = 0x%x, 0x%x, %s, %d", \
                GetCurrentThreadId(), &(cs), __FILE__, __LINE__); \
    (cs).Enter(); \
    J2dTraceLn4(J2D_TRACE_VERBOSE2, \
                "CS.Enter: tid, cs, file, line = 0x%x, 0x%x, %s, %d", \
                GetCurrentThreadId(), &(cs), __FILE__, __LINE__); \
}

#define CRITICAL_SECTION_LEAVE(cs) { \
    J2dTraceLn4(J2D_TRACE_VERBOSE2, \
                "CS.Leave: tid, cs, file, line = 0x%x, 0x%x, %s, %d", \
                GetCurrentThreadId(), &(cs), __FILE__, __LINE__); \
    (cs).Leave(); \
    J2dTraceLn4(J2D_TRACE_VERBOSE2, \
                "CS.Left:  tid, cs, file, line = 0x%x, 0x%x, %s, %d", \
                GetCurrentThreadId(), &(cs), __FILE__, __LINE__); \
}

// Redefine WinAPI values related to touch input, if OS < Windows 7.
#if (!defined(WINVER) || ((WINVER) < 0x0601))
    /*
     * RegisterTouchWindow flag values
     */
    #define TWF_FINETOUCH       (0x00000001)
    #define TWF_WANTPALM        (0x00000002)

    #define WM_TOUCH                        0x0240

    /*
     * Touch input handle
     */
    typedef HANDLE HTOUCHINPUT;

    typedef struct tagTOUCHINPUT {
        LONG x;
        LONG y;
        HANDLE hSource;
        DWORD dwID;
        DWORD dwFlags;
        DWORD dwMask;
        DWORD dwTime;
        ULONG_PTR dwExtraInfo;
        DWORD cxContact;
        DWORD cyContact;
    } TOUCHINPUT, *PTOUCHINPUT;
    typedef TOUCHINPUT const * PCTOUCHINPUT;

    /*
     * Touch input flag values (TOUCHINPUT.dwFlags)
     */
    #define TOUCHEVENTF_MOVE            0x0001
    #define TOUCHEVENTF_DOWN            0x0002
    #define TOUCHEVENTF_UP              0x0004
    #define TOUCHEVENTF_INRANGE         0x0008
    #define TOUCHEVENTF_PRIMARY         0x0010
    #define TOUCHEVENTF_NOCOALESCE      0x0020
    #define TOUCHEVENTF_PEN             0x0040
    #define TOUCHEVENTF_PALM            0x0080
#endif

/************************************************************************
 * AwtToolkit class
 */

class AwtToolkit {
public:
    enum {
        KB_STATE_SIZE = 256
    };

    /* java.awt.Toolkit method ids */
    static jmethodID getDefaultToolkitMID;
    static jmethodID getFontMetricsMID;
    static jmethodID insetsMID;

    /* sun.awt.windows.WToolkit ids */
    static jmethodID windowsSettingChangeMID;
    static jmethodID displayChangeMID;

    static jmethodID userSessionMID;
    static jmethodID systemSleepMID;

    BOOL m_isDynamicLayoutSet;

    AwtToolkit();
    ~AwtToolkit();

    BOOL Initialize(BOOL localPump);
    BOOL Dispose();

    void SetDynamicLayout(BOOL dynamic);
    BOOL IsDynamicLayoutSet();
    BOOL IsDynamicLayoutSupported();
    BOOL IsDynamicLayoutActive();
    BOOL areExtraMouseButtonsEnabled();
    void setExtraMouseButtonsEnabled(BOOL enable);
    static UINT GetNumberOfButtons();

    bool IsWin8OrLater();
    bool IsTouchKeyboardAutoShowEnabled();
    bool IsAnyKeyboardAttached();
    bool IsTouchKeyboardAutoShowSystemEnabled();
    void ShowTouchKeyboard();
    void HideTouchKeyboard();
    BOOL TIRegisterTouchWindow(HWND hWnd, ULONG ulFlags);
    BOOL TIGetTouchInputInfo(HTOUCHINPUT hTouchInput,
        UINT cInputs, PTOUCHINPUT pInputs, int cbSize);
    BOOL TICloseTouchInputHandle(HTOUCHINPUT hTouchInput);

    LRESULT InvokeInputMethodFunction(UINT msg, WPARAM wParam=0, LPARAM lParam=0);

    INLINE BOOL localPump() { return m_localPump; }
    INLINE BOOL VerifyComponents() { return FALSE; } // TODO: Use new DebugHelper class to set this flag
    INLINE HWND GetHWnd() { return m_toolkitHWnd; }

    INLINE HMODULE GetModuleHandle() { return m_dllHandle; }
    INLINE void SetModuleHandle(HMODULE h) { m_dllHandle = h; }

    INLINE static DWORD MainThread() { return GetInstance().m_mainThreadId; }
    INLINE void VerifyActive() throw (awt_toolkit_shutdown) {
        if (!m_isActive && m_mainThreadId != ::GetCurrentThreadId()) {
            throw awt_toolkit_shutdown();
        }
    }
    INLINE BOOL IsDisposed() { return m_isDisposed; }
    static UINT GetMouseKeyState();
    static void GetKeyboardState(PBYTE keyboardState);

    static ATOM RegisterClass();
    static void UnregisterClass();
    INLINE LRESULT SendMessage(UINT msg, WPARAM wParam=0, LPARAM lParam=0) {
        if (!m_isDisposed) {
            return ::SendMessage(GetHWnd(), msg, wParam, lParam);
        } else {
            return NULL;
        }
    }
    static LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam,
                                    LPARAM lParam);
    static LRESULT CALLBACK GetMessageFilter(int code, WPARAM wParam,
                                             LPARAM lParam);
    static LRESULT CALLBACK ForegroundIdleFilter(int code, WPARAM wParam,
                                                 LPARAM lParam);
    static LRESULT CALLBACK MouseLowLevelHook(int code, WPARAM wParam,
            LPARAM lParam);

    INLINE static AwtToolkit& GetInstance() { return theInstance; }
    INLINE void SetPeer(JNIEnv *env, jobject wToolkit) {
        AwtToolkit &tk = AwtToolkit::GetInstance();
        if (tk.m_peer != NULL) {
            env->DeleteGlobalRef(tk.m_peer);
        }
        tk.m_peer = (wToolkit != NULL) ? env->NewGlobalRef(wToolkit) : NULL;
    }

    INLINE jobject GetPeer() {
        return m_peer;
    }

    // is this thread the main thread?

    INLINE static BOOL IsMainThread() {
        return GetInstance().m_mainThreadId == ::GetCurrentThreadId();
    }

    // post a message to the message pump thread

    INLINE BOOL PostMessage(UINT msg, WPARAM wp=0, LPARAM lp=0) {
        return ::PostMessage(GetHWnd(), msg, wp, lp);
    }

    // cause the message pump thread to call the function synchronously now!

    INLINE void * InvokeFunction(void*(*ftn)(void)) {
        return (void *)SendMessage(WM_AWT_INVOKE_VOID_METHOD, (WPARAM)ftn, 0);
    }
    INLINE void InvokeFunction(void (*ftn)(void)) {
        InvokeFunction((void*(*)(void))ftn);
    }
    INLINE void * InvokeFunction(void*(*ftn)(void *), void* param) {
        return (void *)SendMessage(WM_AWT_INVOKE_METHOD, (WPARAM)ftn,
                                   (LPARAM)param);
    }
    INLINE void InvokeFunction(void (*ftn)(void *), void* param) {
        InvokeFunction((void*(*)(void*))ftn, param);
    }

    INLINE CriticalSection &GetSyncCS() { return m_Sync; }

    void *SyncCall(void*(*ftn)(void *), void* param);
    void SyncCall(void (*ftn)(void *), void *param);
    void *SyncCall(void *(*ftn)(void));
    void SyncCall(void (*ftn)(void));

    // cause the message pump thread to call the function later ...

    INLINE void InvokeFunctionLater(void (*ftn)(void *), void* param) {
        if (!PostMessage(WM_AWT_INVOKE_METHOD, (WPARAM)ftn, (LPARAM)param)) {
            JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
            JNU_ThrowInternalError(env, "Message not posted, native event queue may be full.");
        }
    }

   // cause the message pump thread to synchronously synchronize on the handle

    INLINE void WaitForSingleObject(HANDLE handle) {
        SendMessage(WM_AWT_WAIT_FOR_SINGLE_OBJECT, 0, (LPARAM)handle);
    }

    /*
     * Create an AwtXxxx C++ component using a given factory
     */
    typedef void (*ComponentFactory)(void*, void*);
    static void CreateComponent(void* hComponent, void* hParent,
                                ComponentFactory compFactory, BOOL isParentALocalReference=TRUE);

    static void DestroyComponentHWND(HWND hwnd);

    // constants used to PostQuitMessage

    static const int EXIT_ENCLOSING_LOOP;
    static const int EXIT_ALL_ENCLOSING_LOOPS;

    // ...

    void QuitMessageLoop(int status);

    UINT MessageLoop(IDLEPROC lpIdleFunc, PEEKMESSAGEPROC lpPeekMessageFunc);
    BOOL PumpWaitingMessages(PEEKMESSAGEPROC lpPeekMessageFunc);
    void PumpToDestroy(class AwtComponent* p);
    void ProcessMsg(MSG& msg);
    BOOL PreProcessMsg(MSG& msg);
    BOOL PreProcessMouseMsg(class AwtComponent* p, MSG& msg);
    BOOL PreProcessKeyMsg(class AwtComponent* p, MSG& msg);

    /* Checks that an free ID exists. */
    jboolean isFreeIDAvailable();
    /* Create an ID which maps to an AwtObject pointer, such as a menu. */
    UINT CreateCmdID(AwtObject* object);

    // removes cmd id mapping
    void RemoveCmdID(UINT id);

    /* Return the AwtObject associated with its ID. */
    AwtObject* LookupCmdID(UINT id);

    /* Return the current application icon. */
    HICON GetAwtIcon();
    HICON GetAwtIconSm();

    // Calculate a wave-like value out of the integer 'value' and
    // the specified period.
    // The argument 'value' is an integer 0, 1, 2, ... *infinity*.
    //
    // Examples:
    //    Period == 3
    //    Generated sequence: 0 1 2 1 0 .....
    //
    //    Period == 4
    //    Generated sequence: 0 1 2 3 2 1 0 .....
    static inline UINT CalculateWave(UINT value, const UINT period) {
        if (period < 2) {
            return 0;
        }
        // -2 is necessary to avoid repeating extreme values (0 and period-1)
        value %= period * 2 -2;
        if (value >= period) {
            value = period * 2 -2 - value;
        }
        return value;
    }

    HICON GetSecurityWarningIcon(UINT index, UINT w, UINT h);

    /* Turns on/off dialog modality for the system. */
    INLINE AwtDialog* SetModal(AwtDialog* frame) {
        AwtDialog* previousDialog = m_pModalDialog;
        m_pModalDialog = frame;
        return previousDialog;
    };
    INLINE void ResetModal(AwtDialog* oldFrame) { m_pModalDialog = oldFrame; };
    INLINE BOOL IsModal() { return (m_pModalDialog != NULL); };
    INLINE AwtDialog* GetModalDialog(void) { return m_pModalDialog; };

    /* Stops the current message pump (normally a modal dialog pump) */
    INLINE void StopMessagePump() { m_breakOnError = TRUE; }

    /* Debug settings */
    INLINE void SetVerbose(long flag)   { m_verbose = (flag != 0); }
    INLINE void SetVerify(long flag)    { m_verifyComponents = (flag != 0); }
    INLINE void SetBreak(long flag)     { m_breakOnError = (flag != 0); }
    INLINE void SetHeapCheck(long flag);

    static void SetBusy(BOOL busy);

    /* Set and get the default input method Window handler. */
    INLINE void SetInputMethodWindow(HWND inputMethodHWnd) { m_inputMethodHWnd = inputMethodHWnd; }
    INLINE HWND GetInputMethodWindow() { return m_inputMethodHWnd; }

    static VOID CALLBACK PrimaryIdleFunc();
    static VOID CALLBACK SecondaryIdleFunc();
    static BOOL CALLBACK CommonPeekMessageFunc(MSG& msg);
    static BOOL activateKeyboardLayout(HKL hkl);

    HANDLE m_waitEvent;
    volatile DWORD eventNumber;
    volatile BOOL isInDoDragDropLoop;
private:
    HWND CreateToolkitWnd(LPCTSTR name);

    void InitTouchKeyboardExeFilePath();
    HWND GetTouchKeyboardWindow();

    BOOL m_localPump;
    DWORD m_mainThreadId;
    HWND m_toolkitHWnd;
    HWND m_inputMethodHWnd;
    BOOL m_verbose;
    BOOL m_isActive; // set to FALSE at beginning of Dispose
    BOOL m_isDisposed; // set to TRUE at end of Dispose
    BOOL m_areExtraMouseButtonsEnabled;

    typedef BOOL (WINAPI *RegisterTouchWindowFunc)(HWND hWnd, ULONG ulFlags);
    typedef BOOL (WINAPI *GetTouchInputInfoFunc)(HTOUCHINPUT hTouchInput,
        UINT cInputs, PTOUCHINPUT pInputs, int cbSize);
    typedef BOOL (WINAPI *CloseTouchInputHandleFunc)(HTOUCHINPUT hTouchInput);

    BOOL m_isWin8OrLater;
    BOOL m_touchKbrdAutoShowIsEnabled;
    TCHAR* m_touchKbrdExeFilePath;
    RegisterTouchWindowFunc m_pRegisterTouchWindow;
    GetTouchInputInfoFunc m_pGetTouchInputInfo;
    CloseTouchInputHandleFunc m_pCloseTouchInputHandle;

    BOOL m_vmSignalled; // set to TRUE if QUERYENDSESSION has successfully
                        // raised SIGTERM

    BOOL m_verifyComponents;
    BOOL m_breakOnError;

    BOOL  m_breakMessageLoop;
    UINT  m_messageLoopResult;

    class AwtComponent* m_lastMouseOver;
    BOOL                m_mouseDown;

    HHOOK m_hGetMessageHook;
    HHOOK m_hMouseLLHook;
    UINT_PTR  m_timer;

    class AwtCmdIDList* m_cmdIDs;
    BYTE                m_lastKeyboardState[KB_STATE_SIZE];
    CriticalSection     m_lockKB;

    static AwtToolkit theInstance;

    /* The current modal dialog frame (normally NULL). */
    AwtDialog* m_pModalDialog;

    /* The WToolkit peer instance */
    jobject m_peer;

    HMODULE m_dllHandle;  /* The module handle. */

    CriticalSection m_Sync;
    CriticalSection m_inputMethodLock;

    HANDLE m_inputMethodWaitEvent;
    LRESULT m_inputMethodData;

/* track display changes - used by palette-updating code.
   This is a workaround for a windows bug that prevents
   WM_PALETTECHANGED event from occurring immediately after
   a WM_DISPLAYCHANGED event.
  */
private:
    BOOL m_displayChanged;  /* Tracks displayChanged events */
    // 0 means we are not embedded.
    DWORD m_embedderProcessID;

public:
    BOOL HasDisplayChanged() { return m_displayChanged; }
    void ResetDisplayChanged() { m_displayChanged = FALSE; }
    void RegisterEmbedderProcessId(HWND);
    BOOL IsEmbedderProcessId(const DWORD processID) const
    {
        return m_embedderProcessID && (processID == m_embedderProcessID);
    }

 private:
    static JNIEnv *m_env;
    static DWORD m_threadId;
 public:
    static void SetEnv(JNIEnv *env);
    static JNIEnv* GetEnv();

    static BOOL GetScreenInsets(int screenNum, RECT * rect);

    // If the DWM is active, this function uses
    // DwmGetWindowAttribute()/DWMWA_EXTENDED_FRAME_BOUNDS.
    // Otherwise, fall back to regular ::GetWindowRect().
    // See 6711576 for more details.
    static void GetWindowRect(HWND hWnd, LPRECT lpRect);

 private:
    // The window handle of a toplevel window last seen under the mouse cursor.
    // See MouseLowLevelHook() for details.
    HWND m_lastWindowUnderMouse;
 public:
    HWND GetWindowUnderMouse() { return m_lastWindowUnderMouse; }

    void InstallMouseLowLevelHook();
    void UninstallMouseLowLevelHook();


/* AWT preloading (early Toolkit thread start)
 */
public:
    /* Toolkit preload action class.
     * Preload actions should be registered with
     * AwtToolkit::getInstance().GetPreloadThread().AddAction().
     * AwtToolkit thread calls InitImpl method at the beghining
     * and CleanImpl(false) before exiting for all registered actions.
     * If an application provides own Toolkit thread
     * (sun.awt.windows.WToolkit.embeddedInit), the thread calls Clean(true)
     * for each action.
     */
    class PreloadThread;    // forward declaration
    class PreloadAction {
        friend class PreloadThread;
    public:
        PreloadAction() : initThreadId(0), pNext(NULL) {}
        virtual ~PreloadAction() {}

    protected:
        // called by PreloadThread or as result
        // of EnsureInited() call (on Toolkit thread!).
        virtual void InitImpl() = 0;

        // called by PreloadThread (before exiting).
        // reInit == false: normal shutdown;
        // reInit == true: PreloadThread is shutting down due external
        //   Toolkit thread was provided.
        virtual void CleanImpl(bool reInit) = 0;

    public:
        // Initialized the action on the Toolkit thread if not yet initialized.
        bool EnsureInited();

        // returns thread ID which the action was inited on (0 if not inited)
        DWORD GetInitThreadID();

        // Allows to deinitialize action earlier.
        // The method must be called on the Toolkit thread only.
        // returns true on success,
        //         false if the action was inited on other thread.
        bool Clean();

    private:
        unsigned initThreadId;
        // lock for Init/Clean
        CriticalSection initLock;

        // Chain support (for PreloadThread)
        PreloadAction *pNext;   // for action chain used by PreloadThread
        void SetNext(PreloadAction *pNext) { this->pNext = pNext; }
        PreloadAction *GetNext() { return pNext; }

        // wrapper for AwtToolkit::InvokeFunction
        static void InitWrapper(void *param);

        void Init();
        void Clean(bool reInit);

    };

    /** Toolkit preload thread class.
     */
    class PreloadThread {
    public:
        PreloadThread();
        ~PreloadThread();

        // adds action & start the thread if not yet started
        bool AddAction(PreloadAction *pAction);

        // sets termination flag; returns true if the thread is running.
        // wrongThread specifies cause of the termination:
        //   false means termination on the application shutdown;
        // wrongThread is used as reInit parameter for action cleanup.
        bool Terminate(bool wrongThread);
        bool InvokeAndTerminate(void(_cdecl *fn)(void *), void *param);

        // waits for the thread completion;
        // use the method after Terminate() only if Terminate() returned true
        INLINE void Wait4Finish() {
            ::WaitForSingleObject(hFinished, INFINITE);
        }

        INLINE unsigned GetThreadId() {
            CriticalSection::Lock lock(threadLock);
            return threadId;
        }
        INLINE bool IsWrongThread() {
            CriticalSection::Lock lock(threadLock);
            return wrongThread;
        }
        // returns true if the current thread is "preload" thread
        bool OnPreloadThread();

    private:
        // data access lock
        CriticalSection threadLock;

        // the thread status
        enum Status {
            None = -1,      // initial
            Preloading = 0, // preloading in progress
            RunningToolkit, // Running as Toolkit thread
            Cleaning,       // exited from Toolkit thread proc, cleaning
            Finished        //
        } status;

        // "wrong thread" flag
        bool wrongThread;

        // thread proc (calls (this)param->ThreadProc())
        static unsigned WINAPI StaticThreadProc(void *param);
        unsigned ThreadProc();

        INLINE void AwakeThread() {
            ::SetEvent(hAwake);
        }

        // if threadId != 0 -> we are running
        unsigned threadId;
        // ThreadProc sets the event on exit
        HANDLE hFinished;
        // ThreadProc waits on the event for NewAction/Terminate/InvokeAndTerminate
        HANDLE hAwake;

        // function/param to invoke (InvokeAndTerminate)
        // if execFunc == NULL => just terminate
        void(_cdecl *execFunc)(void *);
        void *execParam;

        // action chain
        PreloadAction *pActionChain;
        PreloadAction *pLastProcessedAction;

        // returns next action in the list (NULL if no more actions)
        PreloadAction* GetNextAction();

    };

    INLINE PreloadThread& GetPreloadThread() { return preloadThread; }

private:
    PreloadThread preloadThread;

};


/*  creates an instance of T and assigns it to the argument, but only if
    the argument is initially NULL. Supposed to be thread-safe.
    returns the new value of the argument. I'm not using volatile here
    as InterlockedCompareExchange ensures volatile semantics
    and acquire/release.
    The function is useful when used with static POD NULL-initialized
    pointers, as they are guaranteed to be NULL before any dynamic
    initialization takes place. This function turns such a pointer
    into a thread-safe singleton, working regardless of dynamic
    initialization order. Destruction problem is not solved,
    we don't need it here.
*/

template<typename T> inline T* SafeCreate(T* &pArg) {
    /*  this implementation has no locks, it just destroys the object if it
        fails to be the first to init. another way would be using a special
        flag pointer value to mark the pointer as "being initialized". */
    T* pTemp = (T*)InterlockedCompareExchangePointer((void**)&pArg, NULL, NULL);
    if (pTemp != NULL) return pTemp;
    T* pNew = new T;
    pTemp = (T*)InterlockedCompareExchangePointer((void**)&pArg, pNew, NULL);
    if (pTemp != NULL) {
        // we failed it - another thread has already initialized pArg
        delete pNew;
        return pTemp;
    } else {
        return pNew;
    }
}

#endif /* AWT_TOOLKIT_H */
