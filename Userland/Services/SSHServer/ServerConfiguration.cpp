/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ServerConfiguration.h"
#include <LibCore/Account.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibCrypto/Curves/Ed25519.h>
#include <LibCrypto/Hash/SHA2.h>
#include <LibSSH/DataTypes.h>

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

static ErrorOr<Vector<TypedBlob>> read_blobs_from_file(NonnullOwnPtr<Core::File> raw_file)
{
    auto file = TRY(Core::InputBufferedFile::create(move(raw_file)));

    Vector<TypedBlob> blobs;

    while (TRY(file->can_read_line())) {
        Array<u8, 1024> buffer;
        auto line = TRY(file->read_line(buffer));

        blobs.append(TRY(TypedBlob::read_from_string(line)));
    }

    return blobs;
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

ErrorOr<Vector<TypedBlob>> ServerConfiguration::get_authorized_keys_for_user(Core::Account const& user) const
{
    auto raw_file = TRY(Core::File::open(TRY(user_authorized_keys_file(user)), Core::File::OpenMode::Read));
    return read_blobs_from_file(move(raw_file));
}

ErrorOr<StringView> ServerConfiguration::user_authorized_keys_file([[maybe_unused]] Core::Account const& user) const
{
    if (!m_user_authorized_keys_file.is_empty())
        return m_user_authorized_keys_file;

#ifdef AK_OS_SERENITY
    static auto default_path = ByteString::formatted(
        "{}/{}",
        user.home_directory(),
        "/.config/ssh/authorized_keys"sv);
    return default_path;
#endif

    return Error::from_string_literal("No default path for ssh keys is provided on Lagom");
}

}
