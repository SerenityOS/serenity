/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ALooperEventLoopImplementation.h"
#include <AK/Format.h>
#include <AK/OwnPtr.h>
#include <Ladybird/Utilities.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <jni.h>

OwnPtr<Core::EventLoop> s_main_event_loop;

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_LadybirdActivity_initNativeCode(JNIEnv* env, jobject /* thiz */, jstring resource_dir, jstring tag_name, jobject timer_service)
{
    char const* raw_resource_dir = env->GetStringUTFChars(resource_dir, nullptr);
    s_serenity_resource_root = raw_resource_dir;
    env->ReleaseStringUTFChars(resource_dir, raw_resource_dir);

    char const* raw_tag_name = env->GetStringUTFChars(tag_name, nullptr);
    AK::set_log_tag_name(raw_tag_name);
    env->ReleaseStringUTFChars(tag_name, raw_tag_name);

    dbgln("Set resource dir to {}", s_serenity_resource_root);

    jobject timer_service_ref = env->NewGlobalRef(timer_service);
    JavaVM* vm = nullptr;
    jint ret = env->GetJavaVM(&vm);
    VERIFY(ret == 0);
    Core::EventLoopManager::install(*new Ladybird::ALooperEventLoopManager(vm, timer_service_ref));
    s_main_event_loop = make<Core::EventLoop>();
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_LadybirdActivity_execMainEventLoop(JNIEnv*, jobject /* thiz */)
{
    s_main_event_loop->pump(Core::EventLoop::WaitMode::PollForEvents);
}
