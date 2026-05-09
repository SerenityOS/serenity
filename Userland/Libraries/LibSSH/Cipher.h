/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullOwnPtr.h>
#include <LibCrypto/Hash/HashFunction.h>

namespace SSH {

class Cipher {
    // FIXME: Erase the secrets on destruction.
public:
    Cipher(u8 block_size, u8 mac_size);
    virtual ~Cipher() = default;

    ErrorOr<void> decrypt(u32 packet_sequence_number, Bytes);
    ErrorOr<void> encrypt(u32 packet_sequence_number, Bytes);

    u8 block_size() const { return m_block_size; }
    u8 mac_size() const { return m_mac_size; }
    virtual u8 aad_size() const { return 0; }

private:
    virtual void decrypt_impl(u32 packet_sequence_number, Bytes) = 0;
    virtual void encrypt_impl(u32 packet_sequence_number, Bytes) = 0;

    u8 m_block_size {};
    u8 m_mac_size {};
};

class IdentityCipher : public Cipher {
public:
    IdentityCipher()
        : Cipher(1, 0)
    {
    }
    virtual ~IdentityCipher() = default;

private:
    void decrypt_impl(u32, Bytes) override { }
    void encrypt_impl(u32, Bytes) override { }
};

class ChaCha20Poly1305Cipher : public Cipher {
public:
    using Digest = Crypto::Hash::Digest<256>;

    virtual ~ChaCha20Poly1305Cipher() = default;

    static NonnullOwnPtr<ChaCha20Poly1305Cipher> create(ByteBuffer const& shared_secret, Digest hash, Digest session_id);

private:
    ChaCha20Poly1305Cipher(Digest k_1_client_to_server, Digest k_2_client_to_server, Digest k_1_server_to_client, Digest k_2_server_to_client)
        : Cipher(1, 16)
        , m_k_1_client_to_server(k_1_client_to_server)
        , m_k_2_client_to_server(k_2_client_to_server)
        , m_k_1_server_to_client(k_1_server_to_client)
        , m_k_2_server_to_client(k_2_server_to_client)
    {
    }

    void decrypt_impl(u32, Bytes) override;
    void encrypt_impl(u32, Bytes) override;

    virtual u8 aad_size() const override
    {
        // From the view of the payload's encryptor, the packet is:
        //     - u32 packet_length (AAD)
        //     - padding_length, payload, padding (message to encrypt)
        //     - MAC
        return sizeof(u32);
    }

    Digest m_k_1_client_to_server {};
    Digest m_k_2_client_to_server {};
    Digest m_k_1_server_to_client {};
    Digest m_k_2_server_to_client {};
};

} // SSH
