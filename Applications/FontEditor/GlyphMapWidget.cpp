#include "GlyphMapWidget.h"
#include <LibGUI/GPainter.h>

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
