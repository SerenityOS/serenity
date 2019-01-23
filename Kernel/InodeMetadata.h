#pragma once

#include "InodeIdentifier.h"
#include "UnixTypes.h"
#include <AK/HashTable.h>

inline bool isDirectory(mode_t mode) { return (mode & 0170000) == 0040000; }
inline bool isCharacterDevice(mode_t mode) { return (mode & 0170000) == 0020000; }
inline bool isBlockDevice(mode_t mode) { return (mode & 0170000) == 0060000; }
inline bool isRegularFile(mode_t mode) { return (mode & 0170000) == 0100000; }
inline bool isFIFO(mode_t mode) { return (mode & 0170000) == 0010000; }
inline bool isSymbolicLink(mode_t mode) { return (mode & 0170000) == 0120000; }
inline bool isSocket(mode_t mode) { return (mode & 0170000) == 0140000; }
inline bool isSticky(mode_t mode) { return mode & 01000; }
inline bool isSetUID(mode_t mode) { return mode & 04000; }
inline bool isSetGID(mode_t mode) { return mode & 02000; }

struct InodeMetadata {
    bool isValid() const { return inode.is_valid(); }

    bool mayExecute(uid_t u, const HashTable<gid_t>& g) const
    {
        if (uid == u)
            return mode & 0100;
        if (g.contains(gid))
            return mode & 0010;
        return mode & 0001;
    }

    bool isDirectory() const { return ::isDirectory(mode); }
    bool isCharacterDevice() const { return ::isCharacterDevice(mode); }
    bool isBlockDevice() const { return ::isBlockDevice(mode); }
    bool isRegularFile() const { return ::isRegularFile(mode); }
    bool isFIFO() const { return ::isFIFO(mode); }
    bool isSymbolicLink() const { return ::isSymbolicLink(mode); }
    bool isSocket() const { return ::isSocket(mode); }
    bool isSticky() const { return ::isSticky(mode); }
    bool isSetUID() const { return ::isSetUID(mode); }
    bool isSetGID() const { return ::isSetGID(mode); }

    InodeIdentifier inode;
    off_t size { 0 };
    mode_t mode { 0 };
    uid_t uid { 0 };
    gid_t gid { 0 };
    nlink_t linkCount { 0 };
    time_t atime { 0 };
    time_t ctime { 0 };
    time_t mtime { 0 };
    time_t dtime { 0 };
    blkcnt_t blockCount { 0 };
    blksize_t blockSize { 0 };
    unsigned majorDevice { 0 };
    unsigned minorDevice { 0 };
};


