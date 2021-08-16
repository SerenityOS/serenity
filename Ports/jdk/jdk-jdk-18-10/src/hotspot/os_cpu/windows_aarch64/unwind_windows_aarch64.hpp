/*
 * Copyright (c) 2020, Microsoft Corporation. All rights reserved.
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

#ifndef OS_CPU_WINDOWS_AARCH64_UNWIND_WINDOWS_AARCH64_HPP
#define OS_CPU_WINDOWS_AARCH64_UNWIND_WINDOWS_AARCH64_HPP


typedef unsigned char UBYTE;

#if _MSC_VER < 1700

/* Not needed for VS2012 compiler, comes from winnt.h. */
#define UNW_FLAG_EHANDLER  0x01
#define UNW_FLAG_UHANDLER  0x02
#define UNW_FLAG_CHAININFO 0x04

#endif

// See https://docs.microsoft.com/en-us/cpp/build/arm64-exception-handling#xdata-records
typedef struct _UNWIND_INFO_EH_ONLY {
    DWORD FunctionLength : 18;
    DWORD Version        : 2;
    DWORD X              : 1; // = 1
    DWORD E              : 1; // = 1
    DWORD EpilogCount    : 5; // = 0
    DWORD CodeWords      : 5; // = 1
    DWORD UnwindCode0    : 8;
    DWORD UnwindCode1    : 8;
    DWORD UnwindCode2    : 8;
    DWORD UnwindCode3    : 8;
    DWORD ExceptionHandler;
} UNWIND_INFO_EH_ONLY, *PUNWIND_INFO_EH_ONLY;

/*
typedef struct _RUNTIME_FUNCTION {
    DWORD BeginAddress;
    union {
        DWORD UnwindData;
        struct {
            DWORD Flag : 2;
            DWORD FunctionLength : 11;
            DWORD RegF : 3;
            DWORD RegI : 4;
            DWORD H : 1;
            DWORD CR : 2;
            DWORD FrameSize : 9;
        } DUMMYSTRUCTNAME;
    } DUMMYUNIONNAME;
} RUNTIME_FUNCTION, *PRUNTIME_FUNCTION;
*/

#if _MSC_VER < 1700

/* Not needed for VS2012 compiler, comes from winnt.h. */
typedef struct _DISPATCHER_CONTEXT {
    ULONG64 ControlPc;
    ULONG64 ImageBase;
    PRUNTIME_FUNCTION FunctionEntry;
    ULONG64 EstablisherFrame;
    ULONG64 TargetIp;
    PCONTEXT ContextRecord;
//    PEXCEPTION_ROUTINE LanguageHandler;
    char * LanguageHandler; // double dependency problem
    PVOID HandlerData;
} DISPATCHER_CONTEXT, *PDISPATCHER_CONTEXT;

#endif

#if _MSC_VER < 1500

/* Not needed for VS2008 compiler, comes from winnt.h. */
typedef EXCEPTION_DISPOSITION (*PEXCEPTION_ROUTINE) (
    IN PEXCEPTION_RECORD ExceptionRecord,
    IN ULONG64 EstablisherFrame,
    IN OUT PCONTEXT ContextRecord,
    IN OUT PDISPATCHER_CONTEXT DispatcherContext
);

#endif

#endif // OS_CPU_WINDOWS_AARCH64_UNWIND_WINDOWS_AARCH64_HPP
