/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal.impl.jna.win;

//OpenJDK changes:
//-references to JNA types commented out
//-replacement types provided where needed (IntByReference, LastErrorException, Pointer)
//-methods not used by JLine commented out
//-provided an implementation of the interface (Kernel32Impl), backed by a native implementation (Kernel32.cpp)

//import com.sun.jna.LastErrorException;
//import com.sun.jna.Native;
//import com.sun.jna.Pointer;
//import com.sun.jna.Structure;
//import com.sun.jna.Union;
//import com.sun.jna.ptr.IntByReference;
//import com.sun.jna.win32.StdCallLibrary;
//import com.sun.jna.win32.W32APIOptions;

interface Kernel32 {//extends StdCallLibrary {

    Kernel32 INSTANCE = new Kernel32Impl();
//    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.UNICODE_OPTIONS);

//    Pointer INVALID_HANDLE_VALUE = Pointer.createConstant(-1L);

    int STD_INPUT_HANDLE =  -10;
    int STD_OUTPUT_HANDLE = -11;
    int STD_ERROR_HANDLE =  -12;

    int ENABLE_PROCESSED_INPUT =    0x0001;
    int ENABLE_LINE_INPUT =         0x0002;
    int ENABLE_ECHO_INPUT =         0x0004;
    int ENABLE_WINDOW_INPUT =       0x0008;
    int ENABLE_MOUSE_INPUT =        0x0010;
    int ENABLE_INSERT_MODE =        0x0020;
    int ENABLE_QUICK_EDIT_MODE =    0x0040;
    int ENABLE_EXTENDED_FLAGS =     0x0080;

    int RIGHT_ALT_PRESSED =     0x0001;
    int LEFT_ALT_PRESSED =      0x0002;
    int RIGHT_CTRL_PRESSED =    0x0004;
    int LEFT_CTRL_PRESSED =     0x0008;
    int SHIFT_PRESSED =         0x0010;

    int FOREGROUND_BLUE =       0x0001;
    int FOREGROUND_GREEN =      0x0002;
    int FOREGROUND_RED =        0x0004;
    int FOREGROUND_INTENSITY =  0x0008;
    int BACKGROUND_BLUE =       0x0010;
    int BACKGROUND_GREEN =      0x0020;
    int BACKGROUND_RED =        0x0040;
    int BACKGROUND_INTENSITY =  0x0080;

    // Button state
    int FROM_LEFT_1ST_BUTTON_PRESSED = 0x0001;
    int RIGHTMOST_BUTTON_PRESSED     = 0x0002;
    int FROM_LEFT_2ND_BUTTON_PRESSED = 0x0004;
    int FROM_LEFT_3RD_BUTTON_PRESSED = 0x0008;
    int FROM_LEFT_4TH_BUTTON_PRESSED = 0x0010;

    // Event flags
    int MOUSE_MOVED                  = 0x0001;
    int DOUBLE_CLICK                 = 0x0002;
    int MOUSE_WHEELED                = 0x0004;
    int MOUSE_HWHEELED               = 0x0008;

    // DWORD WINAPI WaitForSingleObject(
    //  _In_ HANDLE hHandle,
    //  _In_ DWORD  dwMilliseconds
    // );
    int WaitForSingleObject(Pointer in_hHandle, int in_dwMilliseconds);

    // HANDLE WINAPI GetStdHandle(
    // __in DWORD nStdHandle
    // );
    Pointer GetStdHandle(int nStdHandle);

//    // BOOL WINAPI AllocConsole(void);
//    void AllocConsole() throws LastErrorException;
//
//    // BOOL WINAPI FreeConsole(void);
//    void FreeConsole() throws LastErrorException;
//
//    // HWND WINAPI GetConsoleWindow(void);
//    Pointer GetConsoleWindow();
//
//    // UINT WINAPI GetConsoleCP(void)
//    int GetConsoleCP();
//
    // UINT WINAPI GetConsoleOutputCP(void)
    int GetConsoleOutputCP();

