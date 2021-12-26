#include "GWidget.h"
#include "GEvent.h"
#include "GEventLoop.h"
#include "GWindow.h"
#include <LibGUI/GLayout.h>
#include <AK/Assertions.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GMenu.h>
#include <unistd.h>

GWidget::GWidget(GWidget* parent)
    : CObject(parent, true)
{
    set_font(nullptr);
    m_background_color = Color::LightGray;
    m_foreground_color = Color::Black;
}

GWidget::~GWidget()
{
}

void GWidget::child_event(CChildEvent& event)
{
    if (event.type() == GEvent::ChildAdded) {
        if (event.child() && event.child()->is_widget() && layout())
            layout()->add_widget(static_cast<GWidget&>(*event.child()));
    }
    if (event.type() == GEvent::ChildRemoved) {
        if (layout()) {
            if (event.child() && event.child()->is_widget())
                layout()->remove_widget(static_cast<GWidget&>(*event.child()));
            else
                invalidate_layout();
        }
        update();
    }
    return CObject::child_event(event);
}

void GWidget::set_relative_rect(const Rect& rect)
{
    if (rect == m_relative_rect)
        return;

    auto old_rect = m_relative_rect;

    bool size_changed = m_relative_rect.size() != rect.size();
    m_relative_rect = rect;

    if (size_changed) {
        GResizeEvent resize_event(m_relative_rect.size(), rect.size());
        event(resize_event);
    }

    if (auto* parent = parent_widget())
        parent->update(old_rect);
    update();
}

void GWidget::event(CEvent& event)
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
        return handle_mousedown_event(static_cast<GMouseEvent&>(event));
    case GEvent::MouseDoubleClick:
        return handle_mousedoubleclick_event(static_cast<GMouseEvent&>(event));
    case GEvent::MouseUp:
        return handle_mouseup_event(static_cast<GMouseEvent&>(event));
    case GEvent::MouseWheel:
        return mousewheel_event(static_cast<GMouseEvent&>(event));
    case GEvent::Enter:
        return handle_enter_event(event);
    case GEvent::Leave:
        return handle_leave_event(event);
    default:
        return CObject::event(event);
    }
}

void GWidget::handle_paint_event(GPaintEvent& event)
{
    ASSERT(is_visible());
    if (fill_with_background_color()) {
        GPainter painter(*this);
        painter.fill_rect(event.rect(), background_color());
    } else {
#ifdef DEBUG_WIDGET_UNDERDRAW
        // FIXME: This is a bit broken.
        // If the widget is not opaque, let's not mess it up with debugging color.
        GPainter painter(*this);
        painter.fill_rect(rect(), Color::Red);
#endif
    }
    paint_event(event);
    for (auto* ch : children()) {
        auto* child = (GWidget*)ch;
        if (!child->is_visible())
            continue;
        if (child->relative_rect().intersects(event.rect())) {
            GPaintEvent local_event(event.rect().intersected(child->relative_rect()).translated(-child->relative_position()));
            child->event(local_event);
        }
    }
    second_paint_event(event);
}

void GWidget::set_layout(OwnPtr<GLayout>&& layout)
{
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
    invalidate_layout();
}

void GWidget::handle_resize_event(GResizeEvent& event)
{
    if (layout())
        do_layout();
    return resize_event(event);
}

void GWidget::handle_mouseup_event(GMouseEvent& event)
{
    mouseup_event(event);
}

void GWidget::handle_mousedown_event(GMouseEvent& event)
{
    if (accepts_focus())
        set_focus(true);
    mousedown_event(event);
    if (event.button() == GMouseButton::Right) {
        GContextMenuEvent c_event(event.position(), screen_relative_rect().location().translated(event.position()));
        context_menu_event(c_event);
    }
}

void GWidget::handle_mousedoubleclick_event(GMouseEvent& event)
{
    doubleclick_event(event);
}

void GWidget::handle_enter_event(CEvent& event)
{
    if (has_tooltip())
        GApplication::the().show_tooltip(m_tooltip, screen_relative_rect().center().translated(0, height() / 2));
    enter_event(event);
}

void GWidget::handle_leave_event(CEvent& event)
{
    GApplication::the().hide_tooltip();
    leave_event(event);
}

void GWidget::click_event(GMouseEvent&)
{
}

void GWidget::doubleclick_event(GMouseEvent&)
{
}

void GWidget::resize_event(GResizeEvent&)
{
}

void GWidget::paint_event(GPaintEvent&)
{
}

void GWidget::second_paint_event(GPaintEvent&)
{
}

void GWidget::show_event(GShowEvent&)
{
}

void GWidget::hide_event(GHideEvent&)
{
}

