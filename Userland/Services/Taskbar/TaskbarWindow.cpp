/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskbarWindow.h"
#include "ClockWidget.h"
#include "QuickLaunchWidget.h"
#include "TaskbarButton.h"
#include <AK/Debug.h>
#include <LibConfig/Client.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowManagerServerConnection.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <serenity.h>
#include <stdio.h>

class TaskbarWidget final : public GUI::Widget {
    C_OBJECT(TaskbarWidget);

public:
    virtual ~TaskbarWidget() override { }

private:
    TaskbarWidget(Gfx::Alignment location)
        : m_location(move(location))
    {
    }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.fill_rect(rect(), palette().button());

        switch (m_location) {
        case Gfx::Alignment::Bottom:
            painter.draw_line({ 0, 1 }, { width() - 1, 1 }, palette().threed_highlight());
            break;
        case Gfx::Alignment::Top:
            painter.draw_line({ 0, 1 }, { width() - 1, 1 }, palette().threed_highlight());
            break;
        case Gfx::Alignment::Left:
            painter.draw_line({ width() - 2, 0 }, { width() - 2, height() }, palette().threed_highlight());
            break;
        case Gfx::Alignment::Right:
            painter.draw_line({ 0, 1 }, { 0, height() }, palette().threed_highlight());
            break;
        case Gfx::Alignment::TopCenter:
        case Gfx::Alignment::BottomCenter:
        case Gfx::Alignment::Center:
            VERIFY_NOT_REACHED();
        }
    }

    virtual void did_layout() override
    {
        WindowList::the().for_each_window([&](auto& window) {
            if (auto* button = window.button())
                static_cast<TaskbarButton*>(button)->update_taskbar_rect();
        });
    }

    Gfx::Alignment m_location;
};

TaskbarWindow::TaskbarWindow(Gfx::Alignment location, NonnullRefPtr<GUI::Menu> start_menu)
    : m_start_menu(move(start_menu))
    , m_location(move(location))
{
    set_window_type(GUI::WindowType::Taskbar);
    set_title("Taskbar");

    m_orientation = Gfx::Orientation::Horizontal;
    if (m_location == Gfx::Alignment::Left || m_location == Gfx::Alignment::Right)
        m_orientation = Gfx::Orientation::Vertical;

    on_screen_rects_change(GUI::Desktop::the().rects(), GUI::Desktop::the().main_screen_index());

    auto& main_widget = set_main_widget<TaskbarWidget>(m_location);
    if (m_orientation == Gfx::Orientation::Vertical) {
        main_widget.set_layout<GUI::VerticalBoxLayout>();
        main_widget.layout()->set_margins({ 1, 3, 3, 1 });
    } else {
        main_widget.set_layout<GUI::HorizontalBoxLayout>();
        main_widget.layout()->set_margins({ 3, 1, 1, 3 });
    }

    m_start_button = GUI::Button::construct("Serenity");
    set_start_button_font(Gfx::FontDatabase::default_font().bold_variant());
    m_start_button->set_icon_spacing(0);
    auto app_icon = GUI::Icon::default_icon("ladyball");
    m_start_button->set_tooltip("Start menu");
    m_start_button->set_icon(app_icon.bitmap_for_size(16));
    m_start_button->set_menu(m_start_menu);
    if (m_orientation == Gfx::Orientation::Vertical) {
        m_start_button->set_min_size(37, 37);
        m_start_button->set_max_size(37, 37);
        m_start_button->set_text("");
    }

    main_widget.add_child(*m_start_button);
    main_widget.add<Taskbar::QuickLaunchWidget>(m_orientation);

    m_task_button_container = main_widget.add<GUI::Widget>();
    if (m_orientation == Gfx::Orientation::Vertical)
        m_task_button_container->set_layout<GUI::VerticalBoxLayout>();
    else
        m_task_button_container->set_layout<GUI::HorizontalBoxLayout>();

    m_task_button_container->layout()->set_spacing(3);

    m_default_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/window.png").release_value_but_fixme_should_propagate_errors();

    m_applet_area_container = main_widget.add<GUI::Frame>();
    m_applet_area_container->set_frame_thickness(1);
    m_applet_area_container->set_frame_shape(Gfx::FrameShape::Box);
    m_applet_area_container->set_frame_shadow(Gfx::FrameShadow::Sunken);

    auto& clock_widget = main_widget.add<Taskbar::ClockWidget>(m_location);
    if (m_orientation == Gfx::Orientation::Vertical) {
        clock_widget.set_max_size(37, 37);
    }

    m_show_desktop_button = GUI::Button::construct();
    m_show_desktop_button->set_tooltip("Show Desktop");
    m_show_desktop_button->set_icon(GUI::Icon::default_icon("desktop").bitmap_for_size(16));
    m_show_desktop_button->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_show_desktop_button->on_click = TaskbarWindow::show_desktop_button_clicked;
    if (m_orientation == Gfx::Orientation::Vertical) {
        m_show_desktop_button->set_fixed_size(37, 37);
    } else {
        m_show_desktop_button->set_fixed_size(24, 24);
    }
    main_widget.add_child(*m_show_desktop_button);

    auto af_path = String::formatted("{}/{}", Desktop::AppFile::APP_FILES_DIRECTORY, "Assistant.af");
    m_assistant_app_file = Desktop::AppFile::open(af_path);
}

