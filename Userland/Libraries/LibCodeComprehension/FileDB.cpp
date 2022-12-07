/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileDB.h"
#include <AK/LexicalPath.h>

namespace CodeComprehension {

DeprecatedString FileDB::to_absolute_path(StringView filename) const
{
    if (LexicalPath::from_string(filename).release_value_but_fixme_should_propagate_errors().is_absolute()) {
        return filename;
    }
    if (m_project_root.is_null())
        return filename;
    return LexicalPath::from_string(String::formatted("{}/{}", m_project_root, filename).release_value_but_fixme_should_propagate_errors()).release_value_but_fixme_should_propagate_errors().string().to_deprecated_string();
}

}