void GWidget::keydown_event(GKeyEvent& event)
{
    if (!event.alt() && !event.ctrl() && !event.logo() && event.key() == KeyCode::Key_Tab) {
        if (event.shift())
            focus_previous_widget();
        else
            focus_next_widget();
    }
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

void GWidget::mousewheel_event(GMouseEvent&)
{
}

void GWidget::context_menu_event(GContextMenuEvent&)
{
}

void GWidget::focusin_event(CEvent&)
{
}

void GWidget::focusout_event(CEvent&)
{
}

void GWidget::enter_event(CEvent&)
{
}

void GWidget::leave_event(CEvent&)
{
}

void GWidget::update()
{
    if (rect().is_empty())
        return;
    update(rect());
}

void GWidget::update(const Rect& rect)
{
    if (!is_visible())
        return;

    if (!updates_enabled())
        return;

    GWindow* window = m_window;
    GWidget* parent = parent_widget();
    while (parent) {
        if (!parent->updates_enabled())
            return;
        window = parent->m_window;
        parent = parent->parent_widget();
    }
    if (window)
        window->update(rect.translated(window_relative_rect().location()));
}

Rect GWidget::window_relative_rect() const
{
    auto rect = relative_rect();
    for (auto* parent = parent_widget(); parent; parent = parent->parent_widget()) {
        rect.move_by(parent->relative_position());
    }
    return rect;
}

Rect GWidget::screen_relative_rect() const
{
    return window_relative_rect().translated(window()->position());
}

GWidget* GWidget::child_at(const Point& point) const
{
    for (int i = children().size() - 1; i >= 0; --i) {
        if (!children()[i]->is_widget())
            continue;
        auto& child = *(GWidget*)children()[i];
        if (!child.is_visible())
            continue;
        if (child.relative_rect().contains(point))
            return &child;
    }
    return nullptr;
}

GWidget::HitTestResult GWidget::hit_test(const Point& position)
{
    if (is_greedy_for_hits())
        return { this, position };
    if (auto* child = child_at(position))
        return child->hit_test(position - child->relative_position());
    return { this, position };
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
    update();
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
    if (m_layout_dirty)
        return;
    m_layout_dirty = true;
    deferred_invoke([this] (auto&) {
        m_layout_dirty = false;
        auto* w = window();
        if (!w)
            return;
        if (!w->main_widget())
            return;
        do_layout();
        w->main_widget()->do_layout();
    });
}

void GWidget::set_visible(bool visible)
{
    if (visible == m_visible)
        return;
    m_visible = visible;
    if (auto* parent = parent_widget())
        parent->invalidate_layout();
    if (m_visible)
        update();
}

bool GWidget::spans_entire_window_horizontally() const
{
    auto* w = window();
    if (!w)
        return false;
    auto* main_widget = w->main_widget();
    if (!main_widget)
        return false;
    if (main_widget == this)
        return true;
    auto wrr = window_relative_rect();
    return wrr.left() == main_widget->rect().left() && wrr.right() == main_widget->rect().right();
}

void GWidget::set_enabled(bool enabled)
{
    if (m_enabled == enabled)
        return;
    m_enabled = enabled;
    update();
}

void GWidget::move_to_front()
{
    auto* parent = parent_widget();
    if (!parent)
        return;
    if (parent->children().size() == 1)
        return;
    parent->children().remove_first_matching([this] (auto& entry) {
        return entry == this;
    });
    parent->children().append(this);
    parent->update();
}

void GWidget::move_to_back()
{
    auto* parent = parent_widget();
    if (!parent)
        return;
    if (parent->children().size() == 1)
        return;
    parent->children().remove_first_matching([this] (auto& entry) {
        return entry == this;
    });
    parent->children().prepend(this);
    parent->update();
}

bool GWidget::is_frontmost() const
{
    auto* parent = parent_widget();
    if (!parent)
        return true;
    return parent->children().last() == this;
}

bool GWidget::is_backmost() const
{
    auto* parent = parent_widget();
    if (!parent)
        return true;
    return parent->children().first() == this;
}

GAction* GWidget::action_for_key_event(const GKeyEvent& event)
{
    auto it = m_local_shortcut_actions.find(GShortcut(event.modifiers(), (KeyCode)event.key()));
    if (it == m_local_shortcut_actions.end())
        return nullptr;
    return (*it).value;
}

void GWidget::register_local_shortcut_action(Badge<GAction>, GAction& action)
{
    m_local_shortcut_actions.set(action.shortcut(), &action);
}

void GWidget::unregister_local_shortcut_action(Badge<GAction>, GAction& action)
{
    m_local_shortcut_actions.remove(action.shortcut());
}

void GWidget::set_updates_enabled(bool enabled)
{
    if (m_updates_enabled == enabled)
        return;
    m_updates_enabled = enabled;
    if (enabled)
        update();
}

void GWidget::focus_previous_widget()
{
    auto focusable_widgets = window()->focusable_widgets();
    for (int i = focusable_widgets.size() - 1; i >= 0; --i) {
        if (focusable_widgets[i] != this)
            continue;
        if (i > 0)
            focusable_widgets[i - 1]->set_focus(true);
        else
            focusable_widgets.last()->set_focus(true);
    }
}

void GWidget::focus_next_widget()
{
    auto focusable_widgets = window()->focusable_widgets();
    for (int i = 0; i < focusable_widgets.size(); ++i) {
        if (focusable_widgets[i] != this)
            continue;
        if (i < focusable_widgets.size() - 1)
            focusable_widgets[i + 1]->set_focus(true);
        else
            focusable_widgets.first()->set_focus(true);
    }
}
