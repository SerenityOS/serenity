/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectLoader.h"
#include "Image.h"
#include "Layer.h"
#include <AK/JsonObject.h>
#include <AK/MappedFile.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <LibCore/File.h>
#include <LibImageDecoderClient/Client.h>

namespace PixelPaint {

Result<void, String> ProjectLoader::try_load_from_fd_and_close(int fd, StringView path)
{
    auto file = Core::File::construct();
    file->open(fd, Core::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescriptor::No);
    if (file->has_error())
        return String { file->error_string() };

    auto contents = file->read_all();

    auto json_or_error = JsonValue::from_string(contents);
    if (!json_or_error.has_value()) {
        m_is_raw_image = true;

        auto file_or_error = MappedFile::map_from_fd_and_close(fd, path);
        if (file_or_error.is_error())
            return String::formatted("Unable to mmap file {}", file_or_error.error().string());

        auto& mapped_file = *file_or_error.value();
        // FIXME: Find a way to avoid the memory copy here.
        auto bitmap = Image::try_decode_bitmap(mapped_file.bytes());
        if (!bitmap)
            return String { "Unable to decode image"sv };
        auto image = Image::try_create_from_bitmap(bitmap.release_nonnull());
        if (!image)
            return String { "Unable to allocate Image"sv };

        image->set_path(path);
        m_image = image;
        return {};
    }

    close(fd);
    auto& json = json_or_error.value().as_object();
    auto image_or_error = Image::try_create_from_pixel_paint_json(json);

    if (image_or_error.is_error())
        return image_or_error.release_error();

    auto image = image_or_error.release_value();
    image->set_path(path);

    if (json.has("guides"))
        m_json_metadata = json.get("guides").as_array();

    m_image = image;
    return {};
}

}
