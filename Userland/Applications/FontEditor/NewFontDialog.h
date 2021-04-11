/*
 * Copyright (c) 2021, the SerenityOS developers
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

#pragma once

#include <LibGUI/Window.h>
#include <LibGUI/Wizards/WizardDialog.h>
#include <LibGUI/Wizards/WizardPage.h>
#include <LibGfx/BitmapFont.h>

class NewFontDialog final : public GUI::WizardDialog {
    C_OBJECT(NewFontDialog);

public:
    auto new_font_metadata()
    {
        save_metadata();
        return m_new_font_metadata;
    }

private:
    NewFontDialog(GUI::Window* parent_window);

    void save_metadata();

    RefPtr<Gfx::BitmapFont> m_font_clone;

    struct NewFontMetadata {
        u8 glyph_width;
        u8 glyph_height;
        u8 glyph_spacing;
        u8 baseline;
        u8 mean_line;
        u8 presentation_size;
        u16 weight;
        String name;
        String family;
        Gfx::FontTypes type;
        bool is_fixed_width;
    } m_new_font_metadata;

    RefPtr<GUI::WizardPage> m_font_selection_page;
    RefPtr<GUI::ComboBox> m_select_font_combobox;
    RefPtr<GUI::Button> m_browse_button;

    RefPtr<GUI::WizardPage> m_font_properties_page;
    RefPtr<GUI::TextBox> m_name_textbox;
    RefPtr<GUI::TextBox> m_family_textbox;
    RefPtr<GUI::ComboBox> m_type_combobox;
    RefPtr<GUI::Label> m_type_info_label;
    RefPtr<GUI::ComboBox> m_weight_combobox;
    RefPtr<GUI::SpinBox> m_presentation_spinbox;

    RefPtr<GUI::WizardPage> m_glyph_properties_page;
    RefPtr<GUI::Widget> m_glyph_editor_container;
    RefPtr<GUI::SpinBox> m_glyph_height_spinbox;
    RefPtr<GUI::SpinBox> m_glyph_width_spinbox;
    RefPtr<GUI::SpinBox> m_baseline_spinbox;
    RefPtr<GUI::SpinBox> m_mean_line_spinbox;
    RefPtr<GUI::SpinBox> m_spacing_spinbox;
    RefPtr<GUI::CheckBox> m_fixed_width_checkbox;

    Vector<String> m_font_list;
    Vector<String> m_font_type_list;
    Vector<String> m_font_weight_list;
};
