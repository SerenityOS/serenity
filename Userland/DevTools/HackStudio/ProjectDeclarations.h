/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DeclarationsModel.h"
#include <AK/ByteString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/Icon.h>

namespace HackStudio {

class ProjectDeclarations {
    AK_MAKE_NONCOPYABLE(ProjectDeclarations);

public:
    static ProjectDeclarations& the();
    template<typename Func>
    void for_each_declared_symbol(Func);

    void set_declared_symbols(ByteString const& filename, Vector<CodeComprehension::Declaration> const&);

    DeclarationsModel& declarations_model() { return m_declarations_model; }
    void update_declarations_model();

    static Optional<GUI::Icon> get_icon_for(CodeComprehension::DeclarationType);

    Function<void()> on_update = nullptr;

private:
    ProjectDeclarations();

    HashMap<ByteString, Vector<CodeComprehension::Declaration>> m_document_to_declarations;
    NonnullRefPtr<DeclarationsModel> m_declarations_model;
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
