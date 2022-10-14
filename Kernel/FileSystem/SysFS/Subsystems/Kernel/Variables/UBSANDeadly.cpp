/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/UBSanitizer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/UBSANDeadly.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSUBSANDeadly::SysFSUBSANDeadly(SysFSDirectory const& parent_directory)
    : SysFSSystemBoolean(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<SysFSUBSANDeadly> SysFSUBSANDeadly::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_lock_ref_if_nonnull(new (nothrow) SysFSUBSANDeadly(parent_directory)).release_nonnull();
}

bool SysFSUBSANDeadly::value() const
{
    return AK::UBSanitizer::g_ubsan_is_deadly;
}
void SysFSUBSANDeadly::set_value(bool new_value)
{
    AK::UBSanitizer::g_ubsan_is_deadly = new_value;
}

}
