#pragma once

#include "GEvent.h"
#include "GObject.h"
#include <SharedGraphics/Rect.h>
#include <SharedGraphics/Color.h>
#include <SharedGraphics/Font.h>
#include <AK/Badge.h>
#include <AK/AKString.h>

class GraphicsBitmap;
class GLayout;
class GWindow;

enum class SizePolicy { Fixed, Fill };
enum class Orientation { Horizontal, Vertical };
enum class HorizontalDirection { Left, Right };
enum class VerticalDirection { Up, Down };

class GWidget : public GObject {
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

    virtual void event(GEvent&) override;
    virtual void paint_event(GPaintEvent&);
    virtual void resize_event(GResizeEvent&);
    virtual void show_event(GShowEvent&);
    virtual void hide_event(GHideEvent&);
    virtual void keydown_event(GKeyEvent&);
    virtual void keyup_event(GKeyEvent&);
    virtual void mousemove_event(GMouseEvent&);
    virtual void mousedown_event(GMouseEvent&);
    virtual void mouseup_event(GMouseEvent&);
    virtual void focusin_event(GEvent&);
    virtual void focusout_event(GEvent&);
    virtual void enter_event(GEvent&);
    virtual void leave_event(GEvent&);
    virtual void child_event(GChildEvent&) override;

    Rect relative_rect() const { return m_relative_rect; }
    Point relative_position() const { return m_relative_rect.location(); }

    Rect window_relative_rect() const;

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
        int localX { 0 };
        int localY { 0 };
    };
    HitTestResult hit_test(int x, int y);

    virtual const char* class_name() const override { return "GWidget"; }

    void set_relative_rect(const Rect&);
    void set_relative_rect(int x, int y, int width, int height) { set_relative_rect({ x, y, width, height }); }

    void move_to(const Point& point) { set_relative_rect({ point, relative_rect().size() }); }
    void move_to(int x, int y) { move_to({ x, y }); }
    void resize(const Size& size) { set_relative_rect({ relative_rect().location(), size }); }
    void resize(int width, int height) { resize({ width, height }); }

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

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

private:
    virtual bool is_widget() const final { return true; }

    void handle_paint_event(GPaintEvent&);
    void handle_resize_event(GResizeEvent&);
    void do_layout();
    void invalidate_layout();

    GWindow* m_window { nullptr };
    OwnPtr<GLayout> m_layout;

    Rect m_relative_rect;
    Color m_background_color;
    Color m_foreground_color;
    RetainPtr<Font> m_font;

    SizePolicy m_horizontal_size_policy { SizePolicy::Fill };
    SizePolicy m_vertical_size_policy { SizePolicy::Fill };
    Size m_preferred_size;

    bool m_fill_with_background_color { false };
    bool m_visible { true };
};
