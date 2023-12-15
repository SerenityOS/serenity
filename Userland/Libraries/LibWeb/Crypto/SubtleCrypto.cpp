/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/Hash/HashManager.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/Promise.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Crypto/SubtleCrypto.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/Buffers.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Crypto {

JS_DEFINE_ALLOCATOR(SubtleCrypto);

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
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SubtleCryptoPrototype>(realm, "SubtleCrypto"_fly_string));
}

// https://w3c.github.io/webcrypto/#dfn-normalize-an-algorithm
JS::ThrowCompletionOr<Bindings::Algorithm> SubtleCrypto::normalize_an_algorithm(AlgorithmIdentifier const& algorithm, String operation)
{
    auto& realm = this->realm();

    // If alg is an instance of a DOMString:
    if (algorithm.has<String>()) {
        // Return the result of running the normalize an algorithm algorithm,
        // with the alg set to a new Algorithm dictionary whose name attribute is alg, and with the op set to op.
        auto dictionary = JS::make_handle(JS::Object::create(realm, realm.intrinsics().object_prototype()));
        TRY(dictionary->create_data_property("name", JS::PrimitiveString::create(realm.vm(), algorithm.get<String>())));
        TRY(dictionary->create_data_property("op", JS::PrimitiveString::create(realm.vm(), operation)));

        return normalize_an_algorithm(dictionary, operation);
    }

    // If alg is an object:
    // 1. Let registeredAlgorithms be the associative container stored at the op key of supportedAlgorithms.
    // NOTE: There should always be a container at the op key.
    auto internal_object = supported_algorithms();
    auto maybe_registered_algorithms = internal_object.get(operation);
    auto registered_algorithms = maybe_registered_algorithms.value();

    // 2. Let initialAlg be the result of converting the ECMAScript object represented by alg to
    // the IDL dictionary type Algorithm, as defined by [WebIDL].
    // FIXME: How do we turn this into an "Algorithm" in a nice way?
    // NOTE: For now, we just use the object as-is.
    auto initial_algorithm = algorithm.get<JS::Handle<JS::Object>>();

    // 3. If an error occurred, return the error and terminate this algorithm.
    auto has_name = TRY(initial_algorithm->has_property("name"));
    if (!has_name) {
        return realm.vm().throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "Algorithm");
    }

    // 4. Let algName be the value of the name attribute of initialAlg.
    auto algorithm_name = TRY(TRY(initial_algorithm->get("name")).to_string(realm.vm()));

    String desired_type;

    // 5. If registeredAlgorithms contains a key that is a case-insensitive string match for algName:
    if (registered_algorithms.contains(algorithm_name)) {
        // 1. Set algName to the value of the matching key.
        auto it = registered_algorithms.find(algorithm_name);
        algorithm_name = (*it).key;

        // 2. Let desiredType be the IDL dictionary type stored at algName in registeredAlgorithms.
        desired_type = (*it).value;
    } else {
        // Otherwise:
        // Return a new NotSupportedError and terminate this algorithm.
        // FIXME: This should be a DOMException
        return realm.vm().throw_completion<JS::TypeError>(JS::ErrorType::NotImplemented, algorithm_name);
    }

    // 8. Let normalizedAlgorithm be the result of converting the ECMAScript object represented by alg
    // to the IDL dictionary type desiredType, as defined by [WebIDL].
    // FIXME: Should IDL generate a struct for each of these?
    Bindings::Algorithm normalized_algorithm;

    // 9. Set the name attribute of normalizedAlgorithm to algName.
    normalized_algorithm.name = algorithm_name;

    // 10. If an error occurred, return the error and terminate this algorithm.

    // FIXME: 11. Let dictionaries be a list consisting of the IDL dictionary type desiredType
    // and all of desiredType's inherited dictionaries, in order from least to most derived.
    // FIXME: 12. For each dictionary dictionary in dictionaries:

    // 13. Return normalizedAlgorithm.
    return normalized_algorithm;
}

