/*
 * Copyright (c) 2021, ry755 <ryanst755@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileArgument.h"
#include <AK/Optional.h>
#include <AK/String.h>
#include <LibCore/File.h>
#include <LibRegex/Regex.h>

FileArgument::FileArgument(String file_argument)
{
    m_line = {};
    m_column = {};

    if (Core::File::exists(file_argument)) {
        // A file exists with the full specified name, don't attempt to parse it.
        m_file_name = file_argument;
        return;
    }

    // A file doesn't exist with the full specified name, maybe the user entered line/column coordinates?
    Regex<PosixExtended> re("^(.+?)(:([0-9]+):?([0-9]+)?)?$");
    RegexResult result = match(file_argument, re, PosixFlags::Global | PosixFlags::Multiline | PosixFlags::Ungreedy);
    auto& groups = result.capture_group_matches.at(0);

    // Match 0 group 0: file name
    // Match 0 group 1: line number
    // Match 0 group 2: column number

    if (groups.size() > 3) {
        // Both a line and column number were specified.
        auto file_name = groups.at(0).view.to_string();
        auto initial_line_number = groups.at(1).view.to_string().to_int();
        auto initial_column_number = groups.at(2).view.to_string().to_int();

        m_file_name = file_name;
        if (initial_line_number.has_value() && initial_line_number.value() > 0)
            m_line = initial_line_number.value();
        if (initial_column_number.has_value())
            m_column = initial_column_number.value();
    } else if (groups.size() == 3) {
        // Only a line number was specified.
        auto file_name = groups.at(0).view.to_string();
        auto initial_line_number = groups.at(1).view.to_string().to_int();

        m_file_name = file_name;
        if (initial_line_number.has_value() && initial_line_number.value() > 0)
            m_line = initial_line_number.value();
    } else {
        // A colon was found at the end of the file name but no values were found after it.
        auto file_name = groups.at(0).view.to_string();

        m_file_name = file_name;
    }
}

FileArgument::~FileArgument()
{
}
