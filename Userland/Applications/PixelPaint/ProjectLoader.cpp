/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectLoader.h"
#include "Image.h"
#include "Layer.h"
#include <AK/JsonObject.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibImageDecoderClient/Client.h>

namespace PixelPaint {

ErrorOr<void> ProjectLoader::try_load_from_file(Core::File& file)
{
    auto contents = file.read_all();

    auto json_or_error = JsonValue::from_string(contents);
    if (json_or_error.is_error()) {
        m_is_raw_image = true;

        // FIXME: Find a way to avoid the memory copy here.
        auto bitmap = TRY(Image::try_decode_bitmap(contents));
        auto image = TRY(Image::try_create_from_bitmap(move(bitmap)));

        m_image = image;
        return {};
    }

    auto& json = json_or_error.value().as_object();
    auto image = TRY(Image::try_create_from_pixel_paint_json(json));

    if (json.has("guides"))
        m_json_metadata = json.get("guides").as_array();

    m_image = image;
    return {};
}

}