// https://w3c.github.io/webcrypto/#dfn-SubtleCrypto-method-digest
JS::NonnullGCPtr<JS::Promise> SubtleCrypto::digest(AlgorithmIdentifier const& algorithm, JS::Handle<WebIDL::BufferSource> const& data)
{
    auto& realm = this->realm();

    // 1. Let algorithm be the algorithm parameter passed to the digest() method.

    // 2. Let data be the result of getting a copy of the bytes held by the data parameter passed to the digest() method.
    auto data_buffer_or_error = WebIDL::get_buffer_source_copy(*data->raw_object());
    if (data_buffer_or_error.is_error()) {
        auto error = WebIDL::OperationError::create(realm, "Failed to copy bytes from ArrayBuffer"_fly_string);
        auto promise = JS::Promise::create(realm);
        promise->reject(error.ptr());
        return promise;
    }
    auto& data_buffer = data_buffer_or_error.value();

    // 3. Let normalizedAlgorithm be the result of normalizing an algorithm, with alg set to algorithm and op set to "digest".
    auto normalized_algorithm = normalize_an_algorithm(algorithm, "digest"_string);

    // 4. If an error occurred, return a Promise rejected with normalizedAlgorithm.
    if (normalized_algorithm.is_error()) {
        auto promise = JS::Promise::create(realm);
        auto error = normalized_algorithm.release_error();
        auto error_value = error.value().value();
        promise->reject(error_value);
        return promise;
    }

    // 5. Let promise be a new Promise.
    auto promise = JS::Promise::create(realm);

    // 6. Return promise and perform the remaining steps in parallel.
    // FIXME: We don't have a good abstraction for this yet, so we do it in sync.

    // 7. If the following steps or referenced procedures say to throw an error, reject promise with the returned error and then terminate the algorithm.

    // 8. Let result be the result of performing the digest operation specified by normalizedAlgorithm using algorithm, with data as message.
    auto algorithm_object = normalized_algorithm.release_value();
    auto algorithm_name = algorithm_object.name;

    ::Crypto::Hash::HashKind hash_kind;
    if (algorithm_name.equals_ignoring_ascii_case("SHA-1"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA1;
    } else if (algorithm_name.equals_ignoring_ascii_case("SHA-256"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA256;
    } else if (algorithm_name.equals_ignoring_ascii_case("SHA-384"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA384;
    } else if (algorithm_name.equals_ignoring_ascii_case("SHA-512"sv)) {
        hash_kind = ::Crypto::Hash::HashKind::SHA512;
    } else {
        auto error = WebIDL::NotSupportedError::create(realm, MUST(String::formatted("Invalid hash function '{}'", algorithm_name)));
        promise->reject(error.ptr());
        return promise;
    }

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

SubtleCrypto::SupportedAlgorithmsMap& SubtleCrypto::supported_algorithms_internal()
{
    static SubtleCrypto::SupportedAlgorithmsMap s_supported_algorithms;
    return s_supported_algorithms;
}

// https://w3c.github.io/webcrypto/#algorithm-normalization-internalS
SubtleCrypto::SupportedAlgorithmsMap SubtleCrypto::supported_algorithms()
{
    auto& internal_object = supported_algorithms_internal();

    if (!internal_object.is_empty()) {
        return internal_object;
    }

    // 1. For each value, v in the List of supported operations,
    // set the v key of the internal object supportedAlgorithms to a new associative container.
    auto supported_operations = Vector {
        "encrypt"_string,
        "decrypt"_string,
        "sign"_string,
        "verify"_string,
        "digest"_string,
        "deriveBits"_string,
        "wrapKey"_string,
        "unwrapKey"_string,
        "generateKey"_string,
        "importKey"_string,
        "exportKey"_string,
        "get key length"_string,
    };

    for (auto& operation : supported_operations) {
        internal_object.set(operation, {});
    }

    // https://w3c.github.io/webcrypto/#algorithm-conventions
    // https://w3c.github.io/webcrypto/#sha
    define_an_algorithm("digest"_string, "SHA1"_string, ""_string);
    define_an_algorithm("digest"_string, "SHA-256"_string, ""_string);
    define_an_algorithm("digest"_string, "SHA-384"_string, ""_string);
    define_an_algorithm("digest"_string, "SHA-512"_string, ""_string);

    return internal_object;
}

// https://w3c.github.io/webcrypto/#concept-define-an-algorithm
void SubtleCrypto::define_an_algorithm(String op, String algorithm, String type)
{
    auto& internal_object = supported_algorithms_internal();

    // 1. Let registeredAlgorithms be the associative container stored at the op key of supportedAlgorithms.
    // NOTE: There should always be a container at the op key.
    auto maybe_registered_algorithms = internal_object.get(op);
    auto registered_algorithms = maybe_registered_algorithms.value();

    // 2. Set the alg key of registeredAlgorithms to the IDL dictionary type type.
    registered_algorithms.set(algorithm, type);
    internal_object.set(op, registered_algorithms);
}

}
