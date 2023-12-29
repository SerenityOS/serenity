/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/KASANDeadly.h>
#include <Kernel/Security/AddressSanitizer.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSKASANDeadly::SysFSKASANDeadly(SysFSDirectory const& parent_directory)
    : SysFSSystemBooleanVariable(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSKASANDeadly> SysFSKASANDeadly::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSKASANDeadly(parent_directory)).release_nonnull();
}

bool SysFSKASANDeadly::value() const
{
    return AddressSanitizer::g_kasan_is_deadly;
}
void SysFSKASANDeadly::set_value(bool new_value)
{
    AddressSanitizer::g_kasan_is_deadly = new_value;
}

}
