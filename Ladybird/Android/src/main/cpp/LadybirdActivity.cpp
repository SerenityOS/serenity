/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Ladybird/Utilities.h>
#include <android/log.h>
#include <jni.h>

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_LadybirdActivity_initNativeCode(JNIEnv* env, jobject /* thiz */, jstring resource_dir)
{
    char const* raw_resource_dir = env->GetStringUTFChars(resource_dir, nullptr);
    s_serenity_resource_root = raw_resource_dir;
    __android_log_print(ANDROID_LOG_INFO, "Ladybird", "Serenity resource dir is %s", s_serenity_resource_root.characters());
    env->ReleaseStringUTFChars(resource_dir, raw_resource_dir);
}
