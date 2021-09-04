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
    RefPtr<const GUI::TextDocument> get(String const& filename) const;
    RefPtr<GUI::TextDocument> get(String const& filename);
    RefPtr<const GUI::TextDocument> get_or_create_from_filesystem(String const& filename) const;
    RefPtr<GUI::TextDocument> get_or_create_from_filesystem(String const& filename);
    bool add(String const& filename, int fd);
    bool add(String const& filename, String const& content);

    void set_project_root(String const& root_path) { m_project_root = root_path; }
    String const& project_root() const { return m_project_root; }

    void on_file_edit_insert_text(String const& filename, String const& inserted_text, size_t start_line, size_t start_column);
    void on_file_edit_remove_text(String const& filename, size_t start_line, size_t start_column, size_t end_line, size_t end_column);
    String to_absolute_path(String const& filename) const;
    bool is_open(String const& filename) const;

private:
    RefPtr<GUI::TextDocument> create_from_filesystem(String const& filename) const;
    RefPtr<GUI::TextDocument> create_from_fd(int fd) const;
    RefPtr<GUI::TextDocument> create_from_file(Core::File&) const;
    static RefPtr<GUI::TextDocument> create_with_content(String const&);

private:
    HashMap<String, NonnullRefPtr<GUI::TextDocument>> m_open_files;
    String m_project_root;
};

}
