/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/JsonObject.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Event.h>
#include <LibGUI/GMLParser.h>
#include <LibGUI/Layout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <unistd.h>

REGISTER_WIDGET(GUI, Widget)

namespace GUI {

static HashMap<String, WidgetClassRegistration*>& widget_classes()
{
    static HashMap<String, WidgetClassRegistration*>* map;
    if (!map)
        map = new HashMap<String, WidgetClassRegistration*>;
    return *map;
}

WidgetClassRegistration::WidgetClassRegistration(const String& class_name, Function<NonnullRefPtr<Widget>()> factory)
    : m_class_name(class_name)
    , m_factory(move(factory))
{
    widget_classes().set(class_name, this);
}

WidgetClassRegistration::~WidgetClassRegistration()
{
}

void WidgetClassRegistration::for_each(Function<void(const WidgetClassRegistration&)> callback)
{
    for (auto& it : widget_classes()) {
        callback(*it.value);
    }
}

const WidgetClassRegistration* WidgetClassRegistration::find(const String& class_name)
{
    return widget_classes().get(class_name).value_or(nullptr);
}

Widget::Widget()
    : Core::Object(nullptr)
    , m_background_role(Gfx::ColorRole::Window)
    , m_foreground_role(Gfx::ColorRole::WindowText)
    , m_font(Gfx::FontDatabase::default_font())
    , m_palette(Application::the()->palette().impl())
{
    REGISTER_RECT_PROPERTY("relative_rect", relative_rect, set_relative_rect);
    REGISTER_BOOL_PROPERTY("fill_with_background_color", fill_with_background_color, set_fill_with_background_color);
    REGISTER_BOOL_PROPERTY("visible", is_visible, set_visible);
    REGISTER_BOOL_PROPERTY("focused", is_focused, set_focus);
    REGISTER_BOOL_PROPERTY("enabled", is_enabled, set_enabled);
    REGISTER_STRING_PROPERTY("tooltip", tooltip, set_tooltip);

    REGISTER_SIZE_PROPERTY("min_size", min_size, set_min_size);
    REGISTER_SIZE_PROPERTY("max_size", max_size, set_max_size);
    REGISTER_INT_PROPERTY("width", width, set_width);
    REGISTER_INT_PROPERTY("min_width", min_width, set_min_width);
    REGISTER_INT_PROPERTY("max_width", max_width, set_max_width);
    REGISTER_INT_PROPERTY("min_height", min_height, set_min_height);
    REGISTER_INT_PROPERTY("height", height, set_height);
    REGISTER_INT_PROPERTY("max_height", max_height, set_max_height);

    REGISTER_INT_PROPERTY("fixed_width", dummy_fixed_width, set_fixed_width);
    REGISTER_INT_PROPERTY("fixed_height", dummy_fixed_height, set_fixed_height);
    REGISTER_SIZE_PROPERTY("fixed_size", dummy_fixed_size, set_fixed_size);

    REGISTER_BOOL_PROPERTY("shrink_to_fit", is_shrink_to_fit, set_shrink_to_fit);

    REGISTER_INT_PROPERTY("x", x, set_x);
    REGISTER_INT_PROPERTY("y", y, set_y);

    register_property(
        "focus_policy", [this]() -> JsonValue {
        auto policy = focus_policy();
        if (policy == GUI::FocusPolicy::ClickFocus)
            return "ClickFocus";
        if (policy == GUI::FocusPolicy::NoFocus)
            return "NoFocus";
        if (policy == GUI::FocusPolicy::TabFocus)
            return "TabFocus";
        if (policy == GUI::FocusPolicy::StrongFocus)
            return "StrongFocus";
        return JsonValue(); },
        [this](auto& value) {
            if (!value.is_string())
                return false;
            if (value.as_string() == "ClickFocus") {
                set_focus_policy(GUI::FocusPolicy::ClickFocus);
                return true;
            }
            if (value.as_string() == "NoFocus") {
                set_focus_policy(GUI::FocusPolicy::NoFocus);
                return true;
            }
            if (value.as_string() == "TabFocus") {
                set_focus_policy(GUI::FocusPolicy::TabFocus);
                return true;
            }
            if (value.as_string() == "StrongFocus") {
                set_focus_policy(GUI::FocusPolicy::StrongFocus);
                return true;
            }
            return false;
        });

    register_property(
        "foreground_color", [this]() -> JsonValue { return palette().color(foreground_role()).to_string(); },
        [this](auto& value) {
            auto c = Color::from_string(value.to_string());
            if (c.has_value()) {
                auto _palette = palette();
                _palette.set_color(foreground_role(), c.value());
                set_palette(_palette);
                return true;
            }
            return false;
        });

    register_property(
        "background_color", [this]() -> JsonValue { return palette().color(background_role()).to_string(); },
        [this](auto& value) {
            auto c = Color::from_string(value.to_string());
            if (c.has_value()) {
                auto _palette = palette();
                _palette.set_color(background_role(), c.value());
                set_palette(_palette);
                return true;
            }
            return false;
        });
}

Widget::~Widget()
{
}

void Widget::child_event(Core::ChildEvent& event)
{
    if (event.type() == Event::ChildAdded) {
        if (event.child() && is<Widget>(*event.child()) && layout()) {
            if (event.insertion_before_child() && is<Widget>(event.insertion_before_child()))
                layout()->insert_widget_before(downcast<Widget>(*event.child()), downcast<Widget>(*event.insertion_before_child()));
            else
                layout()->add_widget(downcast<Widget>(*event.child()));
        }
        if (window() && event.child() && is<Widget>(*event.child()))
            window()->did_add_widget({}, downcast<Widget>(*event.child()));
    }
    if (event.type() == Event::ChildRemoved) {
        if (layout()) {
            if (event.child() && is<Widget>(*event.child()))
                layout()->remove_widget(downcast<Widget>(*event.child()));
            else
                invalidate_layout();
        }
        if (window() && event.child() && is<Widget>(*event.child()))
            window()->did_remove_widget({}, downcast<Widget>(*event.child()));
        update();
    }
    return Core::Object::child_event(event);
}

void Widget::set_relative_rect(const Gfx::IntRect& a_rect)
{
    // Get rid of negative width/height values.
    Gfx::IntRect rect = {
        a_rect.x(),
        a_rect.y(),
        max(a_rect.width(), 0),
        max(a_rect.height(), 0)
    };

    if (rect == m_relative_rect)
        return;

    auto old_rect = m_relative_rect;

    bool size_changed = m_relative_rect.size() != rect.size();
    m_relative_rect = rect;

    if (size_changed) {
        ResizeEvent resize_event(rect.size());
        event(resize_event);
    }

    if (auto* parent = parent_widget())
        parent->update(old_rect);
    update();
}

void Widget::event(Core::Event& event)
{
    if (!is_enabled()) {
        switch (event.type()) {
        case Event::MouseUp:
        case Event::MouseDown:
        case Event::MouseMove:
        case Event::MouseWheel:
        case Event::MouseDoubleClick:
        case Event::KeyUp:
        case Event::KeyDown:
            return;
        default:
            break;
        }
    }

    switch (event.type()) {
    case Event::Paint:
        return handle_paint_event(static_cast<PaintEvent&>(event));
    case Event::Resize:
        return handle_resize_event(static_cast<ResizeEvent&>(event));
    case Event::FocusIn:
        return focusin_event(static_cast<FocusEvent&>(event));
    case Event::FocusOut:
        return focusout_event(static_cast<FocusEvent&>(event));
    case Event::Show:
        return show_event(static_cast<ShowEvent&>(event));
    case Event::Hide:
        return hide_event(static_cast<HideEvent&>(event));
    case Event::KeyDown:
        return keydown_event(static_cast<KeyEvent&>(event));
    case Event::KeyUp:
        return keyup_event(static_cast<KeyEvent&>(event));
    case Event::MouseMove:
        return mousemove_event(static_cast<MouseEvent&>(event));
    case Event::MouseDown:
        return handle_mousedown_event(static_cast<MouseEvent&>(event));
    case Event::MouseDoubleClick:
        return handle_mousedoubleclick_event(static_cast<MouseEvent&>(event));
    case Event::MouseUp:
        return handle_mouseup_event(static_cast<MouseEvent&>(event));
    case Event::MouseWheel:
        return mousewheel_event(static_cast<MouseEvent&>(event));
    case Event::DragEnter:
        return drag_enter_event(static_cast<DragEvent&>(event));
    case Event::DragMove:
        return drag_move_event(static_cast<DragEvent&>(event));
    case Event::DragLeave:
        return drag_leave_event(static_cast<Event&>(event));
    case Event::Drop:
        return drop_event(static_cast<DropEvent&>(event));
    case Event::ThemeChange:
        return theme_change_event(static_cast<ThemeChangeEvent&>(event));
    case Event::Enter:
        return handle_enter_event(event);
    case Event::Leave:
        return handle_leave_event(event);
    case Event::EnabledChange:
        return change_event(static_cast<Event&>(event));
    default:
        return Core::Object::event(event);
    }
}

void Widget::handle_paint_event(PaintEvent& event)
{
    ASSERT(is_visible());
    if (fill_with_background_color()) {
        Painter painter(*this);
        painter.fill_rect(event.rect(), palette().color(background_role()));
    }
    paint_event(event);
    auto children_clip_rect = this->children_clip_rect();
    for_each_child_widget([&](auto& child) {
        if (!child.is_visible())
            return IterationDecision::Continue;
        if (child.relative_rect().intersects(event.rect())) {
            PaintEvent local_event(event.rect().intersected(children_clip_rect).intersected(child.relative_rect()).translated(-child.relative_position()));
            child.dispatch_event(local_event, this);
        }
        return IterationDecision::Continue;
    });
    second_paint_event(event);

    auto* app = Application::the();

    if (app && app->dnd_debugging_enabled() && has_pending_drop()) {
        Painter painter(*this);
        painter.draw_rect(rect(), Color::Blue);
    }

    if (app && app->focus_debugging_enabled() && is_focused()) {
        Painter painter(*this);
        painter.draw_rect(rect(), Color::Cyan);
    }

    if (is_being_inspected()) {
        Painter painter(*this);
        painter.draw_rect(rect(), Color::Magenta);
    }
}

void Widget::set_layout(NonnullRefPtr<Layout> layout)
{
    if (m_layout) {
        m_layout->notify_disowned({}, *this);
        m_layout->remove_from_parent();
    }
    m_layout = move(layout);
    if (m_layout) {
        add_child(*m_layout);
        m_layout->notify_adopted({}, *this);
        do_layout();
    } else {
        update();
    }
}

void Widget::do_layout()
{
    for_each_child_widget([&](auto& child) {
        child.do_layout();
        return IterationDecision::Continue;
    });
    custom_layout();
    if (!m_layout)
        return;
    m_layout->run(*this);
    did_layout();
    update();
}

void Widget::notify_layout_changed(Badge<Layout>)
{
    invalidate_layout();
}

void Widget::handle_resize_event(ResizeEvent& event)
{
    resize_event(event);
    do_layout();
}

void Widget::handle_mouseup_event(MouseEvent& event)
{
    mouseup_event(event);
}

void Widget::handle_mousedown_event(MouseEvent& event)
{
    if (((unsigned)focus_policy() & (unsigned)FocusPolicy::ClickFocus))
        set_focus(true, FocusSource::Mouse);
    mousedown_event(event);
    if (event.button() == MouseButton::Right) {
        ContextMenuEvent c_event(event.position(), screen_relative_rect().location().translated(event.position()));
        context_menu_event(c_event);
    }
}

void Widget::handle_mousedoubleclick_event(MouseEvent& event)
{
    doubleclick_event(event);
}

void Widget::handle_enter_event(Core::Event& event)
{
    if (auto* window = this->window())
        window->update_cursor({});
    show_or_hide_tooltip();
    enter_event(event);
}

void Widget::handle_leave_event(Core::Event& event)
{
    if (auto* window = this->window())
        window->update_cursor({});
    Application::the()->hide_tooltip();
    leave_event(event);
}

void Widget::doubleclick_event(MouseEvent&)
{
}

void Widget::resize_event(ResizeEvent&)
{
}

void Widget::paint_event(PaintEvent&)
{
}

void Widget::second_paint_event(PaintEvent&)
{
}

void Widget::show_event(ShowEvent&)
{
}

void Widget::hide_event(HideEvent&)
{
}

void Widget::keydown_event(KeyEvent& event)
{
    if (!event.alt() && !event.ctrl() && !event.logo()) {
        if (event.key() == KeyCode::Key_Tab) {
            if (event.shift())
                focus_previous_widget(FocusSource::Keyboard, false);
            else
                focus_next_widget(FocusSource::Keyboard, false);
            event.accept();
            return;
        }
        if (!event.shift() && (event.key() == KeyCode::Key_Left || event.key() == KeyCode::Key_Up)) {
            focus_previous_widget(FocusSource::Keyboard, true);
            event.accept();
            return;
        }
        if (!event.shift() && (event.key() == KeyCode::Key_Right || event.key() == KeyCode::Key_Down)) {
            focus_next_widget(FocusSource::Keyboard, true);
            event.accept();
            return;
        }
    }
    event.ignore();
}

void Widget::keyup_event(KeyEvent& event)
{
    event.ignore();
}

void Widget::mousedown_event(MouseEvent&)
{
}

void Widget::mouseup_event(MouseEvent&)
{
}

void Widget::mousemove_event(MouseEvent&)
{
}

void Widget::mousewheel_event(MouseEvent&)
{
}

void Widget::context_menu_event(ContextMenuEvent&)
{
}

void Widget::focusin_event(FocusEvent&)
{
}

void Widget::focusout_event(FocusEvent&)
{
}

void Widget::enter_event(Core::Event&)
{
}

void Widget::leave_event(Core::Event&)
{
}

void Widget::change_event(Event&)
{
}

void Widget::drag_move_event(DragEvent&)
{
}

void Widget::drag_enter_event(DragEvent& event)
{
    StringBuilder builder;
    builder.join(',', event.mime_types());
    dbgln("{} {:p} DRAG ENTER @ {}, {}", class_name(), this, event.position(), builder.string_view());
}

void Widget::drag_leave_event(Event&)
{
    dbgln("{} {:p} DRAG LEAVE", class_name(), this);
}

void Widget::drop_event(DropEvent& event)
{
    dbgln("{} {:p} DROP @ {}, '{}'", class_name(), this, event.position(), event.text());
}

void Widget::theme_change_event(ThemeChangeEvent&)
{
}

void Widget::update()
{
    if (rect().is_empty())
        return;
    update(rect());
}

void Widget::update(const Gfx::IntRect& rect)
{
    if (!is_visible())
        return;

    if (!updates_enabled())
        return;

    Window* window = m_window;
    Widget* parent = parent_widget();
    while (parent) {
        if (!parent->updates_enabled())
            return;
        window = parent->m_window;
        parent = parent->parent_widget();
    }
    if (window)
        window->update(rect.translated(window_relative_rect().location()));
}

Gfx::IntRect Widget::window_relative_rect() const
{
    auto rect = relative_rect();
    for (auto* parent = parent_widget(); parent; parent = parent->parent_widget()) {
        rect.move_by(parent->relative_position());
    }
    return rect;
}

Gfx::IntRect Widget::screen_relative_rect() const
{
    auto window_position = window()->window_type() == WindowType::MenuApplet
        ? window()->rect_in_menubar().location()
        : window()->rect().location();
    return window_relative_rect().translated(window_position);
}

Widget* Widget::child_at(const Gfx::IntPoint& point) const
{
    for (int i = children().size() - 1; i >= 0; --i) {
        if (!is<Widget>(children()[i]))
            continue;
        auto& child = downcast<Widget>(children()[i]);
        if (!child.is_visible())
            continue;
        if (child.content_rect().contains(point))
            return const_cast<Widget*>(&child);
    }
    return nullptr;
}

Widget::HitTestResult Widget::hit_test(const Gfx::IntPoint& position, ShouldRespectGreediness should_respect_greediness)
{
    if (should_respect_greediness == ShouldRespectGreediness::Yes && is_greedy_for_hits())
        return { this, position };
    if (auto* child = child_at(position))
        return child->hit_test(position - child->relative_position());
    return { this, position };
}

void Widget::set_window(Window* window)
{
    if (m_window == window)
        return;
    m_window = window;
}

void Widget::set_focus_proxy(Widget* proxy)
{
    if (m_focus_proxy == proxy)
        return;

    m_focus_proxy = proxy;
}

FocusPolicy Widget::focus_policy() const
{
    if (m_focus_proxy)
        return m_focus_proxy->focus_policy();
    return m_focus_policy;
}

void Widget::set_focus_policy(FocusPolicy policy)
{
    if (m_focus_proxy)
        return m_focus_proxy->set_focus_policy(policy);
    m_focus_policy = policy;
}

bool Widget::is_focused() const
{
    if (m_focus_proxy)
        return m_focus_proxy->is_focused();

    auto* win = window();
    if (!win)
        return false;
    // Accessory windows are not active despite being the active
    // input window. So we can have focus if either we're the active
    // input window or we're the active window
    if (win->is_active_input() || win->is_active())
        return win->focused_widget() == this;
    return false;
}

void Widget::set_focus(bool focus, FocusSource source)
{
    if (m_focus_proxy)
        return m_focus_proxy->set_focus(focus, source);

    auto* win = window();
    if (!win)
        return;
    if (focus) {
        win->set_focused_widget(this, source);
    } else {
        if (win->focused_widget() == this)
            win->set_focused_widget(nullptr, source);
    }
}

void Widget::set_font(const Gfx::Font* font)
{
    if (m_font.ptr() == font)
        return;

    if (!font)
        m_font = Gfx::FontDatabase::default_font();
    else
        m_font = *font;

    did_change_font();
    update();
}

void Widget::set_global_cursor_tracking(bool enabled)
{
    auto* win = window();
    if (!win)
        return;
    win->set_global_cursor_tracking_widget(enabled ? this : nullptr);
}

bool Widget::global_cursor_tracking() const
{
    auto* win = window();
    if (!win)
        return false;
    return win->global_cursor_tracking_widget() == this;
}

void Widget::set_min_size(const Gfx::IntSize& size)
{
    if (m_min_size == size)
        return;
    m_min_size = size;
    invalidate_layout();
}

void Widget::set_max_size(const Gfx::IntSize& size)
{
    if (m_max_size == size)
        return;
    m_max_size = size;
    invalidate_layout();
}

void Widget::invalidate_layout()
{
    if (window())
        window()->schedule_relayout();
}

void Widget::set_visible(bool visible)
{
    if (visible == m_visible)
        return;
    m_visible = visible;
    if (auto* parent = parent_widget())
        parent->invalidate_layout();
    if (m_visible)
        update();

    if (m_visible) {
        ShowEvent e;
        event(e);
    } else {
        HideEvent e;
        event(e);
    }
}

bool Widget::spans_entire_window_horizontally() const
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

void Widget::set_enabled(bool enabled)
{
    if (m_enabled == enabled)
        return;
    m_enabled = enabled;

    for_each_child_widget([enabled](auto& child) {
        child.set_enabled(enabled);
        return IterationDecision::Continue;
    });

    if (!m_enabled && window() && window()->focused_widget() == this) {
        window()->did_disable_focused_widget({});
    }

    Event e(Event::EnabledChange);
    event(e);
    update();
}

void Widget::move_to_front()
{
    auto* parent = parent_widget();
    if (!parent)
        return;
    if (parent->children().size() == 1)
        return;
    parent->children().remove_first_matching([this](auto& entry) {
        return entry == this;
    });
    parent->children().append(*this);
    parent->update();
}

void Widget::move_to_back()
{
    auto* parent = parent_widget();
    if (!parent)
        return;
    if (parent->children().size() == 1)
        return;
    parent->children().remove_first_matching([this](auto& entry) {
        return entry == this;
    });
    parent->children().prepend(*this);
    parent->update();
}

bool Widget::is_frontmost() const
{
    auto* parent = parent_widget();
    if (!parent)
        return true;
    return &parent->children().last() == this;
}

bool Widget::is_backmost() const
{
    auto* parent = parent_widget();
    if (!parent)
        return true;
    return &parent->children().first() == this;
}

Action* Widget::action_for_key_event(const KeyEvent& event)
{
    Shortcut shortcut(event.modifiers(), (KeyCode)event.key());

    if (!shortcut.is_valid()) {
        return nullptr;
    }

    Action* found_action = nullptr;
    for_each_child_of_type<Action>([&](auto& action) {
        if (action.shortcut() == shortcut) {
            found_action = &action;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_action;
}

void Widget::set_updates_enabled(bool enabled)
{
    if (m_updates_enabled == enabled)
        return;
    m_updates_enabled = enabled;
    if (enabled)
        update();
}

void Widget::focus_previous_widget(FocusSource source, bool siblings_only)
{
    auto focusable_widgets = window()->focusable_widgets(source);
    if (siblings_only)
        focusable_widgets.remove_all_matching([this](auto& entry) { return entry->parent() != parent(); });
    for (int i = focusable_widgets.size() - 1; i >= 0; --i) {
        if (focusable_widgets[i] != this)
            continue;
        if (i > 0)
            focusable_widgets[i - 1]->set_focus(true, source);
        else
            focusable_widgets.last()->set_focus(true, source);
    }
}

void Widget::focus_next_widget(FocusSource source, bool siblings_only)
{
    auto focusable_widgets = window()->focusable_widgets(source);
    if (siblings_only)
        focusable_widgets.remove_all_matching([this](auto& entry) { return entry->parent() != parent(); });
    for (size_t i = 0; i < focusable_widgets.size(); ++i) {
        if (focusable_widgets[i] != this)
            continue;
        if (i < focusable_widgets.size() - 1)
            focusable_widgets[i + 1]->set_focus(true, source);
        else
            focusable_widgets.first()->set_focus(true, source);
    }
}

Vector<Widget*> Widget::child_widgets() const
{
    Vector<Widget*> widgets;
    widgets.ensure_capacity(children().size());
    for (auto& child : const_cast<Widget*>(this)->children()) {
        if (is<Widget>(child))
            widgets.append(static_cast<Widget*>(&child));
    }
    return widgets;
}

void Widget::set_palette(const Palette& palette)
{
    m_palette = palette.impl();
}

void Widget::set_background_role(ColorRole role)
{
    m_background_role = role;
}

void Widget::set_foreground_role(ColorRole role)
{
    m_foreground_role = role;
}

Gfx::Palette Widget::palette() const
{
    return Gfx::Palette(*m_palette);
}

void Widget::did_begin_inspection()
{
    update();
}

void Widget::did_end_inspection()
{
    update();
}

void Widget::set_content_margins(const Margins& margins)
{
    if (m_content_margins == margins)
        return;
    m_content_margins = margins;
    invalidate_layout();
}

Gfx::IntRect Widget::content_rect() const
{
    auto rect = relative_rect();
    rect.move_by(m_content_margins.left(), m_content_margins.top());
    rect.set_width(rect.width() - (m_content_margins.left() + m_content_margins.right()));
    rect.set_height(rect.height() - (m_content_margins.top() + m_content_margins.bottom()));
    return rect;
}

void Widget::set_tooltip(const StringView& tooltip)
{
    m_tooltip = tooltip;
    if (Application::the()->tooltip_source_widget() == this)
        show_or_hide_tooltip();
}

void Widget::show_or_hide_tooltip()
{
    if (has_tooltip())
        Application::the()->show_tooltip(m_tooltip, this);
    else
        Application::the()->hide_tooltip();
}

Gfx::IntRect Widget::children_clip_rect() const
{
    return rect();
}

void Widget::set_override_cursor(Gfx::StandardCursor cursor)
{
    if (m_override_cursor == cursor)
        return;

    m_override_cursor = cursor;
    if (auto* window = this->window())
        window->update_cursor({});
}

bool Widget::load_from_gml(const StringView& gml_string)
{
    return load_from_gml(gml_string, [](const String& class_name) -> RefPtr<Widget> {
        dbgln("Class '{}' not registered", class_name);
        return nullptr;
    });
}

bool Widget::load_from_gml(const StringView& gml_string, RefPtr<Widget> (*unregistered_child_handler)(const String&))
{
    auto value = parse_gml(gml_string);
    if (!value.is_object())
        return false;
    return load_from_json(value.as_object(), unregistered_child_handler);
}

bool Widget::load_from_json(const JsonObject& json, RefPtr<Widget> (*unregistered_child_handler)(const String&))
{
    json.for_each_member([&](auto& key, auto& value) {
        set_property(key, value);
    });

    auto layout_value = json.get("layout");
    if (!layout_value.is_null() && !layout_value.is_object()) {
        dbgln("layout is not an object");
        return false;
    }
    if (layout_value.is_object()) {
        auto& layout = layout_value.as_object();
        auto class_name = layout.get("class");
        if (class_name.is_null()) {
            dbgln("Invalid layout class name");
            return false;
        }

        if (class_name.to_string() == "GUI::VerticalBoxLayout") {
            set_layout<GUI::VerticalBoxLayout>();
        } else if (class_name.to_string() == "GUI::HorizontalBoxLayout") {
            set_layout<GUI::HorizontalBoxLayout>();
        } else {
            dbg() << "Unknown layout class: '" << class_name.to_string() << "'";
            return false;
        }

        layout.for_each_member([&](auto& key, auto& value) {
            this->layout()->set_property(key, value);
        });
    }

    auto children = json.get("children");
    if (children.is_array()) {
        for (auto& child_json_value : children.as_array().values()) {
            if (!child_json_value.is_object())
                return false;
            auto& child_json = child_json_value.as_object();
            auto class_name = child_json.get("class");
            if (!class_name.is_string()) {
                dbgln("No class name in entry");
                return false;
            }

            RefPtr<Widget> child_widget;
            if (auto* registration = WidgetClassRegistration::find(class_name.as_string())) {
                child_widget = registration->construct();
            } else {
                child_widget = unregistered_child_handler(class_name.as_string());
                if (!child_widget)
                    return false;
            }
            add_child(*child_widget);
            child_widget->load_from_json(child_json, unregistered_child_handler);
        }
    }

    return true;
}

bool Widget::has_focus_within() const
{
    auto* window = this->window();
    if (!window)
        return false;
    if (!window->focused_widget())
        return false;
    auto& effective_focus_widget = focus_proxy() ? *focus_proxy() : *this;
    return window->focused_widget() == &effective_focus_widget || is_ancestor_of(*window->focused_widget());
}

void Widget::set_shrink_to_fit(bool b)
{
    if (m_shrink_to_fit == b)
        return;
    m_shrink_to_fit = b;
    invalidate_layout();
}

bool Widget::has_pending_drop() const
{
    return Application::the()->pending_drop_widget() == this;
}

}
