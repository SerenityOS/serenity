/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/BooleanVariable.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>

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

    return Process::current().jail().with([&](auto& my_jail) -> ErrorOr<size_t> {
        // Note: If we are in a jail, don't let the current process to change the variable.
        if (my_jail)
            return Error::from_errno(EPERM);
        if (count != 1)
            return Error::from_errno(EINVAL);
        if (value == '0')
            set_value(false);
        else if (value == '1')
            set_value(true);
        else
            return Error::from_errno(EINVAL);
        return 1;
    });
}

ErrorOr<void> SysFSSystemBooleanVariable::truncate(u64 size)
{
    if (size != 0)
        return EPERM;
    return {};
}

}
