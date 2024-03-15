/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/QuickSort.h>
#include <LibCrypto/ASN1/DER.h>
#include <LibCrypto/Hash/HashManager.h>
#include <LibCrypto/PK/RSA.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibTLS/Certificate.h>
#include <LibWeb/Crypto/CryptoAlgorithms.h>
#include <LibWeb/Crypto/KeyAlgorithms.h>
#include <LibWeb/Crypto/SubtleCrypto.h>
#include <LibWeb/WebIDL/AbstractOperations.h>

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

WebIDL::ExceptionOr<::Crypto::UnsignedBigInteger> base64_url_uint_decode(JS::Realm& realm, String const& base64_url_string)
{
    auto& vm = realm.vm();
    static_assert(AK::HostIsLittleEndian, "This code assumes little-endian");

    // FIXME: Create a version of decode_base64url that ignores padding inconsistencies
    auto padded_string = base64_url_string;
    if (padded_string.byte_count() % 4 != 0) {
        padded_string = TRY_OR_THROW_OOM(vm, String::formatted("{}{}", padded_string, TRY_OR_THROW_OOM(vm, String::repeated('=', 4 - (padded_string.byte_count() % 4)))));
    }

    auto base64_bytes_or_error = decode_base64url(padded_string);
    if (base64_bytes_or_error.is_error()) {
        if (base64_bytes_or_error.error().code() == ENOMEM)
            return vm.throw_completion<JS::InternalError>(vm.error_message(::JS::VM::ErrorMessage::OutOfMemory));
        return WebIDL::DataError::create(realm, MUST(String::formatted("base64 decode: {}", base64_bytes_or_error.release_error())));
    }
    auto base64_bytes = base64_bytes_or_error.release_value();

    // We need to swap the integer's big-endian representation to little endian in order to import it
    Vector<u8, 32> byte_swapped_data;
    byte_swapped_data.ensure_capacity(base64_bytes.size());
    for (size_t i = 0; i < base64_bytes.size(); ++i)
        byte_swapped_data.append(base64_bytes[base64_bytes.size() - i - 1]);

    return ::Crypto::UnsignedBigInteger::import_data(byte_swapped_data.data(), byte_swapped_data.size());
}

// https://w3c.github.io/webcrypto/#concept-parse-an-asn1-structure
template<typename Structure>
static WebIDL::ExceptionOr<Structure> parse_an_ASN1_structure(JS::Realm& realm, ReadonlyBytes data, bool exact_data = true)
{
    // 1. Let data be a sequence of bytes to be parsed.
    // 2. Let structure be the ASN.1 structure to be parsed.
    // 3. Let exactData be an optional boolean value. If it is not supplied, let it be initialized to true.

    // 4. Parse data according to the Distinguished Encoding Rules of [X690], using structure as the ASN.1 structure to be decoded.
    ::Crypto::ASN1::Decoder decoder(data);
    Structure structure;
    if constexpr (IsSame<Structure, TLS::SubjectPublicKey>) {
        auto maybe_subject_public_key = TLS::parse_subject_public_key_info(decoder);
        if (maybe_subject_public_key.is_error())
            return WebIDL::DataError::create(realm, MUST(String::formatted("Error parsing subjectPublicKeyInfo: {}", maybe_subject_public_key.release_error())));
        structure = maybe_subject_public_key.release_value();
    } else if constexpr (IsSame<Structure, TLS::PrivateKey>) {
        auto maybe_private_key = TLS::parse_private_key_info(decoder);
        if (maybe_private_key.is_error())
            return WebIDL::DataError::create(realm, MUST(String::formatted("Error parsing privateKeyInfo: {}", maybe_private_key.release_error())));
        structure = maybe_private_key.release_value();
    } else {
        static_assert(DependentFalse<Structure>, "Don't know how to parse ASN.1 structure type");
    }

    // 5. If exactData was specified, and all of the bytes of data were not consumed during the parsing phase, then throw a DataError.
    if (exact_data && !decoder.eof())
        return WebIDL::DataError::create(realm, "Not all bytes were consumed during the parsing phase"_fly_string);

    // 6. Return the parsed ASN.1 structure.
    return structure;
}

