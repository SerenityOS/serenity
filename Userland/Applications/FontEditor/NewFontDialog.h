/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    struct NewFontMetadata {
        u8 glyph_width;
        u8 glyph_height;
        u8 glyph_spacing;
        u8 baseline;
        u8 mean_line;
        u8 presentation_size;
        u16 weight;
        u8 slope;
        String name;
        String family;
        bool is_fixed_width;
    } m_new_font_metadata;

    RefPtr<GUI::WizardPage> m_font_selection_page;
    RefPtr<GUI::ComboBox> m_select_font_combobox;
    RefPtr<GUI::Button> m_browse_button;

    RefPtr<GUI::WizardPage> m_font_properties_page;
    RefPtr<GUI::TextBox> m_name_textbox;
    RefPtr<GUI::TextBox> m_family_textbox;
    RefPtr<GUI::ComboBox> m_weight_combobox;
    RefPtr<GUI::ComboBox> m_slope_combobox;
    RefPtr<GUI::SpinBox> m_presentation_spinbox;

    RefPtr<GUI::WizardPage> m_glyph_properties_page;
    RefPtr<GUI::Widget> m_glyph_editor_container;
    RefPtr<GUI::SpinBox> m_glyph_height_spinbox;
    RefPtr<GUI::SpinBox> m_glyph_width_spinbox;
    RefPtr<GUI::SpinBox> m_baseline_spinbox;
    RefPtr<GUI::SpinBox> m_mean_line_spinbox;
    RefPtr<GUI::SpinBox> m_spacing_spinbox;
    RefPtr<GUI::CheckBox> m_fixed_width_checkbox;

    Vector<String> m_font_weight_list;
    Vector<String> m_font_slope_list;
};
