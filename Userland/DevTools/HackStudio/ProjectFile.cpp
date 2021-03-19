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

#include "ProjectFile.h"
#include <LibCore/File.h>
#include <string.h>

namespace HackStudio {

ProjectFile::ProjectFile(const String& name)
    : m_name(name)
{
}

GUI::TextDocument& ProjectFile::document() const
{
    create_document_if_needed();
    VERIFY(m_document);
    return *m_document;
}

int ProjectFile::vertical_scroll_value() const
{
    return m_vertical_scroll_value;
}

void ProjectFile::vertical_scroll_value(int vertical_scroll_value)
{
    m_vertical_scroll_value = vertical_scroll_value;
}

int ProjectFile::horizontal_scroll_value() const
{
    return m_horizontal_scroll_value;
}

void ProjectFile::horizontal_scroll_value(int horizontal_scroll_value)
{
    m_horizontal_scroll_value = horizontal_scroll_value;
}

CodeDocument& ProjectFile::code_document() const
{
    create_document_if_needed();
    VERIFY(m_document);
    return *m_document;
}

void ProjectFile::create_document_if_needed() const
{
    if (m_document)
        return;

    m_document = CodeDocument::create(m_name);
    auto file_or_error = Core::File::open(m_name, Core::File::ReadOnly);
    if (file_or_error.is_error()) {
        warnln("Couldn't open '{}': {}", m_name, file_or_error.error());
        // This is okay though, we'll just go with an empty document and create the file when saving.
        return;
    }

    auto& file = *file_or_error.value();
    m_could_render_text = m_document->set_text(file.read_all());
}

}
