/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/TemporaryChange.h>
#include <AK/Vector.h>
#include <errno.h>
#include <shadow.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

extern "C" {

static FILE* s_stream = nullptr;
static unsigned s_line_number = 0;
static struct spwd s_shadow_entry;

static ByteString s_name;
static ByteString s_pwdp;

void setspent()
{
    s_line_number = 0;
    if (s_stream) {
        rewind(s_stream);
    } else {
        s_stream = fopen("/etc/shadow", "r");
        if (!s_stream) {
            dbgln("open /etc/shadow failed: {}", strerror(errno));
        }
    }
}

void endspent()
{
    s_line_number = 0;
    if (s_stream) {
        fclose(s_stream);
        s_stream = nullptr;
    }

    memset(&s_shadow_entry, 0, sizeof(s_shadow_entry));

    s_name = {};
    s_pwdp = {};
}

struct spwd* getspnam(char const* name)
{
    setspent();
    while (auto* sp = getspent()) {
        if (!strcmp(sp->sp_namp, name)) {
            return sp;
        }
    }
    return nullptr;
}

static bool parse_shadow_entry(ByteString const& line)
{
    auto parts = line.split_view(':', SplitBehavior::KeepEmpty);
    if (parts.size() != 9) {
        dbgln("getspent(): Malformed entry on line {}", s_line_number);
        return false;
    }

    s_name = parts[0];
    s_pwdp = parts[1];
    auto& lstchg_string = parts[2];
    auto& min_string = parts[3];
    auto& max_string = parts[4];
    auto& warn_string = parts[5];
    auto& inact_string = parts[6];
    auto& expire_string = parts[7];
    auto& flag_string = parts[8];

    auto lstchg = lstchg_string.to_number<int>();
    if (!lstchg.has_value()) {
        dbgln("getspent(): Malformed lstchg on line {}", s_line_number);
        return false;
    }

    if (min_string.is_empty())
        min_string = "-1"sv;
    auto min_value = min_string.to_number<int>();
    if (!min_value.has_value()) {
        dbgln("getspent(): Malformed min value on line {}", s_line_number);
        return false;
    }

    if (max_string.is_empty())
        max_string = "-1"sv;
    auto max_value = max_string.to_number<int>();
    if (!max_value.has_value()) {
        dbgln("getspent(): Malformed max value on line {}", s_line_number);
        return false;
    }

    if (warn_string.is_empty())
        warn_string = "-1"sv;
    auto warn = warn_string.to_number<int>();
    if (!warn.has_value()) {
        dbgln("getspent(): Malformed warn on line {}", s_line_number);
        return false;
    }

    if (inact_string.is_empty())
        inact_string = "-1"sv;
    auto inact = inact_string.to_number<int>();
    if (!inact.has_value()) {
        dbgln("getspent(): Malformed inact on line {}", s_line_number);
        return false;
    }

    if (expire_string.is_empty())
        expire_string = "-1"sv;
    auto expire = expire_string.to_number<int>();
    if (!expire.has_value()) {
        dbgln("getspent(): Malformed expire on line {}", s_line_number);
        return false;
    }

    if (flag_string.is_empty())
        flag_string = "0"sv;
    auto flag = flag_string.to_number<int>();
    if (!flag.has_value()) {
        dbgln("getspent(): Malformed flag on line {}", s_line_number);
        return false;
    }

    s_shadow_entry.sp_namp = const_cast<char*>(s_name.characters());
    s_shadow_entry.sp_pwdp = const_cast<char*>(s_pwdp.characters());
    s_shadow_entry.sp_lstchg = lstchg.value();
    s_shadow_entry.sp_min = min_value.value();
    s_shadow_entry.sp_max = max_value.value();
    s_shadow_entry.sp_warn = warn.value();
    s_shadow_entry.sp_inact = inact.value();
    s_shadow_entry.sp_expire = expire.value();
    s_shadow_entry.sp_flag = flag.value();

    return true;
}

struct spwd* getspent()
{
    if (!s_stream)
        setspent();

    while (true) {
        if (!s_stream || feof(s_stream))
            return nullptr;

        if (ferror(s_stream)) {
            dbgln("getspent(): Read error: {}", strerror(ferror(s_stream)));
            return nullptr;
        }

        char buffer[1024];
        ++s_line_number;
        char* s = fgets(buffer, sizeof(buffer), s_stream);

        // Silently tolerate an empty line at the end.
        if ((!s || !s[0]) && feof(s_stream))
            return nullptr;

        ByteString line(s, Chomp);
        if (parse_shadow_entry(line))
            return &s_shadow_entry;
        // Otherwise, proceed to the next line.
    }
}

static void construct_spwd(struct spwd* sp, char* buf, struct spwd** result)
{
    auto* buf_name = &buf[0];
    auto* buf_pwdp = &buf[s_name.length() + 1];

    bool ok = true;
    ok = ok && s_name.copy_characters_to_buffer(buf_name, s_name.length() + 1);
    ok = ok && s_pwdp.copy_characters_to_buffer(buf_pwdp, s_pwdp.length() + 1);

    VERIFY(ok);

    *result = sp;
    sp->sp_namp = buf_name;
    sp->sp_pwdp = buf_pwdp;
}

int getspnam_r(char const* name, struct spwd* sp, char* buf, size_t buflen, struct spwd** result)
{
    // FIXME: This is a HACK!
    TemporaryChange name_change { s_name, {} };
    TemporaryChange pwdp_change { s_pwdp, {} };

    setspent();
    bool found = false;
    while (auto* sp = getspent()) {
        if (!strcmp(sp->sp_namp, name)) {
            found = true;
            break;
        }
    }

    if (!found) {
        *result = nullptr;
        return 0;
    }

    auto const total_buffer_length = s_name.length() + s_pwdp.length() + 8;
    if (buflen < total_buffer_length)
        return ERANGE;

    construct_spwd(sp, buf, result);
    return 0;
}

int putspent(struct spwd* p, FILE* stream)
{
    if (!p || !stream || !p->sp_namp || !p->sp_pwdp) {
        errno = EINVAL;
        return -1;
    }

    auto is_valid_field = [](char const* str) {
        return str && !strpbrk(str, ":\n");
    };

    if (!is_valid_field(p->sp_namp) || !is_valid_field(p->sp_pwdp)) {
        errno = EINVAL;
        return -1;
    }

    int nwritten;

    nwritten = fprintf(stream, "%s:%s:", p->sp_namp, p->sp_pwdp);
    if (!nwritten || nwritten < 0) {
        errno = ferror(stream);
        return -1;
    }

    if (p->sp_lstchg != (long int)-1)
        nwritten = fprintf(stream, "%ld:", p->sp_lstchg);
    else
        nwritten = fprintf(stream, "%c", ':');
    if (!nwritten || nwritten < 0) {
        errno = ferror(stream);
        return -1;
    }

    if (p->sp_min != (long int)-1)
        nwritten = fprintf(stream, "%ld:", p->sp_min);
    else
        nwritten = fprintf(stream, "%c", ':');
    if (!nwritten || nwritten < 0) {
        errno = ferror(stream);
        return -1;
    }

    if (p->sp_max != (long int)-1)
        nwritten = fprintf(stream, "%ld:", p->sp_max);
    else
        nwritten = fprintf(stream, "%c", ':');
    if (!nwritten || nwritten < 0) {
        errno = ferror(stream);
        return -1;
    }

    if (p->sp_warn != (long int)-1)
        nwritten = fprintf(stream, "%ld:", p->sp_warn);
    else
        nwritten = fprintf(stream, "%c", ':');
    if (!nwritten || nwritten < 0) {
        errno = ferror(stream);
        return -1;
    }

    if (p->sp_inact != (long int)-1)
        nwritten = fprintf(stream, "%ld:", p->sp_inact);
    else
        nwritten = fprintf(stream, "%c", ':');
    if (!nwritten || nwritten < 0) {
        errno = ferror(stream);
        return -1;
    }

    if (p->sp_expire != (long int)-1)
        nwritten = fprintf(stream, "%ld:", p->sp_expire);
    else
        nwritten = fprintf(stream, "%c", ':');
    if (!nwritten || nwritten < 0) {
        errno = ferror(stream);
        return -1;
    }

    if (p->sp_flag != (unsigned long int)-1)
        nwritten = fprintf(stream, "%ld\n", p->sp_flag);
    else
        nwritten = fprintf(stream, "%c", '\n');
    if (!nwritten || nwritten < 0) {
        errno = ferror(stream);
        return -1;
    }

    return 0;
}
}
