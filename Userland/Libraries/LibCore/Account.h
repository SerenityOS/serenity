/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
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

    static Account self(Read options = Read::All);
    static ErrorOr<Account> from_name(char const* username, Read options = Read::All);
    static ErrorOr<Account> from_uid(uid_t uid, Read options = Read::All);

    bool authenticate(SecretString const& password) const;
    bool login() const;

    String username() const { return m_username; }
    String password_hash() const { return m_password_hash; }

    // Setters only affect in-memory copy of password.
    // You must call sync to apply changes.
    void set_password(SecretString const& password);
    void set_password_enabled(bool enabled);
    void set_home_directory(const char* home_directory) { m_home_directory = home_directory; }
    void set_uid(uid_t uid) { m_uid = uid; }
    void set_gid(gid_t gid) { m_gid = gid; }
    void set_shell(const char* shell) { m_shell = shell; }
    void set_gecos(const char* gecos) { m_gecos = gecos; }
    void delete_password();

    // A null password means that this account was missing from /etc/shadow.
    // It's considered to have a password in that case, and authentication will always fail.
    bool has_password() const { return !m_password_hash.is_empty() || m_password_hash.is_null(); }

    uid_t uid() const { return m_uid; }
    gid_t gid() const { return m_gid; }
    const String& gecos() const { return m_gecos; }
    const String& home_directory() const { return m_home_directory; }
    const String& shell() const { return m_shell; }
    const Vector<gid_t>& extra_gids() const { return m_extra_gids; }

    bool sync();

private:
    static ErrorOr<Account> from_passwd(passwd const&, spwd const&);

    Account(const passwd& pwd, const spwd& spwd, Vector<gid_t> extra_gids);

    String generate_passwd_file() const;
#ifndef AK_OS_BSD_GENERIC
    String generate_shadow_file() const;
#endif

    String m_username;

    String m_password_hash;
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    String m_gecos;
    String m_home_directory;
    String m_shell;
    Vector<gid_t> m_extra_gids;
};

}
