/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/Forward.h>
#include <LibGUI/Forward.h>
#include <sys/stat.h>

namespace FileUtils {

enum class FileOperation {
    Copy = 0,
    Cut
};

void delete_path(String const&, GUI::Window*);
void delete_paths(Vector<String> const&, bool should_confirm, GUI::Window*);
}
