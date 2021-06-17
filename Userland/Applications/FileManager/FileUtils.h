/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/Forward.h>
#include <LibGUI/Forward.h>
#include <sys/stat.h>

namespace FileManager {

enum class FileOperation {
    Copy = 0,
    Cut
};

void delete_path(String const&, GUI::Window*);
void delete_paths(Vector<String> const&, bool should_confirm, GUI::Window*);

void run_file_operation(FileOperation, Vector<String> const& sources, String const& destination, GUI::Window*);
}
