#include <LibDraw/CharacterBitmap.h>
#include <LibDraw/Painter.h>
#include <LibDraw/StylePainter.h>
#include <WindowServer/WSButton.h>
#include <WindowServer/WSEvent.h>
#include <WindowServer/WSWindowManager.h>

WSButton::WSButton(WSWindowFrame& frame, NonnullRefPtr<CharacterBitmap>&& bitmap, Function<void(WSButton&)>&& on_click_handler)
    : on_click(move(on_click_handler))
    , m_frame(frame)
    , m_bitmap(move(bitmap))
{
}

WSButton::~WSButton()
{
}

void WSButton::paint(Painter& painter)
{
    PainterStateSaver saver(painter);
    painter.translate(relative_rect().location());
    StylePainter::paint_button(painter, rect(), ButtonStyle::Normal, m_pressed, m_hovered);
    auto x_location = rect().center();
    x_location.move_by(-(m_bitmap->width() / 2), -(m_bitmap->height() / 2));
    if (m_pressed)
        x_location.move_by(1, 1);
    painter.draw_bitmap(x_location, *m_bitmap, SystemColor::Text);
}

void WSButton::on_mouse_event(const WSMouseEvent& event)
{
    auto& wm = WSWindowManager::the();

    if (event.type() == WSEvent::MouseDown && event.button() == MouseButton::Left) {
        m_pressed = true;
        wm.set_cursor_tracking_button(this);
        wm.invalidate(screen_rect());
        return;
    }

    if (event.type() == WSEvent::MouseUp && event.button() == MouseButton::Left) {
        if (wm.cursor_tracking_button() != this)
            return;
        wm.set_cursor_tracking_button(nullptr);
        bool old_pressed = m_pressed;
        m_pressed = false;
        if (rect().contains(event.position())) {
            if (on_click)
                on_click(*this);
        }
        if (old_pressed != m_pressed)
            wm.invalidate(screen_rect());
        return;
    }

    if (event.type() == WSEvent::MouseMove) {
        bool old_hovered = m_hovered;
        m_hovered = rect().contains(event.position());
        wm.set_hovered_button(m_hovered ? this : nullptr);
        if (old_hovered != m_hovered)
            wm.invalidate(screen_rect());
    }

    if (event.type() == WSEvent::MouseMove && event.buttons() & (unsigned)MouseButton::Left) {
        if (wm.cursor_tracking_button() != this)
            return;
        bool old_pressed = m_pressed;
        m_pressed = m_hovered;
        if (old_pressed != m_pressed)
            wm.invalidate(screen_rect());
    }
}

Rect WSButton::screen_rect() const
{
    return m_relative_rect.translated(m_frame.rect().location());
}
