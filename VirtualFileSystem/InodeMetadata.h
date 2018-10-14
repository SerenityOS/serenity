#pragma once

#include "InodeIdentifier.h"

inline bool isDirectory(word mode) { return (mode & 0170000) == 0040000; }
inline bool isCharacterDevice(word mode) { return (mode & 0170000) == 0020000; }
inline bool isBlockDevice(word mode) { return (mode & 0170000) == 0060000; }
inline bool isRegularFile(word mode) { return (mode & 0170000) == 0100000; }
inline bool isFIFO(word mode) { return (mode & 0170000) == 0010000; }
inline bool isSymbolicLink(word mode) { return (mode & 0170000) == 0120000; }
inline bool isSocket(word mode) { return (mode & 0170000) == 0140000; }
inline bool isSticky(word mode) { return mode & 01000; }
inline bool isSetUID(word mode) { return mode & 04000; }
inline bool isSetGID(word mode) { return mode & 02000; }

struct InodeMetadata {
    bool isValid() const { return inode.isValid(); }

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
    dword size { 0 };
    word mode { 0 };
    dword uid { 0 };
    dword gid { 0 };
    dword linkCount { 0 };
    time_t atime { 0 };
    time_t ctime { 0 };
    time_t mtime { 0 };
    time_t dtime { 0 };
    unsigned majorDevice { 0 };
    unsigned minorDevice { 0 };
};


