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

ErrorOr<void> ProjectLoader::try_load_from_fd_and_close(int fd, StringView path)
{
    auto file = Core::File::construct();
    file->open(fd, Core::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescriptor::No);
    if (file->has_error())
        return Error::from_errno(file->error());

    auto contents = file->read_all();

    auto json_or_error = JsonValue::from_string(contents);
    if (json_or_error.is_error()) {
        m_is_raw_image = true;

        auto mapped_file = TRY(Core::MappedFile::map_from_fd_and_close(fd, path));

        // FIXME: Find a way to avoid the memory copy here.
        auto bitmap = TRY(Image::try_decode_bitmap(mapped_file->bytes()));
        auto image = TRY(Image::try_create_from_bitmap(move(bitmap)));

        image->set_path(path);
        m_image = image;
        return {};
    }

    close(fd);
    auto& json = json_or_error.value().as_object();
    auto image = TRY(Image::try_create_from_pixel_paint_json(json));

    image->set_path(path);

    if (json.has("guides"))
        m_json_metadata = json.get("guides").as_array();

    m_image = image;
    return {};
}

}
