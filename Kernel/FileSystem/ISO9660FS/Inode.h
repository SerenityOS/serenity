/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/ISO9660FS/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class ISO9660Inode final : public Inode {
    friend ISO9660FS;

public:
    virtual ~ISO9660Inode() override;

    ISO9660FS& fs() { return static_cast<ISO9660FS&>(Inode::fs()); }
    ISO9660FS const& fs() const { return static_cast<ISO9660FS const&>(Inode::fs()); }

    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate_locked(u64) override;
    virtual ErrorOr<void> update_timestamps(Optional<UnixDateTime> atime, Optional<UnixDateTime> ctime, Optional<UnixDateTime> mtime) override;

private:
    // HACK: The base ISO 9660 standard says the maximum filename length is 37
    // bytes large; however, we can read filenames longer than that right now
    // without any problems, so let's allow it anyway.
    static constexpr size_t max_file_identifier_length = 256 - sizeof(ISO::DirectoryRecordHeader);

    // ^Inode
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override;

    ISO9660Inode(ISO9660FS&, ISO::DirectoryRecordHeader const& record, StringView name);
    static ErrorOr<NonnullRefPtr<ISO9660Inode>> try_create_from_directory_record(ISO9660FS&, ISO::DirectoryRecordHeader const& record, StringView name);

    static InodeIndex get_inode_index(ISO::DirectoryRecordHeader const& record, StringView name);
    static StringView get_normalized_filename(ISO::DirectoryRecordHeader const& record, Bytes buffer);

    void create_metadata();
    UnixDateTime parse_numerical_date_time(ISO::NumericalDateAndTime const&);

    InodeMetadata m_metadata;
    ISO::DirectoryRecordHeader m_record;
};

}

using Kernel::ISO::has_any_flag;
using Kernel::ISO::has_flag;
