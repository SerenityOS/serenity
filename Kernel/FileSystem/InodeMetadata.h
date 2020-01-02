#pragma once

#include <AK/HashTable.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/KResult.h>
#include <Kernel/UnixTypes.h>

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

    bool may_read(Process&) const;
    bool may_write(Process&) const;
    bool may_execute(Process&) const;

    bool may_read(uid_t u, gid_t g, const HashTable<gid_t>& eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return mode & 0400;
        if (gid == g || eg.contains(gid))
            return mode & 0040;
        return mode & 0004;
    }

    bool may_write(uid_t u, gid_t g, const HashTable<gid_t>& eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return mode & 0200;
        if (gid == g || eg.contains(gid))
            return mode & 0020;
        return mode & 0002;
    }

    bool may_execute(uid_t u, gid_t g, const HashTable<gid_t>& eg) const
    {
        if (u == 0)
            return true;
        if (uid == u)
            return mode & 0100;
        if (gid == g || eg.contains(gid))
            return mode & 0010;
        return mode & 0001;
    }

    bool is_directory() const { return ::is_directory(mode); }
    bool is_character_device() const { return ::is_character_device(mode); }
    bool is_block_device() const { return ::is_block_device(mode); }
    bool is_device() const { return is_character_device() || is_block_device(); }
    bool is_regular_file() const { return ::is_regular_file(mode); }
    bool is_fifo() const { return ::is_fifo(mode); }
    bool is_symlink() const { return ::is_symlink(mode); }
    bool is_socket() const { return ::is_socket(mode); }
    bool is_sticky() const { return ::is_sticky(mode); }
    bool is_setuid() const { return ::is_setuid(mode); }
    bool is_setgid() const { return ::is_setgid(mode); }

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