// https://w3c.github.io/webcrypto/#concept-parse-a-spki
static WebIDL::ExceptionOr<TLS::SubjectPublicKey> parse_a_subject_public_key_info(JS::Realm& realm, ReadonlyBytes bytes)
{
    // When this specification says to parse a subjectPublicKeyInfo, the user agent must parse an ASN.1 structure,
    // with data set to the sequence of bytes to be parsed, structure as the ASN.1 structure of subjectPublicKeyInfo,
    // as specified in [RFC5280], and exactData set to true.
    return parse_an_ASN1_structure<TLS::SubjectPublicKey>(realm, bytes, true);
}

// https://w3c.github.io/webcrypto/#concept-parse-a-privateKeyInfo
static WebIDL::ExceptionOr<TLS::PrivateKey> parse_a_private_key_info(JS::Realm& realm, ReadonlyBytes bytes)
{
    // When this specification says to parse a PrivateKeyInfo, the user agent must parse an ASN.1 structure
    // with data set to the sequence of bytes to be parsed, structure as the ASN.1 structure of PrivateKeyInfo,
    // as specified in [RFC5208], and exactData set to true.
    return parse_an_ASN1_structure<TLS::PrivateKey>(realm, bytes, true);
}

static WebIDL::ExceptionOr<::Crypto::PK::RSAPrivateKey<>> parse_jwk_rsa_private_key(JS::Realm& realm, Bindings::JsonWebKey const& jwk)
{
    auto n = TRY(base64_url_uint_decode(realm, *jwk.n));
    auto d = TRY(base64_url_uint_decode(realm, *jwk.d));
    auto e = TRY(base64_url_uint_decode(realm, *jwk.e));

    // We know that if any of the extra parameters are provided, all of them must be
    if (!jwk.p.has_value())
        return ::Crypto::PK::RSAPrivateKey<>(move(n), move(d), move(e), 0, 0);

    auto p = TRY(base64_url_uint_decode(realm, *jwk.p));
    auto q = TRY(base64_url_uint_decode(realm, *jwk.q));
    auto dp = TRY(base64_url_uint_decode(realm, *jwk.dp));
    auto dq = TRY(base64_url_uint_decode(realm, *jwk.dq));
    auto qi = TRY(base64_url_uint_decode(realm, *jwk.qi));

    return ::Crypto::PK::RSAPrivateKey<>(move(n), move(d), move(e), move(p), move(q), move(dp), move(dq), move(qi));
}

static WebIDL::ExceptionOr<::Crypto::PK::RSAPublicKey<>> parse_jwk_rsa_public_key(JS::Realm& realm, Bindings::JsonWebKey const& jwk)
{
    auto e = TRY(base64_url_uint_decode(realm, *jwk.e));
    auto n = TRY(base64_url_uint_decode(realm, *jwk.n));

    return ::Crypto::PK::RSAPublicKey<>(move(n), move(e));
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
    auto& object = value.as_object();

    auto name_value = TRY(object.get("name"));
    auto name = TRY(name_value.to_string(vm));

    auto salt_value = TRY(object.get("salt"));

    if (!salt_value.is_object() || !(is<JS::TypedArrayBase>(salt_value.as_object()) || is<JS::ArrayBuffer>(salt_value.as_object()) || is<JS::DataView>(salt_value.as_object())))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "BufferSource");

    auto salt = TRY_OR_THROW_OOM(vm, WebIDL::get_buffer_source_copy(salt_value.as_object()));

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

RsaHashedImportParams::~RsaHashedImportParams() = default;

