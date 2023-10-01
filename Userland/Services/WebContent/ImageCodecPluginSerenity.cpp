/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageCodecPluginSerenity.h"
#include <LibImageDecoderClient/Client.h>

namespace WebContent {

ImageCodecPluginSerenity::ImageCodecPluginSerenity() = default;
ImageCodecPluginSerenity::~ImageCodecPluginSerenity() = default;

Optional<Web::Platform::DecodedImage> ImageCodecPluginSerenity::decode_image(ReadonlyBytes bytes)
{
    if (!m_client) {
        m_client = ImageDecoderClient::Client::try_create().release_value_but_fixme_should_propagate_errors();
        m_client->on_death = [&] {
            m_client = nullptr;
        };
    }

    auto result_or_empty = m_client->decode_image(bytes);
    if (!result_or_empty.has_value())
        return {};
    auto result = result_or_empty.release_value();

    Web::Platform::DecodedImage decoded_image;
    decoded_image.is_animated = result.is_animated;
    decoded_image.loop_count = result.loop_count;
    for (auto const& frame : result.frames) {
        decoded_image.frames.empend(frame.bitmap, frame.duration);
    }

    return decoded_image;
}

}
