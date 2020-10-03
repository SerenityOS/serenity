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
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/Event.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Layout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/StatusBar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <unistd.h>

namespace GUI {

REGISTER_WIDGET(GUI, Button)
REGISTER_WIDGET(GUI, CheckBox)
REGISTER_WIDGET(GUI, ColorInput)
REGISTER_WIDGET(GUI, Frame)
REGISTER_WIDGET(GUI, GroupBox)
REGISTER_WIDGET(GUI, HorizontalSplitter)
REGISTER_WIDGET(GUI, Label)
REGISTER_WIDGET(GUI, RadioButton)
REGISTER_WIDGET(GUI, ScrollBar)
REGISTER_WIDGET(GUI, Slider)
REGISTER_WIDGET(GUI, SpinBox)
REGISTER_WIDGET(GUI, StatusBar)
REGISTER_WIDGET(GUI, TextBox)
REGISTER_WIDGET(GUI, TextEditor)
REGISTER_WIDGET(GUI, ToolBar)
REGISTER_WIDGET(GUI, ToolBarContainer)
REGISTER_WIDGET(GUI, Widget)

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
    : Core::Object(nullptr, true)
    , m_background_role(Gfx::ColorRole::Window)
    , m_foreground_role(Gfx::ColorRole::WindowText)
    , m_font(Gfx::Font::default_font())
    , m_palette(Application::the()->palette().impl())
{
    REGISTER_RECT_PROPERTY("relative_rect", relative_rect, set_relative_rect);
    REGISTER_BOOL_PROPERTY("fill_with_background_color", fill_with_background_color, set_fill_with_background_color);
    REGISTER_BOOL_PROPERTY("visible", is_visible, set_visible);
    REGISTER_BOOL_PROPERTY("focused", is_focused, set_focus);
    REGISTER_BOOL_PROPERTY("enabled", is_enabled, set_enabled);
    REGISTER_STRING_PROPERTY("tooltip", tooltip, set_tooltip);
    REGISTER_SIZE_PROPERTY("preferred_size", preferred_size, set_preferred_size);
    REGISTER_INT_PROPERTY("preferred_width", preferred_width, set_preferred_width);
    REGISTER_INT_PROPERTY("preferred_height", preferred_height, set_preferred_height);
    REGISTER_SIZE_POLICY_PROPERTY("horizontal_size_policy", horizontal_size_policy, set_horizontal_size_policy);
    REGISTER_SIZE_POLICY_PROPERTY("vertical_size_policy", vertical_size_policy, set_vertical_size_policy);
}

Widget::~Widget()
{
}

void Widget::child_event(Core::ChildEvent& event)
{
    if (event.type() == Event::ChildAdded) {
        if (event.child() && is<Widget>(*event.child()) && layout()) {
            if (event.insertion_before_child() && event.insertion_before_child()->is_widget())
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
    case Event::DragMove:
        return drag_move_event(static_cast<DragEvent&>(event));
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

    if (is_being_inspected()) {
        Painter painter(*this);
        painter.draw_rect(rect(), Color::Magenta);
    }

    if (Application::the()->focus_debugging_enabled()) {
        if (is_focused()) {
            Painter painter(*this);
            painter.draw_rect(rect(), Color::Cyan);
        }
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
    if (accepts_focus())
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
    show_tooltip();
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
    if (!event.alt() && !event.ctrl() && !event.logo() && event.key() == KeyCode::Key_Tab) {
        if (event.shift())
            focus_previous_widget(FocusSource::Keyboard);
        else
            focus_next_widget(FocusSource::Keyboard);
        event.accept();
        return;
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

void Widget::drag_move_event(DragEvent& event)
{
    dbg() << class_name() << "{" << this << "} DRAG MOVE  position: " << event.position() << ", data_type: '" << event.data_type() << "'";
    event.ignore();
}

void Widget::drop_event(DropEvent& event)
{
    dbg() << class_name() << "{" << this << "} DROP  position: " << event.position() << ", text: '" << event.text() << "'";
    event.ignore();
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

    if (proxy)
        m_focus_proxy = proxy->make_weak_ptr();
    else
        m_focus_proxy = nullptr;
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
        m_font = Gfx::Font::default_font();
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

void Widget::set_preferred_size(const Gfx::IntSize& size)
{
    if (m_preferred_size == size)
        return;
    m_preferred_size = size;
    invalidate_layout();
}

void Widget::set_size_policy(Orientation orientation, SizePolicy policy)
{
    if (orientation == Orientation::Horizontal)
        set_size_policy(policy, m_vertical_size_policy);
    else
        set_size_policy(m_horizontal_size_policy, policy);
}

void Widget::set_size_policy(SizePolicy horizontal_policy, SizePolicy vertical_policy)
{
    if (m_horizontal_size_policy == horizontal_policy && m_vertical_size_policy == vertical_policy)
        return;
    m_horizontal_size_policy = horizontal_policy;
    m_vertical_size_policy = vertical_policy;
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

void Widget::focus_previous_widget(FocusSource source)
{
    auto focusable_widgets = window()->focusable_widgets();
    for (int i = focusable_widgets.size() - 1; i >= 0; --i) {
        if (focusable_widgets[i] != this)
            continue;
        if (i > 0)
            focusable_widgets[i - 1]->set_focus(true, source);
        else
            focusable_widgets.last()->set_focus(true, source);
    }
}

void Widget::focus_next_widget(FocusSource source)
{
    auto focusable_widgets = window()->focusable_widgets();
    for (size_t i = 0; i < focusable_widgets.size(); ++i) {
        if (focusable_widgets[i] != this)
            continue;
        if (i < focusable_widgets.size() - 1)
            focusable_widgets[i + 1]->set_focus(true, source);
        else
            focusable_widgets.first()->set_focus(true, source);
    }
}

void Widget::set_backcolor(const StringView& color_string)
{
    auto color = Color::from_string(color_string);
    if (!color.has_value())
        return;
    set_background_color(color.value());
}

void Widget::set_forecolor(const StringView& color_string)
{
    auto color = Color::from_string(color_string);
    if (!color.has_value())
        return;
    set_foreground_color(color.value());
}

Vector<Widget*> Widget::child_widgets() const
{
    Vector<Widget*> widgets;
    widgets.ensure_capacity(children().size());
    for (auto& child : const_cast<Widget*>(this)->children()) {
        if (child.is_widget())
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
    if (GUI::Application::the()->tooltip_source_widget() == this)
        show_tooltip();
}

void Widget::show_tooltip()
{
    if (has_tooltip())
        Application::the()->show_tooltip(m_tooltip, screen_relative_rect().center().translated(0, height() / 2), this);
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

bool Widget::load_from_json(const StringView& json_string)
{
    auto json_value = JsonValue::from_string(json_string);
    if (!json_value.has_value()) {
        dbg() << "load_from_json parse failed: _" << json_string << "_";
        return false;
    }
    if (!json_value.value().is_object()) {
        dbg() << "load_from_json parse non-object";
        return false;
    }
    return load_from_json(json_value.value().as_object());
}

bool Widget::load_from_json(const JsonObject& json)
{
    json.for_each_member([&](auto& key, auto& value) {
        set_property(key, value);
    });

    auto layout_value = json.get("layout");
    if (!layout_value.is_null() && !layout_value.is_object()) {
        dbg() << "layout is not an object";
        return false;
    }
    if (layout_value.is_object()) {
        auto& layout = layout_value.as_object();
        auto class_name = layout.get("class");
        if (class_name.is_null()) {
            dbg() << "Invalid layout class name";
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
                dbg() << "No class name in entry";
                return false;
            }
            auto* registration = WidgetClassRegistration::find(class_name.as_string());
            if (!registration) {
                dbg() << "Class '" << class_name.as_string() << "' not registered";
                return false;
            }

            auto child_widget = registration->construct();
            add_child(*child_widget);
            child_widget->load_from_json(child_json);
        }
    }

    return true;
}

Widget* Widget::find_child_by_name(const String& name)
{
    Widget* found_widget = nullptr;
    for_each_child_widget([&](auto& child) {
        if (child.name() == name) {
            found_widget = &child;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_widget;
}

Widget* Widget::find_descendant_by_name(const String& name)
{
    Widget* found_widget = nullptr;
    if (this->name() == name)
        return this;
    for_each_child_widget([&](auto& child) {
        found_widget = child.find_descendant_by_name(name);
        if (found_widget)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    return found_widget;
}
}