    // BOOL WINAPI FillConsoleOutputCharacter(
    // _In_ HANDLE hConsoleOutput,
    // _In_ TCHAR cCharacter,
    // _In_ DWORD nLength,
    // _In_ COORD dwWriteCoord,
    // _Out_ LPDWORD lpNumberOfCharsWritten);
    void FillConsoleOutputCharacter(Pointer in_hConsoleOutput,
                                    char in_cCharacter, int in_nLength, COORD in_dwWriteCoord,
                                    IntByReference out_lpNumberOfCharsWritten)
            throws LastErrorException;

    // BOOL WINAPI FillConsoleOutputAttribute(
    // _In_ HANDLE hConsoleOutput,
    // _In_ WORD wAttribute,
    // _In_ DWORD nLength,
    // _In_ COORD dwWriteCoord,
    // _Out_ LPDWORD lpNumberOfAttrsWritten);
    void FillConsoleOutputAttribute(Pointer in_hConsoleOutput,
                                    short in_wAttribute, int in_nLength, COORD in_dwWriteCoord,
                                    IntByReference out_lpNumberOfAttrsWritten)
            throws LastErrorException;
//
////    // BOOL WINAPI GetConsoleCursorInfo(
////    // _In_ HANDLE hConsoleOutput,
////    // _Out_ PCONSOLE_CURSOR_INFO lpConsoleCursorInfo);
////    void GetConsoleCursorInfo(Pointer in_hConsoleOutput,
////                              CONSOLE_CURSOR_INFO.ByReference out_lpConsoleCursorInfo)
////            throws LastErrorException;
//
    // BOOL WINAPI GetConsoleMode(
    //   _In_   HANDLE hConsoleHandle,
    //   _Out_  LPDWORD lpMode);
    void GetConsoleMode(
            Pointer in_hConsoleOutput,
            IntByReference out_lpMode)
            throws LastErrorException;

