#include "FontEditor.h"
#include <SharedGraphics/Painter.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>

FontEditorWidget::FontEditorWidget(GWidget* parent)
    : GWidget(parent)
{
    m_edited_font = Font::load_from_file("/saved.font");
    if (!m_edited_font)
        m_edited_font = Font::default_font().clone();

    m_glyph_map_widget = new GlyphMapWidget(*m_edited_font, this);
    m_glyph_map_widget->move_to({ 90, 5 });

    m_glyph_editor_widget = new GlyphEditorWidget(*m_edited_font, this);
    m_glyph_editor_widget->move_to({ 5, 5 });

    auto* save_button = new GButton(this);
    save_button->set_caption("Save");
    save_button->set_relative_rect({ 5, 135, 140, 20 });
    save_button->on_click = [this] (GButton&) {
        m_edited_font->write_to_file("/saved.font");
    };

    auto* label = new GLabel(this);
    label->set_relative_rect({ 5, 110, 140, 20 });

    m_glyph_editor_widget->on_glyph_altered = [this] {
        m_glyph_map_widget->update();
    };

    m_glyph_map_widget->on_glyph_selected = [this, label] (byte glyph) {
        m_glyph_editor_widget->set_glyph(glyph);
        label->set_text(String::format("Glyph: 0x%b (%c)", glyph, glyph));
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
    return columns() * (font().glyph_width() + m_horizontal_spacing);
}

int GlyphMapWidget::preferred_height() const
{
    return rows() * (font().glyph_height() + m_vertical_spacing);
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
        column * (font().glyph_width() + m_horizontal_spacing),
        row * (font().glyph_height() + m_vertical_spacing),
        font().glyph_width() + m_horizontal_spacing,
        font().glyph_height() + m_horizontal_spacing
    };
}

void GlyphMapWidget::paint_event(GPaintEvent&)
{
    Painter painter(*this);
    painter.set_font(font());
    painter.fill_rect(rect(), Color::White);

    byte glyph = 0;

    for (int row = 0; row < rows(); ++row) {
        for (int column = 0; column < columns(); ++column, ++glyph) {
            Rect outer_rect = get_outer_rect(glyph);
            Rect inner_rect(
                outer_rect.x() + m_horizontal_spacing / 2,
                outer_rect.y() + m_vertical_spacing / 2,
                font().glyph_width(),
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
    Painter painter(*this);
    painter.fill_rect(rect(), Color::White);
    painter.draw_rect(rect(), Color::Black);

    auto& bitmap = font().glyph_bitmap(m_glyph);

    for (int y = 0; y < font().glyph_height(); ++y)
        painter.draw_line({ 0, y * m_scale }, { font().glyph_width() * m_scale - 1, y * m_scale }, Color::Black);

    for (int x = 0; x < font().glyph_width(); ++x)
        painter.draw_line({ x * m_scale, 0 }, { x * m_scale, font().glyph_height() * m_scale - 1 }, Color::Black);

    for (int y = 0; y < font().glyph_height(); ++y) {
        for (int x = 0; x < font().glyph_width(); ++x) {
            Rect rect { x * m_scale, y * m_scale, m_scale, m_scale };
            if (bitmap.bit_at(x, y))
                painter.fill_rect(rect, Color::Black);
        }
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
    byte new_bit = set ? '#' : ' ';
    int x = event.x() / m_scale;
    int y = event.y() / m_scale;
    auto& bitmap = font().glyph_bitmap(m_glyph);
    auto* mutable_bits = const_cast<char*>(bitmap.bits());
    ASSERT((unsigned)x < bitmap.width());
    ASSERT((unsigned)y < bitmap.height());
    auto& bit = mutable_bits[y * bitmap.width() + x];
    if (bit == new_bit)
        return;
    bit = new_bit;
    if (on_glyph_altered)
        on_glyph_altered();
    update();
}

int GlyphEditorWidget::preferred_width() const
{
    return font().glyph_width() * m_scale;
}

int GlyphEditorWidget::preferred_height() const
{
    return font().glyph_height() * m_scale;
}
