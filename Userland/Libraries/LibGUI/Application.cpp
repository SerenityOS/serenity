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

#include <AK/NeverDestroyed.h>
#include <LibCore/EventLoop.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

namespace GUI {

class Application::TooltipWindow final : public Window {
    C_OBJECT(TooltipWindow);

public:
    void set_tooltip(String tooltip)
    {
        // FIXME: Add some kind of GUI::Label auto-sizing feature.
        int text_width = m_label->font().width(tooltip);
        set_rect(rect().x(), rect().y(), text_width + 10, m_label->font().glyph_height() + 8);
        m_label->set_text(move(tooltip));
    }

private:
    TooltipWindow()
    {
        set_window_type(WindowType::Tooltip);
        m_label = set_main_widget<Label>();
        m_label->set_background_role(Gfx::ColorRole::Tooltip);
        m_label->set_foreground_role(Gfx::ColorRole::TooltipText);
        m_label->set_fill_with_background_color(true);
        m_label->set_frame_thickness(1);
        m_label->set_frame_shape(Gfx::FrameShape::Container);
        m_label->set_frame_shadow(Gfx::FrameShadow::Plain);
    }

    RefPtr<Label> m_label;
};

static NeverDestroyed<WeakPtr<Application>> s_the;

Application* Application::the()
{
    // NOTE: If we don't explicitly call revoke_weak_ptrs() in the
    // ~Application destructor, we would have to change this to
    // return s_the->strong_ref().ptr();
    // This is because this is using the unsafe operator*/operator->
    // that do not have the ability to check the ref count!
    return *s_the;
}

Application::Application(int argc, char** argv)
{
    VERIFY(!*s_the);
    *s_the = *this;
    m_event_loop = make<Core::EventLoop>();
    WindowServerConnection::the();
    Clipboard::initialize({});
    if (argc > 0)
        m_invoked_as = argv[0];

    if (getenv("GUI_FOCUS_DEBUG"))
        m_focus_debugging_enabled = true;

    if (getenv("GUI_DND_DEBUG"))
        m_dnd_debugging_enabled = true;

    for (int i = 1; i < argc; i++) {
        String arg(argv[i]);
        m_args.append(move(arg));
    }

    m_tooltip_show_timer = Core::Timer::create_single_shot(700, [this] {
        tooltip_show_timer_did_fire();
    });

    m_tooltip_hide_timer = Core::Timer::create_single_shot(50, [this] {
        tooltip_hide_timer_did_fire();
    });
}

Application::~Application()
{
    revoke_weak_ptrs();
}

int Application::exec()
{
    return m_event_loop->exec();
}

void Application::quit(int exit_code)
{
    m_event_loop->quit(exit_code);
}

void Application::set_menubar(RefPtr<MenuBar> menubar)
{
    if (m_menubar)
        m_menubar->notify_removed_from_application({});
    m_menubar = move(menubar);
    if (m_menubar)
        m_menubar->notify_added_to_application({});
}

void Application::register_global_shortcut_action(Badge<Action>, Action& action)
{
    m_global_shortcut_actions.set(action.shortcut(), &action);
}

void Application::unregister_global_shortcut_action(Badge<Action>, Action& action)
{
    m_global_shortcut_actions.remove(action.shortcut());
}

Action* Application::action_for_key_event(const KeyEvent& event)
{
    auto it = m_global_shortcut_actions.find(Shortcut(event.modifiers(), (KeyCode)event.key()));
    if (it == m_global_shortcut_actions.end())
        return nullptr;
    return (*it).value;
}

void Application::show_tooltip(String tooltip, const Widget* tooltip_source_widget)
{
    m_tooltip_source_widget = tooltip_source_widget;
    if (!m_tooltip_window) {
        m_tooltip_window = TooltipWindow::construct();
        m_tooltip_window->set_double_buffering_enabled(false);
    }
    m_tooltip_window->set_tooltip(move(tooltip));

    if (m_tooltip_window->is_visible()) {
        tooltip_show_timer_did_fire();
        m_tooltip_show_timer->stop();
        m_tooltip_hide_timer->stop();
    } else {
        m_tooltip_show_timer->restart();
        m_tooltip_hide_timer->stop();
    }
}

void Application::hide_tooltip()
{
    m_tooltip_show_timer->stop();
    m_tooltip_hide_timer->start();
}

void Application::did_create_window(Badge<Window>)
{
    if (m_event_loop->was_exit_requested())
        m_event_loop->unquit();
}

void Application::did_delete_last_window(Badge<Window>)
{
    if (m_quit_when_last_window_deleted)
        m_event_loop->quit(0);
}

void Application::set_system_palette(Core::AnonymousBuffer& buffer)
{
    if (!m_system_palette)
        m_system_palette = Gfx::PaletteImpl::create_with_anonymous_buffer(buffer);
    else
        m_system_palette->replace_internal_buffer({}, buffer);

    if (!m_palette)
        m_palette = m_system_palette;
}

void Application::set_palette(const Palette& palette)
{
    m_palette = palette.impl();
}

Gfx::Palette Application::palette() const
{
    return Palette(*m_palette);
}

void Application::tooltip_show_timer_did_fire()
{
    VERIFY(m_tooltip_window);
    Gfx::IntRect desktop_rect = Desktop::the().rect();

    const int margin = 30;
    Gfx::IntPoint adjusted_pos = WindowServerConnection::the().send_sync<Messages::WindowServer::GetGlobalCursorPosition>()->position();

    adjusted_pos.move_by(0, 18);

    if (adjusted_pos.x() + m_tooltip_window->width() >= desktop_rect.width() - margin) {
        adjusted_pos = adjusted_pos.translated(-m_tooltip_window->width(), 0);
    }
    if (adjusted_pos.y() + m_tooltip_window->height() >= desktop_rect.height() - margin) {
        adjusted_pos = adjusted_pos.translated(0, -(m_tooltip_window->height() * 2));
    }

    m_tooltip_window->move_to(adjusted_pos);
    m_tooltip_window->show();
}

void Application::tooltip_hide_timer_did_fire()
{
    m_tooltip_source_widget = nullptr;
    if (m_tooltip_window)
        m_tooltip_window->hide();
}

void Application::window_did_become_active(Badge<Window>, Window& window)
{
    m_active_window = window.make_weak_ptr<Window>();
}

void Application::window_did_become_inactive(Badge<Window>, Window& window)
{
    if (m_active_window.ptr() != &window)
        return;
    m_active_window = nullptr;
}

void Application::set_pending_drop_widget(Widget* widget)
{
    if (m_pending_drop_widget == widget)
        return;
    if (m_pending_drop_widget)
        m_pending_drop_widget->update();
    m_pending_drop_widget = widget;
    if (m_pending_drop_widget)
        m_pending_drop_widget->update();
}

void Application::set_drag_hovered_widget_impl(Widget* widget, const Gfx::IntPoint& position, Vector<String> mime_types)
{
    if (widget == m_drag_hovered_widget)
        return;

    if (m_drag_hovered_widget) {
        Event leave_event(Event::DragLeave);
        m_drag_hovered_widget->dispatch_event(leave_event, m_drag_hovered_widget->window());
    }

    set_pending_drop_widget(nullptr);
    m_drag_hovered_widget = widget;

    if (m_drag_hovered_widget) {
        DragEvent enter_event(Event::DragEnter, position, move(mime_types));
        enter_event.ignore();
        m_drag_hovered_widget->dispatch_event(enter_event, m_drag_hovered_widget->window());
        if (enter_event.is_accepted())
            set_pending_drop_widget(m_drag_hovered_widget);
    }
}

void Application::notify_drag_cancelled(Badge<WindowServerConnection>)
{
    set_drag_hovered_widget_impl(nullptr);
}

}
