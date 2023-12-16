/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TimeZoneSettingsWidget.h"
#include <AK/Time.h>
#include <Applications/ClockSettings/TimeZoneSettingsWidgetGML.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Event.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/Layout.h>
#include <LibGUI/Margins.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Process.h>
#include <LibGfx/Palette.h>
#include <LibLocale/DateTimeFormat.h>
#include <LibLocale/Locale.h>
#include <LibTimeZone/TimeZone.h>
#include <math.h>
#include <spawn.h>
#include <unistd.h>

static constexpr auto PI_OVER_180 = M_PIf32 / 180.0f;
static constexpr auto PI_OVER_4 = M_PIf32 / 4.0f;
static constexpr auto TAU = M_PIf32 * 2.0f;

// The map as stored on disk is a valid Mercadian projected map. But it has quite a bit of dead space that
// we can remove. This makes the map non-Mercadian, so we need to adjust our math based on what we removed.
static constexpr auto TIME_ZONE_MAP_NORTHERN_TRIM = 78;
static constexpr auto TIME_ZONE_MAP_SOUTHERN_TRIM = 50;

static constexpr auto TIME_ZONE_TEXT_WIDTH = 210;
static constexpr auto TIME_ZONE_TEXT_HEIGHT = 40;
static constexpr auto TIME_ZONE_TEXT_PADDING = 5;
static constexpr auto TIME_ZONE_TEXT_COLOR = Gfx::Color::from_rgb(0xeaf688);

ErrorOr<NonnullRefPtr<TimeZoneSettingsWidget>> TimeZoneSettingsWidget::create()
{
    auto timezonesettings_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) TimeZoneSettingsWidget));

    auto time_zone_map_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/map.png"sv));
    auto time_zone_rect = time_zone_map_bitmap->rect().shrunken(TIME_ZONE_MAP_NORTHERN_TRIM, 0, TIME_ZONE_MAP_SOUTHERN_TRIM, 0);
    time_zone_map_bitmap = TRY(time_zone_map_bitmap->cropped(time_zone_rect));

    timezonesettings_widget->m_time_zone_map = *timezonesettings_widget->find_descendant_of_type_named<GUI::ImageWidget>("time_zone_map");
    timezonesettings_widget->m_time_zone_map->set_bitmap(time_zone_map_bitmap);

    auto time_zone_marker = TRY(Gfx::Bitmap::load_from_file("/res/icons/32x32/ladyball.png"sv));
    timezonesettings_widget->m_time_zone_marker = TRY(time_zone_marker->scaled(0.75f, 0.75f));

    timezonesettings_widget->set_time_zone_location();

    return timezonesettings_widget;
}

TimeZoneSettingsWidget::TimeZoneSettingsWidget()
{
    load_from_gml(time_zone_settings_widget_gml).release_value_but_fixme_should_propagate_errors();

    static auto time_zones = []() {
        Vector<StringView> time_zones;

        for (auto const& time_zone : TimeZone::all_time_zones()) {
            if (time_zone.is_link == TimeZone::IsLink::No)
                time_zones.append(time_zone.name);
        }

        return time_zones;
    }();

    m_time_zone = TimeZone::system_time_zone();

    m_time_zone_combo_box = *find_descendant_of_type_named<GUI::ComboBox>("time_zone_input");
    m_time_zone_combo_box->set_only_allow_values_from_model(true);
    m_time_zone_combo_box->set_model(*GUI::ItemListModel<StringView>::create(time_zones));
    m_time_zone_combo_box->set_text(m_time_zone);
    m_time_zone_combo_box->on_change = [&](auto, auto) {
        set_modified(true);
    };
}

void TimeZoneSettingsWidget::second_paint_event(GUI::PaintEvent& event)
{
    GUI::Widget::second_paint_event(event);

    if (!m_time_zone_location.has_value())
        return;

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(m_time_zone_map->relative_rect());

    auto x = m_time_zone_map->x() + m_time_zone_map->parent_widget()->layout()->margins().left();
    auto y = m_time_zone_map->y() + m_time_zone_map->parent_widget()->layout()->margins().top();

    auto point = m_time_zone_location->to_type<int>().translated(x, y);
    point.translate_by(-m_time_zone_marker->width() / 2, -m_time_zone_marker->height() / 2);
    painter.blit(point, *m_time_zone_marker, rect());

    point = m_time_zone_location->to_type<int>().translated(x, y);
    point.translate_by(0, -TIME_ZONE_TEXT_HEIGHT / 2);

    if (point.x() <= (m_time_zone_map->width() / 2))
        point.translate_by(m_time_zone_marker->width() / 2 + TIME_ZONE_TEXT_PADDING, 0);
    else
        point.translate_by(-m_time_zone_marker->width() / 2 - TIME_ZONE_TEXT_PADDING - TIME_ZONE_TEXT_WIDTH, 0);

    auto text_area = Gfx::IntRect { point.x(), point.y(), TIME_ZONE_TEXT_WIDTH, TIME_ZONE_TEXT_HEIGHT };
    painter.draw_rect(text_area, palette().active_window_border1());

    text_area.shrink(2, 2);
    painter.fill_rect(text_area, TIME_ZONE_TEXT_COLOR);
    painter.draw_text(text_area, m_time_zone_text, Gfx::TextAlignment::Center);
}

void TimeZoneSettingsWidget::reset_default_values()
{
    m_time_zone = "UTC"sv;
    m_time_zone_combo_box->set_text(m_time_zone);
    m_time_zone_location.clear();

    set_time_zone();
    update();
}

void TimeZoneSettingsWidget::apply_settings()
{
    m_time_zone = m_time_zone_combo_box->text();

    set_time_zone_location();
    set_time_zone();
    update();
}

void TimeZoneSettingsWidget::set_time_zone_location()
{
    m_time_zone_location = compute_time_zone_location();

    auto locale = Locale::default_locale();
    auto now = AK::UnixDateTime::now();

    auto name = Locale::format_time_zone(locale, m_time_zone, Locale::CalendarPatternStyle::Long, now);
    auto offset = Locale::format_time_zone(locale, m_time_zone, Locale::CalendarPatternStyle::LongOffset, now);

    m_time_zone_text = ByteString::formatted("{}\n({})", name, offset);
}

// https://en.wikipedia.org/wiki/Mercator_projection#Derivation
Optional<Gfx::FloatPoint> TimeZoneSettingsWidget::compute_time_zone_location() const
{
    auto location = TimeZone::get_time_zone_location(m_time_zone);
    if (!location.has_value())
        return {};

    auto latitude = location->latitude.decimal_coordinate();
    auto longitude = location->longitude.decimal_coordinate();

    auto rect = m_time_zone_map->bitmap()->rect().to_type<float>();

    latitude = logf(tanf(PI_OVER_4 + (latitude * PI_OVER_180 / 2.0f)));

    auto mercadian_x = (longitude + 180.0f) * (rect.width() / 360.0f);
    auto mercadian_y = (rect.height() / 2.0f) - (rect.width() * latitude / TAU);

    mercadian_y -= TIME_ZONE_MAP_NORTHERN_TRIM / 2;
    mercadian_y += TIME_ZONE_MAP_SOUTHERN_TRIM / 2;

    return Gfx::FloatPoint { mercadian_x, mercadian_y };
}

void TimeZoneSettingsWidget::set_time_zone()
{
    GUI::Process::spawn_or_show_error(window(), "/bin/timezone"sv, Array { m_time_zone.characters() });
}
