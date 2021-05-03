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
void HackStudio::ProjectDeclarations::set_declared_symbols(const String& filename, const Vector<GUI::AutocompleteProvider::Declaration>& declarations)
{
    m_document_to_declarations.set(filename, declarations);
    if (on_update)
        on_update();
}

Optional<GUI::Icon> HackStudio::ProjectDeclarations::get_icon_for(GUI::AutocompleteProvider::DeclarationType type)
{
    static GUI::Icon struct_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Struct.png"));
    static GUI::Icon class_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Class.png"));
    static GUI::Icon function_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Function.png"));
    static GUI::Icon variable_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Variable.png"));
    static GUI::Icon preprocessor_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Preprocessor.png"));
    static GUI::Icon member_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Member.png"));
    static GUI::Icon namespace_icon(Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Namespace.png"));
    switch (type) {
    case GUI::AutocompleteProvider::DeclarationType::Struct:
        return struct_icon;
    case GUI::AutocompleteProvider::DeclarationType::Class:
        return class_icon;
    case GUI::AutocompleteProvider::DeclarationType::Function:
        return function_icon;
    case GUI::AutocompleteProvider::DeclarationType::Variable:
        return variable_icon;
    case GUI::AutocompleteProvider::DeclarationType::PreprocessorDefinition:
        return preprocessor_icon;
    case GUI::AutocompleteProvider::DeclarationType::Member:
        return member_icon;
    case GUI::AutocompleteProvider::DeclarationType::Namespace:
        return namespace_icon;
    default:
        return {};
    }
}
