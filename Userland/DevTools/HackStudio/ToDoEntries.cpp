/*
 * Copyright (c) 2021, Federico Guerinoni <guerinoni.federico@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ToDoEntries.h"

namespace HackStudio {

ToDoEntries& HackStudio::ToDoEntries::the()
{
    static ToDoEntries s_instance;
    return s_instance;
}

void ToDoEntries::set_entries(ByteString const& filename, Vector<CodeComprehension::TodoEntry> const&& entries)
{
    m_document_to_entries.set(filename, move(entries));
    if (on_update)
        on_update();
}

Vector<CodeComprehension::TodoEntry> ToDoEntries::get_entries()
{
    Vector<CodeComprehension::TodoEntry> ret;
    for (auto& it : m_document_to_entries) {
        for (auto& entry : it.value)
            ret.append({ entry.content, it.key, entry.line, entry.column });
    }
    return ret;
}

void ToDoEntries::clear_entries()
{
    m_document_to_entries.clear();
}

}
