/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <LibCrypto/ASN1/ASN1.h>

namespace Crypto::ASN1 {

DeprecatedString kind_name(Kind kind)
{
    switch (kind) {
    case Kind::Eol:
        return "EndOfList";
    case Kind::Boolean:
        return "Boolean";
    case Kind::Integer:
        return "Integer";
    case Kind::BitString:
        return "BitString";
    case Kind::OctetString:
        return "OctetString";
    case Kind::Null:
        return "Null";
    case Kind::ObjectIdentifier:
        return "ObjectIdentifier";
    case Kind::ObjectDescriptor:
        return "ObjectDescriptor";
    case Kind::External:
        return "External";
    case Kind::Real:
        return "Real";
    case Kind::Enumerated:
        return "Enumerated";
    case Kind::EmbeddedPdv:
        return "EmbeddedPdv";
    case Kind::Utf8String:
        return "Utf8String";
    case Kind::RelativeOid:
        return "RelativeOid";
    case Kind::Time:
        return "Time";
    case Kind::Reserved:
        return "Reserved";
    case Kind::Sequence:
        return "Sequence";
    case Kind::Set:
        return "Set";
    case Kind::NumericString:
        return "NumericString";
    case Kind::PrintableString:
        return "PrintableString";
    case Kind::T61String:
        return "T61String";
    case Kind::VideotexString:
        return "VideotexString";
    case Kind::IA5String:
        return "IA5String";
    case Kind::UTCTime:
        return "UTCTime";
    case Kind::GeneralizedTime:
        return "GeneralizedTime";
    case Kind::GraphicString:
        return "GraphicString";
    case Kind::VisibleString:
        return "VisibleString";
    case Kind::GeneralString:
        return "GeneralString";
    case Kind::UniversalString:
        return "UniversalString";
    case Kind::CharacterString:
        return "CharacterString";
    case Kind::BMPString:
        return "BMPString";
    case Kind::Date:
        return "Date";
    case Kind::TimeOfDay:
        return "TimeOfDay";
    case Kind::DateTime:
        return "DateTime";
    case Kind::Duration:
        return "Duration";
    case Kind::OidIri:
        return "OidIri";
    case Kind::RelativeOidIri:
        return "RelativeOidIri";
    }

    return "InvalidKind";
}

DeprecatedString class_name(Class class_)
{
    switch (class_) {
    case Class::Application:
        return "Application";
    case Class::Context:
        return "Context";
    case Class::Private:
        return "Private";
    case Class::Universal:
        return "Universal";
    }

    return "InvalidClass";
}

DeprecatedString type_name(Type type)
{
    switch (type) {
    case Type::Constructed:
        return "Constructed";
    case Type::Primitive:
        return "Primitive";
    }

    return "InvalidType";
}

Optional<Core::DateTime> parse_utc_time(StringView time)
{
    // YYMMDDhhmm[ss]Z or YYMMDDhhmm[ss](+|-)hhmm
    GenericLexer lexer(time);
    auto year_in_century = lexer.consume(2).to_uint();
    auto month = lexer.consume(2).to_uint();
    auto day = lexer.consume(2).to_uint();
    auto hour = lexer.consume(2).to_uint();
    auto minute = lexer.consume(2).to_uint();
    Optional<unsigned> seconds, offset_hours, offset_minutes;
    [[maybe_unused]] bool negative_offset = false;

    if (lexer.next_is(is_any_of("0123456789"sv))) {
        seconds = lexer.consume(2).to_uint();
        if (!seconds.has_value()) {
            return {};
        }
    }

    if (lexer.next_is('Z')) {
        lexer.consume();
    } else if (lexer.next_is(is_any_of("+-"sv))) {
        negative_offset = lexer.consume() == '-';
        offset_hours = lexer.consume(2).to_uint();
        offset_minutes = lexer.consume(2).to_uint();
        if (!offset_hours.has_value() || !offset_minutes.has_value()) {
            return {};
        }
    } else {
        return {};
    }

    if (!year_in_century.has_value() || !month.has_value() || !day.has_value() || !hour.has_value() || !minute.has_value()) {
        return {};
    }

    // RFC5280 section 4.1.2.5.1.
    auto full_year = year_in_century.value();
    full_year += (full_year < 50) ? 2000 : 1900;
    auto full_seconds = seconds.value_or(0);

    // FIXME: Handle offsets!
    if (offset_hours.has_value() || offset_minutes.has_value())
        dbgln("FIXME: Implement UTCTime with offset!");

    return Core::DateTime::create(full_year, month.value(), day.value(), hour.value(), minute.value(), full_seconds);
}

Optional<Core::DateTime> parse_generalized_time(StringView time)
{
    // YYYYMMDDhh[mm[ss[.fff]]] or YYYYMMDDhh[mm[ss[.fff]]]Z or YYYYMMDDhh[mm[ss[.fff]]](+|-)hhmm
    GenericLexer lexer(time);
    auto year = lexer.consume(4).to_uint();
    auto month = lexer.consume(2).to_uint();
    auto day = lexer.consume(2).to_uint();
    auto hour = lexer.consume(2).to_uint();
    Optional<unsigned> minute, seconds, milliseconds, offset_hours, offset_minutes;
    [[maybe_unused]] bool negative_offset = false;
    if (!lexer.is_eof()) {
        if (lexer.consume_specific('Z'))
            goto done_parsing;

        if (!lexer.next_is(is_any_of("+-"sv))) {
            minute = lexer.consume(2).to_uint();
            if (!minute.has_value()) {
                return {};
            }
            if (lexer.is_eof() || lexer.consume_specific('Z'))
                goto done_parsing;
        }

        if (!lexer.next_is(is_any_of("+-"sv))) {
            seconds = lexer.consume(2).to_uint();
            if (!seconds.has_value()) {
                return {};
            }
            if (lexer.is_eof() || lexer.consume_specific('Z'))
                goto done_parsing;
        }

        if (lexer.consume_specific('.')) {
            milliseconds = lexer.consume(3).to_uint();
            if (!milliseconds.has_value()) {
                return {};
            }
            if (lexer.is_eof() || lexer.consume_specific('Z'))
                goto done_parsing;
        }

        if (lexer.next_is(is_any_of("+-"sv))) {
            negative_offset = lexer.consume() == '-';
            offset_hours = lexer.consume(2).to_uint();
            offset_minutes = lexer.consume(2).to_uint();
            if (!offset_hours.has_value() || !offset_minutes.has_value()) {
                return {};
            }
        }

        // Any character would be garbage.
        if (!lexer.is_eof()) {
            return {};
        }
    }

done_parsing:;

    if (!year.has_value() || !month.has_value() || !day.has_value() || !hour.has_value()) {
        return {};
    }

    // FIXME: Handle offsets!
    if (offset_hours.has_value() || offset_minutes.has_value())
        dbgln("FIXME: Implement GeneralizedTime with offset!");

    // Unceremoniously drop the milliseconds on the floor.
    return Core::DateTime::create(year.value(), month.value(), day.value(), hour.value(), minute.value_or(0), seconds.value_or(0));
}
}
