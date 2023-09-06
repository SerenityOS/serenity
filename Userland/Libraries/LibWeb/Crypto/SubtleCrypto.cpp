/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/Hash/HashManager.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/Promise.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Crypto/SubtleCrypto.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Crypto {

JS::NonnullGCPtr<SubtleCrypto> SubtleCrypto::create(JS::Realm& realm)
{
    return realm.heap().allocate<SubtleCrypto>(realm, realm);
}

SubtleCrypto::SubtleCrypto(JS::Realm& realm)
    : PlatformObject(realm)
{
}

SubtleCrypto::~SubtleCrypto() = default;

void SubtleCrypto::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SubtleCryptoPrototype>(realm, "SubtleCrypto"));
}

// https://w3c.github.io/webcrypto/#dfn-SubtleCrypto-method-digest
JS::NonnullGCPtr<JS::Promise> SubtleCrypto::digest(String const& algorithm, JS::Handle<JS::Object> const& data)
{
    auto& realm = this->realm();

    // 1. Let algorithm be the algorithm parameter passed to the digest() method.

    // 2. Let data be the result of getting a copy of the bytes held by the data parameter passed to the digest() method.
    auto data_buffer_or_error = WebIDL::get_buffer_source_copy(*data.cell());
    if (data_buffer_or_error.is_error()) {
        auto error = WebIDL::OperationError::create(realm, "Failed to copy bytes from ArrayBuffer"_fly_string);
        auto promise = JS::Promise::create(realm);
        promise->reject(error.ptr());
        return promise;
    }
    auto& data_buffer = data_buffer_or_error.value();

    // 3. Let normalizedAlgorithm be the result of normalizing an algorithm, with alg set to algorithm and op set to "digest".
    // FIXME: This is way more generic than it needs to be right now, so we simplify it.
    ::Crypto::Hash::HashKind hash_kind;
    auto algorithm_as_string_view = algorithm.bytes_as_string_view();
    if (algorithm_as_string_view.equals_ignoring_ascii_case("SHA-1"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA1;
    } else if (algorithm_as_string_view.equals_ignoring_ascii_case("SHA-256"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA256;
    } else if (algorithm_as_string_view.equals_ignoring_ascii_case("SHA-384"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA384;
    } else if (algorithm_as_string_view.equals_ignoring_ascii_case("SHA-512"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA512;
    }
    // 4. If an error occurred, return a Promise rejected with normalizedAlgorithm.
    else {
        auto error = WebIDL::NotSupportedError::create(realm, MUST(String::formatted("Invalid hash function '{}'", algorithm)));
        auto promise = JS::Promise::create(realm);
        promise->reject(error.ptr());
        return promise;
    }

    // 5. Let promise be a new Promise.
    auto promise = JS::Promise::create(realm);

    // 6. Return promise and perform the remaining steps in parallel.
    // FIXME: We don't have a good abstraction for this yet, so we do it in sync.

    // 7. If the following steps or referenced procedures say to throw an error, reject promise with the returned error and then terminate the algorithm.

    // 8. Let result be the result of performing the digest operation specified by normalizedAlgorithm using algorithm, with data as message.
    ::Crypto::Hash::Manager hash { hash_kind };
    hash.update(data_buffer);

    auto digest = hash.digest();
    auto result_buffer = ByteBuffer::copy(digest.immutable_data(), hash.digest_size());
    if (result_buffer.is_error()) {
        auto error = WebIDL::OperationError::create(realm, "Failed to create result buffer"_fly_string);
        promise->reject(error.ptr());
        return promise;
    }

    auto result = JS::ArrayBuffer::create(realm, result_buffer.release_value());

    // 9. Resolve promise with result.
    promise->fulfill(result);
    return promise;
}

}
