/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/QuickSort.h>
#include <LibCrypto/Hash/HashManager.h>
#include <LibCrypto/PK/RSA.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Crypto/CryptoAlgorithms.h>
#include <LibWeb/Crypto/KeyAlgorithms.h>

namespace Web::Crypto {

// https://w3c.github.io/webcrypto/#concept-usage-intersection
static Vector<Bindings::KeyUsage> usage_intersection(ReadonlySpan<Bindings::KeyUsage> a, ReadonlySpan<Bindings::KeyUsage> b)
{
    Vector<Bindings::KeyUsage> result;
    for (auto const& usage : a) {
        if (b.contains_slow(usage))
            result.append(usage);
    }
    quick_sort(result);
    return result;
}

// Out of line to ensure this class has a key function
AlgorithmMethods::~AlgorithmMethods() = default;

// https://w3c.github.io/webcrypto/#big-integer
static ::Crypto::UnsignedBigInteger big_integer_from_api_big_integer(JS::GCPtr<JS::Uint8Array> const& big_integer)
{
    static_assert(AK::HostIsLittleEndian, "This method needs special treatment for BE");

    // The BigInteger typedef is a Uint8Array that holds an arbitrary magnitude unsigned integer
    // **in big-endian order**. Values read from the API SHALL have minimal typed array length
    // (that is, at most 7 leading zero bits, except the value 0 which shall have length 8 bits).
    // The API SHALL accept values with any number of leading zero bits, including the empty array, which represents zero.

    auto const& buffer = big_integer->viewed_array_buffer()->buffer();

    ::Crypto::UnsignedBigInteger result(0);
    if (buffer.size() > 0) {

        // We need to reverse the buffer to get it into little-endian order
        Vector<u8, 32> reversed_buffer;
        reversed_buffer.resize(buffer.size());
        for (size_t i = 0; i < buffer.size(); ++i) {
            reversed_buffer[buffer.size() - i - 1] = buffer[i];
        }

        result = ::Crypto::UnsignedBigInteger::import_data(reversed_buffer.data(), reversed_buffer.size());
    }
    return result;
}

// https://www.rfc-editor.org/rfc/rfc7518#section-2
ErrorOr<String> base64_url_uint_encode(::Crypto::UnsignedBigInteger integer)
{
    static_assert(AK::HostIsLittleEndian, "This code assumes little-endian");

    // The representation of a positive or zero integer value as the
    // base64url encoding of the value's unsigned big-endian
    // representation as an octet sequence.  The octet sequence MUST
    // utilize the minimum number of octets needed to represent the
    // value.  Zero is represented as BASE64URL(single zero-valued
    // octet), which is "AA".

    auto bytes = TRY(ByteBuffer::create_uninitialized(integer.trimmed_byte_length()));

    bool const remove_leading_zeroes = true;
    auto data_size = integer.export_data(bytes.span(), remove_leading_zeroes);

    auto data_slice = bytes.bytes().slice(bytes.size() - data_size, data_size);

    // We need to encode the integer's big endian representation as a base64 string
    Vector<u8, 32> byte_swapped_data;
    byte_swapped_data.ensure_capacity(data_size);
    for (size_t i = 0; i < data_size; ++i)
        byte_swapped_data.append(data_slice[data_size - i - 1]);

    auto encoded = TRY(encode_base64url(byte_swapped_data));

    // FIXME: create a version of encode_base64url that omits padding bytes
    if (auto first_padding_byte = encoded.find_byte_offset('='); first_padding_byte.has_value())
        return encoded.substring_from_byte_offset(0, first_padding_byte.value());
    return encoded;
}

AlgorithmParams::~AlgorithmParams() = default;

JS::ThrowCompletionOr<NonnullOwnPtr<AlgorithmParams>> AlgorithmParams::from_value(JS::VM& vm, JS::Value value)
{
    auto& object = value.as_object();

    auto name = TRY(object.get("name"));
    auto name_string = TRY(name.to_string(vm));

    return adopt_own(*new AlgorithmParams { name_string });
}

PBKDF2Params::~PBKDF2Params() = default;

JS::ThrowCompletionOr<NonnullOwnPtr<AlgorithmParams>> PBKDF2Params::from_value(JS::VM& vm, JS::Value value)
{
    auto& realm = *vm.current_realm();
    auto& object = value.as_object();

    auto name_value = TRY(object.get("name"));
    auto name = TRY(name_value.to_string(vm));

    auto salt_value = TRY(object.get("salt"));
    JS::Handle<WebIDL::BufferSource> salt;

    if (!salt_value.is_object() || !(is<JS::TypedArrayBase>(salt_value.as_object()) || is<JS::ArrayBuffer>(salt_value.as_object()) || is<JS::DataView>(salt_value.as_object())))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "BufferSource");

