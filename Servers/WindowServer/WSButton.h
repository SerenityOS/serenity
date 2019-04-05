#pragma once

#include <SharedGraphics/Rect.h>
#include <AK/Function.h>
#include <AK/Retained.h>

class CharacterBitmap;
class Painter;
class WSMouseEvent;

class WSButton {
public:
    WSButton(Retained<CharacterBitmap>&&, Function<void()>&& on_click_handler);
    ~WSButton();

    Rect relative_rect() const { return m_relative_rect; }
    void set_relative_rect(const Rect& rect) { m_relative_rect = rect; }

    Rect rect() const { return { { }, m_relative_rect.size() }; }

    void paint(Painter&);

    void on_mouse_event(const WSMouseEvent&);

    Function<void()> on_click;

    bool is_visible() const { return m_visible; }

private:
    Rect m_relative_rect;
    Retained<CharacterBitmap> m_bitmap;
    bool m_pressed { false };
    bool m_visible { true };
};
