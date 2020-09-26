/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include "Language.h"
#include <AK/LexicalPath.h>
#include <LibGUI/TextDocument.h>

namespace HackStudio {

class CodeDocument final : public GUI::TextDocument {
public:
    virtual ~CodeDocument() override;
    static NonnullRefPtr<CodeDocument> create(const LexicalPath& file_path, Client* client = nullptr);
    static NonnullRefPtr<CodeDocument> create(Client* client = nullptr);

    const Vector<size_t>& breakpoint_lines() const { return m_breakpoint_lines; }
    Vector<size_t>& breakpoint_lines() { return m_breakpoint_lines; }
    Optional<size_t> execution_position() const { return m_execution_position; }
    void set_execution_position(size_t line) { m_execution_position = line; }
    void clear_execution_position() { m_execution_position.clear(); }
    const LexicalPath& file_path() const { return m_file_path; }
    Language language() const { return m_language; }

    virtual bool is_code_document() const override final { return true; }

private:
    explicit CodeDocument(const LexicalPath& file_path, Client* client = nullptr);
    explicit CodeDocument(Client* client = nullptr);

    LexicalPath m_file_path;
    Language m_language { Language::Unknown };
    Vector<size_t> m_breakpoint_lines;
    Optional<size_t> m_execution_position;
};

}
