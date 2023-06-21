/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/SyncTask.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT void SyncTask::spawn()
{
    MUST(Process::create_kernel_process(KString::must_create("VFS Sync Task"sv), [] {
        dbgln("VFS SyncTask is running");
        for (;;) {
            VirtualFileSystem::sync();
            (void)Thread::current()->sleep(Duration::from_seconds(1));
        }
    }));
}

}
