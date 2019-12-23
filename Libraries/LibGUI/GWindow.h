#pragma once

#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/CObject.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/Rect.h>
#include <LibGUI/GWindowType.h>

class GWMEvent;
class GWidget;
class GWindowServerConnection;

enum class GStandardCursor {
    None = 0,
    Arrow,
    IBeam,
    ResizeHorizontal,
    ResizeVertical,
    ResizeDiagonalTLBR,
    ResizeDiagonalBLTR,
    Hand,
};

class GWindow : public CObject {
    C_OBJECT(GWindow)
public:
    virtual ~GWindow() override;

    static GWindow* from_window_id(int);

    bool is_modal() const { return m_modal; }
    void set_modal(bool);

    bool is_fullscreen() const { return m_fullscreen; }
    void set_fullscreen(bool);

    bool is_resizable() const { return m_resizable; }
    void set_resizable(bool resizable) { m_resizable = resizable; }

    void set_double_buffering_enabled(bool);
    void set_has_alpha_channel(bool);
    void set_opacity(float);
    void set_window_type(GWindowType);

    int window_id() const { return m_window_id; }

    String title() const;
    void set_title(const StringView&);

    bool show_titlebar() const { return m_show_titlebar; };
    void set_show_titlebar(bool show) { m_show_titlebar = show; };

    Color background_color() const { return m_background_color; }
    void set_background_color(Color color) { m_background_color = color; }

    enum class CloseRequestDecision {
        StayOpen,
        Close,
    };

    Function<CloseRequestDecision()> on_close_request;

    int x() const { return rect().x(); }
    int y() const { return rect().y(); }
    int width() const { return rect().width(); }
    int height() const { return rect().height(); }

    Rect rect() const;
    Size size() const { return rect().size(); }
    void set_rect(const Rect&);
    void set_rect(int x, int y, int width, int height) { set_rect({ x, y, width, height }); }

    Point position() const { return rect().location(); }

    void move_to(int x, int y) { move_to({ x, y }); }
    void move_to(const Point& point) { set_rect({ point, size() }); }

    void resize(int width, int height) { resize({ width, height }); }
    void resize(const Size& size) { set_rect({ position(), size }); }

    virtual void event(CEvent&) override;

    bool is_visible() const;
    bool is_active() const { return m_is_active; }

    void show();
    void hide();
    virtual void close();
    void move_to_front();

    void start_wm_resize();

    GWidget* main_widget() { return m_main_widget; }
    const GWidget* main_widget() const { return m_main_widget; }
    void set_main_widget(GWidget*);

    GWidget* focused_widget() { return m_focused_widget; }
    const GWidget* focused_widget() const { return m_focused_widget; }
    void set_focused_widget(GWidget*);

    void update(const Rect& = Rect());

    void set_global_cursor_tracking_widget(GWidget*);
    GWidget* global_cursor_tracking_widget() { return m_global_cursor_tracking_widget.ptr(); }
    const GWidget* global_cursor_tracking_widget() const { return m_global_cursor_tracking_widget.ptr(); }

    void set_automatic_cursor_tracking_widget(GWidget*);
    GWidget* automatic_cursor_tracking_widget() { return m_automatic_cursor_tracking_widget.ptr(); }
    const GWidget* automatic_cursor_tracking_widget() const { return m_automatic_cursor_tracking_widget.ptr(); }

    GWidget* hovered_widget() { return m_hovered_widget.ptr(); }
    const GWidget* hovered_widget() const { return m_hovered_widget.ptr(); }
    void set_hovered_widget(GWidget*);

    GraphicsBitmap* front_bitmap() { return m_front_bitmap.ptr(); }
    GraphicsBitmap* back_bitmap() { return m_back_bitmap.ptr(); }

    Size size_increment() const { return m_size_increment; }
    void set_size_increment(const Size& increment) { m_size_increment = increment; }
    Size base_size() const { return m_base_size; }
    void set_base_size(const Size& size) { m_base_size = size; }

    void set_override_cursor(GStandardCursor);

    void set_icon(const GraphicsBitmap*);
    void apply_icon();
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }

    Vector<GWidget*> focusable_widgets() const;

    virtual void save_to(AK::JsonObject&) override;

    void schedule_relayout();

    static void update_all_windows(Badge<GWindowServerConnection>);

protected:
    GWindow(CObject* parent = nullptr);
    virtual void wm_event(GWMEvent&);

private:
    virtual bool is_window() const override final { return true; }

    void paint_keybinds();

    void collect_keyboard_activation_targets();

    NonnullRefPtr<GraphicsBitmap> create_backing_bitmap(const Size&);
    NonnullRefPtr<GraphicsBitmap> create_shared_bitmap(GraphicsBitmap::Format, const Size&);
    void set_current_backing_bitmap(GraphicsBitmap&, bool flush_immediately = false);
    void flip(const Vector<Rect, 32>& dirty_rects);

    RefPtr<GraphicsBitmap> m_front_bitmap;
    RefPtr<GraphicsBitmap> m_back_bitmap;
    RefPtr<GraphicsBitmap> m_icon;
    int m_window_id { 0 };
    float m_opacity_when_windowless { 1.0f };
    RefPtr<GWidget> m_main_widget;
    WeakPtr<GWidget> m_focused_widget;
    WeakPtr<GWidget> m_global_cursor_tracking_widget;
    WeakPtr<GWidget> m_automatic_cursor_tracking_widget;
    WeakPtr<GWidget> m_hovered_widget;
    Rect m_rect_when_windowless;
    String m_title_when_windowless;
    Vector<Rect, 32> m_pending_paint_event_rects;
    Size m_size_increment;
    Size m_base_size;
    Color m_background_color { Color::WarmGray };
    GWindowType m_window_type { GWindowType::Normal };
    bool m_is_active { false };
    bool m_has_alpha_channel { false };
    bool m_double_buffering_enabled { true };
    bool m_modal { false };
    bool m_resizable { true };
    bool m_fullscreen { false };
    bool m_show_titlebar { true };
    bool m_keybind_mode { false };
    String m_entered_keybind;
    size_t m_max_keybind_length { 0 };
    HashMap<String, WeakPtr<GWidget>> m_keyboard_activation_targets;
    bool m_layout_pending { false };
};
