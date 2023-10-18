/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Driver.h"
#include "FileSystem.h"

namespace Kernel::FS {

FS_DRIVER(Ext2FSDriver);
void Ext2FSDriver::init()
{
    // Note the FS::Driver already registers us automatically
    (void)make_ref_counted<Ext2FSDriver>();
}

ErrorOr<NonnullRefPtr<FileSystem>> Ext2FSDriver::probe(OpenFileDescription& fd, ReadonlyBytes mount_specific_data)
{
    auto fs = TRY(Ext2FS::try_create(fd, mount_specific_data));
    auto result = fs->initialize();
    if (result.is_error()) {
        dbgln("Ext2FSDriver: Mounting fd as Ext2FS failed: {}", result.error());
        return result.release_error();
    }
    return fs;
}

}
