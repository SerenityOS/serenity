#include <LibGUI/GScrollBar.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>

GScrollBar::GScrollBar(GWidget* parent)
    : GWidget(parent)
{
}

GScrollBar::~GScrollBar()
{
}

void GScrollBar::set_range(int min, int max)
{
    ASSERT(min <= max);
    if (m_min == min && m_max == max)
        return;

    m_min = min;
    m_max = max;

    int old_value = m_value;
    if (m_value < m_min)
        m_value = m_min;
    if (m_value > m_max)
        m_value = m_max;
    if (on_change && m_value != old_value)
        on_change(m_value);

    update();
}

void GScrollBar::set_value(int value)
{
    if (value < m_min)
        value = m_min;
    if (value > m_max)
        value = m_max;
    if (value == m_value)
        return;
    m_value = value;
    if (on_change)
        on_change(value);
    update();
}

Rect GScrollBar::up_button_rect() const
{
    return { 0, 0, button_size(), button_size() };
}

Rect GScrollBar::down_button_rect() const
{
    return { 0, height() - button_size(), button_size(), button_size() };
}

Rect GScrollBar::pgup_rect() const
{
    return { 0, button_size(), button_size(), scrubber_rect().top() - button_size() };
}

Rect GScrollBar::pgdn_rect() const
{
    auto scrubber_rect = this->scrubber_rect();
    return { 0, scrubber_rect.bottom(), button_size(), height() - button_size() - scrubber_rect.bottom() };
}

Rect GScrollBar::scrubber_rect() const
{
    int range_size = m_max - m_min;
    if (range_size == 0)
        return { 0, button_size(), button_size(), height() - button_size() * 2 };
    float available_y = height() - button_size() * 3;
    float y_step = available_y / range_size;
    float y = button_size() + (y_step * m_value);
    return { 0, (int)y, button_size(), button_size() };
}

static const char* s_up_arrow_bitmap_data = {
    "          "
    "          "
    "    ##    "
    "   ####   "
    "  ######  "
    " ######## "
    "    ##    "
    "    ##    "
    "    ##    "
    "          "
};

static const char* s_down_arrow_bitmap_data = {
    "          "
    "    ##    "
    "    ##    "
    "    ##    "
    " ######## "
    "  ######  "
    "   ####   "
    "    ##    "
    "          "
    "          "
};

static CharacterBitmap* s_up_arrow_bitmap;
static CharacterBitmap* s_down_arrow_bitmap;

void GScrollBar::paint_event(GPaintEvent&)
{
    if (!s_up_arrow_bitmap)
        s_up_arrow_bitmap = CharacterBitmap::create_from_ascii(s_up_arrow_bitmap_data, 10, 10).leak_ref();
    if (!s_down_arrow_bitmap)
        s_down_arrow_bitmap = CharacterBitmap::create_from_ascii(s_down_arrow_bitmap_data, 10, 10).leak_ref();

    Painter painter(*this);

    painter.fill_rect(rect(), Color::MidGray);

    painter.draw_rect(up_button_rect(), Color::DarkGray, true);
    painter.fill_rect_with_gradient(up_button_rect().shrunken(2, 2), Color::LightGray, Color::White);
    painter.draw_bitmap(up_button_rect().location().translated(3, 3), *s_up_arrow_bitmap, Color::Black);

    painter.draw_rect(down_button_rect(), Color::DarkGray, true);
    painter.fill_rect_with_gradient(down_button_rect().shrunken(2, 2), Color::LightGray, Color::White);
    painter.draw_bitmap(down_button_rect().location().translated(3, 3), *s_down_arrow_bitmap, Color::Black);

    painter.draw_rect(scrubber_rect(), Color::White, true);
    painter.fill_rect_with_gradient(scrubber_rect().shrunken(2, 2), Color::LightGray, Color::White);
}

void GScrollBar::mousedown_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    if (up_button_rect().contains(event.position())) {
        set_value(value() - m_step);
        return;
    }
    if (down_button_rect().contains(event.position())) {
        set_value(value() + m_step);
        return;
    }
    if (pgup_rect().contains(event.position())) {
        set_value(value() - m_big_step);
        return;
    }
    if (pgdn_rect().contains(event.position())) {
        set_value(value() + m_big_step);
        return;
    }
}