    // BOOL WINAPI GetConsoleScreenBufferInfo(
    // _In_   HANDLE hConsoleOutput,
    // _Out_  PCONSOLE_SCREEN_BUFFER_INFO lpConsoleScreenBufferInfo);
    void GetConsoleScreenBufferInfo(
            Pointer in_hConsoleOutput,
            CONSOLE_SCREEN_BUFFER_INFO out_lpConsoleScreenBufferInfo)
            throws LastErrorException;
//
//    // BOOL WINAPI GetNumberOfConsoleInputEvents(
//    // _In_ HANDLE hConsoleInput,
//    // _Out_ LPDWORD lpcNumberOfEvents);
//    void GetNumberOfConsoleInputEvents(Pointer in_hConsoleOutput,
//                                       IntByReference out_lpcNumberOfEvents) throws LastErrorException;
//
    // BOOL WINAPI ReadConsoleInput(
    // _In_ HANDLE hConsoleInput,
    // _Out_ PINPUT_RECORD lpBuffer,
    // _In_ DWORD nLength,
    // _Out_ LPDWORD lpNumberOfEventsRead);
    void ReadConsoleInput(Pointer in_hConsoleOutput,
                          INPUT_RECORD[] out_lpBuffer, int in_nLength,
                          IntByReference out_lpNumberOfEventsRead) throws LastErrorException;

//    // BOOL WINAPI SetConsoleCtrlHandler(
//    // _In_opt_  PHANDLER_ROUTINE HandlerRoutine,
//    // _In_      BOOL Add);
//    void SetConsoleCtrlHandler(
//            Pointer in_opt_HandlerRoutine,
//            boolean in_Add)
//            throws LastErrorException;
//
//    // BOOL WINAPI ReadConsoleOutput(
//    // _In_     HANDLE hConsoleOutput,
//    // _Out_    PCHAR_INFO lpBuffer,
//    // _In_     COORD dwBufferSize,
//    // _In_     COORD dwBufferCoord,
//    // _Inout_  PSMALL_RECT lpReadRegion);
////    void ReadConsoleOutput(Pointer in_hConsoleOutput, CHAR_INFO[] out_lpBuffer,
////                           COORD in_dwBufferSize, COORD in_dwBufferCoord,
////                           SMALL_RECT inout_lpReadRegion) throws LastErrorException;
////    void ReadConsoleOutputA(Pointer in_hConsoleOutput, CHAR_INFO[] out_lpBuffer,
////                            COORD in_dwBufferSize, COORD in_dwBufferCoord,
////                            SMALL_RECT inout_lpReadRegion) throws LastErrorException;
//
//    // BOOL WINAPI ReadConsoleOutputCharacter(
//    // _In_   HANDLE hConsoleOutput,
//    // _Out_  LPTSTR lpCharacter,
//    // _In_   DWORD nLength,
//    // _In_   COORD dwReadCoord,
//    // _Out_  LPDWORD lpNumberOfCharsRead);
//    void ReadConsoleOutputCharacter(Pointer in_hConsoleOutput,
//                                    char[] ouy_lpCharacter, int in_nLength, COORD in_dwReadCoord,
//                                    IntByReference out_lpNumberOfCharsRead)
//            throws LastErrorException;
//    void ReadConsoleOutputCharacterA(Pointer in_hConsoleOutput,
//                                     byte[] ouy_lpCharacter, int in_nLength, COORD in_dwReadCoord,
//                                     IntByReference out_lpNumberOfCharsRead)
//            throws LastErrorException;
//
//    // BOOL WINAPI SetConsoleCursorInfo(
//    // _In_ HANDLE hConsoleOutput,
//    // _In_ const CONSOLE_CURSOR_INFO *lpConsoleCursorInfo);
//    void SetConsoleCursorInfo(Pointer in_hConsoleOutput,
//                              CONSOLE_CURSOR_INFO in_lpConsoleCursorInfo)
//            throws LastErrorException;
//
//    // BOOL WINAPI SetConsoleCP(
//    // _In_ UINT wCodePageID);
//    void SetConsoleCP(int in_wCodePageID) throws LastErrorException;
//
//    // BOOL WINAPI SetConsoleOutputCP(
//    // _In_ UINT wCodePageID);
//    void SetConsoleOutputCP(int in_wCodePageID) throws LastErrorException;
//
    // BOOL WINAPI SetConsoleCursorPosition(
    // _In_ HANDLE hConsoleOutput,
    // _In_ COORD dwCursorPosition);
    void SetConsoleCursorPosition(Pointer in_hConsoleOutput,
                                  COORD in_dwCursorPosition) throws LastErrorException;

    // BOOL WINAPI SetConsoleMode(
    //   _In_  HANDLE hConsoleHandle,
    //   _In_  DWORD dwMode);
    void SetConsoleMode(
            Pointer in_hConsoleOutput,
            int in_dwMode) throws LastErrorException;

//    // BOOL WINAPI SetConsoleScreenBufferSize(
//    // __in HANDLE hConsoleOutput,
//    // __in COORD dwSize
//    // );
//    void SetConsoleScreenBufferSize(Pointer in_hConsoleOutput,
//                                    COORD in_dwSize) throws LastErrorException;
//
    // BOOL WINAPI SetConsoleTextAttribute(
    // _In_ HANDLE hConsoleOutput,
    // _In_ WORD   wAttributes
    // );
    void SetConsoleTextAttribute(Pointer in_hConsoleOutput,
                                 short in_wAttributes)
            throws LastErrorException;

    // BOOL WINAPI SetConsoleTitle(
    // _In_ LPCTSTR lpConsoleTitle
    // );
    void SetConsoleTitle(String in_lpConsoleTitle)
            throws LastErrorException;

//    // BOOL WINAPI SetConsoleWindowInfo(
//    // _In_ HANDLE hConsoleOutput,
//    // _In_ BOOL bAbsolute,
//    // _In_ const SMALL_RECT *lpConsoleWindow);
//    void SetConsoleWindowInfo(Pointer in_hConsoleOutput,
//                              boolean in_bAbsolute, SMALL_RECT in_lpConsoleWindow)
//            throws LastErrorException;

