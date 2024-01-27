/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/BooleanVariable.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<void> SysFSSystemBooleanVariable::try_generate(KBufferBuilder& builder)
{
    return builder.appendff("{}\n", static_cast<int>(value()));
}

ErrorOr<size_t> SysFSSystemBooleanVariable::write_bytes(off_t, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription*)
{
    MutexLocker locker(m_refresh_lock);
    // Note: We do all of this code before taking the spinlock because then we disable
    // interrupts so page faults will not work.
    char value = 0;
    TRY(buffer.read(&value, 1));

    // NOTE: If we are in a jail, don't let the current process to change the variable.
    if (Process::current().is_jailed())
        return Error::from_errno(EPERM);

    if (count != 1)
        return Error::from_errno(EINVAL);
    if (value == '0') {
        set_value(false);
        return 1;
    } else if (value == '1') {
        set_value(true);
        return 1;
    }
    return Error::from_errno(EINVAL);
}

ErrorOr<void> SysFSSystemBooleanVariable::truncate(u64 size)
{
    if (size != 0)
        return EPERM;
    return {};
}

}
