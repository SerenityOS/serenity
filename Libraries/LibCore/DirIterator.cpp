/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <errno.h>

namespace Core {

DirIterator::DirIterator(const StringView& path, Flags flags)
    : m_path(path)
    , m_flags(flags)
{
    m_dir = opendir(path.to_string().characters());
    if (!m_dir) {
        m_error = errno;
    }
}

DirIterator::~DirIterator()
{
    if (m_dir) {
        closedir(m_dir);
        m_dir = nullptr;
    }
}

bool DirIterator::advance_next()
{
    if (!m_dir)
        return false;

    while (true) {
        errno = 0;
        auto* de = readdir(m_dir);
        if (!de) {
            m_error = errno;
            m_next = String();
            return false;
        }

        m_next = de->d_name;
        if (m_next.is_null())
            return false;

        if (m_flags & Flags::SkipDots && m_next.starts_with('.'))
            continue;

        if (m_flags & Flags::SkipParentAndBaseDir && (m_next == "." || m_next == ".."))
            continue;

        return !m_next.is_empty();
    }
}

bool DirIterator::has_next()
{
    if (!m_next.is_null())
        return true;

    return advance_next();
}

String DirIterator::next_path()
{
    if (m_next.is_null())
        advance_next();

    auto tmp = m_next;
    m_next = String();
    return tmp;
}

String DirIterator::next_full_path()
{
    return String::format("%s/%s", m_path.characters(), next_path().characters());
}

String find_executable_in_path(String filename)
{
    if (filename.starts_with('/')) {
        if (access(filename.characters(), X_OK) == 0)
            return filename;

        return {};
    }

    for (auto directory : String { getenv("PATH") }.split(':')) {
        auto fullpath = String::format("%s/%s", directory.characters(), filename.characters());

        if (access(fullpath.characters(), X_OK) == 0)
            return fullpath;
    }

    return {};
}

}
