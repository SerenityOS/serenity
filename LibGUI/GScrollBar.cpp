#include <LibGUI/GScrollBar.h>
#include <LibGUI/GStyle.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>

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
        s_up_arrow_bitmap = CharacterBitmap::create_from_ascii(s_up_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_down_arrow_bitmap)
        s_down_arrow_bitmap = CharacterBitmap::create_from_ascii(s_down_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_left_arrow_bitmap)
        s_left_arrow_bitmap = CharacterBitmap::create_from_ascii(s_left_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_right_arrow_bitmap)
        s_right_arrow_bitmap = CharacterBitmap::create_from_ascii(s_right_arrow_bitmap_data, 9, 9).leak_ref();

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
    return { 0, 0, button_size(), button_size() };
}

Rect GScrollBar::down_button_rect() const
{
    if (orientation() == Orientation::Vertical)
        return { 0, height() - button_size(), button_size(), button_size() };
    else
        return { width() - button_size(), 0, button_size(), button_size() };
}

Rect GScrollBar::upper_gutter_rect() const
{
    if (orientation() == Orientation::Vertical)
        return { 0, button_size(), button_size(), scrubber_rect().top() - button_size() };
    else
        return { button_size(), 0, scrubber_rect().x() - button_size(), button_size() };
}

Rect GScrollBar::lower_gutter_rect() const
{
    auto scrubber_rect = this->scrubber_rect();
    if (orientation() == Orientation::Vertical)
        return { 0, scrubber_rect.bottom() + 1, button_size(), height() - button_size() - scrubber_rect.bottom() - 1};
    else
        return { scrubber_rect.right() + 1, 0, width() - button_size() - scrubber_rect.right() - 1, button_size() };
}

int GScrollBar::scrubbable_range_in_pixels() const
{
    if (orientation() == Orientation::Vertical)
        return height() - button_size() * 3;
    else
        return width() - button_size() * 3;
}

bool GScrollBar::has_scrubber() const
{
    return m_max != m_min;
}

Rect GScrollBar::scrubber_rect() const
{
    if (!has_scrubber())
        return { };
    float x_or_y;
    if (m_value == m_min)
        x_or_y = button_size();
    else if (m_value == m_max)
        x_or_y = (orientation() == Orientation::Vertical ? height() : width()) - (button_size() * 2);
    else {
        float range_size = m_max - m_min;
        float available = scrubbable_range_in_pixels();
        float step = available / range_size;
        x_or_y = (button_size() + (step * m_value));
    }

    if (orientation() == Orientation::Vertical)
        return { 0, (int)x_or_y, button_size(), button_size() };
    else
        return { (int)x_or_y, 0, button_size(), button_size() };
}

void GScrollBar::paint_event(GPaintEvent&)
{
    Painter painter(*this);

    painter.fill_rect(rect(), Color(164, 164, 164));

    GStyle::the().paint_button(painter, up_button_rect(), false);
    painter.draw_bitmap(up_button_rect().location().translated(3, 3), orientation() == Orientation::Vertical ? *s_up_arrow_bitmap : *s_left_arrow_bitmap, has_scrubber() ? Color::Black : Color::MidGray);

    GStyle::the().paint_button(painter, down_button_rect(), false);
    painter.draw_bitmap(down_button_rect().location().translated(3, 3), orientation() == Orientation::Vertical ? *s_down_arrow_bitmap : *s_right_arrow_bitmap, has_scrubber() ? Color::Black : Color::MidGray);

    if (has_scrubber())
        GStyle::the().paint_button(painter, scrubber_rect(), m_scrubbing);
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
    if (has_scrubber() && upper_gutter_rect().contains(event.position())) {
        set_value(value() - m_big_step);
        return;
    }
    if (has_scrubber() && lower_gutter_rect().contains(event.position())) {
        set_value(value() + m_big_step);
        return;
    }
    if (has_scrubber() && scrubber_rect().contains(event.position())) {
        m_scrubbing = true;
        m_scrub_start_value = value();
        m_scrub_origin = event.position();
        set_global_cursor_tracking(true);
        update();
        return;
    }
}

void GScrollBar::mouseup_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    if (!m_scrubbing)
        return;
    m_scrubbing = false;
    set_global_cursor_tracking(false);
    update();
}

void GScrollBar::mousemove_event(GMouseEvent& event)
{
    if (!m_scrubbing)
        return;
    float delta = orientation() == Orientation::Vertical ? (event.y() - m_scrub_origin.y()) : (event.x() - m_scrub_origin.x());
    float scrubbable_range = scrubbable_range_in_pixels();
    float value_steps_per_scrubbed_pixel = (m_max - m_min) / scrubbable_range;
    float new_value = m_scrub_start_value + (value_steps_per_scrubbed_pixel * delta);
    set_value(new_value);
}