JS::ThrowCompletionOr<NonnullOwnPtr<AlgorithmParams>> RsaHashedImportParams::from_value(JS::VM& vm, JS::Value value)
{
    auto& object = value.as_object();

    auto name_value = TRY(object.get("name"));
    auto name = TRY(name_value.to_string(vm));

    auto hash_value = TRY(object.get("hash"));
    auto hash = Variant<Empty, HashAlgorithmIdentifier> { Empty {} };
    if (hash_value.is_string()) {
        auto hash_string = TRY(hash_value.to_string(vm));
        hash = HashAlgorithmIdentifier { hash_string };
    } else {
        auto hash_object = TRY(hash_value.to_object(vm));
        hash = HashAlgorithmIdentifier { hash_object };
    }

    return adopt_own<AlgorithmParams>(*new RsaHashedImportParams { name, hash.get<HashAlgorithmIdentifier>() });
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
WebIDL::ExceptionOr<JS::NonnullGCPtr<CryptoKey>> RSAOAEP::import_key(Web::Crypto::AlgorithmParams const& params, Bindings::KeyFormat key_format, CryptoKey::InternalKeyData key_data, bool extractable, Vector<Bindings::KeyUsage> const& usages)
{
    auto& realm = m_realm;

    // 1. Let keyData be the key data to be imported.

    JS::GCPtr<CryptoKey> key = nullptr;
    auto const& normalized_algorithm = static_cast<RsaHashedImportParams const&>(params);

    // 2. -> If format is "spki":
    if (key_format == Bindings::KeyFormat::Spki) {
        // 1. If usages contains an entry which is not "encrypt" or "wrapKey", then throw a SyntaxError.
        for (auto const& usage : usages) {
            if (usage != Bindings::KeyUsage::Encrypt && usage != Bindings::KeyUsage::Wrapkey) {
                return WebIDL::SyntaxError::create(m_realm, MUST(String::formatted("Invalid key usage '{}'", idl_enum_to_string(usage))));
            }
        }

        VERIFY(key_data.has<ByteBuffer>());

        // 2. Let spki be the result of running the parse a subjectPublicKeyInfo algorithm over keyData.
        // 3. If an error occurred while parsing, then throw a DataError.
        auto spki = TRY(parse_a_subject_public_key_info(m_realm, key_data.get<ByteBuffer>()));

        // 4. If the algorithm object identifier field of the algorithm AlgorithmIdentifier field of spki
        //    is not equal to the rsaEncryption object identifier defined in [RFC3447], then throw a DataError.
        if (spki.algorithm.identifier != TLS::rsa_encryption_oid)
            return WebIDL::DataError::create(m_realm, "Algorithm object identifier is not the rsaEncryption object identifier"_fly_string);

        // 5. Let publicKey be the result of performing the parse an ASN.1 structure algorithm,
        //    with data as the subjectPublicKeyInfo field of spki, structure as the RSAPublicKey structure
        //    specified in Section A.1.1 of [RFC3447], and exactData set to true.
        // NOTE: We already did this in parse_a_subject_public_key_info
        auto& public_key = spki.rsa;

        // 6. If an error occurred while parsing, or it can be determined that publicKey is not
        //    a valid public key according to [RFC3447], then throw a DataError.
        // FIXME: Validate the public key

        // 7. Let key be a new CryptoKey that represents the RSA public key identified by publicKey.
        key = CryptoKey::create(m_realm, CryptoKey::InternalKeyData { public_key });

        // 8. Set the [[type]] internal slot of key to "public"
        key->set_type(Bindings::KeyType::Public);
    }

    // -> If format is "pkcs8":
    else if (key_format == Bindings::KeyFormat::Pkcs8) {
        // 1. If usages contains an entry which is not "decrypt" or "unwrapKey", then throw a SyntaxError.
        for (auto const& usage : usages) {
            if (usage != Bindings::KeyUsage::Decrypt && usage != Bindings::KeyUsage::Unwrapkey) {
                return WebIDL::SyntaxError::create(m_realm, MUST(String::formatted("Invalid key usage '{}'", idl_enum_to_string(usage))));
            }
        }

        VERIFY(key_data.has<ByteBuffer>());

        // 2. Let privateKeyInfo be the result of running the parse a privateKeyInfo algorithm over keyData.
        // 3. If an error occurred while parsing, then throw a DataError.
        auto private_key_info = TRY(parse_a_private_key_info(m_realm, key_data.get<ByteBuffer>()));

        // 4. If the algorithm object identifier field of the privateKeyAlgorithm PrivateKeyAlgorithm field of privateKeyInfo
        //    is not equal to the rsaEncryption object identifier defined in [RFC3447], then throw a DataError.
        if (private_key_info.algorithm.identifier != TLS::rsa_encryption_oid)
            return WebIDL::DataError::create(m_realm, "Algorithm object identifier is not the rsaEncryption object identifier"_fly_string);

        // 5. Let rsaPrivateKey be the result of performing the parse an ASN.1 structure algorithm,
        //    with data as the privateKey field of privateKeyInfo, structure as the RSAPrivateKey structure
        //    specified in Section A.1.2 of [RFC3447], and exactData set to true.
        // NOTE: We already did this in parse_a_private_key_info
        auto& rsa_private_key = private_key_info.rsa;

        // 6. If an error occurred while parsing, or if rsaPrivateKey is not
        //    a valid RSA private key according to [RFC3447], then throw a DataError.
        // FIXME: Validate the private key

        // 7. Let key be a new CryptoKey that represents the RSA private key identified by rsaPrivateKey.
        key = CryptoKey::create(m_realm, CryptoKey::InternalKeyData { rsa_private_key });

        // 8. Set the [[type]] internal slot of key to "private"
        key->set_type(Bindings::KeyType::Private);
    }

    // -> If format is "jwk":
    else if (key_format == Bindings::KeyFormat::Jwk) {
        // 1. -> If keyData is a JsonWebKey dictionary:
        //         Let jwk equal keyData.
        //    -> Otherwise:
        //         Throw a DataError.
        if (!key_data.has<Bindings::JsonWebKey>())
            return WebIDL::DataError::create(m_realm, "keyData is not a JsonWebKey dictionary"_fly_string);
        auto& jwk = key_data.get<Bindings::JsonWebKey>();

        // 2. If the d field of jwk is present and usages contains an entry which is not "decrypt" or "unwrapKey", then throw a SyntaxError.
        if (jwk.d.has_value()) {
            for (auto const& usage : usages) {
                if (usage != Bindings::KeyUsage::Decrypt && usage != Bindings::KeyUsage::Unwrapkey) {
                    return WebIDL::SyntaxError::create(m_realm, MUST(String::formatted("Invalid key usage '{}'", Bindings::idl_enum_to_string(usage))));
                }
            }
        }

        // 3. If the d field of jwk is not present and usages contains an entry which is not "encrypt" or "wrapKey", then throw a SyntaxError.
        if (!jwk.d.has_value()) {
            for (auto const& usage : usages) {
                if (usage != Bindings::KeyUsage::Encrypt && usage != Bindings::KeyUsage::Wrapkey) {
                    return WebIDL::SyntaxError::create(m_realm, MUST(String::formatted("Invalid key usage '{}'", Bindings::idl_enum_to_string(usage))));
                }
            }
        }

        // 4. If the kty field of jwk is not a case-sensitive string match to "RSA", then throw a DataError.
        if (jwk.kty != "RSA"_string)
            return WebIDL::DataError::create(m_realm, "Invalid key type"_fly_string);

        // 5. If usages is non-empty and the use field of jwk is present and is not a case-sensitive string match to "enc", then throw a DataError.
        if (!usages.is_empty() && jwk.use.has_value() && *jwk.use != "enc"_string)
            return WebIDL::DataError::create(m_realm, "Invalid use field"_fly_string);

        // 6. If the key_ops field of jwk is present, and is invalid according to the requirements of JSON Web Key [JWK]
        //    or does not contain all of the specified usages values, then throw a DataError.
        for (auto const& usage : usages) {
            if (!jwk.key_ops->contains_slow(Bindings::idl_enum_to_string(usage)))
                return WebIDL::DataError::create(m_realm, MUST(String::formatted("Missing key_ops field: {}", Bindings::idl_enum_to_string(usage))));
        }
        // FIXME: Validate jwk.key_ops against requirements in https://www.rfc-editor.org/rfc/rfc7517#section-4.3

        // 7. If the ext field of jwk is present and has the value false and extractable is true, then throw a DataError.
        if (jwk.ext.has_value() && !*jwk.ext && extractable)
            return WebIDL::DataError::create(m_realm, "Invalid ext field"_fly_string);

        Optional<String> hash = {};
        // 8. -> If the alg field of jwk is not present:
        if (!jwk.alg.has_value()) {
            //     Let hash be undefined.
        }
        //    ->  If the alg field of jwk is equal to "RSA-OAEP":
        if (jwk.alg == "RSA-OAEP"sv) {
            //     Let hash be the string "SHA-1".
            hash = "SHA-1"_string;
        }
        //    -> If the alg field of jwk is equal to "RSA-OAEP-256":
        else if (jwk.alg == "RSA-OAEP-256"sv) {
            //     Let hash be the string "SHA-256".
            hash = "SHA-256"_string;
        }
        //    -> If the alg field of jwk is equal to "RSA-OAEP-384":
        else if (jwk.alg == "RSA-OAEP-384"sv) {
            //     Let hash be the string "SHA-384".
            hash = "SHA-384"_string;
        }
        //    -> If the alg field of jwk is equal to "RSA-OAEP-512":
        else if (jwk.alg == "RSA-OAEP-512"sv) {
            //     Let hash be the string "SHA-512".
            hash = "SHA-512"_string;
        }
        //    -> Otherwise:
        else {
            // FIXME: Support 'other applicable specifications'
            // 1. Perform any key import steps defined by other applicable specifications, passing format, jwk and obtaining hash.
            // 2. If an error occurred or there are no applicable specifications, throw a DataError.
            return WebIDL::DataError::create(m_realm, "Invalid alg field"_fly_string);
        }

        // 9.  If hash is not undefined:
        if (hash.has_value()) {
            // 1. Let normalizedHash be the result of normalize an algorithm with alg set to hash and op set to digest.
            auto normalized_hash = TRY(normalize_an_algorithm(m_realm, AlgorithmIdentifier { *hash }, "digest"_string));

            // 2. If normalizedHash is not equal to the hash member of normalizedAlgorithm, throw a DataError.
            if (normalized_hash.parameter->name != TRY(normalized_algorithm.hash.visit([](String const& name) -> JS::ThrowCompletionOr<String> { return name; }, [&](JS::Handle<JS::Object> const& obj) -> JS::ThrowCompletionOr<String> {
                        auto name_property = TRY(obj->get("name"));
                        return name_property.to_string(m_realm.vm()); })))
                return WebIDL::DataError::create(m_realm, "Invalid hash"_fly_string);
        }

        // 10. -> If the d field of jwk is present:
        if (jwk.d.has_value()) {
            // 1. If jwk does not meet the requirements of Section 6.3.2 of JSON Web Algorithms [JWA], then throw a DataError.
            bool meets_requirements = jwk.e.has_value() && jwk.n.has_value() && jwk.d.has_value();
            if (jwk.p.has_value() || jwk.q.has_value() || jwk.dp.has_value() || jwk.dq.has_value() || jwk.qi.has_value())
                meets_requirements |= jwk.p.has_value() && jwk.q.has_value() && jwk.dp.has_value() && jwk.dq.has_value() && jwk.qi.has_value();

            if (jwk.oth.has_value()) {
                // FIXME: We don't support > 2 primes in RSA keys
                meets_requirements = false;
            }

            if (!meets_requirements)
                return WebIDL::DataError::create(m_realm, "Invalid JWK private key"_fly_string);

            // FIXME: Spec error, it should say 'the RSA private key identified by interpreting jwk according to section 6.3.2'
            // 2. Let privateKey represent the RSA public key identified by interpreting jwk according to Section 6.3.1 of JSON Web Algorithms [JWA].
            auto private_key = TRY(parse_jwk_rsa_private_key(realm, jwk));

            // FIXME: Spec error, it should say 'not to be a valid RSA private key'
            // 3. If privateKey can be determined to not be a valid RSA public key according to [RFC3447], then throw a DataError.
            // FIXME: Validate the private key

            // 4. Let key be a new CryptoKey representing privateKey.
            key = CryptoKey::create(m_realm, CryptoKey::InternalKeyData { private_key });

            // 5. Set the [[type]] internal slot of key to "private"
            key->set_type(Bindings::KeyType::Private);
        }

        //     -> Otherwise:
        else {
            // 1. If jwk does not meet the requirements of Section 6.3.1 of JSON Web Algorithms [JWA], then throw a DataError.
            if (!jwk.e.has_value() || !jwk.n.has_value())
                return WebIDL::DataError::create(m_realm, "Invalid JWK public key"_fly_string);

            // 2. Let publicKey represent the RSA public key identified by interpreting jwk according to Section 6.3.1 of JSON Web Algorithms [JWA].
            auto public_key = TRY(parse_jwk_rsa_public_key(realm, jwk));

            // 3. If publicKey can be determined to not be a valid RSA public key according to [RFC3447], then throw a DataError.
            // FIXME: Validate the public key

            // 4. Let key be a new CryptoKey representing publicKey.
            key = CryptoKey::create(m_realm, CryptoKey::InternalKeyData { public_key });

            // 5. Set the [[type]] internal slot of key to "public"
            key->set_type(Bindings::KeyType::Public);
        }
    }

    // -> Otherwise: throw a NotSupportedError.
    else {
        return WebIDL::NotSupportedError::create(m_realm, "Unsupported key format"_fly_string);
    }

    // 3. Let algorithm be a new RsaHashedKeyAlgorithm.
    auto algorithm = RsaHashedKeyAlgorithm::create(m_realm);

    // 4. Set the name attribute of algorithm to "RSA-OAEP"
    algorithm->set_name("RSA-OAEP"_string);

    // 5. Set the modulusLength attribute of algorithm to the length, in bits, of the RSA public modulus.
    // 6. Set the publicExponent attribute of algorithm to the BigInteger representation of the RSA public exponent.
    TRY(key->handle().visit(
        [&](::Crypto::PK::RSAPublicKey<> const& public_key) -> WebIDL::ExceptionOr<void> {
            algorithm->set_modulus_length(public_key.length());
            TRY(algorithm->set_public_exponent(public_key.public_exponent()));
            return {};
        },
        [&](::Crypto::PK::RSAPrivateKey<> const& private_key) -> WebIDL::ExceptionOr<void> {
            algorithm->set_modulus_length(private_key.length());
            TRY(algorithm->set_public_exponent(private_key.public_exponent()));
            return {};
        },
        [](auto) -> WebIDL::ExceptionOr<void> { VERIFY_NOT_REACHED(); }));

    // 7. Set the hash attribute of algorithm to the hash member of normalizedAlgorithm.
    algorithm->set_hash(normalized_algorithm.hash);

    // 8. Set the [[algorithm]] internal slot of key to algorithm
    key->set_algorithm(algorithm);

    // 9. Return key.
    return JS::NonnullGCPtr { *key };
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
                jwk.p = TRY(base64_url_uint_encode(private_key.prime1()));
                jwk.q = TRY(base64_url_uint_encode(private_key.prime2()));
                jwk.dp = TRY(base64_url_uint_encode(private_key.exponent1()));
                jwk.dq = TRY(base64_url_uint_encode(private_key.exponent2()));
                jwk.qi = TRY(base64_url_uint_encode(private_key.coefficient()));

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
