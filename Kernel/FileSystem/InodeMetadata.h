/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Span.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/Forward.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class Process;

constexpr u32 encoded_device(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}
static inline unsigned int major_from_encoded_device(dev_t dev) { return (dev & 0xfff00u) >> 8u; }
static inline unsigned int minor_from_encoded_device(dev_t dev) { return (dev & 0xffu) | ((dev >> 12u) & 0xfff00u); }

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

struct InodeMetadata {
    bool is_valid() const { return inode.is_valid(); }

    bool may_read(const Process&) const;
    bool may_write(const Process&) const;
    bool may_execute(const Process&) const;

    bool may_read(UserID u, GroupID g, Span<GroupID const> eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return (mode & S_IRUSR) == S_IRUSR;
        if (gid == g || eg.contains_slow(gid))
            return (mode & S_IRGRP) == S_IRGRP;
        return (mode & S_IROTH) == S_IROTH;
    }

    bool may_write(UserID u, GroupID g, Span<GroupID const> eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return (mode & S_IWUSR) == S_IWUSR;
        if (gid == g || eg.contains_slow(gid))
            return (mode & S_IWGRP) == S_IWGRP;
        return (mode & S_IWOTH) == S_IWOTH;
    }

    bool may_execute(UserID u, GroupID g, Span<GroupID const> eg) const
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

    ErrorOr<void> stat(stat& buffer) const
    {
        if (!is_valid())
            return EIO;
        buffer.st_rdev = encoded_device(major_device, minor_device);
        buffer.st_ino = inode.index().value();
        buffer.st_mode = mode;
        buffer.st_nlink = link_count;
        buffer.st_uid = uid.value();
        buffer.st_gid = gid.value();
        buffer.st_dev = 0; // FIXME
        buffer.st_size = size;
        buffer.st_blksize = block_size;
        buffer.st_blocks = block_count;
        buffer.st_atim.tv_sec = atime;
        buffer.st_atim.tv_nsec = 0;
        buffer.st_mtim.tv_sec = mtime;
        buffer.st_mtim.tv_nsec = 0;
        buffer.st_ctim.tv_sec = ctime;
        buffer.st_ctim.tv_nsec = 0;
        return {};
    }

    InodeIdentifier inode;
    off_t size { 0 };
    mode_t mode { 0 };
    UserID uid { 0 };
    GroupID gid { 0 };
    nlink_t link_count { 0 };
    time_t atime { 0 };
    time_t ctime { 0 };
    time_t mtime { 0 };
    time_t dtime { 0 };
    blkcnt_t block_count { 0 };
    blksize_t block_size { 0 };
    unsigned major_device { 0 };
    unsigned minor_device { 0 };
};

}
