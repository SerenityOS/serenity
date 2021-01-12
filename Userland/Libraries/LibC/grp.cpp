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
#include <grp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

extern "C" {

static FILE* s_stream = nullptr;
static unsigned s_line_number = 0;
static struct group s_group;

static String s_name;
static String s_passwd;
static Vector<String> s_members;
static Vector<const char*> s_members_ptrs;

void setgrent()
{
    s_line_number = 0;
    if (s_stream) {
        rewind(s_stream);
    } else {
        s_stream = fopen("/etc/group", "r");
        if (!s_stream) {
            perror("open /etc/group");
        }
    }
}

void endgrent()
{
    s_line_number = 0;
    if (s_stream) {
        fclose(s_stream);
        s_stream = nullptr;
    }

    memset(&s_group, 0, sizeof(s_group));

    s_name = {};
    s_passwd = {};
    s_members = {};
    s_members_ptrs = {};
}

struct group* getgrgid(gid_t gid)
{
    setgrent();
    while (auto* gr = getgrent()) {
        if (gr->gr_gid == gid)
            return gr;
    }
    return nullptr;
}

struct group* getgrnam(const char* name)
{
    setgrent();
    while (auto* gr = getgrent()) {
        if (!strcmp(gr->gr_name, name))
            return gr;
    }
    return nullptr;
}

static bool parse_grpdb_entry(const String& line)
{
    auto parts = line.split_view(':', true);
    if (parts.size() != 4) {
        fprintf(stderr, "getgrent(): Malformed entry on line %u: '%s' has %zu parts\n", s_line_number, line.characters(), parts.size());
        return false;
    }

    s_name = parts[0];
    s_passwd = parts[1];

    auto& gid_string = parts[2];
    String members_string = parts[3];

    auto gid = gid_string.to_uint();
    if (!gid.has_value()) {
        fprintf(stderr, "getgrent(): Malformed GID on line %u\n", s_line_number);
        return false;
    }

    s_members = members_string.split(',');
    s_members_ptrs.clear_with_capacity();
    s_members_ptrs.ensure_capacity(s_members.size() + 1);
    for (auto& member : s_members) {
        s_members_ptrs.append(member.characters());
    }
    s_members_ptrs.append(nullptr);

    s_group.gr_gid = gid.value();
    s_group.gr_name = const_cast<char*>(s_name.characters());
    s_group.gr_passwd = const_cast<char*>(s_passwd.characters());
    s_group.gr_mem = const_cast<char**>(s_members_ptrs.data());

    return true;
}

struct group* getgrent()
{
    if (!s_stream)
        setgrent();

    while (true) {
        if (!s_stream || feof(s_stream))
            return nullptr;

        if (ferror(s_stream)) {
            fprintf(stderr, "getgrent(): Read error: %s\n", strerror(ferror(s_stream)));
            return nullptr;
        }

        char buffer[1024];
        ++s_line_number;
        char* s = fgets(buffer, sizeof(buffer), s_stream);

        // Silently tolerate an empty line at the end.
        if ((!s || !s[0]) && feof(s_stream))
            return nullptr;

        String line(s, Chomp);
        if (parse_grpdb_entry(line))
            return &s_group;
        // Otherwise, proceed to the next line.
    }
}

int initgroups(const char* user, gid_t extra_gid)
{
    size_t count = 0;
    gid_t gids[32];
    bool extra_gid_added = false;
    setgrent();
    while (auto* gr = getgrent()) {
        for (auto* mem = gr->gr_mem; *mem; ++mem) {
            if (!strcmp(*mem, user)) {
                gids[count++] = gr->gr_gid;
                if (gr->gr_gid == extra_gid)
                    extra_gid_added = true;
                break;
            }
        }
    }
    endgrent();
    if (!extra_gid_added)
        gids[count++] = extra_gid;
    return setgroups(count, gids);
}
}
