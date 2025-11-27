/*
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Configuration/CoredumpDirectory.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Coredump.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSCoredumpDirectory::SysFSCoredumpDirectory(SysFSDirectory const& parent_directory)
    : SysFSSystemStringVariable(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSCoredumpDirectory> SysFSCoredumpDirectory::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSCoredumpDirectory(parent_directory)).release_nonnull();
}

ErrorOr<NonnullOwnPtr<KString>> SysFSCoredumpDirectory::value() const
{
    return Coredump::directory_path().with([&](auto& coredump_directory_path) -> ErrorOr<NonnullOwnPtr<KString>> {
        if (coredump_directory_path)
            return KString::try_create(coredump_directory_path->view());
        return KString::try_create(""sv);
    });
}

ErrorOr<void> SysFSCoredumpDirectory::set_value(NonnullOwnPtr<KString> new_value)
{
    if (new_value->length() > 0 && new_value->bytes()[0] != '/')
        return Error::from_errno(EINVAL);

    Coredump::directory_path().with([&](auto& coredump_directory_path) {
        coredump_directory_path = move(new_value);
    });
    return {};
}

mode_t SysFSCoredumpDirectory::permissions() const
{
    // NOTE: Let's not allow users to randomly change the coredump path, but only
    // allow this for the root user.
    return S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH;
}

}
