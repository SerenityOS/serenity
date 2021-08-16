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

#ifndef AWT_COMPONENT_H
#define AWT_COMPONENT_H

#include "awtmsg.h"
#include "awt_Object.h"
#include "awt_Font.h"
#include "awt_Brush.h"
#include "awt_Pen.h"
#include "awt_Win32GraphicsDevice.h"
#include "GDIWindowSurfaceData.h"

#include "java_awt_Component.h"
#include "sun_awt_windows_WComponentPeer.h"
#include "java_awt_event_KeyEvent.h"
#include "java_awt_event_MouseEvent.h"
#include "java_awt_event_WindowEvent.h"
#include "java_awt_Dimension.h"

extern LPCTSTR szAwtComponentClassName;

static LPCTSTR DrawingStateProp = TEXT("SunAwtDrawingStateProp");

const UINT IGNORE_KEY = (UINT)-1;
const UINT MAX_ACP_STR_LEN = 7; // ANSI CP identifiers are no longer than this

#define LEFT_BUTTON 1
#define MIDDLE_BUTTON 2
#define RIGHT_BUTTON 4
#define DBL_CLICK 8
#define X1_BUTTON 16
#define X2_BUTTON 32

#ifndef MK_XBUTTON1
#define MK_XBUTTON1         0x0020
#endif

#ifndef MK_XBUTTON2
#define MK_XBUTTON2         0x0040
#endif

// combination of standard mouse button flags
const int ALL_MK_BUTTONS = MK_LBUTTON|MK_MBUTTON|MK_RBUTTON;
const int X_BUTTONS = MK_XBUTTON1|MK_XBUTTON2;

// The allowable difference between coordinates of the WM_TOUCH event and the
// corresponding WM_LBUTTONDOWN/WM_LBUTTONUP event letting to associate these
// events, when their coordinates are slightly different.
const int TOUCH_MOUSE_COORDS_DELTA = 10;

// Whether to check for embedded frame and adjust location
#define CHECK_EMBEDDED 0
#define DONT_CHECK_EMBEDDED 1

class AwtPopupMenu;

class AwtDropTarget;

/*
 * Message routing codes
 */
enum MsgRouting {
    mrPassAlong,    /* pass along to next in chain */
    mrDoDefault,    /* skip right to underlying default behavior */
    mrConsume,      /* consume msg & terminate routing immediatly,
                     * don't pass anywhere
                     */
};

/************************************************************************
 * AwtComponent class
 */

class AwtComponent : public AwtObject {
public:
    /* java.awt.Component fields and method IDs */
    static jfieldID peerID;
    static jfieldID xID;
    static jfieldID yID;
    static jfieldID widthID;
    static jfieldID heightID;
    static jfieldID visibleID;
    static jfieldID backgroundID;
    static jfieldID foregroundID;
    static jfieldID enabledID;
    static jfieldID parentID;
    static jfieldID cursorID;
    static jfieldID graphicsConfigID;
    static jfieldID peerGCID;
    static jfieldID focusableID;
    static jfieldID appContextID;
    static jfieldID hwndID;

    static jmethodID getFontMID;
    static jmethodID getToolkitMID;
    static jmethodID isEnabledMID;
    static jmethodID getLocationOnScreenMID;
    static jmethodID replaceSurfaceDataMID;
    static jmethodID replaceSurfaceDataLaterMID;
    static jmethodID disposeLaterMID;

    static const UINT WmAwtIsComponent;
    static jint * masks; //InputEvent mask array
    AwtComponent();
    virtual ~AwtComponent();

    /*
     * Dynamic class registration & creation
     */
    virtual LPCTSTR GetClassName() = 0;
    /*
     * Fix for 4964237: Win XP: Changing theme changes java dialogs title icon
     * WNDCLASS structure has been superseded by the WNDCLASSEX in Win32
     */
    virtual void FillClassInfo(WNDCLASSEX *lpwc);
    virtual void RegisterClass();
    virtual void UnregisterClass();

    virtual void CreateHWnd(JNIEnv *env, LPCWSTR title,
                    DWORD windowStyle, DWORD windowExStyle,
                    int x, int y, int w, int h,
                    HWND hWndParent, HMENU hMenu,
                    COLORREF colorForeground, COLORREF colorBackground,
                    jobject peer);
    virtual void DestroyHWnd();
    void InitPeerGraphicsConfig(JNIEnv *env, jobject peer);

    virtual void Dispose();

