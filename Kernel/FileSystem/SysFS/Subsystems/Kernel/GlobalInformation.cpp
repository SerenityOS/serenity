/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/GlobalInformation.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<size_t> SysFSGlobalInformation::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    dbgln_if(SYSFS_DEBUG, "SysFSGlobalInformation @ {}: read_bytes offset: {} count: {}", name(), offset, count);

    VERIFY(offset >= 0);
    VERIFY(buffer.user_or_kernel_ptr());

    if (!description)
        return Error::from_errno(EIO);

    MutexLocker locker(m_refresh_lock);

    if (!description->data()) {
        dbgln("SysFSGlobalInformation: Do not have cached data!");
        return Error::from_errno(EIO);
    }

    auto& typed_cached_data = static_cast<SysFSInodeData&>(*description->data());
    auto& data_buffer = typed_cached_data.buffer;

    if (!data_buffer || (size_t)offset >= data_buffer->size())
        return 0;

    ssize_t nread = min(static_cast<off_t>(data_buffer->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(data_buffer->data() + offset, nread));
    return nread;
}

SysFSGlobalInformation::SysFSGlobalInformation(SysFSDirectory const& parent_directory)
    : SysFSComponent(parent_directory)
{
}

ErrorOr<void> SysFSGlobalInformation::refresh_data(OpenFileDescription& description) const
{
    MutexLocker lock(m_refresh_lock);
    auto& cached_data = description.data();
    if (!cached_data) {
        cached_data = adopt_own_if_nonnull(new (nothrow) SysFSInodeData);
        if (!cached_data)
            return ENOMEM;
    }
    auto builder = TRY(KBufferBuilder::try_create());
    if (Process::current().is_jailed() && !is_readable_by_jailed_processes())
        return Error::from_errno(EPERM);
    TRY(const_cast<SysFSGlobalInformation&>(*this).try_generate(builder));
    auto& typed_cached_data = static_cast<SysFSInodeData&>(*cached_data);
    typed_cached_data.buffer = builder.build();
    if (!typed_cached_data.buffer)
        return ENOMEM;
    return {};
}

}
