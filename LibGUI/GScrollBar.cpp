#include <LibGUI/GScrollBar.h>
#include <SharedGraphics/StylePainter.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <LibGUI/GPainter.h>

//#define GUTTER_DOES_PAGEUP_PAGEDOWN

static const char* s_up_arrow_bitmap_data = {
    "         "
    "    #    "
    "   ###   "
    "  #####  "
    " ####### "
    "   ###   "
    "   ###   "
    "   ###   "
    "         "
};

static const char* s_down_arrow_bitmap_data = {
    "         "
    "   ###   "
    "   ###   "
    "   ###   "
    " ####### "
    "  #####  "
    "   ###   "
    "    #    "
    "         "
};


static const char* s_left_arrow_bitmap_data = {
    "         "
    "    #    "
    "   ##    "
    "  ###### "
    " ####### "
    "  ###### "
    "   ##    "
    "    #    "
    "         "
};

static const char* s_right_arrow_bitmap_data = {
    "         "
    "    #    "
    "    ##   "
    " ######  "
    " ####### "
    " ######  "
    "    ##   "
    "    #    "
    "         "
};

static CharacterBitmap* s_up_arrow_bitmap;
static CharacterBitmap* s_down_arrow_bitmap;
static CharacterBitmap* s_left_arrow_bitmap;
static CharacterBitmap* s_right_arrow_bitmap;

GScrollBar::GScrollBar(Orientation orientation, GWidget* parent)
    : GWidget(parent)
    , m_orientation(orientation)
{
    if (!s_up_arrow_bitmap)
        s_up_arrow_bitmap = &CharacterBitmap::create_from_ascii(s_up_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_down_arrow_bitmap)
        s_down_arrow_bitmap = &CharacterBitmap::create_from_ascii(s_down_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_left_arrow_bitmap)
        s_left_arrow_bitmap = &CharacterBitmap::create_from_ascii(s_left_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_right_arrow_bitmap)
        s_right_arrow_bitmap = &CharacterBitmap::create_from_ascii(s_right_arrow_bitmap_data, 9, 9).leak_ref();

    if (m_orientation == Orientation::Vertical) {
        set_preferred_size({ 15, 0 });
    } else {
        set_preferred_size({ 0, 15 });
    }
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
    return { 0, 0, button_width(), button_height() };
}

Rect GScrollBar::down_button_rect() const
{
    if (orientation() == Orientation::Vertical)
        return { 0, height() - button_height(), button_width(), button_height() };
    else
        return { width() - button_width(), 0, button_width(), button_height() };
}

Rect GScrollBar::upper_gutter_rect() const
{
    if (orientation() == Orientation::Vertical)
        return { 0, button_height(), button_width(), scrubber_rect().top() - button_height() };
    else
        return { button_width(), 0, scrubber_rect().x() - button_width(), button_height() };
}

Rect GScrollBar::lower_gutter_rect() const
{
    auto scrubber_rect = this->scrubber_rect();
    if (orientation() == Orientation::Vertical)
        return { 0, scrubber_rect.bottom() + 1, button_width(), height() - button_height() - scrubber_rect.bottom() - 1};
    else
        return { scrubber_rect.right() + 1, 0, width() - button_width() - scrubber_rect.right() - 1, button_width() };
}

int GScrollBar::scrubbable_range_in_pixels() const
{
    if (orientation() == Orientation::Vertical)
        return height() - button_height() * 2 - scrubber_size();
    else
        return width() - button_width() * 2 - scrubber_size();
}

bool GScrollBar::has_scrubber() const
{
    return m_max != m_min;
}

int GScrollBar::scrubber_size() const
{
    int pixel_range = (orientation() == Orientation::Vertical ? height() : width()) - button_size() * 2;
    int value_range = m_max - m_min;
    return ::max(pixel_range - value_range, button_size());
}

