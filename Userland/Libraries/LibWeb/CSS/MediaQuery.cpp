/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/MediaQuery.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Window.h>

namespace Web::CSS {

NonnullRefPtr<MediaQuery> MediaQuery::create_not_all()
{
    auto media_query = new MediaQuery;
    media_query->m_negated = true;
    media_query->m_media_type = MediaType::All;

    return adopt_ref(*media_query);
}

String MediaQuery::MediaFeature::to_string() const
{
    switch (type) {
    case Type::IsTrue:
        return name;
    case Type::ExactValue:
        return String::formatted("{}:{}", name, value->to_string());
    case Type::MinValue:
        return String::formatted("min-{}:{}", name, value->to_string());
    case Type::MaxValue:
        return String::formatted("max-{}:{}", name, value->to_string());
    }

    VERIFY_NOT_REACHED();
}

bool MediaQuery::MediaFeature::evaluate(DOM::Window const& window) const
{
    auto queried_value = window.query_media_feature(name);
    if (!queried_value)
        return false;

    switch (type) {
    case Type::IsTrue:
        if (queried_value->has_number())
            return queried_value->to_number() != 0;
        if (queried_value->has_length())
            return queried_value->to_length().raw_value() != 0;
        if (queried_value->has_identifier())
            return queried_value->to_identifier() != ValueID::None;
        return false;

    case Type::ExactValue:
        return queried_value->equals(*value);

    case Type::MinValue:
        if (queried_value->has_number() && value->has_number())
            return queried_value->to_number() >= value->to_number();
        if (queried_value->has_length() && value->has_length()) {
            auto queried_length = queried_value->to_length();
            auto value_length = value->to_length();
            // FIXME: We should be checking that lengths are valid during parsing
            if (!value_length.is_absolute()) {
                dbgln("Media feature was given a non-absolute length, which is invalid! {}", value_length.to_string());
                return false;
            }
            return queried_length.absolute_length_to_px() >= value_length.absolute_length_to_px();
        }
        return false;

    case Type::MaxValue:
        if (queried_value->has_number() && value->has_number())
            return queried_value->to_number() <= value->to_number();
        if (queried_value->has_length() && value->has_length()) {
            auto queried_length = queried_value->to_length();
            auto value_length = value->to_length();
            // FIXME: We should be checking that lengths are valid during parsing
            if (!value_length.is_absolute()) {
                dbgln("Media feature was given a non-absolute length, which is invalid! {}", value_length.to_string());
                return false;
            }
            return queried_length.absolute_length_to_px() <= value_length.absolute_length_to_px();
        }
        return false;
    }

    VERIFY_NOT_REACHED();
}

String MediaQuery::MediaCondition::to_string() const
{
    StringBuilder builder;
    builder.append('(');
    switch (type) {
    case Type::Single:
        builder.append(feature.to_string());
        break;
    case Type::Not:
        builder.append("not ");
        builder.append(conditions.first().to_string());
        break;
    case Type::And:
        builder.join(" and ", conditions);
        break;
    case Type::Or:
        builder.join(" or ", conditions);
        break;
    case Type::GeneralEnclosed:
        builder.append(general_enclosed->to_string());
        break;
    }
    builder.append(')');
    return builder.to_string();
}

MatchResult MediaQuery::MediaCondition::evaluate(DOM::Window const& window) const
{
    switch (type) {
    case Type::Single:
        return as_match_result(feature.evaluate(window));
    case Type::Not:
        return negate(conditions.first().evaluate(window));
    case Type::And:
        return evaluate_and(conditions, [&](auto& child) { return child.evaluate(window); });
    case Type::Or:
        return evaluate_or(conditions, [&](auto& child) { return child.evaluate(window); });
    case Type::GeneralEnclosed:
        return general_enclosed->evaluate();
    }
    VERIFY_NOT_REACHED();
}

String MediaQuery::to_string() const
{
    StringBuilder builder;

    if (m_negated)
        builder.append("not ");

    if (m_negated || m_media_type != MediaType::All || !m_media_condition) {
        switch (m_media_type) {
        case MediaType::All:
            builder.append("all");
            break;
        case MediaType::Aural:
            builder.append("aural");
            break;
        case MediaType::Braille:
            builder.append("braille");
            break;
        case MediaType::Embossed:
            builder.append("embossed");
            break;
        case MediaType::Handheld:
            builder.append("handheld");
            break;
        case MediaType::Print:
            builder.append("print");
            break;
        case MediaType::Projection:
            builder.append("projection");
            break;
        case MediaType::Screen:
            builder.append("screen");
            break;
        case MediaType::Speech:
            builder.append("speech");
            break;
        case MediaType::TTY:
            builder.append("tty");
            break;
        case MediaType::TV:
            builder.append("tv");
            break;
        }
        if (m_media_condition)
            builder.append(" and ");
    }

    if (m_media_condition) {
        builder.append(m_media_condition->to_string());
    }

    return builder.to_string();
}

bool MediaQuery::evaluate(DOM::Window const& window)
{
    auto matches_media = [](MediaType media) -> MatchResult {
        switch (media) {
        case MediaType::All:
            return MatchResult::True;
        case MediaType::Print:
            // FIXME: Enable for printing, when we have printing!
            return MatchResult::False;
        case MediaType::Screen:
            // FIXME: Disable for printing, when we have printing!
            return MatchResult::True;
        // Deprecated, must never match:
        case MediaType::TTY:
        case MediaType::TV:
        case MediaType::Projection:
        case MediaType::Handheld:
        case MediaType::Braille:
        case MediaType::Embossed:
        case MediaType::Aural:
        case MediaType::Speech:
            return MatchResult::False;
        }
        VERIFY_NOT_REACHED();
    };

    MatchResult result = matches_media(m_media_type);

    if ((result == MatchResult::True) && m_media_condition)
        result = m_media_condition->evaluate(window);

    if (m_negated)
        result = negate(result);

    m_matches = result == MatchResult::True;
    return m_matches;
}

// https://www.w3.org/TR/cssom-1/#serialize-a-media-query-list
String serialize_a_media_query_list(NonnullRefPtrVector<MediaQuery> const& media_queries)
{
    // 1. If the media query list is empty, then return the empty string.
    if (media_queries.is_empty())
        return "";

    // 2. Serialize each media query in the list of media queries, in the same order as they
    // appear in the media query list, and then serialize the list.
    StringBuilder builder;
    builder.join(", ", media_queries);
    return builder.to_string();
}

}
