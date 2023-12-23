/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <AK/Vector.h>
#include <errno.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

extern "C" {

static FILE* s_stream = nullptr;
static unsigned s_line_number = 0;

void setpwent()
{
    s_line_number = 0;
    if (s_stream) {
        rewind(s_stream);
    } else {
        s_stream = fopen("/etc/passwd", "r");
        if (!s_stream) {
            perror("open /etc/passwd");
        }
    }
}

void endpwent()
{
    s_line_number = 0;
    if (s_stream) {
        fclose(s_stream);
        s_stream = nullptr;
    }
}

struct passwd* getpwuid(uid_t uid)
{
    setpwent();
    ScopeGuard guard = [] { endpwent(); };
    while (auto* pw = getpwent()) {
        if (pw->pw_uid == uid)
            return pw;
    }
    return nullptr;
}

struct passwd* getpwnam(char const* name)
{
    setpwent();
    ScopeGuard guard = [] { endpwent(); };
    while (auto* pw = getpwent()) {
        if (!strcmp(pw->pw_name, name))
            return pw;
    }
    return nullptr;
}

static bool parse_pwddb_entry(char* raw_line, struct passwd& passwd_entry)
{
    size_t line_length = strlen(raw_line);
    for (size_t i = 0; i < line_length; ++i) {
        auto& ch = raw_line[i];
        if (ch == '\r' || ch == '\n')
            line_length = i;
        if (ch == ':' || ch == '\r' || ch == '\n')
            ch = '\0';
    }
    auto line = StringView { raw_line, line_length };
    auto parts = line.split_view('\0', SplitBehavior::KeepEmpty);
    if (parts.size() != 7) {
        dbgln("getpwent(): Malformed entry on line {}", s_line_number);
        return false;
    }

    auto& name = parts[0];
    auto& passwd = parts[1];
    auto& uid_string = parts[2];
    auto& gid_string = parts[3];
    auto& gecos = parts[4];
    auto& dir = parts[5];
    auto& shell = parts[6];

    auto uid = uid_string.to_number<uid_t>();
    if (!uid.has_value()) {
        dbgln("getpwent(): Malformed UID on line {}", s_line_number);
        return false;
    }
    auto gid = gid_string.to_number<gid_t>();
    if (!gid.has_value()) {
        dbgln("getpwent(): Malformed GID on line {}", s_line_number);
        return false;
    }

    passwd_entry.pw_name = const_cast<char*>(name.characters_without_null_termination());
    passwd_entry.pw_passwd = const_cast<char*>(passwd.characters_without_null_termination());
    passwd_entry.pw_uid = uid.value();
    passwd_entry.pw_gid = gid.value();
    passwd_entry.pw_gecos = const_cast<char*>(gecos.characters_without_null_termination());
    passwd_entry.pw_dir = const_cast<char*>(dir.characters_without_null_termination());
    passwd_entry.pw_shell = const_cast<char*>(shell.characters_without_null_termination());

    return true;
}

struct passwd* getpwent()
{
    static struct passwd passwd_entry;
    static char buffer[1024];
    struct passwd* result;
    if (getpwent_r(&passwd_entry, buffer, sizeof(buffer), &result) < 0)
        return nullptr;
    return result;
}

int getpwent_r(struct passwd* passwd_buf, char* buffer, size_t buffer_size, struct passwd** passwd_entry_ptr)
{
    if (!s_stream)
        setpwent();

    while (true) {
        if (!s_stream || feof(s_stream)) {
            *passwd_entry_ptr = nullptr;
            return ENOENT;
        }

        if (ferror(s_stream)) {
            *passwd_entry_ptr = nullptr;
            return ferror(s_stream);
        }

        ++s_line_number;
        char* s = fgets(buffer, buffer_size, s_stream);

        if ((!s || !s[0]) && feof(s_stream)) {
            *passwd_entry_ptr = nullptr;
            return ENOENT;
        }

        if (strlen(s) == buffer_size - 1) {
            *passwd_entry_ptr = nullptr;
            return ERANGE;
        }

        if (parse_pwddb_entry(buffer, *passwd_buf)) {
            *passwd_entry_ptr = passwd_buf;
            return 0;
        }
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getpwnam.html
int getpwnam_r(char const* name, struct passwd* pwd, char* buffer, size_t bufsize, struct passwd** result)
{
    setpwent();
    for (;;) {
        if (auto rc = getpwent_r(pwd, buffer, bufsize, result); rc != 0)
            return rc;
        if (strcmp(pwd->pw_name, name) == 0)
            return 0;
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getpwuid.html
int getpwuid_r(uid_t uid, struct passwd* pwd, char* buffer, size_t bufsize, struct passwd** result)
{
    setpwent();
    for (;;) {
        if (auto rc = getpwent_r(pwd, buffer, bufsize, result); rc != 0)
            return rc;
        if (pwd->pw_uid == uid)
            return 0;
    }
}

int putpwent(const struct passwd* p, FILE* stream)
{
    if (!p || !stream || !p->pw_passwd || !p->pw_name || !p->pw_dir || !p->pw_gecos || !p->pw_shell) {
        errno = EINVAL;
        return -1;
    }

    auto is_valid_field = [](char const* str) {
        return str && !strpbrk(str, ":\n");
    };

    if (!is_valid_field(p->pw_name) || !is_valid_field(p->pw_dir) || !is_valid_field(p->pw_gecos) || !is_valid_field(p->pw_shell)) {
        errno = EINVAL;
        return -1;
    }

    int nwritten = fprintf(stream, "%s:%s:%u:%u:%s,,,:%s:%s\n", p->pw_name, p->pw_passwd, p->pw_uid, p->pw_gid, p->pw_gecos, p->pw_dir, p->pw_shell);
    if (!nwritten || nwritten < 0) {
        errno = ferror(stream);
        return -1;
    }

    return 0;
}
}
