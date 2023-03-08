/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <LibGUI/TextDocument.h>
#include <LibSyntax/Language.h>

namespace HackStudio {

class CodeDocument final : public GUI::TextDocument {
public:
    virtual ~CodeDocument() override = default;
    static NonnullRefPtr<CodeDocument> create(DeprecatedString const& file_path, Client* client = nullptr);
    static NonnullRefPtr<CodeDocument> create(Client* client = nullptr);

    Vector<size_t> const& breakpoint_lines() const { return m_breakpoint_lines; }
    Vector<size_t>& breakpoint_lines() { return m_breakpoint_lines; }
    Optional<size_t> execution_position() const { return m_execution_position; }
    void set_execution_position(size_t line) { m_execution_position = line; }
    void clear_execution_position() { m_execution_position.clear(); }
    DeprecatedString const& file_path() const { return m_file_path; }
    Optional<Syntax::Language> const& language() const { return m_language; }

    virtual bool is_code_document() const override final { return true; }

private:
    explicit CodeDocument(DeprecatedString const& file_path, Client* client = nullptr);
    explicit CodeDocument(Client* client = nullptr);

    DeprecatedString m_file_path;
    Optional<Syntax::Language> m_language;
    Vector<size_t> m_breakpoint_lines;
    Optional<size_t> m_execution_position;
};

}
