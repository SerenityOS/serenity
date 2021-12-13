/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/Hash/HashManager.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/Promise.h>
#include <LibWeb/Bindings/DOMExceptionWrapper.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/Crypto/SubtleCrypto.h>
#include <LibWeb/DOM/DOMException.h>

namespace Web::Crypto {

JS::Promise* SubtleCrypto::digest(String const& algorithm, JS::Handle<JS::Object> const& data)
{
    auto& global_object = wrapper()->global_object();

    // 1. Let algorithm be the algorithm parameter passed to the digest() method.

    // 2. Let data be the result of getting a copy of the bytes held by the data parameter passed to the digest() method.
    auto data_buffer = Bindings::IDL::get_buffer_source_copy(*data.cell());
    if (!data_buffer.has_value()) {
        auto* error = wrap(wrapper()->global_object(), DOM::OperationError::create("Failed to copy bytes from ArrayBuffer"));
        auto* promise = JS::Promise::create(global_object);
        promise->reject(error);
        return promise;
    }

    // 3. Let normalizedAlgorithm be the result of normalizing an algorithm, with alg set to algorithm and op set to "digest".
    // FIXME: This is way more generic than it needs to be right now, so we simplify it.
    ::Crypto::Hash::HashKind hash_kind;
    if (algorithm.equals_ignoring_case("SHA-1"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA1;
    } else if (algorithm.equals_ignoring_case("SHA-256"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA256;
    } else if (algorithm.equals_ignoring_case("SHA-384"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA384;
    } else if (algorithm.equals_ignoring_case("SHA-512"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA512;
    }
    // 4. If an error occurred, return a Promise rejected with normalizedAlgorithm.
    else {
        auto* error = wrap(wrapper()->global_object(), DOM::NotSupportedError::create(String::formatted("Invalid hash function '{}'", algorithm)));
        auto* promise = JS::Promise::create(global_object);
        promise->reject(error);
        return promise;
    }

    // 5. Let promise be a new Promise.
    auto* promise = JS::Promise::create(global_object);

    // 6. Return promise and perform the remaining steps in parallel.
    // FIXME: We don't have a good abstraction for this yet, so we do it in sync.

    // 7. If the following steps or referenced procedures say to throw an error, reject promise with the returned error and then terminate the algorithm.

    // 8. Let result be the result of performing the digest operation specified by normalizedAlgorithm using algorithm, with data as message.
    ::Crypto::Hash::Manager hash;
    hash.initialize(hash_kind);
    hash.update(*data_buffer);
    auto digest = hash.digest();
    auto const* digest_data = digest.immutable_data();
    auto result_buffer = ByteBuffer::create_zeroed(hash.digest_size());
    if (!result_buffer.has_value()) {
        auto* error = wrap(wrapper()->global_object(), DOM::OperationError::create("Failed to create result buffer"));
        promise->reject(error);
        return promise;
    }
    for (size_t i = 0; i < hash.digest_size(); ++i)
        (*result_buffer)[i] = digest_data[i];

    auto* result = JS::ArrayBuffer::create(global_object, result_buffer.release_value());

    // 9. Resolve promise with result.
    promise->fulfill(result);
    return promise;
}

}
