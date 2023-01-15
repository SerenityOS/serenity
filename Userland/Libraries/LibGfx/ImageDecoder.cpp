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

static OwnPtr<ImageDecoderPlugin> probe_and_sniff_for_appropriate_plugin(ReadonlyBytes bytes)
{
    auto* data = bytes.data();
    auto size = bytes.size();
    OwnPtr<ImageDecoderPlugin> plugin;

    plugin = make<PNGImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    plugin = make<GIFImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    plugin = make<BMPImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    plugin = make<PBMImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    plugin = make<PGMImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    plugin = make<PPMImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    plugin = make<ICOImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    plugin = make<JPGImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    plugin = make<DDSImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    plugin = make<QOIImageDecoderPlugin>(data, size);
    if (plugin->sniff())
        return plugin;

    return {};
}

RefPtr<ImageDecoder> ImageDecoder::try_create_for_raw_bytes(ReadonlyBytes bytes)
{
    OwnPtr<ImageDecoderPlugin> plugin = probe_and_sniff_for_appropriate_plugin(bytes);
    if (!plugin)
        return {};
    return adopt_ref_if_nonnull(new (nothrow) ImageDecoder(plugin.release_nonnull()));
}

static OwnPtr<ImageDecoderPlugin> probe_and_sniff_for_appropriate_plugin_with_known_path(StringView path, ReadonlyBytes bytes)
{
    LexicalPath lexical_mapped_file_path(path);
    auto* data = bytes.data();
    auto size = bytes.size();
    OwnPtr<ImageDecoderPlugin> plugin;
    if (lexical_mapped_file_path.extension() == "tga"sv) {
        plugin = make<TGAImageDecoderPlugin>(data, size);
        if (plugin->sniff())
            return plugin;
    }
    return {};
}

RefPtr<ImageDecoder> ImageDecoder::try_create_for_raw_bytes_with_known_path(StringView path, ReadonlyBytes bytes)
{
    OwnPtr<ImageDecoderPlugin> plugin = probe_and_sniff_for_appropriate_plugin_with_known_path(path, bytes);
    if (!plugin)
        return try_create_for_raw_bytes(bytes);
    return adopt_ref_if_nonnull(new (nothrow) ImageDecoder(plugin.release_nonnull()));
}

ImageDecoder::ImageDecoder(NonnullOwnPtr<ImageDecoderPlugin> plugin)
    : m_plugin(move(plugin))
{
}

}
