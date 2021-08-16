/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CICLASSLIST_HPP
#define SHARE_CI_CICLASSLIST_HPP

class ciEnv;
class ciObjectFactory;
class ciConstantPoolCache;

class ciField;
class ciConstant;
class ciFlags;
class ciExceptionHandler;
class ciCallProfile;
class ciSignature;

class ciBytecodeStream;
class ciSignatureStream;
class ciExceptionHandlerStream;

class ciTypeFlow;

class ciBaseObject;
class ciObject;
class   ciNullObject;
class   ciInstance;
class     ciCallSite;
class     ciMemberName;
class     ciNativeEntryPoint;
class     ciMethodHandle;
class     ciMethodType;
class     ciArray;
class       ciObjArray;
class       ciTypeArray;
class   ciSymbol;
class   ciMetadata;
class   ciMethod;
class   ciMethodData;
class     ciReceiverTypeData;  // part of ciMethodData
class   ciType;
class    ciReturnAddress;
class    ciKlass;
class     ciInstanceKlass;
class     ciArrayKlass;
class       ciObjArrayKlass;
class       ciTypeArrayKlass;

// Simulate Java Language style package-private access with
// friend declarations.
// This is a great idea but gcc and other C++ compilers give an
// error for being friends with yourself, so this macro does not
// compile on some platforms.

// Everyone gives access to ciObjectFactory
#define CI_PACKAGE_ACCESS \
friend class ciObjectFactory; \
friend class VMStructs;

// These are the packages that have access to ciEnv
// Any more access must be given explicitly.
#define CI_PACKAGE_ACCESS_TO           \
friend class ciObjectFactory;          \
friend class VMStructs;                \
friend class ciCallSite;               \
friend class ciConstantPoolCache;      \
friend class ciField;                  \
friend class ciConstant;               \
friend class ciFlags;                  \
friend class ciExceptionHandler;       \
friend class ciCallProfile;            \
friend class ciSignature;              \
friend class ciBytecodeStream;         \
friend class ciSignatureStream;        \
friend class ciExceptionHandlerStream; \
friend class ciObject;                 \
friend class ciNullObject;             \
friend class ciInstance;               \
friend class ciMemberName;             \
friend class ciNativeEntryPoint;       \
friend class ciMethod;                 \
friend class ciMethodData;             \
friend class ciMethodHandle;           \
friend class ciMethodType;             \
friend class ciReceiverTypeData;       \
friend class ciTypeEntries;            \
friend class ciSpeculativeTrapData;    \
friend class ciSymbol;                 \
friend class ciArray;                  \
friend class ciObjArray;               \
friend class ciMetadata;               \
friend class ciReplay;                 \
friend class ciTypeArray;              \
friend class ciType;                   \
friend class ciReturnAddress;          \
friend class ciKlass;                  \
friend class ciInstanceKlass;          \
friend class ciArrayKlass;             \
friend class ciObjArrayKlass;          \
friend class ciTypeArrayKlass;         \

#endif // SHARE_CI_CICLASSLIST_HPP
