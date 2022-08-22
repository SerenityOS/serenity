/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Thread.h>

namespace Kernel::Memory {

struct CR3Map {
    SpinlockProtected<IntrusiveRedBlackTree<&PageDirectory::m_tree_node>> map { LockRank::None };
};

static Singleton<CR3Map> s_cr3_map;

void PageDirectory::register_page_directory(PageDirectory* directory)
{
    s_cr3_map->map.with([&](auto& map) {
        map.insert(directory->cr3(), *directory);
    });
}

void PageDirectory::deregister_page_directory(PageDirectory* directory)
{
    s_cr3_map->map.with([&](auto& map) {
        map.remove(directory->cr3());
    });
}

LockRefPtr<PageDirectory> PageDirectory::find_current()
{
    return s_cr3_map->map.with([&](auto& map) {
        return map.find(read_cr3());
    });
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
