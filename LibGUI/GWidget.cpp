#include "GWidget.h"
#include "GEvent.h"
#include "GEventLoop.h"
#include "GWindow.h"
#include <AK/Assertions.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>

GWidget::GWidget(GWidget* parent)
    : GObject(parent)
{
    setFont(nullptr);
    m_backgroundColor = Color::White;
    m_foregroundColor = Color::Black;
}

GWidget::~GWidget()
{
}

void GWidget::setWindowRelativeRect(const Rect& rect, bool should_update)
{
    // FIXME: Make some kind of event loop driven ResizeEvent?
    m_relativeRect = rect;
    if (should_update)
        update();
}

void GWidget::repaint(const Rect& rect)
{
    // FIXME: Implement.
}

void GWidget::event(GEvent& event)
{
    switch (event.type()) {
    case GEvent::Paint:
        m_hasPendingPaintEvent = false;
        if (auto* win = window()) {
            if (!win->is_visible())
                return;
        }
        return paintEvent(static_cast<GPaintEvent&>(event));
    case GEvent::Show:
        return showEvent(static_cast<GShowEvent&>(event));
    case GEvent::Hide:
        return hideEvent(static_cast<GHideEvent&>(event));
    case GEvent::KeyDown:
        return keyDownEvent(static_cast<GKeyEvent&>(event));
    case GEvent::KeyUp:
        return keyUpEvent(static_cast<GKeyEvent&>(event));
    case GEvent::MouseMove:
        return mouseMoveEvent(static_cast<GMouseEvent&>(event));
    case GEvent::MouseDown:
        // FIXME: Focus self if needed.
        return mouseDownEvent(static_cast<GMouseEvent&>(event));
    case GEvent::MouseUp:
        return mouseUpEvent(static_cast<GMouseEvent&>(event));
    default:
        return GObject::event(event);
    }
}

void GWidget::paintEvent(GPaintEvent& event)
{
    //printf("GWidget::paintEvent :)\n");
    if (fillWithBackgroundColor()) {
        Painter painter(*this);
        painter.fill_rect(rect(), backgroundColor());
    }
    for (auto* ch : children()) {
        auto* child = (GWidget*)ch;
        child->event(event);
    }
}

void GWidget::showEvent(GShowEvent&)
{
}

void GWidget::hideEvent(GHideEvent&)
{
}

void GWidget::keyDownEvent(GKeyEvent&)
{
}

void GWidget::keyUpEvent(GKeyEvent&)
{
}

void GWidget::mouseDownEvent(GMouseEvent&)
{
}

void GWidget::mouseUpEvent(GMouseEvent&)
{
}

void GWidget::mouseMoveEvent(GMouseEvent&)
{
}

void GWidget::update()
{
    auto* w = window();
    if (!w)
        return;
    if (m_hasPendingPaintEvent)
        return;
    m_hasPendingPaintEvent = true;
    GEventLoop::main().post_event(w, make<GPaintEvent>(relativeRect()));
}

GWidget::HitTestResult GWidget::hitTest(int x, int y)
{
    // FIXME: Care about z-order.
    for (auto* ch : children()) {
        auto* child = (GWidget*)ch;
        if (child->relativeRect().contains(x, y)) {
            return child->hitTest(x - child->relativeRect().x(), y - child->relativeRect().y());
        }
    }
    return { this, x, y };
}

void GWidget::setWindow(GWindow* window)
{
    if (m_window == window)
        return;
    m_window = window;
}

bool GWidget::isFocused() const
{
    // FIXME: Implement.
    return false;
}

void GWidget::setFocus(bool focus)
{
    if (focus == isFocused())
        return;
    // FIXME: Implement.
}

void GWidget::setFont(RetainPtr<Font>&& font)
{
    if (!font)
        m_font = Font::default_font();
    else
        m_font = move(font);
}

GraphicsBitmap* GWidget::backing()
{
    if (auto* w = window())
        return w->backing();
    return nullptr;
}
