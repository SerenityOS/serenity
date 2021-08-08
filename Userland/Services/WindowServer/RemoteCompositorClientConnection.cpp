/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RemoteCompositorClientConnection.h"
#include "Compositor.h"
#include "Window.h"
#include <AK/Debug.h>

namespace WindowServer {

HashMap<int, NonnullRefPtr<RemoteCompositorClientConnection>> RemoteCompositorClientConnection::s_connections {};

RemoteCompositorClientConnection::RemoteCompositorClientConnection(NonnullRefPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ClientConnection<RemoteCompositorClientEndpoint, RemoteCompositorServerEndpoint>(*this, move(client_socket), client_id)
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "RemoteCompositorClientConnection {:p}", this);
    s_connections.set(client_id, *this);
}

RemoteCompositorClientConnection::~RemoteCompositorClientConnection()
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "~RemoteCompositorClientConnection {:p}", this);
    if (m_is_active)
        Compositor::the().set_remote_client(nullptr);
}

RemoteDesktop::Compositor::WindowId RemoteCompositorClientConnection::allocate_window_id()
{
    auto id = m_window_ids.find_first_unset();
    if (!id.has_value()) {
        m_window_ids.grow(32, false);
        id = m_window_ids.find_first_unset();
    }
    m_window_ids.set(id.value(), true);
    return id.value();
}

void RemoteCompositorClientConnection::free_window_id(RemoteDesktop::Compositor::WindowId id)
{
    m_window_ids.set(id, false);
}

void RemoteCompositorClientConnection::begin_compose()
{
    m_pending_occlusions.clear_with_capacity();
    m_pending_delete_windows.clear_with_capacity();
    m_pending_dirty_rects.clear_with_capacity();
}

void RemoteCompositorClientConnection::begin_update_occlusions()
{
    m_current_occlusions_tag++;
    m_ordered_window_data.clear_with_capacity();
    m_window_order_dirty = false;
}

RemoteCompositorClientConnection::WindowData::WindowData(RemoteDesktop::Compositor::WindowId id, u32 current_occlusions_tag, Window& window)
    : id(id)
    , window(&window)
    , occlusions_tag(current_occlusions_tag)
{
    update();
}

void RemoteCompositorClientConnection::WindowData::update()
{
    VERIFY(window);
    auto absolute_to_relative = -window->frame().render_rect().location();
    auto is_relative_equal = [&](Gfx::DisjointRectSet const& relative_rects, Gfx::DisjointRectSet const& absolute_rects) {
        auto& rects = relative_rects.rects();
        if (rects.size() != absolute_rects.size())
            return false;
        auto& abs_rects = absolute_rects.rects();
        for (size_t i = 0; i < rects.size(); i++) {
            auto& relative_rect = rects[i];
            if (relative_rect != abs_rects[i].translated(absolute_to_relative))
                return false;
        }
        return true;
    };
    if (!is_relative_equal(opaque_rects, window->opaque_rects())) {
        opaque_rects = window->opaque_rects().clone();
        opaque_rects.translate_by(absolute_to_relative);
        opaque_rects_dirty = true;
    }
    if (!window->transparency_wallpaper_rects().is_empty()) {
        auto all_transparent_rects = window->transparency_rects().clone();
        all_transparent_rects.add(window->transparency_wallpaper_rects());
        if (!is_relative_equal(transparent_rects, all_transparent_rects)) {
            transparent_rects = move(all_transparent_rects);
            transparent_rects.translate_by(absolute_to_relative);
            transparent_rects_dirty = true;
        }
    } else if (!is_relative_equal(transparent_rects, window->transparency_rects())) {
        transparent_rects = window->transparency_rects().clone();
        transparent_rects.translate_by(absolute_to_relative);
        transparent_rects_dirty = true;
    }
}

