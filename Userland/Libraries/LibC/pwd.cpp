/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <AK/Vector.h>
#include <errno_numbers.h>
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
        fprintf(stderr, "getpwent(): Malformed entry on line %u\n", s_line_number);
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
        fprintf(stderr, "getpwent(): Malformed UID on line %u\n", s_line_number);
        return false;
    }
    auto gid = gid_string.to_uint();
    if (!gid.has_value()) {
        fprintf(stderr, "getpwent(): Malformed GID on line %u\n", s_line_number);
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
            fprintf(stderr, "getpwent(): Read error: %s\n", strerror(ferror(s_stream)));
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
