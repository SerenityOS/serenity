#include <LibGUI/GRadioButton.h>
#include <LibGUI/GPainter.h>
#include <SharedGraphics/GraphicsBitmap.h>

static RetainPtr<GraphicsBitmap> s_unfilled_circle_bitmap;
static RetainPtr<GraphicsBitmap> s_filled_circle_bitmap;
static RetainPtr<GraphicsBitmap> s_changing_filled_circle_bitmap;
static RetainPtr<GraphicsBitmap> s_changing_unfilled_circle_bitmap;

GRadioButton::GRadioButton(const String& text, GWidget* parent)
    : GAbstractButton(text, parent)
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

    auto& bitmap = circle_bitmap(is_checked(), is_being_pressed());
    painter.blit(circle_rect.location(), bitmap, bitmap.rect());

    if (!text().is_empty()) {
        Rect text_rect { circle_rect.right() + 4, 0, font().width(text()), font().glyph_height() };
        text_rect.center_vertically_within(rect());
        painter.draw_text(text_rect, text(), TextAlignment::CenterLeft, foreground_color());

        if (is_focused())
            painter.draw_rect(text_rect.inflated(6, 4), Color(140, 140, 140));
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

void GRadioButton::click()
{
    if (!is_enabled())
        return;
    for_each_in_group([this] (auto& button) {
        if (&button != this)
            button.set_checked(false);
    });
    set_checked(true);
}
