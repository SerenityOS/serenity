/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "OutOfProcessWebView.h"
#include "WebContentClient.h"
#include <AK/DeprecatedString.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <LibWeb/Crypto/Crypto.h>

REGISTER_WIDGET(WebView, OutOfProcessWebView)

namespace WebView {

OutOfProcessWebView::OutOfProcessWebView()
{
    set_should_hide_unnecessary_scrollbars(true);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);

    create_client();

    on_did_layout = [this](auto content_size) {
        set_content_size(content_size);
    };

    on_ready_to_paint = [this]() {
        update();
    };

    on_request_file = [this](auto const& path, auto request_id) {
        auto file = FileSystemAccessClient::Client::the().request_file_read_only_approved(window(), path);

        if (file.is_error())
            client().async_handle_file_return(file.error().code(), {}, request_id);
        else
            client().async_handle_file_return(0, IPC::File(file.value().stream()), request_id);
    };

    on_scroll_by_delta = [this](auto x_delta, auto y_delta) {
        horizontal_scrollbar().increase_slider_by(x_delta);
        vertical_scrollbar().increase_slider_by(y_delta);
    };

    on_scroll_to_point = [this](auto position) {
        horizontal_scrollbar().set_value(position.x());
        vertical_scrollbar().set_value(position.y());
    };

    on_scroll_into_view = [this](auto rect) {
        scroll_into_view(rect, true, true);
    };

    on_cursor_change = [this](auto cursor) {
        set_override_cursor(cursor);
    };

    on_enter_tooltip_area = [](auto, auto tooltip) {
        GUI::Application::the()->show_tooltip(tooltip, nullptr);
    };

    on_leave_tooltip_area = []() {
        GUI::Application::the()->hide_tooltip();
    };

    on_finish_handling_input_event = [this](auto event_was_accepted) {
        did_finish_handling_input_event(event_was_accepted);
    };
}

OutOfProcessWebView::~OutOfProcessWebView() = default;

void OutOfProcessWebView::create_client(EnableCallgrindProfiling)
{
    m_client_state = {};

    m_client_state.client = WebContentClient::try_create(*this).release_value_but_fixme_should_propagate_errors();
    m_client_state.client->on_web_content_process_crash = [this] {
        deferred_invoke([this] {
            handle_web_content_process_crash();
        });
    };

    m_client_state.client_handle = Web::Crypto::generate_random_uuid().release_value_but_fixme_should_propagate_errors();
    client().async_set_window_handle(m_client_state.client_handle);

    client().async_update_system_theme(Gfx::current_system_theme_buffer());
    client().async_update_system_fonts(Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query(), Gfx::FontDatabase::window_title_font_query());
    client().async_update_screen_rects(GUI::Desktop::the().rects(), GUI::Desktop::the().main_screen_index());
}

void OutOfProcessWebView::paint_event(GUI::PaintEvent& event)
{
    Super::paint_event(event);

    // If the available size is empty, we don't have a front or back bitmap to draw.
    if (available_size().is_empty())
        return;

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (auto* bitmap = m_client_state.has_usable_bitmap ? m_client_state.front_bitmap.bitmap.ptr() : m_backup_bitmap.ptr()) {
        painter.add_clip_rect(frame_inner_rect());
        painter.translate(frame_thickness(), frame_thickness());
        if (m_content_scales_to_viewport) {
            auto bitmap_rect = Gfx::IntRect {
                {},
                m_client_state.has_usable_bitmap
                    ? m_client_state.front_bitmap.last_painted_size
                    : m_backup_bitmap_size
            };
            painter.draw_scaled_bitmap(rect(), *bitmap, bitmap_rect);
        } else {
            painter.blit({ 0, 0 }, *bitmap, bitmap->rect());
        }
        return;
    }

    painter.fill_rect(frame_inner_rect(), palette().base());
}

void OutOfProcessWebView::resize_event(GUI::ResizeEvent& event)
{
    Super::resize_event(event);
    client().async_set_viewport_rect(Gfx::IntRect({ horizontal_scrollbar().value(), vertical_scrollbar().value() }, available_size()));
    handle_resize();
}

Gfx::IntRect OutOfProcessWebView::viewport_rect() const
{
    return visible_content_rect();
}

Gfx::IntPoint OutOfProcessWebView::to_content_position(Gfx::IntPoint widget_position) const
{
    return GUI::AbstractScrollableWidget::to_content_position(widget_position);
}

Gfx::IntPoint OutOfProcessWebView::to_widget_position(Gfx::IntPoint content_position) const
{
    return GUI::AbstractScrollableWidget::to_widget_position(content_position);
}

