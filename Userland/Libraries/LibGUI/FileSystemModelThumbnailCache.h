/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/ByteString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <LibGfx/Forward.h>
#include <LibImageDecoderClient/Client.h>
#include <LibThreading/BackgroundAction.h>
#include <LibThreading/MutexProtected.h>
#include <sys/stat.h>

namespace GUI {

static constexpr size_t DISK_THUMBNAIL_CACHE_MAX_SIZE = 10 * 1024 * 1024; // 10 MiB

class ThumbnailCache {
public:
    static ThumbnailCache& the();

    enum class FetchResult {
        Cached,         // Thumbnail ready; out_bitmap has been set.
        Error,          // A previous load attempt failed.
        Loading,        // Already loading asynchronously.
        StartedLoading, // Async load started; @on_loaded called on the main thread when done.
    };

    // Looks up or starts loading the thumbnail for @path.
    // On Cached, @out_bitmap is set and returned immediately.
    // On StartedLoading, an async load is started and @on_loaded is called on the main
    // thread when it finishes. The bitmap argument is null on failure.
    FetchResult fetch(ByteString const& path,
        RefPtr<Gfx::Bitmap>& out_bitmap,
        Function<void(RefPtr<Gfx::Bitmap>)> on_loaded);

private:
    ThumbnailCache() = default;

    ByteString disk_cache_dir();
    static ByteString cache_filename(StringView path, struct stat const& st);
    RefPtr<Gfx::Bitmap> load_from_disk(ByteString const& cache_path);
    void save_to_disk(ByteString const& cache_path, Gfx::Bitmap const&);
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> render(StringView path);

    Atomic<size_t> m_disk_cache_total_size { 0 };
    Atomic<bool> m_disk_cache_initialized { false };

    using BitmapBackgroundAction = Threading::BackgroundAction<NonnullRefPtr<Gfx::Bitmap>>;

    struct CacheData {
        HashMap<ByteString, RefPtr<Gfx::Bitmap>> thumbnails;
        HashMap<ByteString, NonnullRefPtr<BitmapBackgroundAction>> loading;
    };

    Threading::MutexProtected<CacheData> m_cache;
    Threading::MutexProtected<RefPtr<ImageDecoderClient::Client>> m_image_decoder_client;
};

}
