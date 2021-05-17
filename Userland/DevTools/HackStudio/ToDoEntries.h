/*
 * Copyright (c) 2021, Federico Guerinoni <guerinoni.federico@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/String.h>

namespace HackStudio {

struct ToDoEntryPair {
    String filename;
    String comment;
};

class ToDoEntries {
    AK_MAKE_NONCOPYABLE(ToDoEntries);

public:
    static ToDoEntries& the();

    void set_entries(const String& filename, const Vector<String>&& entries);

    Vector<ToDoEntryPair> get_entries();

    Function<void()> on_update = nullptr;

private:
    ToDoEntries() = default;
    HashMap<String, Vector<String>> m_document_to_entries;
};

}
