/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>

#include <Kernel/Arch/InterruptDisabler.h>
#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Thread.h>

namespace Kernel::Memory {

// FIXME: This needs real locking:
static Singleton<IntrusiveRedBlackTree<&PageDirectory::m_tree_node>> s_cr3_map;

static IntrusiveRedBlackTree<&PageDirectory::m_tree_node>& cr3_map()
{
    VERIFY_INTERRUPTS_DISABLED();
    return *s_cr3_map;
}

void PageDirectory::register_page_directory(PageDirectory* directory)
{
    InterruptDisabler disabler;
    cr3_map().insert(directory->cr3(), *directory);
}

void PageDirectory::deregister_page_directory(PageDirectory* directory)
{
    InterruptDisabler disabler;
    cr3_map().remove(directory->cr3());
}

LockRefPtr<PageDirectory> PageDirectory::find_current()
{
    SpinlockLocker lock(s_mm_lock);
    return cr3_map().find(read_cr3());
}

void activate_kernel_page_directory(PageDirectory const& pgd)
{
    write_cr3(pgd.cr3());
}

void activate_page_directory(PageDirectory const& pgd, Thread* current_thread)
{
    current_thread->regs().cr3 = pgd.cr3();
    write_cr3(pgd.cr3());
}

}
