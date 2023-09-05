/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MapWidget.h"
#include <AK/URL.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibProtocol/Request.h>

// Math helpers
// https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Pseudo-code
static double longitude_to_tile_x(double longitude, int zoom)
{
    return pow(2, zoom) * ((longitude + 180.0) / 360.0);
}

static double latitude_to_tile_y(double latitude, int zoom)
{
    return pow(2, zoom) * (1.0 - (log(tan(AK::to_radians(latitude)) + (1.0 / cos(AK::to_radians(latitude)))) / M_PI)) / 2.0;
}

static double tile_x_to_longitude(double x, int zoom)
{
    return x / pow(2, zoom) * 360.0 - 180.0;
}

static double tile_y_to_latitude(double y, int zoom)
{
    return AK::to_degrees(atan(sinh(M_PI * (1.0 - 2.0 * y / pow(2, zoom)))));
}

static double nice_round_number(double number)
{
    double pow10 = pow(10, floor(log10(floor(number))));
    double d = number / pow10;
    return pow10 * (d >= 10 ? 10 : (d >= 5 ? 5 : (d >= 3 ? 3 : (d >= 2 ? 2 : 1))));
}

double MapWidget::LatLng::distance_to(LatLng const& other) const
{
    double const earth_radius = 6371000.0;
    return earth_radius * 2.0 * asin(sqrt(pow(sin((AK::to_radians(other.latitude) - AK::to_radians(latitude)) / 2.0), 2.0) + cos(AK::to_radians(latitude)) * cos(AK::to_radians(other.latitude)) * pow(sin((AK::to_radians(other.longitude) - AK::to_radians(longitude)) / 2.0), 2.0)));
}

// MapWidget class
MapWidget::MapWidget(Options const& options)
    : m_tile_layer_url(options.tile_layer_url)
    , m_center(options.center)
    , m_zoom(options.zoom)
    , m_scale_enabled(options.scale_enabled)
    , m_scale_max_width(options.scale_max_width)
    , m_attribution_enabled(options.attribution_enabled)
{
    m_request_client = Protocol::RequestClient::try_create().release_value_but_fixme_should_propagate_errors();
    if (options.attribution_enabled)
        add_panel({ options.attribution_text, Panel::Position::BottomRight, options.attribution_url, true });
}

void MapWidget::set_zoom(int zoom)
{
    m_zoom = min(max(zoom, ZOOM_MIN), ZOOM_MAX);
    clear_tile_queue();
    update();
}

void MapWidget::doubleclick_event(GUI::MouseEvent& event)
{
    int new_zoom = event.shift() ? m_zoom - 1 : m_zoom + 1;
    set_zoom_for_mouse_event(new_zoom, event);
}

void MapWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (m_connection_failed)
        return;

    if (event.button() == GUI::MouseButton::Primary) {
        // Ignore panels click
        for (auto& panel : m_panels)
            if (panel.rect.contains(event.x(), event.y()))
                return;

        // Start map tiles dragging
        m_dragging = true;
        m_last_mouse_x = event.x();
        m_last_mouse_y = event.y();
        set_override_cursor(Gfx::StandardCursor::Drag);
    }
}

void MapWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (m_connection_failed)
        return;

    if (m_dragging) {
        // Adjust map center by mouse delta
        double delta_x = event.x() - m_last_mouse_x;
        double delta_y = event.y() - m_last_mouse_y;
        set_center({ tile_y_to_latitude(latitude_to_tile_y(m_center.latitude, m_zoom) - delta_y / TILE_SIZE, m_zoom),
            tile_x_to_longitude(longitude_to_tile_x(m_center.longitude, m_zoom) - delta_x / TILE_SIZE, m_zoom) });
        m_last_mouse_x = event.x();
        m_last_mouse_y = event.y();
        return;
    }

    // Handle panels hover
    for (auto& panel : m_panels)
        if (panel.url.has_value() && panel.rect.contains(event.x(), event.y()))
            return set_override_cursor(Gfx::StandardCursor::Hand);
    set_override_cursor(Gfx::StandardCursor::Arrow);
}

void MapWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (m_connection_failed)
        return;

    // Stop map tiles dragging
    if (m_dragging) {
        m_dragging = false;
        set_override_cursor(Gfx::StandardCursor::Arrow);
        return;
    }

    if (event.button() == GUI::MouseButton::Primary) {
        // Handle panels click
        for (auto& panel : m_panels) {
            if (panel.url.has_value() && panel.rect.contains(event.x(), event.y())) {
                Desktop::Launcher::open(panel.url.value());
                return;
            }
        }
    }
}

