/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Platform/ImageCodecPlugin.h>

namespace Web::Platform {

static ImageCodecPlugin* s_the;

ImageCodecPlugin::~ImageCodecPlugin() = default;

ImageCodecPlugin& ImageCodecPlugin::the()
{
    VERIFY(s_the);
    return *s_the;
}

void ImageCodecPlugin::install(ImageCodecPlugin& plugin)
{
    VERIFY(!s_the);
    s_the = &plugin;
}

Optional<DecodedImage> ImageCodecPlugin::decode_image(ReadonlyBytes encoded_data)
{
    auto promise = decode_image(encoded_data, {}, {});
    auto result = promise->await();
    if (result.is_error())
        return {};
    return result.release_value();
}

}
