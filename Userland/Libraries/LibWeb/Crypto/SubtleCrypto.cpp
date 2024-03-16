/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibCrypto/Hash/HashManager.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/Promise.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Crypto/KeyAlgorithms.h>
#include <LibWeb/Crypto/SubtleCrypto.h>
#include <LibWeb/HTML/Scripting/TemporaryExecutionContext.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/Buffers.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Crypto {

static void normalize_key_usages(Vector<Bindings::KeyUsage>& key_usages)
{
    quick_sort(key_usages);
}

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
    WEB_SET_PROTOTYPE_FOR_INTERFACE(SubtleCrypto);
}

// https://w3c.github.io/webcrypto/#dfn-normalize-an-algorithm
WebIDL::ExceptionOr<SubtleCrypto::NormalizedAlgorithmAndParameter> SubtleCrypto::normalize_an_algorithm(AlgorithmIdentifier const& algorithm, String operation)
{
    auto& realm = this->realm();
    auto& vm = this->vm();

    // If alg is an instance of a DOMString:
    if (algorithm.has<String>()) {
        // Return the result of running the normalize an algorithm algorithm,
        // with the alg set to a new Algorithm dictionary whose name attribute is alg, and with the op set to op.
        auto dictionary = JS::make_handle(JS::Object::create(realm, realm.intrinsics().object_prototype()));
        TRY(dictionary->create_data_property("name", JS::PrimitiveString::create(vm, algorithm.get<String>())));

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
    // 3. If an error occurred, return the error and terminate this algorithm.
    // Note: We're not going to bother creating an Algorithm object, all we want is the name attribute so that we can
    //       fetch the actual algorithm factory from the registeredAlgorithms map.
    auto initial_algorithm = TRY(algorithm.get<JS::Handle<JS::Object>>()->get("name"));

    // 4. Let algName be the value of the name attribute of initialAlg.
    auto algorithm_name = TRY(initial_algorithm.to_string(vm));

    RegisteredAlgorithm desired_type;

    // 5. If registeredAlgorithms contains a key that is a case-insensitive string match for algName:
    if (auto it = registered_algorithms.find(algorithm_name); it != registered_algorithms.end()) {
        // 1. Set algName to the value of the matching key.
        // 2. Let desiredType be the IDL dictionary type stored at algName in registeredAlgorithms.
        desired_type = it->value;
    } else {
        // Otherwise:
        // Return a new NotSupportedError and terminate this algorithm.
        return WebIDL::NotSupportedError::create(realm, MUST(String::formatted("Algorithm '{}' is not supported", algorithm_name)));
    }

    // 8. Let normalizedAlgorithm be the result of converting the ECMAScript object represented by alg
    // to the IDL dictionary type desiredType, as defined by [WebIDL].
    // 9. Set the name attribute of normalizedAlgorithm to algName.
    // 10. If an error occurred, return the error and terminate this algorithm.
    // 11. Let dictionaries be a list consisting of the IDL dictionary type desiredType
    // and all of desiredType's inherited dictionaries, in order from least to most derived.
    // 12. For each dictionary dictionary in dictionaries:
    //    Note: All of these steps are handled by the create_methods and parameter_from_value methods.
    auto methods = desired_type.create_methods(realm);
    auto parameter = TRY(desired_type.parameter_from_value(vm, algorithm.get<JS::Handle<JS::Object>>()));
    auto normalized_algorithm = NormalizedAlgorithmAndParameter { move(methods), move(parameter) };

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
    if (data_buffer_or_error.is_error())
        return WebIDL::create_rejected_promise_from_exception(realm, WebIDL::OperationError::create(realm, "Failed to copy bytes from ArrayBuffer"_fly_string));
    auto data_buffer = data_buffer_or_error.release_value();

    // 3. Let normalizedAlgorithm be the result of normalizing an algorithm, with alg set to algorithm and op set to "digest".
    auto normalized_algorithm = normalize_an_algorithm(algorithm, "digest"_string);

    // 4. If an error occurred, return a Promise rejected with normalizedAlgorithm.
    // FIXME: Spec bug: link to https://webidl.spec.whatwg.org/#a-promise-rejected-with
    if (normalized_algorithm.is_error())
        return WebIDL::create_rejected_promise_from_exception(realm, normalized_algorithm.release_error());

    // 5. Let promise be a new Promise.
    auto promise = WebIDL::create_promise(realm);

    // 6. Return promise and perform the remaining steps in parallel.
    Platform::EventLoopPlugin::the().deferred_invoke([&realm, algorithm_object = normalized_algorithm.release_value(), promise, data_buffer = move(data_buffer)]() -> void {
        HTML::TemporaryExecutionContext context(Bindings::host_defined_environment_settings_object(realm), HTML::TemporaryExecutionContext::CallbacksEnabled::Yes);
        // 7. If the following steps or referenced procedures say to throw an error, reject promise with the returned error and then terminate the algorithm.
        // FIXME: Need spec reference to https://webidl.spec.whatwg.org/#reject

        // 8. Let result be the result of performing the digest operation specified by normalizedAlgorithm using algorithm, with data as message.
        auto result = algorithm_object.methods->digest(*algorithm_object.parameter, data_buffer);

        if (result.is_exception()) {
            WebIDL::reject_promise(realm, promise, Bindings::dom_exception_to_throw_completion(realm.vm(), result.release_error()).release_value().value());
            return;
        }

        // 9. Resolve promise with result.
        WebIDL::resolve_promise(realm, promise, result.release_value());
    });

    return verify_cast<JS::Promise>(*promise->promise());
}

// https://w3c.github.io/webcrypto/#dfn-SubtleCrypto-method-generateKey
JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Promise>> SubtleCrypto::generate_key(AlgorithmIdentifier algorithm, bool extractable, Vector<Bindings::KeyUsage> key_usages)
{
    auto& realm = this->realm();

    // 1. Let algorithm, extractable and usages be the algorithm, extractable and keyUsages
    //    parameters passed to the generateKey() method, respectively.

    // 2. Let normalizedAlgorithm be the result of normalizing an algorithm,
    //    with alg set to algorithm and op set to "generateKey".
    auto normalized_algorithm = normalize_an_algorithm(algorithm, "generateKey"_string);

    // 3. If an error occurred, return a Promise rejected with normalizedAlgorithm.
    if (normalized_algorithm.is_error())
        return WebIDL::create_rejected_promise_from_exception(realm, normalized_algorithm.release_error());

    // 4. Let promise be a new Promise.
    auto promise = WebIDL::create_promise(realm);

    // 5. Return promise and perform the remaining steps in parallel.
    Platform::EventLoopPlugin::the().deferred_invoke([&realm, normalized_algorithm = normalized_algorithm.release_value(), promise, extractable, key_usages = move(key_usages)]() -> void {
        HTML::TemporaryExecutionContext context(Bindings::host_defined_environment_settings_object(realm), HTML::TemporaryExecutionContext::CallbacksEnabled::Yes);
        // 6. If the following steps or referenced procedures say to throw an error, reject promise with
        //    the returned error and then terminate the algorithm.

        // 7. Let result be the result of performing the generate key operation specified by normalizedAlgorithm
        //    using algorithm, extractable and usages.
        auto result_or_error = normalized_algorithm.methods->generate_key(*normalized_algorithm.parameter, extractable, key_usages);

        if (result_or_error.is_error()) {
            WebIDL::reject_promise(realm, promise, Bindings::dom_exception_to_throw_completion(realm.vm(), result_or_error.release_error()).release_value().value());
            return;
        }
        auto result = result_or_error.release_value();

        // 8. If result is a CryptoKey object:
        //      If the [[type]] internal slot of result is "secret" or "private" and usages is empty, then throw a SyntaxError.
        //    If result is a CryptoKeyPair object:
        //      If the [[usages]] internal slot of the privateKey attribute of result is the empty sequence, then throw a SyntaxError.
        // 9. Resolve promise with result.
        result.visit(
            [&](JS::NonnullGCPtr<CryptoKey>& key) {
                if ((key->type() == Bindings::KeyType::Secret || key->type() == Bindings::KeyType::Private) && key_usages.is_empty()) {
                    WebIDL::reject_promise(realm, promise, WebIDL::SyntaxError::create(realm, "usages must not be empty"_fly_string));
                    return;
                }
                WebIDL::resolve_promise(realm, promise, key);
            },
            [&](JS::NonnullGCPtr<CryptoKeyPair>& key_pair) {
                if (key_pair->private_key()->internal_usages().is_empty()) {
                    WebIDL::reject_promise(realm, promise, WebIDL::SyntaxError::create(realm, "usages must not be empty"_fly_string));
                    return;
                }
                WebIDL::resolve_promise(realm, promise, key_pair);
            });
    });

    return verify_cast<JS::Promise>(*promise->promise());
}

// https://w3c.github.io/webcrypto/#SubtleCrypto-method-importKey
JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Promise>> SubtleCrypto::import_key(Bindings::KeyFormat format, KeyDataType key_data, AlgorithmIdentifier algorithm, bool extractable, Vector<Bindings::KeyUsage> key_usages)
{
    auto& realm = this->realm();

    // 1. Let format, algorithm, extractable and usages, be the format, algorithm, extractable
    // and key_usages parameters passed to the importKey() method, respectively.

    Variant<ByteBuffer, Bindings::JsonWebKey, Empty> real_key_data;
    // 2. If format is equal to the string "raw", "pkcs8", or "spki":
    if (format == Bindings::KeyFormat::Raw || format == Bindings::KeyFormat::Pkcs8 || format == Bindings::KeyFormat::Spki) {
        // 1. If the keyData parameter passed to the importKey() method is a JsonWebKey dictionary, throw a TypeError.
        if (key_data.has<Bindings::JsonWebKey>()) {
            return realm.vm().throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "BufferSource");
        }

        // 2. Let keyData be the result of getting a copy of the bytes held by the keyData parameter passed to the importKey() method.
        real_key_data = MUST(WebIDL::get_buffer_source_copy(*key_data.get<JS::Handle<WebIDL::BufferSource>>()->raw_object()));
    }

    if (format == Bindings::KeyFormat::Jwk) {
        // 1. If the keyData parameter passed to the importKey() method is not a JsonWebKey dictionary, throw a TypeError.
        if (!key_data.has<Bindings::JsonWebKey>()) {
            return realm.vm().throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "JsonWebKey");
        }

        // 2. Let keyData be the keyData parameter passed to the importKey() method.
        real_key_data = key_data.get<Bindings::JsonWebKey>();
    }

    // NOTE: The spec jumps to 5 here for some reason?
    // 5. Let normalizedAlgorithm be the result of normalizing an algorithm, with alg set to algorithm and op set to "importKey".
    auto normalized_algorithm = normalize_an_algorithm(algorithm, "importKey"_string);

    // 6. If an error occurred, return a Promise rejected with normalizedAlgorithm.
    if (normalized_algorithm.is_error())
        return WebIDL::create_rejected_promise_from_exception(realm, normalized_algorithm.release_error());

    // 7. Let promise be a new Promise.
    auto promise = WebIDL::create_promise(realm);

    // 8. Return promise and perform the remaining steps in parallel.
    Platform::EventLoopPlugin::the().deferred_invoke([&realm, real_key_data = move(real_key_data), normalized_algorithm = normalized_algorithm.release_value(), promise, format, extractable, key_usages = move(key_usages), algorithm = move(algorithm)]() mutable -> void {
        HTML::TemporaryExecutionContext context(Bindings::host_defined_environment_settings_object(realm), HTML::TemporaryExecutionContext::CallbacksEnabled::Yes);

        // 9. If the following steps or referenced procedures say to throw an error, reject promise with the returned error and then terminate the algorithm.

        // 10. Let result be the CryptoKey object that results from performing the import key operation
        // specified by normalizedAlgorithm using keyData, algorithm, format, extractable and usages.
        auto maybe_result = normalized_algorithm.methods->import_key(*normalized_algorithm.parameter, format, real_key_data.downcast<CryptoKey::InternalKeyData>(), extractable, key_usages);
        if (maybe_result.is_error()) {
            WebIDL::reject_promise(realm, promise, Bindings::dom_exception_to_throw_completion(realm.vm(), maybe_result.release_error()).release_value().value());
            return;
        }
        auto result = maybe_result.release_value();

        // 11. If the [[type]] internal slot of result is "secret" or "private" and usages is empty, then throw a SyntaxError.
        if ((result->type() == Bindings::KeyType::Secret || result->type() == Bindings::KeyType::Private) && key_usages.is_empty()) {
            WebIDL::reject_promise(realm, promise, WebIDL::SyntaxError::create(realm, "usages must not be empty"_fly_string));
            return;
        }

        // 12. Set the [[extractable]] internal slot of result to extractable.
        result->set_extractable(extractable);

        // 13. Set the [[usages]] internal slot of result to the normalized value of usages.
        normalize_key_usages(key_usages);
        result->set_usages(key_usages);

        // 14. Resolve promise with result.
        WebIDL::resolve_promise(realm, promise, result);
    });

    return verify_cast<JS::Promise>(*promise->promise());
}

