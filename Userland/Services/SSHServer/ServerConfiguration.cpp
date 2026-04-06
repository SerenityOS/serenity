/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ServerConfiguration.h"
#include <LibCrypto/Curves/Ed25519.h>
#include <LibSSH/DataTypes.h>

#include <LibCrypto/Hash/SHA2.h>

namespace SSH::Server {

ServerConfiguration& ServerConfiguration::the()
{
    static ServerConfiguration config {};
    return config;
}

TypedBlob const& ServerConfiguration::ssh_ed25519_server_public_key() const
{
    ensure_ssh_ed25519_keys();
    return m_ssh_ed25519_server_public_key;
}

TypedBlob const& ServerConfiguration::ssh_ed25519_server_private_key() const
{
    ensure_ssh_ed25519_keys();
    return m_ssh_ed25519_server_private_key;
}

void ServerConfiguration::ensure_ssh_ed25519_keys() const
{
    if (m_ssh_ed25519_server_public_key.key.is_empty()
        || m_ssh_ed25519_server_private_key.key.is_empty()) {

        if (m_use_unsafe_stubbed_private_key) {
            auto stub = MUST(ByteBuffer ::create_uninitialized(32));
            stub.bytes().fill(0x42);
            m_ssh_ed25519_server_private_key = {
                TypedBlob::Type::SSH_ED25519,
                stub
            };
        } else {
            m_ssh_ed25519_server_private_key = {
                TypedBlob::Type::SSH_ED25519,
                MUST(Crypto::Curves::Ed25519::generate_private_key())
            };
        }

        m_ssh_ed25519_server_public_key = {
            TypedBlob::Type::SSH_ED25519,
            MUST(Crypto::Curves::Ed25519::generate_public_key(m_ssh_ed25519_server_private_key.key))
        };
    }
}

}
