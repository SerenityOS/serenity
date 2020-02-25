/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/FixedArray.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/KResult.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class Process;

inline constexpr u32 encoded_device(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}

inline bool is_directory(mode_t mode) { return (mode & 0170000) == 0040000; }
inline bool is_character_device(mode_t mode) { return (mode & 0170000) == 0020000; }
inline bool is_block_device(mode_t mode) { return (mode & 0170000) == 0060000; }
inline bool is_regular_file(mode_t mode) { return (mode & 0170000) == 0100000; }
inline bool is_fifo(mode_t mode) { return (mode & 0170000) == 0010000; }
inline bool is_symlink(mode_t mode) { return (mode & 0170000) == 0120000; }
inline bool is_socket(mode_t mode) { return (mode & 0170000) == 0140000; }
inline bool is_sticky(mode_t mode) { return mode & 01000; }
inline bool is_setuid(mode_t mode) { return mode & 04000; }
inline bool is_setgid(mode_t mode) { return mode & 02000; }

struct InodeMetadata {
    bool is_valid() const { return inode.is_valid(); }

    bool may_read(const Process&) const;
    bool may_write(const Process&) const;
    bool may_execute(const Process&) const;

    bool may_read(uid_t u, gid_t g, const FixedArray<gid_t>& eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return mode & 0400;
        if (gid == g || eg.contains(gid))
            return mode & 0040;
        return mode & 0004;
    }

    bool may_write(uid_t u, gid_t g, const FixedArray<gid_t>& eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return mode & 0200;
        if (gid == g || eg.contains(gid))
            return mode & 0020;
        return mode & 0002;
    }

    bool may_execute(uid_t u, gid_t g, const FixedArray<gid_t>& eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return mode & 0100;
        if (gid == g || eg.contains(gid))
            return mode & 0010;
        return mode & 0001;
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

    KResult stat(stat& buffer) const
    {
        if (!is_valid())
            return KResult(-EIO);
        buffer.st_rdev = encoded_device(major_device, minor_device);
        buffer.st_ino = inode.index();
        buffer.st_mode = mode;
        buffer.st_nlink = link_count;
        buffer.st_uid = uid;
        buffer.st_gid = gid;
        buffer.st_dev = 0; // FIXME
        buffer.st_size = size;
        buffer.st_blksize = block_size;
        buffer.st_blocks = block_count;
        buffer.st_atime = atime;
        buffer.st_mtime = mtime;
        buffer.st_ctime = ctime;
        return KSuccess;
    }

    InodeIdentifier inode;
    off_t size { 0 };
    mode_t mode { 0 };
    uid_t uid { 0 };
    gid_t gid { 0 };
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
