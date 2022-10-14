/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Variables/BooleanVariable.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<void> SysFSSystemBoolean::try_generate(KBufferBuilder& builder)
{
    return builder.appendff("{}\n", static_cast<int>(value()));
}

ErrorOr<size_t> SysFSSystemBoolean::write_bytes(off_t, size_t count, UserOrKernelBuffer const& buffer, OpenFileDescription*)
{
    if (count != 1)
        return EINVAL;
    MutexLocker locker(m_refresh_lock);
    char value = 0;
    TRY(buffer.read(&value, 1));
    if (value == '0')
        set_value(false);
    else if (value == '1')
        set_value(true);
    else
        return EINVAL;
    return 1;
}

ErrorOr<void> SysFSSystemBoolean::truncate(u64 size)
{
    if (size != 0)
        return EPERM;
    return {};
}

}
