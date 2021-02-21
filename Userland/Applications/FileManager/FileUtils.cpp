/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "FileUtils.h"
#include <AK/LexicalPath.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/MessageBox.h>
#include <sys/stat.h>
#include <unistd.h>

namespace FileUtils {

void delete_path(const String& path, GUI::Window* parent_window)
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

void delete_paths(const Vector<String>& paths, bool should_confirm, GUI::Window* parent_window)
{
    String message;
    if (paths.size() == 1) {
        message = String::formatted("Really delete {}?", LexicalPath(paths[0]).basename());
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
