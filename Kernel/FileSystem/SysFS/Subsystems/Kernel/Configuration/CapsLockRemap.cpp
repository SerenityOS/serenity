/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Input/Management.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/CapsLockRemap.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSCapsLockRemap::SysFSCapsLockRemap(SysFSDirectory const& parent_directory)
    : SysFSSystemBooleanVariable(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSCapsLockRemap> SysFSCapsLockRemap::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSCapsLockRemap(parent_directory)).release_nonnull();
}

bool SysFSCapsLockRemap::value() const
{
    SpinlockLocker locker(m_lock);
    return g_caps_lock_remapped_to_ctrl.load();
}
void SysFSCapsLockRemap::set_value(bool new_value)
{
    SpinlockLocker locker(m_lock);
    g_caps_lock_remapped_to_ctrl.exchange(new_value);
}

}
