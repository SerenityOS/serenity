#pragma once

#include "Object.h"
#include "Rect.h"
#include "GraphicsBitmap.h"
#include <AK/AKString.h>
#include <AK/InlineLinkedList.h>

class Process;

class Window final : public Object, public InlineLinkedListNode<Window> {
public:
    Window(Process&, int window_id);
    virtual ~Window() override;

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
    void set_position(const Point& position) { set_rect({ position.x(), position.y(), width(), height() }); }
    void set_position_without_repaint(const Point& position) { set_rect_without_repaint({ position.x(), position.y(), width(), height() }); }

    virtual void event(Event&) override;

    bool is_being_dragged() const { return m_is_being_dragged; }
    void set_is_being_dragged(bool b) { m_is_being_dragged = b; }

    bool is_visible() const;

    void close();

    GraphicsBitmap* backing() { return m_backing.ptr(); }

    // For InlineLinkedList.
    // FIXME: Maybe make a ListHashSet and then WindowManager can just use that.
    Window* m_next { nullptr };
    Window* m_prev { nullptr };

private:
    String m_title;
    Rect m_rect;
    bool m_is_being_dragged { false };

    RetainPtr<GraphicsBitmap> m_backing;
    Process& m_process;
    int m_window_id { -1 };
};

