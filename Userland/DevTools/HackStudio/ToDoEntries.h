/*
 * Copyright (c) 2021, Federico Guerinoni <guerinoni.federico@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Function.h>
#include <YAK/HashMap.h>
#include <YAK/Noncopyable.h>
#include <YAK/String.h>
#include <LibCpp/Parser.h>

namespace HackStudio {

class ToDoEntries {
    YAK_MAKE_NONCOPYABLE(ToDoEntries);

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
