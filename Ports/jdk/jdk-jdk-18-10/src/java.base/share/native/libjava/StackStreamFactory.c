/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 *      Implementation of class StackStreamfactory and AbstractStackWalker
 */

#include <stdio.h>
#include <signal.h>

#include "jni.h"
#include "jvm.h"

#include "java_lang_StackStreamFactory.h"
#include "java_lang_StackStreamFactory_AbstractStackWalker.h"

/*
 * Class:     java_lang_StackStreamFactory
 * Method:    checkStackWalkModes
 * Signature: ()
 */
JNIEXPORT jboolean JNICALL Java_java_lang_StackStreamFactory_checkStackWalkModes
  (JNIEnv *env, jclass dummy)
{
   return JVM_STACKWALK_FILL_CLASS_REFS_ONLY == java_lang_StackStreamFactory_FILL_CLASS_REFS_ONLY &&
          JVM_STACKWALK_SHOW_HIDDEN_FRAMES == java_lang_StackStreamFactory_SHOW_HIDDEN_FRAMES &&
          JVM_STACKWALK_FILL_LIVE_STACK_FRAMES == java_lang_StackStreamFactory_FILL_LIVE_STACK_FRAMES;
}

/*
 * Class:     java_lang_StackStreamFactory_AbstractStackWalker
 * Method:    callStackWalk
 * Signature: (JIII[Ljava/lang/Object;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_java_lang_StackStreamFactory_00024AbstractStackWalker_callStackWalk
  (JNIEnv *env, jobject stackstream, jlong mode, jint skipFrames, jint batchSize, jint startIndex,
   jobjectArray frames)
{
    return JVM_CallStackWalk(env, stackstream, mode, skipFrames, batchSize,
                             startIndex, frames);
}

/*
 * Class:     java_lang_StackStreamFactory_AbstractStackWalker
 * Method:    fetchStackFrames
 * Signature: (JJII[Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_java_lang_StackStreamFactory_00024AbstractStackWalker_fetchStackFrames
  (JNIEnv *env, jobject stackstream, jlong mode, jlong anchor,
   jint batchSize, jint startIndex,
   jobjectArray frames)
{
    return JVM_MoreStackWalk(env, stackstream, mode, anchor, batchSize,
                             startIndex, frames);
}
