/*
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibGUI/BitmapCache.h>

namespace GUI {

HashMap<String, RefPtr<Gfx::Bitmap>> BitmapCache::s_cache {};

ErrorOr<RefPtr<Gfx::Bitmap>> BitmapCache::load_bitmap(StringView path, SilenceImageLoadingErrors silence_errors)
{

    if (auto maybe_bitmap = s_cache.find(path); maybe_bitmap != s_cache.end()) {
        // Don't return nullptr for a previously failed load, if the caller wants us
        // to not silence errors, instead we will try to reload the image
        if (silence_errors == SilenceImageLoadingErrors::Yes || maybe_bitmap->value)
            return maybe_bitmap->value;
    }

    auto bitmap_or_error = Gfx::Bitmap::try_load_from_file(path);
    if (bitmap_or_error.is_error()) {
        if (silence_errors == SilenceImageLoadingErrors::No)
            return bitmap_or_error.release_error();

        dbgln("Failed to load Bitmap from {}: {}", path, bitmap_or_error.error());
        TRY(s_cache.try_set(
            TRY(String::from_utf8(path)), nullptr));
        return bitmap_or_error.release_error();
    }
    TRY(s_cache.try_set(
        TRY(String::from_utf8(path)), bitmap_or_error.value()));

    return bitmap_or_error.release_value();
}

}
