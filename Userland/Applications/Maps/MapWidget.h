/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>

class MapWidget final : public GUI::Widget {
    C_OBJECT(MapWidget);

public:
    struct LatLng {
        double latitude;
        double longitude;

        double distance_to(LatLng const& other) const;
    };

    struct Options {
        String tile_layer_url { "https://tile.openstreetmap.org/{}/{}/{}.png"_string };
        LatLng center;
        int zoom;
        bool scale_enabled { true };
        int scale_max_width { 100 };
        bool attribution_enabled { true };
        String attribution_text { "© OpenStreetMap contributors"_string };
        URL attribution_url { "https://www.openstreetmap.org/copyright"sv };
    };

    LatLng center() const { return m_center; }
    void set_center(LatLng const& center)
    {
        m_center = {
            min(max(center.latitude, -LATITUDE_MAX), LATITUDE_MAX),
            min(max(center.longitude, -180.0), 180.0)
        };
        update();
    }

    int zoom() const { return m_zoom; }
    void set_zoom(int zoom);

    struct TileKey {
        int x;
        int y;
        int zoom;

        unsigned hash() const
        {
            return pair_int_hash(x, pair_int_hash(y, zoom));
        }

        bool operator==(TileKey const& other) const
        {
            return x == other.x && y == other.y && zoom == other.zoom;
        }
    };

    enum class TileDownloadBehavior {
        DoNotDownload,
        Download,
    };

private:
    MapWidget(Options const&);

    virtual void mousemove_event(GUI::MouseEvent&) override;

    virtual void mousedown_event(GUI::MouseEvent&) override;

    virtual void mouseup_event(GUI::MouseEvent&) override;

    virtual void mousewheel_event(GUI::MouseEvent&) override;

    virtual void paint_event(GUI::PaintEvent&) override;

    Optional<RefPtr<Gfx::Bitmap>> get_tile_image(int x, int y, int zoom, TileDownloadBehavior);
    void process_tile_queue();
    void clear_tile_queue();

    void paint_tiles(GUI::Painter&);

    void paint_scale_line(GUI::Painter&, String label, Gfx::IntRect rect);

    void paint_scale(GUI::Painter&);

    void paint_attribution(GUI::Painter&);

    static int constexpr TILE_SIZE = 256;
    static double constexpr LATITUDE_MAX = 85.0511287798066;
    static size_t constexpr TILES_CACHE_MAX = 256;
    static constexpr size_t TILES_DOWNLOAD_PARALLEL_MAX = 8;
    static int constexpr ZOOM_MIN = 2;
    static int constexpr ZOOM_MAX = 19;
    static float constexpr PANEL_PADDING_X = 6;
    static float constexpr PANEL_PADDING_Y = 4;

    // These colors match the default OpenStreetMap map tiles style, so they don't depend on any system theme colors
    static Gfx::Color constexpr map_background_color = { 200, 200, 200 };
    static Gfx::Color constexpr panel_background_color = { 255, 255, 255, 204 };
    static Gfx::Color constexpr panel_foreground_color = { 51, 51, 51 };

    RefPtr<Protocol::RequestClient> m_request_client;
    Vector<RefPtr<Protocol::Request>, TILES_DOWNLOAD_PARALLEL_MAX> m_active_requests;
    Queue<TileKey, 32> m_tile_queue;
    String m_tile_layer_url;
    LatLng m_center;
    int m_zoom {};
    bool m_scale_enabled {};
    int m_scale_max_width {};
    bool m_attribution_enabled {};
    String m_attribution_text;
    float m_attribution_width {};
    float m_attribution_height {};
    URL m_attribution_url;
    bool m_dragging { false };
    int m_last_mouse_x { 0 };
    int m_last_mouse_y { 0 };
    bool m_first_image_loaded { false };
    bool m_connection_failed { false };
    OrderedHashMap<TileKey, RefPtr<Gfx::Bitmap>> m_tiles;
};

template<>
struct AK::Traits<MapWidget::TileKey> : public GenericTraits<MapWidget::TileKey> {
    static unsigned hash(MapWidget::TileKey const& t) { return t.hash(); }
};