    // BOOL WINAPI WriteConsole(
    //  _In_             HANDLE  hConsoleOutput,
    //  _In_       const VOID    *lpBuffer,
    //  _In_             DWORD   nNumberOfCharsToWrite,
    //  _Out_            LPDWORD lpNumberOfCharsWritten,
    //  _Reserved_       LPVOID  lpReserved
    // );
    void WriteConsoleW(Pointer in_hConsoleOutput, char[] in_lpBuffer, int in_nNumberOfCharsToWrite,
                          IntByReference out_lpNumberOfCharsWritten, Pointer reserved_lpReserved) throws LastErrorException;

//    // BOOL WINAPI WriteConsoleOutput(
//    // _In_ HANDLE hConsoleOutput,
//    // _In_ const CHAR_INFO *lpBuffer,
//    // _In_ COORD dwBufferSize,
//    // _In_ COORD dwBufferCoord,
//    // _Inout_ PSMALL_RECT lpWriteRegion);
////    void WriteConsoleOutput(Pointer in_hConsoleOutput, CHAR_INFO[] in_lpBuffer,
////                            COORD in_dwBufferSize, COORD in_dwBufferCoord,
////                            SMALL_RECT inout_lpWriteRegion) throws LastErrorException;
////    void WriteConsoleOutputA(Pointer in_hConsoleOutput, CHAR_INFO[] in_lpBuffer,
////                             COORD in_dwBufferSize, COORD in_dwBufferCoord,
////                             SMALL_RECT inout_lpWriteRegion) throws LastErrorException;
//
//    // BOOL WINAPI WriteConsoleOutputCharacter(
//    // _In_ HANDLE hConsoleOutput,
//    // _In_ LPCTSTR lpCharacter,
//    // _In_ DWORD nLength,
//    // _In_ COORD dwWriteCoord,
//    // _Out_ LPDWORD lpNumberOfCharsWritten);
//    void WriteConsoleOutputCharacter(Pointer in_hConsoleOutput,
//                                     char[] in_lpCharacter, int in_nLength, COORD in_dwWriteCoord,
//                                     IntByReference out_lpNumberOfCharsWritten)
//            throws LastErrorException;
//    void WriteConsoleOutputCharacterA(Pointer in_hConsoleOutput,
//                                      byte[] in_lpCharacter, int in_nLength, COORD in_dwWriteCoord,
//                                      IntByReference out_lpNumberOfCharsWritten)
//            throws LastErrorException;
//
    // BOOL WINAPI ScrollConsoleScreenBuffer(
    //     _In_           HANDLE     hConsoleOutput,
    //     _In_     const SMALL_RECT *lpScrollRectangle,
    //     _In_opt_ const SMALL_RECT *lpClipRectangle,
    //     _In_           COORD      dwDestinationOrigin,
    //     _In_     const CHAR_INFO  *lpFill);
    void ScrollConsoleScreenBuffer(Pointer in_hConsoleOutput,
                                   SMALL_RECT in_lpScrollRectangle,
                                   SMALL_RECT in_lpClipRectangle,
                                   COORD in_dwDestinationOrigin,
                                   CHAR_INFO in_lpFill)
            throws LastErrorException;

    // typedef struct _CHAR_INFO {
    //   union {
    //     WCHAR UnicodeChar;
    //     CHAR  AsciiChar;
    //   } Char;
    //   WORD  Attributes;
    // } CHAR_INFO, *PCHAR_INFO;
    class CHAR_INFO {//extends Structure {
        public CHAR_INFO() {
        }

        public CHAR_INFO(char c, short attr) {
            uChar = new UnionChar(c);
            Attributes = attr;
        }

//        public CHAR_INFO(byte c, short attr) {
//            uChar = new UnionChar(c);
//            Attributes = attr;
//        }

