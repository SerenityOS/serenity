/*
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Base64.h>
#include <AK/Random.h>
#include <AK/ScopeGuard.h>
#include <LibCore/Account.h>
#include <LibCore/File.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

namespace Core {

static String get_salt()
{
    char random_data[12];
    AK::fill_with_random(random_data, sizeof(random_data));

    StringBuilder builder;
    builder.append("$5$");
    builder.append(encode_base64(ReadonlyBytes(random_data, sizeof(random_data))));

    return builder.build();
}

static Vector<gid_t> get_gids(const StringView& username)
{
    Vector<gid_t> extra_gids;
    for (auto* group = getgrent(); group; group = getgrent()) {
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

Result<Account, String> Account::from_passwd(const passwd& pwd)
{
    Account account(pwd, get_gids(pwd.pw_name));
    endpwent();
    return account;
}

Result<Account, String> Account::from_name(const char* username)
{
    struct passwd* pwd = nullptr;
    errno = 0;
    pwd = getpwnam(username);
    if (!pwd) {
        if (errno == 0)
            return String("No such user");

        return String(strerror(errno));
    }
    return from_passwd(*pwd);
}

Result<Account, String> Account::from_uid(uid_t uid)
{
    struct passwd* pwd = nullptr;
    errno = 0;
    pwd = getpwuid(uid);
    if (!pwd) {
        if (errno == 0)
            return String("No such user");

        return String(strerror(errno));
    }
    return from_passwd(*pwd);
}

bool Account::authenticate(const char* password) const
{
    // An empty passwd field indicates that no password is required to log in.
    if (m_password_hash.is_empty())
        return true;

    // FIXME: Use crypt_r if it can be built in lagom.
    char* hash = crypt(password, m_password_hash.characters());
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

void Account::set_password(const char* password)
{
    m_password_hash = crypt(password, get_salt().characters());
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

Account::Account(const passwd& pwd, Vector<gid_t> extra_gids)
    : m_username(pwd.pw_name)
    , m_uid(pwd.pw_uid)
    , m_gid(pwd.pw_gid)
    , m_gecos(pwd.pw_gecos)
    , m_home_directory(pwd.pw_dir)
    , m_shell(pwd.pw_shell)
    , m_extra_gids(extra_gids)
{
    load_shadow_file();
}

String Account::generate_passwd_file() const
{
    StringBuilder builder;

    setpwent();

    struct passwd* p;
    errno = 0;
    while ((p = getpwent())) {
        if (p->pw_uid == m_uid) {
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

    if (errno) {
        dbgln("errno was non-zero after generating new passwd file.");
        return {};
    }

    return builder.to_string();
}

void Account::load_shadow_file()
{
    auto file_or_error = Core::File::open("/etc/shadow", Core::File::ReadOnly);
    ASSERT(!file_or_error.is_error());
    auto shadow_file = file_or_error.release_value();
    ASSERT(shadow_file->is_open());

    Vector<ShadowEntry> entries;

    for (;;) {
        auto line = shadow_file->read_line();
        if (line.is_null())
            break;
        auto parts = line.split(':');
        if (parts.size() != 2) {
            dbgln("Malformed shadow entry, ignoring.");
            continue;
        }
        const auto& username = parts[0];
        const auto& password_hash = parts[1];
        entries.append({ username, password_hash });

        if (username == m_username) {
            m_password_hash = password_hash;
        }
    }

    m_shadow_entries = move(entries);
}

String Account::generate_shadow_file() const
{
    StringBuilder builder;
    bool updated_entry_in_place = false;
    for (auto& entry : m_shadow_entries) {
        if (entry.username == m_username) {
            updated_entry_in_place = true;
            builder.appendff("{}:{}\n", m_username, m_password_hash);
        } else {
            builder.appendff("{}:{}\n", entry.username, entry.password_hash);
        }
    }
    if (!updated_entry_in_place)
        builder.appendff("{}:{}\n", m_username, m_password_hash);
    return builder.to_string();
}

bool Account::sync()
{
    auto new_passwd_file_content = generate_passwd_file();
    auto new_shadow_file_content = generate_shadow_file();

    if (new_passwd_file_content.is_null() || new_shadow_file_content.is_null()) {
        ASSERT_NOT_REACHED();
    }

    char new_passwd_name[] = "/etc/passwd.XXXXXX";
    char new_shadow_name[] = "/etc/shadow.XXXXXX";

    {
        auto new_passwd_fd = mkstemp(new_passwd_name);
        if (new_passwd_fd < 0) {
            perror("mkstemp");
            ASSERT_NOT_REACHED();
        }
        ScopeGuard new_passwd_fd_guard = [new_passwd_fd] { close(new_passwd_fd); };
        auto new_shadow_fd = mkstemp(new_shadow_name);
        if (new_shadow_fd < 0) {
            perror("mkstemp");
            ASSERT_NOT_REACHED();
        }
        ScopeGuard new_shadow_fd_guard = [new_shadow_fd] { close(new_shadow_fd); };

        if (fchmod(new_passwd_fd, 0644) < 0) {
            perror("fchmod");
            ASSERT_NOT_REACHED();
        }

        auto nwritten = write(new_passwd_fd, new_passwd_file_content.characters(), new_passwd_file_content.length());
        if (nwritten < 0) {
            perror("write");
            ASSERT_NOT_REACHED();
        }
        ASSERT(static_cast<size_t>(nwritten) == new_passwd_file_content.length());

        nwritten = write(new_shadow_fd, new_shadow_file_content.characters(), new_shadow_file_content.length());
        if (nwritten < 0) {
            perror("write");
            ASSERT_NOT_REACHED();
        }
        ASSERT(static_cast<size_t>(nwritten) == new_shadow_file_content.length());
    }

    if (rename(new_passwd_name, "/etc/passwd") < 0) {
        perror("Failed to install new /etc/passwd");
        return false;
    }

    if (rename(new_shadow_name, "/etc/shadow") < 0) {
        perror("Failed to install new /etc/shadow");
        return false;
    }

    return true;
    // FIXME: Sync extra groups.
}

}
