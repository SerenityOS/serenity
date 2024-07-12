/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>

namespace Kernel {

class FileBackedFileSystem : public FileSystem {

public:
    virtual ~FileBackedFileSystem() override;

    File& file() { return m_file_description->file(); }
    OpenFileDescription& file_description() { return *m_file_description; }
    File const& file() const { return m_file_description->file(); }
    OpenFileDescription& file_description() const { return *m_file_description; }

protected:
    explicit FileBackedFileSystem(OpenFileDescription&);

    // Note: We require all FileBackedFileSystem to implement something that actually
    // takes into account the fact that we will clean the last mount of the filesystem,
    // therefore, removing the file system with it from the Kernel memory.
    virtual ErrorOr<void> prepare_to_clear_last_mount(Inode& mount_guest_inode) override = 0;

    virtual ErrorOr<void> initialize_while_locked() = 0;
    virtual bool is_initialized_while_locked() = 0;

private:
    virtual ErrorOr<void> initialize() override final;
    virtual bool is_file_backed() const override { return true; }

    IntrusiveListNode<FileBackedFileSystem> m_file_backed_file_system_node;
    NonnullRefPtr<OpenFileDescription> const m_file_description;

public:
    using List = IntrusiveList<&FileBackedFileSystem::m_file_backed_file_system_node>;

    // NOTE: This method is implemented in Kernel/FileSystem/VirtualFileSystem.cpp
    static ErrorOr<NonnullRefPtr<FileBackedFileSystem>> create_and_append_filesystems_list_from_mount_file_and_description(MountFile& mount_file, OpenFileDescription& source_description);
};
}
