/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/DistinctNumeric.h>
#include <AK/String.h>
#include <AK/Types.h>

namespace Kernel {

class FileSystem;
struct InodeMetadata;

TYPEDEF_DISTINCT_ORDERED_ID(u64, InodeIndex);

class InodeIdentifier {
public:
    InodeIdentifier() = default;
    InodeIdentifier(u32 fsid, InodeIndex inode)
        : m_fsid(fsid)
        , m_index(inode)
    {
    }

    bool is_valid() const { return m_fsid != 0 && m_index != 0; }

    u32 fsid() const { return m_fsid; }
    InodeIndex index() const { return m_index; }

    FileSystem* fs();
    FileSystem const* fs() const;

    bool operator==(InodeIdentifier const& other) const
    {
        return m_fsid == other.m_fsid && m_index == other.m_index;
    }

    bool operator!=(InodeIdentifier const& other) const
    {
        return m_fsid != other.m_fsid || m_index != other.m_index;
    }

    String to_string() const { return String::formatted("{}:{}", m_fsid, m_index); }

private:
    u32 m_fsid { 0 };
    InodeIndex m_index { 0 };
};

}

template<>
struct AK::Formatter<Kernel::InodeIdentifier> : AK::Formatter<FormatString> {
    void format(FormatBuilder& builder, Kernel::InodeIdentifier value)
    {
        return AK::Formatter<FormatString>::format(builder, "{}:{}", value.fsid(), value.index());
    }
};

template<>
struct AK::Formatter<Kernel::InodeIndex> : AK::Formatter<FormatString> {
    void format(FormatBuilder& builder, Kernel::InodeIndex value)
    {
        return AK::Formatter<FormatString>::format(builder, "{}", value.value());
    }
};
