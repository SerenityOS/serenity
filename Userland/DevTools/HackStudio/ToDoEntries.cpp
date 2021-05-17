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

void ToDoEntries::set_entries(const String& filename, const Vector<String>&& entries)
{
    m_document_to_entries.set(filename, move(entries));
    if (on_update)
        on_update();
}

Vector<ToDoEntryPair> ToDoEntries::get_entries()
{
    Vector<ToDoEntryPair> ret;
    for (auto& it : m_document_to_entries)
        for (auto& entry : it.value)
            ret.append({ it.key, entry });

    return ret;
}

}
