/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGfx/Painter.h>
#include <LibRemoteDesktop/RemoteDesktopRenderer.h>
#include <LibRemoteDesktop/RemoteDesktopServerConnection.h>

namespace RemoteDesktop {

Renderer::Renderer(RendererCallbacks& callbacks, RemoteDesktopServerConnection& connection)
    : RemoteCompositorServerProxy<RemoteCompositorClientEndpoint, RemoteCompositorServerEndpoint, Renderer>(*this, {})
    , m_callbacks(callbacks)
    , m_connection(connection)
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Renderer {:p}", this);

    m_font_database.populate_own_fonts();

    connection.register_compositor(*this);
    connection.on_new_gfx_client = [this](int client_id) {
        auto gfx_renderer = adopt_own(*new RemoteGfx::RemoteGfxRenderer(*this, m_font_database, client_id));
        m_connection.register_gfx(client_id, *gfx_renderer);
        auto result = m_remote_gfx_clients.set(client_id, move(gfx_renderer));
        VERIFY(result == AK::HashSetResult::InsertedNewEntry);
        return true;
    };
    connection.on_delete_gfx_client = [this](int client_id) {
        m_connection.unregister_gfx(client_id);
        bool removed = m_remote_gfx_clients.remove(client_id);
        VERIFY(removed);
    };
    connection.on_associate_clients = [this](int windows_client_id, int gfx_client_id) {
        clients_were_associated(windows_client_id, gfx_client_id);
    };
}

Renderer::~Renderer()
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "~Renderer {:p}", this);
    m_connection.unregister_compositor(*this);
    m_connection.on_new_gfx_client = nullptr;
    m_connection.on_delete_gfx_client = nullptr;
    m_connection.on_associate_clients = nullptr;
}

void Renderer::paint(Gfx::Painter& painter, Gfx::IntRect const& paint_rect)
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Renderer::paint rect: {} windows: {}", paint_rect, m_ordered_window_data.size());
    if (!m_surface) {
        painter.clear_rect(paint_rect, Gfx::Color::Black);
        return;
    }
    if (!m_dirty_rects.is_empty())
        render_desktop();
    painter.blit(paint_rect.location(), *m_surface, paint_rect, 1.0f, false);
}

