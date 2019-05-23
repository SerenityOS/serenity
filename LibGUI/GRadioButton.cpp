#include <LibGUI/GRadioButton.h>
#include <LibGUI/GPainter.h>
#include <SharedGraphics/GraphicsBitmap.h>

static RetainPtr<GraphicsBitmap> s_unfilled_circle_bitmap;
static RetainPtr<GraphicsBitmap> s_filled_circle_bitmap;
static RetainPtr<GraphicsBitmap> s_changing_filled_circle_bitmap;
static RetainPtr<GraphicsBitmap> s_changing_unfilled_circle_bitmap;

GRadioButton::GRadioButton(const String& label, GWidget* parent)
    : GWidget(parent)
    , m_label(label)
{
    if (!s_unfilled_circle_bitmap) {
        s_unfilled_circle_bitmap = GraphicsBitmap::load_from_file("/res/icons/unfilled-radio-circle.png");
        s_filled_circle_bitmap = GraphicsBitmap::load_from_file("/res/icons/filled-radio-circle.png");
        s_changing_filled_circle_bitmap = GraphicsBitmap::load_from_file("/res/icons/changing-filled-radio-circle.png");
        s_changing_unfilled_circle_bitmap = GraphicsBitmap::load_from_file("/res/icons/changing-unfilled-radio-circle.png");
    }
}

GRadioButton::~GRadioButton()
{
}

Size GRadioButton::circle_size()
{
    return s_unfilled_circle_bitmap->size();
}

static const GraphicsBitmap& circle_bitmap(bool checked, bool changing)
{
    if (changing)
        return checked ? *s_changing_filled_circle_bitmap : *s_changing_unfilled_circle_bitmap;
    return checked ? *s_filled_circle_bitmap : *s_unfilled_circle_bitmap;
}

void GRadioButton::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    Rect circle_rect { { 2, 0 }, circle_size() };
    circle_rect.center_vertically_within(rect());

    auto& bitmap = circle_bitmap(m_checked, m_changing);
    painter.blit(circle_rect.location(), bitmap, bitmap.rect());

    if (!m_label.is_empty()) {
        Rect text_rect { circle_rect.right() + 4, 0, font().width(m_label), font().glyph_height() };
        text_rect.center_vertically_within(rect());
        painter.draw_text(text_rect, m_label, TextAlignment::CenterLeft, foreground_color());
    }
}

template<typename Callback>
void GRadioButton::for_each_in_group(Callback callback)
{
    if (!parent())
        return;
    for (auto& object : parent()->children()) {
        if (!object->is_widget())
            continue;
        if (!static_cast<GWidget*>(object)->is_radio_button())
            continue;
        callback(*static_cast<GRadioButton*>(object));
    }
}

void GRadioButton::mousedown_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;

    m_changing = rect().contains(event.position());
    m_tracking = true;
    update();
}

void GRadioButton::mousemove_event(GMouseEvent& event)
{
    if (m_tracking) {
        bool old_changing = m_changing;
        m_changing = rect().contains(event.position());
        if (old_changing != m_changing)
            update();
    }
}

void GRadioButton::mouseup_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;

    if (rect().contains(event.position())) {
        for_each_in_group([this] (auto& button) {
            if (&button != this)
                button.set_checked(false);
        });
        set_checked(true);
    }

    m_changing = false;
    m_tracking = false;
    update();
}

void GRadioButton::set_label(const String& label)
{
    if (m_label == label)
        return;
    m_label = label;
    update();
}

void GRadioButton::set_checked(bool checked)
{
    if (m_checked == checked)
        return;
    m_checked = checked;
    update();
}
