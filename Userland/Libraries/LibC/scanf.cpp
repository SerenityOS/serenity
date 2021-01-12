/*
 * Copyright (c) 2000-2002 Opsycon AB  (www.opsycon.se)
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by Opsycon AB.
 * 4. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */
#include <AK/Assertions.h>
#include <ctype.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

static const char* determine_base(const char* p, int& base)
{
    if (p[0] == '0') {
        switch (p[1]) {
        case 'x':
            base = 16;
            break;
        case 't':
        case 'n':
            base = 10;
            break;
        case 'o':
            base = 8;
            break;
        default:
            base = 10;
            return p;
        }
        return p + 2;
    }
    base = 10;
    return p;
}

static int _atob(unsigned long* vp, const char* p, int base)
{
    unsigned long value, v1, v2;
    const char* q;
    char tmp[20];
    int digit;

    if (p[0] == '0' && (p[1] == 'x' || p[1] == 'X')) {
        base = 16;
        p += 2;
    }

    if (base == 16 && (q = strchr(p, '.')) != 0) {
        if (q - p > (ssize_t)sizeof(tmp) - 1)
            return 0;
        memcpy(tmp, p, q - p);
        tmp[q - p] = '\0';

        if (!_atob(&v1, tmp, 16))
            return 0;
        ++q;
        if (strchr(q, '.'))
            return 0;
        if (!_atob(&v2, q, 16))
            return 0;
        *vp = (v1 << 16) + v2;
        return 1;
    }

    value = *vp = 0;
    for (; *p; p++) {
        if (*p >= '0' && *p <= '9')
            digit = *p - '0';
        else if (*p >= 'a' && *p <= 'f')
            digit = *p - 'a' + 10;
        else if (*p >= 'A' && *p <= 'F')
            digit = *p - 'A' + 10;
        else
            return 0;

        if (digit >= base)
            return 0;
        value *= base;
        value += digit;
    }
    *vp = value;
    return 1;
}

static int atob(unsigned int* vp, const char* p, int base)
{
    unsigned long v;

    if (base == 0)
        p = determine_base(p, base);
    if (_atob(&v, p, base)) {
        *vp = v;
        return 1;
    }
    return 0;
}

#define ISSPACE " \t\n\r\f\v"

int vsscanf(const char* buf, const char* s, va_list ap)
{
    int base = 10;
    char* t;
    char tmp[BUFSIZ];
    bool noassign = false;
    int count = 0;
    int width = 0;

    // FIXME: This doesn't work quite right. For example, it fails to match 'SSH-2.0-OpenSSH_8.2p1 Ubuntu-4ubuntu0.1\r\n'
    //        with 'SSH-%d.%d-%[^\n]\n'

    while (*s && *buf) {
        while (isspace(*s))
            s++;
        if (*s == '%') {
            s++;
            for (; *s; s++) {
                if (strchr("dibouxcsefg%", *s))
                    break;
                if (*s == '*')
                    noassign = true;
                else if (*s >= '1' && *s <= '9') {
                    const char* tc;
                    for (tc = s; isdigit(*s); s++)
                        ;
                    ASSERT((ssize_t)sizeof(tmp) >= s - tc + 1);
                    memcpy(tmp, tc, s - tc);
                    tmp[s - tc] = '\0';
                    atob((uint32_t*)&width, tmp, 10);
                    s--;
                }
            }
            if (*s == 's') {
                while (isspace(*buf))
                    buf++;
                if (!width)
                    width = strcspn(buf, ISSPACE);
                if (!noassign) {
                    // In this case, we have no way to ensure the user buffer is not overflown :(
                    memcpy(t = va_arg(ap, char*), buf, width);
                    t[width] = '\0';
                }
                buf += width;
            } else if (*s == 'c') {
                if (!width)
                    width = 1;
                if (!noassign) {
                    memcpy(t = va_arg(ap, char*), buf, width);
                    // No null terminator!
                }
                buf += width;
            } else if (strchr("dobxu", *s)) {
                while (isspace(*buf))
                    buf++;
                if (*s == 'd' || *s == 'u')
                    base = 10;
                else if (*s == 'x')
                    base = 16;
                else if (*s == 'o')
                    base = 8;
                else if (*s == 'b')
                    base = 2;
                if (!width) {
                    if (isspace(*(s + 1)) || *(s + 1) == 0) {
                        width = strcspn(buf, ISSPACE);
                    } else {
                        auto* p = strchr(buf, *(s + 1));
                        if (p)
                            width = p - buf;
                        else {
                            noassign = true;
                            width = 0;
                        }
                    }
                }
                memcpy(tmp, buf, width);
                tmp[width] = '\0';
                buf += width;
                if (!noassign) {
                    if (!atob(va_arg(ap, uint32_t*), tmp, base))
                        noassign = true;
                }
            }
            if (!noassign)
                ++count;
            width = 0;
            noassign = false;
            ++s;
        } else {
            while (isspace(*buf))
                buf++;
            if (*s != *buf)
                break;
            else {
                ++s;
                ++buf;
            }
        }
    }
    return count;
}
