#pragma once

#include <AK/AKString.h>
#include <AK/ByteBuffer.h>
#include <AK/Types.h>

class FS;
struct InodeMetadata;

class InodeIdentifier {
public:
    InodeIdentifier() {}
    InodeIdentifier(u32 fsid, u32 inode)
        : m_fsid(fsid)
        , m_index(inode)
    {
    }

    bool is_valid() const { return m_fsid != 0 && m_index != 0; }

    u32 fsid() const { return m_fsid; }
    u32 index() const { return m_index; }

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

    String to_string() const { return String::format("%u:%u", m_fsid, m_index); }

private:
    u32 m_fsid { 0 };
    u32 m_index { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, const InodeIdentifier& value)
{
    stream << value.fsid() << ':' << value.index();
    return stream;
}

