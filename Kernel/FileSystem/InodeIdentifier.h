/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/DistinctNumeric.h>
#include <AK/Types.h>

namespace Kernel {

class FileSystem;
struct InodeMetadata;

AK_TYPEDEF_DISTINCT_ORDERED_ID(u32, FileSystemID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u64, InodeIndex);

class InodeIdentifier {
public:
    InodeIdentifier() = default;
    InodeIdentifier(FileSystemID fsid, InodeIndex inode)
        : m_fsid(fsid)
        , m_index(inode)
    {
    }

    bool is_valid() const { return m_fsid != 0 && m_index != 0; }

    FileSystemID fsid() const { return m_fsid; }
    InodeIndex index() const { return m_index; }

    bool operator==(InodeIdentifier const& other) const
    {
        return m_fsid == other.m_fsid && m_index == other.m_index;
    }

    bool operator!=(InodeIdentifier const& other) const
    {
        return m_fsid != other.m_fsid || m_index != other.m_index;
    }

private:
    FileSystemID m_fsid { 0 };
    InodeIndex m_index { 0 };
};

}

template<>
struct AK::Formatter<Kernel::InodeIdentifier> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::InodeIdentifier value)
    {
        return AK::Formatter<FormatString>::format(builder, "{}:{}"sv, value.fsid(), value.index());
    }
};
