/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/StringVariable.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<void> SysFSSystemStringVariable::try_generate(KBufferBuilder& builder)
{
    auto string_value = TRY(value());
    return builder.appendff("{}\n", string_value->view());
}

ErrorOr<size_t> SysFSSystemStringVariable::write_bytes(off_t, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription*)
{
    MutexLocker locker(m_refresh_lock);
    // Note: We do all of this code before taking the spinlock because then we disable
    // interrupts so page faults will not work.
    char* value = nullptr;
    auto new_value = TRY(KString::try_create_uninitialized(count, value));
    TRY(buffer.read(value, count));
    auto new_value_without_possible_newlines = TRY(KString::try_create(new_value->view().trim("\n"sv)));
    TRY(Process::current().jail().with([&](auto& my_jail) -> ErrorOr<void> {
        // Note: If we are in a jail, don't let the current process to change the variable.
        if (my_jail)
            return Error::from_errno(EPERM);
        return {};
    }));
    set_value(move(new_value_without_possible_newlines));
    return count;
}

ErrorOr<void> SysFSSystemStringVariable::truncate(u64 size)
{
    if (size != 0)
        return EPERM;
    return {};
}

}