void Renderer::render_desktop()
{
    Gfx::Painter painter(*m_surface);
    for (auto& rect : m_dirty_rects.rects())
        painter.clear_rect(rect, m_wallpaper_color);
    for (auto* window_data : m_ordered_window_data) {
        dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Render window {} render: {} rect: {}", window_data->id, window_data->geometry.render_rect, window_data->geometry.rect);
        if (window_data->frame_left_right_bitmap && window_data->frame_top_bottom_bitmap) {
            //dbgln("Render frame for window {}", window_data->id);
            auto frame_rects = window_data->geometry.render_rect.shatter(window_data->geometry.rect);
            auto frame_top_height = window_data->geometry.rect.top() - window_data->geometry.render_rect.top();
            auto frame_left_width = window_data->geometry.rect.left() - window_data->geometry.render_rect.left();
            auto render_frame_rect = [&]<bool is_transparent>(Gfx::IntRect const& absolute_render_rect) {
                m_dirty_rects.for_each_intersected(absolute_render_rect, [&](auto& render_rect) {
                    Gfx::PainterStateSaver save(painter);
                    painter.add_clip_rect(render_rect);
                    painter.blit(window_data->geometry.render_rect.location(), *window_data->frame_top_bottom_bitmap, { 0, 0, window_data->frame_top_bottom_bitmap->width(), frame_top_height }, 1.0f, is_transparent);
                    painter.blit({ window_data->geometry.render_rect.left(), window_data->geometry.rect.bottom() + 1 }, *window_data->frame_top_bottom_bitmap, { 0, frame_top_height, window_data->frame_top_bottom_bitmap->width(), window_data->frame_top_bottom_bitmap->height() - frame_top_height }, 1.0f, is_transparent);
                    painter.blit({ window_data->geometry.render_rect.left(), window_data->geometry.render_rect.top() + frame_top_height }, *window_data->frame_left_right_bitmap, { 0, 0, frame_left_width, window_data->geometry.rect.height() }, 1.0f, is_transparent);
                    painter.blit({ window_data->geometry.rect.right() + 1, window_data->geometry.render_rect.top() + frame_top_height }, *window_data->frame_left_right_bitmap, { frame_left_width, 0, window_data->frame_left_right_bitmap->width() + frame_left_width, window_data->geometry.rect.height() }, 1.0f, is_transparent);
                    return IterationDecision::Continue;
                });
            };
            for (auto& relative_rect : window_data->opaque_rects.rects()) {
                auto absolute_rect = relative_rect.translated(window_data->geometry.render_rect.location());
                for (auto frame_rect : frame_rects) {
                    auto absolute_render_rect = frame_rect.intersected(absolute_rect);
                    if (!absolute_render_rect.is_empty())
                        render_frame_rect.template operator()<false>(absolute_render_rect);
                }
            }
            for (auto& relative_rect : window_data->transparent_rects.rects()) {
                auto absolute_rect = relative_rect.translated(window_data->geometry.render_rect.location());
                for (auto frame_rect : frame_rects) {
                    auto absolute_render_rect = frame_rect.intersected(absolute_rect);
                    if (!absolute_render_rect.is_empty())
                        render_frame_rect.template operator()<true>(absolute_render_rect);
                }
            }
        }
        m_dirty_rects.for_each_intersected(window_data->geometry.render_rect, [&](auto& render_rect) {
            auto render_window_backing_store = [&](Gfx::IntRect const& absolute_rect) {
                Gfx::PainterStateSaver save(painter);
                painter.add_clip_rect(absolute_rect);
                if (!window_data->backing_bitmap)
                    window_data->backing_bitmap = window_data->backing_store_gfx_renderer ? window_data->backing_store_gfx_renderer->find_bitmap(window_data->backing_bitmap_id) : nullptr;
                if (window_data->backing_bitmap) {
                    dbgln_if(1, "Render window {} backing bitmap {} client: {} window rect: {} at {}", window_data->id, window_data->backing_bitmap_id, window_data->client_id, window_data->geometry.rect, absolute_rect);
                    painter.blit(window_data->geometry.rect.location(), *window_data->backing_bitmap, { {}, window_data->geometry.rect.size() }, 1.0f, window_data->backing_bitmap->has_alpha_channel());
                } else {
                    dbgln_if(1, "Render window {} at {}, have no backing bitmap (id: {} client: {}), clear at {}", window_data->id, window_data->geometry.rect, window_data->backing_bitmap_id, window_data->client_id, absolute_rect);
                    painter.clear_rect(window_data->geometry.rect, Color::Black);
                }
            };

            auto relative_frame_rects = Gfx::IntRect { {}, window_data->geometry.render_rect.size() }.shatter(window_data->geometry.rect.translated(-window_data->geometry.render_rect.location()));
            auto render_window_rects = [&](Gfx::DisjointRectSet const& relative_rects) {
                if (!window_data->backing_store_gfx_renderer) {
                    window_data->backing_store_gfx_renderer = window_data->is_windowserver_backing_bitmap ? windowserver_gfx_client() : m_connection.find_gfx_renderer(m_connection.window_to_gfx_client(window_data->client_id));
                    if (!window_data->backing_store_gfx_renderer) {
                        dbgln("!!!!!!!!!!! No gfx renderer for window {} client id {}", window_data->id, window_data->client_id);
                        return;
                    }
                }
                for (auto& relative_rect : relative_rects.rects()) {
                    auto absolute_rect = relative_rect.translated(window_data->geometry.render_rect.location()).intersected(render_rect);
                    auto intersected_backing_store_rect = absolute_rect.intersected(window_data->geometry.rect);
                    if (!intersected_backing_store_rect.is_empty())
                        render_window_backing_store(intersected_backing_store_rect);
                }
            };

            render_window_rects(window_data->opaque_rects);
            render_window_rects(window_data->transparent_rects);

            if (window_data->geometry.render_rect != window_data->geometry.rect) {
                auto top_height = window_data->geometry.rect.top() - window_data->geometry.frame_rect.top();
                auto bottom_height = window_data->geometry.frame_rect.bottom() - window_data->geometry.rect.bottom();
                auto left_width = window_data->geometry.rect.left() - window_data->geometry.frame_rect.left();
                auto right_width = window_data->geometry.frame_rect.right() - window_data->geometry.rect.right();
                Gfx::IntRect top_rect { 0, 0, window_data->geometry.render_rect.width(), top_height };
                Gfx::IntRect bottom_rect { 0, window_data->geometry.render_rect.height() - bottom_height, window_data->geometry.render_rect.width(), top_height };
                Gfx::IntRect left_rect { 0, top_rect.height(), left_width, window_data->geometry.frame_rect.height() - top_height - bottom_height };
                Gfx::IntRect right_rect { window_data->geometry.render_rect.width() - right_width, top_rect.height(), left_width, left_rect.height() };
                auto render_frame_top = [&](Gfx::IntRect const&) {

                };
                auto render_window_frame = [&](Gfx::IntRect const& relative_frame_rect) {
                    if (auto intersected_top = top_rect.intersected(relative_frame_rect); !intersected_top.is_empty())
                        render_frame_top(intersected_top);
                    if (auto intersected_bottom = bottom_rect.intersected(relative_frame_rect); !intersected_bottom.is_empty())
                        render_frame_top(intersected_bottom);
                };

                for (auto& relative_rect : window_data->opaque_rects.rects())
                    render_window_frame(relative_rect);
                for (auto& relative_rect : window_data->transparent_rects.rects())
                    render_window_frame(relative_rect);
            }
            return AK::IterationDecision::Continue;
        });
    }
    m_outside_rects.for_each_intersected(m_dirty_rects, [&](auto& rect) {
        painter.clear_rect(rect, Gfx::Color::Black);
        return IterationDecision::Continue;
    });

    m_dirty_rects.clear_with_capacity();
}

