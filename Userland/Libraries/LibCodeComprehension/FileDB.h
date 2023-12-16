/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/StringView.h>

namespace CodeComprehension {

class FileDB {
    AK_MAKE_NONCOPYABLE(FileDB);
    AK_MAKE_NONMOVABLE(FileDB);

public:
    virtual ~FileDB() = default;

    virtual Optional<ByteString> get_or_read_from_filesystem(StringView filename) const = 0;
    void set_project_root(StringView project_root)
    {
        if (project_root.is_null())
            m_project_root.clear();
        else
            m_project_root = project_root;
    }
    Optional<ByteString> const& project_root() const { return m_project_root; }
    ByteString to_absolute_path(StringView filename) const;

protected:
    FileDB() = default;

private:
    Optional<ByteString> m_project_root;
};

}
