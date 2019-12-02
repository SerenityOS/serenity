#pragma once

#include <AK/InlineLinkedList.h>
#include <AK/String.h>
#include <LibCore/CObject.h>
#include <LibDraw/DisjointRectSet.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/Rect.h>
#include <WindowServer/WSWindowFrame.h>
#include <WindowServer/WSWindowType.h>

class WSClientConnection;
class WSCursor;
class WSMenu;
class WSMouseEvent;

enum WSWMEventMask {
    WindowRectChanges = 1 << 0,
    WindowStateChanges = 1 << 1,
    WindowIconChanges = 1 << 2,
    WindowRemovals = 1 << 3,
};

class WSWindow final : public CObject
    , public InlineLinkedListNode<WSWindow> {
    C_OBJECT(WSWindow)
public:
    WSWindow(WSClientConnection&, WSWindowType, int window_id, bool modal, bool resizable, bool fullscreen);
    WSWindow(CObject&, WSWindowType);
    virtual ~WSWindow() override;

    void popup_window_menu(const Point&);
    void request_close();

    unsigned wm_event_mask() const { return m_wm_event_mask; }
    void set_wm_event_mask(unsigned mask) { m_wm_event_mask = mask; }

    Color background_color() const { return m_background_color; }
    void set_background_color(Color color) { m_background_color = color; }

    bool is_minimized() const { return m_minimized; }
    void set_minimized(bool);

    bool is_maximized() const { return m_maximized; }
    void set_maximized(bool);

    bool is_fullscreen() const { return m_fullscreen; }
    void set_fullscreen(bool);

    bool show_titlebar() const { return m_show_titlebar; }
    void set_show_titlebar(bool show) { m_show_titlebar = show; }

    bool is_movable() const
    {
        return m_type == WSWindowType::Normal;
    }

    WSWindowFrame& frame() { return m_frame; }
    const WSWindowFrame& frame() const { return m_frame; }

    bool is_blocked_by_modal_window() const;

    bool listens_to_wm_events() const { return m_listens_to_wm_events; }

    WSClientConnection* client() { return m_client; }
    const WSClientConnection* client() const { return m_client; }

    WSWindowType type() const { return m_type; }
    int window_id() const { return m_window_id; }

    String title() const { return m_title; }
    void set_title(const String&);

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

    bool is_resizable() const { return m_resizable && !m_fullscreen; }

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

    virtual void event(CEvent&) override;

    GraphicsBitmap* backing_store() { return m_backing_store.ptr(); }
    void set_backing_store(RefPtr<GraphicsBitmap>&& backing_store)
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

    Size size_increment() const { return m_size_increment; }
    void set_size_increment(const Size& increment) { m_size_increment = increment; }

    Size base_size() const { return m_base_size; }
    void set_base_size(const Size& size) { m_base_size = size; }

    const GraphicsBitmap& icon() const { return *m_icon; }
    void set_icon(NonnullRefPtr<GraphicsBitmap>&& icon) { m_icon = move(icon); }

    void set_default_icon();

    const WSCursor* override_cursor() const { return m_override_cursor.ptr(); }
    void set_override_cursor(RefPtr<WSCursor>&& cursor) { m_override_cursor = move(cursor); }

    void request_update(const Rect&);
    DisjointRectSet take_pending_paint_rects() { return move(m_pending_paint_rects); }

    // For InlineLinkedList.
    // FIXME: Maybe make a ListHashSet and then WSWindowManager can just use that.
    WSWindow* m_next { nullptr };
    WSWindow* m_prev { nullptr };

private:
    void handle_mouse_event(const WSMouseEvent&);

    WSClientConnection* m_client { nullptr };
    String m_title;
    Rect m_rect;
    Rect m_saved_nonfullscreen_rect;
    WSWindowType m_type { WSWindowType::Normal };
    bool m_global_cursor_tracking_enabled { false };
    bool m_automatic_cursor_tracking_enabled { false };
    bool m_visible { true };
    bool m_has_alpha_channel { false };
    bool m_modal { false };
    bool m_resizable { false };
    bool m_listens_to_wm_events { false };
    bool m_minimized { false };
    bool m_maximized { false };
    bool m_fullscreen { false };
    bool m_show_titlebar { true };
    RefPtr<GraphicsBitmap> m_backing_store;
    RefPtr<GraphicsBitmap> m_last_backing_store;
    int m_window_id { -1 };
    float m_opacity { 1 };
    Size m_size_increment;
    Size m_base_size;
    NonnullRefPtr<GraphicsBitmap> m_icon;
    RefPtr<WSCursor> m_override_cursor;
    WSWindowFrame m_frame;
    Color m_background_color { Color::WarmGray };
    unsigned m_wm_event_mask { 0 };
    DisjointRectSet m_pending_paint_rects;
    Rect m_unmaximized_rect;
    RefPtr<WSMenu> m_window_menu;
};
