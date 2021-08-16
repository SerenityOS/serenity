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

#ifndef JNIUTILS_H
#define JNIUTILS_H

#include <memory>
#include "jni.h"
#include "tstrings.h"


namespace jni {

struct JniObjWithEnv {
    JniObjWithEnv(): env(0), obj(0) {

    }

    JniObjWithEnv(JNIEnv *env, jobject obj) : env(env), obj(obj) {
    }

    bool operator == (const JniObjWithEnv& other) const {
        return env == other.env && obj == other.obj;
    }

    bool operator != (const JniObjWithEnv& other) const {
        return ! operator == (other);
    }

    explicit operator bool() const {
        return env && obj;
    }

    JNIEnv *env;
    jobject obj;

    struct LocalRefDeleter {
        typedef JniObjWithEnv pointer;

        void operator()(pointer v);
    };
};

typedef std::unique_ptr<JniObjWithEnv, JniObjWithEnv::LocalRefDeleter> LocalRef;

tstring toUnicodeString(JNIEnv *env, jstring val);

jstring toJString(JNIEnv *env, const tstring& val);

tstring_array toUnicodeStringArray(JNIEnv *env, jobjectArray val);

} // namespace jni

#endif // JNIUTILS_H
