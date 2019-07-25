#pragma once

#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/CObject.h>
#include <LibDraw/Rect.h>

class Painter;
class WSKeyEvent;
class WSWindow;

class WSWindowSwitcher : public CObject {
    C_OBJECT(WSWindowSwitcher)
public:
    static WSWindowSwitcher& the();

    WSWindowSwitcher();
    virtual ~WSWindowSwitcher() override;

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    void show() { set_visible(true); }
    void hide() { set_visible(false); }

    void on_key_event(const WSKeyEvent&);
    void refresh();
    void refresh_if_needed();

    void draw();

    int thumbnail_width() { return 40; }
    int thumbnail_height() { return 40; }

    int item_height() { return 10 + thumbnail_height(); }
    int padding() { return 8; }
    int item_padding() { return 8; }

    WSWindow* selected_window();

    WSWindow* switcher_window() { return m_switcher_window.ptr(); }

private:
    OwnPtr<WSWindow> m_switcher_window;
    Rect m_rect;
    bool m_visible { false };
    Vector<WeakPtr<WSWindow>> m_windows;
    int m_selected_index { 0 };
};
