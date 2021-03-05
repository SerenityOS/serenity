/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
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

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <LibGUI/TextDocument.h>

namespace LanguageServers {

class FileDB final {
public:
    RefPtr<const GUI::TextDocument> get(const String& file_name) const;
    RefPtr<GUI::TextDocument> get(const String& file_name);
    RefPtr<const GUI::TextDocument> get_or_create_from_filesystem(const String& file_name) const;
    RefPtr<GUI::TextDocument> get_or_create_from_filesystem(const String& file_name);
    bool add(const String& file_name, int fd);
    bool add(const String& file_name, const String& content);

    void set_project_root(const String& root_path) { m_project_root = root_path; }

    void on_file_edit_insert_text(const String& file_name, const String& inserted_text, size_t start_line, size_t start_column);
    void on_file_edit_remove_text(const String& file_name, size_t start_line, size_t start_column, size_t end_line, size_t end_column);
    String to_absolute_path(const String& file_name) const;
    bool is_open(const String& file_name) const;

private:
    RefPtr<GUI::TextDocument> create_from_filesystem(const String& file_name) const;
    RefPtr<GUI::TextDocument> create_from_fd(int fd) const;
    RefPtr<GUI::TextDocument> create_from_file(Core::File&) const;
    static RefPtr<GUI::TextDocument> create_with_content(const String&);

private:
    HashMap<String, NonnullRefPtr<GUI::TextDocument>> m_open_files;
    String m_project_root;
};

}
