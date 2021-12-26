#include "GlyphEditorWidget.h"
#include <LibGUI/GPainter.h>

GlyphEditorWidget::GlyphEditorWidget(Font& mutable_font, GWidget* parent)
    : GFrame(parent)
    , m_font(mutable_font)
{
    set_frame_thickness(2);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_shape(FrameShape::Container);
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

void GlyphEditorWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), Color::White);
    painter.translate(frame_thickness(), frame_thickness());

    painter.translate(-1, -1);
    for (int y = 1; y < font().glyph_height(); ++y)
        painter.draw_line({ 0, y * m_scale }, { font().max_glyph_width() * m_scale, y * m_scale }, Color::Black);

    for (int x = 1; x < font().max_glyph_width(); ++x)
        painter.draw_line({ x * m_scale, 0 }, { x * m_scale, font().glyph_height() * m_scale }, Color::Black);

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
    return frame_thickness() * 2 + font().max_glyph_width() * m_scale - 1;
}

int GlyphEditorWidget::preferred_height() const
{
    return frame_thickness() * 2 + font().glyph_height() * m_scale - 1;
}
