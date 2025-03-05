/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <ImageDecoder/ConnectionFromClient.h>
#include <ImageDecoder/ImageDecoderClientEndpoint.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ImageFormats/TIFFMetadata.h>

namespace ImageDecoder {

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionFromClient<ImageDecoderClientEndpoint, ImageDecoderServerEndpoint>(*this, move(socket), 1)
{
}

void ConnectionFromClient::die()
{
    for (auto& [_, job] : m_pending_jobs) {
        job->cancel();
    }
    m_pending_jobs.clear();

    Threading::quit_background_thread();
    Core::EventLoop::current().quit(0);
}

namespace {

void decode_image_to_bitmaps_and_durations_with_decoder(Gfx::ImageDecoder const& decoder, Optional<Gfx::IntSize> ideal_size, Vector<Optional<NonnullRefPtr<Gfx::Bitmap>>>& bitmaps, Vector<u32>& durations)
{
    for (size_t i = 0; i < decoder.frame_count(); ++i) {
        auto frame_or_error = decoder.frame(i, ideal_size);
        if (frame_or_error.is_error()) {
            bitmaps.append({});
            durations.append(0);
        } else {
            auto frame = frame_or_error.release_value();
            bitmaps.append(frame.image.release_nonnull());
            durations.append(frame.duration);
        }
    }
}

ErrorOr<ConnectionFromClient::DecodeResult> decode_image_to_details(Core::AnonymousBuffer const& encoded_buffer, Optional<Gfx::IntSize> ideal_size, Optional<ByteString> const& known_mime_type)
{
    auto decoder = TRY(Gfx::ImageDecoder::try_create_for_raw_bytes(ReadonlyBytes { encoded_buffer.data<u8>(), encoded_buffer.size() }, known_mime_type));

    if (!decoder)
        return Error::from_string_literal("Could not find suitable image decoder plugin for data");

    if (!decoder->frame_count())
        return Error::from_string_literal("Could not decode image from encoded data");

    ConnectionFromClient::DecodeResult result;
    result.is_animated = decoder->is_animated();
    result.loop_count = decoder->loop_count();

    Vector<Optional<NonnullRefPtr<Gfx::Bitmap>>> bitmaps;

    if (auto maybe_metadata = decoder->metadata(); maybe_metadata.has_value() && is<Gfx::ExifMetadata>(*maybe_metadata)) {
        auto const& exif = static_cast<Gfx::ExifMetadata const&>(maybe_metadata.value());
        if (exif.x_resolution().has_value() && exif.y_resolution().has_value()) {
            auto const x_resolution = exif.x_resolution()->as_double();
            auto const y_resolution = exif.y_resolution()->as_double();
            if (x_resolution < y_resolution)
                result.scale.set_y(x_resolution / y_resolution);
            else
                result.scale.set_x(y_resolution / x_resolution);
        }
    }

    decode_image_to_bitmaps_and_durations_with_decoder(*decoder, move(ideal_size), bitmaps, result.durations);

    auto no_frame_available = !any_of(bitmaps, [](Optional<NonnullRefPtr<Gfx::Bitmap>> bitmap) {
        return bitmap.has_value();
    });

    if (bitmaps.is_empty() || no_frame_available)
        return Error::from_string_literal("Could not decode image");

    result.bitmaps = { move(bitmaps) };

    return result;
}

}

NonnullRefPtr<ConnectionFromClient::Job> ConnectionFromClient::make_decode_image_job(i64 image_id, Core::AnonymousBuffer encoded_buffer, Optional<Gfx::IntSize> ideal_size, Optional<ByteString> mime_type)
{
    return Job::construct(
        [encoded_buffer = move(encoded_buffer), ideal_size = move(ideal_size), mime_type = move(mime_type)](auto&) -> ErrorOr<DecodeResult> {
            return TRY(decode_image_to_details(encoded_buffer, ideal_size, mime_type));
        },
        [strong_this = NonnullRefPtr(*this), image_id](DecodeResult result) -> ErrorOr<void> {
            strong_this->async_did_decode_image(image_id, result.is_animated, result.loop_count, move(result.bitmaps), move(result.durations), result.scale);
            strong_this->m_pending_jobs.remove(image_id);
            return {};
        },
        [strong_this = NonnullRefPtr(*this), image_id](Error error) -> void {
            if (strong_this->is_open())
                strong_this->async_did_fail_to_decode_image(image_id, MUST(String::formatted("Decoding failed: {}", error)));
            strong_this->m_pending_jobs.remove(image_id);
        });
}

Messages::ImageDecoderServer::DecodeImageResponse ConnectionFromClient::decode_image(Core::AnonymousBuffer const& encoded_buffer, Optional<Gfx::IntSize> const& ideal_size, Optional<ByteString> const& mime_type)
{
    auto image_id = m_next_image_id++;

    if (!encoded_buffer.is_valid()) {
        dbgln_if(IMAGE_DECODER_DEBUG, "Encoded data is invalid");
        async_did_fail_to_decode_image(image_id, "Encoded data is invalid"_string);
        return image_id;
    }

    m_pending_jobs.set(image_id, make_decode_image_job(image_id, encoded_buffer, ideal_size, mime_type));

    return image_id;
}

void ConnectionFromClient::cancel_decoding(i64 image_id)
{
    if (auto job = m_pending_jobs.take(image_id); job.has_value()) {
        job.value()->cancel();
    }
}

}