TaskbarWindow::~TaskbarWindow()
{
}

void TaskbarWindow::show_desktop_button_clicked(unsigned)
{
    GUI::WindowManagerServerConnection::the().async_toggle_show_desktop();
}

void TaskbarWindow::on_screen_rects_change(const Vector<Gfx::IntRect, 4>& rects, size_t main_screen_index)
{
    const auto& rect = rects[main_screen_index];
    Gfx::IntRect new_rect;

    switch (m_location) {
    case Gfx::Alignment::Bottom:
        new_rect = { rect.x(), rect.bottom() - taskbar_height() + 1, rect.width(), taskbar_height() };
        break;
    case Gfx::Alignment::Left:
        new_rect = { rect.x(), rect.top(), taskbar_width(), rect.height() };
        break;
    case Gfx::Alignment::Top:
        new_rect = { rect.x(), rect.top() - 1, rect.width(), taskbar_height() };
        break;
    case Gfx::Alignment::Right:
        new_rect = { rect.right() - taskbar_width() + 1, rect.top(), taskbar_width(), rect.height() };
        break;
    case Gfx::Alignment::TopCenter:
    case Gfx::Alignment::BottomCenter:
    case Gfx::Alignment::Center:
        VERIFY_NOT_REACHED();
    }

    set_rect(new_rect);
    update_applet_area();
}

void TaskbarWindow::update_applet_area()
{
    // NOTE: Widget layout is normally lazy, but here we have to force it right away so we can tell
    //       WindowServer where to place the applet area window.
    if (!main_widget())
        return;
    main_widget()->do_layout();
    auto new_rect = Gfx::IntRect({}, m_applet_area_size).centered_within(m_applet_area_container->screen_relative_rect());
    GUI::WindowManagerServerConnection::the().async_set_applet_area_position(new_rect.location());
}

NonnullRefPtr<GUI::Button> TaskbarWindow::create_button(const WindowIdentifier& identifier)
{
    auto& button = m_task_button_container->add<TaskbarButton>(identifier);
    if (m_orientation == Gfx::Orientation::Vertical) {
        button.set_min_size(33, 20);
        button.set_max_size(33, 33);
        button.set_icon_spacing(0);
        button.set_text("");
    } else {
        button.set_min_size(20, 21);
        button.set_max_size(140, 21);
    }
    button.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    button.set_icon(*m_default_icon);
    return button;
}

void TaskbarWindow::add_window_button(::Window& window, const WindowIdentifier& identifier)
{
    if (window.button())
        return;
    window.set_button(create_button(identifier));
    auto* button = window.button();
    button->on_click = [window = &window, identifier, button](auto) {
        // We need to look at the button's checked state here to figure
        // out if the application is active or not. That's because this
        // button's window may not actually be active when a modal window
        // is displayed, in which case window->is_active() would return
        // false because window is the modal window's owner (which is not
        // active)
        if (window->is_minimized() || !button->is_checked()) {
            GUI::WindowManagerServerConnection::the().async_set_active_window(identifier.client_id(), identifier.window_id());
        } else {
            GUI::WindowManagerServerConnection::the().async_set_window_minimized(identifier.client_id(), identifier.window_id(), true);
        }
    };
}

