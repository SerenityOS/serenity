/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Cache.h"
#include "PreviewProvider.h"
#include <AK/String.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Promise.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGfx/QOIWriter.h>
#include <LibImageDecoderClient/Client.h>
#include <sys/stat.h>
#include <unistd.h>

namespace PreviewServer {

static Singleton<Cache> s_the;

Cache& Cache::the()
{
    if (!s_the->m_cache_requests.is_valid()) {
        auto maybe_cache_requests = Core::SharedSingleProducerCircularQueue<String, 128>::create();
        if (maybe_cache_requests.is_error())
            dbgln("Error while creating cache request queue: {}, PreviewServer will terminate shortly", maybe_cache_requests.release_error());
        else
            s_the->m_cache_requests = maybe_cache_requests.release_value();
    }
    return s_the;
}

Cache::~Cache()
{
    if (m_generator_thread && m_generator_thread->needs_to_be_joined()) {
        if (m_generator_event_loop)
            m_generator_event_loop->quit(0);
        (void)m_generator_thread->join();
    }
}

ErrorOr<Hash> Cache::hash_for(String const& file_name)
{
    Crypto::Hash::SHA512 hash;
    hash.update(file_name);
    auto stat = TRY(Core::System::stat(file_name));
    auto modification_timestamp = stat.st_mtim.tv_nsec;
    hash.update(Bytes { &modification_timestamp, sizeof(modification_timestamp) });
    return hash.digest();
}

bool Cache::excluded_from_cache(String const& file_name)
{
    // A non-existent file should error out due to other reasons.
    if (!FileSystem::exists(file_name))
        return false;

    // Don't cache things in our cache directory; that would lead to recursive cache creation!
    if (LexicalPath { LexicalPath::canonicalized_path(file_name.to_deprecated_string()) }.is_child_of(s_cache_directory))
        return true;

    auto directory = LexicalPath::dirname(file_name.to_deprecated_string());
    auto nomedia_file = LexicalPath::join(directory, ".nomedia"sv);
    return FileSystem::exists(nomedia_file.string());
}

ErrorOr<void> Cache::request_preview(String const& file_name, Hash file_hash, NonnullRefPtr<CachePromise> client_promise)
{
    if (excluded_from_cache(file_name))
        return EINVAL;

    TRY(m_client_promises.with_locked([&](auto& client_promises) -> ErrorOr<void> {
        TRY(client_promises.try_ensure(file_hash, []() -> Vector<NonnullRefPtr<CachePromise>> { return {}; }));
        TRY(client_promises.get(file_hash)->try_append(move(client_promise)));
        return {};
    }));

    // Enqueue before possibly starting the thread, so the thread immediately has something to work with.
    MUST(m_cache_requests.blocking_enqueue(file_name, [] { usleep(1000); }));

    if (m_generator_event_loop)
        m_generator_event_loop->wake();
    create_generator_thread_if_necessary();

    return {};
}

void Cache::create_generator_thread_if_necessary()
{
    if (!m_generator_thread) {
        // Grab the event loop from the main thread, since the second thread sends promise callbacks to it.
        auto maybe_thread = Threading::Thread::try_create([this, &main_event_loop = Core::EventLoop::current()] {
            this->generator_main(main_event_loop);
            return static_cast<intptr_t>(0);
        },
            "Preview Generator"sv);
        if (maybe_thread.is_error()) {
            dbgln("Could not create generator thread; preview creation will be unreliable.");
            return;
        }
        m_generator_thread = maybe_thread.release_value();
        m_generator_thread->start();
    }
}

void Cache::generator_main(Core::EventLoop& main_event_loop)
{
    Core::EventLoop generator_thread_event_loop;
    m_generator_event_loop = &generator_thread_event_loop;

    while (!main_event_loop.was_exit_requested() && !generator_thread_event_loop.was_exit_requested()) {
        // If we can dequeue already, don't wait for an event.
        if (m_cache_requests.can_dequeue())
            generator_thread_event_loop.pump(Core::EventLoop::WaitMode::PollForEvents);
        // We get notified when there is a request to the cache.
        else
            generator_thread_event_loop.pump(Core::EventLoop::WaitMode::WaitForEvents);

        auto maybe_request = m_cache_requests.dequeue();
        if (maybe_request.is_error())
            continue;

        auto requested_file = maybe_request.release_value();
        auto file_hash_or_error = hash_for(requested_file);
        if (file_hash_or_error.is_error()) {
            dbgln_if(PREVIEW_SERVER_DEBUG, "Couldn't hash {}, ignoring request.", requested_file);
            continue;
        }
        dbgln_if(PREVIEW_SERVER_DEBUG, "Processing preview request for file {}", requested_file);

        auto file_hash = file_hash_or_error.release_value();
        // Check disk cache.
        if (auto disk_cache = load_preview_from_disk_cache(file_hash); !disk_cache.is_error()) {
            auto preview = disk_cache.release_value();
            ValidCacheEntry cache_entry = {
                preview,
                LexicalPath { requested_file.to_deprecated_string() },
            };
            dbgln_if(PREVIEW_SERVER_DEBUG, "Found preview for file {} cached on disk: {}", requested_file, file_hash);
            enqueue_new_preview(file_hash, move(cache_entry), main_event_loop);
            continue;
        }

        // Render preview.
        auto rendered_preview = PreviewProvider::generate_preview_with_any_provider(requested_file);
        dbgln_if(PREVIEW_SERVER_DEBUG, "Generated new preview for file {} ({})", requested_file, file_hash);
        enqueue_new_preview(file_hash, CacheEntry { rendered_preview }, main_event_loop);

        // Write preview to disk cache, since it doesn't exist there yet.
        if (!rendered_preview.entry.is_error()) {
            auto disk_write_result = write_preview_to_disk_cache(file_hash, rendered_preview.entry.release_value().preview);
            if (disk_write_result.is_error())
                dbgln("Writing preview {} to disk failed: {}", file_hash, disk_write_result.release_error());
        }
    }
}

void Cache::enqueue_new_preview(Hash file_hash, CacheEntry preview, Core::EventLoop& main_event_loop)
{
    // Call all promises associated with that hash, so clients can receive their data.
    m_client_promises.with_locked([&](auto& client_promises) {
        if (auto maybe_promises_for_file = client_promises.get(file_hash); maybe_promises_for_file.has_value()) {
            auto promises_for_file = maybe_promises_for_file.release_value();
            for (auto& client_promise : promises_for_file) {
                // These functions are run on the main thread event loop, not the generator thread event loop,
                // since the IPC connection for the clients runs on the main thread.
                main_event_loop.deferred_invoke([client_promise = client_promise, preview = CacheEntry { preview }]() mutable {
                    // Clients should not give us fallible promises.
                    MUST(client_promise->resolve(move(preview)));
                });
                main_event_loop.wake();
            }
            client_promises.remove(file_hash);
        }
    });
}

ErrorOr<Gfx::ShareableBitmap> Cache::load_preview_from_disk_cache(Hash file_hash)
{
    auto cache_file_path = TRY(path_for_hash(file_hash));
    if (!FileSystem::exists(cache_file_path))
        return Error::from_string_view("File preview not cached on disk"sv);

    auto decoder_client = TRY(PreviewProvider::image_decoder_client());
    auto image_file = TRY(Core::File::open(cache_file_path, Core::File::OpenMode::Read));
    auto image_data = TRY(image_file->read_until_eof());

    auto delete_bad_cache = [&] {
        image_file->close();
        auto remove_result = FileSystem::remove(cache_file_path, FileSystem::RecursionMode::Disallowed);
        if (remove_result.is_error())
            dbgln("Could not remove bad preview cache file {}: {}", cache_file_path, remove_result.release_error());
    };
    auto maybe_preview_image = decoder_client->decode_image(image_data);
    if (!maybe_preview_image.has_value()) {
        delete_bad_cache();
        return Error::from_string_view("Could not load cached preview"sv);
    }
    auto preview = maybe_preview_image->frames[0].bitmap;
    if (preview->height() != preview_size || preview->width() != preview_size) {
        delete_bad_cache();
        return Error::from_string_view("Cached preview has incorrect size"sv);
    }
    auto maybe_shareable_preview = preview->to_shareable_bitmap();
    if (!maybe_shareable_preview.is_valid())
        return ENOMEM;
    return maybe_shareable_preview;
}

ErrorOr<String> Cache::path_for_hash(Hash file_hash)
{
    auto cache_file_name = TRY(String::formatted("{}.qoi", file_hash));
    return String::from_deprecated_string(LexicalPath::canonicalized_path(LexicalPath::join(s_cache_directory.string(), cache_file_name).string()));
}

ErrorOr<void> Cache::write_preview_to_disk_cache(Hash file_hash, Gfx::ShareableBitmap preview)
{
    if (!FileSystem::exists(s_cache_directory.string())) {
        // Also create cache directory if absent.
        if (!FileSystem::exists(s_cache_directory.parent().string()))
            TRY(Core::System::mkdir(s_cache_directory.parent().string(), S_IRWXU | S_IRGRP | S_IXGRP));

        TRY(Core::System::mkdir(s_cache_directory.string(), S_IRWXU | S_IRGRP | S_IXGRP));
    }

    auto cache_file_path = TRY(path_for_hash(file_hash));
    auto image_file = TRY(Core::File::open(cache_file_path, Core::File::OpenMode::Write | Core::File::OpenMode::Truncate));

    if (preview.bitmap()->width() != preview_size || preview.bitmap()->height() != preview_size)
        return Error::from_string_view("Preview to cache has incorrect size"sv);

    auto preview_data = TRY(Gfx::QOIWriter::encode(*preview.bitmap()));
    TRY(image_file->write_until_depleted(preview_data));

    return {};
}

}
