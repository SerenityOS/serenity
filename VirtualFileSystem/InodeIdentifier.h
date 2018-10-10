#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Types.h>

class FileSystem;
struct InodeMetadata;

class InodeIdentifier {
public:
    InodeIdentifier() { }
    InodeIdentifier(dword fileSystemID, dword inode)
        : m_fileSystemID(fileSystemID)
        , m_index(inode)
    {
    }

    bool isValid() const { return m_fileSystemID != 0 && m_index != 0; }

    dword fileSystemID() const { return m_fileSystemID; }
    dword index() const { return m_index; }

    FileSystem* fileSystem();
    const FileSystem* fileSystem() const;

    bool operator==(const InodeIdentifier& other) const
    {
        return m_fileSystemID == other.m_fileSystemID && m_index == other.m_index;
    }

    InodeMetadata metadata() const;
    bool isRootInode() const;

    ByteBuffer readEntireFile() const;

private:
    dword m_fileSystemID { 0 };
    dword m_index { 0 };
};

