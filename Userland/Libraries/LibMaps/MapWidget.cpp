/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/MapsSettings/Defaults.h>
#include <LibConfig/Client.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibMaps/MapWidget.h>
#include <LibProtocol/Request.h>
#include <LibURL/URL.h>

namespace Maps {

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
    return EARTH_RADIUS * 2.0 * asin(sqrt(pow(sin((AK::to_radians(other.latitude) - AK::to_radians(latitude)) / 2.0), 2.0) + cos(AK::to_radians(latitude)) * cos(AK::to_radians(other.latitude)) * pow(sin((AK::to_radians(other.longitude) - AK::to_radians(longitude)) / 2.0), 2.0)));
}

int MapWidget::LatLngBounds::get_zoom() const
{
    double distance_meters = north_west.distance_to(south_east);
    int zoom = ZOOM_MIN;
    while (distance_meters < EARTH_RADIUS / pow(2, zoom - 1) && zoom != ZOOM_MAX)
        ++zoom;
    return min(zoom + 1, ZOOM_MAX);
}

// MapWidget class
MapWidget::MapWidget(Options const& options)
    : m_tile_provider(options.tile_provider)
    , m_center(options.center)
    , m_zoom(options.zoom)
    , m_context_menu_enabled(options.context_menu_enabled)
    , m_scale_enabled(options.scale_enabled)
    , m_scale_max_width(options.scale_max_width)
    , m_attribution_enabled(options.attribution_enabled)
{
    m_request_client = Protocol::RequestClient::try_create().release_value_but_fixme_should_propagate_errors();
    if (options.attribution_enabled) {
        auto attribution_text = options.attribution_text.value_or(MUST(String::from_byte_string(Config::read_string("Maps"sv, "MapWidget"sv, "TileProviderAttributionText"sv, Maps::default_tile_provider_attribution_text))));
        URL::URL attribution_url = options.attribution_url.value_or(URL::URL(Config::read_string("Maps"sv, "MapWidget"sv, "TileProviderAttributionUrl"sv, Maps::default_tile_provider_attribution_url)));
        add_panel({ attribution_text, Panel::Position::BottomRight, attribution_url, "attribution"_string });
    }
    m_marker_image = Gfx::Bitmap::load_from_file("/res/graphics/maps/marker-blue.png"sv).release_value_but_fixme_should_propagate_errors();
    m_default_tile_provider = MUST(String::from_byte_string(Config::read_string("Maps"sv, "MapWidget"sv, "TileProviderUrlFormat"sv, Maps::default_tile_provider_url_format)));
}

void MapWidget::set_zoom(int zoom)
{
    m_zoom = min(max(zoom, ZOOM_MIN), ZOOM_MAX);
    clear_tile_queue();
    update();
}