    void UpdateBackground(JNIEnv *env, jobject target);

    virtual void SubclassHWND();
    virtual void UnsubclassHWND();

    static LRESULT CALLBACK WndProc(HWND hWnd, UINT message,
        WPARAM wParam, LPARAM lParam);

    /*
     * Access to the various objects of this aggregate component
     */
    INLINE HWND GetHWnd() { return m_hwnd; }
    INLINE void SetHWnd(HWND hwnd) { m_hwnd = hwnd; }

    static AwtComponent* GetComponent(HWND hWnd);

    /*
     * Access to the properties of the component
     */
    INLINE COLORREF GetColor() { return m_colorForeground; }
    virtual void SetColor(COLORREF c);
    HPEN GetForegroundPen();

    COLORREF GetBackgroundColor();
    virtual void SetBackgroundColor(COLORREF c);
    HBRUSH GetBackgroundBrush();
    INLINE BOOL IsBackgroundColorSet() { return m_backgroundColorSet; }

    virtual void SetFont(AwtFont *pFont);

    INLINE void SetText(LPCTSTR text) { ::SetWindowText(GetHWnd(), text); }
    INLINE int GetText(LPTSTR buffer, int size) {
        return ::GetWindowText(GetHWnd(), buffer, size);
    }
    INLINE int GetTextLength() { return ::GetWindowTextLength(GetHWnd()); }

    virtual void GetInsets(RECT* rect) {
        VERIFY(::SetRectEmpty(rect));
    }

    BOOL IsVisible() { return m_visible;};

    HDC GetDCFromComponent();

    /*
     * Enable/disable component
     */
    virtual void Enable(BOOL bEnable);

    /*
     * Validate and call handleExpose on rects of UpdateRgn
     */
    void PaintUpdateRgn(const RECT *insets);

    static HWND GetTopLevelParentForWindow(HWND hwndDescendant);

    static jobject FindHeavyweightUnderCursor(BOOL useCache);

    /*
     * Returns the parent component.  If no parent window, or the
     * parent window isn't an AwtComponent, returns NULL.
     */
    AwtComponent* GetParent();

    /* Get the component's immediate container. Note: may return NULL while
       the component is being reparented in full-screen mode by Direct3D */
    class AwtWindow* GetContainer();

    /* Is a component a container? Used by above method */
    virtual BOOL IsContainer() { return FALSE;} // Plain components can't

    /**
     * Returns TRUE if this message will trigger native focus change, FALSE otherwise.
     */
    virtual BOOL IsFocusingKeyMessage(MSG *pMsg);
    virtual BOOL IsFocusingMouseMessage(MSG *pMsg);

    BOOL IsFocusable();

    /*
     * Returns an increasing unsigned value used for child control IDs.
     * There is no attempt to reclaim command ID's.
     */
    INLINE UINT CreateControlID() { return m_nextControlID++; }

    // returns the current keyboard layout
    INLINE static HKL GetKeyboardLayout() {
        return m_hkl;
    }

    // returns the current code page that should be used in
    // all MultiByteToWideChar and WideCharToMultiByte calls.
    // This code page should also be use in IsDBCSLeadByteEx.
    INLINE static UINT GetCodePage()
    {
        return m_CodePage;
    }

// Added by waleed for BIDI Support
    // returns the right to left status
    INLINE static BOOL GetRTLReadingOrder() {
        return sm_rtlReadingOrder;
    }
    // returns the right to left status
    INLINE static BOOL GetRTL() {
        return sm_rtl;
    }
    // returns the current sub language
    INLINE static LANGID GetSubLanguage() {
        return SUBLANGID(m_idLang);
    }
// end waleed

    // returns the current input language
    INLINE static LANGID GetInputLanguage()
    {
        return m_idLang;
    }
    // Convert Language ID to CodePage
    static UINT LangToCodePage(LANGID idLang);

    /*
     * methods on this component
     */
    virtual int GetScreenImOn();
    virtual void Show();
    virtual void Hide();
    virtual void Reshape(int x, int y, int w, int h);
    void ReshapeNoScale(int x, int y, int w, int h);

    /*
     * Fix for 4046446.
     * Component size/position helper, for the values above the short int limit.
     */
    static BOOL SetWindowPos(HWND wnd, HWND after,
                             int x, int y, int w, int h, UINT flags);

