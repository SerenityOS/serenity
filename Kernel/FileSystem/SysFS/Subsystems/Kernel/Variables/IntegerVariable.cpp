/*
 * Copyright (c) 2023, Mark Douglas <dmarkd@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/IntegerVariable.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<void> SysFSSystemIntegerVariable::try_generate(KBufferBuilder& builder)
{
    return builder.appendff("{}\n", value());
}

ErrorOr<size_t> SysFSSystemIntegerVariable::write_bytes(off_t, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription*)
{
    MutexLocker locker(m_refresh_lock);
    // Note: We do all of this code before taking the spinlock because then we disable
    // interrupts so page faults will not work.

    char* value = nullptr;
    i32 result = 0;
    i32 sign = 1;
    auto new_value = TRY(KString::try_create_uninitialized(count, value));
    TRY(buffer.read(value, count));

    if (*value == '-') {
        sign = -1;
        value++;
    }

    while (*value != '\0') {
        if (*value >= '0' && *value <= '9') {
            result = result * 10 + (*value - '0');
        } else {
            break;
        }
        value++;
    }

    result *= sign;

    // NOTE: If we are in a jail, don't let the current process to change the variable.
    if (Process::current().is_currently_in_jail())
        return Error::from_errno(EPERM);
    dbgln("Setting value: {}", result);
    set_value(result);
    return count;
}

ErrorOr<void> SysFSSystemIntegerVariable::truncate(u64 size)
{
    if (size != 0)
        return EPERM;
    return {};
}

}