        public UnionChar uChar;
        public short Attributes;

//        public static CHAR_INFO[] createArray(int size) {
//            return (CHAR_INFO[]) new CHAR_INFO().toArray(size);
//        }
//
//        private static String[] fieldOrder = { "uChar", "Attributes" };
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }
    }

    // typedef struct _CONSOLE_CURSOR_INFO {
    //   DWORD dwSize;
    //   BOOL  bVisible;
    // } CONSOLE_CURSOR_INFO, *PCONSOLE_CURSOR_INFO;
    class CONSOLE_CURSOR_INFO {//extends Structure {
        public int dwSize;
        public boolean bVisible;

//        public static class ByReference extends CONSOLE_CURSOR_INFO implements
//                Structure.ByReference {
//        }
//
//        private static String[] fieldOrder = { "dwSize", "bVisible" };
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }
    }

    // typedef struct _CONSOLE_SCREEN_BUFFER_INFO {
    //   COORD      dwSize;
    //   COORD      dwCursorPosition;
    //   WORD       wAttributes;
    //   SMALL_RECT srWindow;
    //   COORD      dwMaximumWindowSize;
    // } CONSOLE_SCREEN_BUFFER_INFO;
    class CONSOLE_SCREEN_BUFFER_INFO {//extends Structure {
        public COORD      dwSize;
        public COORD      dwCursorPosition;
        public short      wAttributes;
        public SMALL_RECT srWindow;
        public COORD      dwMaximumWindowSize;

//        private static String[] fieldOrder = { "dwSize", "dwCursorPosition", "wAttributes", "srWindow", "dwMaximumWindowSize" };
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }

        public int windowWidth() {
            return this.srWindow.width() + 1;
        }

        public int windowHeight() {
            return this.srWindow.height() + 1;
        }
    }

    // typedef struct _COORD {
    //    SHORT X;
    //    SHORT Y;
    //  } COORD, *PCOORD;
    class COORD {//extends Structure implements Structure.ByValue {
        public COORD() {
        }

        public COORD(short X, short Y) {
            this.X = X;
            this.Y = Y;
        }

        public short X;
        public short Y;

//        private static String[] fieldOrder = { "X", "Y" };
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }
    }

    // typedef struct _INPUT_RECORD {
    //   WORD  EventType;
    //   union {
    //     KEY_EVENT_RECORD          KeyEvent;
    //     MOUSE_EVENT_RECORD        MouseEvent;
    //     WINDOW_BUFFER_SIZE_RECORD WindowBufferSizeEvent;
    //     MENU_EVENT_RECORD         MenuEvent;
    //     FOCUS_EVENT_RECORD        FocusEvent;
    //   } Event;
    // } INPUT_RECORD;
    class INPUT_RECORD {//extends Structure {
        public static final short KEY_EVENT = 0x0001;
        public static final short MOUSE_EVENT = 0x0002;
        public static final short WINDOW_BUFFER_SIZE_EVENT = 0x0004;
        public static final short MENU_EVENT = 0x0008;
        public static final short FOCUS_EVENT = 0x0010;

        public short EventType;
        public EventUnion Event;

        public static class EventUnion {//extends Union {
            public KEY_EVENT_RECORD KeyEvent;
            public MOUSE_EVENT_RECORD MouseEvent;
            public WINDOW_BUFFER_SIZE_RECORD WindowBufferSizeEvent;
            public MENU_EVENT_RECORD MenuEvent;
            public FOCUS_EVENT_RECORD FocusEvent;
        }

//        @Override
//        public void read() {
//            readField("EventType");
//            switch (EventType) {
//                case KEY_EVENT:
//                    Event.setType(KEY_EVENT_RECORD.class);
//                    break;
//                case MOUSE_EVENT:
//                    Event.setType(MOUSE_EVENT_RECORD.class);
//                    break;
//                case WINDOW_BUFFER_SIZE_EVENT:
//                    Event.setType(WINDOW_BUFFER_SIZE_RECORD.class);
//                    break;
//                case MENU_EVENT:
//                    Event.setType(MENU_EVENT_RECORD.class);
//                    break;
//                case FOCUS_EVENT:
//                    Event.setType(MENU_EVENT_RECORD.class);
//                    break;
//            }
//            super.read();
//        }

//        private static String[] fieldOrder = {"EventType", "Event"};
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }
    }

    // typedef struct _KEY_EVENT_RECORD {
    //   BOOL  bKeyDown;
    //   WORD  wRepeatCount;
    //   WORD  wVirtualKeyCode;
    //   WORD  wVirtualScanCode;
    //   union {
    //     WCHAR UnicodeChar;
    //     CHAR  AsciiChar;
    //   } uChar;
    //   DWORD dwControlKeyState;
    // } KEY_EVENT_RECORD;
    class KEY_EVENT_RECORD {//extends Structure {
        public boolean bKeyDown;
        public short wRepeatCount;
        public short wVirtualKeyCode;
        public short wVirtualScanCode;
        public UnionChar uChar;
        public int dwControlKeyState;

//        private static String[] fieldOrder = {"bKeyDown", "wRepeatCount", "wVirtualKeyCode", "wVirtualScanCode", "uChar", "dwControlKeyState"};
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }
    }

    // typedef struct _MOUSE_EVENT_RECORD {
    //   COORD dwMousePosition;
    //   DWORD dwButtonState;
    //   DWORD dwControlKeyState;
    //   DWORD dwEventFlags;
    // } MOUSE_EVENT_RECORD;
    class MOUSE_EVENT_RECORD {//extends Structure {
        public COORD dwMousePosition;
        public int dwButtonState;
        public int dwControlKeyState;
        public int dwEventFlags;

//        private static String[] fieldOrder = { "dwMousePosition", "dwButtonState", "dwControlKeyState", "dwEventFlags"};
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }
    }

    // typedef struct _WINDOW_BUFFER_SIZE_RECORD {
    //   COORD dwSize;
    // } WINDOW_BUFFER_SIZE_RECORD;
    class WINDOW_BUFFER_SIZE_RECORD {//extends Structure {
        public COORD dwSize;

//        private static String[] fieldOrder = {"dwSize"};
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }
    }

    // typedef struct _MENU_EVENT_RECORD {
    //   UINT dwCommandId;
    // } MENU_EVENT_RECORD, *PMENU_EVENT_RECORD;
    class MENU_EVENT_RECORD {//extends Structure {

        public int dwCommandId;

//        private static String[] fieldOrder = {"dwCommandId"};
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }
    }

    // typedef struct _FOCUS_EVENT_RECORD {
    //  BOOL bSetFocus;
    //} FOCUS_EVENT_RECORD;
    class FOCUS_EVENT_RECORD {//extends Structure {
        public boolean bSetFocus;

//        private static String[] fieldOrder = {"bSetFocus"};
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }
    }

    // typedef struct _SMALL_RECT {
    //    SHORT Left;
    //    SHORT Top;
    //    SHORT Right;
    //    SHORT Bottom;
    //  } SMALL_RECT;
    class SMALL_RECT {//extends Structure {
        public SMALL_RECT() {
        }

        public SMALL_RECT(SMALL_RECT org) {
            this(org.Top, org.Left, org.Bottom, org.Right);
        }

        public SMALL_RECT(short Top, short Left, short Bottom, short Right) {
            this.Top = Top;
            this.Left = Left;
            this.Bottom = Bottom;
            this.Right = Right;
        }

        public short Left;
        public short Top;
        public short Right;
        public short Bottom;

//        private static String[] fieldOrder = { "Left", "Top", "Right", "Bottom" };
//
//        @Override
//        protected java.util.List<String> getFieldOrder() {
//            return java.util.Arrays.asList(fieldOrder);
//        }

        public short width() {
            return (short)(this.Right - this.Left);
        }

        public short height() {
            return (short)(this.Bottom - this.Top);
        }

    }

    class UnionChar {//extends Union {
        public UnionChar() {
        }

        public UnionChar(char c) {
//            setType(char.class);
            UnicodeChar = c;
        }

//        public UnionChar(byte c) {
//            setType(byte.class);
//            AsciiChar = c;
//        }

        public void set(char c) {
//            setType(char.class);
            UnicodeChar = c;
        }

//        public void set(byte c) {
//            setType(byte.class);
//            AsciiChar = c;
//        }

        public char UnicodeChar;
//        public byte AsciiChar;
    }
}
