/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 */

/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/* Copyright  (c) 2002 Graz University of Technology. All rights reserved.
 *
 * Redistribution and use in  source and binary forms, with or without
 * modification, are permitted  provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in  binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment:
 *
 *    "This product includes software developed by IAIK of Graz University of
 *     Technology."
 *
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Graz University of Technology" and "IAIK of Graz University of
 *    Technology" must not be used to endorse or promote products derived from
 *    this software without prior written permission.
 *
 * 5. Products derived from this software may not be called
 *    "IAIK PKCS Wrapper", nor may "IAIK" appear in their name, without prior
 *    written permission of Graz University of Technology.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE LICENSOR BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY  OF SUCH DAMAGE.
 */

/*
 * platoform.h
 * 10.12.2001
 *
 * declaration of all platform dependent functions used by pkcs11wrapper.c
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 */

/* defines for WIN32 platform *************************************************/

#include <windows.h>

/* statement according to PKCS11 docu */
#pragma pack(push, cryptoki, 1)

/* definitions according to PKCS#11 docu for Win32 environment */
#define CK_PTR *
#define CK_DEFINE_FUNCTION(returnType, name) returnType __declspec(dllexport) name
#define CK_DECLARE_FUNCTION(returnType, name) returnType __declspec(dllimport) name
#define CK_DECLARE_FUNCTION_POINTER(returnType, name) returnType __declspec(dllimport) (* name)
#define CK_CALLBACK_FUNCTION(returnType, name) returnType (* name)
#ifndef NULL_PTR
#define NULL_PTR 0
#endif /* NULL_PTR */

/* to avoid clash with Win32 #define */
#ifdef CreateMutex
#undef CreateMutex
#endif /* CreateMutex */

#include "pkcs11.h"
#include "pkcs11gcm2.h"

/* statement according to PKCS11 docu */
#pragma pack(pop, cryptoki)

#include "jni.h"

/* A data structure to hold required information about a PKCS#11 module. */
struct ModuleData {

    HINSTANCE hModule;

    /* The pointer to the PKCS#11 functions of this module. */
    CK_FUNCTION_LIST_PTR ckFunctionListPtr;

    /* Reference to the object to use for mutex handling. NULL, if not used. */
    jobject applicationMutexHandler;

};
typedef struct ModuleData ModuleData;
