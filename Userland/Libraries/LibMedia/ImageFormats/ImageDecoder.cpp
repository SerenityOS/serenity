/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/File.h>
#include <LibCore/MimeData.h>
#include <LibMedia/ImageFormats/BMPLoader.h>
#include <LibMedia/ImageFormats/DDSLoader.h>
#include <LibMedia/ImageFormats/GIFLoader.h>
#include <LibMedia/ImageFormats/ICOLoader.h>
#include <LibMedia/ImageFormats/ILBMLoader.h>
#include <LibMedia/ImageFormats/ImageDecoder.h>
#include <LibMedia/ImageFormats/JPEGLoader.h>
#include <LibMedia/ImageFormats/JPEGXLLoader.h>
#include <LibMedia/ImageFormats/PBMLoader.h>
#include <LibMedia/ImageFormats/PGMLoader.h>
#include <LibMedia/ImageFormats/PNGLoader.h>
#include <LibMedia/ImageFormats/PPMLoader.h>
#include <LibMedia/ImageFormats/QOILoader.h>
#include <LibMedia/ImageFormats/TGALoader.h>
#include <LibMedia/ImageFormats/TIFFLoader.h>
#include <LibMedia/ImageFormats/TinyVGLoader.h>
#include <LibMedia/ImageFormats/WebPLoader.h>