    /*
     * Sets the scrollbar values.  'bar' can be either SB_VERT or
     * SB_HORZ.  'min', 'value', and 'max' can have the value INT_MAX
     * which means that the value should not be changed.
     */
    void SetScrollValues(UINT bar, int min, int value, int max);

    INLINE LRESULT SendMessage(UINT msg, WPARAM wParam=0, LPARAM lParam=0) {
        DASSERT(GetHWnd());
        return ::SendMessage(GetHWnd(), msg, wParam, lParam);
    }

    void PostUngrabEvent();

    INLINE virtual LONG GetStyle() {
        DASSERT(GetHWnd());
        return ::GetWindowLong(GetHWnd(), GWL_STYLE);
    }
    INLINE virtual void SetStyle(LONG style) {
        DASSERT(GetHWnd());
        // SetWindowLong() error handling as recommended by Win32 API doc.
        ::SetLastError(0);
        DWORD ret = ::SetWindowLong(GetHWnd(), GWL_STYLE, style);
        DASSERT(ret != 0 || ::GetLastError() == 0);
    }
    INLINE virtual LONG GetStyleEx() {
        DASSERT(GetHWnd());
        return ::GetWindowLong(GetHWnd(), GWL_EXSTYLE);
    }
    INLINE virtual void SetStyleEx(LONG style) {
        DASSERT(GetHWnd());
        // SetWindowLong() error handling as recommended by Win32 API doc.
        ::SetLastError(0);
        DWORD ret = ::SetWindowLong(GetHWnd(), GWL_EXSTYLE, style);
        DASSERT(ret != 0 || ::GetLastError() == 0);
    }

    virtual BOOL NeedDblClick() { return FALSE; }

    /* for multifont component */
    static void DrawWindowText(HDC hDC, jobject font, jstring text,
                               int x, int y);
    static void DrawGrayText(HDC hDC, jobject font, jstring text,
                             int x, int y);

    void DrawListItem(JNIEnv *env, DRAWITEMSTRUCT &drawInfo);

    void MeasureListItem(JNIEnv *env, MEASUREITEMSTRUCT &measureInfo);

    jstring GetItemString(JNIEnv *env, jobject target, jint index);

    jint GetFontHeight(JNIEnv *env);

    virtual jobject PreferredItemSize(JNIEnv *env) {DASSERT(FALSE); return NULL; }

    INLINE BOOL isEnabled() {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        if (env->EnsureLocalCapacity(2) < 0) {
            return NULL;
        }
        jobject self = GetPeer(env);
        jobject target = env->GetObjectField(self, AwtObject::targetID);
        BOOL e = env->CallBooleanMethod(target, AwtComponent::isEnabledMID);
        DASSERT(!safe_ExceptionOccurred(env));

        env->DeleteLocalRef(target);

        return e;
    }

    INLINE BOOL isRecursivelyEnabled() {
        AwtComponent* p = this;
        do {
            if (!p->isEnabled()) {
                return FALSE;
            }
        } while (!p->IsTopLevel() &&
            (p = p->GetParent()) != NULL);
        return TRUE;
    }

    void SendKeyEventToFocusOwner(jint id, jlong when, jint raw, jint cooked,
                                  jint modifiers, jint keyLocation, jlong nativeCode,
                                  MSG *msg = NULL);
    /*
     * Allocate and initialize a new java.awt.event.KeyEvent, and
     * post it to the peer's target object.  No response is expected
     * from the target.
     */
    void SendKeyEvent(jint id, jlong when, jint raw, jint cooked,
                      jint modifiers, jint keyLocation, jlong nativeCode,
                      MSG *msg = NULL);

    /*
     * Allocate and initialize a new java.awt.event.MouseEvent, and
     * post it to the peer's target object.  No response is expected
     * from the target.
     */
    void SendMouseEvent(jint id, jlong when, jint x, jint y,
                        jint modifiers, jint clickCount,
                        jboolean popupTrigger, jint button = 0,
                        MSG *msg = NULL, BOOL causedByTouchEvent = FALSE);

    /*
     * Allocate and initialize a new java.awt.event.MouseWheelEvent, and
     * post it to the peer's target object.  No response is expected
     * from the target.
     */
    void SendMouseWheelEvent(jint id, jlong when, jint x, jint y,
                             jint modifiers, jint clickCount,
                             jboolean popupTrigger, jint scrollType,
                             jint scrollAmount, jint wheelRotation,
                             jdouble preciseWheelRotation, MSG *msg = NULL);

