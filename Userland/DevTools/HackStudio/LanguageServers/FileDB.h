/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <LibGUI/TextDocument.h>

namespace LanguageServers {

class FileDB final {
public:
    RefPtr<const GUI::TextDocument> get(const String& filename) const;
    RefPtr<GUI::TextDocument> get(const String& filename);
    RefPtr<const GUI::TextDocument> get_or_create_from_filesystem(const String& filename) const;
    RefPtr<GUI::TextDocument> get_or_create_from_filesystem(const String& filename);
    bool add(const String& filename, int fd);
    bool add(const String& filename, const String& content);

    void set_project_root(const String& root_path) { m_project_root = root_path; }
    const String& project_root() const { return m_project_root; }

    void on_file_edit_insert_text(const String& filename, const String& inserted_text, size_t start_line, size_t start_column);
    void on_file_edit_remove_text(const String& filename, size_t start_line, size_t start_column, size_t end_line, size_t end_column);
    String to_absolute_path(const String& filename) const;
    bool is_open(const String& filename) const;

private:
    RefPtr<GUI::TextDocument> create_from_filesystem(const String& filename) const;
    RefPtr<GUI::TextDocument> create_from_fd(int fd) const;
    RefPtr<GUI::TextDocument> create_from_file(Core::File&) const;
    static RefPtr<GUI::TextDocument> create_with_content(const String&);

private:
    HashMap<String, NonnullRefPtr<GUI::TextDocument>> m_open_files;
    String m_project_root;
};

}
