#include "FontEditor.h"
#include "GlyphEditorWidget.h"
#include "GlyphMapWidget.h"
#include <LibGUI/GButton.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GSpinBox.h>
#include <LibGUI/GTextBox.h>
#include <stdlib.h>

FontEditorWidget::FontEditorWidget(const String& path, RefPtr<Font>&& edited_font, GWidget* parent)
    : GWidget(parent)
    , m_edited_font(move(edited_font))
{
    set_fill_with_background_color(true);

    if (path.is_empty())
        m_path = "/tmp/saved.font";
    else
        m_path = path;

    m_glyph_map_widget = new GlyphMapWidget(*m_edited_font, this);
    m_glyph_map_widget->move_to({ 90, 5 });

    m_glyph_editor_widget = new GlyphEditorWidget(*m_edited_font, this);
    m_glyph_editor_widget->move_to({ 5, 5 });

    auto* font_group_box = new GGroupBox("Font metadata", this);
    font_group_box->set_relative_rect(5, 195, 210, 70);

    m_name_textbox = new GTextBox(font_group_box);
    m_name_textbox->set_relative_rect(10, 20, 180, 20);
    m_name_textbox->set_text(m_edited_font->name());
    m_name_textbox->on_change = [this] {
        m_edited_font->set_name(m_name_textbox->text());
    };

    auto* fixed_width_checkbox = new GCheckBox(font_group_box);
    fixed_width_checkbox->set_relative_rect(10, 45, 190, 20);
    fixed_width_checkbox->set_text("Fixed width");
    fixed_width_checkbox->set_checked(m_edited_font->is_fixed_width());

    m_path_textbox = new GTextBox(this);
    m_path_textbox->set_relative_rect(5, 270, 210, 20);
    m_path_textbox->set_text(m_path);
    m_path_textbox->on_change = [this] {
        m_path = m_path_textbox->text();
    };

    auto* save_button = new GButton(this);
    save_button->set_text("Save");
    save_button->set_relative_rect({ 5, 300, 105, 20 });
    save_button->on_click = [this](GButton&) {
        dbgprintf("write to file: '%s'\n", m_path.characters());
        m_edited_font->write_to_file(m_path);
    };

    auto* quit_button = new GButton(this);
    quit_button->set_text("Quit");
    quit_button->set_relative_rect({ 110, 300, 105, 20 });
    quit_button->on_click = [](GButton&) {
        exit(0);
    };

    auto* info_label = new GLabel(this);
    info_label->set_text_alignment(TextAlignment::CenterLeft);
    info_label->set_relative_rect({ 5, 110, 100, 20 });

    auto* width_label = new GLabel("Glyph width:", this);
    width_label->set_text_alignment(TextAlignment::CenterLeft);
    width_label->set_relative_rect({ 5, 135, 100, 20 });

    auto* width_spinbox = new GSpinBox(this);
    width_spinbox->set_range(0, 32);
    width_spinbox->set_relative_rect({ 5, 155, m_glyph_editor_widget->preferred_width(), 20 });

    auto* demo_label_1 = new GLabel(this);
    demo_label_1->set_font(m_edited_font.copy_ref());
    demo_label_1->set_text("quick fox jumps nightly above wizard.");
    demo_label_1->set_relative_rect({ 110, 120, 300, 20 });

    auto* demo_label_2 = new GLabel(this);
    demo_label_2->set_font(m_edited_font.copy_ref());
    demo_label_2->set_text("QUICK FOX JUMPS NIGHTLY ABOVE WIZARD!");
    demo_label_2->set_relative_rect({ 110, 140, 300, 20 });

    auto update_demo = [demo_label_1, demo_label_2] {
        demo_label_1->update();
        demo_label_2->update();
    };

    m_glyph_editor_widget->on_glyph_altered = [this, update_demo](byte glyph) {
        m_glyph_map_widget->update_glyph(glyph);
        update_demo();
    };

    m_glyph_map_widget->on_glyph_selected = [this, info_label, width_spinbox](byte glyph) {
        m_glyph_editor_widget->set_glyph(glyph);
        width_spinbox->set_value(m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph()));
        info_label->set_text(String::format("0x%b (%c)", glyph, glyph));
    };

    fixed_width_checkbox->on_checked = [this, width_spinbox, update_demo](bool checked) {
        m_edited_font->set_fixed_width(checked);
        width_spinbox->set_value(m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph()));
        m_glyph_editor_widget->update();
        update_demo();
    };

    width_spinbox->on_change = [this, update_demo](int value) {
        m_edited_font->set_glyph_width(m_glyph_map_widget->selected_glyph(), value);
        m_glyph_editor_widget->update();
        m_glyph_map_widget->update_glyph(m_glyph_map_widget->selected_glyph());
        update_demo();
    };

    m_glyph_map_widget->set_selected_glyph('A');
}

FontEditorWidget::~FontEditorWidget()
{
}