void RemoteCompositorClientConnection::update_window_occlusions(Window& window)
{
    auto it = m_window_data.find(&window);
    if (it != m_window_data.end()) {
        auto& window_data = *it->value;
        VERIFY(window_data.window == &window);
        dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Update WindowData {:p} for window {}", this, window_data.id);
        m_ordered_window_data.append(&window_data);
        window_data.update();
        window_data.occlusions_tag = m_current_occlusions_tag;
    } else {
        auto window_data = adopt_own_if_nonnull(new WindowData(allocate_window_id(), m_current_occlusions_tag, window));
        dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Created WindowData {:p} for window {}", this, window_data->id);
        m_ordered_window_data.append(window_data.ptr());
        m_window_order_dirty = true;
        auto result = m_window_data.set(&window, window_data.release_nonnull());
        VERIFY(result == AK::HashSetResult::InsertedNewEntry);
    }
}

void RemoteCompositorClientConnection::end_update_occlusions()
{
    Vector<WindowData*, 32> delete_windows;
    Vector<RemoteDesktop::Compositor::WindowId, 32> delete_windows_ids;
    for (auto& it : m_window_data) {
        auto& window_data = *it.value;
        if (window_data.occlusions_tag == m_current_occlusions_tag)
            continue;
        delete_windows.append(&window_data);
        m_pending_delete_windows.append(window_data.id);
    }
    for (auto* delete_window : delete_windows) {
        // m_ordered_window_data won't have this window already, no need to remove it
        dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Delete WindowData {:p} for window {}", this, delete_window->id);
        free_window_id(delete_window->id);
        bool removed = m_ordered_window_ids.remove_first_matching([&](auto& id) {
            return id == delete_window->id;
        });
        VERIFY(removed);
        m_window_data.remove(delete_window->window);
    }

    if (m_ordered_window_data.size() != m_ordered_window_ids.size())
        m_window_order_dirty = true;

    size_t index = 0;
    for (auto& window : m_ordered_window_data) {
        if (!m_window_order_dirty) {
            if (m_ordered_window_ids[index] != window->id) {
                m_window_order_dirty = true;
                break;
            }
        }
    }

    if (m_window_order_dirty)
        m_ordered_window_ids.clear_with_capacity();

    m_occlusions_dirty = false;

    // TODO: This seems overly inefficient

    for (auto* window_data : m_ordered_window_data) {
        if (m_window_order_dirty)
            m_ordered_window_ids.append(window_data->id);

        RemoteDesktop::Compositor::Window compositor_window {
            .id = window_data->id,
            .client_id = window_data->window->client_id()
        };
        bool anything_dirty = false;
        auto render_rect = window_data->window->frame().render_rect();
        auto frame_rect = window_data->window->frame().rect();
        auto rect = window_data->window->rect();
        if (render_rect != window_data->last_sent_render_rect || frame_rect != window_data->last_sent_frame_rect || rect != window_data->last_sent_rect) {
            compositor_window.geometry = RemoteDesktop::Compositor::WindowGeometry {
                .render_rect = render_rect,
                .frame_rect = frame_rect,
                .rect = rect
            };
            window_data->last_sent_frame_rect = frame_rect;
            window_data->last_sent_rect = rect;
            anything_dirty = true;
        }
        if (window_data->opaque_rects_dirty) {
            compositor_window.opaque_rects = window_data->opaque_rects.rects();
            window_data->opaque_rects_dirty = false;
            anything_dirty = true;
        }
        if (window_data->transparent_rects_dirty) {
            compositor_window.transparent_rects = window_data->transparent_rects.rects();
            window_data->transparent_rects_dirty = false;
            anything_dirty = true;
        }

        if (anything_dirty) {
            dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Window {} occlusions did change", window_data->id);
            m_pending_occlusions.append(move(compositor_window));
        } else {
            dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Window {} occlusions unchanged", window_data->id);
        }
    }
}

