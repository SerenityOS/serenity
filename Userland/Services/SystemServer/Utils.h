/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/API/DeviceFileTypes.h>
#include <LibCore/System.h>
#include <fcntl.h>
#include <sys/sysmacros.h>
#include <sys/types.h>

namespace SystemServer {

inline ErrorOr<void> create_devtmpfs_block_device(StringView name, mode_t mode, MajorNumber major, MinorNumber minor)
{
    return Core::System::mknod(name, mode | S_IFBLK, makedev(major.value(), minor.value()));
}

inline ErrorOr<void> create_devtmpfs_char_device(StringView name, mode_t mode, MajorNumber major, MinorNumber minor)
{
    return Core::System::mknod(name, mode | S_IFCHR, makedev(major.value(), minor.value()));
}

}
