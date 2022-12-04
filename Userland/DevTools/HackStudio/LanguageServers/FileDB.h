/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <LibCodeComprehension/FileDB.h>
#include <LibGUI/TextDocument.h>

namespace LanguageServers {

class FileDB final : public CodeComprehension::FileDB {
public:
    FileDB() = default;
    virtual Optional<DeprecatedString> get_or_read_from_filesystem(StringView filename) const override;

    RefPtr<const GUI::TextDocument> get_document(DeprecatedString const& filename) const;
    RefPtr<GUI::TextDocument> get_document(DeprecatedString const& filename);

    bool add(DeprecatedString const& filename, int fd);
    bool add(DeprecatedString const& filename, DeprecatedString const& content);

    void on_file_edit_insert_text(DeprecatedString const& filename, DeprecatedString const& inserted_text, size_t start_line, size_t start_column);
    void on_file_edit_remove_text(DeprecatedString const& filename, size_t start_line, size_t start_column, size_t end_line, size_t end_column);
    DeprecatedString to_absolute_path(DeprecatedString const& filename) const;
    bool is_open(DeprecatedString const& filename) const;

private:
    RefPtr<GUI::TextDocument> create_from_filesystem(DeprecatedString const& filename) const;
    RefPtr<GUI::TextDocument> create_from_fd(int fd) const;
    RefPtr<GUI::TextDocument> create_from_file(Core::File&) const;
    static RefPtr<GUI::TextDocument> create_with_content(DeprecatedString const&);

private:
    HashMap<DeprecatedString, NonnullRefPtr<GUI::TextDocument>> m_open_files;
    DeprecatedString m_project_root;
};

}