void Renderer::fast_greet(Vector<Gfx::IntRect> const& screen_rects, Gfx::Color const& wallpaper_color, Gfx::IntPoint const& cursor_position)
{
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Renderer::fast_greet");
    m_screen_rects.clear_with_capacity();
    m_screen_rects.add_many(screen_rects);
    for (size_t i = 0; i < screen_rects.size(); i++) {
        if (i == 0)
            m_bounds = screen_rects[i];
        else
            m_bounds = m_bounds.united(screen_rects[i]);
    }
    m_outside_rects = Gfx::DisjointRectSet(m_bounds).shatter(m_screen_rects);

    if (!m_surface || m_surface->size() != m_bounds.size()) {
        m_surface = nullptr;
        m_surface = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, m_bounds.size(), 1).release_value();
    }

    VERIFY(m_bounds.location().is_null());
    m_dirty_rects = m_bounds;

    m_wallpaper_color = wallpaper_color;

    m_window_data.clear();
    m_ordered_window_data.clear();

    m_callbacks.set_surface_size(m_bounds.size());

    m_cursor_position = cursor_position;
}

auto Renderer::window_data(Compositor::WindowId id) -> WindowData&
{
    auto it = m_window_data.find(id);
    VERIFY(it != m_window_data.end());
    return *it->value;
}

RemoteGfx::RemoteGfxRenderer* Renderer::windowserver_gfx_client()
{
    if (m_windowserver_gfx_client)
        return m_windowserver_gfx_client;
    m_windowserver_gfx_client = m_connection.find_gfx_renderer(m_connection.window_to_gfx_client(-1));
    return m_windowserver_gfx_client;
}

