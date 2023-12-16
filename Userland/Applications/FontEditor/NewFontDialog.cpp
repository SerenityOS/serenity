/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NewFontDialog.h"
#include <AK/StringBuilder.h>
#include <Applications/FontEditor/NewFontDialogPage1GML.h>
#include <Applications/FontEditor/NewFontDialogPage2GML.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Wizards/WizardDialog.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/FontStyleMapping.h>
#include <LibGfx/Palette.h>

namespace FontEditor {

class GlyphPreviewWidget final : public GUI::Frame {
    C_OBJECT(GlyphPreviewWidget)
public:
    void set_preview_size(int width, int height)
    {
        m_width = width;
        m_height = height;
        for (int i = 10; i > 0; i--) {
            if ((frame_thickness() * 2 + (m_width * i) - 1) <= 250
                && (frame_thickness() * 2 + (m_height * i) - 1) <= 205) {
                set_scale(i);
                break;
            }
        }
        set_fixed_width(frame_thickness() * 2 + (m_width * m_scale) - 1);
        set_fixed_height(frame_thickness() * 2 + (m_height * m_scale) - 1);
    }

    void set_scale(int scale) { m_scale = scale; }
    void set_baseline(int i) { m_baseline = i; }
    void set_mean_line(int i) { m_mean_line = i; }

private:
    GlyphPreviewWidget()
    {
        set_preview_size(m_width, m_height);
    }
    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Frame::paint_event(event);
        GUI::Painter painter(*this);
        painter.add_clip_rect(frame_inner_rect());
        painter.add_clip_rect(event.rect());
        painter.fill_rect(frame_inner_rect(), palette().base());
        painter.translate(frame_thickness(), frame_thickness());

        painter.translate(-1, -1);
        for (int y = 1; y < m_height; ++y) {
            int y_below = y - 1;
            bool bold_line = y_below == m_baseline || y_below == m_mean_line;
            painter.draw_line({ 0, y * m_scale }, { m_width * m_scale, y * m_scale }, palette().threed_shadow2(), bold_line ? 2 : 1);
        }

        for (int x = 1; x < m_width; ++x)
            painter.draw_line({ x * m_scale, 0 }, { x * m_scale, m_height * m_scale }, palette().threed_shadow2());

        for (int y = 0; y < m_height; ++y) {
            for (int x = 0; x < m_width; ++x) {
                Gfx::IntRect rect { x * m_scale, y * m_scale, m_scale, m_scale };
                if (x >= m_width) {
                    painter.fill_rect(rect, palette().threed_shadow1());
                } else {
                    if (m_bits[x][y])
                        painter.fill_rect(rect, palette().base_text());
                }
            }
        }
    }
    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        draw_at_mouse(event);
    }
    virtual void mousemove_event(GUI::MouseEvent& event) override
    {
        if (event.buttons() & (GUI::MouseButton::Primary | GUI::MouseButton::Secondary))
            draw_at_mouse(event);
    }
    void draw_at_mouse(GUI::MouseEvent const& event)
    {
        bool set = event.buttons() & GUI::MouseButton::Primary;
        bool unset = event.buttons() & GUI::MouseButton::Secondary;
        if (!(set ^ unset))
            return;
        int x = (event.x() - 1) / m_scale;
        int y = (event.y() - 1) / m_scale;
        if (x < 0 || x >= m_width)
            return;
        if (y < 0 || y >= m_height)
            return;
        if (m_bits[x][y] == set)
            return;
        m_bits[x][y] = set;
        update();
    }

    int m_scale { 10 };
    int m_width { 20 };
    int m_height { 20 };
    int m_mean_line { 2 };
    int m_baseline { 16 };
    u8 m_bits[Gfx::GlyphBitmap::max_width()][Gfx::GlyphBitmap::max_height()] {};
};

}

REGISTER_WIDGET(FontEditor, GlyphPreviewWidget);

ErrorOr<NonnullRefPtr<NewFontDialog>> NewFontDialog::create(GUI::Window* parent_window)
{
    auto dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) NewFontDialog(parent_window)));
    TRY(dialog->build());
    return dialog;
}

