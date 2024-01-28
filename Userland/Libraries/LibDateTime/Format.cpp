/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Format.h"
#include "ISOCalendar.h"
#include "ZonedDateTime.h"
#include <AK/FormatParser.h>

namespace DateTime {

template<typename DateTimeType>
static ErrorOr<void> perform_format(AK::FormatBuilder& builder, FormatField field_to_format, AK::StandardFormatter field_format, DateTimeType const& date_time)
{
    Optional<ISOCalendar::OutputParts> maybe_iso_calendar_parts;
    auto iso_calendar_parts = [&]() -> ISOCalendar::OutputParts const& {
        maybe_iso_calendar_parts = maybe_iso_calendar_parts.value_or_lazy_evaluated_optional([&] { return date_time.template to_parts<ISOCalendar>(); });
        return maybe_iso_calendar_parts.value();
    };

    switch (field_to_format) {
    case Year:
        return AK::Formatter<i32> { field_format }.format(builder, iso_calendar_parts().year);
    case Month:
        return AK::Formatter<u8> { field_format }.format(builder, iso_calendar_parts().month);
    case Day:
        return AK::Formatter<u8> { field_format }.format(builder, iso_calendar_parts().day_of_month);
    case Hour24:
        return AK::Formatter<u8> { field_format }.format(builder, iso_calendar_parts().hour);
    case Hour12: {
        auto hour = iso_calendar_parts().hour % 12;
        // Hour 12 and 24 are not "0 pm/am", but "12 am/pm".
        if (hour == 0)
            hour = 12;
        return AK::Formatter<u8> { field_format }.format(builder, static_cast<u8>(hour));
    }
    case Minute:
        return AK::Formatter<u8> { field_format }.format(builder, iso_calendar_parts().minute);
    case Second:
        return AK::Formatter<u8> { field_format }.format(builder, iso_calendar_parts().second);
    case SecondFraction:
        return AK::Formatter<u32> { field_format }.format(builder, iso_calendar_parts().nanosecond);
    case TimezoneName: {
        if constexpr (!DateTimeType::has_timezone) {
            return {};
        } else {
            auto timezone_name = TimeZone::time_zone_to_string(date_time.time_zone());
            return AK::Formatter<StringView> { field_format }.format(builder, timezone_name);
        }
    }
    case TimezoneOffset: {
        if constexpr (!DateTimeType::has_timezone) {
            return {};
        } else {
            auto offset_minutes = iso_calendar_parts().time_zone_offset_seconds / 60;
            return AK::Formatter<FormatString> { AK::Formatter<StringView> { field_format } }.format(builder, "{:+02}{:02}"sv, offset_minutes / 60, offset_minutes % 60);
        }
    }
    case TimezoneOffsetWithColon: {
        if constexpr (!DateTimeType::has_timezone) {
            return {};
        } else {
            auto offset_minutes = iso_calendar_parts().time_zone_offset_seconds / 60;
            return AK::Formatter<FormatString> { AK::Formatter<StringView> { field_format } }.format(builder, "{:+02}:{:02}"sv, offset_minutes / 60, offset_minutes % 60);
        }
    }
    }

    VERIFY_NOT_REACHED();
}

template<typename DateTimeType>
static ErrorOr<String> format_impl(DateTimeType const& self, StringView format)
{
    AK::FormatParser parser { format };
    StringBuilder string_builder;
    AK::FormatBuilder builder { string_builder };

    AK::TypeErasedFormatParams dummy_params { 0 };
    while (!parser.is_eof()) {
        TRY(builder.put_literal(parser.consume_literal()));
        if (parser.is_eof())
            break;

        if (!parser.consume_specific('{'))
            return Error::from_string_view("Broken format string"sv);
        // FIXME: We can't currently share this implementation with consume_specifier() since our field specifier is not a number.
        auto field_specifier = parser.consume_until([](auto next) { return ":}"sv.contains(next); });

        StringView flags {};
        if (parser.consume_specific(':')) {
            auto const begin = parser.tell();

            size_t level = 1;
            while (level > 0) {
                if (parser.is_eof())
                    return Error::from_string_view("Unexpected EOF in format string"sv);

                if (parser.consume_specific('{')) {
                    ++level;
                    continue;
                }

                if (parser.consume_specific('}')) {
                    --level;
                    continue;
                }

                parser.consume();
            }

            flags = parser.input().substring_view(begin, parser.tell() - begin - 1);
        } else {
            if (!parser.consume_specific('}'))
                return Error::from_string_view("Broken format string"sv);
        }

        auto maybe_field = parse_field_name(field_specifier);
        if (!maybe_field.has_value())
            return Error::from_string_view("Invalid field specifier"sv);
        auto field = maybe_field.release_value();
        auto field_format = flags.is_empty() ? default_format_for_field(field) : AK::StandardFormatter {};
        AK::FormatParser field_parser { flags };
        field_format.parse(dummy_params, field_parser);
        TRY(perform_format(builder, field, field_format, self));
    }

    TRY(builder.put_literal(parser.consume_literal()));

    return builder.builder().to_string();
}

ErrorOr<String> ZonedDateTime::format(StringView format) const
{
    return format_impl(*this, format);
}

ErrorOr<String> LocalDateTime::format(StringView format) const
{
    return format_impl(*this, format);
}

AK::StandardFormatter default_format_for_field(FormatField field)
{
    using enum AK::StandardFormatter::Mode;
    switch (field) {
    case Year:
        return { .m_mode = Decimal, .m_zero_pad = true, .m_width = 4 };
    case Month:
    case Day:
    case Hour24:
    case Hour12:
    case Minute:
    case Second:
        return { .m_mode = Decimal, .m_zero_pad = true, .m_width = 2 };
    case SecondFraction:
        return { .m_mode = Decimal, .m_zero_pad = true, .m_width = 9 };
    case TimezoneName:
    case TimezoneOffset:
    case TimezoneOffsetWithColon:
        return { .m_mode = String };
    }
    VERIFY_NOT_REACHED();
}

Optional<FormatField> parse_field_name(StringView name)
{
    if (name == "Y"sv)
        return FormatField::Year;
    if (name == "m"sv)
        return FormatField::Month;
    if (name == "d"sv)
        return FormatField::Day;
    if (name == "H"sv)
        return FormatField::Hour24;
    if (name == "I"sv)
        return FormatField::Hour12;
    if (name == "M"sv)
        return FormatField::Minute;
    if (name == "S"sv)
        return FormatField::Second;
    if (name == "f"sv)
        return FormatField::SecondFraction;
    if (name == "Z"sv)
        return FormatField::TimezoneName;
    if (name == "z"sv)
        return FormatField::TimezoneOffset;
    if (name == "0z"sv)
        return FormatField::TimezoneOffsetWithColon;
    return {};
}

}

void AK::Formatter<DateTime::ZonedDateTime>::parse(AK::TypeErasedFormatParams&, AK::FormatParser& parser)
{
    format_string = parser.input();
}

void AK::Formatter<DateTime::LocalDateTime>::parse(AK::TypeErasedFormatParams&, AK::FormatParser& parser)
{
    format_string = parser.input();
}
