#pragma once

#include "GObject.h"
#include <SharedGraphics/Rect.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <AK/AKString.h>

class GWidget;

class GWindow final : public GObject {
public:
    GWindow(GObject* parent = nullptr);
    virtual ~GWindow() override;

    static GWindow* from_window_id(int);

    int window_id() const { return m_window_id; }

    String title() const { return m_title; }
    void set_title(String&&);

    int x() const { return m_rect.x(); }
    int y() const { return m_rect.y(); }
    int width() const { return m_rect.width(); }
    int height() const { return m_rect.height(); }

    const Rect& rect() const { return m_rect; }
    void set_rect(const Rect&);
    void set_rect_without_repaint(const Rect& rect) { m_rect = rect; }

    Point position() const { return m_rect.location(); }
    void set_position_without_repaint(const Point& position) { set_rect_without_repaint({ position.x(), position.y(), width(), height() }); }

    virtual void event(GEvent&) override;

    bool is_visible() const;

    void close();

    GWidget* main_widget() { return m_main_widget; }
    const GWidget* main_widget() const { return m_main_widget; }
    void set_main_widget(GWidget*);

    GraphicsBitmap* backing() { return m_backing.ptr(); }

    void show();

    void update();

private:
    String m_title;
    Rect m_rect;

    RetainPtr<GraphicsBitmap> m_backing;
    int m_window_id { -1 };
    GWidget* m_main_widget { nullptr };
};

