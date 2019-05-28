#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Types.h>

class FS;
struct InodeMetadata;

class InodeIdentifier {
public:
    InodeIdentifier() {}
    InodeIdentifier(dword fsid, dword inode)
        : m_fsid(fsid)
        , m_index(inode)
    {
    }

    bool is_valid() const { return m_fsid != 0 && m_index != 0; }

    dword fsid() const { return m_fsid; }
    dword index() const { return m_index; }

    FS* fs();
    const FS* fs() const;

    bool operator==(const InodeIdentifier& other) const
    {
        return m_fsid == other.m_fsid && m_index == other.m_index;
    }

    bool operator!=(const InodeIdentifier& other) const
    {
        return m_fsid != other.m_fsid || m_index != other.m_index;
    }

    bool is_root_inode() const;

private:
    dword m_fsid { 0 };
    dword m_index { 0 };
};
