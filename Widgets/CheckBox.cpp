#include "CheckBox.h"
#include "Painter.h"
#include "CharacterBitmap.h"

CheckBox::CheckBox(Widget* parent)
    : Widget(parent)
{
}

CheckBox::~CheckBox()
{
}

void CheckBox::setCaption(String&& caption)
{
    if (caption == m_caption)
        return;
    m_caption = move(caption);
    update();
}

void CheckBox::setIsChecked(bool b)
{
    if (m_isChecked == b)
        return;
    m_isChecked = b;
    update();
}

static const char* uncheckedBitmap = {
    "###########"
    "#         #"
    "#         #"
    "#         #"
    "#         #"
    "#         #"
    "#         #"
    "#         #"
    "#         #"
    "#         #"
    "###########"
};

#if 0
static const char* checkedBitmap = {
    "############"
    "#          #"
    "#       ## #"
    "#       ## #"
    "#      ##  #"
    "#      ##  #"
    "#     ##   #"
    "# ##  ##   #"
    "#  ## ##   #"
    "#   ###    #"
    "#          #"
    "############"
};
#endif

static const char* checkedBitmap = {
    "###########"
    "##       ##"
    "# #     # #"
    "#  #   #  #"
    "#   # #   #"
    "#    #    #"
    "#   # #   #"
    "#  #   #  #"
    "# #     # #"
    "##       ##"
    "###########"
};

void CheckBox::paintEvent(PaintEvent&)
{
    Painter painter(*this);
    auto bitmap = CharacterBitmap::create_from_ascii(isChecked() ? checkedBitmap : uncheckedBitmap, 11, 11);

    auto textRect = rect();
    textRect.set_left(bitmap->width() + 4);
    textRect.set_top(height() / 2 - font().glyph_height() / 2);

    Point bitmapPosition;
    bitmapPosition.set_x(2);
    bitmapPosition.set_y(height() / 2 - bitmap->height() / 2 - 1);

    painter.fill_rect(rect(), backgroundColor());
    painter.draw_bitmap(bitmapPosition, *bitmap, foregroundColor());

    if (!caption().is_empty()) {
        painter.draw_text(textRect, caption(), Painter::TextAlignment::TopLeft, foregroundColor());
    }
}

void CheckBox::mouseDownEvent(MouseEvent& event)
{
    printf("CheckBox::mouseDownEvent: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());

    setIsChecked(!isChecked());
}