void TaskbarWindow::remove_window_button(::Window& window, bool was_removed)
{
    auto* button = window.button();
    if (!button)
        return;
    if (!was_removed)
        static_cast<TaskbarButton*>(button)->clear_taskbar_rect();
    window.set_button(nullptr);
    button->remove_from_parent();
}

void TaskbarWindow::update_window_button(::Window& window, bool show_as_active)
{
    auto* button = window.button();
    if (!button)
        return;

    if (m_orientation == Gfx::Orientation::Horizontal)
        button->set_text(window.title());

    button->set_tooltip(window.title());
    button->set_checked(show_as_active);
    button->set_visible(is_window_on_current_workspace(window));
}

::Window* TaskbarWindow::find_window_owner(::Window& window) const
{
    if (!window.is_modal())
        return &window;

    ::Window* parent = nullptr;
    auto* current_window = &window;
    while (current_window) {
        parent = WindowList::the().find_parent(*current_window);
        if (!parent || !parent->is_modal())
            break;
        current_window = parent;
    }
    return parent;
}

void TaskbarWindow::event(Core::Event& event)
{
    switch (event.type()) {
    case GUI::Event::MouseDown: {
        // If the cursor is at the edge/corner of the screen but technically not within the start button (or other taskbar buttons),
        // we adjust it so that the nearest button ends up being clicked anyways.

        auto& mouse_event = static_cast<GUI::MouseEvent&>(event);
        const int ADJUSTMENT = 4;
        auto adjusted_x = AK::clamp(mouse_event.x(), ADJUSTMENT, width() - ADJUSTMENT);
        auto adjusted_y = AK::min(mouse_event.y(), height() - ADJUSTMENT);
        Gfx::IntPoint adjusted_point = { adjusted_x, adjusted_y };

        if (adjusted_point != mouse_event.position()) {
            GUI::WindowServerConnection::the().async_set_global_cursor_position(position() + adjusted_point);
            GUI::MouseEvent adjusted_event = { (GUI::Event::Type)mouse_event.type(), adjusted_point, mouse_event.buttons(), mouse_event.button(), mouse_event.modifiers(), mouse_event.wheel_delta() };
            Window::event(adjusted_event);
            return;
        }
        break;
    }
    case GUI::Event::FontsChange:
        set_start_button_font(Gfx::FontDatabase::default_font().bold_variant());
        break;
    }
    Window::event(event);
}

