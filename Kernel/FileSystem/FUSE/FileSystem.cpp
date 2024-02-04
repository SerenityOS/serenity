/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntegralMath.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/FUSE/FileSystem.h>
#include <Kernel/FileSystem/FUSE/Inode.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

struct [[gnu::packed]] FUSESpecificFlagsBytes {
    u64 pid;
    u64 device_inode;
    u64 rootmode;
    u64 gid;
    u64 uid;
};

ErrorOr<NonnullRefPtr<FileSystem>> FUSE::try_create(ReadonlyBytes mount_flags)
{
    auto* fuse_mount_flags = reinterpret_cast<FUSESpecificFlagsBytes const*>(mount_flags.data());

    auto description = TRY(Process::current().open_file_description(fuse_mount_flags->device_inode));
    auto connection = TRY(FUSEConnection::try_create(description));

    u64 rootmode = AK::reinterpret_as_octal(fuse_mount_flags->rootmode);
    u64 gid = fuse_mount_flags->gid;
    u64 uid = fuse_mount_flags->uid;
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) FUSE(connection, rootmode, uid, gid)));
}

ErrorOr<void> FUSE::handle_mount_unsigned_integer_flag(Bytes mount_file_specific_flags_buffer, StringView key, u64 value)
{
    auto* fuse_mount_flags = reinterpret_cast<FUSESpecificFlagsBytes*>(mount_file_specific_flags_buffer.data());
    if (key == "fd") {
        fuse_mount_flags->device_inode = value;
        return {};
    }
    if (key == "rootmode") {
        fuse_mount_flags->rootmode = value;
        return {};
    }
    if (key == "gid") {
        fuse_mount_flags->gid = value;
        return {};
    }
    if (key == "uid") {
        fuse_mount_flags->uid = value;
        return {};
    }
    return EINVAL;
}

FUSE::FUSE(NonnullRefPtr<FUSEConnection> connection, u64 rootmode, u64 gid, u64 uid)
    : m_connection(connection)
    , m_rootmode(rootmode)
    , m_gid(gid)
    , m_uid(uid)
{
}

FUSE::~FUSE() = default;

ErrorOr<void> FUSE::initialize()
{
    m_root_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) FUSEInode(*this)));
    m_root_inode->m_metadata.mode = m_rootmode;
    m_root_inode->m_metadata.uid = m_gid;
    m_root_inode->m_metadata.gid = m_uid;
    m_root_inode->m_metadata.size = 0;
    m_root_inode->m_metadata.mtime = TimeManagement::boot_time();
    return {};
}

Inode& FUSE::root_inode()
{
    return *m_root_inode;
}

u8 FUSE::internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const
{
    return ram_backed_file_type_to_directory_entry_type(entry);
}

}
