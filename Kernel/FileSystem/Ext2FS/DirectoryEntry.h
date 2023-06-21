/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Library/KString.h>

namespace Kernel {

struct Ext2FSDirectoryEntry {
    NonnullOwnPtr<KString> name;
    InodeIndex inode_index { 0 };
    u8 file_type { 0 };
    u16 record_length { 0 };
};

}