void MapWidget::mousewheel_event(GUI::MouseEvent& event)
{
    if (m_connection_failed)
        return;

    int new_zoom = event.wheel_delta_y() > 0 ? m_zoom - 1 : m_zoom + 1;
    set_zoom_for_mouse_event(new_zoom, event);
}

void MapWidget::set_zoom_for_mouse_event(int zoom, GUI::MouseEvent& event)
{
    if (zoom == m_zoom || zoom < ZOOM_MIN || zoom > ZOOM_MAX)
        return;
    if (zoom < m_zoom) {
        set_center({ tile_y_to_latitude(latitude_to_tile_y(m_center.latitude, m_zoom) - static_cast<double>(event.y() - height() / 2) / TILE_SIZE, m_zoom),
            tile_x_to_longitude(longitude_to_tile_x(m_center.longitude, m_zoom) - static_cast<double>(event.x() - width() / 2) / TILE_SIZE, m_zoom) });
    } else {
        set_center({ tile_y_to_latitude(latitude_to_tile_y(m_center.latitude, zoom) + static_cast<double>(event.y() - height() / 2) / TILE_SIZE, zoom),
            tile_x_to_longitude(longitude_to_tile_x(m_center.longitude, zoom) + static_cast<double>(event.x() - width() / 2) / TILE_SIZE, zoom) });
    }
    set_zoom(zoom);
}

Optional<RefPtr<Gfx::Bitmap>> MapWidget::get_tile_image(int x, int y, int zoom, TileDownloadBehavior download_behavior)
{
    // Get the tile from tiles cache
    TileKey const key = { x, y, zoom };
    if (auto it = m_tiles.find(key); it != m_tiles.end()) {
        if (it->value)
            return it->value;
        return {};
    }
    if (download_behavior == TileDownloadBehavior::DoNotDownload)
        return {};

    // Register an empty tile so we don't send requests multiple times
    if (m_tiles.size() >= TILES_CACHE_MAX)
        m_tiles.remove(m_tiles.begin());
    m_tiles.set(key, nullptr);

    // Schedule the tile download
    m_tile_queue.enqueue(key);
    process_tile_queue();
    return {};
}

void MapWidget::process_tile_queue()
{
    if (m_active_requests.size() >= TILES_DOWNLOAD_PARALLEL_MAX)
        return;
    if (m_tile_queue.is_empty())
        return;

    auto tile_key = m_tile_queue.dequeue();

    // Start HTTP GET request to load image
    HashMap<DeprecatedString, DeprecatedString> headers;
    headers.set("User-Agent", "SerenityOS Maps");
    headers.set("Accept", "image/png");
    URL url(MUST(String::formatted(m_tile_layer_url, tile_key.zoom, tile_key.x, tile_key.y)));
    auto request = m_request_client->start_request("GET", url, headers, {});
    VERIFY(!request.is_null());

    m_active_requests.append(request);
    request->on_buffered_request_finish = [this, request, url, tile_key](bool success, auto, auto&, auto, ReadonlyBytes payload) {
        auto was_active = m_active_requests.remove_first_matching([request](auto const& other_request) { return other_request->id() == request->id(); });
        if (!was_active)
            return;
        deferred_invoke([this]() { this->process_tile_queue(); });

        // When first image load fails set connection failed
        if (!success) {
            if (!m_first_image_loaded) {
                m_first_image_loaded = true;
                m_connection_failed = true;
            }
            dbgln("Maps: Can't load image: {}", url);
            return;
        }
        m_first_image_loaded = true;

        // Decode loaded PNG image data
        auto decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(payload, "image/png");
        if (!decoder || (decoder->frame_count() == 0)) {
            dbgln("Maps: Can't decode image: {}", url);
            return;
        }
        m_tiles.set(tile_key, decoder->frame(0).release_value_but_fixme_should_propagate_errors().image);

        // FIXME: only update the part of the screen that this tile covers
        update();
    };
    request->set_should_buffer_all_input(true);
    request->on_certificate_requested = []() -> Protocol::Request::CertificateAndKey { return {}; };
}

void MapWidget::clear_tile_queue()
{
    m_tile_queue.clear();

    // FIXME: ideally we would like to abort all active requests here, but invoking `->stop()`
    //        often causes hangs for me for some reason.
    m_active_requests.clear_with_capacity();

    m_tiles.remove_all_matching([](auto, auto const& value) -> bool { return !value; });
}

