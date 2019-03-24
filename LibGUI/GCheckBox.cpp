#include "GCheckBox.h"
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/CharacterBitmap.h>
#include <Kernel/KeyCode.h>

//#define GCHECKBOX_DEBUG

static const char* s_checked_bitmap_data = {
    "         "
    "      ## "
    "     ##  "
    "     ##  "
    "    ##   "
    " ## ##   "
    "  ####   "
    "   ##    "
    "         "
};

static CharacterBitmap* s_checked_bitmap;
static const int s_checked_bitmap_width = 9;
static const int s_checked_bitmap_height = 9;
static const int s_box_width = 11;
static const int s_box_height = 11;

GCheckBox::GCheckBox(GWidget* parent)
    : GWidget(parent)
{
    if (!s_checked_bitmap)
        s_checked_bitmap = &CharacterBitmap::create_from_ascii(s_checked_bitmap_data, s_checked_bitmap_width, s_checked_bitmap_height).leak_ref();
}

GCheckBox::~GCheckBox()
{
}

void GCheckBox::set_caption(String&& caption)
{
    if (caption == m_caption)
        return;
    m_caption = move(caption);
    update();
}

void GCheckBox::set_checked(bool b)
{
    if (m_checked == b)
        return;
    m_checked = b;
    if (on_change)
        on_change(*this, b);
    update();
}

void GCheckBox::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());

    auto text_rect = rect();
    text_rect.set_left(s_box_width + 4);
    text_rect.set_top(height() / 2 - font().glyph_height() / 2);

    if (fill_with_background_color())
        painter.fill_rect(rect(), background_color());

    Rect box_rect {
        2, height() / 2 - s_box_height / 2 - 1,
        s_box_width, s_box_height
    };
    painter.fill_rect(box_rect, Color::White);
    painter.draw_rect(box_rect, Color::Black);

    if (m_being_modified)
        painter.draw_rect(box_rect.shrunken(2, 2), Color::MidGray);

    if (m_checked)
        painter.draw_bitmap(box_rect.shrunken(2, 2).location(), *s_checked_bitmap, foreground_color());

    if (!caption().is_empty())
        painter.draw_text(text_rect, caption(), TextAlignment::TopLeft, foreground_color());

    if (is_focused()) {
        // NOTE: Painter::draw_focus_rect() will shrink(2,2) the passed rect.
        auto focus_rect = box_rect;
        focus_rect.inflate(4, 4);
        painter.draw_focus_rect(focus_rect);
    }
}

void GCheckBox::mousemove_event(GMouseEvent& event)
{
    if (event.buttons() == GMouseButton::Left) {
        bool being_modified = rect().contains(event.position());
        if (being_modified != m_being_modified) {
            m_being_modified = being_modified;
            update();
        }
    }
    GWidget::mousemove_event(event);
}

void GCheckBox::mousedown_event(GMouseEvent& event)
{
#ifdef GCHECKBOX_DEBUG
    dbgprintf("GCheckBox::mouse_down_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == GMouseButton::Left) {
        m_being_modified = true;
        update();
    }
    GWidget::mousedown_event(event);
}

void GCheckBox::mouseup_event(GMouseEvent& event)
{
#ifdef GCHECKBOX_DEBUG
    dbgprintf("GCheckBox::mouseup_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == GMouseButton::Left) {
        bool was_being_pressed = m_being_modified;
        m_being_modified = false;
        if (was_being_pressed)
            set_checked(!is_checked());
        update();
    }
    GWidget::mouseup_event(event);
}

void GCheckBox::keydown_event(GKeyEvent& event)
{
    if (event.key() == KeyCode::Key_Space) {
        set_checked(!is_checked());
        update();
    }
    GWidget::keydown_event(event);
}
