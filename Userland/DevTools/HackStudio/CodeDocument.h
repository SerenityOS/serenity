/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Language.h"
#include <AK/LexicalPath.h>
#include <LibGUI/TextDocument.h>

namespace HackStudio {

class CodeDocument final : public GUI::TextDocument {
public:
    virtual ~CodeDocument() override = default;
    static NonnullRefPtr<CodeDocument> create(const String& file_path, Client* client = nullptr);
    static NonnullRefPtr<CodeDocument> create(Client* client = nullptr);

    const Vector<size_t>& breakpoint_lines() const { return m_breakpoint_lines; }
    Vector<size_t>& breakpoint_lines() { return m_breakpoint_lines; }
    Optional<size_t> execution_position() const { return m_execution_position; }
    void set_execution_position(size_t line) { m_execution_position = line; }
    void clear_execution_position() { m_execution_position.clear(); }
    const String& file_path() const { return m_file_path; }
    const String& language_name() const { return m_language_name; };
    Language language() const { return m_language; }

    virtual bool is_code_document() const override final { return true; }

private:
    explicit CodeDocument(const String& file_path, Client* client = nullptr);
    explicit CodeDocument(Client* client = nullptr);

    String m_file_path;
    String m_language_name;
    Language m_language { Language::Unknown };
    Vector<size_t> m_breakpoint_lines;
    Optional<size_t> m_execution_position;
};

}
