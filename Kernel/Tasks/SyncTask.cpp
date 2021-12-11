/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Discard.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/SyncTask.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT void SyncTask::spawn()
{
    RefPtr<Thread> syncd_thread;
    discard(Process::create_kernel_process(syncd_thread, KString::must_create("SyncTask"), [] {
        dbgln("SyncTask is running");
        for (;;) {
            VirtualFileSystem::sync();
            discard(Thread::current()->sleep(Time::from_seconds(1)));
        }
    }));
}

}
