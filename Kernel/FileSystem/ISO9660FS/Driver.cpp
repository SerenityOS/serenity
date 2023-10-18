/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Driver.h"
#include "FileSystem.h"

namespace Kernel::FS {

FS_DRIVER(ISO9660FSDriver);
void ISO9660FSDriver::init()
{
    // Note the FS::Driver already registers us automatically
    (void)make_ref_counted<ISO9660FSDriver>();
}

ErrorOr<NonnullRefPtr<FileSystem>> ISO9660FSDriver::probe(OpenFileDescription& fd, ReadonlyBytes mount_specific_data)
{
    auto fs = TRY(ISO9660FS::try_create(fd, mount_specific_data));
    auto result = fs->initialize();
    if (result.is_error()) {
        dbgln("ISO9660FSDriver: Mounting fd as ISO9660FS failed: {}", result.error());
        return result.release_error();
    }
    return fs;
}

}
