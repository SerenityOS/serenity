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
    auto bitmap = CharacterBitmap::createFromASCII(isChecked() ? checkedBitmap : uncheckedBitmap, 11, 11);

    auto textRect = rect();
    textRect.setLeft(bitmap->width() + 4);
    textRect.setTop(height() / 2 - font().glyphHeight() / 2);

    Point bitmapPosition;
    bitmapPosition.setX(2);
    bitmapPosition.setY(height() / 2 - bitmap->height() / 2 - 1);

    painter.fillRect(rect(), backgroundColor());
    painter.drawBitmap(bitmapPosition, *bitmap, foregroundColor());

    if (!caption().is_empty()) {
        painter.drawText(textRect, caption(), Painter::TextAlignment::TopLeft, foregroundColor());
    }
}

void CheckBox::mouseDownEvent(MouseEvent& event)
{
    printf("CheckBox::mouseDownEvent: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());

    setIsChecked(!isChecked());
}