ErrorOr<void> NewFontDialog::build()
{
    TRY(GUI::WizardDialog::build());

    m_font_properties_page = TRY(GUI::WizardPage::create("Typeface properties"sv, "Edit details about this font."sv));
    TRY(m_font_properties_page->body_widget().load_from_gml(new_font_dialog_page_1_gml));

    m_name_textbox = m_font_properties_page->body_widget().find_descendant_of_type_named<GUI::TextBox>("name_textbox");
    m_family_textbox = m_font_properties_page->body_widget().find_descendant_of_type_named<GUI::TextBox>("family_textbox");
    m_weight_combobox = m_font_properties_page->body_widget().find_descendant_of_type_named<GUI::ComboBox>("weight_combobox");
    m_slope_combobox = m_font_properties_page->body_widget().find_descendant_of_type_named<GUI::ComboBox>("slope_combobox");
    m_presentation_spinbox = m_font_properties_page->body_widget().find_descendant_of_type_named<GUI::SpinBox>("presentation_spinbox");

    TRY(m_font_weight_list.try_ensure_capacity(Gfx::font_weight_names.size()));
    for (auto& it : Gfx::font_weight_names)
        m_font_weight_list.unchecked_append(TRY(String::from_utf8(it.name)));
    m_weight_combobox->set_model(GUI::ItemListModel<String>::create(m_font_weight_list));
    m_weight_combobox->set_selected_index(3);

    TRY(m_font_slope_list.try_ensure_capacity(Gfx::font_slope_names.size()));
    for (auto& it : Gfx::font_slope_names)
        m_font_slope_list.unchecked_append(TRY(String::from_utf8(it.name)));
    m_slope_combobox->set_model(GUI::ItemListModel<String>::create(m_font_slope_list));
    m_slope_combobox->set_selected_index(0);

    m_presentation_spinbox->set_value(12);

    m_font_properties_page->on_page_enter = [&]() {
        m_name_textbox->set_focus(true);
    };
    m_font_properties_page->on_next_page = [&]() {
        return m_glyph_properties_page;
    };

    m_glyph_properties_page = TRY(GUI::WizardPage::create("Glyph properties"sv, "Edit details about this font."sv));
    TRY(m_glyph_properties_page->body_widget().load_from_gml(new_font_dialog_page_2_gml));
    m_glyph_properties_page->set_is_final_page(true);

    m_glyph_height_spinbox = m_glyph_properties_page->body_widget().find_descendant_of_type_named<GUI::SpinBox>("height_spinbox");
    m_glyph_width_spinbox = m_glyph_properties_page->body_widget().find_descendant_of_type_named<GUI::SpinBox>("width_spinbox");
    m_baseline_spinbox = m_glyph_properties_page->body_widget().find_descendant_of_type_named<GUI::SpinBox>("baseline_spinbox");
    m_mean_line_spinbox = m_glyph_properties_page->body_widget().find_descendant_of_type_named<GUI::SpinBox>("mean_line_spinbox");
    m_spacing_spinbox = m_glyph_properties_page->body_widget().find_descendant_of_type_named<GUI::SpinBox>("spacing_spinbox");
    m_fixed_width_checkbox = m_glyph_properties_page->body_widget().find_descendant_of_type_named<GUI::CheckBox>("fixed_width_checkbox");

    m_glyph_height_spinbox->set_value(20);
    m_glyph_width_spinbox->set_value(20);
    m_glyph_height_spinbox->set_max(Gfx::GlyphBitmap::max_height());
    m_glyph_width_spinbox->set_max(Gfx::GlyphBitmap::max_width());
    m_mean_line_spinbox->set_value(2);
    m_baseline_spinbox->set_value(16);
    m_mean_line_spinbox->set_max(max(m_glyph_height_spinbox->value() - 2, 0));
    m_baseline_spinbox->set_max(max(m_glyph_height_spinbox->value() - 2, 0));
    m_spacing_spinbox->set_value(1);
    m_fixed_width_checkbox->set_checked(false);

    auto& preview_editor = *m_glyph_properties_page->body_widget().find_descendant_of_type_named<FontEditor::GlyphPreviewWidget>("glyph_preview_widget");

    m_glyph_width_spinbox->on_change = [&](int value) {
        preview_editor.set_preview_size(value, m_glyph_height_spinbox->value());
    };
    m_glyph_height_spinbox->on_change = [&](int value) {
        preview_editor.set_preview_size(m_glyph_width_spinbox->value(), value);
        m_mean_line_spinbox->set_max(max(value - 2, 0));
        m_baseline_spinbox->set_max(max(value - 2, 0));
    };
    m_baseline_spinbox->on_change = [&](int value) {
        preview_editor.set_baseline(value);
        preview_editor.update();
    };
    m_mean_line_spinbox->on_change = [&](int value) {
        preview_editor.set_mean_line(value);
        preview_editor.update();
    };

    push_page(*m_font_properties_page);

    return {};
}

NewFontDialog::NewFontDialog(GUI::Window* parent_window)
    : GUI::WizardDialog(parent_window)
{
    set_title("New Font");
    set_icon(parent_window->icon());
}

void NewFontDialog::save_metadata()
{
    m_new_font_metadata.name = MUST(String::from_byte_string(m_name_textbox->text()));
    m_new_font_metadata.family = MUST(String::from_byte_string(m_family_textbox->text()));
    m_new_font_metadata.weight = Gfx::name_to_weight(m_weight_combobox->text());
    m_new_font_metadata.slope = Gfx::name_to_slope(m_slope_combobox->text());
    m_new_font_metadata.presentation_size = m_presentation_spinbox->value();

    m_new_font_metadata.baseline = m_baseline_spinbox->value();
    m_new_font_metadata.mean_line = m_mean_line_spinbox->value();
    m_new_font_metadata.glyph_height = m_glyph_height_spinbox->value();
    m_new_font_metadata.glyph_width = m_glyph_width_spinbox->value();
    m_new_font_metadata.glyph_spacing = m_spacing_spinbox->value();
    m_new_font_metadata.is_fixed_width = m_fixed_width_checkbox->is_checked();
}

ErrorOr<NonnullRefPtr<Gfx::BitmapFont>> NewFontDialog::create_font()
{
    save_metadata();

    auto font = TRY(Gfx::BitmapFont::create(m_new_font_metadata.glyph_height, m_new_font_metadata.glyph_width, m_new_font_metadata.is_fixed_width, 0x110000));
    font->set_name(m_new_font_metadata.name);
    font->set_family(m_new_font_metadata.family);
    font->set_presentation_size(m_new_font_metadata.presentation_size);
    font->set_weight(m_new_font_metadata.weight);
    font->set_slope(m_new_font_metadata.slope);
    font->set_baseline(m_new_font_metadata.baseline);
    font->set_mean_line(m_new_font_metadata.mean_line);
    font->set_glyph_spacing(m_new_font_metadata.glyph_spacing);

    return font;
}