void Renderer::invalidate_window(WindowData& window_data, Gfx::DisjointRectSet const& relative_rects)
{
    auto relative_offset = window_data.geometry.render_rect.location();
    window_data.opaque_rects.for_each_intersected(relative_rects, [&](auto& relative_invalidate_rect) {
        m_dirty_rects.add(relative_invalidate_rect.translated(relative_offset));
        return AK::IterationDecision::Continue;
    });
    window_data.transparent_rects.for_each_intersected(relative_rects, [&](auto& relative_invalidate_rect) {
        m_dirty_rects.add(relative_invalidate_rect.translated(relative_offset));
        return AK::IterationDecision::Continue;
    });
}

void Renderer::invalidate_window(WindowData& window_data, bool frame, bool window_content)
{
    auto relative_offset = window_data.geometry.render_rect.location();
    if (frame && window_content) {
        for (auto& relative_invalidate_rect : window_data.opaque_rects.rects())
            m_dirty_rects.add(relative_invalidate_rect.translated(relative_offset));
        for (auto& relative_invalidate_rect : window_data.transparent_rects.rects())
            m_dirty_rects.add(relative_invalidate_rect.translated(relative_offset));
    } else if (frame) {
        for (auto& rect : window_data.geometry.render_rect.shatter(window_data.geometry.rect)) {
            auto relative_rect = rect.translated(-relative_offset);
            window_data.opaque_rects.for_each_intersected(relative_rect, [&](auto& relative_invalidate_rect) {
                m_dirty_rects.add(relative_invalidate_rect.translated(relative_offset));
                return IterationDecision::Continue;
            });
            window_data.transparent_rects.for_each_intersected(relative_rect, [&](auto& relative_invalidate_rect) {
                m_dirty_rects.add(relative_invalidate_rect.translated(relative_offset));
                return IterationDecision::Continue;
            });
        }
    } else if (window_content) {
        auto relative_rect = window_data.geometry.rect.translated(-relative_offset);
        window_data.opaque_rects.for_each_intersected(relative_rect, [&](auto& relative_invalidate_rect) {
            m_dirty_rects.add(relative_invalidate_rect.translated(relative_offset));
            return IterationDecision::Continue;
        });
        window_data.transparent_rects.for_each_intersected(relative_rect, [&](auto& relative_invalidate_rect) {
            m_dirty_rects.add(relative_invalidate_rect.translated(relative_offset));
            return IterationDecision::Continue;
        });
    }
}

void Renderer::WindowData::update(Renderer& renderer, RemoteDesktop::Compositor::Window const& window)
{
    if (window.geometry.has_value()) {
        // Invalidate everything before updating the geometry
        renderer.invalidate_window(*this, true, true);
        geometry = window.geometry.value();
    }

    if (window.opaque_rects.has_value())
        opaque_rects = window.opaque_rects.value();
    if (window.transparent_rects.has_value())
        transparent_rects = window.transparent_rects.value();
    renderer.invalidate_window(*this, true, true);
}

