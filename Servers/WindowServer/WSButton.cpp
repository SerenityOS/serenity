#include <WindowServer/WSButton.h>
#include <WindowServer/WSMessage.h>
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/StylePainter.h>
#include <SharedGraphics/CharacterBitmap.h>

WSButton::WSButton(Retained<CharacterBitmap>&& bitmap, Function<void()>&& on_click_handler)
    : on_click(move(on_click_handler))
    , m_bitmap(move(bitmap))
{
}

WSButton::~WSButton()
{
}

void WSButton::paint(Painter& painter)
{
    StylePainter::paint_button(painter, m_rect, ButtonStyle::Normal, m_pressed);
    auto x_location = m_rect.center();
    x_location.move_by(-(m_bitmap->width() / 2), -(m_bitmap->height() / 2));
    painter.draw_bitmap(x_location, *m_bitmap, Color::Black);
}

void WSButton::on_mouse_event(const WSMouseEvent& event)
{
    if (event.type() == WSMessage::MouseDown) {
        if (on_click)
            on_click();
    }
}