// https://w3c.github.io/webcrypto/#dfn-SubtleCrypto-method-exportKey
JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Promise>> SubtleCrypto::export_key(Bindings::KeyFormat format, JS::NonnullGCPtr<CryptoKey> key)
{
    auto& realm = this->realm();
    // 1. Let format and key be the format and key parameters passed to the exportKey() method, respectively.

    // 2. Let promise be a new Promise.
    auto promise = WebIDL::create_promise(realm);

    // 3. Return promise and perform the remaining steps in parallel.
    Platform::EventLoopPlugin::the().deferred_invoke([&realm, key, this, promise, format]() -> void {
        HTML::TemporaryExecutionContext context(Bindings::host_defined_environment_settings_object(realm), HTML::TemporaryExecutionContext::CallbacksEnabled::Yes);
        // 4.  If the following steps or referenced procedures say to throw an error, reject promise with the returned error and then terminate the algorithm.

        // 5. If the name member of the [[algorithm]] internal slot of key does not identify a registered algorithm that supports the export key operation,
        //    then throw a NotSupportedError.
        // Note: Handled by the base AlgorithmMethods implementation
        auto& algorithm = verify_cast<KeyAlgorithm>(*key->algorithm());
        // FIXME: Stash the AlgorithmMethods on the KeyAlgorithm
        auto normalized_algorithm_or_error = normalize_an_algorithm(algorithm.name(), "exportKey"_string);
        if (normalized_algorithm_or_error.is_error()) {
            WebIDL::reject_promise(realm, promise, Bindings::dom_exception_to_throw_completion(realm.vm(), normalized_algorithm_or_error.release_error()).release_value().value());
            return;
        }
        auto normalized_algorithm = normalized_algorithm_or_error.release_value();

        // 6. If the [[extractable]] internal slot of key is false, then throw an InvalidAccessError.
        if (!key->extractable()) {
            WebIDL::reject_promise(realm, promise, WebIDL::InvalidAccessError::create(realm, "Key is not extractable"_fly_string));
            return;
        }

        // 7. Let result be the result of performing the export key operation specified by the [[algorithm]] internal slot of key using key and format.
        auto result_or_error = normalized_algorithm.methods->export_key(format, key);
        if (result_or_error.is_error()) {
            WebIDL::reject_promise(realm, promise, Bindings::dom_exception_to_throw_completion(realm.vm(), result_or_error.release_error()).release_value().value());
            return;
        }

        // 8. Resolve promise with result.
        WebIDL::resolve_promise(realm, promise, result_or_error.release_value());
    });

    return verify_cast<JS::Promise>(*promise->promise());
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
    define_an_algorithm<SHA>("digest"_string, "SHA-1"_string);
    define_an_algorithm<SHA>("digest"_string, "SHA-256"_string);
    define_an_algorithm<SHA>("digest"_string, "SHA-384"_string);
    define_an_algorithm<SHA>("digest"_string, "SHA-512"_string);

    // https://w3c.github.io/webcrypto/#pbkdf2
    define_an_algorithm<PBKDF2>("importKey"_string, "PBKDF2"_string);
    // FIXME: define_an_algorithm("deriveBits"_string, "PBKDF2"_string, "Pbkdf2Params"_string);
    // FIXME: define_an_algorithm("get key length"_string, "PBKDF2"_string, ""_string);

    // https://w3c.github.io/webcrypto/#rsa-oaep
    define_an_algorithm<RSAOAEP, RsaHashedKeyGenParams>("generateKey"_string, "RSA-OAEP"_string);
    define_an_algorithm<RSAOAEP>("exportKey"_string, "RSA-OAEP"_string);
    // FIXME: encrypt, decrypt, importKey

    return internal_object;
}

// https://w3c.github.io/webcrypto/#concept-define-an-algorithm
template<typename Methods, typename Param>
void SubtleCrypto::define_an_algorithm(AK::String op, AK::String algorithm)
{
    auto& internal_object = supported_algorithms_internal();

    // 1. Let registeredAlgorithms be the associative container stored at the op key of supportedAlgorithms.
    // NOTE: There should always be a container at the op key.
    auto maybe_registered_algorithms = internal_object.get(op);
    auto registered_algorithms = maybe_registered_algorithms.value();

    // 2. Set the alg key of registeredAlgorithms to the IDL dictionary type type.
    registered_algorithms.set(algorithm, RegisteredAlgorithm { &Methods::create, &Param::from_value });
    internal_object.set(op, registered_algorithms);
}

}
