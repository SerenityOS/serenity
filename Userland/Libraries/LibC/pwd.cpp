/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/TemporaryChange.h>
#include <AK/Vector.h>
#include <errno.h>
#include <pwd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

extern "C" {

static FILE* s_stream = nullptr;
static unsigned s_line_number = 0;
static struct passwd s_passwd_entry;

static String s_name;
static String s_passwd;
static String s_gecos;
static String s_dir;
static String s_shell;

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

    memset(&s_passwd_entry, 0, sizeof(s_passwd_entry));

    s_name = {};
    s_passwd = {};
    s_gecos = {};
    s_dir = {};
    s_shell = {};
}

struct passwd* getpwuid(uid_t uid)
{
    setpwent();
    while (auto* pw = getpwent()) {
        if (pw->pw_uid == uid)
            return pw;
    }
    return nullptr;
}

struct passwd* getpwnam(const char* name)
{
    setpwent();
    while (auto* pw = getpwent()) {
        if (!strcmp(pw->pw_name, name))
            return pw;
    }
    return nullptr;
}

static bool parse_pwddb_entry(const String& line)
{
    auto parts = line.split_view(':', true);
    if (parts.size() != 7) {
        dbgln("getpwent(): Malformed entry on line {}", s_line_number);
        return false;
    }

    s_name = parts[0];
    s_passwd = parts[1];
    auto& uid_string = parts[2];
    auto& gid_string = parts[3];
    s_gecos = parts[4];
    s_dir = parts[5];
    s_shell = parts[6];

    auto uid = uid_string.to_uint();
    if (!uid.has_value()) {
        dbgln("getpwent(): Malformed UID on line {}", s_line_number);
        return false;
    }
    auto gid = gid_string.to_uint();
    if (!gid.has_value()) {
        dbgln("getpwent(): Malformed GID on line {}", s_line_number);
        return false;
    }

    s_passwd_entry.pw_name = const_cast<char*>(s_name.characters());
    s_passwd_entry.pw_passwd = const_cast<char*>(s_passwd.characters());
    s_passwd_entry.pw_uid = uid.value();
    s_passwd_entry.pw_gid = gid.value();
    s_passwd_entry.pw_gecos = const_cast<char*>(s_gecos.characters());
    s_passwd_entry.pw_dir = const_cast<char*>(s_dir.characters());
    s_passwd_entry.pw_shell = const_cast<char*>(s_shell.characters());

    return true;
}

struct passwd* getpwent()
{
    if (!s_stream)
        setpwent();

    while (true) {
        if (!s_stream || feof(s_stream))
            return nullptr;

        if (ferror(s_stream)) {
            dbgln("getpwent(): Read error: {}", strerror(ferror(s_stream)));
            return nullptr;
        }

        char buffer[1024];
        ++s_line_number;
        char* s = fgets(buffer, sizeof(buffer), s_stream);

        // Silently tolerate an empty line at the end.
        if ((!s || !s[0]) && feof(s_stream))
            return nullptr;

        String line(s, Chomp);
        if (parse_pwddb_entry(line))
            return &s_passwd_entry;
        // Otherwise, proceed to the next line.
    }
}

static void construct_pwd(struct passwd* pwd, char* buf, struct passwd** result)
{
    auto* buf_name = &buf[0];
    auto* buf_passwd = &buf[s_name.length() + 1];
    auto* buf_gecos = &buf[s_name.length() + 1 + s_gecos.length() + 1];
    auto* buf_dir = &buf[s_gecos.length() + 1 + s_name.length() + 1 + s_gecos.length() + 1];
    auto* buf_shell = &buf[s_dir.length() + 1 + s_gecos.length() + 1 + s_name.length() + 1 + s_gecos.length() + 1];

    bool ok = true;
    ok = ok && s_name.copy_characters_to_buffer(buf_name, s_name.length() + 1);
    ok = ok && s_passwd.copy_characters_to_buffer(buf_passwd, s_passwd.length() + 1);
    ok = ok && s_gecos.copy_characters_to_buffer(buf_gecos, s_gecos.length() + 1);
    ok = ok && s_dir.copy_characters_to_buffer(buf_dir, s_dir.length() + 1);
    ok = ok && s_shell.copy_characters_to_buffer(buf_shell, s_shell.length() + 1);

    VERIFY(ok);

    *result = pwd;
    pwd->pw_name = buf_name;
    pwd->pw_passwd = buf_passwd;
    pwd->pw_gecos = buf_gecos;
    pwd->pw_dir = buf_dir;
    pwd->pw_shell = buf_shell;
}

int getpwnam_r(const char* name, struct passwd* pwd, char* buf, size_t buflen, struct passwd** result)
{
    // FIXME: This is a HACK!
    TemporaryChange name_change { s_name, {} };
    TemporaryChange passwd_change { s_passwd, {} };
    TemporaryChange gecos_change { s_gecos, {} };
    TemporaryChange dir_change { s_dir, {} };
    TemporaryChange shell_change { s_shell, {} };

    setpwent();
    bool found = false;
    while (auto* pw = getpwent()) {
        if (!strcmp(pw->pw_name, name)) {
            found = true;
            break;
        }
    }

    if (!found) {
        *result = nullptr;
        return 0;
    }

    const auto total_buffer_length = s_name.length() + s_passwd.length() + s_gecos.length() + s_dir.length() + s_shell.length() + 5;
    if (buflen < total_buffer_length)
        return ERANGE;

    construct_pwd(pwd, buf, result);
    return 0;
}

int getpwuid_r(uid_t uid, struct passwd* pwd, char* buf, size_t buflen, struct passwd** result)
{
    // FIXME: This is a HACK!
    TemporaryChange name_change { s_name, {} };
    TemporaryChange passwd_change { s_passwd, {} };
    TemporaryChange gecos_change { s_gecos, {} };
    TemporaryChange dir_change { s_dir, {} };
    TemporaryChange shell_change { s_shell, {} };

    setpwent();
    bool found = false;
    while (auto* pw = getpwent()) {
        if (pw->pw_uid == uid) {
            found = true;
            break;
        }
    }

    if (!found) {
        *result = nullptr;
        return 0;
    }

    const auto total_buffer_length = s_name.length() + s_passwd.length() + s_gecos.length() + s_dir.length() + s_shell.length() + 5;
    if (buflen < total_buffer_length)
        return ERANGE;

    construct_pwd(pwd, buf, result);
    return 0;
}

int putpwent(const struct passwd* p, FILE* stream)
{
    if (!p || !stream || !p->pw_passwd || !p->pw_name || !p->pw_dir || !p->pw_gecos || !p->pw_shell) {
        errno = EINVAL;
        return -1;
    }

    auto is_valid_field = [](const char* str) {
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
