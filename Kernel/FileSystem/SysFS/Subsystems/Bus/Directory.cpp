/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/RootDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/Directory.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<SysFSBusDirectory> SysFSBusDirectory::must_create(SysFSRootDirectory const& parent_directory)
{
    auto directory = adopt_ref(*new (nothrow) SysFSBusDirectory(parent_directory));
    return directory;
}

UNMAP_AFTER_INIT SysFSBusDirectory::SysFSBusDirectory(SysFSRootDirectory const& parent_directory)
    : SysFSDirectory(parent_directory)
{
}

}
