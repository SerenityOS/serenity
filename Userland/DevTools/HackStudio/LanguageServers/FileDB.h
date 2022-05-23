/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <LibCodeComprehension/FileDB.h>
#include <LibGUI/TextDocument.h>

namespace LanguageServers {

class FileDB final : public CodeComprehension::FileDB {
public:
    FileDB() = default;
    virtual Optional<String> get_or_read_from_filesystem(StringView filename) const override;

    RefPtr<const GUI::TextDocument> get_document(String const& filename) const;
    RefPtr<GUI::TextDocument> get_document(String const& filename);

    bool add(String const& filename, int fd);
    bool add(String const& filename, String const& content);

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
