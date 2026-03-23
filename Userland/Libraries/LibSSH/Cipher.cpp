/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Cipher.h"

#include <AK/ByteReader.h>
#include <AK/Endian.h>
#include <AK/OwnPtr.h>
#include <LibCrypto/Authentication/Poly1305.h>
#include <LibCrypto/Cipher/ChaCha20.h>
#include <LibCrypto/Hash/SHA2.h>
#include <LibSSH/DataTypes.h>

namespace SSH {

Cipher::Cipher(u8 block_size, u8 mac_size)
    : m_block_size(block_size)
    , m_mac_size(mac_size)
{
}

ErrorOr<void> Cipher::decrypt(u32 packet_sequence_number, Bytes bytes)
{
    if (bytes.size() % m_block_size != 0)
        return Error::from_string_literal("Can't decipher message of invalid block size");

    decrypt_impl(packet_sequence_number, bytes);
    return {};
}

ErrorOr<void> Cipher::encrypt(u32 packet_sequence_number, Bytes bytes)
{
    if (bytes.size() % m_block_size != 0)
        return Error::from_string_literal("Can't obfuscate message of invalid block size");

    encrypt_impl(packet_sequence_number, bytes);
    return {};
}

NonnullOwnPtr<ChaCha20Poly1305Cipher> ChaCha20Poly1305Cipher::create(ByteBuffer const& shared_secret, Crypto::Hash::Digest<256> hash, Crypto::Hash::Digest<256> session_id)
{

    // 7.2.  Output from Key Exchange
    // https://datatracker.ietf.org/doc/html/rfc4253#section-7.2

    // "Each key exchange method specifies a hash function that is used in the key exchange.
    // The same hash algorithm MUST be used in key derivation.  Here, we'll call it HASH."
    Crypto::Hash::SHA256 sha256;

    // Seems like shared_secret is supposed to be encoded as a mpint.
    // FIXME: Find spec quote for this.
    auto shared_secret_as_mpint = MUST(as_mpint(shared_secret));

    // "Encryption key client to server: HASH(K || H || "C" || session_id)"

    sha256.update(shared_secret_as_mpint);
    sha256.update(hash.bytes());
    sha256.update("C"sv);
    sha256.update(session_id.bytes());

    auto encryption_client_to_server = sha256.digest();

    // Encryption key server to client: HASH(K || H || "D" || session_id)

    sha256.update(shared_secret_as_mpint);
    sha256.update(hash.bytes());
    sha256.update("D"sv);
    sha256.update(session_id.bytes());

    auto encryption_server_to_client = sha256.digest();

    // "If the key length needed is longer than the output of the HASH, the key is extended
    // by computing HASH of the concatenation of K and H and the entire key so far, and
    // appending the resulting bytes (as many as HASH generates) to the key."
    // [...]
    // "In other words:
    // K1 = HASH(K || H || X || session_id)   (X is e.g., "A")
    // K2 = HASH(K || H || K1)
    // K3 = HASH(K || H || K1 || K2)
    // ...
    // key = K1 || K2 || K3 || ..."

    auto extend_key = [&shared_secret_as_mpint, &hash](Crypto::Hash::Digest<256> key) {
        Crypto::Hash::SHA256 sha256;
        sha256.update(shared_secret_as_mpint);
        sha256.update(hash.bytes());
        sha256.update(key.bytes());
        return sha256.digest();
    };

    //  6. Detailed Construction
    // https://datatracker.ietf.org/doc/html/draft-ietf-sshm-chacha20-poly1305-02#section-6

    // "The "chacha20-poly1305" cipher requires 512 bits of key material as output from the SSH key exchange. This forms two
    // 256 bit keys (K_1 and K_2), used by two separate instances of chacha20."
    auto k_2_client_to_server = extend_key(encryption_client_to_server);
    auto k_2_server_to_client = extend_key(encryption_server_to_client);

    return MUST(adopt_nonnull_own_or_enomem(new ChaCha20Poly1305Cipher {
        encryption_client_to_server,
        k_2_client_to_server,
        encryption_server_to_client,
        k_2_server_to_client,
    }));
}

// 7. Packet Handling
// https://datatracker.ietf.org/doc/html/draft-ietf-sshm-chacha20-poly1305-02#section-7
void ChaCha20Poly1305Cipher::decrypt_impl(u32 packet_sequence_number, Bytes bytes)
{
    // "When receiving a packet, the length must be decrypted first. When 4 bytes o
    // ciphertext length have been received, they may be decrypted using the K_2 key,
    // a nonce consisting of the packet sequence number encoded as a uint64 under the
    // usual SSH wire encoding and a zero block counter to obtain the plaintext length."

    auto encrypted_length = bytes.trim(4);
    NetworkOrdered<u64> nonce_data(packet_sequence_number);
    ReadonlyBytes nonce { &nonce_data, sizeof(nonce_data) };

    Crypto::Cipher::ChaCha20 length_decryptor(m_k_2_client_to_server.bytes(), nonce, 0);
    length_decryptor.decrypt(encrypted_length, encrypted_length);

    // FIXME: "Once the entire packet has been received, the MAC MUST be checked before decryption."

    // "the packet decrypted using ChaCha20 as described above (with K_1, the packet
    // sequence number as nonce and a starting block counter of 1)."

    u32 packet_length = AK::convert_between_host_and_network_endian(ByteReader::load32(encrypted_length.data()));
    auto packet = bytes.slice(4, packet_length);

    Crypto::Cipher::ChaCha20 packet_decryptor(m_k_1_client_to_server.bytes(), nonce, 1);
    packet_decryptor.decrypt(packet, packet);
}

// 7. Packet Handling
// https://datatracker.ietf.org/doc/html/draft-ietf-sshm-chacha20-poly1305-02#section-7
void ChaCha20Poly1305Cipher::encrypt_impl(u32 packet_sequence_number, Bytes bytes)
{
    VERIFY(bytes.size() % block_size() == 0);
    VERIFY(bytes.size() >= mac_size() + sizeof(u32) + 4);

    auto packet_length_bytes = bytes.trim(4);
    auto mac = bytes.slice_from_end(mac_size());
    auto packet = bytes.trim(bytes.size() - mac.size()).slice(packet_length_bytes.size());
    VERIFY(packet_length_bytes.size() + packet.size() + mac.size() == bytes.size());

    // "To send a packet, first encode the 4 byte length and encrypt it using K_2."
    NetworkOrdered<u64> nonce_data(packet_sequence_number);
    ReadonlyBytes nonce { &nonce_data, sizeof(nonce_data) };

    Crypto::Cipher::ChaCha20 length_encryptor(m_k_2_server_to_client.bytes(), nonce, 0);
    length_encryptor.encrypt(packet_length_bytes, packet_length_bytes);

    // "Encrypt the packet payload (using K_1) and append it to the encrypted length."
    Crypto::Cipher::ChaCha20 packet_encryptor(m_k_1_server_to_client.bytes(), nonce, 1);
    packet_encryptor.encrypt(packet, packet);

    // "Finally, calculate a MAC tag and append it."

    // From  6. Detailed Construction:
    // "To generate keying information for the Poly1305 MAC, the K_1 cipher instance is run with
    // the same key and nonce but a block counter value of 0."
    Array<u8, 32> poly_key {};

    Crypto::Cipher::ChaCha20 mac_generator(m_k_1_server_to_client.bytes(), nonce, 0);
    mac_generator.encrypt(poly_key, poly_key);

    Crypto::Authentication::Poly1305 poly1305(poly_key);
    poly1305.update(packet_length_bytes);
    poly1305.update(packet);
    auto mac_result = MUST(poly1305.digest());
    mac_result.bytes().copy_to(mac);
}

} // SSH
