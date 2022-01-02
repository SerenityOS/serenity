/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Random.h>
#include <AK/ScopeGuard.h>
#include <LibCore/Account.h>
#include <LibCore/System.h>
#include <LibCore/UmaskScope.h>
#include <errno.h>
#include <grp.h>
#include <pwd.h>
#ifndef AK_OS_BSD_GENERIC
#    include <crypt.h>
#    include <shadow.h>
#endif
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

namespace Core {

static String get_salt()
{
    char random_data[12];
    fill_with_random(random_data, sizeof(random_data));

    StringBuilder builder;
    builder.append("$5$");
    builder.append(encode_base64(ReadonlyBytes(random_data, sizeof(random_data))));

    return builder.build();
}

static Vector<gid_t> get_extra_gids(const passwd& pwd)
{
    StringView username { pwd.pw_name };
    Vector<gid_t> extra_gids;
    setgrent();
    for (auto* group = getgrent(); group; group = getgrent()) {
        if (group->gr_gid == pwd.pw_gid)
            continue;
        for (size_t i = 0; group->gr_mem[i]; ++i) {
            if (username == group->gr_mem[i]) {
                extra_gids.append(group->gr_gid);
                break;
            }
        }
    }
    endgrent();
    return extra_gids;
}

ErrorOr<Account> Account::from_passwd(const passwd& pwd, const spwd& spwd)
{
    Account account(pwd, spwd, get_extra_gids(pwd));
    endpwent();
#ifndef AK_OS_BSD_GENERIC
    endspent();
#endif
    return account;
}

ErrorOr<Account> Account::self([[maybe_unused]] Read options)
{
    Vector<gid_t> extra_gids = TRY(Core::System::getgroups());

    auto pwd = TRY(Core::System::getpwuid(getuid()));
    if (!pwd.has_value())
        return Error::from_string_literal("No such user"sv);

    spwd spwd = {};
#ifndef AK_OS_BSD_GENERIC
    if (options != Read::PasswdOnly) {
        auto maybe_spwd = TRY(Core::System::getspnam(pwd->pw_name));
        if (!maybe_spwd.has_value())
            return Error::from_string_literal("No shadow entry for user"sv);
        spwd = maybe_spwd.release_value();
    }
#endif

    return Account(*pwd, spwd, extra_gids);
}

ErrorOr<Account> Account::from_name(const char* username, [[maybe_unused]] Read options)
{
    auto pwd = TRY(Core::System::getpwnam(username));
    if (!pwd.has_value())
        return Error::from_string_literal("No such user"sv);

    spwd spwd = {};
#ifndef AK_OS_BSD_GENERIC
    if (options != Read::PasswdOnly) {
        auto maybe_spwd = TRY(Core::System::getspnam(pwd->pw_name));
        if (!maybe_spwd.has_value())
            return Error::from_string_literal("No shadow entry for user"sv);
        spwd = maybe_spwd.release_value();
    }
#endif
    return from_passwd(*pwd, spwd);
}

ErrorOr<Account> Account::from_uid(uid_t uid, [[maybe_unused]] Read options)
{
    auto pwd = TRY(Core::System::getpwuid(uid));
    if (!pwd.has_value())
        return Error::from_string_literal("No such user"sv);

    spwd spwd = {};
#ifndef AK_OS_BSD_GENERIC
    if (options != Read::PasswdOnly) {
        auto maybe_spwd = TRY(Core::System::getspnam(pwd->pw_name));
        if (!maybe_spwd.has_value())
            return Error::from_string_literal("No shadow entry for user"sv);
        spwd = maybe_spwd.release_value();
    }
#endif
    return from_passwd(*pwd, spwd);
}

bool Account::authenticate(SecretString const& password) const
{
    // If there was no shadow entry for this account, authentication always fails.
    if (m_password_hash.is_null())
        return false;

    // An empty passwd field indicates that no password is required to log in.
    if (m_password_hash.is_empty())
        return true;

    // FIXME: Use crypt_r if it can be built in lagom.
    char* hash = crypt(password.characters(), m_password_hash.characters());
    return hash != nullptr && strcmp(hash, m_password_hash.characters()) == 0;
}

bool Account::login() const
{
    if (setgroups(m_extra_gids.size(), m_extra_gids.data()) < 0)
        return false;

    if (setgid(m_gid) < 0)
        return false;

    if (setuid(m_uid) < 0)
        return false;

    return true;
}

void Account::set_password(SecretString const& password)
{
    m_password_hash = crypt(password.characters(), get_salt().characters());
}

void Account::set_password_enabled(bool enabled)
{
    if (enabled && m_password_hash != "" && m_password_hash[0] == '!') {
        m_password_hash = m_password_hash.substring(1, m_password_hash.length() - 1);
    } else if (!enabled && (m_password_hash == "" || m_password_hash[0] != '!')) {
        StringBuilder builder;
        builder.append('!');
        builder.append(m_password_hash);
        m_password_hash = builder.build();
    }
}

void Account::delete_password()
{
    m_password_hash = "";
}

Account::Account(const passwd& pwd, const spwd& spwd, Vector<gid_t> extra_gids)
    : m_username(pwd.pw_name)
    , m_password_hash(spwd.sp_pwdp)
    , m_uid(pwd.pw_uid)
    , m_gid(pwd.pw_gid)
    , m_gecos(pwd.pw_gecos)
    , m_home_directory(pwd.pw_dir)
    , m_shell(pwd.pw_shell)
    , m_extra_gids(move(extra_gids))
{
}

ErrorOr<String> Account::generate_passwd_file() const
{
    StringBuilder builder;

    setpwent();

    struct passwd* p;
    errno = 0;
    while ((p = getpwent())) {
        if (p->pw_name == m_username) {
            builder.appendff("{}:!:{}:{}:{}:{}:{}\n",
                m_username,
                m_uid, m_gid,
                m_gecos,
                m_home_directory,
                m_shell);

        } else {
            builder.appendff("{}:!:{}:{}:{}:{}:{}\n",
                p->pw_name, p->pw_uid,
                p->pw_gid, p->pw_gecos, p->pw_dir,
                p->pw_shell);
        }
    }
    endpwent();

    if (errno)
        return Error::from_errno(errno);

    return builder.to_string();
}

#ifndef AK_OS_BSD_GENERIC
ErrorOr<String> Account::generate_shadow_file() const
{
    StringBuilder builder;

    setspent();

    struct spwd* p;
    errno = 0;
    while ((p = getspent())) {
        if (p->sp_namp == m_username) {
            builder.appendff("{}:{}:{}:{}:{}:{}:{}:{}:{}\n",
                m_username, m_password_hash,
                (p->sp_lstchg == -1) ? "" : String::formatted("{}", p->sp_lstchg),
                (p->sp_min == -1) ? "" : String::formatted("{}", p->sp_min),
                (p->sp_max == -1) ? "" : String::formatted("{}", p->sp_max),
                (p->sp_warn == -1) ? "" : String::formatted("{}", p->sp_warn),
                (p->sp_inact == -1) ? "" : String::formatted("{}", p->sp_inact),
                (p->sp_expire == -1) ? "" : String::formatted("{}", p->sp_expire),
                (p->sp_flag == 0) ? "" : String::formatted("{}", p->sp_flag));

        } else {
            builder.appendff("{}:{}:{}:{}:{}:{}:{}:{}:{}\n",
                p->sp_namp, p->sp_pwdp,
                (p->sp_lstchg == -1) ? "" : String::formatted("{}", p->sp_lstchg),
                (p->sp_min == -1) ? "" : String::formatted("{}", p->sp_min),
                (p->sp_max == -1) ? "" : String::formatted("{}", p->sp_max),
                (p->sp_warn == -1) ? "" : String::formatted("{}", p->sp_warn),
                (p->sp_inact == -1) ? "" : String::formatted("{}", p->sp_inact),
                (p->sp_expire == -1) ? "" : String::formatted("{}", p->sp_expire),
                (p->sp_flag == 0) ? "" : String::formatted("{}", p->sp_flag));
        }
    }
    endspent();

    if (errno)
        return Error::from_errno(errno);

    return builder.to_string();
}
#endif

ErrorOr<void> Account::sync()
{
    Core::UmaskScope umask_scope(0777);

    auto new_passwd_file_content = TRY(generate_passwd_file());
#ifndef AK_OS_BSD_GENERIC
    auto new_shadow_file_content = TRY(generate_shadow_file());
#endif

    char new_passwd_name[] = "/etc/passwd.XXXXXX";
#ifndef AK_OS_BSD_GENERIC
    char new_shadow_name[] = "/etc/shadow.XXXXXX";
#endif

    {
        auto new_passwd_fd = TRY(Core::System::mkstemp(new_passwd_name));
        ScopeGuard new_passwd_fd_guard = [new_passwd_fd] { close(new_passwd_fd); };
        TRY(Core::System::fchmod(new_passwd_fd, 0644));

#ifndef AK_OS_BSD_GENERIC
        auto new_shadow_fd = TRY(Core::System::mkstemp(new_shadow_name));
        ScopeGuard new_shadow_fd_guard = [new_shadow_fd] { close(new_shadow_fd); };
        TRY(Core::System::fchmod(new_shadow_fd, 0600));
#endif

        auto nwritten = TRY(Core::System::write(new_passwd_fd, new_passwd_file_content.bytes()));
        VERIFY(static_cast<size_t>(nwritten) == new_passwd_file_content.length());

#ifndef AK_OS_BSD_GENERIC
        nwritten = TRY(Core::System::write(new_shadow_fd, new_shadow_file_content.bytes()));
        VERIFY(static_cast<size_t>(nwritten) == new_shadow_file_content.length());
#endif
    }

    TRY(Core::System::rename(new_passwd_name, "/etc/passwd"));
#ifndef AK_OS_BSD_GENERIC
    TRY(Core::System::rename(new_shadow_name, "/etc/shadow"));
#endif

    return {};
    // FIXME: Sync extra groups.
}

}
