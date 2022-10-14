/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <LibCore/Forward.h>
#include <LibGUI/Forward.h>

namespace FileManager {

enum class FileOperation {
    Copy = 0,
    Move,
    Delete,
};

void delete_paths(Vector<DeprecatedString> const&, bool should_confirm, GUI::Window*);

ErrorOr<void> run_file_operation(FileOperation, Vector<DeprecatedString> const& sources, DeprecatedString const& destination, GUI::Window*);
}