    /*
     * Allocate and initialize a new java.awt.event.FocusEvent, and
     * post it to the peer's target object.  No response is expected
     * from the target.
     */
    void SendFocusEvent(jint id, HWND opposite);

    /* Forward a filtered event directly to the subclassed window.
       synthetic should be TRUE iff the message was generated because
       of a synthetic Java event, rather than a native event. */
    virtual MsgRouting HandleEvent(MSG *msg, BOOL synthetic);

    /* Post a WM_AWT_HANDLE_EVENT message which invokes HandleEvent
       on the toolkit thread. This method may pre-filter the messages. */
    virtual BOOL PostHandleEventMessage(MSG *msg, BOOL synthetic);

    /* Event->message synthesizer methods. */
    void SynthesizeKeyMessage(JNIEnv *env, jobject keyEvent);
    void SynthesizeMouseMessage(JNIEnv *env, jobject mouseEvent);

    /* Components which inherit native mouse wheel behavior will
     * return TRUE.  These are TextArea, Choice, FileDialog, and
     * List.  All other Components return FALSE.
     */
    virtual BOOL InheritsNativeMouseWheelBehavior();

    /* Determines whether the component is obscured by another window */
    // Called on Toolkit thread
    static jboolean _IsObscured(void *param);

    /* Invalidate the specified rectangle. */
    virtual void Invalidate(RECT* r);

    /* Begin and end deferred window positioning. */
    virtual void BeginValidate();
    virtual void EndValidate();

    /* Keyboard conversion routines. */
    static void InitDynamicKeyMapTable();
    static void BuildDynamicKeyMapTable();
    static jint GetJavaModifiers();
    static jint GetActionModifiers();
    static jint GetButton(int mouseButton);
    static UINT GetButtonMK(int mouseButton);
    static UINT WindowsKeyToJavaKey(UINT windowsKey, UINT modifiers, UINT character, BOOL isDeadKey);
    static void JavaKeyToWindowsKey(UINT javaKey, UINT *windowsKey, UINT *modifiers, UINT originalWindowsKey);
    static void UpdateDynPrimaryKeymap(UINT wkey, UINT jkeyLegacy, jint keyLocation, UINT modifiers);

    INLINE static void JavaKeyToWindowsKey(UINT javaKey,
                                       UINT *windowsKey, UINT *modifiers)
    {
        JavaKeyToWindowsKey(javaKey, windowsKey, modifiers, IGNORE_KEY);
    }

    enum TransOps {NONE, LOAD, SAVE};

    UINT WindowsKeyToJavaChar(UINT wkey, UINT modifiers, TransOps ops, BOOL &isDeadKey);

    /* routines used for input method support */
    void SetInputMethod(jobject im, BOOL useNativeCompWindow);
    void SendInputMethodEvent(jint id, jstring text, int cClause,
                              int *rgClauseBoundary, jstring *rgClauseReading,
                              int cAttrBlock, int *rgAttrBoundary,
                              BYTE *rgAttrValue, int commitedTextLength,
                              int caretPos, int visiblePos);
    void InquireCandidatePosition();
    INLINE LPARAM GetCandidateType() { return m_bitsCandType; }
    HWND ImmGetHWnd();
    HIMC ImmAssociateContext(HIMC himc);
    HWND GetProxyFocusOwner();

    INLINE HWND GetProxyToplevelContainer() {
        HWND proxyHWnd = GetProxyFocusOwner();
        return ::GetAncestor(proxyHWnd, GA_ROOT); // a browser in case of EmbeddedFrame
    }

    void CallProxyDefWindowProc(UINT message,
                                WPARAM wParam,
                                LPARAM lParam,
                                LRESULT &retVal,
                                MsgRouting &mr);

    /*
     * Windows message handler functions
     */
    virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
    virtual LRESULT DefWindowProc(UINT msg, WPARAM wParam, LPARAM lParam);

    /* return true if msg is processed */
    virtual MsgRouting PreProcessMsg(MSG& msg);

    virtual MsgRouting WmCreate() {return mrDoDefault;}
    virtual MsgRouting WmClose() {return mrDoDefault;}
    virtual MsgRouting WmDestroy();
    virtual MsgRouting WmNcDestroy();

    virtual MsgRouting WmActivate(UINT nState, BOOL fMinimized, HWND opposite)
    {
        return mrDoDefault;
    }

    virtual MsgRouting WmEraseBkgnd(HDC hDC, BOOL& didErase)
    {
        return mrDoDefault;
    }

