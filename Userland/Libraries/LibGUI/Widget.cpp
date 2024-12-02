/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <AK/IterationDecision.h>
#include <AK/JsonObject.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Event.h>
#include <LibGUI/GML/AST.h>
#include <LibGUI/GML/Parser.h>
#include <LibGUI/Layout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <unistd.h>

REGISTER_GUI_OBJECT(GUI, Widget)

namespace GUI {

Widget::Widget()
    : m_background_role(Gfx::ColorRole::Window)
    , m_foreground_role(Gfx::ColorRole::WindowText)
    , m_font(Gfx::FontDatabase::default_font())
    , m_palette(Application::the()->palette().impl())
{
    REGISTER_READONLY_STRING_PROPERTY("class_name", class_name);
    REGISTER_DEPRECATED_STRING_PROPERTY("name", name, set_name);

    register_property(
        "address"sv, [this] { return FlatPtr(this); },
        nullptr, nullptr);

    register_property(
        "parent"sv, [this] { return FlatPtr(this->parent()); },
        nullptr, nullptr);

    REGISTER_RECT_PROPERTY("relative_rect", relative_rect, set_relative_rect);
    REGISTER_BOOL_PROPERTY("fill_with_background_color", fill_with_background_color, set_fill_with_background_color);
    REGISTER_BOOL_PROPERTY("visible", is_visible, set_visible);
    REGISTER_BOOL_PROPERTY("focused", is_focused, set_focus);
    REGISTER_BOOL_PROPERTY("enabled", is_enabled, set_enabled);
    REGISTER_STRING_PROPERTY("tooltip", tooltip, set_tooltip);

    REGISTER_UI_SIZE_PROPERTY("min_size", min_size, set_min_size);
    REGISTER_READONLY_UI_SIZE_PROPERTY("effective_min_size", effective_min_size);
    REGISTER_UI_SIZE_PROPERTY("max_size", max_size, set_max_size);
    REGISTER_UI_SIZE_PROPERTY("preferred_size", preferred_size, set_preferred_size);
    REGISTER_READONLY_UI_SIZE_PROPERTY("effective_preferred_size", effective_preferred_size);
    REGISTER_INT_PROPERTY("width", width, set_width);
    REGISTER_UI_DIMENSION_PROPERTY("min_width", min_width, set_min_width);
    REGISTER_UI_DIMENSION_PROPERTY("max_width", max_width, set_max_width);
    REGISTER_UI_DIMENSION_PROPERTY("preferred_width", preferred_width, set_preferred_width);
    REGISTER_INT_PROPERTY("height", height, set_height);
    REGISTER_UI_DIMENSION_PROPERTY("min_height", min_height, set_min_height);
    REGISTER_UI_DIMENSION_PROPERTY("max_height", max_height, set_max_height);
    REGISTER_UI_DIMENSION_PROPERTY("preferred_height", preferred_height, set_preferred_height);

    REGISTER_INT_PROPERTY("fixed_width", dummy_fixed_width, set_fixed_width);
    REGISTER_INT_PROPERTY("fixed_height", dummy_fixed_height, set_fixed_height);
    REGISTER_SIZE_PROPERTY("fixed_size", dummy_fixed_size, set_fixed_size);

    REGISTER_BOOL_PROPERTY("shrink_to_fit", is_shrink_to_fit, set_shrink_to_fit);

    REGISTER_INT_PROPERTY("x", x, set_x);
    REGISTER_INT_PROPERTY("y", y, set_y);

    REGISTER_STRING_PROPERTY("font", font_family, set_font_family);
    REGISTER_INT_PROPERTY("font_size", m_font->presentation_size, set_font_size);
    REGISTER_FONT_WEIGHT_PROPERTY("font_weight", m_font->weight, set_font_weight);

    REGISTER_STRING_PROPERTY("title", title, set_title);

    REGISTER_BOOL_PROPERTY("font_fixed_width", is_font_fixed_width, set_font_fixed_width)
    register_property(
        "font_type"sv, [this] { return m_font->is_fixed_width() ? "FixedWidth" : "Normal"; },
        [](JsonValue const& value) -> ErrorOr<bool> {
            if (value.is_string()) {
                auto string = value.as_string();
                if (string == "FixedWidth")
                    return true;
                if (string == "Normal")
                    return false;
            }
            return Error::from_string_literal("\"FixedWidth\" or \"Normal\" is expected");
        },
        [this](auto const& value) { return set_font_fixed_width(value); });

    REGISTER_ENUM_PROPERTY("focus_policy", focus_policy, set_focus_policy, GUI::FocusPolicy,
        { GUI::FocusPolicy::ClickFocus, "ClickFocus" },
        { GUI::FocusPolicy::NoFocus, "NoFocus" },
        { GUI::FocusPolicy::TabFocus, "TabFocus" },
        { GUI::FocusPolicy::StrongFocus, "StrongFocus" });

    register_property(
        "foreground_color"sv,
        [this]() { return palette().color(foreground_role()).to_byte_string(); },
        ::GUI::PropertyDeserializer<Color> {},
        [this](Gfx::Color const& color) {
            auto _palette = palette();
            _palette.set_color(foreground_role(), color);
            set_palette(_palette);
        });

    register_property(
        "background_color"sv,
        [this]() { return palette().color(background_role()).to_byte_string(); },
        ::GUI::PropertyDeserializer<Color> {},
        [this](Gfx::Color const& color) {
            set_background_color(color);
        });

#define __ENUMERATE_COLOR_ROLE(role) \
    {                                \
        Gfx::ColorRole::role, #role  \
    },
    REGISTER_ENUM_PROPERTY("foreground_role", foreground_role, set_foreground_role, Gfx::ColorRole,
        { Gfx::ColorRole::NoRole, "NoRole" },
        ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE));
    REGISTER_ENUM_PROPERTY("background_role", background_role, set_background_role, Gfx::ColorRole,
        { Gfx::ColorRole::NoRole, "NoRole" },
        ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE));
