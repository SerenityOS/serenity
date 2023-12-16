/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectFile.h"
#include <LibCore/File.h>

namespace HackStudio {

ProjectFile::ProjectFile(ByteString const& name)
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
    auto file_or_error = Core::File::open(m_name, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        warnln("Couldn't open '{}': {}", m_name, file_or_error.error());
        // This is okay though, we'll just go with an empty document and create the file when saving.
        return;
    }

    auto& file = *file_or_error.value();
    m_could_render_text = m_document->set_text(file.read_until_eof().release_value_but_fixme_should_propagate_errors());
}

}
