/*
 * Copyright (c) 2025, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Minisign.h"
#include <AK/Base64.h>
#include <AK/ByteReader.h>
#include <AK/Forward.h>
#include <AK/MemoryStream.h>
#include <AK/Random.h>
#include <AK/Stream.h>
#include <AK/StringView.h>
#include <AK/Try.h>
#include <Kernel/API/POSIX/sys/limits.h>
#include <LibCore/SecretString.h>
#include <LibCrypto/Curves/Curve25519.h>
#include <LibCrypto/Curves/Ed25519.h>
#include <LibCrypto/Hash/BLAKE2b.h>

namespace Crypto::Minisign {

constexpr StringView untrusted_comment_id = "untrusted comment: "sv;
constexpr StringView trusted_comment_id = "trusted comment: "sv;
// The `D` is capitalized to indicate the pre-hashed signature scheme, which is the only one we support.
constexpr StringView signature_algorithm_id = "ED"sv;
// The ID here is different from the signature algorithm ID since it wasn’t changed for the prehashed scheme (keys are valid for use with both schemes).
constexpr StringView key_signature_algorithm_id = "Ed"sv;
constexpr StringView scrypt_algorithm_id = "Sc"sv;
// BLAKE2b.
constexpr StringView checksum_algorithm_id = "B2"sv;

// Signature file format:
//
// untrusted comment: <arbitrary text>
// base64(<signature_algorithm> || <key_id> || <signature>)
// trusted_comment: <arbitrary text>
// base64(<global_signature>)
ErrorOr<Signature> Signature::from_signature_file(StringView signature_file_data)
{
    auto const lines = signature_file_data.split_view('\n', SplitBehavior::KeepEmpty);
    if (lines.size() < 4)
        return Error::from_string_view("Signature file has less than 4 lines"sv);

    auto const untrusted_comment_line = lines[0];
    auto const base64_file_signature_line = lines[1];
    auto const trusted_comment_line = lines[2];
    auto const base64_global_signature_line = lines[3];

    if (!untrusted_comment_line.starts_with(untrusted_comment_id))
        return Error::from_string_view("Untrusted comment line malformed"sv);
    auto const untrusted_comment = TRY(String::from_utf8(untrusted_comment_line.substring_view(untrusted_comment_id.length())));
    if (!trusted_comment_line.starts_with(trusted_comment_id))
        return Error::from_string_view("Trusted comment line malformed"sv);
    auto const trusted_comment = TRY(String::from_utf8(trusted_comment_line.substring_view(trusted_comment_id.length())));

    auto const file_signature_line = TRY(AK::decode_base64(base64_file_signature_line.trim_whitespace()));
    auto const global_signature = TRY(AK::decode_base64(base64_global_signature_line.trim_whitespace()));

    auto const signature_algorithm = file_signature_line.span().trim(2);
    if (StringView { signature_algorithm } != signature_algorithm_id)
        return Error::from_string_view("Unknown signature ID"sv);
    auto const key_id = file_signature_line.span().slice(2, 8);
    auto const file_signature = file_signature_line.span().slice(10);

    return Signature {
        untrusted_comment,
        trusted_comment,
        TRY(ByteBuffer::copy(file_signature)),
        global_signature,
        KeyID::from_span(key_id),
    };
}

ErrorOr<ByteString> Signature::to_signature_file() const
{
    auto file_signature_data = TRY(ByteBuffer::create_uninitialized(10 + m_file_signature.size()));
    signature_algorithm_id.bytes().copy_to(file_signature_data);
    m_key_id.span().copy_to(file_signature_data.span().slice(2));
    m_file_signature.span().copy_to(file_signature_data.span().slice(10));

    return ByteString::formatted("untrusted comment: {}\n{}\ntrusted comment: {}\n{}\n", m_untrusted_comment, TRY(AK::encode_base64(file_signature_data.bytes())), m_trusted_comment, TRY(AK::encode_base64(m_global_signature)));
}

// Public key file format:
//
// untrusted comment: <arbitrary text>
// base64(<signature_algorithm> || <key_id> || <public_key>)
ErrorOr<PublicKey> PublicKey::from_public_key_file(StringView key_file_data)
{
    auto const lines = key_file_data.split_view('\n', SplitBehavior::KeepEmpty);
    if (lines.size() < 2)
        return Error::from_string_view("Public key file has less than 2 lines"sv);

    auto const untrusted_comment_line = lines[0];
    auto const base64_public_key_line = lines[1];

    if (!untrusted_comment_line.starts_with(untrusted_comment_id))
        return Error::from_string_view("Untrusted comment line malformed"sv);
    auto const untrusted_comment = TRY(String::from_utf8(untrusted_comment_line.substring_view(untrusted_comment_id.length())));

    auto key = TRY(from_base64(base64_public_key_line.bytes()));
    key.m_untrusted_comment = untrusted_comment;
    return key;
}

ErrorOr<PublicKey> PublicKey::from_base64(ReadonlyBytes key)
{
    auto const public_key_line = TRY(AK::decode_base64(StringView { key }.trim_whitespace()));

    auto const signature_algorithm = public_key_line.span().trim(2);
    if (StringView { signature_algorithm } != key_signature_algorithm_id)
        return Error::from_string_view("Unknown algorithm ID"sv);
    auto const key_id = public_key_line.span().slice(2, 8);
    auto const public_key = public_key_line.span().slice(10);

    return PublicKey { {}, KeyID::from_span(key_id), TRY(ByteBuffer::copy(public_key)) };
}

ErrorOr<ByteString> PublicKey::to_public_key_file() const
{
    auto key_data = TRY(ByteBuffer::create_zeroed(10 + m_public_key.size()));
    key_signature_algorithm_id.bytes().copy_to(key_data);
    m_id.span().copy_to(key_data.span().slice(2));
    m_public_key.span().copy_to(key_data.span().slice(10));

    return ByteString::formatted("untrusted comment: {}\n{}\n", m_untrusted_comment, TRY(AK::encode_base64(key_data.bytes())));
}

ErrorOr<SecretKey> SecretKey::from_secret_key_file(Core::SecretString const& key_file)
{
    auto const lines = key_file.view().split_view('\n', SplitBehavior::KeepEmpty);
    if (lines.size() < 2)
        return Error::from_string_view("Secret key file has less than 2 lines"sv);

    auto const untrusted_comment_line = lines[0];
    auto const base64_secret_key_line = lines[1];

    if (!untrusted_comment_line.starts_with(untrusted_comment_id))
        return Error::from_string_view("Untrusted comment line malformed"sv);
    auto const untrusted_comment = TRY(String::from_utf8(untrusted_comment_line.substring_view(untrusted_comment_id.length())));

    auto const secret_key_line = TRY(AK::decode_base64(StringView { base64_secret_key_line }.trim_whitespace()));

    ReadonlyBytes reader { secret_key_line };

    auto const signature_algorithm = reader.trim(2);
    if (StringView { signature_algorithm } != key_signature_algorithm_id)
        return Error::from_string_view("Unknown algorithm ID"sv);
    reader = reader.slice(2);

    auto const kdf_algorithm = reader.trim(2);
    if (StringView { kdf_algorithm } == scrypt_algorithm_id)
        return Error::from_string_view("Scrypt KDF is not currently supported. Use a key without password protection."sv);
    if (StringView { kdf_algorithm } != "\0\0"sv)
        return Error::from_string_view("Unknown KDF ID"sv);
    reader = reader.slice(2);

    auto const checksum_algorithm = reader.trim(2);
    if (StringView { checksum_algorithm } != checksum_algorithm_id)
        return Error::from_string_view("Unknown checksum algorithm ID"sv);
    reader = reader.slice(2);

    auto const kdf_salt = reader.trim(32);
    reader = reader.slice(32);
    auto const kdf_opslimit = reader.trim(8);
    reader = reader.slice(8);
    auto const kdf_memlimit = reader.trim(8);
    reader = reader.slice(8);
    auto const key_id = reader.trim(8);
    reader = reader.slice(8);
    auto const temporary_secret_key = reader.trim(Curves::Ed25519 {}.key_size());
    auto secret_key = TRY(ByteBuffer::copy(temporary_secret_key));
    reader = reader.slice(Curves::Ed25519 {}.key_size());
    auto const public_key = reader.trim(Curves::Ed25519 {}.key_size());
    reader = reader.slice(Curves::Ed25519 {}.key_size());
    auto const checksum = reader.trim(Hash::BLAKE2b::DigestSize);

    // These are currently intentionally unused. When no KDF is in used, they’re zeroed out.
    (void)kdf_salt;
    (void)kdf_opslimit;
    (void)kdf_memlimit;

    return SecretKey {
        untrusted_comment,
        KeyID::from_span(key_id),
        TRY(ByteBuffer::copy(public_key)),
        Core::SecretString::take_ownership(move(secret_key)),
        Array<u8, 32>::from_span(checksum),
    };
}

ErrorOr<Core::SecretString> SecretKey::to_secret_key_file() const
{
    auto key_data = TRY(ByteBuffer::create_zeroed(6 + 32 + 8 + 8 + 104));
    // All unnecessary fields are simply left as zero.
    key_signature_algorithm_id.bytes().copy_to(key_data);
    checksum_algorithm_id.bytes().copy_to(key_data.span().slice(4));
    // 32 + 8 + 8 bytes of unused KDF data
    m_id.span().copy_to(key_data.span().slice(6 + 32 + 8 + 8));
    m_secret_key.view().bytes().copy_to(key_data.span().slice(6 + 32 + 8 + 8 + 8));
    m_public_key.span().copy_to(key_data.span().slice(6 + 32 + 8 + 8 + 8 + m_secret_key.length() - 1));
    // FIXME: Checksum seems to be empty for minisign created secret key files too... let’s hope that doesn’t cause any issues.

    return Core::SecretString::take_ownership(ByteString::formatted("untrusted comment: {}\n{}\n", m_untrusted_comment, TRY(AK::encode_base64(key_data.bytes()))).to_byte_buffer());
}

bool Signature::matches_public_key(PublicKey const& public_key) const { return public_key.id() == key_id(); }

bool PublicKey::matches_secret_key(SecretKey const& secret_key) const { return m_public_key == secret_key.public_key(); }

ErrorOr<ByteBuffer> Signature::global_data() const
{
    // global_signature = ed25519(<signature> || <trusted_comment>)
    auto global_data = TRY(ByteBuffer::create_uninitialized(m_file_signature.size() + m_trusted_comment.byte_count()));
    m_file_signature.span().copy_to(global_data);
    m_trusted_comment.bytes().copy_to(global_data.span().slice(m_file_signature.size()));
    return global_data;
}

static ErrorOr<Hash::BLAKE2b::DigestType> stream_hash(Stream& contents)
{
    Hash::BLAKE2b hash;
    // Strike some kind of balance between
    // - not allocating enough buffer space, which will yield frequent calls to update() and read() and be slow (e.g. Core::File issues one syscall per read)
    // - allocating too much buffer space or even reading in the entire file at once, which makes signing large files (common for software packages!) infeasible.
    // There may be a better tradeoff, 256 pages (~1MB) was chosen for a buffer size that’s most definitely allocatable.
    auto intermediate_buffer = TRY(ByteBuffer::create_uninitialized(256z * PAGE_SIZE));
    while (!contents.is_eof()) {
        auto const read_buffer = TRY(contents.read_some(intermediate_buffer));
        if (read_buffer.is_empty())
            continue;
        hash.update(read_buffer.data(), read_buffer.size());
    }
    return hash.digest();
}

ErrorOr<VerificationResult> PublicKey::verify(Signature const& signature, Stream& contents) const
{
    if (!signature.matches_public_key(*this))
        return VerificationResult::Invalid;

    auto const calculated_hash = TRY(stream_hash(contents));

    if (!Curves::Ed25519 {}.verify(m_public_key, signature.file_signature(), calculated_hash.bytes())) {
        // Note that from a UI perspective we want to skip checking the global signature, and mark both as invalid.
        // A valid trusted comment associated with an invalid file signature is basically useless.
        return VerificationResult::Invalid;
    }

    auto const global_data = TRY(signature.global_data());
    if (!Curves::Ed25519 {}.verify(m_public_key, signature.global_signature(), global_data.bytes()))
        return VerificationResult::GlobalSignatureInvalid;

    return VerificationResult::Valid;
}

ErrorOr<Signature> SecretKey::sign(Stream& contents, String const& untrusted_comment, String const& trusted_comment) const
{
    auto const hash = TRY(stream_hash(contents));

    // FIXME: The secret key length trim is an ugly workaround for SecretString adding a null byte to the end of all data.
    auto const file_signature = TRY(Curves::Ed25519 {}.sign(m_public_key, m_secret_key.view().bytes().trim(m_secret_key.length() - 1), hash.bytes()));

    // Fill out the global signature with an empty buffer at first so we can now use Signature’s utility function to sign the global data.
    Signature signature { untrusted_comment, trusted_comment, file_signature, {}, m_id };
    auto const global_data = TRY(signature.global_data());
    signature.m_global_signature = TRY(Curves::Ed25519 {}.sign(m_public_key, m_secret_key.view().bytes().trim(m_secret_key.length() - 1), global_data.bytes()));

    return signature;
}

ErrorOr<SecretKey> SecretKey::generate()
{
    auto private_key = TRY(Curves::Ed25519 {}.generate_private_key());
    auto const public_key = TRY(Curves::Ed25519 {}.generate_public_key(private_key.bytes()));
    KeyID key_id;
    fill_with_random(key_id);

    return SecretKey {
        "iffysign unencrypted secret key"_string,
        key_id,
        public_key,
        Core::SecretString::take_ownership(move(private_key)),
        {},
    };
}

}
