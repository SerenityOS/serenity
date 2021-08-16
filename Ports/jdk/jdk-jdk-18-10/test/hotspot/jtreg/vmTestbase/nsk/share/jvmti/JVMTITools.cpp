/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <string.h>
#include "JVMTITools.h"

extern "C" {

const char* TranslateState(jint flags) {
    static char str[15 * 20];

    if (flags == 0)
        return "<none>";

    str[0] = '\0';

    if (flags & JVMTI_THREAD_STATE_ALIVE) {
        strcat(str, " ALIVE");
    }

    if (flags & JVMTI_THREAD_STATE_TERMINATED) {
        strcat(str, " TERMINATED");
    }

    if (flags & JVMTI_THREAD_STATE_RUNNABLE) {
        strcat(str, " RUNNABLE");
    }

    if (flags & JVMTI_THREAD_STATE_WAITING) {
        strcat(str, " WAITING");
    }

    if (flags & JVMTI_THREAD_STATE_WAITING_INDEFINITELY) {
        strcat(str, " WAITING_INDEFINITELY");
    }

    if (flags & JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT) {
        strcat(str, " WAITING_WITH_TIMEOUT");
    }

    if (flags & JVMTI_THREAD_STATE_SLEEPING) {
        strcat(str, " SLEEPING");
    }

    if (flags & JVMTI_THREAD_STATE_IN_OBJECT_WAIT) {
        strcat(str, " IN_OBJECT_WAIT");
    }

    if (flags & JVMTI_THREAD_STATE_PARKED) {
        strcat(str, " PARKED");
    }

    if (flags & JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER) {
        strcat(str, " BLOCKED_ON_MONITOR_ENTER");
    }

    if (flags & JVMTI_THREAD_STATE_SUSPENDED) {
        strcat(str, " SUSPENDED");
    }

    if (flags & JVMTI_THREAD_STATE_INTERRUPTED) {
        strcat(str, " INTERRUPTED");
    }

    if (flags & JVMTI_THREAD_STATE_IN_NATIVE) {
        strcat(str, " IN_NATIVE");
    }


    return str;
}

const char* TranslateEvent(jvmtiEvent event_type) {
    switch (event_type) {
    case JVMTI_EVENT_VM_INIT:
        return ("JVMTI_EVENT_VM_INIT");
    case JVMTI_EVENT_VM_DEATH:
        return ("JVMTI_EVENT_VM_DEATH");
    case JVMTI_EVENT_THREAD_START:
        return ("JVMTI_EVENT_THREAD_START");
    case JVMTI_EVENT_THREAD_END:
        return ("JVMTI_EVENT_THREAD_END");
    case JVMTI_EVENT_CLASS_FILE_LOAD_HOOK:
        return ("JVMTI_EVENT_CLASS_FILE_LOAD_HOOK");
    case JVMTI_EVENT_CLASS_LOAD:
        return ("JVMTI_EVENT_CLASS_LOAD");
    case JVMTI_EVENT_CLASS_PREPARE:
        return ("JVMTI_EVENT_CLASS_PREPARE");
    case JVMTI_EVENT_VM_START:
        return ("JVMTI_EVENT_VM_START");
    case JVMTI_EVENT_EXCEPTION:
        return ("JVMTI_EVENT_EXCEPTION");
    case JVMTI_EVENT_EXCEPTION_CATCH:
        return ("JVMTI_EVENT_EXCEPTION_CATCH");
    case JVMTI_EVENT_SINGLE_STEP:
        return ("JVMTI_EVENT_SINGLE_STEP");
    case JVMTI_EVENT_FRAME_POP:
        return ("JVMTI_EVENT_FRAME_POP");
    case JVMTI_EVENT_BREAKPOINT:
        return ("JVMTI_EVENT_BREAKPOINT");
    case JVMTI_EVENT_FIELD_ACCESS:
        return ("JVMTI_EVENT_FIELD_ACCESS");
    case JVMTI_EVENT_FIELD_MODIFICATION:
        return ("JVMTI_EVENT_FIELD_MODIFICATION");
    case JVMTI_EVENT_METHOD_ENTRY:
        return ("JVMTI_EVENT_METHOD_ENTRY");
    case JVMTI_EVENT_METHOD_EXIT:
        return ("JVMTI_EVENT_METHOD_EXIT");
    case JVMTI_EVENT_NATIVE_METHOD_BIND:
        return ("JVMTI_EVENT_NATIVE_METHOD_BIND");
    case JVMTI_EVENT_COMPILED_METHOD_LOAD:
        return ("JVMTI_EVENT_COMPILED_METHOD_LOAD");
    case JVMTI_EVENT_COMPILED_METHOD_UNLOAD:
        return ("JVMTI_EVENT_COMPILED_METHOD_UNLOAD");
    case JVMTI_EVENT_DYNAMIC_CODE_GENERATED:
        return ("JVMTI_EVENT_DYNAMIC_CODE_GENERATED");
    case JVMTI_EVENT_DATA_DUMP_REQUEST:
        return ("JVMTI_EVENT_DATA_DUMP_REQUEST");
    case JVMTI_EVENT_MONITOR_WAIT:
        return ("JVMTI_EVENT_MONITOR_WAIT");
    case JVMTI_EVENT_MONITOR_WAITED:
        return ("JVMTI_EVENT_MONITOR_WAITED");
    case JVMTI_EVENT_MONITOR_CONTENDED_ENTER:
        return ("JVMTI_EVENT_MONITOR_CONTENDED_ENTER");
    case JVMTI_EVENT_MONITOR_CONTENDED_ENTERED:
        return ("JVMTI_EVENT_MONITOR_CONTENDED_ENTERED");
    case JVMTI_EVENT_GARBAGE_COLLECTION_START:
        return ("JVMTI_EVENT_GARBAGE_COLLECTION_START");
    case JVMTI_EVENT_GARBAGE_COLLECTION_FINISH:
        return ("JVMTI_EVENT_GARBAGE_COLLECTION_FINISH");
    case JVMTI_EVENT_OBJECT_FREE:
        return ("JVMTI_EVENT_OBJECT_FREE");
    case JVMTI_EVENT_VM_OBJECT_ALLOC:
        return ("JVMTI_EVENT_VM_OBJECT_ALLOC");
    default:
        return ("<unknown event>");
    }
}

const char* TranslateError(jvmtiError err) {
    switch (err) {
    case JVMTI_ERROR_NONE:
        return ("JVMTI_ERROR_NONE");
    case JVMTI_ERROR_INVALID_THREAD:
        return ("JVMTI_ERROR_INVALID_THREAD");
    case JVMTI_ERROR_INVALID_THREAD_GROUP:
        return ("JVMTI_ERROR_INVALID_THREAD_GROUP");
    case JVMTI_ERROR_INVALID_PRIORITY:
        return ("JVMTI_ERROR_INVALID_PRIORITY");
    case JVMTI_ERROR_THREAD_NOT_SUSPENDED:
        return ("JVMTI_ERROR_THREAD_NOT_SUSPENDED");
    case JVMTI_ERROR_THREAD_SUSPENDED:
        return ("JVMTI_ERROR_THREAD_SUSPENDED");
    case JVMTI_ERROR_THREAD_NOT_ALIVE:
        return ("JVMTI_ERROR_THREAD_NOT_ALIVE");
    case JVMTI_ERROR_INVALID_OBJECT:
        return ("JVMTI_ERROR_INVALID_OBJECT");
    case JVMTI_ERROR_INVALID_CLASS:
        return ("JVMTI_ERROR_INVALID_CLASS");
    case JVMTI_ERROR_CLASS_NOT_PREPARED:
        return ("JVMTI_ERROR_CLASS_NOT_PREPARED");
    case JVMTI_ERROR_INVALID_METHODID:
        return ("JVMTI_ERROR_INVALID_METHODID");
    case JVMTI_ERROR_INVALID_LOCATION:
        return ("JVMTI_ERROR_INVALID_LOCATION");
    case JVMTI_ERROR_INVALID_FIELDID:
        return ("JVMTI_ERROR_INVALID_FIELDID");
    case JVMTI_ERROR_NO_MORE_FRAMES:
        return ("JVMTI_ERROR_NO_MORE_FRAMES");
    case JVMTI_ERROR_OPAQUE_FRAME:
        return ("JVMTI_ERROR_OPAQUE_FRAME");
    case JVMTI_ERROR_TYPE_MISMATCH:
        return ("JVMTI_ERROR_TYPE_MISMATCH");
    case JVMTI_ERROR_INVALID_SLOT:
        return ("JVMTI_ERROR_INVALID_SLOT");
    case JVMTI_ERROR_DUPLICATE:
        return ("JVMTI_ERROR_DUPLICATE");
    case JVMTI_ERROR_NOT_FOUND:
        return ("JVMTI_ERROR_NOT_FOUND");
    case JVMTI_ERROR_INVALID_MONITOR:
        return ("JVMTI_ERROR_INVALID_MONITOR");
    case JVMTI_ERROR_NOT_MONITOR_OWNER:
        return ("JVMTI_ERROR_NOT_MONITOR_OWNER");
    case JVMTI_ERROR_INTERRUPT:
        return ("JVMTI_ERROR_INTERRUPT");
    case JVMTI_ERROR_INVALID_CLASS_FORMAT:
        return ("JVMTI_ERROR_INVALID_CLASS_FORMAT");
    case JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION:
        return ("JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION");
    case JVMTI_ERROR_FAILS_VERIFICATION:
        return ("JVMTI_ERROR_FAILS_VERIFICATION");
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED:
        return ("JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED");
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED:
        return ("JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED");
    case JVMTI_ERROR_INVALID_TYPESTATE:
        return ("JVMTI_ERROR_INVALID_TYPESTATE");
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED:
        return ("JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED");
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED:
        return ("JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED");
    case JVMTI_ERROR_UNSUPPORTED_VERSION:
        return ("JVMTI_ERROR_UNSUPPORTED_VERSION");
    case JVMTI_ERROR_NAMES_DONT_MATCH:
        return ("JVMTI_ERROR_NAMES_DONT_MATCH");
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED:
        return ("JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED");
    case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED:
        return ("JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED");
    case JVMTI_ERROR_UNMODIFIABLE_CLASS:
        return ("JVMTI_ERROR_UNMODIFIABLE_CLASS");
    case JVMTI_ERROR_NOT_AVAILABLE:
        return ("JVMTI_ERROR_NOT_AVAILABLE");
    case JVMTI_ERROR_MUST_POSSESS_CAPABILITY:
        return ("JVMTI_ERROR_MUST_POSSESS_CAPABILITY");
    case JVMTI_ERROR_NULL_POINTER:
        return ("JVMTI_ERROR_NULL_POINTER");
    case JVMTI_ERROR_ABSENT_INFORMATION:
        return ("JVMTI_ERROR_ABSENT_INFORMATION");
    case JVMTI_ERROR_INVALID_EVENT_TYPE:
        return ("JVMTI_ERROR_INVALID_EVENT_TYPE");
    case JVMTI_ERROR_ILLEGAL_ARGUMENT:
        return ("JVMTI_ERROR_ILLEGAL_ARGUMENT");
    case JVMTI_ERROR_NATIVE_METHOD:
        return ("JVMTI_ERROR_NATIVE_METHOD");
    case JVMTI_ERROR_OUT_OF_MEMORY:
        return ("JVMTI_ERROR_OUT_OF_MEMORY");
    case JVMTI_ERROR_ACCESS_DENIED:
        return ("JVMTI_ERROR_ACCESS_DENIED");
    case JVMTI_ERROR_WRONG_PHASE:
        return ("JVMTI_ERROR_WRONG_PHASE");
    case JVMTI_ERROR_INTERNAL:
        return ("JVMTI_ERROR_INTERNAL");
    case JVMTI_ERROR_UNATTACHED_THREAD:
        return ("JVMTI_ERROR_UNATTACHED_THREAD");
    case JVMTI_ERROR_INVALID_ENVIRONMENT:
        return ("JVMTI_ERROR_INVALID_ENVIRONMENT");
    default:
        return ("<unknown error>");
    }
}

const char* TranslatePhase(jvmtiPhase phase) {
    switch (phase) {
    case JVMTI_PHASE_ONLOAD:
        return ("JVMTI_PHASE_ONLOAD");
    case JVMTI_PHASE_PRIMORDIAL:
        return ("JVMTI_PHASE_PRIMORDIAL");
    case JVMTI_PHASE_START:
        return ("JVMTI_PHASE_START");
    case JVMTI_PHASE_LIVE:
        return ("JVMTI_PHASE_LIVE");
    case JVMTI_PHASE_DEAD:
        return ("JVMTI_PHASE_DEAD");
    default:
        return ("<unknown phase>");
    }
}

const char* TranslateRootKind(jvmtiHeapRootKind root) {
    switch (root) {
    case JVMTI_HEAP_ROOT_JNI_GLOBAL:
        return ("JVMTI_HEAP_ROOT_JNI_GLOBAL");
    case JVMTI_HEAP_ROOT_JNI_LOCAL:
        return ("JVMTI_HEAP_ROOT_JNI_LOCAL");
    case JVMTI_HEAP_ROOT_SYSTEM_CLASS:
        return ("JVMTI_HEAP_ROOT_SYSTEM_CLASS");
    case JVMTI_HEAP_ROOT_MONITOR:
        return ("JVMTI_HEAP_ROOT_MONITOR");
    case JVMTI_HEAP_ROOT_STACK_LOCAL:
        return ("JVMTI_HEAP_ROOT_STACK_LOCAL");
    case JVMTI_HEAP_ROOT_THREAD:
        return ("JVMTI_HEAP_ROOT_THREAD");
    case JVMTI_HEAP_ROOT_OTHER:
        return ("JVMTI_HEAP_ROOT_OTHER");
    default:
        return ("<unknown root kind>");
    }
}

const char* TranslateObjectRefKind(jvmtiObjectReferenceKind ref) {
    switch (ref) {
    case JVMTI_REFERENCE_CLASS:
        return ("JVMTI_REFERENCE_CLASS");
    case JVMTI_REFERENCE_FIELD:
        return ("JVMTI_REFERENCE_FIELD");
    case JVMTI_REFERENCE_ARRAY_ELEMENT:
        return ("JVMTI_REFERENCE_ARRAY_ELEMENT");
    case JVMTI_REFERENCE_CLASS_LOADER:
        return ("JVMTI_REFERENCE_CLASS_LOADER");
    case JVMTI_REFERENCE_SIGNERS:
        return ("JVMTI_REFERENCE_SIGNERS");
    case JVMTI_REFERENCE_PROTECTION_DOMAIN:
        return ("JVMTI_REFERENCE_PROTECTION_DOMAIN");
    case JVMTI_REFERENCE_INTERFACE:
        return ("JVMTI_REFERENCE_INTERFACE");
    case JVMTI_REFERENCE_STATIC_FIELD:
        return ("JVMTI_REFERENCE_STATIC_FIELD");
    case JVMTI_REFERENCE_CONSTANT_POOL:
        return ("JVMTI_REFERENCE_CONSTANT_POOL");
    default:
        return ("<unknown reference kind>");
    }
}

}
