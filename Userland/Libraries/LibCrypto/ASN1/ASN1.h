/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
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

#pragma once

#include <AK/Types.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>

namespace Crypto {

namespace ASN1 {

enum class Kind {
    Eol,
    Boolean,
    Integer,
    ShortInteger,
    BitString,
    OctetString,
    Null,
    ObjectIdentifier,
    IA5String,
    PrintableString,
    Utf8String,
    UTCTime,
    Choice,
    Sequence,
    Set,
    SetOf
};

static inline StringView kind_name(Kind kind)
{
    switch (kind) {
    case Kind::Eol:
        return "EndOfList";
    case Kind::Boolean:
        return "Boolean";
    case Kind::Integer:
        return "Integer";
    case Kind::ShortInteger:
        return "ShortInteger";
    case Kind::BitString:
        return "BitString";
    case Kind::OctetString:
        return "OctetString";
    case Kind::Null:
        return "Null";
    case Kind::ObjectIdentifier:
        return "ObjectIdentifier";
    case Kind::IA5String:
        return "IA5String";
    case Kind::PrintableString:
        return "PrintableString";
    case Kind::Utf8String:
        return "UTF8String";
    case Kind::UTCTime:
        return "UTCTime";
    case Kind::Choice:
        return "Choice";
    case Kind::Sequence:
        return "Sequence";
    case Kind::Set:
        return "Set";
    case Kind::SetOf:
        return "SetOf";
    }

    return "InvalidKind";
}

struct List {
    Kind kind;
    void* data;
    size_t size;
    bool used;
    List *prev, *next, *child, *parent;
};

static constexpr void set(List& list, Kind type, void* data, size_t size)
{
    list.kind = type;
    list.data = data;
    list.size = size;
    list.used = false;
}
}

}