    virtual MsgRouting WmPaint(HDC hDC);
    virtual MsgRouting WmGetMinMaxInfo(LPMINMAXINFO lpmmi);
    virtual MsgRouting WmMove(int x, int y);
    virtual MsgRouting WmSize(UINT type, int w, int h);
    virtual MsgRouting WmSizing();
    virtual MsgRouting WmShowWindow(BOOL show, UINT status);
    virtual MsgRouting WmSetFocus(HWND hWndLost);
    virtual MsgRouting WmKillFocus(HWND hWndGot);
    virtual MsgRouting WmCtlColor(HDC hDC, HWND hCtrl,
                                  UINT ctlColor, HBRUSH& retBrush);
    virtual MsgRouting WmHScroll(UINT scrollCode, UINT pos, HWND hScrollBar);
    virtual MsgRouting WmVScroll(UINT scrollCode, UINT pos, HWND hScrollBar);

    virtual MsgRouting WmMouseEnter(UINT flags, int x, int y);
    virtual MsgRouting WmMouseDown(UINT flags, int x, int y, int button);
    virtual MsgRouting WmMouseUp(UINT flags, int x, int y, int button);
    virtual MsgRouting WmMouseMove(UINT flags, int x, int y);
    virtual MsgRouting WmMouseExit(UINT flags, int x, int y);
    virtual MsgRouting WmMouseWheel(UINT flags, int x, int y,
                                    int wheelRotation, BOOL isHorizontal);
    virtual MsgRouting WmNcMouseDown(WPARAM hitTest, int x, int y, int button);
    virtual MsgRouting WmNcMouseUp(WPARAM hitTest, int x, int y, int button);
    virtual MsgRouting WmWindowPosChanging(LPARAM windowPos);
    virtual MsgRouting WmWindowPosChanged(LPARAM windowPos);
    virtual void WmTouch(WPARAM wParam, LPARAM lParam);

    // NB: 64-bit: vkey is wParam of the message, but other API's take
    // vkey parameters of type UINT, so we do the cast before dispatching.
    virtual MsgRouting WmKeyDown(UINT vkey, UINT repCnt, UINT flags, BOOL system);
    virtual MsgRouting WmKeyUp(UINT vkey, UINT repCnt, UINT flags, BOOL system);

    virtual MsgRouting WmChar(UINT character, UINT repCnt, UINT flags, BOOL system);
    virtual MsgRouting WmIMEChar(UINT character, UINT repCnt, UINT flags, BOOL system);
    virtual MsgRouting WmInputLangChange(UINT charset, HKL hKeyBoardLayout);
    virtual MsgRouting WmForwardChar(WCHAR character, LPARAM lParam,
                                     BOOL synthethic);
    virtual MsgRouting WmPaste();

    virtual void SetCompositionWindow(RECT &r);
    virtual void OpenCandidateWindow(int x, int y);
    virtual void SetCandidateWindow(int iCandType, int x, int y);
    virtual MsgRouting WmImeSetContext(BOOL fSet, LPARAM *lplParam);
    virtual MsgRouting WmImeNotify(WPARAM subMsg, LPARAM bitsCandType);
    virtual MsgRouting WmImeStartComposition();
    virtual MsgRouting WmImeEndComposition();
    virtual MsgRouting WmImeComposition(WORD wChar, LPARAM flags);

    virtual MsgRouting WmTimer(UINT_PTR timerID) {return mrDoDefault;}

    virtual MsgRouting WmCommand(UINT id, HWND hWndCtrl, UINT notifyCode);

    /* reflected WmCommand from parent */
    virtual MsgRouting WmNotify(UINT notifyCode);

    virtual MsgRouting WmCompareItem(UINT /*ctrlId*/,
                                     COMPAREITEMSTRUCT &compareInfo,
                                     LRESULT &result);
    virtual MsgRouting WmDeleteItem(UINT /*ctrlId*/,
                                    DELETEITEMSTRUCT &deleteInfo);
    virtual MsgRouting WmDrawItem(UINT ctrlId,
                                  DRAWITEMSTRUCT &drawInfo);
    virtual MsgRouting WmMeasureItem(UINT ctrlId,
                                     MEASUREITEMSTRUCT &measureInfo);
    /* Fix 4181790 & 4223341 : These functions get overridden in owner-drawn
     * components instead of the Wm... versions.
     */
    virtual MsgRouting OwnerDrawItem(UINT ctrlId,
                                     DRAWITEMSTRUCT &drawInfo);
    virtual MsgRouting OwnerMeasureItem(UINT ctrlId,
                                        MEASUREITEMSTRUCT &measureInfo);

