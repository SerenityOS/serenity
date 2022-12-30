/*
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

enum class SilenceImageLoadingErrors {
    No,
    Yes
};

class BitmapCache {
public:
    // NOTE: This silences loading errors by default and will return nullptr instead
    //       This allows us to continue gracefully when for example the image is not available.
    //       Most widgets just don't draw missing images, while still working correctly otherwise.
    //       If writing to the cache or string allocation fails on the other hand,
    //       we are usually in a bad enough position to allow a crash, aka OOM
    static ErrorOr<RefPtr<Gfx::Bitmap>> load_bitmap(StringView path, SilenceImageLoadingErrors silence_errors = SilenceImageLoadingErrors::Yes);

private:
    static HashMap<String, RefPtr<Gfx::Bitmap>> s_cache;
};

}
