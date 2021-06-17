/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileUtils.h"
#include <AK/LexicalPath.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/MessageBox.h>
#include <sys/stat.h>
#include <unistd.h>

namespace FileUtils {

void delete_path(String const& path, GUI::Window* parent_window)
{
    struct stat st;
    if (lstat(path.characters(), &st)) {
        GUI::MessageBox::show(parent_window,
            String::formatted("lstat({}) failed: {}", path, strerror(errno)),
            "Delete failed",
            GUI::MessageBox::Type::Error);
    }

    bool is_directory = S_ISDIR(st.st_mode);
    auto result = Core::File::remove(path, Core::File::RecursionMode::Allowed, false);

    if (result.is_error()) {
        auto& error = result.error();
        if (is_directory) {
            GUI::MessageBox::show(parent_window,
                String::formatted("Failed to delete directory \"{}\": {}", error.file, error.error_code),
                "Delete failed",
                GUI::MessageBox::Type::Error);
        } else {
            GUI::MessageBox::show(parent_window,
                String::formatted("Failed to delete file \"{}\": {}", error.file, error.error_code),
                "Delete failed",
                GUI::MessageBox::Type::Error);
        }
    }
}

void delete_paths(Vector<String> const& paths, bool should_confirm, GUI::Window* parent_window)
{
    String message;
    if (paths.size() == 1) {
        message = String::formatted("Really delete {}?", LexicalPath::basename(paths[0]));
    } else {
        message = String::formatted("Really delete {} files?", paths.size());
    }

    if (should_confirm) {
        auto result = GUI::MessageBox::show(parent_window,
            message,
            "Confirm deletion",
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel);
        if (result == GUI::MessageBox::ExecCancel)
            return;
    }

    for (auto& path : paths) {
        delete_path(path, parent_window);
    }
}

}
