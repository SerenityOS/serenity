#include "GlyphEditorWidget.h"
#include <LibGUI/GPainter.h>

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
        on_glyph_altered(m_glyph);
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
