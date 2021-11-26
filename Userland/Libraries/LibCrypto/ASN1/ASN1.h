/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibCore/DateTime.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>

namespace Crypto::ASN1 {

enum class Kind : u8 {
    Eol,
    Boolean = 0x01,
    Integer = 0x02,
    BitString = 0x03,
    OctetString = 0x04,
    Null = 0x05,
    ObjectIdentifier = 0x06,
    IA5String = 0x16,
    PrintableString = 0x13,
    Utf8String = 0x0c,
    UTCTime = 0x017,
    GeneralizedTime = 0x018,
    Sequence = 0x10,
    Set = 0x11,
    // Choice = ??,
};

enum class Class : u8 {
    Universal = 0,
    Application = 0x40,
    Context = 0x80,
    Private = 0xc0,
};

enum class Type : u8 {
    Primitive = 0,
    Constructed = 0x20,
};

struct Tag {
    Kind kind;
    Class class_;
    Type type;
};

String kind_name(Kind);
String class_name(Class);
String type_name(Type);

Optional<Core::DateTime> parse_utc_time(StringView);
Optional<Core::DateTime> parse_generalized_time(StringView);

}
