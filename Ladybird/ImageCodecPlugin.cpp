/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageCodecPlugin.h"
#include "HelperProcess.h"
#include "Utilities.h"
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibImageDecoderClient/Client.h>

namespace Ladybird {

ImageCodecPlugin::~ImageCodecPlugin() = default;

NonnullRefPtr<Core::Promise<Web::Platform::DecodedImage>> ImageCodecPlugin::decode_image(ReadonlyBytes bytes, Function<ErrorOr<void>(Web::Platform::DecodedImage&)> on_resolved, Function<void(Error&)> on_rejected)
{
    if (!m_client) {
        auto candidate_image_decoder_paths = get_paths_for_helper_process("ImageDecoder"sv).release_value_but_fixme_should_propagate_errors();
        m_client = launch_image_decoder_process(candidate_image_decoder_paths).release_value_but_fixme_should_propagate_errors();
        m_client->on_death = [&] {
            m_client = nullptr;
        };
    }

    auto promise = Core::Promise<Web::Platform::DecodedImage>::construct();
    if (on_resolved)
        promise->on_resolution = move(on_resolved);
    if (on_rejected)
        promise->on_rejection = move(on_rejected);

    auto image_decoder_promise = m_client->decode_image(
        bytes,
        [promise](ImageDecoderClient::DecodedImage& result) -> ErrorOr<void> {
            // FIXME: Remove this codec plugin and just use the ImageDecoderClient directly to avoid these copies
            Web::Platform::DecodedImage decoded_image;
            decoded_image.is_animated = result.is_animated;
            decoded_image.loop_count = result.loop_count;
            for (auto& frame : result.frames) {
                decoded_image.frames.empend(move(frame.bitmap), frame.duration);
            }
            promise->resolve(move(decoded_image));
            return {};
        },
        [promise](auto& error) {
            promise->reject(Error::copy(error));
        });

    return promise;
}

}
