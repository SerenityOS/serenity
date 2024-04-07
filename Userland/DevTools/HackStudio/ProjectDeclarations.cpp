/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectDeclarations.h"
#include "HackStudio.h"

namespace HackStudio {

ProjectDeclarations::ProjectDeclarations()
    : m_declarations_model(adopt_ref(*new DeclarationsModel({})))
{
}

ProjectDeclarations& ProjectDeclarations::the()
{
    static ProjectDeclarations s_instance;
    return s_instance;
}

void ProjectDeclarations::set_declared_symbols(ByteString const& filename, Vector<CodeComprehension::Declaration> const& declarations)
{
    m_document_to_declarations.set(filename, declarations);
    // FIXME: Partially invalidate the model instead of fully rebuilding it.
    update_declarations_model();
    if (on_update)
        on_update();
}

Optional<GUI::Icon> ProjectDeclarations::get_icon_for(CodeComprehension::DeclarationType type)
{
    static GUI::Icon struct_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Struct.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon class_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Class.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon function_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Function.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon variable_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Variable.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon preprocessor_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Preprocessor.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon member_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Member.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon namespace_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Namespace.png"sv).release_value_but_fixme_should_propagate_errors());
    switch (type) {
    case CodeComprehension::DeclarationType::Struct:
        return struct_icon;
    case CodeComprehension::DeclarationType::Class:
        return class_icon;
    case CodeComprehension::DeclarationType::Function:
        return function_icon;
    case CodeComprehension::DeclarationType::Variable:
        return variable_icon;
    case CodeComprehension::DeclarationType::PreprocessorDefinition:
        return preprocessor_icon;
    case CodeComprehension::DeclarationType::Member:
        return member_icon;
    case CodeComprehension::DeclarationType::Namespace:
        return namespace_icon;
    default:
        return {};
    }
}

void ProjectDeclarations::update_declarations_model()
{
    Vector<Declaration> declarations;
    project().for_each_text_file([&](auto& file) {
        declarations.append(Declaration::create_filename(file.name()));
    });
    for_each_declared_symbol([&declarations](auto& decl) {
        declarations.append((Declaration::create_symbol_declaration(decl)));
    });
    m_declarations_model->set_declarations(move(declarations));
}

}
