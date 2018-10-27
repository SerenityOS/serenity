#pragma once

#include "InodeIdentifier.h"
#include "UnixTypes.h"

inline bool isDirectory(Unix::mode_t mode) { return (mode & 0170000) == 0040000; }
inline bool isCharacterDevice(Unix::mode_t mode) { return (mode & 0170000) == 0020000; }
inline bool isBlockDevice(Unix::mode_t mode) { return (mode & 0170000) == 0060000; }
inline bool isRegularFile(Unix::mode_t mode) { return (mode & 0170000) == 0100000; }
inline bool isFIFO(Unix::mode_t mode) { return (mode & 0170000) == 0010000; }
inline bool isSymbolicLink(Unix::mode_t mode) { return (mode & 0170000) == 0120000; }
inline bool isSocket(Unix::mode_t mode) { return (mode & 0170000) == 0140000; }
inline bool isSticky(Unix::mode_t mode) { return mode & 01000; }
inline bool isSetUID(Unix::mode_t mode) { return mode & 04000; }
inline bool isSetGID(Unix::mode_t mode) { return mode & 02000; }

struct InodeMetadata {
    bool isValid() const { return inode.isValid(); }

    bool mayExecute(uid_t u, gid_t g) const
    {
        if (uid == u)
            return mode & 0100;
        if (gid == g)
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
    Unix::off_t size { 0 };
    Unix::mode_t mode { 0 };
    Unix::uid_t uid { 0 };
    Unix::gid_t gid { 0 };
    Unix::nlink_t linkCount { 0 };
    Unix::time_t atime { 0 };
    Unix::time_t ctime { 0 };
    Unix::time_t mtime { 0 };
    Unix::time_t dtime { 0 };
    Unix::blkcnt_t blockCount { 0 };
    Unix::blksize_t blockSize { 0 };
    unsigned majorDevice { 0 };
    unsigned minorDevice { 0 };
};


