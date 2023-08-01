/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/FileAPI/Blob.h>
#include <LibWeb/FileAPI/BlobURLStore.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Scripting/Environments.h>

namespace Web::FileAPI {

BlobURLStore& blob_url_store()
{
    static HashMap<String, BlobURLEntry> store;
    return store;
}

// https://w3c.github.io/FileAPI/#unicodeBlobURL
ErrorOr<String> generate_new_blob_url()
{
    // 1. Let result be the empty string.
    StringBuilder result;

    // 2. Append the string "blob:" to result.
    TRY(result.try_append("blob:"sv));

    // 3. Let settings be the current settings object
    auto& settings = HTML::current_settings_object();

    // 4. Let origin be settings’s origin.
    auto origin = settings.origin();

    // 5. Let serialized be the ASCII serialization of origin.
    auto serialized = origin.serialize();

    // 6. If serialized is "null", set it to an implementation-defined value.
    if (serialized == "null"sv)
        serialized = "ladybird"sv;

    // 7. Append serialized to result.
    TRY(result.try_append(serialized));

    // 8. Append U+0024 SOLIDUS (/) to result.
    TRY(result.try_append('/'));

    // 9. Generate a UUID [RFC4122] as a string and append it to result.
    auto uuid = TRY(Crypto::generate_random_uuid());
    TRY(result.try_append(uuid));

    // 10. Return result.
    return result.to_string();
}

// https://w3c.github.io/FileAPI/#add-an-entry
ErrorOr<String> add_entry_to_blob_url_store(JS::NonnullGCPtr<Blob> object)
{
    // 1. Let store be the user agent’s blob URL store.
    auto& store = blob_url_store();

    // 2. Let url be the result of generating a new blob URL.
    auto url = TRY(generate_new_blob_url());

    // 3. Let entry be a new blob URL entry consisting of object and the current settings object.
    BlobURLEntry entry { object, HTML::current_settings_object() };

    // 4. Set store[url] to entry.
    TRY(store.try_set(url, move(entry)));

    // 5. Return url.
    return url;
}

// https://w3c.github.io/FileAPI/#removeTheEntry
ErrorOr<void> remove_entry_from_blob_url_store(StringView url)
{
    // 1. Let store be the user agent’s blob URL store;
    auto& store = blob_url_store();

    // 2. Let url string be the result of serializing url.
    auto url_string = TRY(AK::URL { url }.to_string());

    // 3. Remove store[url string].
    store.remove(url_string);
    return {};
}

}