namespace Media {

static OwnPtr<ImageDecoderPlugin> probe_and_sniff_for_appropriate_plugin(ReadonlyBytes bytes)
{
    struct ImagePluginInitializer {
        bool (*sniff)(ReadonlyBytes) = nullptr;
        ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> (*create)(ReadonlyBytes) = nullptr;
    };

    static constexpr ImagePluginInitializer s_initializers[] = {
        { BMPImageDecoderPlugin::sniff, BMPImageDecoderPlugin::create },
        { DDSImageDecoderPlugin::sniff, DDSImageDecoderPlugin::create },
        { GIFImageDecoderPlugin::sniff, GIFImageDecoderPlugin::create },
        { ICOImageDecoderPlugin::sniff, ICOImageDecoderPlugin::create },
        { ILBMImageDecoderPlugin::sniff, ILBMImageDecoderPlugin::create },
        { JPEGImageDecoderPlugin::sniff, JPEGImageDecoderPlugin::create },
        { JPEGXLImageDecoderPlugin::sniff, JPEGXLImageDecoderPlugin::create },
        { PBMImageDecoderPlugin::sniff, PBMImageDecoderPlugin::create },
        { PGMImageDecoderPlugin::sniff, PGMImageDecoderPlugin::create },
        { PNGImageDecoderPlugin::sniff, PNGImageDecoderPlugin::create },
        { PPMImageDecoderPlugin::sniff, PPMImageDecoderPlugin::create },
        { QOIImageDecoderPlugin::sniff, QOIImageDecoderPlugin::create },
        { TIFFImageDecoderPlugin::sniff, TIFFImageDecoderPlugin::create },
        { TinyVGImageDecoderPlugin::sniff, TinyVGImageDecoderPlugin::create },
        { WebPImageDecoderPlugin::sniff, WebPImageDecoderPlugin::create },
    };

    for (auto& plugin : s_initializers) {
        auto sniff_result = plugin.sniff(bytes);
        if (!sniff_result)
            continue;
        auto plugin_decoder = plugin.create(bytes);
        if (!plugin_decoder.is_error())
            return plugin_decoder.release_value();
    }
    return {};
}

static OwnPtr<ImageDecoderPlugin> probe_and_sniff_for_appropriate_plugin_with_known_mime_type(StringView mime_type, ReadonlyBytes bytes)
{
    struct ImagePluginWithMIMETypeInitializer {
        ErrorOr<bool> (*validate_before_create)(ReadonlyBytes) = nullptr;
        ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> (*create)(ReadonlyBytes) = nullptr;
        StringView mime_type;
    };

    static constexpr ImagePluginWithMIMETypeInitializer s_initializers_with_mime_type[] = {
        { TGAImageDecoderPlugin::validate_before_create, TGAImageDecoderPlugin::create, "image/x-targa"sv },
    };

    for (auto& plugin : s_initializers_with_mime_type) {
        if (plugin.mime_type != mime_type)
            continue;
        auto validation_result = plugin.validate_before_create(bytes).release_value_but_fixme_should_propagate_errors();
        if (!validation_result)
            continue;
        auto plugin_decoder = plugin.create(bytes);
        if (!plugin_decoder.is_error())
            return plugin_decoder.release_value();
    }
    return {};
}

RefPtr<ImageDecoder> ImageDecoder::try_create_for_raw_bytes(ReadonlyBytes bytes, Optional<DeprecatedString> mime_type)
{
    if (OwnPtr<ImageDecoderPlugin> plugin = probe_and_sniff_for_appropriate_plugin(bytes); plugin)
        return adopt_ref_if_nonnull(new (nothrow) ImageDecoder(plugin.release_nonnull()));

    if (mime_type.has_value()) {
        if (OwnPtr<ImageDecoderPlugin> plugin = probe_and_sniff_for_appropriate_plugin_with_known_mime_type(mime_type.value(), bytes); plugin)
            return adopt_ref_if_nonnull(new (nothrow) ImageDecoder(plugin.release_nonnull()));
    }

    return {};
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> ImageDecoder::load_scaled_bitmap(StringView path, int scale_factor, Optional<Gfx::IntSize> ideal_size)
{
    LexicalPath lexical_path { path };
    StringBuilder highdpi_icon_path;
    TRY(highdpi_icon_path.try_appendff("{}/{}-{}x.{}", lexical_path.dirname(), lexical_path.title(), scale_factor, lexical_path.extension()));

    auto highdpi_icon_string = highdpi_icon_path.string_view();
    auto file = TRY(Core::File::open(highdpi_icon_string, Core::File::OpenMode::Read));

    auto bitmap = TRY(load_from_file(move(file), highdpi_icon_string, ideal_size));
    if (bitmap->width() % scale_factor != 0 || bitmap->height() % scale_factor != 0)
        return Error::from_string_literal("ImageDecoder::load_from_file: HighDPI image size should be divisible by scale factor");
    bitmap->m_size.set_width(bitmap->width() / scale_factor);
    bitmap->m_size.set_height(bitmap->height() / scale_factor);
    bitmap->m_scale = scale_factor;
    return bitmap;
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> ImageDecoder::load_from_file(StringView path, int scale_factor, Optional<Gfx::IntSize> ideal_size)
{
    if (scale_factor > 1 && path.starts_with("/res/"sv)) {
        auto scaled_bitmap_or_error = load_scaled_bitmap(path, scale_factor, ideal_size);
        if (!scaled_bitmap_or_error.is_error())
            return scaled_bitmap_or_error.release_value();

        auto error = scaled_bitmap_or_error.release_error();
        if (!(error.is_syscall() && error.code() == ENOENT)) {
            dbgln("Couldn't load scaled bitmap: {}", error);
            dbgln("Trying base scale instead.");
        }
    }

    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
    return load_from_file(move(file), path, ideal_size);
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> ImageDecoder::load_from_file(NonnullOwnPtr<Core::File> file, StringView path, Optional<Gfx::IntSize> ideal_size)
{
    auto mapped_file = TRY(Core::MappedFile::map_from_file(move(file), path));
    auto mime_type = Core::guess_mime_type_based_on_filename(path);
    return load_from_bytes(mapped_file->bytes(), ideal_size, mime_type);
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> ImageDecoder::load_from_bytes(ReadonlyBytes bytes, Optional<Gfx::IntSize> ideal_size, Optional<DeprecatedString> mine_type)
{
    if (auto decoder = ImageDecoder::try_create_for_raw_bytes(bytes, mine_type)) {
        auto frame = TRY(decoder->frame(0, ideal_size));
        if (auto& bitmap = frame.image)
            return bitmap.release_nonnull();
    }

    return Error::from_string_literal("Gfx::Bitmap unable to load from file");
}

ImageDecoder::ImageDecoder(NonnullOwnPtr<ImageDecoderPlugin> plugin)
    : m_plugin(move(plugin))
{
}

}
