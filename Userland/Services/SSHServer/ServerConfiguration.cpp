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

ErrorOr<void> ServerConfiguration::load_server_keys_from_file() const
{
    auto private_key_file = TRY(Core::File::open("/etc/ssh/host_ed25519"sv, Core::File::OpenMode::Read));
    auto content = TRY(private_key_file->read_until_eof());
    auto private_key = TRY(TypedBlob::read_from_openssh_private_key(content));
    VERIFY(private_key.type == TypedBlob::Type::SSH_ED25519);

    auto public_key_file = TRY(Core::File::open("/etc/ssh/host_ed25519.pub"sv, Core::File::OpenMode::Read));
    auto blobs = TRY(read_blobs_from_file(move(public_key_file)));
    VERIFY(blobs.size() == 1);
    auto public_key = blobs.take_first();
    VERIFY(public_key.type == TypedBlob::Type::SSH_ED25519);

    auto computed_public_key = TRY(Crypto::Curves::Ed25519::generate_public_key(private_key.key));
    if (computed_public_key != public_key.key)
        return Error::from_string_literal("Corrupted host key");

    m_ssh_ed25519_server_private_key = move(private_key);
    m_ssh_ed25519_server_public_key = move(public_key);

    return {};
}

void ServerConfiguration::ensure_ssh_ed25519_keys() const
{
    if (m_ssh_ed25519_server_public_key.key.is_empty()
        || m_ssh_ed25519_server_private_key.key.is_empty()) {

        if (!m_use_unsafe_stubbed_private_key) {
            auto maybe_error = load_server_keys_from_file();
            if (!maybe_error.is_error())
                return;
            dbgln("Unable to use the host key from /etc/ssh/: {}", maybe_error.error());
        }

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
