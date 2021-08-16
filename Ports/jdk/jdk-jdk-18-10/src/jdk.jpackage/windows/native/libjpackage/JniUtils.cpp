/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "JniUtils.h"
#include "ErrorHandling.h"
#include "Toolbox.h"


namespace jni {

void JniObjWithEnv::LocalRefDeleter::operator()(pointer v) {
    if (v.env && v.obj) {
        v.env->DeleteLocalRef(v.obj);
    }
}


#ifdef TSTRINGS_WITH_WCHAR
std::wstring toUnicodeString(JNIEnv *env, jstring val) {
    const jchar* chars = env->GetStringChars(val, 0);
    if (!chars) {
        JP_THROW("GetStringChars() failed");
    }

    const auto releaseStringChars =
            runAtEndOfScope([env, val, chars]() -> void {
        env->ReleaseStringChars(val, chars);
    });

    const jsize len = env->GetStringLength(val);

    return std::wstring(reinterpret_cast<const wchar_t*>(chars), len);
}


jstring toJString(JNIEnv *env, const std::wstring& val) {
    jstring result = env->NewString(
            reinterpret_cast<const jchar*>(val.c_str()), jsize(val.size()));
    if (!result) {
        JP_THROW("NewString() failed");
    }
    return result;
}
#endif


tstring_array toUnicodeStringArray(JNIEnv *env, jobjectArray val) {
    tstring_array result;

    const jsize len = env->GetArrayLength(val);
    for (int i = 0; i < len; ++i) {
        LocalRef localRef(JniObjWithEnv(env,
                env->GetObjectArrayElement(val, i)));
        result.push_back(toUnicodeString(env,
                static_cast<jstring>(localRef.get().obj)));
    }

    return result;
}

} // namespace jni