void MapWidget::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
{
    if (domain != "Maps" || group != "MapWidget")
        return;

    if (key == "TileProviderUrlFormat") {
        // When config tile provider changes clear all active requests and loaded tiles
        m_default_tile_provider = MUST(String::from_utf8(value));
        m_first_image_loaded = false;
        m_active_requests.clear();
        m_tiles.clear();
        update();
    }

    if (key == "TileProviderAttributionText") {
        // Update attribution panel text when it exists
        for (auto& panel : m_panels) {
            if (panel.name == "attribution") {
                panel.text = MUST(String::from_utf8(value));
                return;
            }
        }
        update();
    }

    if (key == "TileProviderAttributionUrl") {
        // Update attribution panel url when it exists
        for (auto& panel : m_panels) {
            if (panel.name == "attribution") {
                panel.url = URL::URL(value);
                return;
            }
        }
    }
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

    // Handle marker tooltip hover
    int center_tile_x = longitude_to_tile_x(m_center.longitude, m_zoom);
    int center_tile_y = latitude_to_tile_y(m_center.latitude, m_zoom);
    double offset_x = (longitude_to_tile_x(m_center.longitude, m_zoom) - center_tile_x) * TILE_SIZE;
    double offset_y = (latitude_to_tile_y(m_center.latitude, m_zoom) - center_tile_y) * TILE_SIZE;
    for (auto const& marker : m_markers) {
        if (!marker.tooltip.has_value())
            continue;
        RefPtr<Gfx::Bitmap> marker_image = marker.image ? marker.image : m_marker_image;
        Gfx::IntRect marker_rect = {
            static_cast<int>(width() / 2 + (longitude_to_tile_x(marker.latlng.longitude, m_zoom) - center_tile_x) * TILE_SIZE - offset_x) - marker_image->width() / 2,
            static_cast<int>(height() / 2 + (latitude_to_tile_y(marker.latlng.latitude, m_zoom) - center_tile_y) * TILE_SIZE - offset_y) - marker_image->height(),
            marker_image->width(),
            marker_image->height()
        };
        if (marker_rect.contains(event.x(), event.y())) {
            GUI::Application::the()->show_tooltip(marker.tooltip.value(), this);
            return;
        }
    }
    GUI::Application::the()->hide_tooltip();
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

void MapWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu_enabled)
        return;

    m_context_menu_latlng = { tile_y_to_latitude(latitude_to_tile_y(m_center.latitude, m_zoom) + static_cast<double>(event.position().y() - height() / 2) / TILE_SIZE, m_zoom),
        tile_x_to_longitude(longitude_to_tile_x(m_center.longitude, m_zoom) + static_cast<double>(event.position().x() - width() / 2) / TILE_SIZE, m_zoom) };

    m_context_menu = GUI::Menu::construct();
    m_context_menu->add_action(GUI::Action::create(
        "&Copy Coordinates to Clipboard", MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"sv)), [this](auto&) {
            GUI::Clipboard::the().set_plain_text(MUST(String::formatted("{}, {}", m_context_menu_latlng.latitude, m_context_menu_latlng.longitude)));
        }));
    m_context_menu->add_separator();
    if (!m_context_menu_actions.is_empty()) {
        for (auto& action : m_context_menu_actions)
            m_context_menu->add_action(action);
        m_context_menu->add_separator();
    }
    auto link_icon = MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-symlink.png"sv));
    m_context_menu->add_action(GUI::Action::create(
        "Open in &OpenStreetMap", link_icon, [this](auto&) {
            Desktop::Launcher::open(URL::URL(MUST(String::formatted("https://www.openstreetmap.org/#map={}/{}/{}", m_zoom, m_context_menu_latlng.latitude, m_context_menu_latlng.longitude))));
        }));
    m_context_menu->add_action(GUI::Action::create(
        "Open in &Google Maps", link_icon, [this](auto&) {
            Desktop::Launcher::open(URL::URL(MUST(String::formatted("https://www.google.com/maps/@{},{},{}z", m_context_menu_latlng.latitude, m_context_menu_latlng.longitude, m_zoom))));
        }));
    m_context_menu->add_action(GUI::Action::create(
        "Open in &Bing Maps", link_icon, [this](auto&) {
            Desktop::Launcher::open(URL::URL(MUST(String::formatted("https://www.bing.com/maps/?cp={}~{}&lvl={}", m_context_menu_latlng.latitude, m_context_menu_latlng.longitude, m_zoom))));
        }));
    m_context_menu->add_action(GUI::Action::create(
        "Open in &DuckDuckGo Maps", link_icon, [this](auto&) {
            Desktop::Launcher::open(URL::URL(MUST(String::formatted("https://duckduckgo.com/?q={},+{}&ia=web&iaxm=maps", m_context_menu_latlng.latitude, m_context_menu_latlng.longitude))));
        }));
    m_context_menu->add_separator();
    m_context_menu->add_action(GUI::Action::create(
        "Center &map here", MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/scale.png"sv)), [this](auto&) { set_center(m_context_menu_latlng); }));
    m_context_menu->popup(event.screen_position());
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
    HTTP::HeaderMap headers;
    headers.set("User-Agent", "SerenityOS Maps");
    headers.set("Accept", "image/png");
    URL::URL url(MUST(String::formatted(m_tile_provider.value_or(m_default_tile_provider), tile_key.zoom, tile_key.x, tile_key.y)));
    auto request = m_request_client->start_request("GET", url, headers, {});
    VERIFY(!request.is_null());

    m_active_requests.append(request);

    request->set_buffered_request_finished_callback([this, request, url, tile_key](bool success, auto, auto&, auto, ReadonlyBytes payload) {
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
        auto decoder_or_err = Gfx::ImageDecoder::try_create_for_raw_bytes(payload, "image/png");
        if (decoder_or_err.is_error() || !decoder_or_err.value() || (decoder_or_err.value()->frame_count() == 0)) {
            dbgln("Maps: Can't decode image: {}", url);
            return;
        }
        auto decoder = decoder_or_err.release_value();
        m_tiles.set(tile_key, decoder->frame(0).release_value_but_fixme_should_propagate_errors().image);

        // FIXME: only update the part of the screen that this tile covers
        update();
    });

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

// Iterates from the center (0,0) outwards, towards a certain width or height (inclusive).
// The current iteration algorithm is a basic generator-like spiral algorithm adapted for C++ iterators.
template<Integral T>
class CenterOutwardsIterable {
public:
    using ElementType = T;

    constexpr CenterOutwardsIterable(T width, T height)
        : m_width(move(width))
        , m_height(move(height))
    {
    }

    struct Iterator {
        T width;
        T height;

        T index { 0 };

        Gfx::Point<T> position { 0, 0 };

        constexpr Iterator(T width, T height)
            : width(move(width))
            , height(move(height))
        {
        }

        constexpr Iterator end() const
        {
            auto end = *this;
            end.index = AK::max(width, height) * AK::max(width, height);
            return end;
        }
        constexpr bool is_end() const { return *this == end(); }

        constexpr bool operator==(Iterator const& other) const { return index == other.index; }
        constexpr bool operator!=(Iterator const& other) const { return index != other.index; }

        constexpr Gfx::Point<T> operator*() const { return position; }

        constexpr Iterator operator++()
        {
            while (!is_end()) {
                // Figure out in which of the four squares we are.
                if (AK::abs(position.x()) <= AK::abs(position.y()) && (position.x() != position.y() || position.x() >= 0))
                    position.translate_by(position.y() >= 0 ? 1 : -1, 0);
                else
                    position.translate_by(0, position.x() >= 0 ? -1 : 1);
                ++index;

                // Translating the coordinates makes the range check simpler:
                T xp = position.x() + width / 2;
                T yp = position.y() + height / 2;
                if (xp >= 0 && xp <= width && yp >= 0 && yp <= height) {
                    break;
                }
            }

            return *this;
        }
    };

    constexpr Iterator begin() { return Iterator { m_width, m_height }; }
    constexpr Iterator end() { return begin().end(); }

private:
    T m_width;
    T m_height;
};

void MapWidget::paint_map(GUI::Painter& painter)
{
    int center_tile_x = longitude_to_tile_x(m_center.longitude, m_zoom);
    int center_tile_y = latitude_to_tile_y(m_center.latitude, m_zoom);
    double offset_x = (longitude_to_tile_x(m_center.longitude, m_zoom) - center_tile_x) * TILE_SIZE;
    double offset_y = (latitude_to_tile_y(m_center.latitude, m_zoom) - center_tile_y) * TILE_SIZE;

    // Draw grid around center tile; always pad the dimensions with 2 tiles for left/right and top/bottom edges
    // plus one additional tile to account for the width() / 2 in CenterOutwardsIterable.
    int grid_width = width() / TILE_SIZE + 3;
    int grid_height = height() / TILE_SIZE + 3;
    for (auto const delta : CenterOutwardsIterable { grid_width, grid_height }) {
        int tile_x = center_tile_x + delta.x();
        int tile_y = center_tile_y + delta.y();

        // Only draw tiles that exist
        if (tile_x < 0 || tile_y < 0 || tile_x > pow(2, m_zoom) - 1 || tile_y > pow(2, m_zoom) - 1)
            continue;

        auto tile_rect = Gfx::IntRect {
            static_cast<int>(width() / 2 + delta.x() * TILE_SIZE - offset_x),
            static_cast<int>(height() / 2 + delta.y() * TILE_SIZE - offset_y),
            TILE_SIZE,
            TILE_SIZE,
        };
        if (!tile_rect.intersects(frame_inner_rect()))
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

                    painter.draw_scaled_bitmap(target_rect, *child_tile.release_value(), tile_source, 1.f, Gfx::ScalingMode::BilinearBlend);
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
                painter.draw_scaled_bitmap(tile_rect, *larger_tile.release_value(), source_rect, 1.f, Gfx::ScalingMode::BilinearBlend);
            }
        }
    }

    // Draw markers
    for (auto const& marker : m_markers) {
        RefPtr<Gfx::Bitmap> marker_image = marker.image ? marker.image : m_marker_image;
        Gfx::IntRect marker_rect = {
            static_cast<int>(width() / 2 + (longitude_to_tile_x(marker.latlng.longitude, m_zoom) - center_tile_x) * TILE_SIZE - offset_x) - marker_image->width() / 2,
            static_cast<int>(height() / 2 + (latitude_to_tile_y(marker.latlng.latitude, m_zoom) - center_tile_y) * TILE_SIZE - offset_y) - marker_image->height(),
            marker_image->width(),
            marker_image->height()
        };
        if (marker_rect.intersects(frame_inner_rect()))
            painter.blit(marker_rect.location(), *marker_image, { 0, 0, marker_image->width(), marker_image->height() }, 1);
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

    paint_map(painter);
    if (m_scale_enabled)
        paint_scale(painter);
    paint_panels(painter);
}

}
