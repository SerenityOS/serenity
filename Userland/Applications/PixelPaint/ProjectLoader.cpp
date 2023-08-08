/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectLoader.h"
#include "Image.h"
#include "Layer.h"
#include <AK/JsonObject.h>
#include <LibCore/MimeData.h>
#include <LibImageDecoderClient/Client.h>

namespace PixelPaint {

ErrorOr<void> ProjectLoader::load_from_file(StringView filename, NonnullOwnPtr<Core::File> file)
{
    auto contents = TRY(file->read_until_eof());

    auto json_or_error = JsonValue::from_string(contents);
    if (json_or_error.is_error()) {
        m_is_raw_image = true;
        auto guessed_mime_type = Core::guess_mime_type_based_on_filename(filename);

        // FIXME: Find a way to avoid the memory copy here.
        auto bitmap = TRY(Image::decode_bitmap(contents, guessed_mime_type));
        auto image = TRY(Image::create_from_bitmap(move(bitmap)));

        m_image = image;
        return {};
    }

    auto& json = json_or_error.value().as_object();
    auto image = TRY(Image::create_from_pixel_paint_json(json));

    if (json.has_array("guides"sv))
        m_json_metadata = json.get_array("guides"sv).value();

    m_image = image;
    return {};
}

}
