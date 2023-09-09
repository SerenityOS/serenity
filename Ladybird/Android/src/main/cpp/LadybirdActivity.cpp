/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ALooperEventLoopImplementation.h"
#include <AK/OwnPtr.h>
#include <Ladybird/Utilities.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <android/log.h>
#include <jni.h>

OwnPtr<Core::EventLoop> s_main_event_loop;
RefPtr<Core::Timer> s_timer;

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_LadybirdActivity_initNativeCode(JNIEnv* env, jobject /* thiz */, jstring resource_dir, jobject timer_service)
{
    char const* raw_resource_dir = env->GetStringUTFChars(resource_dir, nullptr);
    s_serenity_resource_root = raw_resource_dir;
    __android_log_print(ANDROID_LOG_INFO, "Ladybird", "Serenity resource dir is %s", s_serenity_resource_root.characters());
    env->ReleaseStringUTFChars(resource_dir, raw_resource_dir);

    jobject timer_service_ref = env->NewGlobalRef(timer_service);
    JavaVM* vm = nullptr;
    jint ret = env->GetJavaVM(&vm);
    VERIFY(ret == 0);
    Core::EventLoopManager::install(*new Ladybird::ALooperEventLoopManager(vm, timer_service_ref));
    s_main_event_loop = make<Core::EventLoop>();

    s_timer = MUST(Core::Timer::create_repeating(1000, [] {
        __android_log_print(ANDROID_LOG_DEBUG, "Ladybird", "EventLoop is alive!");
    }));
    s_timer->start();
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_LadybirdActivity_execMainEventLoop(JNIEnv*, jobject /* thiz */)
{
    s_main_event_loop->pump(Core::EventLoop::WaitMode::PollForEvents);
}
