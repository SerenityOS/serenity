/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LadybirdServiceBase.h"
#include <AK/Atomic.h>
#include <AK/Format.h>
#include <Ladybird/Utilities.h>
#include <jni.h>

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_LadybirdServiceBase_nativeThreadLoop(JNIEnv*, jobject /* thiz */, jint ipc_socket, jint fd_passing_socket)
{
    dbgln("New binding received, sockets {} and {}", ipc_socket, fd_passing_socket);
    auto ret = service_main(ipc_socket, fd_passing_socket);
    if (ret.is_error()) {
        warnln("Runtime Error: {}", ret.release_error());
    } else {
        outln("Thread exited with code {}", ret.release_value());
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_LadybirdServiceBase_initNativeCode(JNIEnv* env, jobject /* thiz */, jstring resource_dir, jstring tag_name)
{
    static Atomic<bool> s_initialized_flag { false };
    if (s_initialized_flag.exchange(true) == true) {
        // Skip initializing if someone else already started the process at some point in the past
        return;
    }

    char const* raw_resource_dir = env->GetStringUTFChars(resource_dir, nullptr);
    s_serenity_resource_root = raw_resource_dir;
    env->ReleaseStringUTFChars(resource_dir, raw_resource_dir);

    char const* raw_tag_name = env->GetStringUTFChars(tag_name, nullptr);
    AK::set_log_tag_name(raw_tag_name);
    env->ReleaseStringUTFChars(tag_name, raw_tag_name);
}
