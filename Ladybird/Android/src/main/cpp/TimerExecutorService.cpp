/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ALooperEventLoopImplementation.h"
#include <LibCore/EventLoop.h>
#include <jni.h>

extern "C" JNIEXPORT void JNICALL Java_org_serenityos_ladybird_TimerExecutorService_00024Timer_nativeRun(JNIEnv*, jobject /* thiz */, jlong native_data, jlong id)
{
    auto& thread_data = *reinterpret_cast<Ladybird::EventLoopThreadData*>(native_data);

    if (auto timer_data = thread_data.timers.get(id); timer_data.has_value()) {
        auto receiver = timer_data->receiver.strong_ref();
        if (!receiver)
            return;

        if (timer_data->visibility == Core::TimerShouldFireWhenNotVisible::No)
            if (!receiver->is_visible_for_timer_purposes())
                return;

        Core::TimerEvent event(id);

        // FIXME: Should the dispatch happen on the thread that registered the timer?
        receiver->dispatch_event(event);
    }
}
