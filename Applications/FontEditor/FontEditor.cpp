#include "FontEditor.h"
#include "GlyphMapWidget.h"
#include "GlyphEditorWidget.h"
#include <LibGUI/GPainter.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GCheckBox.h>

FontEditorWidget::FontEditorWidget(const String& path, RetainPtr<Font>&& edited_font, GWidget* parent)
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

    auto* fixed_width_checkbox = new GCheckBox(this);
    fixed_width_checkbox->set_caption("Fixed width");
    fixed_width_checkbox->set_checked(m_edited_font->is_fixed_width());
    fixed_width_checkbox->set_relative_rect({ 5, 195, 100, 20 });

    m_name_textbox = new GTextBox(this);
    m_name_textbox->set_relative_rect({ 5, 220, 300, 20 });
    m_name_textbox->set_text(m_edited_font->name());
    m_name_textbox->on_change = [this] (GTextBox&) {
        m_edited_font->set_name(m_name_textbox->text());
    };

    m_path_textbox = new GTextBox(this);
    m_path_textbox->set_relative_rect({ 5, 245, 300, 20 });
    m_path_textbox->set_text(m_path);
    m_path_textbox->on_change = [this] (GTextBox&) {
        m_path = m_path_textbox->text();
    };

    auto* save_button = new GButton(this);
    save_button->set_caption("Save");
    save_button->set_relative_rect({ 5, 270, 100, 20 });
    save_button->on_click = [this] (GButton&) {
        dbgprintf("write to file: '%s'\n", m_path.characters());
        m_edited_font->write_to_file(m_path);
    };

    auto* quit_button = new GButton(this);
    quit_button->set_caption("Quit");
    quit_button->set_relative_rect({ 110, 270, 100, 20 });
    quit_button->on_click = [] (GButton&) {
        exit(0);
    };

    auto* info_label = new GLabel(this);
    info_label->set_text_alignment(TextAlignment::CenterLeft);
    info_label->set_relative_rect({ 5, 110, 100, 20 });

    auto* width_textbox = new GTextBox(this);
    width_textbox->set_relative_rect({ 5, 135, 100, 20 });

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

    m_glyph_editor_widget->on_glyph_altered = [this, update_demo] {
        m_glyph_map_widget->update();
        update_demo();
    };

    m_glyph_map_widget->on_glyph_selected = [this, info_label, width_textbox] (byte glyph) {
        m_glyph_editor_widget->set_glyph(glyph);
        width_textbox->set_text(String::format("%u", m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph())));
        info_label->set_text(String::format("0x%b (%c)", glyph, glyph));
    };

    fixed_width_checkbox->on_change = [this, width_textbox, update_demo] (GCheckBox&, bool is_checked) {
        m_edited_font->set_fixed_width(is_checked);
        width_textbox->set_text(String::format("%u", m_edited_font->glyph_width(m_glyph_map_widget->selected_glyph())));
        m_glyph_editor_widget->update();
        update_demo();
    };

    width_textbox->on_change = [this, update_demo] (GTextBox& textbox) {
        bool ok;
        unsigned width = textbox.text().to_uint(ok);
        if (ok) {
            m_edited_font->set_glyph_width(m_glyph_map_widget->selected_glyph(), width);
            m_glyph_editor_widget->update();
            update_demo();
        }
    };

    m_glyph_map_widget->set_selected_glyph('A');
}

FontEditorWidget::~FontEditorWidget()
{
}
