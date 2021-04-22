/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtrVector.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

KResultOr<int> Process::sys$purge(int mode)
{
    REQUIRE_NO_PROMISES;
    if (!is_superuser())
        return EPERM;
    int purged_page_count = 0;
    if (mode & PURGE_ALL_VOLATILE) {
        NonnullRefPtrVector<AnonymousVMObject> vmobjects;
        {
            InterruptDisabler disabler;
            MM.for_each_vmobject([&](auto& vmobject) {
                if (vmobject.is_anonymous())
                    vmobjects.append(vmobject);
                return IterationDecision::Continue;
            });
        }
        for (auto& vmobject : vmobjects) {
            purged_page_count += vmobject.purge();
        }
    }
    if (mode & PURGE_ALL_CLEAN_INODE) {
        NonnullRefPtrVector<InodeVMObject> vmobjects;
        {
            InterruptDisabler disabler;
            MM.for_each_vmobject([&](auto& vmobject) {
                if (vmobject.is_inode())
                    vmobjects.append(static_cast<InodeVMObject&>(vmobject));
                return IterationDecision::Continue;
            });
        }
        for (auto& vmobject : vmobjects) {
            purged_page_count += vmobject.release_all_clean_pages();
        }
    }
    return purged_page_count;
}

}