void Renderer::update_display(Vector<Compositor::WindowId> const& window_order, Vector<Compositor::Window> const& windows, Vector<Compositor::WindowId> const& delete_windows, Vector<Compositor::WindowDirtyRects> const& window_dirty_rects)
{
    VERIFY(!m_screen_rects.is_empty());
    dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "Renderer::update_display: windows: {} delete windows: {} dirty windows: {}", windows.size(), delete_windows.size(), window_dirty_rects.size());
    ScopeGuard guard([&]() {
        if (!m_dirty_rects.is_empty())
            m_callbacks.invalidate_rects(m_dirty_rects);
    });
    for (auto& deleted_id : delete_windows) {
        auto it = m_window_data.find(deleted_id);
        VERIFY(it != m_window_data.end());
        auto& window_data = *it->value;
        invalidate_window(window_data, true, true);
        m_window_data.remove(it);
    }
    for (auto& window : windows) {
        VERIFY(!delete_windows.contains_slow(window.id));
        auto it = m_window_data.find(window.id);
        if (it != m_window_data.end()) {
            auto& window_data = *it->value;
            VERIFY(window_data.id == window.id);
            window_data.update(*this, window);
            // We should have received a new window order if we received new windows
            VERIFY(!window_order.is_empty());
        } else {
            VERIFY(window.geometry.has_value());
            auto window_data = adopt_own(*new WindowData(*this, window));
            invalidate_window(*window_data, true, true);
            auto result = m_window_data.set(window.id, move(window_data));
            VERIFY(result == AK::HashSetResult::InsertedNewEntry);
        }
    }

    if (!window_order.is_empty()) {
        m_ordered_window_data.clear_with_capacity();
        for (auto& window_id : window_order)
            m_ordered_window_data.append(&window_data(window_id));
    }

    for (auto& dirty_rects : window_dirty_rects) {
        auto& window_data = this->window_data(dirty_rects.id);
        dbgln_if(REMOTE_COMPOSITOR_SERVER_DEBUG, "    window {} render rect: {} has {} rects, backing bitmap: {}", dirty_rects.id, window_data.geometry.render_rect, dirty_rects.dirty_rects.size(), dirty_rects.backing_bitmap_id);
        bool invalidate_frame = false;
        bool invalidate_content = false;
        auto backing_bitmap_updated = [&]() {
            // It's possible that we don't have the backing bitmap yet! We'll look it up when we need it,
            // and if it's still not available we'll just clear the area until we do get it
            if (window_data.backing_bitmap_id > 0) {
                auto* backing_bitmap = window_data.backing_store_gfx_renderer ? window_data.backing_store_gfx_renderer->find_bitmap(window_data.backing_bitmap_id) : nullptr;
                if (backing_bitmap) {
                    window_data.backing_dirty_rects = dirty_rects.dirty_rects;
                    auto* previous_backing_bitmap = window_data.backing_bitmap.ptr();
                    window_data.backing_bitmap = backing_bitmap;
                    if (previous_backing_bitmap)
                        invalidate_window(window_data, window_data.backing_dirty_rects);
                    else
                        invalidate_content = true;
                } else if (!window_data.backing_bitmap && window_data.last_backing_bitmap_id > 0) {
                    backing_bitmap = window_data.backing_store_gfx_renderer ? window_data.backing_store_gfx_renderer->find_bitmap(window_data.last_backing_bitmap_id) : nullptr;
                    if (backing_bitmap) {
                        auto* previous_backing_bitmap = window_data.backing_bitmap.ptr();
                        window_data.backing_bitmap = backing_bitmap;
                        if (previous_backing_bitmap)
                            invalidate_window(window_data, window_data.last_backing_dirty_rects);
                        else
                            invalidate_content = true;
                    }
                }
            } else {
                if (window_data.backing_bitmap) {
                    window_data.backing_bitmap = nullptr;
                    invalidate_content = true;
                }
            }
        };
        if (window_data.backing_bitmap_id != dirty_rects.backing_bitmap_id || window_data.is_windowserver_backing_bitmap != dirty_rects.is_windowserver_backing_bitmap) {
            window_data.last_backing_bitmap_id = window_data.backing_bitmap_id;
            window_data.last_backing_bitmap_sync_tag = window_data.backing_bitmap_sync_tag;
            window_data.backing_bitmap_id = dirty_rects.backing_bitmap_id;
            window_data.backing_bitmap_sync_tag = dirty_rects.backing_bitmap_sync_tag;
            window_data.last_backing_dirty_rects = move(window_data.backing_dirty_rects);
            window_data.is_windowserver_backing_bitmap = dirty_rects.is_windowserver_backing_bitmap;
            window_data.backing_store_gfx_renderer = window_data.is_windowserver_backing_bitmap ? windowserver_gfx_client() : m_connection.find_gfx_renderer(m_connection.window_to_gfx_client(window_data.client_id));
            backing_bitmap_updated();
        } else if (window_data.backing_bitmap_sync_tag != dirty_rects.backing_bitmap_sync_tag) {
            dbgln("Window {} backing sync tag changed from {} to {}", window_data.id, window_data.backing_bitmap_sync_tag, dirty_rects.backing_bitmap_sync_tag);
            window_data.backing_bitmap_sync_tag = dirty_rects.backing_bitmap_sync_tag;
            if (!window_data.backing_store_gfx_renderer)
                window_data.backing_store_gfx_renderer = window_data.is_windowserver_backing_bitmap ? windowserver_gfx_client() : m_connection.find_gfx_renderer(m_connection.window_to_gfx_client(window_data.client_id));
            backing_bitmap_updated();
        }
        if (window_data.frame_top_bottom_bitmap_id != dirty_rects.frame_top_bottom_bitmap_id || window_data.frame_left_right_bitmap_id != dirty_rects.frame_left_right_bitmap_id) {
            window_data.frame_top_bottom_bitmap_id = dirty_rects.frame_top_bottom_bitmap_id;
            window_data.frame_left_right_bitmap_id = dirty_rects.frame_left_right_bitmap_id;
            if (auto* windowserver_gfx = windowserver_gfx_client()) {
                if (auto* top_bottom_bitmap = windowserver_gfx->find_bitmap(dirty_rects.frame_top_bottom_bitmap_id))
                    window_data.frame_top_bottom_bitmap = *top_bottom_bitmap;
                if (auto* left_right_bitmap = windowserver_gfx->find_bitmap(dirty_rects.frame_left_right_bitmap_id))
                    window_data.frame_left_right_bitmap = *left_right_bitmap;
                invalidate_frame = true;
            }
        }
        if (invalidate_frame || invalidate_content)
            invalidate_window(window_data, invalidate_frame, invalidate_content);
    }
}
void Renderer::bitmap_was_synced(i32 gfx_client_id, i32 bitmap_id, Gfx::Bitmap& synced_bitmap, Gfx::DisjointRectSet const& update_rects)
{
    dbgln("bitmap_was_synced gfx client {} bitmap_id {} bitmap size {}", gfx_client_id, bitmap_id, synced_bitmap.size());
    ScopeGuard guard([&]() {
        if (!m_dirty_rects.is_empty())
            m_callbacks.invalidate_rects(m_dirty_rects);
    });

    auto window_client_id = m_connection.gfx_to_window_client(gfx_client_id);
    if (window_client_id == -1) {
        // Windowserver bitmaps
        for (auto& it : m_window_data) {
            auto& window_data = *it.value;
            if (!window_data.backing_store_gfx_renderer)
                window_data.backing_store_gfx_renderer = window_data.is_windowserver_backing_bitmap ? windowserver_gfx_client() : m_connection.find_gfx_renderer(m_connection.window_to_gfx_client(window_data.client_id));
            if (bitmap_id == window_data.frame_top_bottom_bitmap_id || bitmap_id == window_data.frame_left_right_bitmap_id) {
                if (bitmap_id == window_data.frame_top_bottom_bitmap_id)
                    window_data.frame_top_bottom_bitmap = synced_bitmap;
                else
                    window_data.frame_left_right_bitmap = synced_bitmap;
                invalidate_window(window_data, true, false);
                return;
            }
        }
        // It's possible that windows like menus use a windowserver bitmap as backing store, so keep searching
    }
    for (auto& it : m_window_data) {
        auto& window_data = *it.value;
        if (window_data.client_id != window_client_id)
            continue;
        if (!window_data.backing_store_gfx_renderer)
            window_data.backing_store_gfx_renderer = window_data.is_windowserver_backing_bitmap ? windowserver_gfx_client() : m_connection.find_gfx_renderer(m_connection.window_to_gfx_client(window_data.client_id));
        if (bitmap_id == window_data.backing_bitmap_id || bitmap_id == window_data.last_backing_bitmap_id) {
            //dbgln("bitmap_updated backing bitmap was changed for window {}", window_data.id);
            window_data.backing_bitmap = synced_bitmap;
            auto render_offset = window_data.geometry.render_rect.location();
            auto invalidate_update_rect = [&](Gfx::IntRect const& update_rect) {
                auto rect_relative_to_frame = update_rect.translated(window_data.geometry.rect.location() - render_offset);
                window_data.opaque_rects.for_each_intersected(rect_relative_to_frame, [&](auto& rect) {
                    auto invalidate_rect = rect.translated(render_offset);
                    m_dirty_rects.add(invalidate_rect);
                    return IterationDecision::Continue;
                });
                window_data.transparent_rects.for_each_intersected(rect_relative_to_frame, [&](auto& rect) {
                    auto invalidate_rect = rect.translated(render_offset);
                    m_dirty_rects.add(invalidate_rect);
                    return IterationDecision::Continue;
                });
            };
            if (!update_rects.is_empty()) {
                for (auto& update_rect : update_rects.rects())
                    invalidate_update_rect(update_rect);
            } else {
                invalidate_update_rect({ {}, window_data.geometry.rect.size() });
            }
            break;
        }
    }
}

