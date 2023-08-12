/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/DumpKmallocStack.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSDumpKmallocStacks::SysFSDumpKmallocStacks(SysFSDirectory const& parent_directory)
    : SysFSSystemBooleanVariable(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSDumpKmallocStacks> SysFSDumpKmallocStacks::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSDumpKmallocStacks(parent_directory)).release_nonnull();
}

bool SysFSDumpKmallocStacks::value() const
{
    SpinlockLocker locker(m_lock);
    return g_dump_kmalloc_stacks;
}

void SysFSDumpKmallocStacks::set_value(bool new_value)
{
    SpinlockLocker locker(m_lock);
    g_dump_kmalloc_stacks = new_value;
}

}
