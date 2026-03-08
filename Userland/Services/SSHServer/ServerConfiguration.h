/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSSH/DataTypes.h>

namespace SSH::Server {

// FIXME: This generates a brand new host key every single time
//        a server is started. We should store in on the disk to
//        make it persistent.
class ServerConfiguration {
public:
    static ServerConfiguration const& the();

    TypedBlob const& ssh_ed25519_server_public_key() const;
    TypedBlob const& ssh_ed25519_server_private_key() const;

private:
    mutable TypedBlob m_ssh_ed25519_server_public_key;
    mutable TypedBlob m_ssh_ed25519_server_private_key;
    void ensure_ssh_ed25519_keys() const;
};

}
