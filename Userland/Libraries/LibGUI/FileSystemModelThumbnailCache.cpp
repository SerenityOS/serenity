/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibCore/DirIterator.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibCore/StandardPaths.h>
#include <LibCrypto/Hash/MD5.h>
#include <LibGUI/FileSystemModelThumbnailCache.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/BMPWriter.h>
#include <LibImageDecoderClient/Client.h>
#include <sys/stat.h>
#include <unistd.h>
#include <utime.h>

namespace GUI {

ThumbnailCache& ThumbnailCache::the()
{
    static ThumbnailCache instance;
    return instance;
}

ByteString ThumbnailCache::disk_cache_dir()
{
    auto cache_base = Core::StandardPaths::cache_directory();
    auto parent = ByteString::formatted("{}/FileManager", cache_base);
    auto dir = ByteString::formatted("{}/FileManager/thumbnails", cache_base);
    (void)mkdir(cache_base.characters(), 0755);
    (void)mkdir(parent.characters(), 0755);
    (void)mkdir(dir.characters(), 0755);

    // Scan to populate m_disk_cache_total_size
    bool expected = false;
    if (m_disk_cache_initialized.compare_exchange_strong(expected, true, AK::MemoryOrder::memory_order_acq_rel)) {
        size_t total = 0;
        Core::DirIterator it(dir, Core::DirIterator::SkipParentAndBaseDir);
        while (it.has_next()) {
            auto full = it.next_full_path();
            struct stat st;
            if (stat(full.characters(), &st) == 0)
                total += st.st_size;
        }
        m_disk_cache_total_size.store(total, AK::MemoryOrder::memory_order_relaxed);
    }
    return dir;
}

ByteString ThumbnailCache::cache_filename(StringView path, struct stat const& st)
{
    // Hash: path + mtime + size
    auto key = ByteString::formatted("{}:{}:{}", path, st.st_mtime, st.st_size);
    auto digest = Crypto::Hash::MD5::hash(reinterpret_cast<u8 const*>(key.characters()), key.length());
    StringBuilder hex;
    for (size_t i = 0; i < digest.data_length(); ++i)
        hex.appendff("{:02x}", digest.data[i]);
    return ByteString::formatted("{}.bmp", hex.to_byte_string());
}

RefPtr<Gfx::Bitmap> ThumbnailCache::load_from_disk(ByteString const& cache_path)
{
    auto bitmap_or_error = Gfx::Bitmap::load_from_file(cache_path);
    if (bitmap_or_error.is_error())
        return nullptr;
    return bitmap_or_error.release_value();
}

void ThumbnailCache::save_to_disk(ByteString const& cache_path, Gfx::Bitmap const& bitmap)
{
    auto encoded = Gfx::BMPWriter::encode(bitmap);
    if (encoded.is_error())
        return;

    auto file = Core::File::open(cache_path, Core::File::OpenMode::Write);
    if (file.is_error())
        return;
    (void)file.value()->write_until_depleted(encoded.value());

    size_t written = encoded.value().size();
    auto new_total = m_disk_cache_total_size.fetch_add(written, AK::MemoryOrder::memory_order_relaxed) + written;

    if (new_total <= DISK_THUMBNAIL_CACHE_MAX_SIZE)
        return;

    // Remove oldest thumbnails until we're under the limit again
    struct Entry {
        ByteString path;
        time_t mtime;
        off_t size;
    };
    Vector<Entry> entries;
    Core::DirIterator it(disk_cache_dir(), Core::DirIterator::SkipParentAndBaseDir);
    while (it.has_next()) {
        auto full = it.next_full_path();
        struct stat st;
        if (stat(full.characters(), &st) == 0)
            entries.append({ full, st.st_mtime, st.st_size });
    }
    quick_sort(entries, [](auto const& a, auto const& b) { return a.mtime < b.mtime; });

    size_t current = m_disk_cache_total_size.load(AK::MemoryOrder::memory_order_relaxed);
    for (auto const& entry : entries) {
        if (current <= DISK_THUMBNAIL_CACHE_MAX_SIZE)
            break;
        if (unlink(entry.path.characters()) == 0) {
            current -= entry.size;
            m_disk_cache_total_size.fetch_sub(entry.size, AK::MemoryOrder::memory_order_relaxed);
        }
    }
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> ThumbnailCache::render(StringView path)
{
    Core::EventLoop event_loop;
    Gfx::IntSize const thumbnail_size { 32, 32 };

    auto file = TRY(Core::MappedFile::map(path));
    auto decoded_image = TRY(m_image_decoder_client.with_locked([=, this, &file](auto& maybe_client) -> ErrorOr<Optional<ImageDecoderClient::DecodedImage>> {
        if (!maybe_client) {
            maybe_client = TRY(ImageDecoderClient::Client::try_create());
            maybe_client->on_death = [this]() {
                m_image_decoder_client.with_locked([](auto& client) {
                    client = nullptr;
                });
            };
        }

        auto mime_type = Core::guess_mime_type_based_on_filename(path);

        // FIXME: Refactor thumbnail rendering to be more async-aware. Possibly return this promise to the caller.
        auto decoded_image = TRY(maybe_client->decode_image(file->bytes(), {}, {}, thumbnail_size, mime_type)->await());

        return decoded_image;
    }));

    auto bitmap = decoded_image.value().frames[0].bitmap;

    auto thumbnail = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, thumbnail_size));

