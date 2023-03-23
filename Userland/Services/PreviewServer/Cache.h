/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/LexicalPath.h>
#include <AK/Singleton.h>
#include <LibCore/EventLoop.h>
#include <LibCore/SharedCircularQueue.h>
#include <LibCore/StandardPaths.h>
#include <LibCrypto/Hash/SHA2.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibThreading/ConditionVariable.h>
#include <LibThreading/Thread.h>

namespace PreviewServer {

using Hash = Crypto::Hash::SHA512::DigestType;

}

namespace AK {
template<>
struct Traits<PreviewServer::Hash> : public AK::GenericTraits<PreviewServer::Hash> {
    using Hash = PreviewServer::Hash;
    static constexpr unsigned hash(Hash value)
    {
        static_assert(popcount(Hash::Size) == 1);
        Array<u32, Hash::Size> partial_hashes;
        for (size_t i = 0; i < partial_hashes.size(); ++i)
            partial_hashes[i] = Traits<u8>::hash(value.bytes()[i]);

        size_t hash_count = partial_hashes.size();
        // Iteratively combine pairs of hashes.
        while (hash_count > 1) {
            for (size_t i = 0; i < hash_count; i += 2) {
                auto left = partial_hashes[i];
                auto right = partial_hashes[i + 1];
                partial_hashes[i / 2] = pair_int_hash(left, right);
            }
            hash_count /= 2;
        }
        return partial_hashes[0];
    }
};
}

namespace PreviewServer {

// TODO: Make the shareable bitmaps volatile while we're not sending them out,
//       so that cache entries can be purged by the Kernel under memory pressure.
// TODO: Keep disk cache and in-memory cache small by regularly removing old entries.
struct ValidCacheEntry {
    ValidCacheEntry()
        : path(DeprecatedString::empty())
    {
    }
    ValidCacheEntry(Gfx::ShareableBitmap preview, LexicalPath path)
        : preview(move(preview))
        , path(move(path))
    {
    }
    Gfx::ShareableBitmap preview;
    LexicalPath path;
};

// This struct MUST be incompatible with ErrorOr.
// Otherwise, Promise will "flatten" our contained error and crash because it expected a successful result.
struct CacheEntry {
    CacheEntry()
        : entry(ValidCacheEntry {})
    {
    }
    explicit CacheEntry(ErrorOr<ValidCacheEntry>&& entry)
        : entry(move(entry))
    {
    }
    explicit CacheEntry(CacheEntry const& cache_entry)
        : entry(cache_entry.entry.copy())
    {
    }
    CacheEntry(CacheEntry&&) = default;
    CacheEntry& operator=(CacheEntry&&) = default;
    CacheEntry(Error&& error)
        : entry(move(error))
    {
    }
    CacheEntry(ValidCacheEntry&& valid_entry)
        : entry(move(valid_entry))
    {
    }
    ErrorOr<ValidCacheEntry> entry;
};

using CachePromise = Core::Promise<CacheEntry>;

static LexicalPath s_cache_directory = LexicalPath::join(Core::StandardPaths::home_directory(), ".cache/preview"sv);

constexpr size_t preview_size = 32;

// TODO: Keep an in-memory cache.
class Cache final {
public:
    ~Cache();

    static Cache& the();

    static ErrorOr<Hash> hash_for(String const& file_name);
    static ErrorOr<String> path_for_hash(Hash file_hash);

    static bool excluded_from_cache(String const& file_name);

    ErrorOr<void> request_preview(String const& file_name, Hash file_hash, NonnullRefPtr<CachePromise> client_promise);

private:
    void create_generator_thread_if_necessary();

    // Main function of the generator thread.
    void generator_main(Core::EventLoop& main_event_loop);
    void enqueue_new_preview(Hash file_hash, CacheEntry preview, Core::EventLoop& main_event_loop);

    static ErrorOr<Gfx::ShareableBitmap> load_preview_from_disk_cache(Hash file_hash);
    static ErrorOr<void> write_preview_to_disk_cache(Hash file_hash, Gfx::ShareableBitmap preview);

    // Shared queue for pushing data to the preview generator thread.
    Core::SharedSingleProducerCircularQueue<String, 128> m_cache_requests;

    RefPtr<Threading::Thread> m_generator_thread;
    Core::EventLoop* m_generator_event_loop;

    // All registered promises of all clients. These are used to invoke preview_failed and preview_rendered callbacks.
    // The promises are never cancelled, therefore they will always run their on_complete callbacks.
    Threading::MutexProtected<HashMap<Hash, Vector<NonnullRefPtr<CachePromise>>>> m_client_promises;
};

}