void RemoteCompositorClientConnection::update_window_dirty_rects(Window& window)
{
    VERIFY(!m_need_greet);
    auto& window_data = get_window_data(window);

    auto& dirty_rects = window.dirty_rects();
    if (dirty_rects.is_empty()) {
        auto backing_store_bitmap_id = window_data.window->backing_store_remote_bitmap_id();
        auto backing_store_bitmap_sync_tag = window_data.window->backing_store_remote_bitmap_sync_tag();
        if (window_data.last_sent_backing_store_bitmap_id != backing_store_bitmap_id || window_data.last_sent_backing_store_bitmap_sync_tag != backing_store_bitmap_sync_tag) {
            window_data.dirty_rects_dirty = true;
            m_dirty_rects_dirty = true;
        } else {
            dbgln("update_window_dirty_rects for {} -> no dirty rects", window.title());
            return;
        }
    }
    // We track dirty rects relative to the window, this way we can detect whether dirty
    // rects have changed even if the window was moved
    auto relative_offset = -window.frame().render_rect().location();
    auto dirty_rects_before = window_data.dirty_rects.clone();
    for (auto& r : dirty_rects_before.rects())
        dbgln("    dirty_rects_before: {}", r);
    window_data.dirty_rects.add_many_translated(dirty_rects.rects(), relative_offset);
    if (!window_data.dirty_rects.shatter(dirty_rects_before).is_empty()) {
        window_data.dirty_rects_dirty = true;
        dbgln("update_window_dirty_rects for {} (render rect: {}) -> changed (window: {})", window.title(), window.frame().render_rect(), window_data.id);
        for (auto& r : window_data.dirty_rects.rects())
            dbgln("    {}", r);
        for (auto& r : dirty_rects.rects())
            dbgln("    original dirty_rects: {}", r);
    } else {
        dbgln("update_window_dirty_rects for {} -> unchanged", window.title());
    }
    m_dirty_rects_dirty = true;
}

void RemoteCompositorClientConnection::flush_dirty()
{
    VERIFY(!m_need_greet);
    if (m_dirty_rects_dirty)
        update_dirty_rects();

    bool did_send_anything = false;
    if (!m_pending_occlusions.is_empty() || !m_pending_delete_windows.is_empty() || !m_pending_dirty_rects.is_empty()) {
        async_update_display(m_window_order_dirty ? m_ordered_window_ids : decltype(m_ordered_window_ids) {}, m_pending_occlusions, m_pending_delete_windows, m_pending_dirty_rects);
        m_window_order_dirty = false;
        did_send_anything = true;
    }

    auto cursor_location = ScreenInput::the().cursor_location();
    if (m_last_sent_cursor_location != cursor_location) {
        async_cursor_position_changed(cursor_location);
        m_last_sent_cursor_location = cursor_location;
    }

    if (did_send_anything) {
        m_is_ready = false;
        if (auto result = flush_send_buffer(); result.is_error())
            dbgln("RemoteCompositorClientConnection::flush_dirty failed to flush send buffer: {}", result.error());
    }
}

