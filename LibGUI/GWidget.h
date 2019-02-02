#pragma once

#include "GEvent.h"
#include "GObject.h"
#include <SharedGraphics/Rect.h>
#include <SharedGraphics/Color.h>
#include <SharedGraphics/Font.h>
#include <AK/AKString.h>

class GraphicsBitmap;
class GWindow;

class GWidget : public GObject {
public:
    explicit GWidget(GWidget* parent = nullptr);
    virtual ~GWidget() override;

    virtual void event(GEvent&) override;
    virtual void paint_event(GPaintEvent&);
    virtual void show_event(GShowEvent&);
    virtual void hide_event(GHideEvent&);
    virtual void keydown_event(GKeyEvent&);
    virtual void keyup_event(GKeyEvent&);
    virtual void mousemove_event(GMouseEvent&);
    virtual void mousedown_event(GMouseEvent&);
    virtual void mouseup_event(GMouseEvent&);
    virtual void focusin_event(GEvent&);
    virtual void focusout_event(GEvent&);

    Rect relative_rect() const { return m_relative_rect; }
    Point relative_position() const { return m_relative_rect.location(); }

    int x() const { return m_relative_rect.x(); }
    int y() const { return m_relative_rect.y(); }
    int width() const { return m_relative_rect.width(); }
    int height() const { return m_relative_rect.height(); }

    Rect rect() const { return { 0, 0, width(), height() }; }
    Size size() const { return m_relative_rect.size(); }

    void update();
    void repaint(const Rect&);

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
    void move_to(const Point& point) { set_relative_rect({ point, relative_rect().size() }); }

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

    GWidget* parent_widget() { return static_cast<GWidget*>(parent()); }
    const GWidget* parent_widget() const { return static_cast<const GWidget*>(parent()); }

    void set_fill_with_background_color(bool b) { m_fill_with_background_color = b; }
    bool fill_with_background_color() const { return m_fill_with_background_color; }

    const Font& font() const { return *m_font; }
    void set_font(RetainPtr<Font>&&);

    void set_global_cursor_tracking(bool);
    bool global_cursor_tracking() const;

private:
    GWindow* m_window { nullptr };

    Rect m_relative_rect;
    Color m_background_color { 0xffffff };
    Color m_foreground_color { 0x000000 };
    RetainPtr<Font> m_font;

    bool m_has_pending_paint_event { false };
    bool m_fill_with_background_color { true };
};
