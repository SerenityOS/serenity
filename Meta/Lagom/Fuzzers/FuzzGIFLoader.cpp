/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/String.h>
#include <LibGfx/GIFLoader.h>
#include <stddef.h>
#include <stdint.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size)
{
    Gfx::GIFImageDecoderPlugin gif_decoder(data, size);
    auto bitmap_or_error = gif_decoder.frame(0);
    if (!bitmap_or_error.is_error()) {
        auto const& bitmap = bitmap_or_error.value().image;
        // Looks like a valid GIF. Try to load the other frames:
        dbgln_if(GIF_DEBUG, "bitmap size: {}", bitmap->size());
        dbgln_if(GIF_DEBUG, "codec size: {}", gif_decoder.size());
        dbgln_if(GIF_DEBUG, "is_sniff: {}", gif_decoder.sniff());
        dbgln_if(GIF_DEBUG, "is_animated: {}", gif_decoder.is_animated());
        dbgln_if(GIF_DEBUG, "loop_count: {}", gif_decoder.loop_count());
        dbgln_if(GIF_DEBUG, "frame_count: {}", gif_decoder.frame_count());
        for (size_t i = 0; i < gif_decoder.frame_count(); ++i) {
            auto ifd = gif_decoder.frame(i).release_value_but_fixme_should_propagate_errors();
            dbgln_if(GIF_DEBUG, "frame #{} size: {}", i, ifd.image->size());
            dbgln_if(GIF_DEBUG, "frame #{} duration: {}", i, ifd.duration);
        }
        dbgln_if(GIF_DEBUG, "Done.");
    }

    return 0;
}
