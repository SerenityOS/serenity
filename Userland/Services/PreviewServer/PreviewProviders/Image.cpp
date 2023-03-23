/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Image.h"
#include <LibCore/File.h>
#include <LibCore/MimeData.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/Painter.h>

REGISTER_PREVIEW_PROVIDER(Image)

namespace PreviewServer::Providers {

bool Image::can_generate_preview_for(String const& file)
{
    constexpr StringView image_mime_group = "image"sv;
    // Ideally, detect image files without even opening the file.
    if (Core::guess_mime_type_based_on_filename(file).starts_with(image_mime_group))
        return true;

    // Sniff 1KiB of the file's contents and try to guess the file type based on that.
    auto maybe_open_file = Core::File::open(file, Core::File::OpenMode::Read);
    if (maybe_open_file.is_error())
        return false;
    auto open_file = maybe_open_file.release_value();
    auto maybe_first_kibibyte = ByteBuffer::create_uninitialized(KiB);
    if (maybe_first_kibibyte.is_error())
        return false;
    auto first_kibibyte = maybe_first_kibibyte.release_value();
    auto read_result = open_file->read_until_filled(first_kibibyte);
    if (read_result.is_error())
        return false;

    if (Core::guess_mime_type_based_on_sniffed_bytes(first_kibibyte)->starts_with(image_mime_group))
        return true;
    return false;
}

CacheEntry Image::generate_preview(String const& file)
{
    dbgln_if(PREVIEW_SERVER_DEBUG, "Generating image preview for {}", file);
    auto open_file = TRY(Core::File::open(file, Core::File::OpenMode::Read));
    auto image_data = TRY(open_file->read_until_eof());
    auto maybe_image = TRY(image_decoder_client())->decode_image(image_data);
    if (!maybe_image.has_value())
        return Error::from_string_view("Image decoding failed"sv);
    auto image_descriptor = maybe_image->frames[0];
    if (image_descriptor.bitmap.is_null())
        return Error::from_string_view("Reading first frame of image failed"sv);
    auto image = image_descriptor.bitmap.release_nonnull();

    auto preview = TRY(Gfx::Bitmap::create_shareable(Gfx::BitmapFormat::BGRA8888, { preview_size, preview_size }));

    double scale = min(preview_size / static_cast<double>(image->width()), preview_size / static_cast<double>(image->height()));
    auto destination = Gfx::IntRect(0, 0, (int)(image->width() * scale), (int)(image->height() * scale)).centered_within(preview->rect());

    Gfx::Painter painter { preview };
    painter.clear_rect(preview->rect(), Gfx::Color::Transparent);
    // If the image is smaller than 32x32, it's most likely pixel art.
    auto scaling_mode = image->rect().width() <= preview->rect().width() || image->rect().height() <= preview->rect().height()
        ? Gfx::Painter::ScalingMode::NearestNeighbor
        : Gfx::Painter::ScalingMode::SmoothPixels;
    painter.draw_scaled_bitmap(destination, *image, image->rect(), 1.0f, scaling_mode);

    return ValidCacheEntry {
        preview->to_shareable_bitmap(),
        LexicalPath { file.to_deprecated_string() }
    };
}

}