void Renderer::clients_were_associated(i32 window_client_id, i32)
{
    for (auto& it : m_window_data) {
        auto& window_data = *it.value;
        bool invalidate_frame = false;
        bool invalidate_content = false;
        if (window_client_id == -1) {
            if (window_data.is_windowserver_backing_bitmap)
                invalidate_content = true;
            if (window_data.frame_top_bottom_bitmap_id > 0 || window_data.frame_left_right_bitmap_id > 0)
                invalidate_frame = true;
        } else if (window_client_id == window_data.client_id) {
            invalidate_content = true;
        }
        if (invalidate_frame || invalidate_content) {
            dbgln("Window client {} was associated, invalidate window {} frame: {} content: {}", window_client_id, window_data.id, invalidate_frame, invalidate_content);
            invalidate_window(window_data, invalidate_frame, invalidate_content);
        }
    }
}

void Renderer::bitmap_updated(i32, i32, Gfx::IntRect const*)
{
}

void Renderer::associate_window_client(int windowserver_client_id, u64 cookie)
{
    m_connection.handle_associate_window_client(windowserver_client_id, cookie);
}

void Renderer::disassociate_window_client(int windowserver_client_id)
{
    m_connection.handle_disassociate_window_client(windowserver_client_id);
}

void Renderer::cursor_position_changed(Gfx::IntPoint const& cursor_position)
{
    dbgln("Cursor position updated: {}", cursor_position);
    m_cursor_position = cursor_position;
    if (m_pending_set_cursor_position.has_value()) {
        if (m_pending_set_cursor_position.value() != m_cursor_position)
            send_new_cusor_position();
        else
            m_pending_set_cursor_position = {};
    }
}

void Renderer::send_new_cusor_position()
{
    if (!m_connection.is_connected())
        return;
    dbgln("Send new cursor position: {}", m_pending_set_cursor_position.value());
    m_connection.compositor_server().async_set_cursor_position(m_pending_set_cursor_position.release_value());
}

void Renderer::set_cursor_position(Gfx::IntPoint const& cursor_position)
{
    bool new_position_was_already_pending = m_pending_set_cursor_position.has_value();
    m_pending_set_cursor_position = cursor_position;
    if (!new_position_was_already_pending)
        send_new_cusor_position();
}

void Renderer::set_mouse_buttons(Gfx::IntPoint const& position, unsigned buttons)
{
    m_pending_set_cursor_position = {}; // Cancel pending new mouse position

    if (!m_connection.is_connected())
        return;
    m_connection.compositor_server().async_set_mouse_buttons(position, buttons);
}

void Renderer::mouse_wheel_turned(Gfx::IntPoint const& position, int delta)
{
    m_pending_set_cursor_position = {}; // Cancel pending new mouse position

    if (!m_connection.is_connected())
        return;
    // TODO: delta is already multiplied, but we need to send the raw delta!
    m_connection.compositor_server().async_mouse_wheel_turned(position, delta);
}

}
