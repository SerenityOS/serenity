#include "CheckBox.h"
#include "Painter.h"
#include "CBitmap.h"
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
    "############"
    "#          #"
    "#          #"
    "#          #"
    "#          #"
    "#          #"
    "#          #"
    "#          #"
    "#          #"
    "#          #"
    "#          #"
    "############"
};

static const char* checkedBitmap = {
    "############"
    "#          #"
    "# #      # #"
    "#  #    #  #"
    "#   #  #   #"
    "#    ##    #"
    "#    ##    #"
    "#   #  #   #"
    "#  #    #  #"
    "# #      # #"
    "#          #"
    "############"
};

void CheckBox::onPaint(PaintEvent&)
{
    Painter painter(*this);
    auto bitmap = CBitmap::createFromASCII(isChecked() ? checkedBitmap : uncheckedBitmap, 12, 12);

    auto textRect = rect();
    textRect.setLeft(bitmap->width() + 4);
    textRect.setTop(height() / 2 - bitmap->height() / 2);

    painter.fillRect(rect(), backgroundColor());
    painter.drawBitmap({ 2, textRect.y() }, *bitmap, Color(0, 0, 0));

    if (!caption().isEmpty()) {
        painter.drawText(textRect, caption(), Painter::TextAlignment::TopLeft, Color(0, 0, 0));
    }
}

void CheckBox::onMouseDown(MouseEvent& event)
{
    printf("CheckBox::onMouseDown: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());

    setIsChecked(!isChecked());
}

