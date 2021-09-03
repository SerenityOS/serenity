/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Function.h>
#include <YAK/HashMap.h>
#include <YAK/Noncopyable.h>
#include <YAK/String.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/Icon.h>

namespace HackStudio {

class ProjectDeclarations {
    YAK_MAKE_NONCOPYABLE(ProjectDeclarations);

public:
    static ProjectDeclarations& the();
    template<typename Func>
    void for_each_declared_symbol(Func);

    void set_declared_symbols(const String& filename, const Vector<GUI::AutocompleteProvider::Declaration>&);

    static Optional<GUI::Icon> get_icon_for(GUI::AutocompleteProvider::DeclarationType);

    Function<void()> on_update = nullptr;

private:
    ProjectDeclarations() = default;
    HashMap<String, Vector<GUI::AutocompleteProvider::Declaration>> m_document_to_declarations;
};

template<typename Func>
void ProjectDeclarations::for_each_declared_symbol(Func f)
{
    for (auto& item : m_document_to_declarations) {
        for (auto& decl : item.value) {
            f(decl);
        }
    }
}

}
