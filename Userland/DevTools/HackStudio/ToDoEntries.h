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
#include <LibCpp/Parser.h>

namespace HackStudio {

class ToDoEntries {
    AK_MAKE_NONCOPYABLE(ToDoEntries);

public:
    static ToDoEntries& the();

    void set_entries(String const& filename, Vector<Cpp::Parser::TodoEntry> const&& entries);

    Vector<Cpp::Parser::TodoEntry> get_entries();

    void clear_entries();

    Function<void()> on_update = nullptr;

private:
    ToDoEntries() = default;
    HashMap<String, Vector<Cpp::Parser::TodoEntry>> m_document_to_entries;
};

}