    virtual MsgRouting WmPrint(HDC hDC, LPARAM flags);
    virtual MsgRouting WmPrintClient(HDC hDC, LPARAM flags);

    virtual MsgRouting WmNcCalcSize(BOOL fCalcValidRects,
                                    LPNCCALCSIZE_PARAMS lpncsp,
                                    LRESULT &retVal);
    virtual MsgRouting WmNcPaint(HRGN hrgn);
    virtual MsgRouting WmNcHitTest(UINT x, UINT y, LRESULT &retVal);
    virtual MsgRouting WmSysCommand(UINT uCmdType, int xPos, int yPos);
    virtual MsgRouting WmEnterSizeMove();
    virtual MsgRouting WmExitSizeMove();
    virtual MsgRouting WmEnterMenuLoop(BOOL isTrackPopupMenu);
    virtual MsgRouting WmExitMenuLoop(BOOL isTrackPopupMenu);

    virtual MsgRouting WmQueryNewPalette(LRESULT &retVal);
    virtual MsgRouting WmPaletteChanged(HWND hwndPalChg);
    virtual MsgRouting WmPaletteIsChanging(HWND hwndPalChg);
    virtual MsgRouting WmStyleChanged(int wStyleType, LPSTYLESTRUCT lpss);
    virtual MsgRouting WmSettingChange(UINT wFlag, LPCTSTR pszSection);

    virtual MsgRouting WmContextMenu(HWND hCtrl, UINT xPos, UINT yPos) {
        return mrDoDefault;
    }

    void UpdateColorModel();

    jintArray CreatePrintedPixels(SIZE &loc, SIZE &size, int alpha);

    /*
     * HWND, AwtComponent and Java Peer interaction
     *
     * Link the C++, Java peer, and HWNDs together.
     */
    void LinkObjects(JNIEnv *env, jobject peer);

    void UnlinkObjects();

    static BOOL QueryNewPaletteCalled() { return m_QueryNewPaletteCalled; }

#ifdef DEBUG
    virtual void VerifyState(); /* verify component and peer are in sync. */
#else
    void VerifyState() {}       /* no-op */
#endif

    virtual AwtDropTarget* CreateDropTarget(JNIEnv* env);
    virtual void DestroyDropTarget();

    INLINE virtual HWND GetDBCSEditHandle() { return NULL; }
    // State for native drawing API
    INLINE jint GetDrawState() { return GetDrawState(m_hwnd); }
    INLINE void SetDrawState(jint state) { SetDrawState(m_hwnd, state); }    // State for native drawing API

    INLINE virtual BOOL IsTopLevel() { return FALSE; }
    INLINE virtual BOOL IsEmbeddedFrame() { return FALSE; }
    INLINE virtual BOOL IsScrollbar() { return FALSE; }

    static INLINE BOOL IsTopLevelHWnd(HWND hwnd) {
        AwtComponent *comp = AwtComponent::GetComponent(hwnd);
        return (comp != NULL && comp->IsTopLevel());
    }
    static INLINE BOOL IsEmbeddedFrameHWnd(HWND hwnd) {
        AwtComponent *comp = AwtComponent::GetComponent(hwnd);
        return (comp != NULL && comp->IsEmbeddedFrame());
    }

    static jint GetDrawState(HWND hwnd);
    static void SetDrawState(HWND hwnd, jint state);

    static HWND GetHWnd(JNIEnv* env, jobject target);

    static MSG* CreateMessage(UINT message, WPARAM wParam, LPARAM lParam, int x, int y);
    static void InitMessage(MSG* msg, UINT message, WPARAM wParam, LPARAM lParam, int x, int y);

    // Some methods to be called on Toolkit thread via Toolkit.InvokeFunction()
    static void _Show(void *param);
    static void _Hide(void *param);
    static void _Enable(void *param);
    static void _Disable(void *param);
    static jobject _GetLocationOnScreen(void *param);
    static void _Reshape(void *param);
    static void _ReshapeNoCheck(void *param);
    static void _NativeHandleEvent(void *param);
    static void _SetForeground(void *param);
    static void _SetBackground(void *param);
    static void _SetFont(void *param);
    static void _Start(void *param);
    static void _BeginValidate(void *param);
    static void _EndValidate(void *param);
    static void _UpdateWindow(void *param);
    static jlong _AddNativeDropTarget(void *param);
    static void _RemoveNativeDropTarget(void *param);
    static jintArray _CreatePrintedPixels(void *param);
    static jboolean _NativeHandlesWheelScrolling(void *param);
    static void _SetParent(void * param);
    static void _SetRectangularShape(void *param);
    static void _SetZOrder(void *param);

