#pragma once

#include <LibCore/CElapsedTimer.h>
#include <LibGUI/GEvent.h>
#include <LibGUI/GShortcut.h>
#include <LibCore/CObject.h>
#include <SharedGraphics/Rect.h>
#include <SharedGraphics/Color.h>
#include <SharedGraphics/Font.h>
#include <AK/Badge.h>
#include <AK/AKString.h>
#include <AK/HashMap.h>

class GraphicsBitmap;
class GAction;
class GLayout;
class GMenu;
class GWindow;

enum class SizePolicy { Fixed, Fill };
enum class Orientation { Horizontal, Vertical };
enum class HorizontalDirection { Left, Right };
enum class VerticalDirection { Up, Down };

class GWidget : public CObject {
public:
    explicit GWidget(GWidget* parent = nullptr);
    virtual ~GWidget() override;

    GLayout* layout() { return m_layout.ptr(); }
    void set_layout(OwnPtr<GLayout>&&);

    SizePolicy horizontal_size_policy() const { return m_horizontal_size_policy; }
    SizePolicy vertical_size_policy() const { return m_vertical_size_policy; }
    SizePolicy size_policy(Orientation orientation) { return orientation == Orientation::Horizontal ? m_horizontal_size_policy : m_vertical_size_policy; }
    void set_size_policy(SizePolicy horizontal_policy, SizePolicy vertical_policy);

    Size preferred_size() const { return m_preferred_size; }
    void set_preferred_size(const Size&);

    bool has_tooltip() const { return !m_tooltip.is_empty(); }
    String tooltip() const { return m_tooltip; }
    void set_tooltip(const String& tooltip) { m_tooltip = tooltip; }

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool updates_enabled() const { return m_updates_enabled; }
    void set_updates_enabled(bool);

    virtual void event(CEvent&) override;
    virtual void paint_event(GPaintEvent&);
    virtual void resize_event(GResizeEvent&);
    virtual void show_event(GShowEvent&);
    virtual void hide_event(GHideEvent&);
    virtual void keydown_event(GKeyEvent&);
    virtual void keyup_event(GKeyEvent&);
    virtual void mousemove_event(GMouseEvent&);
    virtual void mousedown_event(GMouseEvent&);
    virtual void mouseup_event(GMouseEvent&);
    virtual void mousewheel_event(GMouseEvent&);
    virtual void click_event(GMouseEvent&);
    virtual void doubleclick_event(GMouseEvent&);
    virtual void context_menu_event(GContextMenuEvent&);
    virtual void focusin_event(CEvent&);
    virtual void focusout_event(CEvent&);
    virtual void enter_event(CEvent&);
    virtual void leave_event(CEvent&);
    virtual void child_event(CChildEvent&) override;

    // This is called after children have been painted.
    virtual void second_paint_event(GPaintEvent&);

    Rect relative_rect() const { return m_relative_rect; }
    Point relative_position() const { return m_relative_rect.location(); }

    Rect window_relative_rect() const;
    Rect screen_relative_rect() const;

    int x() const { return m_relative_rect.x(); }
    int y() const { return m_relative_rect.y(); }
    int width() const { return m_relative_rect.width(); }
    int height() const { return m_relative_rect.height(); }

    Rect rect() const { return { 0, 0, width(), height() }; }
    Size size() const { return m_relative_rect.size(); }

    void update();
    void update(const Rect&);

    virtual bool accepts_focus() const { return false; }

    bool is_focused() const;
    void set_focus(bool);

    struct HitTestResult {
        GWidget* widget { nullptr };
        Point local_position;
    };
    HitTestResult hit_test(const Point&);
    GWidget* child_at(const Point&) const;

    virtual const char* class_name() const override { return "GWidget"; }

    void set_relative_rect(const Rect&);
    void set_relative_rect(int x, int y, int width, int height) { set_relative_rect({ x, y, width, height }); }

    void set_x(int x) { set_relative_rect(x, y(), width(), height()); }
    void set_y(int y) { set_relative_rect(x(), y, width(), height()); }
    void set_width(int width) { set_relative_rect(x(), y(), width, height()); }
    void set_height(int height) { set_relative_rect(x(), y(), width(), height); }

    void move_to(const Point& point) { set_relative_rect({ point, relative_rect().size() }); }
    void move_to(int x, int y) { move_to({ x, y }); }
    void resize(const Size& size) { set_relative_rect({ relative_rect().location(), size }); }
    void resize(int width, int height) { resize({ width, height }); }

    void move_by(int x, int y) { move_by({ x, y }); }
    void move_by(const Point& delta) { set_relative_rect({ relative_position().translated(delta), size() }); }

    Color background_color() const { return m_background_color; }
    Color foreground_color() const { return m_foreground_color; }

    void set_background_color(Color color) { m_background_color = color; }
    void set_foreground_color(Color color) { m_foreground_color = color; }

    GWindow* window()
    {
        if (auto* pw = parent_widget())
            return pw->window();
        return m_window;
    }

    const GWindow* window() const
    {
        if (auto* pw = parent_widget())
            return pw->window();
        return m_window;
    }

    void set_window(GWindow*);

    GWidget* parent_widget()
    {
        if (parent() && parent()->is_widget())
            return static_cast<GWidget*>(parent());
        return nullptr;
    }
    const GWidget* parent_widget() const
    {
        if (parent() && parent()->is_widget())
            return static_cast<const GWidget*>(parent());
        return nullptr;
    }

    void set_fill_with_background_color(bool b) { m_fill_with_background_color = b; }
    bool fill_with_background_color() const { return m_fill_with_background_color; }

    const Font& font() const { return *m_font; }
    void set_font(RetainPtr<Font>&&);

    void set_global_cursor_tracking(bool);
    bool global_cursor_tracking() const;

    void notify_layout_changed(Badge<GLayout>);
    void invalidate_layout();

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    bool spans_entire_window_horizontally() const;

    bool is_greedy_for_hits() const { return m_greedy_for_hits; }
    void set_greedy_for_hits(bool b) { m_greedy_for_hits = b; }

    void move_to_front();
    void move_to_back();

    bool is_frontmost() const;
    bool is_backmost() const;

    GAction* action_for_key_event(const GKeyEvent&);

    void register_local_shortcut_action(Badge<GAction>, GAction&);
    void unregister_local_shortcut_action(Badge<GAction>, GAction&);

private:
    virtual bool is_widget() const final { return true; }

    void handle_paint_event(GPaintEvent&);
    void handle_resize_event(GResizeEvent&);
    void handle_mousedown_event(GMouseEvent&);
    void handle_mousedoubleclick_event(GMouseEvent&);
    void handle_mouseup_event(GMouseEvent&);
    void handle_enter_event(CEvent&);
    void handle_leave_event(CEvent&);
    void do_layout();
    void focus_previous_widget();
    void focus_next_widget();

    GWindow* m_window { nullptr };
    OwnPtr<GLayout> m_layout;

    Rect m_relative_rect;
    Color m_background_color;
    Color m_foreground_color;
    RetainPtr<Font> m_font;
    String m_tooltip;

    SizePolicy m_horizontal_size_policy { SizePolicy::Fill };
    SizePolicy m_vertical_size_policy { SizePolicy::Fill };
    Size m_preferred_size;

    bool m_fill_with_background_color { false };
    bool m_visible { true };
    bool m_greedy_for_hits { false };
    bool m_enabled { true };
    bool m_layout_dirty { false };
    bool m_updates_enabled { true };

    HashMap<GShortcut, GAction*> m_local_shortcut_actions;
};
