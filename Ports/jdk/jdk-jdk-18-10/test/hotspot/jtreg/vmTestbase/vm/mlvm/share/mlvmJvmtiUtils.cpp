/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"
#include "mlvmJvmtiUtils.h"

extern "C" {

void copyFromJString(JNIEnv * pEnv, jstring src, char ** dst) {
    const char * pStr;
        jsize len;

    if (!NSK_VERIFY((pStr = pEnv->GetStringUTFChars(src, NULL)) != NULL)) {
        return;
    }

    len = pEnv->GetStringUTFLength(src) + 1;
    *dst = (char*) malloc(len);
    strncpy(*dst, pStr, len);

    pEnv->ReleaseStringUTFChars(src, pStr);
}


/**
 * Helper class to track JVMTI resources, deallocating the resource in the destructor.
 */
class JvmtiResource {
private:
  jvmtiEnv* const _jvmtiEnv;
  void* const _ptr;

public:
  JvmtiResource(jvmtiEnv* jvmtiEnv, void* ptr) : _jvmtiEnv(jvmtiEnv), _ptr(ptr) { }

  ~JvmtiResource() {
    NSK_JVMTI_VERIFY(_jvmtiEnv->Deallocate((unsigned char*)_ptr));
  }
};

struct MethodName * getMethodName(jvmtiEnv * pJvmtiEnv, jmethodID method) {
    char * szName;
    char * szSignature;
    jclass clazz;
    struct MethodName * mn;

    if (!NSK_JVMTI_VERIFY(pJvmtiEnv->GetMethodName(method, &szName, NULL, NULL))) {
        return NULL;
    }

    JvmtiResource szNameResource(pJvmtiEnv, szName);

    if (!NSK_JVMTI_VERIFY(pJvmtiEnv->GetMethodDeclaringClass(method, &clazz))) {
        return NULL;
    }

    if (!NSK_JVMTI_VERIFY(pJvmtiEnv->GetClassSignature(clazz, &szSignature, NULL))) {
        return NULL;
    }

    JvmtiResource szSignatureResource(pJvmtiEnv, szSignature);

    if (strlen(szName) + 1 > sizeof(mn->methodName) ||
        strlen(szSignature) + 1 > sizeof(mn->classSig)) {
      return NULL;
    }

    mn = (MethodName*) malloc(sizeof(MethodNameStruct));
    if (mn == NULL) {
      return NULL;
    }

    strncpy(mn->methodName, szName, sizeof(mn->methodName) - 1);
    mn->methodName[sizeof(mn->methodName) - 1] = '\0';

    strncpy(mn->classSig, szSignature, sizeof(mn->classSig) - 1);
    mn->classSig[sizeof(mn->classSig) - 1] = '\0';

    return mn;
}

char * locationToString(jvmtiEnv * pJvmtiEnv, jmethodID method, jlocation location) {
    struct MethodName * pMN;
    int len;
    char * result;
    const char * const format = "%s .%s :" JLONG_FORMAT;

    pMN = getMethodName(pJvmtiEnv, method);
    if (!pMN)
        return strdup("NONE");

    len = snprintf(NULL, 0, format, pMN->classSig, pMN->methodName, location) + 1;

    if (len <= 0) {
        free(pMN);
        return NULL;
    }

    result = (char*) malloc(len);
    if (result == NULL) {
        free(pMN);
        return NULL;
    }

    snprintf(result, len, format, pMN->classSig, pMN->methodName, location);

    free(pMN);
    return result;
}

void * getTLS(jvmtiEnv * pJvmtiEnv, jthread thread, jsize sizeToAllocate) {
    void * tls;
    if (!NSK_JVMTI_VERIFY(pJvmtiEnv->GetThreadLocalStorage(thread, &tls)))
        return NULL;

    if (!tls) {
        if (!NSK_VERIFY((tls = malloc(sizeToAllocate)) != NULL))
            return NULL;

        memset(tls, 0, sizeToAllocate);

        if (!NSK_JVMTI_VERIFY(pJvmtiEnv->SetThreadLocalStorage(thread, tls)))
            return NULL;
    }

    return tls;
}

}
