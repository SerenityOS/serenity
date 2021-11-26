/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <LibGfx/SystemTheme.h>
#include <unistd.h>

REGISTER_CORE_OBJECT(GUI, Widget)

namespace GUI {

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

    REGISTER_STRING_PROPERTY("font", m_font->family, set_font_family);
    REGISTER_INT_PROPERTY("font_size", m_font->presentation_size, set_font_size);
    REGISTER_FONT_WEIGHT_PROPERTY("font_weight", m_font->weight, set_font_weight);

    register_property(
        "font_type", [this] { return m_font->is_fixed_width() ? "FixedWidth" : "Normal"; },
        [this](auto& value) {
            if (value.to_string() == "FixedWidth") {
                set_font_fixed_width(true);
                return true;
            }
            if (value.to_string() == "Normal") {
                set_font_fixed_width(false);
                return true;
            }
            return false;
        });

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

    register_property(
        "foreground_role", [this]() -> JsonValue { return Gfx::to_string(foreground_role()); },
        [this](auto& value) {
            if (!value.is_string())
                return false;
            auto str = value.as_string();
            if (str == "NoRole") {
                set_foreground_role(Gfx::ColorRole::NoRole);
                return true;
            }
#undef __ENUMERATE_COLOR_ROLE
#define __ENUMERATE_COLOR_ROLE(role)               \
    else if (str == #role)                         \
    {                                              \
        set_foreground_role(Gfx::ColorRole::role); \
        return true;                               \
    }
            ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE
            return false;
        });

    register_property(
        "background_role", [this]() -> JsonValue { return Gfx::to_string(background_role()); },
        [this](auto& value) {
            if (!value.is_string())
                return false;
            auto str = value.as_string();
            if (str == "NoRole") {
                set_background_role(Gfx::ColorRole::NoRole);
                return true;
            }
#undef __ENUMERATE_COLOR_ROLE
#define __ENUMERATE_COLOR_ROLE(role)               \
    else if (str == #role)                         \
    {                                              \
        set_background_role(Gfx::ColorRole::role); \
        return true;                               \
    }
            ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE
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
                layout()->insert_widget_before(verify_cast<Widget>(*event.child()), verify_cast<Widget>(*event.insertion_before_child()));
            else
                layout()->add_widget(verify_cast<Widget>(*event.child()));
        }
        if (window() && event.child() && is<Widget>(*event.child()))
            window()->did_add_widget({}, verify_cast<Widget>(*event.child()));
    }
    if (event.type() == Event::ChildRemoved) {
        if (layout()) {
            if (event.child() && is<Widget>(*event.child()))
                layout()->remove_widget(verify_cast<Widget>(*event.child()));
            else
                invalidate_layout();
        }
        if (window() && event.child() && is<Widget>(*event.child()))
            window()->did_remove_widget({}, verify_cast<Widget>(*event.child()));
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
        return handle_keydown_event(static_cast<KeyEvent&>(event));
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
    case Event::FontsChange:
        return fonts_change_event(static_cast<FontsChangeEvent&>(event));
    case Event::Enter:
        return handle_enter_event(event);
    case Event::Leave:
        return handle_leave_event(event);
    case Event::EnabledChange:
        return change_event(static_cast<Event&>(event));
    case Event::ContextMenu:
        return context_menu_event(static_cast<ContextMenuEvent&>(event));
    case Event::AppletAreaRectChange:
        return applet_area_rect_change_event(static_cast<AppletAreaRectChangeEvent&>(event));
    default:
        return Core::Object::event(event);
    }
}

void Widget::handle_keydown_event(KeyEvent& event)
{
    keydown_event(event);
    if (event.key() == KeyCode::Key_Menu) {
        ContextMenuEvent c_event(window_relative_rect().bottom_right(), screen_relative_rect().bottom_right());
        dispatch_event(c_event);
    }
}

void Widget::handle_paint_event(PaintEvent& event)
{
    VERIFY(is_visible());

    if (!rect().intersects(event.rect())) {
        // This widget is not inside the paint event rect.
        // Since widgets fully contain their children, we don't need to recurse further.
        return;
    }

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

    if (app && app->hover_debugging_enabled() && this == window()->hovered_widget()) {
        Painter painter(*this);
        painter.draw_rect(rect(), Color::Red);
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
    if (has_flag(focus_policy(), FocusPolicy::ClickFocus))
        set_focus(true, FocusSource::Mouse);
    mousedown_event(event);
    if (event.button() == MouseButton::Secondary) {
        ContextMenuEvent c_event(event.position(), screen_relative_rect().location().translated(event.position()));
        dispatch_event(c_event);
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
    if (!event.alt() && !event.ctrl() && !event.super()) {
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

void Widget::mousewheel_event(MouseEvent& event)
{
    event.ignore();
}

void Widget::context_menu_event(ContextMenuEvent& event)
{
    event.ignore();
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
    event.ignore();
}

void Widget::theme_change_event(ThemeChangeEvent&)
{
}

void Widget::fonts_change_event(FontsChangeEvent&)
{
    if (m_default_font)
        set_font(nullptr);
}

void Widget::screen_rects_change_event(ScreenRectsChangeEvent&)
{
}

void Widget::applet_area_rect_change_event(AppletAreaRectChangeEvent&)
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

    auto bound_by_widget = rect.intersected(this->rect());
    if (bound_by_widget.is_empty())
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
        window->update(bound_by_widget.translated(window_relative_rect().location()));
}

void Widget::repaint()
{
    if (rect().is_empty())
        return;
    repaint(rect());
}

void Widget::repaint(Gfx::IntRect const& rect)
{
    auto* window = this->window();
    if (!window)
        return;
    update(rect);
    window->flush_pending_paints_immediately();
}

Gfx::IntRect Widget::window_relative_rect() const
{
    auto rect = relative_rect();
    for (auto* parent = parent_widget(); parent; parent = parent->parent_widget()) {
        rect.translate_by(parent->relative_position());
    }
    return rect;
}

Gfx::IntRect Widget::screen_relative_rect() const
{
    auto window_position = window()->window_type() == WindowType::Applet
        ? window()->applet_rect_on_screen().location()
        : window()->rect().location();
    return window_relative_rect().translated(window_position);
}

Widget* Widget::child_at(const Gfx::IntPoint& point) const
{
    for (int i = children().size() - 1; i >= 0; --i) {
        if (!is<Widget>(children()[i]))
            continue;
        auto& child = verify_cast<Widget>(children()[i]);
        if (!child.is_visible())
            continue;
        if (child.relative_non_grabbable_rect().contains(point))
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

    if (!font) {
        m_font = Gfx::FontDatabase::default_font();
        m_default_font = true;
    } else {
        m_font = *font;
        m_default_font = false;
    }

    did_change_font();
    update();
}

void Widget::set_font_family(const String& family)
{
    set_font(Gfx::FontDatabase::the().get(family, m_font->presentation_size(), m_font->weight()));
}

void Widget::set_font_size(unsigned size)
{
    set_font(Gfx::FontDatabase::the().get(m_font->family(), size, m_font->weight()));
}

void Widget::set_font_weight(unsigned weight)
{
    set_font(Gfx::FontDatabase::the().get(m_font->family(), m_font->presentation_size(), weight));
}

void Widget::set_font_fixed_width(bool fixed_width)
{
    if (fixed_width)
        set_font(Gfx::FontDatabase::the().get(Gfx::FontDatabase::the().default_fixed_width_font().family(), m_font->presentation_size(), m_font->weight()));
    else
        set_font(Gfx::FontDatabase::the().get(Gfx::FontDatabase::the().default_font().family(), m_font->presentation_size(), m_font->weight()));
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
    if (!m_visible && is_focused())
        set_focus(false);

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

    if (!m_enabled)
        set_override_cursor(Gfx::StandardCursor::None);

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
        if (action.shortcut() == shortcut || action.alternate_shortcut() == shortcut) {
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
        focusable_widgets.remove_all_matching([this](auto& entry) { return entry.parent() != parent(); });
    for (int i = focusable_widgets.size() - 1; i >= 0; --i) {
        if (&focusable_widgets[i] != this)
            continue;
        if (i > 0)
            focusable_widgets[i - 1].set_focus(true, source);
        else
            focusable_widgets.last().set_focus(true, source);
    }
}

void Widget::focus_next_widget(FocusSource source, bool siblings_only)
{
    auto focusable_widgets = window()->focusable_widgets(source);
    if (siblings_only)
        focusable_widgets.remove_all_matching([this](auto& entry) { return entry.parent() != parent(); });
    for (size_t i = 0; i < focusable_widgets.size(); ++i) {
        if (&focusable_widgets[i] != this)
            continue;
        if (i < focusable_widgets.size() - 1)
            focusable_widgets[i + 1].set_focus(true, source);
        else
            focusable_widgets.first().set_focus(true, source);
    }
}

Vector<Widget&> Widget::child_widgets() const
{
    Vector<Widget&> widgets;
    widgets.ensure_capacity(children().size());
    for (auto& child : const_cast<Widget*>(this)->children()) {
        if (is<Widget>(child))
            widgets.append(static_cast<Widget&>(child));
    }
    return widgets;
}

void Widget::set_palette(const Palette& palette)
{
    m_palette = palette.impl();
    update();
}

void Widget::set_background_role(ColorRole role)
{
    m_background_role = role;
    update();
}

void Widget::set_foreground_role(ColorRole role)
{
    m_foreground_role = role;
    update();
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

void Widget::set_grabbable_margins(const Margins& margins)
{
    if (m_grabbable_margins == margins)
        return;
    m_grabbable_margins = margins;
    invalidate_layout();
}

Gfx::IntRect Widget::relative_non_grabbable_rect() const
{
    auto rect = relative_rect();
    rect.translate_by(m_grabbable_margins.left(), m_grabbable_margins.top());
    rect.set_width(rect.width() - (m_grabbable_margins.left() + m_grabbable_margins.right()));
    rect.set_height(rect.height() - (m_grabbable_margins.top() + m_grabbable_margins.bottom()));
    return rect;
}

void Widget::set_tooltip(String tooltip)
{
    m_tooltip = move(tooltip);
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

void Widget::set_override_cursor(AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor)
{
    auto const& are_cursors_the_same = [](AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> const& a, AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> const& b) {
        if (a.has<Gfx::StandardCursor>() != b.has<Gfx::StandardCursor>())
            return false;
        if (a.has<Gfx::StandardCursor>())
            return a.get<Gfx::StandardCursor>() == b.get<Gfx::StandardCursor>();
        return a.get<NonnullRefPtr<Gfx::Bitmap>>().ptr() == b.get<NonnullRefPtr<Gfx::Bitmap>>().ptr();
    };

    if (are_cursors_the_same(m_override_cursor, cursor))
        return;

    m_override_cursor = move(cursor);
    if (auto* window = this->window()) {
        window->update_cursor({});
    }
}

bool Widget::load_from_gml(StringView gml_string)
{
    return load_from_gml(gml_string, [](const String& class_name) -> RefPtr<Core::Object> {
        dbgln("Class '{}' not registered", class_name);
        return nullptr;
    });
}

bool Widget::load_from_gml(StringView gml_string, RefPtr<Core::Object> (*unregistered_child_handler)(const String&))
{
    auto value = parse_gml(gml_string);
    if (!value.is_object())
        return false;
    return load_from_json(value.as_object(), unregistered_child_handler);
}

bool Widget::load_from_json(const JsonObject& json, RefPtr<Core::Object> (*unregistered_child_handler)(const String&))
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

        auto& layout_class = *Core::ObjectClassRegistration::find("GUI::Layout");
        if (auto* registration = Core::ObjectClassRegistration::find(class_name.as_string())) {
            auto layout = registration->construct();
            if (!layout || !registration->is_derived_from(layout_class)) {
                dbgln("Invalid layout class: '{}'", class_name.to_string());
                return false;
            }
            set_layout(static_ptr_cast<Layout>(layout).release_nonnull());
        } else {
            dbgln("Unknown layout class: '{}'", class_name.to_string());
            return false;
        }

        layout.for_each_member([&](auto& key, auto& value) {
            this->layout()->set_property(key, value);
        });
    }

    auto& widget_class = *Core::ObjectClassRegistration::find("GUI::Widget");
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

            RefPtr<Core::Object> child;
            if (auto* registration = Core::ObjectClassRegistration::find(class_name.as_string())) {
                child = registration->construct();
                if (!child || !registration->is_derived_from(widget_class)) {
                    dbgln("Invalid widget class: '{}'", class_name.to_string());
                    return false;
                }
            } else {
                child = unregistered_child_handler(class_name.as_string());
            }
            if (!child)
                return false;
            add_child(*child);
            child->load_from_json(child_json, unregistered_child_handler);
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

bool Widget::is_visible_for_timer_purposes() const
{
    return is_visible() && Object::is_visible_for_timer_purposes();
}

}
