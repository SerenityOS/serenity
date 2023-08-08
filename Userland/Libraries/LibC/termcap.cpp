/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <assert.h>
#include <string.h>
#include <termcap.h>

extern "C" {

char PC;
char* UP;
char* BC;

int __attribute__((weak)) tgetent([[maybe_unused]] char* bp, [[maybe_unused]] char const* name)
{
    warnln_if(TERMCAP_DEBUG, "tgetent: bp={:p}, name='{}'", bp, name);
    PC = '\0';
    BC = const_cast<char*>("\033[D");
    UP = const_cast<char*>("\033[A");
    return 1;
}

static HashMap<String, char const*>* caps = nullptr;

static void ensure_caps()
{
    if (caps)
        return;
    caps = new HashMap<String, char const*>;
    caps->set("DC"_string, "\033[%p1%dP");
    caps->set("IC"_string, "\033[%p1%d@");
    caps->set("ce"_string, "\033[K");
    caps->set("cl"_string, "\033[H\033[J");
    caps->set("cr"_string, "\015");
    caps->set("dc"_string, "\033[P");
    caps->set("ei"_string, "");
    caps->set("ic"_string, "");
    caps->set("im"_string, "");
    caps->set("kd"_string, "\033[B");
    caps->set("kl"_string, "\033[D");
    caps->set("kr"_string, "\033[C");
    caps->set("ku"_string, "\033[A");
    caps->set("ks"_string, "");
    caps->set("ke"_string, "");
    caps->set("le"_string, "\033[D");
    caps->set("mm"_string, "");
    caps->set("mo"_string, "");
    caps->set("pc"_string, "");
    caps->set("up"_string, "\033[A");
    caps->set("vb"_string, "");
    caps->set("am"_string, "");
    caps->set("@7"_string, "");
    caps->set("kH"_string, "");
    caps->set("kI"_string, "\033[L");
    caps->set("kh"_string, "\033[H");
    caps->set("vs"_string, "");
    caps->set("ve"_string, "");
    caps->set("E3"_string, "");
    caps->set("kD"_string, "");
    caps->set("nd"_string, "\033[C");

    caps->set("co"_string, "80");
    caps->set("li"_string, "25");
}

// Unfortunately, tgetstr() doesn't accept a size argument for the buffer
// pointed to by area, so we have to use bare strcpy().
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

char* __attribute__((weak)) tgetstr(char const* id, char** area)
{
    ensure_caps();
    warnln_if(TERMCAP_DEBUG, "tgetstr: id='{}'", id);
    auto it = caps->find(StringView { id, strlen(id) });
    if (it != caps->end()) {
        char* ret = *area;
        char const* val = (*it).value;
        strcpy(*area, val);
        *area += strlen(val) + 1;
        return ret;
    }
    warnln_if(TERMCAP_DEBUG, "tgetstr: missing cap id='{}'", id);
    return nullptr;
}

#pragma GCC diagnostic pop

int __attribute__((weak)) tgetflag([[maybe_unused]] char const* id)
{
    warnln_if(TERMCAP_DEBUG, "tgetflag: '{}'", id);
    auto it = caps->find(StringView { id, strlen(id) });
    if (it != caps->end())
        return 1;
    return 0;
}

int __attribute__((weak)) tgetnum(char const* id)
{
    warnln_if(TERMCAP_DEBUG, "tgetnum: '{}'", id);
    auto it = caps->find(StringView { id, strlen(id) });
    if (it != caps->end())
        return atoi((*it).value);
    return -1;
}

static Vector<char> s_tgoto_buffer;
char* __attribute__((weak)) tgoto([[maybe_unused]] char const* cap, [[maybe_unused]] int col, [[maybe_unused]] int row)
{
    auto row_string = String::number(row);
    auto column_string = String::number(col);
    if (row_string.is_error() || column_string.is_error()) {
        // According to Linux man pages (https://man7.org/linux/man-pages/man3/tgoto.3x.html) we apparently don't have to set errno.
        return nullptr;
    }
    auto cap_str = StringView { cap, strlen(cap) }.replace("%p1%d"sv, column_string.release_value(), ReplaceMode::FirstOnly).replace("%p2%d"sv, row_string.release_value(), ReplaceMode::FirstOnly);

    s_tgoto_buffer.clear_with_capacity();
    s_tgoto_buffer.ensure_capacity(cap_str.length());
    (void)cap_str.copy_characters_to_buffer(s_tgoto_buffer.data(), cap_str.length());
    return s_tgoto_buffer.data();
}

int __attribute__((weak)) tputs(char const* str, [[maybe_unused]] int affcnt, int (*putc)(int))
{
    size_t len = strlen(str);
    for (size_t i = 0; i < len; ++i)
        putc(str[i]);
    return 0;
}
}