    salt = JS::make_handle(vm.heap().allocate<WebIDL::BufferSource>(realm, salt_value.as_object()));

    auto iterations_value = TRY(object.get("iterations"));
    auto iterations = TRY(iterations_value.to_u32(vm));

    auto hash_value = TRY(object.get("hash"));
    auto hash = Variant<Empty, HashAlgorithmIdentifier> { Empty {} };
    if (hash_value.is_string()) {
        auto hash_string = TRY(hash_value.to_string(vm));
        hash = HashAlgorithmIdentifier { hash_string };
    } else {
        auto hash_object = TRY(hash_value.to_object(vm));
        hash = HashAlgorithmIdentifier { hash_object };
    }

    return adopt_own<AlgorithmParams>(*new PBKDF2Params { name, salt, iterations, hash.downcast<HashAlgorithmIdentifier>() });
}

RsaKeyGenParams::~RsaKeyGenParams() = default;

JS::ThrowCompletionOr<NonnullOwnPtr<AlgorithmParams>> RsaKeyGenParams::from_value(JS::VM& vm, JS::Value value)
{
    auto& object = value.as_object();

    auto name_value = TRY(object.get("name"));
    auto name = TRY(name_value.to_string(vm));

    auto modulus_length_value = TRY(object.get("modulusLength"));
    auto modulus_length = TRY(modulus_length_value.to_u32(vm));

    auto public_exponent_value = TRY(object.get("publicExponent"));
    JS::GCPtr<JS::Uint8Array> public_exponent;

    if (!public_exponent_value.is_object() || !is<JS::Uint8Array>(public_exponent_value.as_object()))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "Uint8Array");

    public_exponent = static_cast<JS::Uint8Array&>(public_exponent_value.as_object());

    return adopt_own<AlgorithmParams>(*new RsaKeyGenParams { name, modulus_length, big_integer_from_api_big_integer(public_exponent) });
}

RsaHashedKeyGenParams::~RsaHashedKeyGenParams() = default;

JS::ThrowCompletionOr<NonnullOwnPtr<AlgorithmParams>> RsaHashedKeyGenParams::from_value(JS::VM& vm, JS::Value value)
{
    auto& object = value.as_object();

    auto name_value = TRY(object.get("name"));
    auto name = TRY(name_value.to_string(vm));

    auto modulus_length_value = TRY(object.get("modulusLength"));
    auto modulus_length = TRY(modulus_length_value.to_u32(vm));

    auto public_exponent_value = TRY(object.get("publicExponent"));
    JS::GCPtr<JS::Uint8Array> public_exponent;

    if (!public_exponent_value.is_object() || !is<JS::Uint8Array>(public_exponent_value.as_object()))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "Uint8Array");

    public_exponent = static_cast<JS::Uint8Array&>(public_exponent_value.as_object());

    auto hash_value = TRY(object.get("hash"));
    auto hash = Variant<Empty, HashAlgorithmIdentifier> { Empty {} };
    if (hash_value.is_string()) {
        auto hash_string = TRY(hash_value.to_string(vm));
        hash = HashAlgorithmIdentifier { hash_string };
    } else {
        auto hash_object = TRY(hash_value.to_object(vm));
        hash = HashAlgorithmIdentifier { hash_object };
    }

    return adopt_own<AlgorithmParams>(*new RsaHashedKeyGenParams { name, modulus_length, big_integer_from_api_big_integer(public_exponent), hash.get<HashAlgorithmIdentifier>() });
}

