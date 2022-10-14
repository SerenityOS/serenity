/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileBackedFileSystem.h>

namespace Kernel {

FileBackedFileSystem::FileBackedFileSystem(OpenFileDescription& file_description)
    : m_file_description(file_description)
{
}

FileBackedFileSystem::~FileBackedFileSystem() = default;

ErrorOr<void> FileBackedFileSystem::initialize()
{
    MutexLocker locker(m_lock);
    if (is_initialized_while_locked())
        return {};
    return initialize_while_locked();
}

}
