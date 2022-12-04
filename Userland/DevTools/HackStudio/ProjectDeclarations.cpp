/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectDeclarations.h"

HackStudio::ProjectDeclarations& HackStudio::ProjectDeclarations::the()
{
    static ProjectDeclarations s_instance;
    return s_instance;
}
void HackStudio::ProjectDeclarations::set_declared_symbols(DeprecatedString const& filename, Vector<CodeComprehension::Declaration> const& declarations)
{
    m_document_to_declarations.set(filename, declarations);
    if (on_update)
        on_update();
}

Optional<GUI::Icon> HackStudio::ProjectDeclarations::get_icon_for(CodeComprehension::DeclarationType type)
{
    static GUI::Icon struct_icon(Gfx::Bitmap::try_load_from_file("/res/icons/hackstudio/Struct.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon class_icon(Gfx::Bitmap::try_load_from_file("/res/icons/hackstudio/Class.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon function_icon(Gfx::Bitmap::try_load_from_file("/res/icons/hackstudio/Function.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon variable_icon(Gfx::Bitmap::try_load_from_file("/res/icons/hackstudio/Variable.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon preprocessor_icon(Gfx::Bitmap::try_load_from_file("/res/icons/hackstudio/Preprocessor.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon member_icon(Gfx::Bitmap::try_load_from_file("/res/icons/hackstudio/Member.png"sv).release_value_but_fixme_should_propagate_errors());
    static GUI::Icon namespace_icon(Gfx::Bitmap::try_load_from_file("/res/icons/hackstudio/Namespace.png"sv).release_value_but_fixme_should_propagate_errors());
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
