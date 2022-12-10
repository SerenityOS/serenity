/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Keegan Saunders <keegan@undefinedbehaviour.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/AddressSanitizer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/KASANDeadly.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSKASANDeadly::SysFSKASANDeadly(SysFSDirectory const& parent_directory)
    : SysFSSystemBooleanVariable(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSKASANDeadly> SysFSKASANDeadly::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSKASANDeadly(parent_directory)).release_nonnull();
}

bool SysFSKASANDeadly::value() const
{
    return Kernel::AddressSanitizer::g_kasan_is_deadly;
}
void SysFSKASANDeadly::set_value(bool new_value)
{
    Kernel::AddressSanitizer::g_kasan_is_deadly = new_value;
}

}