    double scale = min(thumbnail_size.width() / (double)bitmap->width(), thumbnail_size.height() / (double)bitmap->height());
    auto destination = Gfx::IntRect(0, 0, (int)(bitmap->width() * scale), (int)(bitmap->height() * scale)).centered_within(thumbnail->rect());

    Painter painter(thumbnail);
    painter.draw_scaled_bitmap(destination, *bitmap, bitmap->rect(), 1.f, Gfx::ScalingMode::BoxSampling);
    return thumbnail;
}

// Shares on_loaded between the async completion and error handlers.
struct CallbackHolder : RefCounted<CallbackHolder> {
    explicit CallbackHolder(Function<void(RefPtr<Gfx::Bitmap>)> callback)
        : fn(move(callback))
    {
    }
    Function<void(RefPtr<Gfx::Bitmap>)> fn;
};

ThumbnailCache::FetchResult ThumbnailCache::fetch(
    ByteString const& path,
    RefPtr<Gfx::Bitmap>& out_bitmap,
    Function<void(RefPtr<Gfx::Bitmap>)> on_loaded)
{
    // Avoid caching files inside the cache directory itself, because that would cause infinite recursion
    auto cache_dir = disk_cache_dir();
    if (path.starts_with(cache_dir))
        return FetchResult::Error;

    // Compute the disk cache path
    struct stat img_st {};
    ByteString disk_cache_path;
    if (stat(path.characters(), &img_st) == 0)
        disk_cache_path = ByteString::formatted("{}/{}", cache_dir, cache_filename(path, img_st));

    // Check the in-memory cache first
    auto result = m_cache.with_locked([&](auto& cache) {
        auto it = cache.thumbnails.find(path);
        if (it != cache.thumbnails.end()) {
            out_bitmap = (*it).value;
            return out_bitmap ? FetchResult::Cached : FetchResult::Error;
        }
        if (cache.loading.contains(path))
            return FetchResult::Loading;
        return FetchResult::StartedLoading;
    });

    if (result != FetchResult::StartedLoading)
        return result;

    // Start an async background load, sharing on_loaded across the success and error paths
    auto holder = adopt_ref(*new CallbackHolder(move(on_loaded)));
    auto holder_for_complete = holder;

    auto action = [this, path, disk_cache_path](auto&) -> ErrorOr<NonnullRefPtr<Gfx::Bitmap>> {
        if (auto cached = load_from_disk(disk_cache_path))
            return cached.release_nonnull();
        auto thumbnail = TRY(render(path));
        save_to_disk(disk_cache_path, thumbnail);
        return thumbnail;
    };

    auto complete = [this, path, holder = move(holder_for_complete)](NonnullRefPtr<Gfx::Bitmap> thumbnail) -> ErrorOr<void> {
        m_cache.with_locked([&](auto& cache) {
            cache.thumbnails.set(path, thumbnail);
            cache.loading.remove(path);
        });
        holder->fn(move(thumbnail));
        return {};
    };

    auto error = [this, path, holder = move(holder)](Error err) {
        // Defer the cache update: removing the BackgroundAction from its own completion
        // handler would destroy its last reference inside itself, which is prohibited.
        Core::EventLoop::current().deferred_invoke([this, path, err = Error::copy(err), holder = move(holder)]() mutable {
            m_cache.with_locked([&](auto& cache) {
                if (err != Error::from_errno(ECANCELED)) {
                    cache.thumbnails.set(path, nullptr);
                    dbgln("Failed to load thumbnail for {}: {}", path, err);
                }
                cache.loading.remove(path);
            });
            holder->fn(nullptr);
        });
    };

    m_cache.with_locked([&](auto& cache) {
        cache.loading.set(path, BitmapBackgroundAction::construct(move(action), move(complete), move(error)));
    });

    return FetchResult::StartedLoading;
}

}