// https://w3c.github.io/webcrypto/#rsa-oaep-operations
WebIDL::ExceptionOr<Variant<JS::NonnullGCPtr<CryptoKey>, JS::NonnullGCPtr<CryptoKeyPair>>> RSAOAEP::generate_key(AlgorithmParams const& params, bool extractable, Vector<Bindings::KeyUsage> const& key_usages)
{
    // 1. If usages contains an entry which is not "encrypt", "decrypt", "wrapKey" or "unwrapKey", then throw a SyntaxError.
    for (auto const& usage : key_usages) {
        if (usage != Bindings::KeyUsage::Encrypt && usage != Bindings::KeyUsage::Decrypt && usage != Bindings::KeyUsage::Wrapkey && usage != Bindings::KeyUsage::Unwrapkey) {
            return WebIDL::SyntaxError::create(m_realm, MUST(String::formatted("Invalid key usage '{}'", idl_enum_to_string(usage))));
        }
    }

    // 2. Generate an RSA key pair, as defined in [RFC3447], with RSA modulus length equal to the modulusLength member of normalizedAlgorithm
    //    and RSA public exponent equal to the publicExponent member of normalizedAlgorithm.
    // 3. If performing the operation results in an error, then throw an OperationError.
    auto const& normalized_algorithm = static_cast<RsaHashedKeyGenParams const&>(params);
    auto key_pair = ::Crypto::PK::RSA::generate_key_pair(normalized_algorithm.modulus_length, normalized_algorithm.public_exponent);

    // 4. Let algorithm be a new RsaHashedKeyAlgorithm object.
    auto algorithm = RsaHashedKeyAlgorithm::create(m_realm);

    // 5. Set the name attribute of algorithm to "RSA-OAEP".
    algorithm->set_name("RSA-OAEP"_string);

    // 6. Set the modulusLength attribute of algorithm to equal the modulusLength member of normalizedAlgorithm.
    algorithm->set_modulus_length(normalized_algorithm.modulus_length);

    // 7. Set the publicExponent attribute of algorithm to equal the publicExponent member of normalizedAlgorithm.
    TRY(algorithm->set_public_exponent(normalized_algorithm.public_exponent));

    // 8. Set the hash attribute of algorithm to equal the hash member of normalizedAlgorithm.
    algorithm->set_hash(normalized_algorithm.hash);

    // 9. Let publicKey be a new CryptoKey representing the public key of the generated key pair.
    auto public_key = CryptoKey::create(m_realm, CryptoKey::InternalKeyData { key_pair.public_key });

    // 10. Set the [[type]] internal slot of publicKey to "public"
    public_key->set_type(Bindings::KeyType::Public);

    // 11. Set the [[algorithm]] internal slot of publicKey to algorithm.
    public_key->set_algorithm(algorithm);

    // 12. Set the [[extractable]] internal slot of publicKey to true.
    public_key->set_extractable(true);

    // 13. Set the [[usages]] internal slot of publicKey to be the usage intersection of usages and [ "encrypt", "wrapKey" ].
    public_key->set_usages(usage_intersection(key_usages, { { Bindings::KeyUsage::Encrypt, Bindings::KeyUsage::Wrapkey } }));

    // 14. Let privateKey be a new CryptoKey representing the private key of the generated key pair.
    auto private_key = CryptoKey::create(m_realm, CryptoKey::InternalKeyData { key_pair.private_key });

    // 15. Set the [[type]] internal slot of privateKey to "private"
    private_key->set_type(Bindings::KeyType::Private);

    // 16. Set the [[algorithm]] internal slot of privateKey to algorithm.
    private_key->set_algorithm(algorithm);

    // 17. Set the [[extractable]] internal slot of privateKey to extractable.
    private_key->set_extractable(extractable);

    // 18. Set the [[usages]] internal slot of privateKey to be the usage intersection of usages and [ "decrypt", "unwrapKey" ].
    private_key->set_usages(usage_intersection(key_usages, { { Bindings::KeyUsage::Decrypt, Bindings::KeyUsage::Unwrapkey } }));

    // 19. Let result be a new CryptoKeyPair dictionary.
    // 20. Set the publicKey attribute of result to be publicKey.
    // 21. Set the privateKey attribute of result to be privateKey.
    // 22. Return the result of converting result to an ECMAScript Object, as defined by [WebIDL].
    return Variant<JS::NonnullGCPtr<CryptoKey>, JS::NonnullGCPtr<CryptoKeyPair>> { CryptoKeyPair::create(m_realm, public_key, private_key) };
}

