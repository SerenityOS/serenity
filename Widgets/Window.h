#pragma once

#include "Object.h"
#include "Rect.h"
#include "GraphicsBitmap.h"
#include <AK/AKString.h>
#include <AK/InlineLinkedList.h>
#include <AK/WeakPtr.h>

class Process;
class Widget;

class Window final : public Object, public InlineLinkedListNode<Window> {
public:
    Window(Process&, int window_id);
    virtual ~Window() override;

    int window_id() const { return m_window_id; }

    String title() const { return m_title; }
    void setTitle(String&&);

    int x() const { return m_rect.x(); }
    int y() const { return m_rect.y(); }
    int width() const { return m_rect.width(); }
    int height() const { return m_rect.height(); }

    const Rect& rect() const { return m_rect; }
    void setRect(const Rect&);
    void setRectWithoutRepaint(const Rect& rect) { m_rect = rect; }

    Point position() const { return m_rect.location(); }
    void setPosition(const Point& position) { setRect({ position.x(), position.y(), width(), height() }); }
    void setPositionWithoutRepaint(const Point& position) { setRectWithoutRepaint({ position.x(), position.y(), width(), height() }); }

    virtual void event(Event&) override;

    bool isBeingDragged() const { return m_isBeingDragged; }
    void setIsBeingDragged(bool b) { m_isBeingDragged = b; }

    bool isActive() const;

    bool isVisible() const;

    void close();

    GraphicsBitmap* backing() { return m_backing.ptr(); }

    void did_paint();

    // For InlineLinkedList.
    // FIXME: Maybe make a ListHashSet and then WindowManager can just use that.
    Window* m_next { nullptr };
    Window* m_prev { nullptr };

private:
    String m_title;
    Rect m_rect;
    bool m_isBeingDragged { false };

    RetainPtr<GraphicsBitmap> m_backing;
    Process& m_process;
    int m_window_id { -1 };
};

