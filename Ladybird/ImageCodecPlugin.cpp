/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageCodecPlugin.h"
#ifdef AK_OS_ANDROID
#    include <Ladybird/Android/src/main/cpp/WebContentService.h>
#else
#    include "HelperProcess.h"
#endif
#include "Utilities.h"
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibImageDecoderClient/Client.h>

namespace Ladybird {

ImageCodecPlugin::~ImageCodecPlugin() = default;

Optional<Web::Platform::DecodedImage> ImageCodecPlugin::decode_image(ReadonlyBytes bytes)
{
    if (!m_client) {
#ifdef AK_OS_ANDROID
        m_client = MUST(bind_service<ImageDecoderClient::Client>(&bind_image_decoder_java));
#else
        auto candidate_image_decoder_paths = get_paths_for_helper_process("ImageDecoder"sv).release_value_but_fixme_should_propagate_errors();
        m_client = launch_image_decoder_process(candidate_image_decoder_paths).release_value_but_fixme_should_propagate_errors();
#endif
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
        decoded_image.frames.empend(move(frame.bitmap), frame.duration);
    }

    return decoded_image;
}

}
