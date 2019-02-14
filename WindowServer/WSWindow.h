#pragma once

#include <SharedGraphics/Rect.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <AK/AKString.h>
#include <AK/InlineLinkedList.h>
#include <AK/Lock.h>
#include <AK/Badge.h>
#include "WSMessageReceiver.h"
#include <WindowServer/WSWindowType.h>

class WSMenu;

class WSWindow final : public WSMessageReceiver, public InlineLinkedListNode<WSWindow> {
    friend class WSWindowLocker;
public:
    WSWindow(int client_id, int window_id);
    explicit WSWindow(WSMenu&);
    virtual ~WSWindow() override;

    int client_id() const { return m_client_id; }

    WSWindowType type() const { return m_type; }
    int window_id() const { return m_window_id; }

    String title() const { return m_title; }
    void set_title(String&&);

    int x() const { return m_rect.x(); }
    int y() const { return m_rect.y(); }
    int width() const { return m_rect.width(); }
    int height() const { return m_rect.height(); }

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    Rect rect() const { return m_rect; }
    void set_rect(const Rect&);
    void set_rect(int x, int y, int width, int height) { set_rect({ x, y, width, height }); }
    void set_rect_without_repaint(const Rect& rect) { m_rect = rect; }

    void move_to(const Point& position) { set_rect({ position, size() }); }
    void move_to(int x, int y) { move_to({ x, y }); }

    Point position() const { return m_rect.location(); }
    void set_position(const Point& position) { set_rect({ position.x(), position.y(), width(), height() }); }
    void set_position_without_repaint(const Point& position) { set_rect_without_repaint({ position.x(), position.y(), width(), height() }); }

    Size size() const { return m_rect.size(); }

    void invalidate();

    virtual void on_message(WSMessage&) override;

    bool is_being_dragged() const { return m_is_being_dragged; }
    void set_is_being_dragged(bool b) { m_is_being_dragged = b; }

    GraphicsBitmap* backing() { return m_backing.ptr(); }

    void set_global_cursor_tracking_enabled(bool);
    bool global_cursor_tracking() const { return m_global_cursor_tracking_enabled; }

    // For InlineLinkedList.
    // FIXME: Maybe make a ListHashSet and then WSWindowManager can just use that.
    WSWindow* m_next { nullptr };
    WSWindow* m_prev { nullptr };

private:
    Lock m_lock;
    int m_client_id { 0 };
    String m_title;
    Rect m_rect;
    WSWindowType m_type { WSWindowType::Normal };
    bool m_is_being_dragged { false };
    bool m_global_cursor_tracking_enabled { false };
    bool m_visible { true };

    WSMenu* m_menu { nullptr };

    RetainPtr<GraphicsBitmap> m_backing;
    int m_window_id { -1 };
};

class WSWindowLocker {
public:
    WSWindowLocker(WSWindow& window) : m_locker(window.m_lock) { }
    ~WSWindowLocker() { }
private:
    Locker m_locker;
};

