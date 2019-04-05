#pragma once

#include <SharedGraphics/Rect.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <AK/AKString.h>
#include <AK/InlineLinkedList.h>
#include "WSMessageReceiver.h"
#include <WindowServer/WSWindowType.h>
#include <WindowServer/WSWindowFrame.h>

class WSClientConnection;
class WSCursor;
class WSMenu;
class WSMouseEvent;

class WSWindow final : public WSMessageReceiver, public InlineLinkedListNode<WSWindow> {
public:
    WSWindow(WSClientConnection&, WSWindowType, int window_id, bool modal);
    WSWindow(WSMessageReceiver&, WSWindowType);
    virtual ~WSWindow() override;

    bool is_minimized() const { return m_minimized; }
    void set_minimized(bool);

    WSWindowFrame& frame() { return m_frame; }
    const WSWindowFrame& frame() const { return m_frame; }

    bool is_blocked_by_modal_window() const;

    bool listens_to_wm_events() const { return m_listens_to_wm_events; }

    WSClientConnection* client() { return m_client; }
    const WSClientConnection* client() const { return m_client; }

    WSWindowType type() const { return m_type; }
    int window_id() const { return m_window_id; }

    String title() const { return m_title; }
    void set_title(String&&);

    float opacity() const { return m_opacity; }
    void set_opacity(float opacity) { m_opacity = opacity; }

    int x() const { return m_rect.x(); }
    int y() const { return m_rect.y(); }
    int width() const { return m_rect.width(); }
    int height() const { return m_rect.height(); }

    bool is_active() const;

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    bool is_modal() const { return m_modal; }

    bool is_resizable() const { return m_resizable; }
    void set_resizable(bool);

    Rect rect() const { return m_rect; }
    void set_rect(const Rect&);
    void set_rect(int x, int y, int width, int height) { set_rect({ x, y, width, height }); }
    void set_rect_without_repaint(const Rect& rect)
    {
        if (m_rect == rect)
            return;
        auto old_rect = m_rect;
        m_rect = rect;
        m_frame.notify_window_rect_changed(old_rect, rect);
    }

    void set_rect_from_window_manager_resize(const Rect&);

    void move_to(const Point& position) { set_rect({ position, size() }); }
    void move_to(int x, int y) { move_to({ x, y }); }

    Point position() const { return m_rect.location(); }
    void set_position(const Point& position) { set_rect({ position.x(), position.y(), width(), height() }); }
    void set_position_without_repaint(const Point& position) { set_rect_without_repaint({ position.x(), position.y(), width(), height() }); }

    Size size() const { return m_rect.size(); }

    void invalidate();

    virtual void on_message(const WSMessage&) override;

    GraphicsBitmap* backing_store() { return m_backing_store.ptr(); }
    void set_backing_store(RetainPtr<GraphicsBitmap>&& backing_store)
    {
        m_last_backing_store = move(m_backing_store);
        m_backing_store = move(backing_store);
    }
    void swap_backing_stores()
    {
        swap(m_backing_store, m_last_backing_store);
    }

    GraphicsBitmap* last_backing_store() { return m_last_backing_store.ptr(); }

    void set_global_cursor_tracking_enabled(bool);
    void set_automatic_cursor_tracking_enabled(bool enabled) { m_automatic_cursor_tracking_enabled = enabled; }
    bool global_cursor_tracking() const { return m_global_cursor_tracking_enabled || m_automatic_cursor_tracking_enabled; }

    bool has_alpha_channel() const { return m_has_alpha_channel; }
    void set_has_alpha_channel(bool value) { m_has_alpha_channel = value; }

    void set_last_lazy_resize_rect(const Rect& rect) { m_last_lazy_resize_rect = rect; }
    Rect last_lazy_resize_rect() const { return m_last_lazy_resize_rect; }

    bool has_painted_since_last_resize() const { return m_has_painted_since_last_resize; }
    void set_has_painted_since_last_resize(bool b) { m_has_painted_since_last_resize = b; }

    Size size_increment() const { return m_size_increment; }
    void set_size_increment(const Size& increment) { m_size_increment = increment; }

    Size base_size() const { return m_base_size; }
    void set_base_size(const Size& size) { m_base_size = size; }

    const GraphicsBitmap& icon() const { return *m_icon; }
    void set_icon(Retained<GraphicsBitmap>&& icon) { m_icon = move(icon); }

    const WSCursor* override_cursor() const { return m_override_cursor.ptr(); }
    void set_override_cursor(RetainPtr<WSCursor>&& cursor) { m_override_cursor = move(cursor); }

    // For InlineLinkedList.
    // FIXME: Maybe make a ListHashSet and then WSWindowManager can just use that.
    WSWindow* m_next { nullptr };
    WSWindow* m_prev { nullptr };

private:
    void handle_mouse_event(const WSMouseEvent&);

    WSClientConnection* m_client { nullptr };
    WSMessageReceiver* m_internal_owner { nullptr };
    String m_title;
    Rect m_rect;
    WSWindowType m_type { WSWindowType::Normal };
    bool m_global_cursor_tracking_enabled { false };
    bool m_automatic_cursor_tracking_enabled { false };
    bool m_visible { true };
    bool m_has_alpha_channel { false };
    bool m_has_painted_since_last_resize { false };
    bool m_modal { false };
    bool m_resizable { false };
    bool m_listens_to_wm_events { false };
    bool m_minimized { false };
    RetainPtr<GraphicsBitmap> m_backing_store;
    RetainPtr<GraphicsBitmap> m_last_backing_store;
    int m_window_id { -1 };
    float m_opacity { 1 };
    Rect m_last_lazy_resize_rect;
    Size m_size_increment;
    Size m_base_size;
    Retained<GraphicsBitmap> m_icon;
    RetainPtr<WSCursor> m_override_cursor;
    WSWindowFrame m_frame;
};