void OutOfProcessWebView::update_zoom()
{
    client().async_set_device_pixels_per_css_pixel(m_device_pixel_ratio * m_zoom_level);
    // FIXME: Refactor this into separate update_viewport_rect() + request_repaint() like in Ladybird
    handle_resize();
}

void OutOfProcessWebView::keydown_event(GUI::KeyEvent& event)
{
    enqueue_input_event(event);
}

void OutOfProcessWebView::keyup_event(GUI::KeyEvent& event)
{
    enqueue_input_event(event);
}

void OutOfProcessWebView::mousedown_event(GUI::MouseEvent& event)
{
    enqueue_input_event(event);
}

void OutOfProcessWebView::mouseup_event(GUI::MouseEvent& event)
{
    enqueue_input_event(event);

    if (event.button() == GUI::MouseButton::Backward) {
        if (on_navigate_back)
            on_navigate_back();
    } else if (event.button() == GUI::MouseButton::Forward) {
        if (on_navigate_forward)
            on_navigate_forward();
    }
}

void OutOfProcessWebView::mousemove_event(GUI::MouseEvent& event)
{
    enqueue_input_event(event);
}

void OutOfProcessWebView::mousewheel_event(GUI::MouseEvent& event)
{
    enqueue_input_event(event);
}

void OutOfProcessWebView::doubleclick_event(GUI::MouseEvent& event)
{
    enqueue_input_event(event);
}

void OutOfProcessWebView::theme_change_event(GUI::ThemeChangeEvent& event)
{
    Super::theme_change_event(event);
    client().async_update_system_theme(Gfx::current_system_theme_buffer());
    request_repaint();
}

void OutOfProcessWebView::screen_rects_change_event(GUI::ScreenRectsChangeEvent& event)
{
    client().async_update_screen_rects(event.rects(), event.main_screen_index());
}

void OutOfProcessWebView::did_scroll()
{
    client().async_set_viewport_rect(visible_content_rect());
    request_repaint();
}

DeprecatedString OutOfProcessWebView::dump_layout_tree()
{
    return client().dump_layout_tree();
}

OrderedHashMap<DeprecatedString, DeprecatedString> OutOfProcessWebView::get_local_storage_entries()
{
    return client().get_local_storage_entries();
}

OrderedHashMap<DeprecatedString, DeprecatedString> OutOfProcessWebView::get_session_storage_entries()
{
    return client().get_session_storage_entries();
}

void OutOfProcessWebView::set_content_filters(Vector<String> filters)
{
    client().async_set_content_filters(move(filters));
}

void OutOfProcessWebView::set_autoplay_allowed_on_all_websites()
{
    client().async_set_autoplay_allowed_on_all_websites();
}

void OutOfProcessWebView::set_autoplay_allowlist(Vector<String> allowlist)
{
    client().async_set_autoplay_allowlist(move(allowlist));
}

void OutOfProcessWebView::set_proxy_mappings(Vector<DeprecatedString> proxies, HashMap<DeprecatedString, size_t> mappings)
{
    client().async_set_proxy_mappings(move(proxies), move(mappings));
}

void OutOfProcessWebView::connect_to_webdriver(DeprecatedString const& webdriver_ipc_path)
{
    client().async_connect_to_webdriver(webdriver_ipc_path);
}

void OutOfProcessWebView::set_window_position(Gfx::IntPoint position)
{
    client().async_set_window_position(position);
}

void OutOfProcessWebView::set_window_size(Gfx::IntSize size)
{
    client().async_set_window_size(size);
}

void OutOfProcessWebView::focusin_event(GUI::FocusEvent&)
{
    client().async_set_has_focus(true);
}

void OutOfProcessWebView::focusout_event(GUI::FocusEvent&)
{
    client().async_set_has_focus(false);
}

void OutOfProcessWebView::set_system_visibility_state(bool visible)
{
    client().async_set_system_visibility_state(visible);
}

void OutOfProcessWebView::show_event(GUI::ShowEvent&)
{
    set_system_visibility_state(true);
}

void OutOfProcessWebView::hide_event(GUI::HideEvent&)
{
    set_system_visibility_state(false);
}

void OutOfProcessWebView::enqueue_input_event(InputEvent const& event)
{
    m_pending_input_events.enqueue(event);
    process_next_input_event();
}

