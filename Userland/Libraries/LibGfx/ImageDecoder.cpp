/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibGfx/BMPLoader.h>
#include <LibGfx/DDSLoader.h>
#include <LibGfx/GIFLoader.h>
#include <LibGfx/ICOLoader.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/JPGLoader.h>
#include <LibGfx/PBMLoader.h>
#include <LibGfx/PGMLoader.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/PPMLoader.h>
#include <LibGfx/QOILoader.h>
#include <LibGfx/TGALoader.h>

namespace Gfx {

struct ImagePluginInitializer {
    ErrorOr<bool> (*sniff)(ReadonlyBytes) = nullptr;
    ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> (*create)(ReadonlyBytes) = nullptr;
};

static constexpr ImagePluginInitializer s_initializers[] = {
    { PNGImageDecoderPlugin::sniff, PNGImageDecoderPlugin::create },
    { GIFImageDecoderPlugin::sniff, GIFImageDecoderPlugin::create },
    { BMPImageDecoderPlugin::sniff, BMPImageDecoderPlugin::create },
    { PBMImageDecoderPlugin::sniff, PBMImageDecoderPlugin::create },
    { PGMImageDecoderPlugin::sniff, PGMImageDecoderPlugin::create },
    { PPMImageDecoderPlugin::sniff, PPMImageDecoderPlugin::create },
    { ICOImageDecoderPlugin::sniff, ICOImageDecoderPlugin::create },
    { JPGImageDecoderPlugin::sniff, JPGImageDecoderPlugin::create },
    { DDSImageDecoderPlugin::sniff, DDSImageDecoderPlugin::create },
    { QOIImageDecoderPlugin::sniff, QOIImageDecoderPlugin::create },
};

struct ImagePluginWithMIMETypeInitializer {
    ErrorOr<bool> (*validate_before_create)(ReadonlyBytes) = nullptr;
    ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> (*create)(ReadonlyBytes) = nullptr;
    StringView mime_type;
};

static constexpr ImagePluginWithMIMETypeInitializer s_initializers_with_mime_type[] = {
    { TGAImageDecoderPlugin::validate_before_create, TGAImageDecoderPlugin::create, "image/x-targa"sv },
};

static OwnPtr<ImageDecoderPlugin> probe_and_sniff_for_appropriate_plugin(ReadonlyBytes bytes)
{
    for (auto& plugin : s_initializers) {
        auto sniff_result = plugin.sniff(bytes).release_value_but_fixme_should_propagate_errors();
        if (!sniff_result)
            continue;
        auto plugin_decoder = plugin.create(bytes).release_value_but_fixme_should_propagate_errors();
        if (plugin_decoder->initialize())
            return plugin_decoder;
    }
    return {};
}

static OwnPtr<ImageDecoderPlugin> probe_and_sniff_for_appropriate_plugin_with_known_mime_type(StringView mime_type, ReadonlyBytes bytes)
{
    for (auto& plugin : s_initializers_with_mime_type) {
        if (plugin.mime_type != mime_type)
            continue;
        auto validation_result = plugin.validate_before_create(bytes).release_value_but_fixme_should_propagate_errors();
        if (!validation_result)
            continue;
        auto plugin_decoder = plugin.create(bytes).release_value_but_fixme_should_propagate_errors();
        if (plugin_decoder->initialize())
            return plugin_decoder;
    }
    return {};
}

RefPtr<ImageDecoder> ImageDecoder::try_create_for_raw_bytes(ReadonlyBytes bytes, Optional<DeprecatedString> mime_type)
{
    OwnPtr<ImageDecoderPlugin> plugin = probe_and_sniff_for_appropriate_plugin(bytes);
    if (!plugin) {
        if (mime_type.has_value()) {
            plugin = probe_and_sniff_for_appropriate_plugin_with_known_mime_type(mime_type.value(), bytes);
            if (!plugin)
                return {};
        } else {
            return {};
        }
    }
    return adopt_ref_if_nonnull(new (nothrow) ImageDecoder(plugin.release_nonnull()));
}

ImageDecoder::ImageDecoder(NonnullOwnPtr<ImageDecoderPlugin> plugin)
    : m_plugin(move(plugin))
{
}

}
