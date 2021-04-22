/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>
#include <Kernel/Tasks/SyncTask.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

void SyncTask::spawn()
{
    RefPtr<Thread> syncd_thread;
    Process::create_kernel_process(syncd_thread, "SyncTask", [] {
        dbgln("SyncTask is running");
        for (;;) {
            VFS::the().sync();
            (void)Thread::current()->sleep(Time::from_seconds(1));
        }
    });
}

}
