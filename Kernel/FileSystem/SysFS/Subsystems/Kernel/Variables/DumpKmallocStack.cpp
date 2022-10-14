/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/DumpKmallocStack.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSDumpKmallocStacks::SysFSDumpKmallocStacks(SysFSDirectory const& parent_directory)
    : SysFSSystemBoolean(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSDumpKmallocStacks> SysFSDumpKmallocStacks::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSDumpKmallocStacks(parent_directory)).release_nonnull();
}

bool SysFSDumpKmallocStacks::value() const
{
    MutexLocker locker(m_lock);
    return g_dump_kmalloc_stacks;
}

void SysFSDumpKmallocStacks::set_value(bool new_value)
{
    MutexLocker locker(m_lock);
    g_dump_kmalloc_stacks = new_value;
}

}
