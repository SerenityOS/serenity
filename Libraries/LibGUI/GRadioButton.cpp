#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/StylePainter.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GRadioButton.h>

GRadioButton::GRadioButton(GWidget* parent)
    : GRadioButton({}, parent)
{
}

GRadioButton::GRadioButton(const StringView& text, GWidget* parent)
    : GAbstractButton(text, parent)
{
}

GRadioButton::~GRadioButton()
{
}

Size GRadioButton::circle_size()
{
    return { 12, 12 };
}

void GRadioButton::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    Rect circle_rect { { 2, 0 }, circle_size() };
    circle_rect.center_vertically_within(rect());

    StylePainter::paint_radio_button(painter, circle_rect, palette(), is_checked(), is_being_pressed());

    Rect text_rect { circle_rect.right() + 4, 0, font().width(text()), font().glyph_height() };
    text_rect.center_vertically_within(rect());
    paint_text(painter, text_rect, font(), TextAlignment::TopLeft);
}

template<typename Callback>
void GRadioButton::for_each_in_group(Callback callback)
{
    if (!parent())
        return;
    parent()->for_each_child_of_type<GRadioButton>([&](auto& child) {
        return callback(static_cast<GRadioButton&>(child));
    });
}

void GRadioButton::click()
{
    if (!is_enabled())
        return;
    for_each_in_group([this](auto& button) {
        if (&button != this)
            button.set_checked(false);
        return IterationDecision::Continue;
    });
    set_checked(true);
}