#undef __ENUMERATE_COLOR_ROLE
}

Widget::~Widget() = default;

void Widget::layout_relevant_change_occurred()
{
    if (auto* parent = parent_widget())
        parent->layout_relevant_change_occurred();
    else if (window())
        window()->schedule_relayout();
}

void Widget::child_event(Core::ChildEvent& event)
{
    if (event.type() == Event::ChildAdded) {
        if (event.child() && is<Widget>(*event.child()) && layout()) {
            if (event.insertion_before_child() && is<Widget>(event.insertion_before_child()))
                layout()->insert_widget_before(verify_cast<Widget>(*event.child()), verify_cast<Widget>(*event.insertion_before_child()));
            else
                layout()->add_widget(verify_cast<Widget>(*event.child()));
            layout_relevant_change_occurred();
        }
        if (window() && event.child() && is<Widget>(*event.child()))
            window()->did_add_widget({}, verify_cast<Widget>(*event.child()));

        if (event.child() && is<Widget>(*event.child()) && static_cast<Widget const&>(*event.child()).is_visible()) {
            ShowEvent show_event;
            event.child()->dispatch_event(show_event);
        }
    }
    if (event.type() == Event::ChildRemoved) {
        if (layout()) {
            if (event.child() && is<Widget>(*event.child()))
                layout()->remove_widget(verify_cast<Widget>(*event.child()));
            layout_relevant_change_occurred();
        }
        if (window() && event.child() && is<Widget>(*event.child()))
            window()->did_remove_widget({}, verify_cast<Widget>(*event.child()));
        if (event.child() && is<Widget>(*event.child())) {
            HideEvent hide_event;
            event.child()->dispatch_event(hide_event);
        }
        update();
    }
    return Core::EventReceiver::child_event(event);
}

void Widget::set_relative_rect(Gfx::IntRect const& a_rect)
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
        return Core::EventReceiver::event(event);
    }
}

void Widget::handle_keydown_event(KeyEvent& event)
{
    keydown_event(event);
    if (event.is_accepted())
        return;

    if (auto action = Action::find_action_for_shortcut(*this, Shortcut(event.modifiers(), event.key()))) {
        action->process_event(*window(), event);
        if (event.is_accepted())
            return;
    }

    if (event.key() == KeyCode::Key_Menu) {
        ContextMenuEvent c_event(window_relative_rect().bottom_right().translated(-1), screen_relative_rect().bottom_right().translated(-1));
        dispatch_event(c_event);
        return;
    }

    event.ignore();
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
    layout_relevant_change_occurred();
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
    if (Application::the()->tooltip_source_widget() == this)
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
    dbgln_if(DRAG_DEBUG, "{} {:p} DRAG ENTER @ {}, {}", class_name(), this, event.position(), event.mime_data().formats());
}

void Widget::drag_leave_event(Event&)
{
    dbgln_if(DRAG_DEBUG, "{} {:p} DRAG LEAVE", class_name(), this);
}

void Widget::drop_event(DropEvent& event)
{
    dbgln_if(DRAG_DEBUG, "{} {:p} DROP @ {}, '{}'", class_name(), this, event.position(), event.text());
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

    for (auto& it : m_focus_delegators) {
        if (!it.is_null() && !it->rect().is_empty())
            it->update(it->rect());
    }
}

void Widget::update(Gfx::IntRect const& rect)
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

