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
    virtual ~GWidget();

    virtual void event(GEvent&) override;
    virtual void paintEvent(GPaintEvent&);
    virtual void showEvent(GShowEvent&);
    virtual void hideEvent(GHideEvent&);
    virtual void keyDownEvent(GKeyEvent&);
    virtual void keyUpEvent(GKeyEvent&);
    virtual void mouseMoveEvent(GMouseEvent&);
    virtual void mouseDownEvent(GMouseEvent&);
    virtual void mouseUpEvent(GMouseEvent&);

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
        GWidget* widget { nullptr };
        int localX { 0 };
        int localY { 0 };
    };
    HitTestResult hitTest(int x, int y);

    virtual const char* class_name() const override { return "GWidget"; }

    void setWindowRelativeRect(const Rect&, bool should_update = true);

    Color backgroundColor() const { return m_backgroundColor; }
    Color foregroundColor() const { return m_foregroundColor; }

    void setBackgroundColor(Color color) { m_backgroundColor = color; }
    void setForegroundColor(Color color) { m_foregroundColor = color; }

    GWindow* window()
    {
        if (auto* pw = parentWidget())
            return pw->window();
        return m_window;
    }

    const GWindow* window() const
    {
        if (auto* pw = parentWidget())
            return pw->window();
        return m_window;
    }

    void set_window(GWindow*);

    GWidget* parentWidget() { return static_cast<GWidget*>(parent()); }
    const GWidget* parentWidget() const { return static_cast<const GWidget*>(parent()); }

    void setFillWithBackgroundColor(bool b) { m_fillWithBackgroundColor = b; }
    bool fillWithBackgroundColor() const { return m_fillWithBackgroundColor; }

    const Font& font() const { return *m_font; }
    void setFont(RetainPtr<Font>&&);

    GraphicsBitmap* backing();

private:
    GWindow* m_window { nullptr };

    Rect m_relativeRect;
    Color m_backgroundColor { 0xffffff };
    Color m_foregroundColor { 0x000000 };
    RetainPtr<Font> m_font;

    bool m_hasPendingPaintEvent { false };
    bool m_fillWithBackgroundColor { true };
};
