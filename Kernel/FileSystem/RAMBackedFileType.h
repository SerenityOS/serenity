/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/API/POSIX/sys/stat.h>
#include <Kernel/FileSystem/FileSystem.h>

namespace Kernel {

enum class RAMBackedFileType : u8 {
    Directory,
    Character,
    Block,
    Regular,
    FIFO,
    Link,
    Socket,
    Unknown,
};

inline RAMBackedFileType ram_backed_file_type_from_mode(mode_t mode)
{
    switch (mode & S_IFMT) {
    case S_IFDIR:
        return RAMBackedFileType::Directory;
    case S_IFCHR:
        return RAMBackedFileType::Character;
    case S_IFBLK:
        return RAMBackedFileType::Block;
    case S_IFREG:
        return RAMBackedFileType::Regular;
    case S_IFIFO:
        return RAMBackedFileType::FIFO;
    case S_IFLNK:
        return RAMBackedFileType::Link;
    case S_IFSOCK:
        return RAMBackedFileType::Socket;
    default:
        return RAMBackedFileType::Unknown;
    }
}

inline u8 ram_backed_file_type_to_directory_entry_type(FileSystem::DirectoryEntryView const& entry)
{
    switch (static_cast<RAMBackedFileType>(entry.file_type)) {
    case RAMBackedFileType::Directory:
        return DT_DIR;
    case RAMBackedFileType::Character:
        return DT_CHR;
    case RAMBackedFileType::Block:
        return DT_BLK;
    case RAMBackedFileType::Regular:
        return DT_REG;
    case RAMBackedFileType::FIFO:
        return DT_FIFO;
    case RAMBackedFileType::Link:
        return DT_LNK;
    case RAMBackedFileType::Socket:
        return DT_SOCK;
    case RAMBackedFileType::Unknown:
    default:
        return DT_UNKNOWN;
    }
}

}
