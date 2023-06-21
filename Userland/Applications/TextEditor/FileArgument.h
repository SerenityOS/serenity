/*
 * Copyright (c) 2021, ry755 <ryanst755@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace TextEditor {

class FileArgument final {
public:
    explicit FileArgument(String);
    ~FileArgument() = default;

    String filename() { return m_filename; }
    Optional<size_t> line() { return m_line; }
    Optional<size_t> column() { return m_column; }

private:
    String m_filename;
    Optional<size_t> m_line;
    Optional<size_t> m_column;
};

}