// https://w3c.github.io/webcrypto/#rsa-oaep-operations
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Object>> RSAOAEP::export_key(Bindings::KeyFormat format, JS::NonnullGCPtr<CryptoKey> key)
{
    auto& realm = m_realm;
    auto& vm = realm.vm();

    // 1. Let key be the key to be exported.

    // 2. If the underlying cryptographic key material represented by the [[handle]] internal slot of key cannot be accessed, then throw an OperationError.
    // Note: In our impl this is always accessible
    auto const& handle = key->handle();

    JS::GCPtr<JS::Object> result = nullptr;

    // 3. If format is "spki"
    if (format == Bindings::KeyFormat::Spki) {
        // 1. If the [[type]] internal slot of key is not "public", then throw an InvalidAccessError.
        if (key->type() != Bindings::KeyType::Public)
            return WebIDL::InvalidAccessError::create(realm, "Key is not public"_fly_string);

        // FIXME: 2. Let data be an instance of the subjectPublicKeyInfo ASN.1 structure defined in [RFC5280] with the following properties:
        // - Set the algorithm field to an AlgorithmIdentifier ASN.1 type with the following properties:
        //   - Set the algorithm field to the OID rsaEncryption defined in [RFC3447].
        //   - Set the params field to the ASN.1 type NULL.
        // - Set the subjectPublicKey field to the result of DER-encoding an RSAPublicKey ASN.1 type, as defined in [RFC3447], Appendix A.1.1,
        //   that represents the RSA public key represented by the [[handle]] internal slot of key

        // FIXME: 3. Let result be the result of creating an ArrayBuffer containing data.
        result = JS::ArrayBuffer::create(realm, TRY_OR_THROW_OOM(vm, ByteBuffer::copy(("FIXME"sv).bytes())));
    }

    // FIXME: If format is "pkcs8"

    // If format is "jwk"
    else if (format == Bindings::KeyFormat::Jwk) {
        // 1. Let jwk be a new JsonWebKey dictionary.
        Bindings::JsonWebKey jwk = {};

        // 2. Set the kty attribute of jwk to the string "RSA".
        jwk.kty = "RSA"_string;

        // 4. Let hash be the name attribute of the hash attribute of the [[algorithm]] internal slot of key.
        auto hash = TRY(verify_cast<RsaHashedKeyAlgorithm>(*key->algorithm()).hash().visit([](String const& name) -> JS::ThrowCompletionOr<String> { return name; }, [&](JS::Handle<JS::Object> const& obj) -> JS::ThrowCompletionOr<String> {
                auto name_property = TRY(obj->get("name"));
                return name_property.to_string(realm.vm()); }));

        // 4. If hash is "SHA-1":
        //      - Set the alg attribute of jwk to the string "RSA-OAEP".
        if (hash == "SHA-1"sv) {
            jwk.alg = "RSA-OAEP"_string;
        }
        //    If hash is "SHA-256":
        //      - Set the alg attribute of jwk to the string "RSA-OAEP-256".
        else if (hash == "SHA-256"sv) {
            jwk.alg = "RSA-OAEP-256"_string;
        }
        //    If hash is "SHA-384":
        //      - Set the alg attribute of jwk to the string "RSA-OAEP-384".
        else if (hash == "SHA-384"sv) {
            jwk.alg = "RSA-OAEP-384"_string;
        }
        //    If hash is "SHA-512":
        //      - Set the alg attribute of jwk to the string "RSA-OAEP-512".
        else if (hash == "SHA-512"sv) {
            jwk.alg = "RSA-OAEP-512"_string;
        } else {
            // FIXME: Support 'other applicable specifications'
            // - Perform any key export steps defined by other applicable specifications,
            //   passing format and the hash attribute of the [[algorithm]] internal slot of key and obtaining alg.
            // - Set the alg attribute of jwk to alg.
            return WebIDL::NotSupportedError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted("Unsupported hash algorithm '{}'", hash)));
        }

        // 10. Set the attributes n and e of jwk according to the corresponding definitions in JSON Web Algorithms [JWA], Section 6.3.1.
        auto maybe_error = handle.visit(
            [&](::Crypto::PK::RSAPublicKey<> const& public_key) -> ErrorOr<void> {
                jwk.n = TRY(base64_url_uint_encode(public_key.modulus()));
                jwk.e = TRY(base64_url_uint_encode(public_key.public_exponent()));
                return {};
            },
            [&](::Crypto::PK::RSAPrivateKey<> const& private_key) -> ErrorOr<void> {
                jwk.n = TRY(base64_url_uint_encode(private_key.modulus()));
                jwk.e = TRY(base64_url_uint_encode(private_key.public_exponent()));

                // 11. If the [[type]] internal slot of key is "private":
                //    1. Set the attributes named d, p, q, dp, dq, and qi of jwk according to the corresponding definitions in JSON Web Algorithms [JWA], Section 6.3.2.
                jwk.d = TRY(base64_url_uint_encode(private_key.private_exponent()));
                // FIXME: Add p, q, dq, qi

                // 12. If the underlying RSA private key represented by the [[handle]] internal slot of key is represented by more than two primes,
                //     set the attribute named oth of jwk according to the corresponding definition in JSON Web Algorithms [JWA], Section 6.3.2.7
                // FIXME: We don't support more than 2 primes on RSA keys
                return {};
            },
            [](auto) -> ErrorOr<void> {
                VERIFY_NOT_REACHED();
            });
        // FIXME: clang-format butchers the visit if we do the TRY inline
        TRY_OR_THROW_OOM(vm, maybe_error);

        // 13. Set the key_ops attribute of jwk to the usages attribute of key.
        jwk.key_ops = Vector<String> {};
        jwk.key_ops->ensure_capacity(key->internal_usages().size());
        for (auto const& usage : key->internal_usages()) {
            jwk.key_ops->append(Bindings::idl_enum_to_string(usage));
        }

        // 14. Set the ext attribute of jwk to the [[extractable]] internal slot of key.
        jwk.ext = key->extractable();

        // 15. Let result be the result of converting jwk to an ECMAScript Object, as defined by [WebIDL].
        result = TRY(jwk.to_object(realm));
    }

    // Otherwise throw a NotSupportedError.
    else {
        return WebIDL::NotSupportedError::create(realm, TRY_OR_THROW_OOM(vm, String::formatted("Exporting to format {} is not supported", Bindings::idl_enum_to_string(format))));
    }

    // 8. Return result
    return JS::NonnullGCPtr { *result };
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<CryptoKey>> PBKDF2::import_key(AlgorithmParams const&, Bindings::KeyFormat format, CryptoKey::InternalKeyData key_data, bool extractable, Vector<Bindings::KeyUsage> const& key_usages)
{
    // 1. If format is not "raw", throw a NotSupportedError
    if (format != Bindings::KeyFormat::Raw) {
        return WebIDL::NotSupportedError::create(m_realm, "Only raw format is supported"_fly_string);
    }

    // 2. If usages contains a value that is not "deriveKey" or "deriveBits", then throw a SyntaxError.
    for (auto& usage : key_usages) {
        if (usage != Bindings::KeyUsage::Derivekey && usage != Bindings::KeyUsage::Derivebits) {
            return WebIDL::SyntaxError::create(m_realm, MUST(String::formatted("Invalid key usage '{}'", idl_enum_to_string(usage))));
        }
    }

    // 3. If extractable is not false, then throw a SyntaxError.
    if (extractable)
        return WebIDL::SyntaxError::create(m_realm, "extractable must be false"_fly_string);

    // 4. Let key be a new CryptoKey representing keyData.
    auto key = CryptoKey::create(m_realm, move(key_data));

    // 5. Set the [[type]] internal slot of key to "secret".
    key->set_type(Bindings::KeyType::Secret);

    // 6. Set the [[extractable]] internal slot of key to false.
    key->set_extractable(false);

    // 7. Let algorithm be a new KeyAlgorithm object.
    auto algorithm = KeyAlgorithm::create(m_realm);

    // 8. Set the name attribute of algorithm to "PBKDF2".
    algorithm->set_name("PBKDF2"_string);

    // 9. Set the [[algorithm]] internal slot of key to algorithm.
    key->set_algorithm(algorithm);

    // 10. Return key.
    return key;
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::ArrayBuffer>> SHA::digest(AlgorithmParams const& algorithm, ByteBuffer const& data)
{
    auto& algorithm_name = algorithm.name;

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
        return WebIDL::NotSupportedError::create(m_realm, MUST(String::formatted("Invalid hash function '{}'", algorithm_name)));
    }

    ::Crypto::Hash::Manager hash { hash_kind };
    hash.update(data);

    auto digest = hash.digest();
    auto result_buffer = ByteBuffer::copy(digest.immutable_data(), hash.digest_size());
    if (result_buffer.is_error())
        return WebIDL::OperationError::create(m_realm, "Failed to create result buffer"_fly_string);

    return JS::ArrayBuffer::create(m_realm, result_buffer.release_value());
}

}