Widget* Widget::child_at(Gfx::IntPoint point) const
{
    for (int i = children().size() - 1; i >= 0; --i) {
        if (!is<Widget>(children()[i]))
            continue;
        auto& child = verify_cast<Widget>(*children()[i]);
        if (!child.is_visible())
            continue;
        if (child.relative_non_grabbable_rect().contains(point))
            return const_cast<Widget*>(&child);
    }
    return nullptr;
}

Widget::HitTestResult Widget::hit_test(Gfx::IntPoint position, ShouldRespectGreediness should_respect_greediness)
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
    if (proxy)
        proxy->add_focus_delegator(this);
    else if (m_focus_proxy)
        m_focus_proxy->remove_focus_delegator(this);
    m_focus_proxy = proxy;
}

void Widget::add_focus_delegator(Widget* delegator)
{
    m_focus_delegators.remove_all_matching([&](auto& entry) {
        return entry.is_null() || entry == delegator;
    });
    m_focus_delegators.append(delegator);
}

void Widget::remove_focus_delegator(Widget* delegator)
{
    m_focus_delegators.remove_first_matching([&](auto& entry) {
        return entry == delegator;
    });
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
    if (win->is_focusable())
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

void Widget::set_font(Gfx::Font const* font)
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

void Widget::set_font_family(String const& family)
{
    set_font(Gfx::FontDatabase::the().get(family, m_font->presentation_size(), m_font->weight(), m_font->width(), m_font->slope()));
}

void Widget::set_font_size(unsigned size)
{
    set_font(Gfx::FontDatabase::the().get(m_font->family(), size, m_font->weight(), m_font->width(), m_font->slope()));
}

void Widget::set_font_weight(unsigned weight)
{
    set_font(Gfx::FontDatabase::the().get(m_font->family(), m_font->presentation_size(), weight, m_font->width(), m_font->slope()));
}

void Widget::set_font_fixed_width(bool fixed_width)
{
    if (fixed_width)
        set_font(Gfx::FontDatabase::the().get(Gfx::FontDatabase::the().default_fixed_width_font().family(), m_font->presentation_size(), m_font->weight(), m_font->width(), m_font->slope()));
    else
        set_font(Gfx::FontDatabase::the().get(Gfx::FontDatabase::the().default_font().family(), m_font->presentation_size(), m_font->weight(), m_font->width(), m_font->slope()));
}

bool Widget::is_font_fixed_width()
{
    return font().is_fixed_width();
}

void Widget::set_min_size(UISize const& size)
{
    VERIFY(size.width().is_one_of(SpecialDimension::Regular, SpecialDimension::Shrink));
    if (m_min_size == size)
        return;
    m_min_size = size;
    layout_relevant_change_occurred();
}

void Widget::set_max_size(UISize const& size)
{
    VERIFY(size.width().is_one_of(SpecialDimension::Regular, SpecialDimension::Grow));
    if (m_max_size == size)
        return;
    m_max_size = size;
    layout_relevant_change_occurred();
}

void Widget::set_preferred_size(UISize const& size)
{
    if (m_preferred_size == size)
        return;
    m_preferred_size = size;
    layout_relevant_change_occurred();
}

Optional<UISize> Widget::calculated_preferred_size() const
{
    if (layout())
        return { layout()->preferred_size() };
    return {};
}

Optional<UISize> Widget::calculated_min_size() const
{
    if (layout())
        return { layout()->min_size() };
    // Fall back to at least displaying the margins, so the Widget is not 0 size.
    auto m = content_margins();
    if (!m.is_null())
        return UISize { m.left() + m.right(), m.top() + m.bottom() };
    return {};
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
    layout_relevant_change_occurred();
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
    return parent->children().last() == this;
}

bool Widget::is_backmost() const
{
    auto* parent = parent_widget();
    if (!parent)
        return true;
    return parent->children().first() == this;
}

Action* Widget::action_for_shortcut(Shortcut const& shortcut)
{
    return Action::find_action_for_shortcut(*this, shortcut);
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
        if (is<Widget>(*child))
            widgets.append(static_cast<Widget&>(*child));
    }
    return widgets;
}

void Widget::set_palette(Palette& palette)
{
    m_palette = palette.impl();
    update();
}

void Widget::set_title(String title)
{
    m_title = move(title);
    layout_relevant_change_occurred();
    // For tab widget children, our change in title also affects the parent.
    if (parent_widget())
        parent_widget()->update();
}

