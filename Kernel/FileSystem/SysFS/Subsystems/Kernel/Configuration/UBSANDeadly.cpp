/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/UBSanitizer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/UBSANDeadly.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSUBSANDeadly::SysFSUBSANDeadly(SysFSDirectory const& parent_directory)
    : SysFSSystemBooleanVariable(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSUBSANDeadly> SysFSUBSANDeadly::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSUBSANDeadly(parent_directory)).release_nonnull();
}

bool SysFSUBSANDeadly::value() const
{
    return AK::UBSanitizer::g_ubsan_is_deadly;
}

ErrorOr<void> SysFSUBSANDeadly::set_value(bool new_value)
{
#if ENABLE_KERNEL_UNDEFINED_SANITIZER_ALWAYS_DEADLY
    return EPERM;
#endif

    AK::UBSanitizer::g_ubsan_is_deadly = new_value;
    return {};
}

}
