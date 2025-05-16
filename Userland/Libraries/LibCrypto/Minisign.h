/*
 * Copyright (c) 2025, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Forward.h>
#include <AK/String.h>
#include <LibCore/SecretString.h>
#include <LibCrypto/Hash/BLAKE2b.h>

// Minisign signature and key file format support.
// Documentation: https://jedisct1.github.io/minisign/
namespace Crypto::Minisign {

using KeyID = Array<u8, 8>;

class Signature;
class PublicKey;
class SecretKey;

// Pre-hashed (`ED`) signature format of minisign.
class Signature final {
    friend class SecretKey;

public:
    // Reads the on-disk signature file format.
    static ErrorOr<Signature> from_signature_file(StringView signature_file_data);
    ErrorOr<ByteString> to_signature_file() const;
    // Checks that the public key has the same ID as the one that was used to create this signature.
    // This is not a guarantee that the signature was created by this key!
    bool matches_public_key(PublicKey const&) const;

    constexpr String const& untrusted_comment() const { return m_untrusted_comment; }
    constexpr String const& trusted_comment() const { return m_trusted_comment; }
    constexpr ReadonlyBytes file_signature() const { return m_file_signature; }
    constexpr ReadonlyBytes global_signature() const { return m_global_signature; }
    constexpr KeyID key_id() const { return m_key_id; }

    constexpr String& untrusted_comment() { return m_untrusted_comment; }
    constexpr String& trusted_comment() { return m_trusted_comment; }
    constexpr void set_key_id(KeyID id) { m_key_id = id; }

    // The data signed with the global signature.
    ErrorOr<ByteBuffer> global_data() const;

private:
    Signature(
        String untrusted_comment,
        String trusted_comment,
        ByteBuffer file_signature,
        ByteBuffer global_signature,
        KeyID key_id)
        : m_untrusted_comment(move(untrusted_comment))
        , m_trusted_comment(move(trusted_comment))
        , m_file_signature(move(file_signature))
        , m_global_signature(move(global_signature))
        , m_key_id(key_id)
    {
    }

    String m_untrusted_comment {};
    String m_trusted_comment {};
    ByteBuffer m_file_signature;
    ByteBuffer m_global_signature;
    KeyID m_key_id;
};

// The three different kinds of result from minisig signature verification,
// as we have *two* hashes which can be valid and invalid slightly independently.
enum class VerificationResult : u8 {
    // Both signatures are invalid.
    Invalid,
    // Global signature and file signatures are valid.
    Valid,
    // File signature is valid, global signature is invalid.
    GlobalSignatureInvalid,
};

// Ed25519 public key.
class PublicKey final {
    friend class SecretKey;

public:
    // Reads the on-disk public key file format.
    static ErrorOr<PublicKey> from_public_key_file(StringView key_file_data);
    // The base64 public key string doesn’t have an untrusted comment, so that field will always be empty.
    static ErrorOr<PublicKey> from_base64(ReadonlyBytes key);

    ErrorOr<ByteString> to_public_key_file() const;

    // Verify that the signature matches the given contents with this key.
    ErrorOr<VerificationResult> verify(Signature const& signature, Stream& contents) const;

    bool matches_secret_key(SecretKey const&) const;

    constexpr KeyID id() const { return m_id; }
    constexpr String const& untrusted_comment() const { return m_untrusted_comment; }
    void set_untrusted_comment(String comment) { m_untrusted_comment = move(comment); }
    constexpr ReadonlyBytes public_key() const { return m_public_key.span(); }

private:
    PublicKey(String untrusted_comment, KeyID id, ByteBuffer public_key)
        : m_untrusted_comment(move(untrusted_comment))
        , m_id(id)
        , m_public_key(move(public_key))
    {
    }

    String m_untrusted_comment {};
    KeyID m_id;
    ByteBuffer m_public_key;
};

// Ed25519 Secret key without PKDF.
// FIXME: Implement Scrypt-based password encryption of the secret keys.
class SecretKey final {
public:
    // Reads the on-disk secret key file format.
    static ErrorOr<SecretKey> from_secret_key_file(Core::SecretString const& key_file);
    ErrorOr<Core::SecretString> to_secret_key_file() const;

    // Generates a new key pair.
    static ErrorOr<SecretKey> generate();

    ErrorOr<Signature> sign(Stream& contents, String const& untrusted_comment, String const& trusted_comment) const;

    operator PublicKey() const { return { m_untrusted_comment, m_id, m_public_key }; }

    constexpr ReadonlyBytes public_key() const { return m_public_key.bytes(); }
    constexpr String const& untrusted_comment() const { return m_untrusted_comment; }

private:
    SecretKey(
        String untrusted_comment,
        KeyID id,
        ByteBuffer public_key,
        Core::SecretString secret_key,
        Array<u8, 32> checksum)
        : m_untrusted_comment(move(untrusted_comment))
        , m_id(id)
        , m_public_key(move(public_key))
        , m_secret_key(move(secret_key))
        , m_checksum(checksum)
    {
    }

    String m_untrusted_comment;
    KeyID m_id;
    ByteBuffer m_public_key;
    Core::SecretString m_secret_key;
    // Checksummed using BLAKE2b-256.
    // FIXME: We don’t have an implementation for this BLAKE2b variant yet.
    Array<u8, 32> m_checksum;
};

}
