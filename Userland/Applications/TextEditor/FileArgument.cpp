/*
 * Copyright (c) 2021, ry755 <ryanst755@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileArgument.h"
#include <LibRegex/Regex.h>

namespace TextEditor {

FileArgument::FileArgument(String file_argument)
{
    m_line = {};
    m_column = {};

    // A file doesn't exist with the full specified name, maybe the user entered
    // line/column coordinates?
    Regex<PosixExtended> re("^(.+?)(?::([0-9]+))?(?::([0-9]+))?$");
    RegexResult result = match(file_argument, re,
        PosixFlags::Global | PosixFlags::Multiline | PosixFlags::Ungreedy);
    auto& groups = result.capture_group_matches.at(0);

    // Match 0 group 0: file name
    // Match 0 group 1: line number
    // Match 0 group 2: column number
    if (groups.size() > 2) {
        // Both a line and column number were specified.
        auto filename = groups.at(0).view.to_string().release_value_but_fixme_should_propagate_errors();
        auto initial_line_number = groups.at(1).view.to_string().release_value_but_fixme_should_propagate_errors().to_number<int>();
        auto initial_column_number = groups.at(2).view.to_string().release_value_but_fixme_should_propagate_errors().to_number<int>();

        m_filename = filename;
        if (initial_line_number.has_value() && initial_line_number.value() > 0)
            m_line = initial_line_number.value();
        if (initial_column_number.has_value())
            m_column = initial_column_number.value();
    } else if (groups.size() == 2) {
        // Only a line number was specified.
        auto filename = groups.at(0).view.to_string().release_value_but_fixme_should_propagate_errors();
        auto initial_line_number = groups.at(1).view.to_string().release_value_but_fixme_should_propagate_errors().to_number<int>();

        m_filename = filename;
        if (initial_line_number.has_value() && initial_line_number.value() > 0)
            m_line = initial_line_number.value();
    } else {
        // A colon was found at the end of the file name but no values were found
        // after it.
        m_filename = groups.at(0).view.to_string().release_value_but_fixme_should_propagate_errors();
    }
}
}
