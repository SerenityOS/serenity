/*
 * Copyright (c) 2024, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageFormats/TIFFMetadata.h>

namespace Gfx {

class ExifGPS {
public:
    static Optional<ExifGPS> from_exif_metadata(ExifMetadata const& metadata)
    {
        auto optional_gps_latitude = metadata.gps_latitude();
        auto optional_gps_latitude_ref = metadata.gps_latitude_ref();
        auto optional_gps_longitude = metadata.gps_longitude();
        auto optional_gps_longitude_ref = metadata.gps_longitude_ref();
        if (optional_gps_latitude.has_value() && optional_gps_latitude_ref.has_value() && optional_gps_longitude.has_value() && optional_gps_longitude_ref.has_value()) {
            auto gps_latitude = optional_gps_latitude.release_value();
            auto gps_longitude = optional_gps_longitude.release_value();
            double latitude = gps_latitude[0].as_double() + (gps_latitude[1].as_double() / 60.0) + (gps_latitude[2].as_double() / 3600.0);
            if (optional_gps_latitude_ref->starts_with('S'))
                latitude = -latitude;
            double longitude = gps_longitude[0].as_double() + (gps_longitude[1].as_double() / 60.0) + (gps_longitude[2].as_double() / 3600.0);
            if (optional_gps_longitude_ref->starts_with('W'))
                longitude = -longitude;
            return ExifGPS { latitude, longitude };
        }
        return OptionalNone {};
    }

    double latitude() const { return m_latitude; }
    double longitude() const { return m_longitude; }

private:
    ExifGPS(double latitude, double longitude)
        : m_latitude(latitude)
        , m_longitude(longitude)
    {
    }

    double m_latitude;
    double m_longitude;
};

}
