/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/BitCast.h>
#include <AK/PrintfImplementation.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <bits/stdio_file_implementation.h>
#include <errno.h>
#include <stdio.h>
#include <wchar.h>

static_assert(AssertSize<wchar_t, sizeof(u32)>());

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fwide.html
int fwide(FILE*, int mode)
{
    // Nope Nope Nope.
    return mode;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fgetwc.html
wint_t fgetwc(FILE* stream)
{
    VERIFY(stream);
    Array<u8, 4> underlying;
    auto underlying_bytes = underlying.span();
    size_t encoded_length = 0;
    size_t bytes_read = 0;
    do {
        size_t nread = fread(underlying_bytes.offset_pointer(bytes_read), 1, 1, stream);
        if (nread != 1) {
            errno = EILSEQ;
            return WEOF;
        }
        ++bytes_read;
        if (bytes_read == 1) {
            if (underlying[0] >> 7 == 0) {
                encoded_length = 1;
            } else if (underlying[0] >> 5 == 6) {
                encoded_length = 2;
            } else if (underlying[0] >> 4 == 0xe) {
                encoded_length = 3;
            } else if (underlying[0] >> 3 == 0x1e) {
                encoded_length = 4;
            } else {
                errno = EILSEQ;
                return WEOF;
            }
        }
    } while (bytes_read < encoded_length);

    wchar_t code_point;
    auto read_bytes = mbrtowc(&code_point, bit_cast<char const*>(underlying.data()), encoded_length, nullptr);
    VERIFY(read_bytes == encoded_length);
    return code_point;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getwc.html
wint_t getwc(FILE* stream)
{
    return fgetwc(stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getwchar.html
wint_t getwchar()
{
    return getwc(stdin);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fputwc.html
wint_t fputwc(wchar_t wc, FILE* stream)
{
    VERIFY(stream);
    // Negative wide chars are weird
    if constexpr (IsSigned<wchar_t>) {
        if (wc < 0) {
            errno = EILSEQ;
            return WEOF;
        }
    }
    StringBuilder sb;
    sb.append_code_point(static_cast<u32>(wc));
    auto bytes = sb.string_view().bytes();
    ScopedFileLock lock(stream);
    size_t nwritten = stream->write(bytes.data(), bytes.size());
    if (nwritten < bytes.size())
        return WEOF;
    return wc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/putwc.html
wint_t putwc(wchar_t wc, FILE* stream)
{
    return fputwc(wc, stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/putwchar.html
wint_t putwchar(wchar_t wc)
{
    return fputwc(wc, stdout);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fgetws.html
wchar_t* fgetws(wchar_t* __restrict buffer, int size, FILE* __restrict stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    bool ok = stream->gets(bit_cast<u32*>(buffer), size);
    return ok ? buffer : nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fputws.html
int fputws(wchar_t const* __restrict ws, FILE* __restrict stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    int size = 0;
    for (auto const* p = ws; *p != 0; ++p, ++size) {
        if (putwc(*p, stream) == WEOF)
            return WEOF;
    }
    return size;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ungetwc.html
wint_t ungetwc(wint_t wc, FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    StringBuilder sb;
    sb.append_code_point(static_cast<u32>(wc));
    auto bytes = sb.string_view().bytes();
    size_t ok_bytes = 0;
    for (auto byte : bytes) {
        if (!stream->ungetc(byte)) {
            // Discard the half-ungotten bytes.
            stream->read(const_cast<u8*>(bytes.data()), ok_bytes);
            return WEOF;
        }
        ++ok_bytes;
    }
    return wc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wprintf.html
int wprintf(wchar_t const* __restrict format, ...)
{
    va_list ap;
    va_start(ap, format);
    auto rc = vfwprintf(stdout, format, ap);
    va_end(ap);
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fwprintf.html
int fwprintf(FILE* __restrict stream, wchar_t const* __restrict format, ...)
{
    va_list ap;
    va_start(ap, format);
    auto rc = vfwprintf(stream, format, ap);
    va_end(ap);
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/swprintf.html
int swprintf(wchar_t* __restrict wcs, size_t max_length, wchar_t const* __restrict format, ...)
{
    va_list ap;
    va_start(ap, format);
    auto rc = vswprintf(wcs, max_length, format, ap);
    va_end(ap);
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vwprintf.html
int vwprintf(wchar_t const* __restrict format, va_list args)
{
    return vfwprintf(stdout, format, args);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vfwprintf.html
int vfwprintf(FILE* __restrict stream, wchar_t const* __restrict format, va_list args)
{
    auto const* fmt = bit_cast<wchar_t const*>(format);
    return printf_internal([stream](wchar_t*&, wchar_t wc) {
        putwc(wc, stream);
    },
        nullptr, fmt, args);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vswprintf.html
int vswprintf(wchar_t* __restrict wcs, size_t max_length, wchar_t const* __restrict format, va_list args)
{
    auto const* fmt = bit_cast<wchar_t const*>(format);
    size_t length_so_far = 0;
    printf_internal([max_length, &length_so_far](wchar_t*& buffer, wchar_t wc) {
        if (length_so_far > max_length)
            return;
        *buffer++ = wc;
        ++length_so_far;
    },
        wcs, fmt, args);
    if (length_so_far < max_length)
        wcs[length_so_far] = L'\0';
    else
        wcs[max_length - 1] = L'\0';
    return static_cast<int>(length_so_far);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fwscanf.html
int fwscanf(FILE* __restrict stream, wchar_t const* __restrict format, ...)
{
    va_list ap;
    va_start(ap, format);
    auto rc = vfwscanf(stream, format, ap);
    va_end(ap);
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/swscanf.html
int swscanf(wchar_t const* __restrict ws, wchar_t const* __restrict format, ...)
{
    va_list ap;
    va_start(ap, format);
    auto rc = vswscanf(ws, format, ap);
    va_end(ap);
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/wscanf.html
int wscanf(wchar_t const* __restrict format, ...)
{
    va_list ap;
    va_start(ap, format);
    auto rc = vfwscanf(stdout, format, ap);
    va_end(ap);
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vfwscanf.html
int vfwscanf(FILE* __restrict stream, wchar_t const* __restrict format, va_list arg)
{
    (void)stream;
    (void)format;
    (void)arg;
    dbgln("FIXME: Implement vfwscanf()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vswscanf.html
int vswscanf(wchar_t const* __restrict ws, wchar_t const* __restrict format, va_list arg)
{
    (void)ws;
    (void)format;
    (void)arg;
    dbgln("FIXME: Implement vswscanf()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vwscanf.html
int vwscanf(wchar_t const* __restrict format, va_list arg)
{
    (void)format;
    (void)arg;
    dbgln("FIXME: Implement vwscanf()");
    TODO();
}
}
