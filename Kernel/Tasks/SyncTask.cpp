/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/SyncTask.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT void SyncTask::spawn()
{
    LockRefPtr<Thread> syncd_thread;
    (void)Process::create_kernel_process(syncd_thread, KString::must_create("VFS Sync Task"sv), [] {
        dbgln("VFS SyncTask is running");
        for (;;) {
            VirtualFileSystem::sync();
            (void)Thread::current()->sleep(Time::from_seconds(1));
        }
    });
}

}
