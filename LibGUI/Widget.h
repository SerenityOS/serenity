#pragma once

#include "Event.h"
#include "Object.h"
#include <SharedGraphics/Rect.h>
#include <SharedGraphics/Color.h>
#include <SharedGraphics/Font.h>
#include <AK/AKString.h>

class GraphicsBitmap;
class Window;

class Widget : public Object {
public:
    explicit Widget(Widget* parent = nullptr);
    virtual ~Widget();

    virtual void event(Event&) override;
    virtual void paintEvent(PaintEvent&);
    virtual void showEvent(ShowEvent&);
    virtual void hideEvent(HideEvent&);
    virtual void keyDownEvent(KeyEvent&);
    virtual void keyUpEvent(KeyEvent&);
    virtual void mouseMoveEvent(MouseEvent&);
    virtual void mouseDownEvent(MouseEvent&);
    virtual void mouseUpEvent(MouseEvent&);

    Rect relativeRect() const { return m_relativeRect; }
    Point relativePosition() const { return m_relativeRect.location(); }

    int x() const { return m_relativeRect.x(); }
    int y() const { return m_relativeRect.y(); }
    int width() const { return m_relativeRect.width(); }
    int height() const { return m_relativeRect.height(); }

    Rect rect() const { return { 0, 0, width(), height() }; }
    Size size() const { return m_relativeRect.size(); }

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

    virtual const char* class_name() const override { return "Widget"; }

    void setWindowRelativeRect(const Rect&, bool should_update = true);

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

    void setFillWithBackgroundColor(bool b) { m_fillWithBackgroundColor = b; }
    bool fillWithBackgroundColor() const { return m_fillWithBackgroundColor; }

    const Font& font() const { return *m_font; }
    void setFont(RetainPtr<Font>&&);

    virtual GraphicsBitmap* backing();

private:
    Window* m_window { nullptr };

    Rect m_relativeRect;
    Color m_backgroundColor { 0xffffff };
    Color m_foregroundColor { 0x000000 };
    RetainPtr<Font> m_font;

    bool m_hasPendingPaintEvent { false };
    bool m_fillWithBackgroundColor { true };
};