void OutOfProcessWebView::process_next_input_event()
{
    if (m_pending_input_events.is_empty())
        return;

    if (m_is_awaiting_response_for_input_event)
        return;
    m_is_awaiting_response_for_input_event = true;

    // Send the next event over to the web content to be handled by JS.
    // We'll later get a message to say whether JS prevented the default event behavior,
    // at which point we either discard or handle that event, then try and process the next one.
    auto event = m_pending_input_events.head();
    event.visit(
        [this](GUI::KeyEvent const& event) {
            switch (event.type()) {
            case GUI::Event::Type::KeyDown:
                client().async_key_down(event.key(), event.modifiers(), event.code_point());
                break;
            case GUI::Event::Type::KeyUp:
                client().async_key_up(event.key(), event.modifiers(), event.code_point());
                break;
            default:
                dbgln("Unrecognized key event type in OOPWV input event queue: {}", event.type());
                VERIFY_NOT_REACHED();
            }
        },
        [this](GUI::MouseEvent const& event) {
            switch (event.type()) {
            case GUI::Event::Type::MouseDown:
                client().async_mouse_down(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers());
                break;
            case GUI::Event::Type::MouseUp:
                client().async_mouse_up(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers());
                break;
            case GUI::Event::Type::MouseMove:
                client().async_mouse_move(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers());
                break;
            case GUI::Event::Type::MouseWheel: {
                // FIXME: This wheel delta step size multiplier is used to remain the old scroll behaviour, in future use system step size.
                constexpr int scroll_step_size = 24;
                client().async_mouse_wheel(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers(), event.wheel_delta_x() * scroll_step_size, event.wheel_delta_y() * scroll_step_size);
                break;
            }
            case GUI::Event::Type::MouseDoubleClick:
                client().async_doubleclick(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers());
                break;
            default:
                dbgln("Unrecognized mouse event type in OOPWV input event queue: {}", event.type());
                VERIFY_NOT_REACHED();
            }
        });
}

void OutOfProcessWebView::did_finish_handling_input_event(bool event_was_accepted)
{
    VERIFY(m_is_awaiting_response_for_input_event);

    auto event = m_pending_input_events.dequeue();
    m_is_awaiting_response_for_input_event = false;

    if (!event_was_accepted) {
        // Here we handle events that were not consumed or cancelled by web content.
        // That is, we manually implement the steps that would have happened if the original
        // OutOfProcessWebView::foo_event() had called event.ignore().
        //
        // The first step is to give our superclass a chance to handle the event.
        //
        // Then, if it does not, we dispatch the event to our parent widget, but limited so
        // that it will never bubble up to the Window. (Otherwise, it would then dispatch the
        // event to us since we are the focused widget, and it would go round indefinitely.)
        //
        // Finally, any unhandled KeyDown events are propagated to trigger any Actions.
        event.visit(
            [this](GUI::KeyEvent& event) {
                switch (event.type()) {
                case GUI::Event::Type::KeyDown:
                    Super::keydown_event(event);
                    break;
                case GUI::Event::Type::KeyUp:
                    Super::keyup_event(event);
                    break;
                default:
                    dbgln("Unhandled key event type in OOPWV input event queue: {}", event.type());
                    VERIFY_NOT_REACHED();
                }

                if (!event.is_accepted()) {
                    parent_widget()->dispatch_event(event, window());

                    // NOTE: If other events can ever trigger shortcuts, propagate those here.
                    if (!event.is_accepted() && event.type() == GUI::Event::Type::KeyDown)
                        window()->propagate_shortcuts(event, this);
                }
            },
            [this](GUI::MouseEvent& event) {
                switch (event.type()) {
                case GUI::Event::Type::MouseDown:
                    Super::mousedown_event(event);
                    break;
                case GUI::Event::Type::MouseUp:
                    Super::mouseup_event(event);
                    break;
                case GUI::Event::Type::MouseMove:
                    Super::mousemove_event(event);
                    break;
                case GUI::Event::Type::MouseWheel:
                    Super::mousewheel_event(event);
                    break;
                case GUI::Event::Type::MouseDoubleClick:
                    Super::doubleclick_event(event);
                    break;
                default:
                    dbgln("Unhandled mouse event type in OOPWV input event queue: {}", event.type());
                    VERIFY_NOT_REACHED();
                }

                if (!event.is_accepted())
                    parent_widget()->dispatch_event(event, window());
                // FIXME: Propagate event for mouse-button shortcuts once that is implemented.
            });
    }

    process_next_input_event();
}

void OutOfProcessWebView::set_content_scales_to_viewport(bool b)
{
    m_content_scales_to_viewport = b;
}

void OutOfProcessWebView::set_user_style_sheet(String source)
{
    client().async_set_user_style(source);
}

}
