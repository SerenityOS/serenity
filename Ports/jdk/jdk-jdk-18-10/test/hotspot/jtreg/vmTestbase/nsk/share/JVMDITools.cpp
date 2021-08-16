/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "jvmdi.h"
#include "JVMDITools.h"

extern "C" {

char const *TranslateEvent(jint kind) {
    switch (kind) {
    case JVMDI_EVENT_SINGLE_STEP:
        return ("JVMDI_EVENT_SINGLE_STEP");
    case JVMDI_EVENT_BREAKPOINT:
        return ("JVMDI_EVENT_BREAKPOINT");
    case JVMDI_EVENT_FRAME_POP:
        return ("JVMDI_EVENT_FRAME_POP");
    case JVMDI_EVENT_EXCEPTION:
        return ("JVMDI_EVENT_EXCEPTION");
    case JVMDI_EVENT_USER_DEFINED:
        return ("JVMDI_EVENT_USER_DEFINED");
    case JVMDI_EVENT_THREAD_START:
        return ("JVMDI_EVENT_THREAD_START");
    case JVMDI_EVENT_THREAD_END:
        return ("JVMDI_EVENT_THREAD_END");
    case JVMDI_EVENT_CLASS_PREPARE:
        return ("JVMDI_EVENT_CLASS_PREPARE");
    case JVMDI_EVENT_CLASS_UNLOAD:
        return ("JVMDI_EVENT_CLASS_UNLOAD");
    case JVMDI_EVENT_CLASS_LOAD:
        return ("JVMDI_EVENT_CLASS_LOAD");
    case JVMDI_EVENT_FIELD_ACCESS:
        return ("JVMDI_EVENT_FIELD_ACCESS");
    case JVMDI_EVENT_FIELD_MODIFICATION:
        return ("JVMDI_EVENT_FIELD_MODIFICATION");
    case JVMDI_EVENT_EXCEPTION_CATCH:
        return ("JVMDI_EVENT_EXCEPTION_CATCH");
    case JVMDI_EVENT_METHOD_ENTRY:
        return ("JVMDI_EVENT_METHOD_ENTRY");
    case JVMDI_EVENT_METHOD_EXIT:
        return ("JVMDI_EVENT_METHOD_EXIT");
    case JVMDI_EVENT_VM_INIT:
        return ("JVMDI_EVENT_VM_INIT");
    case JVMDI_EVENT_VM_DEATH:
        return ("JVMDI_EVENT_VM_DEATH");
    default:
        return ("<Unknown Event>");
    }
}

char const *TranslateError(jvmdiError err) {
    switch (err) {
    case JVMDI_ERROR_NONE:
        return ("JVMDI_ERROR_NONE");
    case JVMDI_ERROR_OUT_OF_MEMORY:
        return ("JVMDI_ERROR_OUT_OF_MEMORY");
    case JVMDI_ERROR_ACCESS_DENIED:
        return ("JVMDI_ERROR_ACCESS_DENIED");
    case JVMDI_ERROR_UNATTACHED_THREAD:
        return ("JVMDI_ERROR_UNATTACHED_THREAD");
    case JVMDI_ERROR_VM_DEAD:
        return ("JVMDI_ERROR_VM_DEAD");
    case JVMDI_ERROR_INTERNAL:
        return ("JVMDI_ERROR_INTERNAL");
    case JVMDI_ERROR_INVALID_THREAD:
        return ("JVMDI_ERROR_INVALID_THREAD");
    case JVMDI_ERROR_INVALID_FIELDID:
        return ("JVMDI_ERROR_INVALID_FIELDID");
    case JVMDI_ERROR_INVALID_METHODID:
        return ("JVMDI_ERROR_INVALID_METHODID");
    case JVMDI_ERROR_INVALID_LOCATION:
        return ("JVMDI_ERROR_INVALID_LOCATION");
    case JVMDI_ERROR_INVALID_FRAMEID:
        return ("JVMDI_ERROR_INVALID_FRAMEID");
    case JVMDI_ERROR_NO_MORE_FRAMES:
        return ("JVMDI_ERROR_NO_MORE_FRAMES");
    case JVMDI_ERROR_OPAQUE_FRAME:
        return ("JVMDI_ERROR_OPAQUE_FRAME");
    case JVMDI_ERROR_NOT_CURRENT_FRAME:
        return ("JVMDI_ERROR_NOT_CURRENT_FRAME");
    case JVMDI_ERROR_TYPE_MISMATCH:
        return ("JVMDI_ERROR_TYPE_MISMATCH");
    case JVMDI_ERROR_INVALID_SLOT:
        return ("JVMDI_ERROR_INVALID_SLOT");
    case JVMDI_ERROR_DUPLICATE:
        return ("JVMDI_ERROR_DUPLICATE");
    case JVMDI_ERROR_THREAD_NOT_SUSPENDED:
        return ("JVMDI_ERROR_THREAD_NOT_SUSPENDED");
    case JVMDI_ERROR_THREAD_SUSPENDED:
        return ("JVMDI_ERROR_THREAD_SUSPENDED");
    case JVMDI_ERROR_INVALID_OBJECT:
        return ("JVMDI_ERROR_INVALID_OBJECT");
    case JVMDI_ERROR_INVALID_CLASS:
        return ("JVMDI_ERROR_INVALID_CLASS");
    case JVMDI_ERROR_CLASS_NOT_PREPARED:
        return ("JVMDI_ERROR_CLASS_NOT_PREPARED");
    case JVMDI_ERROR_NULL_POINTER:
        return ("JVMDI_ERROR_NULL_POINTER");
    case JVMDI_ERROR_ABSENT_INFORMATION:
        return ("JVMDI_ERROR_ABSENT_INFORMATION");
    case JVMDI_ERROR_INVALID_EVENT_TYPE:
        return ("JVMDI_ERROR_INVALID_EVENT_TYPE");
    case JVMDI_ERROR_NOT_IMPLEMENTED:
        return ("JVMDI_ERROR_NOT_IMPLEMENTED");
    case JVMDI_ERROR_INVALID_THREAD_GROUP:
        return ("JVMDI_ERROR_INVALID_THREAD_GROUP");
    case JVMDI_ERROR_INVALID_PRIORITY:
        return ("JVMDI_ERROR_INVALID_PRIORITY");
    case JVMDI_ERROR_NOT_FOUND:
        return ("JVMDI_ERROR_NOT_FOUND");
    case JVMDI_ERROR_INVALID_MONITOR:
        return ("JVMDI_ERROR_INVALID_MONITOR");
    case JVMDI_ERROR_ILLEGAL_ARGUMENT:
        return ("JVMDI_ERROR_ILLEGAL_ARGUMENT");
    case JVMDI_ERROR_NOT_MONITOR_OWNER:
        return ("JVMDI_ERROR_NOT_MONITOR_OWNER");
    case JVMDI_ERROR_INTERRUPT:
        return ("JVMDI_ERROR_INTERRUPT");
    case JVMDI_ERROR_INVALID_TYPESTATE:
        return ("JVMDI_ERROR_INVALID_TYPESTATE");
    case JVMDI_ERROR_INVALID_CLASS_FORMAT:
        return ("JVMDI_ERROR_INVALID_CLASS_FORMAT");
    case JVMDI_ERROR_CIRCULAR_CLASS_DEFINITION:
        return ("JVMDI_ERROR_CIRCULAR_CLASS_DEFINITION");
    case JVMDI_ERROR_ADD_METHOD_NOT_IMPLEMENTED:
        return ("JVMDI_ERROR_ADD_METHOD_NOT_IMPLEMENTED");
    case JVMDI_ERROR_SCHEMA_CHANGE_NOT_IMPLEMENTED:
        return ("JVMDI_ERROR_SCHEMA_CHANGE_NOT_IMPLEMENTED");
    case JVMDI_ERROR_FAILS_VERIFICATION:
        return ("JVMDI_ERROR_FAILS_VERIFICATION");
#ifdef JVMDI_VERSION_1_2
    case JVMDI_ERROR_UNSUPPORTED_VERSION:
        return ("JVMDI_ERROR_UNSUPPORTED_VERSION");
    case JVMDI_ERROR_HIERARCHY_CHANGE_NOT_IMPLEMENTED:
        return ("JVMDI_ERROR_HIERARCHY_CHANGE_NOT_IMPLEMENTED");
    case JVMDI_ERROR_DELETE_METHOD_NOT_IMPLEMENTED:
        return ("JVMDI_ERROR_DELETE_METHOD_NOT_IMPLEMENTED");
    case JVMDI_ERROR_NAMES_DONT_MATCH:
        return ("JVMDI_ERROR_NAMES_DONT_MATCH");
    case JVMDI_ERROR_CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED:
        return ("JVMDI_ERROR_CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED");
    case JVMDI_ERROR_METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED:
        return ("JVMDI_ERROR_METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED");
#endif
    default:
        return ("<Unknown Error>");
    }
}

}
