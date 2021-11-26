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

int __attribute__((weak)) tgetent([[maybe_unused]] char* bp, [[maybe_unused]] const char* name)
{
    warnln_if(TERMCAP_DEBUG, "tgetent: bp={:p}, name='{}'", bp, name);
    PC = '\0';
    BC = const_cast<char*>("\033[D");
    UP = const_cast<char*>("\033[A");
    return 1;
}

static HashMap<String, const char*>* caps = nullptr;

static void ensure_caps()
{
    if (caps)
        return;
    caps = new HashMap<String, const char*>;
    caps->set("DC", "\033[%p1%dP");
    caps->set("IC", "\033[%p1%d@");
    caps->set("ce", "\033[K");
    caps->set("cl", "\033[H\033[J");
    caps->set("cr", "\015");
    caps->set("dc", "\033[P");
    caps->set("ei", "");
    caps->set("ic", "");
    caps->set("im", "");
    caps->set("kd", "\033[B");
    caps->set("kl", "\033[D");
    caps->set("kr", "\033[C");
    caps->set("ku", "\033[A");
    caps->set("ks", "");
    caps->set("ke", "");
    caps->set("le", "\033[D");
    caps->set("mm", "");
    caps->set("mo", "");
    caps->set("pc", "");
    caps->set("up", "\033[A");
    caps->set("vb", "");
    caps->set("am", "");
    caps->set("@7", "");
    caps->set("kH", "");
    caps->set("kI", "\033[L");
    caps->set("kh", "\033[H");
    caps->set("vs", "");
    caps->set("ve", "");
    caps->set("E3", "");
    caps->set("kD", "");
    caps->set("nd", "\033[C");

    caps->set("co", "80");
    caps->set("li", "25");
}

// Unfortunately, tgetstr() doesn't accept a size argument for the buffer
// pointed to by area, so we have to use bare strcpy().
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

char* __attribute__((weak)) tgetstr(const char* id, char** area)
{
    ensure_caps();
    warnln_if(TERMCAP_DEBUG, "tgetstr: id='{}'", id);
    auto it = caps->find(id);
    if (it != caps->end()) {
        char* ret = *area;
        const char* val = (*it).value;
        strcpy(*area, val);
        *area += strlen(val) + 1;
        return ret;
    }
    warnln_if(TERMCAP_DEBUG, "tgetstr: missing cap id='{}'", id);
    return nullptr;
}

#pragma GCC diagnostic pop

int __attribute__((weak)) tgetflag([[maybe_unused]] const char* id)
{
    warnln_if(TERMCAP_DEBUG, "tgetflag: '{}'", id);
    auto it = caps->find(id);
    if (it != caps->end())
        return 1;
    return 0;
}

int __attribute__((weak)) tgetnum(const char* id)
{
    warnln_if(TERMCAP_DEBUG, "tgetnum: '{}'", id);
    auto it = caps->find(id);
    if (it != caps->end())
        return atoi((*it).value);
    return -1;
}

static Vector<char> s_tgoto_buffer;
char* __attribute__((weak)) tgoto([[maybe_unused]] const char* cap, [[maybe_unused]] int col, [[maybe_unused]] int row)
{
    auto cap_str = StringView(cap).replace("%p1%d", String::number(col)).replace("%p2%d", String::number(row));

    s_tgoto_buffer.clear_with_capacity();
    s_tgoto_buffer.ensure_capacity(cap_str.length());
    (void)cap_str.copy_characters_to_buffer(s_tgoto_buffer.data(), cap_str.length());
    return s_tgoto_buffer.data();
}

int __attribute__((weak)) tputs(const char* str, [[maybe_unused]] int affcnt, int (*putc)(int))
{
    size_t len = strlen(str);
    for (size_t i = 0; i < len; ++i)
        putc(str[i]);
    return 0;
}
}
