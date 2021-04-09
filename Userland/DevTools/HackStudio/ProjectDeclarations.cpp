/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
