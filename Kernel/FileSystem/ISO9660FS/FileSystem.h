/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Error.h>
#include <AK/HashMap.h>
#include <AK/RecursionDecision.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/FileSystem/FileSystemSpecificOption.h>
#include <Kernel/FileSystem/ISO9660FS/Definitions.h>
#include <Kernel/FileSystem/ISO9660FS/DirectoryEntry.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Library/NonnullLockRefPtr.h>

namespace Kernel {

class ISO9660Inode;
class ISO9660DirectoryIterator;

class ISO9660FS final : public BlockBasedFileSystem {
    friend ISO9660Inode;
    friend ISO9660DirectoryIterator;

public:
    static ErrorOr<NonnullRefPtr<FileSystem>> try_create(OpenFileDescription&, FileSystemSpecificOptions const&);

    virtual ~ISO9660FS() override;
    virtual StringView class_name() const override { return "ISO9660FS"sv; }
    virtual Inode& root_inode() override;

    virtual ErrorOr<void> rename(Inode& old_parent_inode, StringView old_basename, Inode& new_parent_inode, StringView new_basename) override;

    virtual unsigned total_block_count() const override;
    virtual unsigned total_inode_count() const override;

    virtual u8 internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const override;

    ErrorOr<NonnullLockRefPtr<ISO9660FSDirectoryEntry>> directory_entry_for_record(Badge<ISO9660DirectoryIterator>, ISO::DirectoryRecordHeader const* record);

private:
    ISO9660FS(OpenFileDescription&);

    virtual ErrorOr<void> prepare_to_clear_last_mount(Inode&) override;

    virtual bool is_initialized_while_locked() override;
    virtual ErrorOr<void> initialize_while_locked() override;

    ErrorOr<void> parse_volume_set();
    ErrorOr<void> create_root_inode();
    ErrorOr<void> calculate_inode_count() const;

    u32 calculate_directory_entry_cache_key(ISO::DirectoryRecordHeader const&);

    ErrorOr<void> visit_directory_record(ISO::DirectoryRecordHeader const& record, Function<ErrorOr<RecursionDecision>(ISO::DirectoryRecordHeader const*)> const& visitor) const;

    OwnPtr<ISO::PrimaryVolumeDescriptor> m_primary_volume;
    RefPtr<ISO9660Inode> m_root_inode;

    mutable u32 m_cached_inode_count { 0 };
    HashMap<u32, NonnullLockRefPtr<ISO9660FSDirectoryEntry>> m_directory_entry_cache;
};

}
