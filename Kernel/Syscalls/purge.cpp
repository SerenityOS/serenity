/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtrVector.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/InodeVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$purge(int mode)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_NO_PROMISES;
    if (!is_superuser())
        return EPERM;
    size_t purged_page_count = 0;
    if (mode & PURGE_ALL_VOLATILE) {
        NonnullRefPtrVector<Memory::AnonymousVMObject> vmobjects;
        {
            ErrorOr<void> result;
            Memory::MemoryManager::for_each_vmobject([&](auto& vmobject) {
                if (vmobject.is_anonymous()) {
                    // In the event that the append fails, only attempt to continue
                    // the purge if we have already appended something successfully.
                    if (auto append_result = vmobjects.try_append(static_cast<Memory::AnonymousVMObject&>(vmobject)); append_result.is_error() && vmobjects.is_empty()) {
                        result = append_result.release_error();
                        return IterationDecision::Break;
                    }
                }
                return IterationDecision::Continue;
            });

            if (result.is_error())
                return result.release_error();
        }
        for (auto& vmobject : vmobjects) {
            purged_page_count += vmobject.purge();
        }
    }
    if (mode & PURGE_ALL_CLEAN_INODE) {
        NonnullRefPtrVector<Memory::InodeVMObject> vmobjects;
        {
            ErrorOr<void> result;
            Memory::MemoryManager::for_each_vmobject([&](auto& vmobject) {
                if (vmobject.is_inode()) {
                    // In the event that the append fails, only attempt to continue
                    // the purge if we have already appended something successfully.
                    if (auto append_result = vmobjects.try_append(static_cast<Memory::InodeVMObject&>(vmobject)); append_result.is_error() && vmobjects.is_empty()) {
                        result = append_result.release_error();
                        return IterationDecision::Break;
                    }
                }
                return IterationDecision::Continue;
            });

            if (result.is_error())
                return result.release_error();
        }
        for (auto& vmobject : vmobjects) {
            purged_page_count += vmobject.release_all_clean_pages();
        }
    }
    return purged_page_count;
}

}