void MapWidget::paint_tiles(GUI::Painter& painter)
{
    int center_tile_x = floor(longitude_to_tile_x(m_center.longitude, m_zoom));
    int center_tile_y = floor(latitude_to_tile_y(m_center.latitude, m_zoom));
    double offset_x = (longitude_to_tile_x(m_center.longitude, m_zoom) - center_tile_x) * TILE_SIZE;
    double offset_y = (latitude_to_tile_y(m_center.latitude, m_zoom) - center_tile_y) * TILE_SIZE;

    // Draw grid around center tile
    int grid_width = ceil(static_cast<double>(width()) / TILE_SIZE);
    int grid_height = ceil(static_cast<double>(height()) / TILE_SIZE);
    for (int dy = -(grid_height / 2) - 1; dy < (grid_height / 2) + 2; dy++) {
        for (int dx = -(grid_width / 2) - 1; dx < (grid_width / 2) + 2; dx++) {
            int tile_x = center_tile_x + dx;
            int tile_y = center_tile_y + dy;

            // Only draw tiles that exist
            if (tile_x < 0 || tile_y < 0 || tile_x > pow(2, m_zoom) - 1 || tile_y > pow(2, m_zoom) - 1)
                continue;

            auto tile_rect = Gfx::IntRect {
                static_cast<int>(width() / 2 + dx * TILE_SIZE - offset_x),
                static_cast<int>(height() / 2 + dy * TILE_SIZE - offset_y),
                TILE_SIZE,
                TILE_SIZE,
            };
            if (!painter.clip_rect().intersects(tile_rect))
                continue;

            // Get tile, when it has a loaded image draw it at the right position
            auto tile_image = get_tile_image(tile_x, tile_y, m_zoom, TileDownloadBehavior::Download);
            auto const tile_source = Gfx::IntRect { 0, 0, TILE_SIZE, TILE_SIZE };
            if (tile_image.has_value()) {
                painter.blit(tile_rect.location(), *tile_image.release_value(), tile_source, 1);
                continue;
            }

            // Fallback: try to compose the tile from already cached tiles from a higher zoom level
            auto cached_tiles_used = 0;
            if (m_zoom < ZOOM_MAX) {
                auto const child_top_left_tile_x = tile_x * 2;
                auto const child_top_left_tile_y = tile_y * 2;
                for (auto child_tile_x = child_top_left_tile_x; child_tile_x <= child_top_left_tile_x + 1; ++child_tile_x) {
                    for (auto child_tile_y = child_top_left_tile_y; child_tile_y <= child_top_left_tile_y + 1; ++child_tile_y) {
                        auto child_tile = get_tile_image(child_tile_x, child_tile_y, m_zoom + 1, TileDownloadBehavior::DoNotDownload);
                        if (!child_tile.has_value())
                            continue;

                        auto target_rect = tile_rect;
                        target_rect.set_size(TILE_SIZE / 2, TILE_SIZE / 2);
                        if ((child_tile_x & 1) > 0)
                            target_rect.translate_by(TILE_SIZE / 2, 0);
                        if ((child_tile_y & 1) > 0)
                            target_rect.translate_by(0, TILE_SIZE / 2);

                        painter.draw_scaled_bitmap(target_rect, *child_tile.release_value(), tile_source, 1.f, Gfx::Painter::ScalingMode::BoxSampling);
                        ++cached_tiles_used;
                    }
                }
            }

            // Fallback: try to use an already cached tile from a lower zoom level
            // Note: we only want to try this if we did not find exactly 4 cached child tiles in the previous fallback (i.e. there are gaps)
            if (m_zoom > ZOOM_MIN && cached_tiles_used < 4) {
                auto const parent_tile_x = tile_x / 2;
                auto const parent_tile_y = tile_y / 2;
                auto larger_tile = get_tile_image(parent_tile_x, parent_tile_y, m_zoom - 1, TileDownloadBehavior::DoNotDownload);
                if (larger_tile.has_value()) {
                    auto source_rect = Gfx::IntRect { 0, 0, TILE_SIZE / 2, TILE_SIZE / 2 };
                    if ((tile_x & 1) > 0)
                        source_rect.translate_by(TILE_SIZE / 2, 0);
                    if ((tile_y & 1) > 0)
                        source_rect.translate_by(0, TILE_SIZE / 2);
                    painter.draw_scaled_bitmap(tile_rect, *larger_tile.release_value(), source_rect, 1.f, Gfx::Painter::ScalingMode::BilinearBlend);
                }
            }
        }
    }
}

