#pragma once

#include "Event.h"
#include "Object.h"
#include "Rect.h"
#include "Color.h"
#include <AK/String.h>

class Window;

class Widget : public Object {
public:
    explicit Widget(Widget* parent = nullptr);
    virtual ~Widget();

    virtual void event(Event&);
    virtual void onPaint(PaintEvent&);
    virtual void onShow(ShowEvent&);
    virtual void onHide(HideEvent&);
    virtual void onKeyDown(KeyEvent&);
    virtual void onKeyUp(KeyEvent&);
    virtual void onMouseMove(MouseEvent&);
    virtual void onMouseDown(MouseEvent&);
    virtual void onMouseUp(MouseEvent&);

    Rect relativeRect() const { return m_relativeRect; }
    Point relativePosition() const { return m_relativeRect.location(); }

    int x() const { return m_relativeRect.x(); }
    int y() const { return m_relativeRect.y(); }
    int width() const { return m_relativeRect.width(); }
    int height() const { return m_relativeRect.height(); }

    Rect rect() const { return { 0, 0, width(), height() }; }

    void update();
    void repaint(const Rect&);

    bool isFocused() const;
    void setFocus(bool);

    struct HitTestResult {
        Widget* widget { nullptr };
        int localX { 0 };
        int localY { 0 };
    };
    HitTestResult hitTest(int x, int y);

    virtual const char* className() const override { return "Widget"; }

    void setWindowRelativeRect(const Rect&);

    Color backgroundColor() const { return m_backgroundColor; }
    Color foregroundColor() const { return m_foregroundColor; }

    void setBackgroundColor(Color color) { m_backgroundColor = color; }
    void setForegroundColor(Color color) { m_foregroundColor = color; }

    Window* window()
    {
        if (auto* pw = parentWidget())
            return pw->window();
        return m_window;
    }

    const Window* window() const
    {
        if (auto* pw = parentWidget())
            return pw->window();
        return m_window;
    }

    void setWindow(Window*);

    Widget* parentWidget() { return static_cast<Widget*>(parent()); }
    const Widget* parentWidget() const { return static_cast<const Widget*>(parent()); }

private:
    Window* m_window { nullptr };

    Rect m_relativeRect;
    Color m_backgroundColor;
    Color m_foregroundColor;

    bool m_hasPendingPaintEvent { false };
};
