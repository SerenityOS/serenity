/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Driver.h"
#include "FileSystem.h"

namespace Kernel::FS {

FS_DRIVER(FATFSDriver);
void FATFSDriver::init()
{
    // Note the FS::Driver already registers us automatically
    (void)make_ref_counted<FATFSDriver>();
}

ErrorOr<NonnullRefPtr<FileSystem>> FATFSDriver::probe(OpenFileDescription& fd, ReadonlyBytes mount_specific_data)
{
    auto fs = TRY(FATFS::try_create(fd, mount_specific_data));
    auto result = fs->initialize();
    if (result.is_error()) {
        dbgln("FATFSDriver: Mounting fd as FATFS failed: {}", result.error());
        return result.release_error();
    }
    return fs;
}

}
