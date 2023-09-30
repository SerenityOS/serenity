/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Span.h>
#include <AK/Time.h>
#include <Kernel/API/DeviceFileTypes.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/Forward.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class Process;

constexpr u64 encoded_device(MajorNumber major, MinorNumber minor)
{
    return (minor.value() & 0xff) | (major.value() << 8) | ((minor.value() & ~0xff) << 12);
}
static inline MajorNumber major_from_encoded_device(dev_t dev) { return (dev & 0xfff00u) >> 8u; }
static inline MinorNumber minor_from_encoded_device(dev_t dev) { return (dev & 0xffu) | ((dev >> 12u) & 0xfff00u); }

inline bool is_directory(mode_t mode) { return (mode & S_IFMT) == S_IFDIR; }
inline bool is_character_device(mode_t mode) { return (mode & S_IFMT) == S_IFCHR; }
inline bool is_block_device(mode_t mode) { return (mode & S_IFMT) == S_IFBLK; }
inline bool is_regular_file(mode_t mode) { return (mode & S_IFMT) == S_IFREG; }
inline bool is_fifo(mode_t mode) { return (mode & S_IFMT) == S_IFIFO; }
inline bool is_symlink(mode_t mode) { return (mode & S_IFMT) == S_IFLNK; }
inline bool is_socket(mode_t mode) { return (mode & S_IFMT) == S_IFSOCK; }
inline bool is_sticky(mode_t mode) { return (mode & S_ISVTX) == S_ISVTX; }
inline bool is_setuid(mode_t mode) { return (mode & S_ISUID) == S_ISUID; }
inline bool is_setgid(mode_t mode) { return (mode & S_ISGID) == S_ISGID; }

enum class UseEffectiveIDs {
    Yes,
    No
};

struct InodeMetadata {
    bool is_valid() const { return inode.is_valid(); }

    bool may_read(Credentials const&, UseEffectiveIDs = UseEffectiveIDs::Yes) const;
    bool may_write(Credentials const&, UseEffectiveIDs = UseEffectiveIDs::Yes) const;
    bool may_execute(Credentials const&, UseEffectiveIDs = UseEffectiveIDs::Yes) const;

    bool may_read(UserID u, GroupID g, ReadonlySpan<GroupID> eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return (mode & S_IRUSR) == S_IRUSR;
        if (gid == g || eg.contains_slow(gid))
            return (mode & S_IRGRP) == S_IRGRP;
        return (mode & S_IROTH) == S_IROTH;
    }

    bool may_write(UserID u, GroupID g, ReadonlySpan<GroupID> eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return (mode & S_IWUSR) == S_IWUSR;
        if (gid == g || eg.contains_slow(gid))
            return (mode & S_IWGRP) == S_IWGRP;
        return (mode & S_IWOTH) == S_IWOTH;
    }

    bool may_execute(UserID u, GroupID g, ReadonlySpan<GroupID> eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return (mode & S_IXUSR) == S_IXUSR;
        if (gid == g || eg.contains_slow(gid))
            return (mode & S_IXGRP) == S_IXGRP;
        return (mode & S_IXOTH) == S_IXOTH;
    }

    bool is_directory() const { return Kernel::is_directory(mode); }
    bool is_character_device() const { return Kernel::is_character_device(mode); }
    bool is_block_device() const { return Kernel::is_block_device(mode); }
    bool is_device() const { return is_character_device() || is_block_device(); }
    bool is_regular_file() const { return Kernel::is_regular_file(mode); }
    bool is_fifo() const { return Kernel::is_fifo(mode); }
    bool is_symlink() const { return Kernel::is_symlink(mode); }
    bool is_socket() const { return Kernel::is_socket(mode); }
    bool is_sticky() const { return Kernel::is_sticky(mode); }
    bool is_setuid() const { return Kernel::is_setuid(mode); }
    bool is_setgid() const { return Kernel::is_setgid(mode); }

    ErrorOr<struct stat> stat() const
    {
        if (!is_valid())
            return EIO;
        struct stat buffer = {};
        buffer.st_rdev = encoded_device(major_device, minor_device);
        buffer.st_ino = inode.index().value();
        buffer.st_mode = mode;
        buffer.st_nlink = link_count;
        buffer.st_uid = uid.value();
        buffer.st_gid = gid.value();
        buffer.st_dev = inode.fsid().value();
        buffer.st_size = size;
        buffer.st_blksize = block_size;
        buffer.st_blocks = block_count;
        buffer.st_atim = atime.to_timespec();
        buffer.st_mtim = mtime.to_timespec();
        buffer.st_ctim = ctime.to_timespec();
        return buffer;
    }

    InodeIdentifier inode;
    off_t size { 0 };
    mode_t mode { 0 };
    UserID uid { 0 };
    GroupID gid { 0 };
    nlink_t link_count { 0 };
    UnixDateTime atime {};
    UnixDateTime ctime {};
    UnixDateTime mtime {};
    UnixDateTime dtime {};
    blkcnt_t block_count { 0 };
    blksize_t block_size { 0 };
    MajorNumber major_device { 0 };
    MinorNumber minor_device { 0 };
};

}
