/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <LibCore/Forward.h>
#include <LibSSH/DataTypes.h>

namespace SSH::Server {

class ServerConfiguration {
public:
    static ServerConfiguration& the();

    TypedBlob const& ssh_ed25519_server_public_key() const;
    TypedBlob const& ssh_ed25519_server_private_key() const;

    void use_unsafe_stubbed_private_key()
    {
        m_use_unsafe_stubbed_private_key = true;
    }

    void set_user_authorized_keys_file(StringView path)
    {
        m_user_authorized_keys_file = path;
    }

    void set_keylog_file(StringView path) { m_keylog_file = path; }
    Optional<ByteString> keylog_file() const { return m_keylog_file; }

    ErrorOr<Vector<TypedBlob>> get_authorized_keys_for_user(Core::Account const&) const;

private:
    ErrorOr<StringView> user_authorized_keys_file(Core::Account const&) const;

    void ensure_ssh_ed25519_keys() const;
    ErrorOr<void> load_server_keys_from_file() const;

    mutable TypedBlob m_ssh_ed25519_server_public_key;
    mutable TypedBlob m_ssh_ed25519_server_private_key;

    bool m_use_unsafe_stubbed_private_key { false };

    ByteString m_user_authorized_keys_file;
    Optional<ByteString> m_keylog_file;
};

}
