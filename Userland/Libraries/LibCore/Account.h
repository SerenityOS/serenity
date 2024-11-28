/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/SecretString.h>
#include <pwd.h>
#ifndef AK_OS_BSD_GENERIC
#    include <shadow.h>
#endif
#include <sys/types.h>

namespace Core {

#ifdef AK_OS_BSD_GENERIC
struct spwd {
    char* sp_namp;
    char* sp_pwdp;
};
#endif

class Account {
public:
    enum class Read {
        All,
        PasswdOnly
    };

    static ErrorOr<Account> self(Read options = Read::All);
    static ErrorOr<Account> from_name(StringView username, Read options = Read::All);
    static ErrorOr<Account> from_uid(uid_t uid, Read options = Read::All);
    static ErrorOr<Vector<Account>> all(Read options = Read::All);

    bool authenticate(SecretString const& password) const;
    ErrorOr<void> login() const;

    ByteString username() const { return m_username; }
    ByteString password_hash() const { return m_password_hash.value_or(ByteString::empty()); }

    // Setters only affect in-memory copy of password.
    // You must call sync to apply changes.
    ErrorOr<void> set_password(SecretString const& password);
    void set_password_enabled(bool enabled);
    void set_home_directory(StringView home_directory) { m_home_directory = home_directory; }
    void set_uid(uid_t uid) { m_uid = uid; }
    void set_gid(gid_t gid) { m_gid = gid; }
    void set_shell(StringView shell) { m_shell = shell; }
    void set_gecos(StringView gecos) { m_gecos = gecos; }
    void set_deleted() { m_deleted = true; }
    void set_extra_gids(Vector<gid_t> extra_gids) { m_extra_gids = move(extra_gids); }
    void delete_password();

    // A nonexistent password means that this account was missing from /etc/shadow.
    // It's considered to have a password in that case, and authentication will always fail.
    bool has_password() const { return !m_password_hash.has_value() || !m_password_hash->is_empty(); }

    uid_t uid() const { return m_uid; }
    gid_t gid() const { return m_gid; }
    ByteString const& gecos() const { return m_gecos; }
    ByteString const& home_directory() const { return m_home_directory; }
    ByteString const& shell() const { return m_shell; }
    Vector<gid_t> const& extra_gids() const { return m_extra_gids; }

    ErrorOr<void> sync();

private:
    static ErrorOr<Account> from_passwd(passwd const&, spwd const&);

    Account(passwd const& pwd, spwd const& spwd, Vector<gid_t> extra_gids);

    ErrorOr<ByteString> generate_passwd_file() const;
    ErrorOr<ByteString> generate_group_file() const;
#ifndef AK_OS_BSD_GENERIC
    ErrorOr<ByteString> generate_shadow_file() const;
#endif

    ByteString m_username;

    Optional<ByteString> m_password_hash;
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    ByteString m_gecos;
    ByteString m_home_directory;
    ByteString m_shell;
    Vector<gid_t> m_extra_gids;
    bool m_deleted { false };
};

}
