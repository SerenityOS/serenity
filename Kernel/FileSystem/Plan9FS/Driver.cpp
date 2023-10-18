/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Driver.h"
#include "FileSystem.h"

namespace Kernel::FS {

FS_DRIVER(Plan9FSDriver);
void Plan9FSDriver::init()
{
    // Note the FS::Driver already registers us automatically
    (void)make_ref_counted<Plan9FSDriver>();
}

ErrorOr<NonnullRefPtr<FileSystem>> Plan9FSDriver::probe(OpenFileDescription& fd, ReadonlyBytes mount_specific_data)
{
    auto fs = TRY(Plan9FS::try_create(fd, mount_specific_data));
    auto result = fs->initialize();
    if (result.is_error()) {
        dbgln("Plan9FSDriver: Mounting fd as Plan9FS failed: {}", result.error());
        return result.release_error();
    }
    return fs;
}

}