void TaskbarWindow::wm_event(GUI::WMEvent& event)
{
    WindowIdentifier identifier { event.client_id(), event.window_id() };
    switch (event.type()) {
    case GUI::Event::WM_WindowRemoved: {
        if constexpr (EVENT_DEBUG) {
            auto& removed_event = static_cast<GUI::WMWindowRemovedEvent&>(event);
            dbgln("WM_WindowRemoved: client_id={}, window_id={}",
                removed_event.client_id(),
                removed_event.window_id());
        }
        if (auto* window = WindowList::the().window(identifier))
            remove_window_button(*window, true);
        WindowList::the().remove_window(identifier);
        update();
        break;
    }
    case GUI::Event::WM_WindowRectChanged: {
        if constexpr (EVENT_DEBUG) {
            auto& changed_event = static_cast<GUI::WMWindowRectChangedEvent&>(event);
            dbgln("WM_WindowRectChanged: client_id={}, window_id={}, rect={}",
                changed_event.client_id(),
                changed_event.window_id(),
                changed_event.rect());
        }
        break;
    }

    case GUI::Event::WM_WindowIconBitmapChanged: {
        auto& changed_event = static_cast<GUI::WMWindowIconBitmapChangedEvent&>(event);
        if (auto* window = WindowList::the().window(identifier)) {
            if (window->button()) {
                auto icon = changed_event.bitmap();
                if (icon->height() != taskbar_icon_size() || icon->width() != taskbar_icon_size()) {
                    auto sw = taskbar_icon_size() / (float)icon->width();
                    auto sh = taskbar_icon_size() / (float)icon->height();
                    auto scaled_bitmap_or_error = icon->scaled(sw, sh);
                    if (scaled_bitmap_or_error.is_error())
                        window->button()->set_icon(nullptr);
                    else
                        window->button()->set_icon(scaled_bitmap_or_error.release_value());
                } else {
                    window->button()->set_icon(icon);
                }
            }
        }
        break;
    }

    case GUI::Event::WM_WindowStateChanged: {
        auto& changed_event = static_cast<GUI::WMWindowStateChangedEvent&>(event);
        if constexpr (EVENT_DEBUG) {
            dbgln("WM_WindowStateChanged: client_id={}, window_id={}, title={}, rect={}, is_active={}, is_minimized={}",
                changed_event.client_id(),
                changed_event.window_id(),
                changed_event.title(),
                changed_event.rect(),
                changed_event.is_active(),
                changed_event.is_minimized());
        }
        if (changed_event.window_type() != GUI::WindowType::Normal || changed_event.is_frameless()) {
            if (auto* window = WindowList::the().window(identifier))
                remove_window_button(*window, false);
            break;
        }
        auto& window = WindowList::the().ensure_window(identifier);
        window.set_parent_identifier({ changed_event.parent_client_id(), changed_event.parent_window_id() });
        if (!window.is_modal())
            add_window_button(window, identifier);
        else
            remove_window_button(window, false);
        window.set_title(changed_event.title());
        window.set_rect(changed_event.rect());
        window.set_modal(changed_event.is_modal());
        window.set_active(changed_event.is_active());
        window.set_minimized(changed_event.is_minimized());
        window.set_progress(changed_event.progress());
        window.set_workspace(changed_event.workspace_row(), changed_event.workspace_column());

        auto* window_owner = find_window_owner(window);
        if (window_owner == &window) {
            update_window_button(window, window.is_active());
        } else if (window_owner) {
            // check the window owner's button if the modal's window button
            // would have been checked
            VERIFY(window.is_modal());
            update_window_button(*window_owner, window.is_active());
        }
        break;
    }
    case GUI::Event::WM_AppletAreaSizeChanged: {
        auto& changed_event = static_cast<GUI::WMAppletAreaSizeChangedEvent&>(event);
        m_applet_area_size = changed_event.size();
        m_applet_area_container->set_fixed_size(changed_event.size().width() + 8, 21);
        update_applet_area();
        break;
    }
    case GUI::Event::WM_SuperKeyPressed: {
        if (m_start_menu->is_visible()) {
            m_start_menu->dismiss();
        } else {
            m_start_menu->popup(m_start_button->screen_relative_rect().top_left());
        }
        break;
    }
    case GUI::Event::WM_SuperSpaceKeyPressed: {
        if (!m_assistant_app_file->spawn())
            warnln("failed to spawn 'Assistant' when requested via Super+Space");
        break;
    }
    case GUI::Event::WM_WorkspaceChanged: {
        auto& changed_event = static_cast<GUI::WMWorkspaceChangedEvent&>(event);
        workspace_change_event(changed_event.current_row(), changed_event.current_column());
        break;
    }
    default:
        break;
    }
}

void TaskbarWindow::screen_rects_change_event(GUI::ScreenRectsChangeEvent& event)
{
    on_screen_rects_change(event.rects(), event.main_screen_index());
}

void TaskbarWindow::config_string_did_change(String const& domain, String const& group, String const& key, String const& value)
{
    if (domain == "Taskbar" && group == "Appearance" && key == "Location") {
        Gfx::Orientation new_orientation;
        if (value == "Bottom" || value == "Top")
            new_orientation = Gfx::Orientation::Horizontal;
        else
            new_orientation = Gfx::Orientation::Vertical;

        // Kill ourselves to force a complete reflow. :^)
        // FIXME: This is currently needed due to a lot of reasons,
        //        but mainly to ensure all positions and sizes are correct.
        if (new_orientation != m_orientation)
            exit(0);
    }
}

bool TaskbarWindow::is_window_on_current_workspace(::Window& window) const
{
    return window.workspace_row() == m_current_workspace_row && window.workspace_column() == m_current_workspace_column;
}

void TaskbarWindow::workspace_change_event(unsigned current_row, unsigned current_column)
{
    m_current_workspace_row = current_row;
    m_current_workspace_column = current_column;

    WindowList::the().for_each_window([&](auto& window) {
        if (auto* button = window.button())
            button->set_visible(is_window_on_current_workspace(window));
    });
}

void TaskbarWindow::set_start_button_font(Gfx::Font const& font)
{
    m_start_button->set_font(font);
    m_start_button->set_fixed_size(font.width(m_start_button->text()) + 30, 21);
}
