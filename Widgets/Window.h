#pragma once

#include <AK/String.h>
#include "Object.h"
#include "Rect.h"

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

    Widget* mainWidget() { return m_mainWidget; }
    const Widget* mainWidget() const { return m_mainWidget; }

    void setMainWidget(Widget*);

    virtual void event(Event&) override;

private:
    String m_title;
    Rect m_rect;
    Widget* m_mainWidget { nullptr };
};

