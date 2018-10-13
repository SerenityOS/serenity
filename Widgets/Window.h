#pragma once

#include "Object.h"
#include "Rect.h"
#include <AK/String.h>
#include <AK/WeakPtr.h>

class Widget;

class Window final : public Object {
public:
    explicit Window(Object* parent = nullptr);
    virtual ~Window() override;

    String title() const { return m_title; }
    void setTitle(String&&);

    int x() const { return m_rect.x(); }
    int y() const { return m_rect.y(); }
    int width() const { return m_rect.width(); }
    int height() const { return m_rect.height(); }

    const Rect& rect() const { return m_rect; }
    void setRect(const Rect&);

    Point position() const { return m_rect.location(); }
    void setPosition(const Point& position) { setRect({ position.x(), position.y(), width(), height() }); }

    Widget* mainWidget() { return m_mainWidget; }
    const Widget* mainWidget() const { return m_mainWidget; }

    void setMainWidget(Widget*);

    virtual void event(Event&) override;

    bool isBeingDragged() const { return m_isBeingDragged; }
    void setIsBeingDragged(bool b) { m_isBeingDragged = b; }

    void repaint();

    bool isActive() const;

    Widget* focusedWidget() { return m_focusedWidget.ptr(); }
    const Widget* focusedWidget() const { return m_focusedWidget.ptr(); }
    void setFocusedWidget(Widget*);

private:
    String m_title;
    Rect m_rect;
    Widget* m_mainWidget { nullptr };
    bool m_isBeingDragged { false };

    WeakPtr<Widget> m_focusedWidget;
};

