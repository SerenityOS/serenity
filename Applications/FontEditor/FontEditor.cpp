#include "FontEditor.h"
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

GlyphMapWidget::GlyphMapWidget(Font& mutable_font, GWidget* parent)
    : GWidget(parent)
    , m_font(mutable_font)
{
    set_relative_rect({ 0, 0, preferred_width(), preferred_height() });
}

GlyphMapWidget::~GlyphMapWidget()
{
}

int GlyphMapWidget::preferred_width() const
{
    return columns() * (font().max_glyph_width() + m_horizontal_spacing) + 2;
}

int GlyphMapWidget::preferred_height() const
{
    return rows() * (font().glyph_height() + m_vertical_spacing) + 2;
}

void GlyphMapWidget::set_selected_glyph(byte glyph)
{
    if (m_selected_glyph == glyph)
        return;
    m_selected_glyph = glyph;
    if (on_glyph_selected)
        on_glyph_selected(glyph);
    update();
}

Rect GlyphMapWidget::get_outer_rect(byte glyph) const
{
    int row = glyph / columns();
    int column = glyph % columns();
    return {
        column * (font().max_glyph_width() + m_horizontal_spacing) + 1,
        row * (font().glyph_height() + m_vertical_spacing) + 1,
        font().max_glyph_width() + m_horizontal_spacing,
        font().glyph_height() + m_horizontal_spacing
    };
}

void GlyphMapWidget::paint_event(GPaintEvent&)
{
    GPainter painter(*this);
    painter.set_font(font());
    painter.fill_rect(rect(), Color::White);
    painter.draw_rect(rect(), Color::Black);

    byte glyph = 0;

    for (int row = 0; row < rows(); ++row) {
        for (int column = 0; column < columns(); ++column, ++glyph) {
            Rect outer_rect = get_outer_rect(glyph);
            Rect inner_rect(
                outer_rect.x() + m_horizontal_spacing / 2,
                outer_rect.y() + m_vertical_spacing / 2,
                font().max_glyph_width(),
                font().glyph_height()
            );
            if (glyph == m_selected_glyph) {
                painter.fill_rect(outer_rect, Color::Red);
                painter.draw_glyph(inner_rect.location(), glyph, Color::White);
            } else {
                painter.draw_glyph(inner_rect.location(), glyph, Color::Black);
            }
        }
    }

    if (is_focused())
        painter.draw_focus_rect(rect());
}

void GlyphMapWidget::mousedown_event(GMouseEvent& event)
{
    // FIXME: This is a silly loop.
    for (unsigned glyph = 0; glyph < 256; ++glyph) {
        if (get_outer_rect(glyph).contains(event.position())) {
            set_selected_glyph(glyph);
            break;
        }
    }
}

GlyphEditorWidget::GlyphEditorWidget(Font& mutable_font, GWidget* parent)
    : GWidget(parent)
    , m_font(mutable_font)
{
    set_relative_rect({ 0, 0, preferred_width(), preferred_height() });
}

GlyphEditorWidget::~GlyphEditorWidget()
{
}

void GlyphEditorWidget::set_glyph(byte glyph)
{
    if (m_glyph == glyph)
        return;
    m_glyph = glyph;
    update();
}

void GlyphEditorWidget::paint_event(GPaintEvent&)
{
    GPainter painter(*this);
    painter.fill_rect(rect(), Color::White);
    painter.draw_rect(rect(), Color::Black);

    for (int y = 0; y < font().glyph_height(); ++y)
        painter.draw_line({ 0, y * m_scale }, { font().max_glyph_width() * m_scale, y * m_scale }, Color::Black);

    for (int x = 0; x < font().max_glyph_width(); ++x)
        painter.draw_line({ x * m_scale, 0 }, { x * m_scale, font().glyph_height() * m_scale }, Color::Black);

    painter.translate(1, 1);

    auto bitmap = font().glyph_bitmap(m_glyph);

    for (int y = 0; y < font().glyph_height(); ++y) {
        for (int x = 0; x < font().max_glyph_width(); ++x) {
            Rect rect { x * m_scale, y * m_scale, m_scale, m_scale };
            if (x >= font().glyph_width(m_glyph)) {
                painter.fill_rect(rect, Color::MidGray);
            } else {
                if (bitmap.bit_at(x, y))
                    painter.fill_rect(rect, Color::Black);
            }
        }
    }

    if (is_focused()) {
        painter.translate(-1, -1);
        painter.draw_focus_rect(rect());
    }
}

void GlyphEditorWidget::mousedown_event(GMouseEvent& event)
{
    draw_at_mouse(event);
}

void GlyphEditorWidget::mousemove_event(GMouseEvent& event)
{
    if (event.buttons() & (GMouseButton::Left | GMouseButton::Right))
        draw_at_mouse(event);
}

void GlyphEditorWidget::draw_at_mouse(const GMouseEvent& event)
{
    bool set = event.buttons() & GMouseButton::Left;
    bool unset = event.buttons() & GMouseButton::Right;
    if (!(set ^ unset))
        return;
    int x = (event.x() - 1) / m_scale;
    int y = (event.y() - 1) / m_scale;
    auto bitmap = font().glyph_bitmap(m_glyph);
    if (x >= bitmap.width())
        return;
    if (y >= bitmap.height())
        return;
    if (bitmap.bit_at(x, y) == set)
        return;
    bitmap.set_bit_at(x, y, set);
    if (on_glyph_altered)
        on_glyph_altered();
    update();
}

int GlyphEditorWidget::preferred_width() const
{
    return font().max_glyph_width() * m_scale + 1;
}

int GlyphEditorWidget::preferred_height() const
{
    return font().glyph_height() * m_scale + 1;
}