String Widget::title() const
{
    return m_title;
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

void Widget::set_background_color(Gfx::Color color)
{
    auto _palette = palette();
    _palette.set_color(background_role(), color);
    set_palette(_palette);
}

Gfx::Palette Widget::palette() const
{
    return Gfx::Palette(*m_palette);
}

void Widget::set_grabbable_margins(Margins const& margins)
{
    if (m_grabbable_margins == margins)
        return;
    m_grabbable_margins = margins;
    layout_relevant_change_occurred();
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

void Widget::set_override_cursor(AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor)
{
    if (m_override_cursor == cursor)
        return;

    m_override_cursor = move(cursor);
    if (auto* window = this->window()) {
        window->update_cursor({});
    }
}

ErrorOr<void> Widget::load_from_gml(StringView gml_string)
{
    return load_from_gml(gml_string, [](StringView class_name) -> ErrorOr<NonnullRefPtr<Core::EventReceiver>> {
        dbgln("Class '{}' not registered", class_name);
        return Error::from_string_literal("Class not registered");
    });
}

ErrorOr<void> Widget::load_from_gml(StringView gml_string, UnregisteredChildHandler unregistered_child_handler)
{
    auto value = TRY(GML::parse_gml(gml_string));
    return load_from_gml_ast(value, unregistered_child_handler);
}

ErrorOr<void> Widget::load_from_gml_ast(NonnullRefPtr<GUI::GML::Node const> ast, UnregisteredChildHandler unregistered_child_handler)
{
    if (is<GUI::GML::GMLFile>(ast.ptr()))
        return load_from_gml_ast(static_cast<GUI::GML::GMLFile const&>(*ast).main_class(), unregistered_child_handler);

    VERIFY(is<GUI::GML::Object>(ast.ptr()));
    auto const& object = static_cast<GUI::GML::Object const&>(*ast);

    object.for_each_property([&](auto key, auto value) {
        set_property(key, value);
    });

    auto layout = object.layout_object();
    if (!layout.is_null()) {
        auto class_name = layout->name();
        if (class_name.is_null()) {
            return Error::from_string_literal("Invalid layout class name");
        }

        auto& layout_class = *GUI::ObjectClassRegistration::find("GUI::Layout"sv);
        if (auto* registration = GUI::ObjectClassRegistration::find(class_name)) {
            auto layout = TRY(registration->construct());
            if (!registration->is_derived_from(layout_class)) {
                dbgln("Invalid layout class: '{}'", class_name.to_byte_string());
                return Error::from_string_literal("Invalid layout class");
            }
            set_layout(static_ptr_cast<Layout>(layout));
        } else {
            dbgln("Unknown layout class: '{}'", class_name.to_byte_string());
            return Error::from_string_literal("Unknown layout class");
        }

        layout->for_each_property([&](auto key, auto value) {
            this->layout()->set_property(key, value);
        });
    }

    auto& widget_class = *GUI::ObjectClassRegistration::find("GUI::Widget"sv);
    bool is_tab_widget = is<TabWidget>(*this);
    TRY(object.try_for_each_child_object([&](auto const& child_data) -> ErrorOr<void> {
        auto class_name = child_data.name();

        // It is very questionable if this pseudo object should exist, but it works fine like this for now.
        if (class_name == "GUI::Layout::Spacer") {
            if (!this->layout()) {
                return Error::from_string_literal("Specified GUI::Layout::Spacer in GML, but the parent has no Layout.");
            }
            add_spacer();
        } else {
            RefPtr<Core::EventReceiver> child;
            if (auto* registration = GUI::ObjectClassRegistration::find(class_name)) {
                child = TRY(registration->construct());
                if (!registration->is_derived_from(widget_class)) {
                    dbgln("Invalid widget class: '{}'", class_name);
                    return Error::from_string_literal("Invalid widget class");
                }
            } else {
                child = TRY(unregistered_child_handler(class_name));
            }
            if (!child)
                return Error::from_string_literal("Unable to construct a Widget class for child");
            add_child(*child);

            // This is possible as we ensure that Widget is a base class above.
            TRY(static_ptr_cast<Widget>(child)->load_from_gml_ast(child_data, unregistered_child_handler));

            if (is_tab_widget) {
                // FIXME: We need to have the child added before loading it so that it can access us. But the TabWidget logic requires the child to not be present yet.
                remove_child(*child);
                reinterpret_cast<TabWidget*>(this)->add_widget(*static_ptr_cast<Widget>(child));
            }
        }

        return {};
    }));

    return {};
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

void Widget::set_shrink_to_fit(bool shrink_to_fit)
{
    // This function is deprecated, and soon to be removed, it is only still here to ease the transition to UIDimensions
    if (shrink_to_fit)
        set_preferred_size(SpecialDimension::Fit);
}

bool Widget::has_pending_drop() const
{
    return Application::the()->pending_drop_widget() == this;
}

bool Widget::is_visible_for_timer_purposes() const
{
    return is_visible() && Object::is_visible_for_timer_purposes();
}

void Widget::add_spacer()
{
    VERIFY(layout());
    return layout()->add_spacer();
}

String Widget::font_family() const
{
    return m_font->family();
}

}