    static HWND sm_focusOwner;

private:
    static HWND sm_focusedWindow;

public:
    static inline HWND GetFocusedWindow() { return sm_focusedWindow; }
    static void SetFocusedWindow(HWND window);

    static void _SetFocus(void *param);

    static void *SetNativeFocusOwner(void *self);
    static void *GetNativeFocusedWindow();
    static void *GetNativeFocusOwner();

    static BOOL sm_inSynthesizeFocus;

    // Execute on Toolkit only.
    INLINE static LRESULT SynthesizeWmSetFocus(HWND targetHWnd, HWND oppositeHWnd) {
        sm_inSynthesizeFocus = TRUE;
        LRESULT res = ::SendMessage(targetHWnd, WM_SETFOCUS, (WPARAM)oppositeHWnd, 0);
        sm_inSynthesizeFocus = FALSE;
        return res;
    }
    // Execute on Toolkit only.
    INLINE static LRESULT SynthesizeWmKillFocus(HWND targetHWnd, HWND oppositeHWnd) {
        sm_inSynthesizeFocus = TRUE;
        LRESULT res = ::SendMessage(targetHWnd, WM_KILLFOCUS, (WPARAM)oppositeHWnd, 0);
        sm_inSynthesizeFocus = FALSE;
        return res;
    }

    static BOOL sm_bMenuLoop;
    static INLINE BOOL isMenuLoopActive() {
        return sm_bMenuLoop;
    }

    // when this component is being destroyed, this method is called
    // to find out if there are any messages being processed, and if
    // there are some then disposal of this component is postponed
    virtual BOOL CanBeDeleted() {
        return m_MessagesProcessing == 0;
    }

    BOOL IsDestroyPaused() const {
        return m_bPauseDestroy;
    }

protected:
    static AwtComponent* GetComponentImpl(HWND hWnd);

    static int GetClickCount();

    HWND     m_hwnd;
    UINT     m_myControlID;     /* its own ID from the view point of parent */
    BOOL     m_backgroundColorSet;
    BOOL     m_visible;         /* copy of Component.visible */

    static BOOL sm_suppressFocusAndActivation;
    static BOOL sm_restoreFocusAndActivation;

    /*
     * The function sets the focus-restore flag ON/OFF.
     * When the flag is ON, focus is restored immidiately after the proxy loses it.
     * All focus messages are suppressed. It's also assumed that sm_focusedWindow and
     * sm_focusOwner don't change after the flag is set ON and before it's set OFF.
     */
    static INLINE void SetRestoreFocus(BOOL doSet) {
        sm_suppressFocusAndActivation = doSet;
        sm_restoreFocusAndActivation = doSet;
    }

    virtual void SetDragCapture(UINT flags);
    virtual void ReleaseDragCapture(UINT flags);

    virtual void FillBackground(HDC hMemoryDC, SIZE &size);
    virtual void FillAlpha(void *bitmapBits, SIZE &size, BYTE alpha);

    int ScaleUpX(int x);
    int ScaleUpAbsX(int x);
    int ScaleUpY(int y);
    int ScaleUpAbsY(int y);
    int ScaleDownX(int x);
    int ScaleDownAbsX(int x);
    int ScaleDownY(int y);
    int ScaleDownAbsY(int y);

private:
    /* A bitmask keeps the button's numbers as MK_LBUTTON, MK_MBUTTON, MK_RBUTTON
     * which are allowed to
     * generate the CLICK event after the RELEASE has happened.
     * There are conditions that must be true for that sending CLICK event:
     * 1) button was initially PRESSED
     * 2) no movement or drag has happened until RELEASE
    */
    UINT m_mouseButtonClickAllowed;

    BOOL m_touchDownOccurred;
    BOOL m_touchUpOccurred;
    POINT m_touchDownPoint;
    POINT m_touchUpPoint;

    BOOL m_bSubclassed;
    BOOL m_bPauseDestroy;

    COLORREF m_colorForeground;
    COLORREF m_colorBackground;

    AwtPen*  m_penForeground;
    AwtBrush* m_brushBackground;

