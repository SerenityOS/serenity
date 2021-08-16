/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * This file primarily consists of all the error and warning messages, that
 * are used in JLI_ReportErrorMessage. All message must be defined here, in
 * order to help with localizing the messages.
 */

#ifndef _EMESSAGES_H
#define _EMESSAGES_H

#define GEN_ERROR       "Error: A fatal exception has occurred. Program will exit."
#define JNI_ERROR       "Error: A JNI error has occurred, please check your installation and try again"
#define JNI_ERROR1      "Error: can't find JNI interfaces in: %s"

#define ARG_INFO_ENVVAR "NOTE: Picked up %s: %s"
#define ARG_WARN        "Warning: %s option is no longer supported."
#define ARG_DEPRECATED  "Warning: %s option is deprecated and may be removed in a future release."

#define ARG_ERROR1      "Error: %s requires class path specification"
#define ARG_ERROR2      "Error: %s requires jar file specification"
#define ARG_ERROR3      "Error: The -J option should not be followed by a space."
#define ARG_ERROR4      "Error: %s requires module path specification"
#define ARG_ERROR5      "Error: %s requires module name"
#define ARG_ERROR6      "Error: %s requires modules to be specified"
#define ARG_ERROR7      "Error: %s can only be specified once"
#define ARG_ERROR8      "Error: Unmatched quote in environment variable %s"
#define ARG_ERROR9      "Error: Option %s is not allowed in environment variable %s"
#define ARG_ERROR10     "Error: Option %s in %s is not allowed in environment variable %s"
#define ARG_ERROR11     "Error: Cannot specify main class in environment variable %s"
#define ARG_ERROR12     "Error: %s requires module name"
#define ARG_ERROR13     "Error: %s requires source version"
#define ARG_ERROR14     "Error: Option %s is not allowed with --source"
#define ARG_ERROR15     "Error: Option %s is not allowed in this context"
#define ARG_ERROR16     "Error: Option %s in %s is not allowed in this context"
#define ARG_ERROR17     "Error: Cannot specify main class in this context"

#define JVM_ERROR1      "Error: Could not create the Java Virtual Machine.\n" GEN_ERROR
#define JVM_ERROR2      "Error: Could not detach main thread.\n" JNI_ERROR

#define JAR_ERROR1      "Error: Failed to load Main-Class manifest attribute from\n%s\n%s"
#define JAR_ERROR2      "Error: Unable to access jarfile %s"
#define JAR_ERROR3      "Error: Invalid or corrupt jarfile %s"

#define CLS_ERROR1      "Error: Could not find the main class %s.\n" JNI_ERROR
#define CLS_ERROR2      "Error: Failed to load Main Class: %s\n%s"
#define CLS_ERROR3      "Error: No main method found in specified class.\n" GEN_ERROR
#define CLS_ERROR4      "Error: Main method not public\n" GEN_ERROR
#define CLS_ERROR5      "Error: main-class: attribute exceeds system limits of %d bytes\n" GEN_ERROR

#define CFG_WARN1       "Warning: %s VM not supported; %s VM will be used"
#define CFG_WARN2       "Warning: No leading - on line %d of `%s'"
#define CFG_WARN3       "Warning: Missing VM type on line %d of `%s'"
#define CFG_WARN4       "Warning: Missing server class VM on line %d of `%s'"
#define CFG_WARN5       "Warning: Unknown VM type on line %d of `%s'"

#define CFG_ERROR1      "Error: Corrupt jvm.cfg file; cycle in alias list."
#define CFG_ERROR2      "Error: Unable to resolve VM alias %s"
#define CFG_ERROR3      "Error: %s VM not supported"
#define CFG_ERROR4      "Error: Unable to locate JRE meeting specification \"%s\""
#define CFG_ERROR5      "Error: Could not determine application home."
#define CFG_ERROR6      "Error: could not open `%s'"
#define CFG_ERROR7      "Error: no known VMs. (check for corrupt jvm.cfg file)"
#define CFG_ERROR8      "Error: missing `%s' JVM at `%s'.\nPlease install or use the JRE or JDK that contains these missing components."
#define CFG_ERROR9      "Error: could not determine JVM type."
#define CFG_ERROR10     "Error: Argument file size should not be larger than %lu."

#define JRE_ERROR1      "Error: Could not find Java SE Runtime Environment."
#define JRE_ERROR2      "Error: This Java instance does not support a %d-bit JVM.\nPlease install the desired version."
#define JRE_ERROR3      "Error: Improper value at line %d."
#define JRE_ERROR4      "Error: trying to exec %s.\nCheck if file exists and permissions are set correctly."
#define JRE_ERROR5      "Error: Failed to start a %d-bit JVM process from a %d-bit JVM."
#define JRE_ERROR6      "Error: Verify all necessary Java SE components have been installed."
#define JRE_ERROR7      "Error: Either 64-bit processes are not supported by this platform\nor the 64-bit components have not been installed."
#define JRE_ERROR8      "Error: could not find "
#define JRE_ERROR9      "Error: Unable to resolve %s"
#define JRE_ERROR10     "Error: Unable to resolve current executable"
#define JRE_ERROR11     "Error: Path length exceeds maximum length (PATH_MAX)"
#define JRE_ERROR12     "Error: Exec of %s failed"
#define JRE_ERROR13     "Error: String processing operation failed"

#define SPC_ERROR1      "Error: Specifying an alternate JDK/JRE version is no longer supported.\n  The use of the flag '-version:' is no longer valid.\n  Please download and execute the appropriate version."
#define SPC_ERROR2      "Error: Specifying an alternate JDK/JRE is no longer supported.\n  The related flags -jre-restrict-search | -jre-no-restrict-search are also no longer valid."

#define DLL_ERROR1      "Error: dl failure on line %d"
#define DLL_ERROR2      "Error: failed %s, because %s"
#define DLL_ERROR3      "Error: could not find executable %s"
#define DLL_ERROR4      "Error: loading: %s"

#define REG_ERROR1      "Error: opening registry key '%s'"
#define REG_ERROR2      "Error: Failed reading value of registry key:\n\t%s\\CurrentVersion"
#define REG_ERROR3      "Error: Registry key '%s'\\CurrentVersion'\nhas value '%s', but '%s' is required."
#define REG_ERROR4      "Failed reading value of registry key:\n\t%s\\%s\\JavaHome"

#define SYS_ERROR1      "Error: CreateProcess(%s, ...) failed:"
#define SYS_ERROR2      "Error: WaitForSingleObject() failed."



#endif /* _EMESSAGES_H */
