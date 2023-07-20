/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/FixedStringBufferVariable.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<void> SysFSSystemFixedStringBufferVariable::try_generate(KBufferBuilder& builder)
{
    auto string_value = TRY(value());
    return builder.appendff("{}\n", string_value->view());
}

ErrorOr<size_t> SysFSSystemFixedStringBufferVariable::write_bytes(off_t offset, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription*)
{
    if (offset != 0)
        return EINVAL;
    if (m_storage_during_write_buffer_size < count)
        return E2BIG;
    // NOTE: If we are in a jail, don't let the current process change the variable.
    if (Process::current().is_currently_in_jail())
        return Error::from_errno(EPERM);
    MutexLocker locker(m_refresh_lock);
    TRY(m_storage_during_write.with([&](auto& storage) -> ErrorOr<void> {
        storage.fill_with(0);
        TRY(buffer.read(storage.span().data(), count));
        VERIFY(storage.span().size() >= count);
        auto new_value_without_possible_newlines = StringView(storage.span().trim(count));
        TRY(set_value(new_value_without_possible_newlines));
        return {};
    }));
    return count;
}

ErrorOr<void> SysFSSystemFixedStringBufferVariable::truncate(u64 size)
{
    if (size != 0)
        return EPERM;
    return {};
}

}