void MapWidget::paint_scale_line(GUI::Painter& painter, String label, Gfx::IntRect rect)
{
    painter.fill_rect(rect, panel_background_color);
    painter.fill_rect({ rect.x(), rect.y(), 1, rect.height() }, panel_foreground_color);
    painter.fill_rect({ rect.x() + rect.width() - 1, rect.y(), 1, rect.height() }, panel_foreground_color);
    Gfx::FloatRect label_rect { rect.x() + PANEL_PADDING_X, rect.y() + PANEL_PADDING_Y, rect.width() - PANEL_PADDING_X * 2, rect.height() - PANEL_PADDING_Y * 2 };
    painter.draw_text(label_rect, label, Gfx::TextAlignment::TopLeft, panel_foreground_color);
}

void MapWidget::paint_scale(GUI::Painter& painter)
{
    double max_meters = m_center.distance_to({ m_center.latitude, tile_x_to_longitude(longitude_to_tile_x(m_center.longitude, m_zoom) + static_cast<double>(m_scale_max_width) / TILE_SIZE, m_zoom) });
    float margin_x = 8;
    float margin_y = 8;
    float line_height = PANEL_PADDING_Y + painter.font().pixel_size() + PANEL_PADDING_Y;

    // Metric line
    double meters = nice_round_number(max_meters);
    float metric_width = m_scale_max_width * (meters / max_meters);
    Gfx::IntRect metric_rect = { frame_inner_rect().x() + margin_x, frame_inner_rect().bottom() - margin_y - line_height * 2, metric_width, line_height };
    if (meters < 1000) {
        paint_scale_line(painter, MUST(String::formatted("{} m", meters)), metric_rect);
    } else {
        paint_scale_line(painter, MUST(String::formatted("{} km", meters / 1000)), metric_rect);
    }

    // Imperial line
    double max_feet = max_meters * 3.28084;
    double feet = nice_round_number(max_feet);
    double max_miles = max_feet / 5280;
    double miles = nice_round_number(max_miles);
    float imperial_width = m_scale_max_width * (feet < 5280 ? feet / max_feet : miles / max_miles);
    Gfx::IntRect imperial_rect = { frame_inner_rect().x() + margin_x, frame_inner_rect().bottom() - margin_y - line_height, imperial_width, line_height };
    if (feet < 5280) {
        paint_scale_line(painter, MUST(String::formatted("{} ft", feet)), imperial_rect);
    } else {
        paint_scale_line(painter, MUST(String::formatted("{} mi", miles)), imperial_rect);
    }

    // Border between
    painter.fill_rect({ frame_inner_rect().x() + margin_x, frame_inner_rect().bottom() - margin_y - line_height, max(metric_width, imperial_width), 1.0f }, panel_foreground_color);
}

void MapWidget::paint_panels(GUI::Painter& painter)
{
    for (auto& panel : m_panels) {
        int panel_width = PANEL_PADDING_X + painter.font().width(panel.text) + PANEL_PADDING_X;
        int panel_height = PANEL_PADDING_Y + painter.font().pixel_size() + PANEL_PADDING_Y;
        if (panel.position == Panel::Position::TopLeft)
            panel.rect = { frame_inner_rect().x(), frame_inner_rect().y(), panel_width, panel_height };
        if (panel.position == Panel::Position::TopRight)
            panel.rect = { frame_inner_rect().right() - panel_width, frame_inner_rect().y(), panel_width, panel_height };
        if (panel.position == Panel::Position::BottomLeft)
            panel.rect = { frame_inner_rect().x(), frame_inner_rect().bottom() - panel_height, panel_width, panel_height };
        if (panel.position == Panel::Position::BottomRight)
            panel.rect = { frame_inner_rect().right() - panel_width, frame_inner_rect().bottom() - panel_height, panel_width, panel_height };
        painter.fill_rect(panel.rect, panel_background_color);

        Gfx::FloatRect text_rect = { panel.rect.x() + PANEL_PADDING_X, panel.rect.y() + PANEL_PADDING_Y, panel.rect.width(), panel.rect.height() };
        painter.draw_text(text_rect, panel.text, Gfx::TextAlignment::TopLeft, panel_foreground_color);
    }
}

void MapWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());
    painter.fill_rect(frame_inner_rect(), map_background_color);

    if (m_connection_failed)
        return painter.draw_text(frame_inner_rect(), "Failed to fetch map tiles :^("sv, Gfx::TextAlignment::Center, panel_foreground_color);

    paint_tiles(painter);
    if (m_scale_enabled)
        paint_scale(painter);
    paint_panels(painter);
}
