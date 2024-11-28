/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2021-2022, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Memory.h>
#include <AK/Random.h>
#include <AK/ScopeGuard.h>
#include <LibCore/Account.h>
#include <LibCore/Directory.h>
#include <LibCore/System.h>
#include <LibCore/UmaskScope.h>
#include <errno.h>
#include <grp.h>
#include <pwd.h>
#if !defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_HAIKU)
#    include <crypt.h>
#    include <shadow.h>
#endif
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

namespace Core {

static ErrorOr<ByteString> get_salt()
{
    char random_data[12];
    fill_with_random({ random_data, sizeof(random_data) });

    StringBuilder builder;
    builder.append("$5$"sv);

    auto salt_string = TRY(encode_base64({ random_data, sizeof(random_data) }));
    builder.append(salt_string);

    return builder.to_byte_string();
}

static Vector<gid_t> get_extra_gids(passwd const& pwd)
{
    StringView username { pwd.pw_name, strlen(pwd.pw_name) };
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

ErrorOr<Account> Account::from_passwd(passwd const& pwd, spwd const& spwd)
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
        return Error::from_string_literal("No such user");

    spwd spwd = {};
#ifndef AK_OS_BSD_GENERIC
    if (options != Read::PasswdOnly) {
        auto maybe_spwd = TRY(Core::System::getspnam({ pwd->pw_name, strlen(pwd->pw_name) }));
        if (!maybe_spwd.has_value())
            return Error::from_string_literal("No shadow entry for user");
        spwd = maybe_spwd.release_value();
    }
#endif

    return Account(*pwd, spwd, extra_gids);
}

ErrorOr<Account> Account::from_name(StringView username, [[maybe_unused]] Read options)
{
    auto pwd = TRY(Core::System::getpwnam(username));
    if (!pwd.has_value())
        return Error::from_string_literal("No such user");

    spwd spwd = {};
#ifndef AK_OS_BSD_GENERIC
    if (options != Read::PasswdOnly) {
        auto maybe_spwd = TRY(Core::System::getspnam({ pwd->pw_name, strlen(pwd->pw_name) }));
        if (!maybe_spwd.has_value())
            return Error::from_string_literal("No shadow entry for user");
        spwd = maybe_spwd.release_value();
    }
#endif
    return from_passwd(*pwd, spwd);
}

ErrorOr<Account> Account::from_uid(uid_t uid, [[maybe_unused]] Read options)
{
    auto pwd = TRY(Core::System::getpwuid(uid));
    if (!pwd.has_value())
        return Error::from_string_literal("No such user");

    spwd spwd = {};
#ifndef AK_OS_BSD_GENERIC
    if (options != Read::PasswdOnly) {
        auto maybe_spwd = TRY(Core::System::getspnam({ pwd->pw_name, strlen(pwd->pw_name) }));
        if (!maybe_spwd.has_value())
            return Error::from_string_literal("No shadow entry for user");
        spwd = maybe_spwd.release_value();
    }
#endif
    return from_passwd(*pwd, spwd);
}

ErrorOr<Vector<Account>> Account::all([[maybe_unused]] Read options)
{
    Vector<Account> accounts;
    char buffer[1024] = { 0 };

    ScopeGuard pwent_guard([] { endpwent(); });
    setpwent();

    while (true) {
        auto pwd = TRY(Core::System::getpwent({ buffer, sizeof(buffer) }));
        if (!pwd.has_value())
            break;

        spwd spwd = {};

#ifndef AK_OS_BSD_GENERIC
        ScopeGuard spent_guard([] { endspent(); });
        if (options != Read::PasswdOnly) {
            auto maybe_spwd = TRY(Core::System::getspnam({ pwd->pw_name, strlen(pwd->pw_name) }));
            if (!maybe_spwd.has_value())
                return Error::from_string_literal("No shadow entry for user");
            spwd = maybe_spwd.release_value();
        }
#endif

        accounts.append({ *pwd, spwd, get_extra_gids(*pwd) });
    }

    return accounts;
}

bool Account::authenticate(SecretString const& password) const
{
    // If there was no shadow entry for this account, authentication always fails.
    if (!m_password_hash.has_value())
        return false;

    // An empty passwd field indicates that no password is required to log in.
    if (m_password_hash->is_empty())
        return true;

    // FIXME: Use crypt_r if it can be built in lagom.
    auto const bytes = m_password_hash->characters();
    char* hash = crypt(password.characters(), bytes);
    return hash != nullptr && AK::timing_safe_compare(hash, bytes, m_password_hash->length());
}

ErrorOr<void> Account::login() const
{
    TRY(Core::System::setgroups(m_extra_gids));
    TRY(Core::System::setgid(m_gid));
    TRY(Core::System::setuid(m_uid));

    return {};
}

ErrorOr<void> Account::set_password(SecretString const& password)
{
    m_password_hash = crypt(password.characters(), TRY(get_salt()).characters());
    return {};
}

void Account::set_password_enabled(bool enabled)
{
    auto flattened_password_hash = m_password_hash.value_or(ByteString::empty());
    if (enabled && !flattened_password_hash.is_empty() && flattened_password_hash[0] == '!') {
        m_password_hash = flattened_password_hash.substring(1, flattened_password_hash.length() - 1);
    } else if (!enabled && (flattened_password_hash.is_empty() || flattened_password_hash[0] != '!')) {
        StringBuilder builder;
        builder.append('!');
        builder.append(flattened_password_hash);
        m_password_hash = builder.to_byte_string();
    }
}

void Account::delete_password()
{
    m_password_hash = ByteString::empty();
}

Account::Account(passwd const& pwd, spwd const& spwd, Vector<gid_t> extra_gids)
    : m_username(pwd.pw_name)
    , m_password_hash(spwd.sp_pwdp ? Optional<ByteString>(spwd.sp_pwdp) : OptionalNone {})
    , m_uid(pwd.pw_uid)
    , m_gid(pwd.pw_gid)
    , m_gecos(pwd.pw_gecos)
    , m_home_directory(pwd.pw_dir)
    , m_shell(pwd.pw_shell)
    , m_extra_gids(move(extra_gids))
{
}

ErrorOr<ByteString> Account::generate_passwd_file() const
{
    StringBuilder builder;
    char buffer[1024] = { 0 };

    ScopeGuard pwent_guard([] { endpwent(); });
    setpwent();

    while (true) {
        auto pwd = TRY(Core::System::getpwent({ buffer, sizeof(buffer) }));
        if (!pwd.has_value())
            break;

        if (pwd->pw_name == m_username) {
            if (m_deleted)
                continue;
            builder.appendff("{}:!:{}:{}:{}:{}:{}\n",
                m_username,
                m_uid, m_gid,
                m_gecos,
                m_home_directory,
                m_shell);

        } else {
            builder.appendff("{}:!:{}:{}:{}:{}:{}\n",
                pwd->pw_name, pwd->pw_uid,
                pwd->pw_gid, pwd->pw_gecos, pwd->pw_dir,
                pwd->pw_shell);
        }
    }

    return builder.to_byte_string();
}

ErrorOr<ByteString> Account::generate_group_file() const
{
    StringBuilder builder;
    char buffer[1024] = { 0 };

    ScopeGuard pwent_guard([] { endgrent(); });
    setgrent();

    while (true) {
        auto group = TRY(Core::System::getgrent(buffer));
        if (!group.has_value())
            break;

        auto should_be_present = !m_deleted && m_extra_gids.contains_slow(group->gr_gid);

        auto already_present = false;
        Vector<char const*> members;
        for (size_t i = 0; group->gr_mem[i]; ++i) {
            auto const* member = group->gr_mem[i];
            if (member == m_username) {
                already_present = true;
                if (!should_be_present)
                    continue;
            }
            members.append(member);
        }

        if (should_be_present && !already_present)
            members.append(m_username.characters());

        builder.appendff("{}:{}:{}:{}\n", group->gr_name, group->gr_passwd, group->gr_gid, ByteString::join(","sv, members));
    }

    return builder.to_byte_string();
}

#ifndef AK_OS_BSD_GENERIC
ErrorOr<ByteString> Account::generate_shadow_file() const
{
    StringBuilder builder;

    setspent();

    struct spwd* p;
    errno = 0;
    while ((p = getspent())) {
        if (p->sp_namp == m_username) {
            if (m_deleted)
                continue;
            builder.appendff("{}:{}", m_username, m_password_hash.value_or(ByteString::empty()));
        } else
            builder.appendff("{}:{}", p->sp_namp, p->sp_pwdp);

        builder.appendff(":{}:{}:{}:{}:{}:{}:{}\n",
            (p->sp_lstchg == -1) ? "" : ByteString::formatted("{}", p->sp_lstchg),
            (p->sp_min == -1) ? "" : ByteString::formatted("{}", p->sp_min),
            (p->sp_max == -1) ? "" : ByteString::formatted("{}", p->sp_max),
            (p->sp_warn == -1) ? "" : ByteString::formatted("{}", p->sp_warn),
            (p->sp_inact == -1) ? "" : ByteString::formatted("{}", p->sp_inact),
            (p->sp_expire == -1) ? "" : ByteString::formatted("{}", p->sp_expire),
            (p->sp_flag == 0) ? "" : ByteString::formatted("{}", p->sp_flag));
    }
    endspent();

    if (errno)
        return Error::from_errno(errno);

    return builder.to_byte_string();
}
#endif

ErrorOr<void> Account::sync()
{
    Core::UmaskScope umask_scope(0777);

    auto new_passwd_file_content = TRY(generate_passwd_file());
    auto new_group_file_content = TRY(generate_group_file());
#ifndef AK_OS_BSD_GENERIC
    auto new_shadow_file_content = TRY(generate_shadow_file());
#endif

    char new_passwd_file[] = "/etc/passwd.XXXXXX";
    char new_group_file[] = "/etc/group.XXXXXX";
#ifndef AK_OS_BSD_GENERIC
    char new_shadow_file[] = "/etc/shadow.XXXXXX";
#endif

    {
        auto new_passwd_fd = TRY(Core::System::mkstemp(new_passwd_file));
        ScopeGuard new_passwd_fd_guard = [new_passwd_fd] { close(new_passwd_fd); };
        TRY(Core::System::fchmod(new_passwd_fd, 0644));

        auto new_group_fd = TRY(Core::System::mkstemp(new_group_file));
        ScopeGuard new_group_fd_guard = [new_group_fd] { close(new_group_fd); };
        TRY(Core::System::fchmod(new_group_fd, 0644));

#ifndef AK_OS_BSD_GENERIC
        auto new_shadow_fd = TRY(Core::System::mkstemp(new_shadow_file));
        ScopeGuard new_shadow_fd_guard = [new_shadow_fd] { close(new_shadow_fd); };
        TRY(Core::System::fchmod(new_shadow_fd, 0600));
#endif

        auto nwritten = TRY(Core::System::write(new_passwd_fd, new_passwd_file_content.bytes()));
        VERIFY(static_cast<size_t>(nwritten) == new_passwd_file_content.length());

        nwritten = TRY(Core::System::write(new_group_fd, new_group_file_content.bytes()));
        VERIFY(static_cast<size_t>(nwritten) == new_group_file_content.length());

#ifndef AK_OS_BSD_GENERIC
        nwritten = TRY(Core::System::write(new_shadow_fd, new_shadow_file_content.bytes()));
        VERIFY(static_cast<size_t>(nwritten) == new_shadow_file_content.length());
#endif
    }

    auto new_passwd_file_view = StringView { new_passwd_file, sizeof(new_passwd_file) };
    TRY(Core::System::rename(new_passwd_file_view, "/etc/passwd"sv));

    auto new_group_file_view = StringView { new_group_file, sizeof(new_group_file) };
    TRY(Core::System::rename(new_group_file_view, "/etc/group"sv));
#ifndef AK_OS_BSD_GENERIC
    auto new_shadow_file_view = StringView { new_shadow_file, sizeof(new_shadow_file) };
    TRY(Core::System::rename(new_shadow_file_view, "/etc/shadow"sv));
#endif

    return {};
}

}
