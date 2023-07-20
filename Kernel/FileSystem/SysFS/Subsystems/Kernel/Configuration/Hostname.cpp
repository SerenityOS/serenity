/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/Hostname.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSHostnameString::SysFSHostnameString(SysFSDirectory const& parent_directory, FixedArray<u8>&& write_storage)
    : SysFSSystemFixedStringBufferVariable(parent_directory, move(write_storage))
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSHostnameString> SysFSHostnameString::must_create(SysFSDirectory const& parent_directory)
{
    auto write_storage = MUST(FixedArray<u8>::create(UTSNAME_ENTRY_LEN - 1));
    return adopt_ref_if_nonnull(new (nothrow) SysFSHostnameString(parent_directory, move(write_storage))).release_nonnull();
}

ErrorOr<NonnullOwnPtr<KString>> SysFSHostnameString::value() const
{
    return hostname().with_shared([&](auto const& name) -> ErrorOr<NonnullOwnPtr<KString>> {
        return KString::formatted("{}\0", name.representable_view());
    });
}

ErrorOr<void> SysFSHostnameString::set_value(StringView new_value)
{
    if (!new_value.is_alphanumeric())
        return Error::from_errno(EINVAL);
    hostname().with_exclusive([&](auto& name) {
        name.store_characters(new_value);
    });
    return {};
}

mode_t SysFSHostnameString::permissions() const
{
    // NOTE: Let's not allow users to randomly change the hostname string, but only
    // allow this for the root user.
    return S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH;
}

}