Rect GScrollBar::scrubber_rect() const
{
    if (!has_scrubber())
        return { };
    float x_or_y;
    if (m_value == m_min)
        x_or_y = button_size();
    else if (m_value == m_max)
        x_or_y = ((orientation() == Orientation::Vertical ? height() : width()) - button_size() - scrubber_size()) + 1;
    else {
        float range_size = m_max - m_min;
        float available = scrubbable_range_in_pixels();
        float step = available / range_size;
        x_or_y = (button_size() + (step * m_value));
    }

    if (orientation() == Orientation::Vertical)
        return { 0, (int)x_or_y, button_width(), scrubber_size() };
    else
        return { (int)x_or_y, 0, scrubber_size(), button_height() };
}

void GScrollBar::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(rect(), Color::from_rgb(0xd6d2ce));

    StylePainter::paint_button(painter, up_button_rect(), ButtonStyle::Normal, false, m_hovered_component == Component::DecrementButton);
    painter.draw_bitmap(up_button_rect().location().translated(3, 3), orientation() == Orientation::Vertical ? *s_up_arrow_bitmap : *s_left_arrow_bitmap, has_scrubber() ? Color::Black : Color::MidGray);

    StylePainter::paint_button(painter, down_button_rect(), ButtonStyle::Normal, false, m_hovered_component == Component::IncrementButton);
    painter.draw_bitmap(down_button_rect().location().translated(3, 3), orientation() == Orientation::Vertical ? *s_down_arrow_bitmap : *s_right_arrow_bitmap, has_scrubber() ? Color::Black : Color::MidGray);

    if (has_scrubber())
        StylePainter::paint_button(painter, scrubber_rect(), ButtonStyle::Normal, false, m_hovered_component == Component::Scrubber);
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
#ifdef GUTTER_DOES_PAGEUP_PAGEDOWN
    if (has_scrubber() && upper_gutter_rect().contains(event.position())) {
        set_value(value() - m_big_step);
        return;
    }
    if (has_scrubber() && lower_gutter_rect().contains(event.position())) {
        set_value(value() + m_big_step);
        return;
    }
#endif
    if (has_scrubber() && scrubber_rect().contains(event.position())) {
        m_scrubbing = true;
        m_scrub_start_value = value();
        m_scrub_origin = event.position();
        update();
        return;
    }
#ifndef GUTTER_DOES_PAGEUP_PAGEDOWN
    if (has_scrubber()) {
        float range_size = m_max - m_min;
        float available = scrubbable_range_in_pixels();

        float x = ::max(0, event.position().x() - button_width() - button_width() / 2);
        float y = ::max(0, event.position().y() - button_height() - button_height() / 2);

        float rel_x = x / available;
        float rel_y = y / available;

        if (orientation() == Orientation::Vertical)
            set_value(m_min + rel_y * range_size);
        else
            set_value(m_min + rel_x * range_size);

        m_scrubbing = true;
        m_scrub_start_value = value();
        m_scrub_origin = event.position();
    }
#endif
}

void GScrollBar::mouseup_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    if (!m_scrubbing)
        return;
    m_scrubbing = false;
    update();
}

void GScrollBar::mousemove_event(GMouseEvent& event)
{
    auto old_hovered_component = m_hovered_component;
    if (scrubber_rect().contains(event.position()))
        m_hovered_component = Component::Scrubber;
    else if (up_button_rect().contains(event.position()))
        m_hovered_component = Component::DecrementButton;
    else if (down_button_rect().contains(event.position()))
        m_hovered_component = Component::IncrementButton;
    else if (rect().contains(event.position()))
        m_hovered_component = Component::Gutter;
    else
        m_hovered_component = Component::Invalid;
    if (old_hovered_component != m_hovered_component)
        update();
    if (!m_scrubbing)
        return;
    float delta = orientation() == Orientation::Vertical ? (event.y() - m_scrub_origin.y()) : (event.x() - m_scrub_origin.x());
    float scrubbable_range = scrubbable_range_in_pixels();
    float value_steps_per_scrubbed_pixel = (m_max - m_min) / scrubbable_range;
    float new_value = m_scrub_start_value + (value_steps_per_scrubbed_pixel * delta);
    set_value(new_value);
}

void GScrollBar::leave_event(CEvent&)
{
    if (m_hovered_component != Component::Invalid) {
        m_hovered_component = Component::Invalid;
        update();
    }
}
