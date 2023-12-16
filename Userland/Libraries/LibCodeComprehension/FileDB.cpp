/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileDB.h"
#include <AK/LexicalPath.h>

namespace CodeComprehension {

ByteString FileDB::to_absolute_path(StringView filename) const
{
    if (LexicalPath { filename }.is_absolute()) {
        return filename;
    }
    if (!m_project_root.has_value())
        return filename;
    return LexicalPath { ByteString::formatted("{}/{}", *m_project_root, filename) }.string();
}

}
