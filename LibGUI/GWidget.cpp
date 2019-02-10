#include "GWidget.h"
#include "GEvent.h"
#include "GEventLoop.h"
#include "GWindow.h"
#include <LibGUI/GLayout.h>
#include <AK/Assertions.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>

GWidget::GWidget(GWidget* parent)
    : GObject(parent)
{
    set_font(nullptr);
    m_background_color = Color::LightGray;
    m_foreground_color = Color::Black;

    if (parent && parent->layout())
        parent->layout()->add_widget(*this);
}

GWidget::~GWidget()
{
}

void GWidget::set_relative_rect(const Rect& rect)
{
    if (rect == m_relative_rect)
        return;
    if (m_relative_rect.size() != rect.size()) {
        auto event = make<GResizeEvent>(m_relative_rect.size(), rect.size());
        GEventLoop::main().post_event(this, move(event));
    }
    m_relative_rect = rect;
    update();
}

void GWidget::event(GEvent& event)
{
    switch (event.type()) {
    case GEvent::Paint:
        return handle_paint_event(static_cast<GPaintEvent&>(event));
    case GEvent::Resize:
        return handle_resize_event(static_cast<GResizeEvent&>(event));
    case GEvent::FocusIn:
        return focusin_event(event);
    case GEvent::FocusOut:
        return focusout_event(event);
    case GEvent::Show:
        return show_event(static_cast<GShowEvent&>(event));
    case GEvent::Hide:
        return hide_event(static_cast<GHideEvent&>(event));
    case GEvent::KeyDown:
        return keydown_event(static_cast<GKeyEvent&>(event));
    case GEvent::KeyUp:
        return keyup_event(static_cast<GKeyEvent&>(event));
    case GEvent::MouseMove:
        return mousemove_event(static_cast<GMouseEvent&>(event));
    case GEvent::MouseDown:
        if (accepts_focus())
            set_focus(true);
        return mousedown_event(static_cast<GMouseEvent&>(event));
    case GEvent::MouseUp:
        return mouseup_event(static_cast<GMouseEvent&>(event));
    default:
        return GObject::event(event);
    }
}

void GWidget::handle_paint_event(GPaintEvent& event)
{
    if (fill_with_background_color()) {
        Painter painter(*this);
        painter.fill_rect(event.rect(), background_color());
    }
    paint_event(event);
    for (auto* ch : children()) {
        auto* child = (GWidget*)ch;
        if (child->relative_rect().intersects(event.rect())) {
            auto local_rect = event.rect();
            local_rect.intersect(child->relative_rect());
            local_rect.move_by(-child->relative_rect().x(), -child->relative_rect().y());
            GPaintEvent local_event(local_rect);
            child->event(local_event);
        }
    }
}

void GWidget::set_layout(OwnPtr<GLayout>&& layout)
{
    if (m_layout.ptr() == layout.ptr())
        return;
    if (m_layout)
        m_layout->notify_disowned(Badge<GWidget>(), *this);
    m_layout = move(layout);
    if (m_layout) {
        m_layout->notify_adopted(Badge<GWidget>(), *this);
        do_layout();
    } else {
        update();
    }
}

void GWidget::do_layout()
{
    if (!m_layout)
        return;
    m_layout->run(*this);
    update();
}

void GWidget::notify_layout_changed(Badge<GLayout>)
{
    do_layout();
}

void GWidget::handle_resize_event(GResizeEvent& event)
{
    if (layout())
        do_layout();
    return resize_event(event);
}

void GWidget::resize_event(GResizeEvent&)
{
}

void GWidget::paint_event(GPaintEvent&)
{
}

void GWidget::show_event(GShowEvent&)
{
}

void GWidget::hide_event(GHideEvent&)
{
}

void GWidget::keydown_event(GKeyEvent&)
{
}

void GWidget::keyup_event(GKeyEvent&)
{
}

void GWidget::mousedown_event(GMouseEvent&)
{
}

void GWidget::mouseup_event(GMouseEvent&)
{
}

void GWidget::mousemove_event(GMouseEvent&)
{
}

void GWidget::focusin_event(GEvent&)
{
}

void GWidget::focusout_event(GEvent&)
{
}

void GWidget::update()
{
    update(rect());
}

void GWidget::update(const Rect& rect)
{
    auto* w = window();
    if (!w)
        return;
    w->update(rect.translated(window_relative_rect().location()));
}

Rect GWidget::window_relative_rect() const
{
    auto rect = relative_rect();
    for (auto* parent = parent_widget(); parent; parent = parent->parent_widget()) {
        rect.move_by(parent->relative_position());
    }
    return rect;
}

GWidget::HitTestResult GWidget::hit_test(int x, int y)
{
    // FIXME: Care about z-order.
    for (auto* ch : children()) {
        auto* child = (GWidget*)ch;
        if (child->relative_rect().contains(x, y)) {
            return child->hit_test(x - child->relative_rect().x(), y - child->relative_rect().y());
        }
    }
    return { this, x, y };
}

void GWidget::set_window(GWindow* window)
{
    if (m_window == window)
        return;
    m_window = window;
}

bool GWidget::is_focused() const
{
    auto* win = window();
    if (!win)
        return false;
    if (!win->is_active())
        return false;
    return win->focused_widget() == this;
}

void GWidget::set_focus(bool focus)
{
    auto* win = window();
    if (!win)
        return;
    if (focus) {
        win->set_focused_widget(this);
    } else {
        if (win->focused_widget() == this)
            win->set_focused_widget(nullptr);
    }
}

void GWidget::set_font(RetainPtr<Font>&& font)
{
    if (!font)
        m_font = Font::default_font();
    else
        m_font = move(font);
}

void GWidget::set_global_cursor_tracking(bool enabled)
{
    auto* win = window();
    if (!win)
        return;
    win->set_global_cursor_tracking_widget(enabled ? this : nullptr);
}

bool GWidget::global_cursor_tracking() const
{
    auto* win = window();
    if (!win)
        return false;
    return win->global_cursor_tracking_widget() == this;
}

void GWidget::set_preferred_size(const Size& size)
{
    if (m_preferred_size == size)
        return;
    m_preferred_size = size;
    invalidate_layout();
}

void GWidget::set_size_policy(SizePolicy horizontal_policy, SizePolicy vertical_policy)
{
    if (m_horizontal_size_policy == horizontal_policy && m_vertical_size_policy == vertical_policy)
        return;
    m_horizontal_size_policy = horizontal_policy;
    m_vertical_size_policy = vertical_policy;
    invalidate_layout();
}

void GWidget::invalidate_layout()
{
    auto* w = window();
    if (!w)
        return;
    if (!w->main_widget())
        return;
    w->main_widget()->do_layout();
}