    WNDPROC  m_DefWindowProc;
    // counter for messages being processed by this component
    UINT     m_MessagesProcessing;

    // provides a unique ID for child controls
    UINT     m_nextControlID;

    // DeferWindowPos handle for batched-up window positioning
    HDWP     m_hdwp;
    // Counter to handle nested calls to Begin/EndValidate
    UINT     m_validationNestCount;

    AwtDropTarget* m_dropTarget; // associated DropTarget object

    // When we process WM_INPUTLANGCHANGE we remember the keyboard
    // layout handle and associated input language and codepage.
    // We also invalidate VK translation table for VK_OEM_* codes
    static HKL    m_hkl;
    static UINT   m_CodePage;
    static LANGID m_idLang;

    static BOOL sm_rtl;
    static BOOL sm_rtlReadingOrder;

    static BOOL sm_PrimaryDynamicTableBuilt;

    jobject m_InputMethod;
    BOOL    m_useNativeCompWindow;
    LPARAM  m_bitsCandType;
    UINT    m_PendingLeadByte;

    void SetComponentInHWND();

    // Determines whether a given virtual key is on the numpad
    static BOOL IsNumPadKey(UINT vkey, BOOL extended);

    // Determines the keyLocation of a given key
    static jint GetKeyLocation(UINT wkey, UINT flags);
    static jint GetShiftKeyLocation(UINT wkey, UINT flags);

    // Cache for FindComponent
    static HWND sm_cursorOn;

    static BOOL m_QueryNewPaletteCalled;

    static AwtComponent* sm_getComponentCache; // a cache for the GetComponent(..) method.

    int windowMoveLockPosX;
    int windowMoveLockPosY;
    int windowMoveLockPosCX;
    int windowMoveLockPosCY;

    // 6524352: support finer-resolution
    int m_wheelRotationAmountX;
    int m_wheelRotationAmountY;

    BOOL deadKeyActive;

    /*
     * The association list of children's IDs and corresponding components.
     * Some components like Choice or List are required their sizes while
     * the creations of themselfs are in progress.
     */
    class ChildListItem {
    public:
        ChildListItem(UINT id, AwtComponent* component) {
            m_ID = id;
            m_Component = component;
            m_next = NULL;
        }
        ~ChildListItem() {
            if (m_next != NULL)
                delete m_next;
        }

        UINT m_ID;
        AwtComponent* m_Component;
        ChildListItem* m_next;
    };

public:
    INLINE void PushChild(UINT id, AwtComponent* component) {
        ChildListItem* child = new ChildListItem(id, component);
        child->m_next = m_childList;
        m_childList = child;
    }

    static void SetParent(void * param);
private:
    AwtComponent* SearchChild(UINT id);
    void RemoveChild(UINT id) ;
    static BOOL IsNavigationKey(UINT wkey);
    static void BuildPrimaryDynamicTable();

    ChildListItem* m_childList;

    HCURSOR m_hCursorCache; // the latest cursor which has been active within the heavyweight component
public:
    inline void setCursorCache(HCURSOR hCursor) {
        m_hCursorCache = hCursor;
    }
    inline HCURSOR getCursorCache() {
        return m_hCursorCache;
    }
};

class CounterHelper {
private:
    UINT *m_counter;
public:
    explicit CounterHelper(UINT *counter) {
        m_counter = counter;
        (*m_counter)++;
    }
    ~CounterHelper() {
        (*m_counter)--;
        m_counter = NULL;
    }
};

// DC management objects; these classes are used to track the list of
// DC's associated with a given Component.  Then DC's can be released
// appropriately on demand or on window destruction to avoid resource
// leakage.
class DCItem {
public:
    HDC             hDC;
    HWND            hWnd;
    DCItem          *next;
};
class DCList {
    DCItem          *head;
    CriticalSection listLock;
public:
    DCList() { head = NULL; }

    void            AddDC(HDC hDC, HWND hWnd);
    void            AddDCItem(DCItem *newItem);
    DCItem          *RemoveDC(HDC hDC, HWND hWnd);
    DCItem          *RemoveAllDCs(HWND hWnd);
    DCItem          *RemoveAllDCs();
    void            RealizePalettes(int screen);
};

void ReleaseDCList(HWND hwnd, DCList &list);
void ReleaseDCList(DCList &list);
void MoveDCToPassiveList(HDC hDC, HWND hWnd);

#include "ObjectList.h"

#endif /* AWT_COMPONENT_H */
