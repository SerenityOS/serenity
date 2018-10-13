#include "CheckBox.h"
#include "Painter.h"
#include "CBitmap.h"
#include "Font.h"
#include <cstdio>

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
    m_caption = std::move(caption);
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
    auto bitmap = CBitmap::createFromASCII(isChecked() ? checkedBitmap : uncheckedBitmap, 11, 11);

    auto textRect = rect();
    textRect.setLeft(bitmap->width() + 4);
    textRect.setTop(height() / 2 - Font::defaultFont().glyphHeight() / 2);

    Point bitmapPosition;
    bitmapPosition.setX(2);
    bitmapPosition.setY(height() / 2 - bitmap->height() / 2 - 1);

    painter.fillRect(rect(), backgroundColor());
    painter.drawBitmap(bitmapPosition, *bitmap, foregroundColor());

    if (!caption().isEmpty()) {
        painter.drawText(textRect, caption(), Painter::TextAlignment::TopLeft, foregroundColor());
    }
}

void CheckBox::mouseDownEvent(MouseEvent& event)
{
    printf("CheckBox::mouseDownEvent: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());

    setIsChecked(!isChecked());
}

