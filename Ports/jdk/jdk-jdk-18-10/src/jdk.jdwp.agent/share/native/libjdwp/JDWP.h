/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JDWP_JDWP_H
#define JDWP_JDWP_H

#include "JDWPCommands.h"

/*
 * JDWPCommands.h is the javah'ed version of all the constants defined
 * com.sun.tools.jdi.JDWP and all its nested classes. Since the names are
 * very long, the macros below are provided for convenience.
 */

#define JDWP_COMMAND_SET(name) JDWP_ ## name
#define JDWP_COMMAND(set, name) JDWP_ ## set ## _ ## name
#define JDWP_REQUEST_MODIFIER(name) \
           JDWP_EventRequest_Set_Out_modifiers_Modifier_ ## name
#define JDWP_EVENT(name) \
           JDWP_EventKind_ ## name
#define JDWP_THREAD_STATUS(name) \
           JDWP_ThreadStatus_ ## name
#define JDWP_SUSPEND_STATUS(name) \
           JDWP_SuspendStatus_SUSPEND_STATUS_ ## name
#define JDWP_CLASS_STATUS(name) \
           JDWP_ClassStatus_ ## name
#define JDWP_TYPE_TAG(name) \
           JDWP_TypeTag_ ## name
#define JDWP_TAG(name) \
           JDWP_Tag_ ## name
#define JDWP_STEP_DEPTH(name) \
           JDWP_StepDepth_ ## name
#define JDWP_STEP_SIZE(name) \
           JDWP_StepSize_ ## name
#define JDWP_SUSPEND_POLICY(name) \
           JDWP_SuspendPolicy_ ## name
#define JDWP_INVOKE_OPTIONS(name) \
           JDWP_InvokeOptions_INVOKE_ ## name
#define JDWP_ERROR(name) \
           JDWP_Error_ ## name
#define JDWP_HIGHEST_COMMAND_SET 18
#define JDWP_REQUEST_NONE        -1

/* This typedef helps keep the event and error types straight. */
typedef unsigned short jdwpError;
typedef unsigned char  jdwpEvent;
typedef jint           jdwpThreadStatus;

#endif