void RemoteCompositorClientConnection::update_dirty_rects()
{
    VERIFY(m_dirty_rects_dirty);
    m_dirty_rects_dirty = false;

    for (auto* window_data : m_ordered_window_data) {
        if (!window_data->dirty_rects_dirty)
            continue;

        dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Window {} has {} dirty rects", window_data->id, window_data->dirty_rects.size());
        for (auto& rect : window_data->dirty_rects.rects()) {
            dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "    {}", rect);
        }

        bool is_windowserver_backing_bitmap = false;
        window_data->last_sent_backing_store_bitmap_id = window_data->window->backing_store_remote_bitmap_id();
        window_data->last_sent_backing_store_bitmap_sync_tag = window_data->window->backing_store_remote_bitmap_sync_tag();
        if (!window_data->last_sent_backing_store_bitmap_id) {
            // If this is a windowserver-managed backing store, we will need to get that bitmap's remote bitmap id instead
            if (auto* backing_store = window_data->window->backing_store())
                window_data->last_sent_backing_store_bitmap_id = backing_store->remote_bitmap_id();
            else
                window_data->last_sent_backing_store_bitmap_id = 0;
            window_data->last_sent_backing_store_bitmap_sync_tag = 0; // not used for windowserver backed bitmaps
            is_windowserver_backing_bitmap = true;
        }
        m_pending_dirty_rects.append(RemoteDesktop::Compositor::WindowDirtyRects {
            .id = window_data->id,
            .backing_bitmap_id = window_data->last_sent_backing_store_bitmap_id,
            .backing_bitmap_sync_tag = window_data->last_sent_backing_store_bitmap_sync_tag,
            .is_windowserver_backing_bitmap = is_windowserver_backing_bitmap,
            .frame_top_bottom_bitmap_id = window_data->window->frame().remote_top_bottom_bitmap_id(),
            .frame_left_right_bitmap_id = window_data->window->frame().remote_left_right_bitmap_id(),
            .dirty_rects = window_data->dirty_rects.rects() });

        window_data->dirty_rects_dirty = false;

        // Unlike Window::dirty_rects, the WindowData::dirty_rects are relative to the render rect of the window
        auto window_rect_offset = window_data->window->rect().location() - window_data->window->frame().render_rect().location();
        dbgln("Dirty rects in window {} rect offset: {} bitmap: {} sync tag: {}", window_data->window->title(), window_rect_offset, window_data->last_sent_backing_store_bitmap_id, window_data->last_sent_backing_store_bitmap_sync_tag);
        for (auto& r : window_data->dirty_rects.rects())
            dbgln("    {}", r);

        if (auto dirty_rects_in_window = window_data->dirty_rects.intersected(Gfx::IntRect { window_rect_offset, window_data->window->rect().size() }); !dirty_rects_in_window.is_empty()) {
            dirty_rects_in_window.translate_by(-window_rect_offset);
            dbgln("Intersected dirty rects in window {} rect offset: {}", window_data->window->title(), window_rect_offset);
            for (auto& r : window_data->dirty_rects.rects())
                dbgln("    dirty_rect: {}", r);
            for (auto& r : dirty_rects_in_window.rects())
                dbgln("    dirty_rects_in_window: {}", r);
        }

        window_data->dirty_rects.clear_with_capacity();
    }
}

auto RemoteCompositorClientConnection::get_window_data(Window& window) -> WindowData&
{
    auto it = m_window_data.find(&window);
    VERIFY(it != m_window_data.end());
    return *it->value;
}

auto RemoteCompositorClientConnection::find_window_data(Window& window) -> WindowData*
{
    auto it = m_window_data.find(&window);
    return it != m_window_data.end() ? it->value.ptr() : nullptr;
}

Messages::RemoteCompositorServer::StartSessionResponse RemoteCompositorClientConnection::start_session()
{
    if (!Compositor::the().set_remote_client(this)) {
        dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "RemoteCompositorClientConnection {:p} can't start session, already connected", this);
        return { true, "Remote client already connected"sv };
    }
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "RemoteCompositorClientConnection {:p} starting session", this);
    m_occlusions_dirty = true;
    // Don't start pushing messages until the client is ready
    m_is_ready = false;
    m_need_greet = true;

    m_is_active = true;

    deferred_invoke([this, protected_this = NonnullRefPtr<RemoteCompositorClientConnection>(*this)]() {
        enable_send_buffer(1500);
    });
    return { false, String::empty() };
}

void RemoteCompositorClientConnection::ready_for_more()
{
    m_is_ready = true;
    Compositor::the().remote_client_is_ready(*this);
    if (m_need_greet) {
        m_need_greet = false;
        async_fast_greet(Screen::rects(), Compositor::the().background_color(), ScreenInput::the().cursor_location());
    }
}

void RemoteCompositorClientConnection::set_cursor_position(Gfx::IntPoint const& position)
{
    auto& screen_input = ScreenInput::the();
    if (position != screen_input.cursor_location()) {
        screen_input.set_cursor_location(position);
        Compositor::the().invalidate_cursor();
    }
}

void RemoteCompositorClientConnection::set_mouse_buttons(Gfx::IntPoint const& position, u32 buttons)
{
    set_cursor_position(position);
    ScreenInput::the().set_mouse_button_state((MouseButton)buttons);
}

void RemoteCompositorClientConnection::mouse_wheel_turned(Gfx::IntPoint const& position, i32 delta)
{
    set_cursor_position(position);
    ScreenInput::the().mouse_wheel_turned(delta);
}

void RemoteCompositorClientConnection::die()
{
    deferred_invoke([this]() {
        s_connections.remove(client_id());
    });
}

}
