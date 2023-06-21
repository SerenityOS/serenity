/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/ISO9660FS/Definitions.h>
#include <Kernel/FileSystem/ISO9660FS/DirectoryEntry.h>
#include <Kernel/FileSystem/ISO9660FS/FileSystem.h>
#include <Kernel/Library/KBuffer.h>

namespace Kernel {

class ISO9660DirectoryIterator {
public:
    ISO9660DirectoryIterator(ISO9660FS& fs, ISO::DirectoryRecordHeader const& header);

    ISO::DirectoryRecordHeader const* operator*() { return m_current_header; }

    // Recurses into subdirectories. May fail.
    ErrorOr<bool> next();

    // Skips to the directory in the list, returns whether there was a next one.
    // No allocation here, cannot fail.
    bool skip();

    bool go_up();
    bool done() const;

private:
    ErrorOr<void> read_directory_contents();

    void get_header();

    ISO9660FS& m_fs;

    ISO9660FSDirectoryState m_current_directory;
    ISO::DirectoryRecordHeader const* m_current_header { nullptr };

    Vector<ISO9660FSDirectoryState> m_directory_stack;
};

}
