/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MapWidget.h"
#include <AK/URL.h>
#include <LibDesktop/Launcher.h>
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
    , m_attribution_text(options.attribution_text)
    , m_attribution_url(options.attribution_url)
{
    m_request_client = Protocol::RequestClient::try_create().release_value_but_fixme_should_propagate_errors();
}

void MapWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (m_connection_failed)
        return;

    if (event.button() == GUI::MouseButton::Primary) {
        // Ignore attribution click
        if (m_attribution_enabled && static_cast<float>(event.x()) > width() - m_attribution_width && static_cast<float>(event.y()) > height() - m_attribution_height) {
            return;
        }

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

    // Handle attribution hover
    if (m_attribution_enabled) {
        if (static_cast<float>(event.x()) > width() - m_attribution_width && static_cast<float>(event.y()) > height() - m_attribution_height) {
            set_override_cursor(Gfx::StandardCursor::Hand);
        } else {
            set_override_cursor(Gfx::StandardCursor::Arrow);
        }
    }
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
        // Handle attribution click
        if (m_attribution_enabled && static_cast<float>(event.x()) > width() - m_attribution_width && static_cast<float>(event.y()) > height() - m_attribution_height) {
            Desktop::Launcher::open(m_attribution_url);
            return;
        }
    }
}

void MapWidget::mousewheel_event(GUI::MouseEvent& event)
{
    if (m_connection_failed)
        return;

    int new_zoom = event.wheel_delta_y() > 0 ? m_zoom - 1 : m_zoom + 1;
    if (new_zoom < ZOOM_MIN || new_zoom > ZOOM_MAX)
        return;
    if (event.wheel_delta_y() > 0) {
        set_center({ tile_y_to_latitude(latitude_to_tile_y(m_center.latitude, m_zoom) - static_cast<double>(event.y() - height() / 2) / TILE_SIZE, m_zoom),
            tile_x_to_longitude(longitude_to_tile_x(m_center.longitude, m_zoom) - static_cast<double>(event.x() - width() / 2) / TILE_SIZE, m_zoom) });
    } else {
        set_center({ tile_y_to_latitude(latitude_to_tile_y(m_center.latitude, new_zoom) + static_cast<double>(event.y() - height() / 2) / TILE_SIZE, new_zoom),
            tile_x_to_longitude(longitude_to_tile_x(m_center.longitude, new_zoom) + static_cast<double>(event.x() - width() / 2) / TILE_SIZE, new_zoom) });
    }
    set_zoom(new_zoom);
}

Optional<RefPtr<Gfx::Bitmap>> MapWidget::get_tile_image(int x, int y)
{
    // Get the right tile from tiles cache
    TileKey key = { x, y, m_zoom };
    if (auto it = m_tiles.find(key); it != m_tiles.end()) {
        if (it->value)
            return it->value;
        return {};
    }

    // Add tile when not in tiles
    if (m_tiles.size() >= TILES_CACHE_MAX)
        m_tiles.remove(m_tiles.begin());
    m_tiles.set(key, nullptr);

    // Start HTTP GET request to load image
    HashMap<DeprecatedString, DeprecatedString> headers;
    headers.set("User-Agent", "SerenityOS Maps");
    headers.set("Accept", "image/png");
    URL url(MUST(String::formatted(m_tile_layer_url, m_zoom, x, y)));
    auto request = m_request_client->start_request("GET", url, headers, {});
    m_active_requests.append(request);
    request->on_buffered_request_finish = [this, request, url, key](bool success, auto, auto&, auto, ReadonlyBytes payload) {
        m_active_requests.remove_all_matching([request](auto const& other_request) { return other_request->id() == request->id(); });
        if (!success) {
            // When first image load fails set connection failed
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
        m_tiles.set(key, decoder->frame(0).release_value_but_fixme_should_propagate_errors().image);
        update();
    };
    request->set_should_buffer_all_input(true);
    request->on_certificate_requested = []() -> Protocol::Request::CertificateAndKey { return {}; };

    // Return no image for now
    return {};
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

            // Get tile, when it has a loaded image draw it at the right position
            auto tile_image = get_tile_image(tile_x, tile_y);
            if (tile_image.has_value())
                painter.blit({ static_cast<int>(width() / 2 + dx * TILE_SIZE - offset_x), static_cast<int>(height() / 2 + dy * TILE_SIZE - offset_y) }, *tile_image.release_value(), { 0, 0, TILE_SIZE, TILE_SIZE }, 1);
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
    Gfx::IntRect metric_rect = { margin_x, height() - margin_y - line_height * 2, metric_width, line_height };
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
    Gfx::IntRect imperial_rect = { margin_x, height() - margin_y - line_height, imperial_width, line_height };
    if (feet < 5280) {
        paint_scale_line(painter, MUST(String::formatted("{} ft", feet)), imperial_rect);
    } else {
        paint_scale_line(painter, MUST(String::formatted("{} mi", miles)), imperial_rect);
    }

    // Border between
    painter.fill_rect({ margin_x, height() - margin_y - line_height, max(metric_width, imperial_width), 1.0f }, panel_foreground_color);
}

void MapWidget::paint_attribution(GUI::Painter& painter)
{
    m_attribution_width = PANEL_PADDING_X + painter.font().width(m_attribution_text) + PANEL_PADDING_X;
    m_attribution_height = PANEL_PADDING_Y + painter.font().pixel_size() + PANEL_PADDING_Y;
    painter.fill_rect({ width() - m_attribution_width, height() - m_attribution_height, m_attribution_width, m_attribution_height }, panel_background_color);
    Gfx::FloatRect attribution_text_rect { 0.0f, 0.0f, width() - PANEL_PADDING_X, height() - PANEL_PADDING_Y };
    painter.draw_text(attribution_text_rect, m_attribution_text, Gfx::TextAlignment::BottomRight, panel_foreground_color);
}

void MapWidget::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);
    painter.fill_rect(rect(), map_background_color);

    if (m_connection_failed) {
        painter.draw_text(rect(), "Failed to fetch map tiles :^("sv, Gfx::TextAlignment::Center, panel_foreground_color);
        return;
    }

    paint_tiles(painter);
    if (m_scale_enabled)
        paint_scale(painter);
    if (m_attribution_enabled)
        paint_attribution(painter);
}
