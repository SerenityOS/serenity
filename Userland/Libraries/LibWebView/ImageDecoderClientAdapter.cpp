/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibImageDecoderClient/Client.h>
#include <LibWebView/ImageDecoderClientAdapter.h>

namespace WebView {

NonnullRefPtr<ImageDecoderClientAdapter> ImageDecoderClientAdapter::create()
{
    return adopt_ref(*new ImageDecoderClientAdapter());
}

Optional<Web::ImageDecoding::DecodedImage> ImageDecoderClientAdapter::decode_image(ReadonlyBytes bytes)
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

    Web::ImageDecoding::DecodedImage decoded_image;
    decoded_image.is_animated = result.is_animated;
    decoded_image.loop_count = result.loop_count;
    for (auto const& frame : result.frames) {
        decoded_image.frames.empend(move(frame.bitmap), frame.duration);
    }

    return decoded_image;
}

}
