/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/StringVariable.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

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
    // NOTE: If we are in a jail, don't let the current process to change the variable.
    if (Process::current().is_jailed())
        return Error::from_errno(EPERM);
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
