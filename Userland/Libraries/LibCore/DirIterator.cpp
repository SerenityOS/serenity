/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>

namespace Core {

DirIterator::DirIterator(ByteString path, Flags flags)
    : m_path(move(path))
    , m_flags(flags)
{
    m_dir = opendir(m_path.characters());
    if (!m_dir) {
        m_error = Error::from_errno(errno);
    }
}

DirIterator::~DirIterator()
{
    if (m_dir) {
        closedir(m_dir);
        m_dir = nullptr;
    }
}

DirIterator::DirIterator(DirIterator&& other)
    : m_dir(other.m_dir)
    , m_error(move(other.m_error))
    , m_next(move(other.m_next))
    , m_path(move(other.m_path))
    , m_flags(other.m_flags)
{
    other.m_dir = nullptr;
}

bool DirIterator::advance_next()
{
    if (!m_dir)
        return false;

    while (true) {
        errno = 0;
        auto* de = readdir(m_dir);
        if (!de) {
            if (errno != 0) {
                m_error = Error::from_errno(errno);
                dbgln("DirIteration error: {}", m_error.value());
            }
            m_next.clear();
            return false;
        }

#if defined(AK_OS_SOLARIS) || defined(AK_OS_HAIKU)
        m_next = DirectoryEntry::from_stat(m_dir, *de);
#else
        m_next = DirectoryEntry::from_dirent(*de);

        // dirent structures from readdir aren't guaranteed to contain valid file types,
        // as it is possible that the underlying filesystem doesn't keep track of those.
        if (m_next->type == DirectoryEntry::Type::Unknown && !m_next->name.is_empty()) {
            struct stat statbuf;
            if (fstatat(dirfd(m_dir), de->d_name, &statbuf, AT_SYMLINK_NOFOLLOW) < 0) {
                m_error = Error::from_errno(errno);
                dbgln("DirIteration error: {}", m_error.value());
                return false;
            }
            m_next->type = DirectoryEntry::directory_entry_type_from_stat(statbuf.st_mode);
        }
#endif

        if (m_next->name.is_empty())
            return false;

        if (m_flags & Flags::SkipDots && m_next->name.starts_with('.'))
            continue;

        if (m_flags & Flags::SkipParentAndBaseDir && (m_next->name == "." || m_next->name == ".."))
            continue;

        return !m_next->name.is_empty();
    }
}

bool DirIterator::has_next()
{
    if (m_next.has_value())
        return true;

    return advance_next();
}

Optional<DirectoryEntry> DirIterator::next()
{
    if (!m_next.has_value())
        advance_next();

    auto result = m_next;
    m_next.clear();
    return result;
}

ByteString DirIterator::next_path()
{
    auto entry = next();
    if (entry.has_value())
        return entry->name;
    return "";
}

ByteString DirIterator::next_full_path()
{
    StringBuilder builder;
    builder.append(m_path);
    if (!m_path.ends_with('/'))
        builder.append('/');
    builder.append(next_path());
    return builder.to_byte_string();
}

int DirIterator::fd() const
{
    if (!m_dir)
        return -1;
    return dirfd(m_dir);
}

}
