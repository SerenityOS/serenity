/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/MediaQuery.h>
#include <LibWeb/CSS/Serialize.h>
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

String MediaFeatureValue::to_string() const
{
    return m_value.visit(
        [](String& ident) { return serialize_an_identifier(ident); },
        [](Length& length) { return length.to_string(); },
        [](double number) { return String::number(number); });
}

bool MediaFeatureValue::is_same_type(MediaFeatureValue const& other) const
{
    return m_value.visit(
        [&](String&) { return other.is_ident(); },
        [&](Length&) { return other.is_length(); },
        [&](double) { return other.is_number(); });
}

bool MediaFeatureValue::equals(MediaFeatureValue const& other) const
{
    if (!is_same_type(other))
        return false;

    if (is_ident() && other.is_ident())
        return m_value.get<String>().equals_ignoring_case(other.m_value.get<String>());
    if (is_length() && other.is_length()) {
        // FIXME: Handle relative lengths. https://www.w3.org/TR/mediaqueries-4/#ref-for-relative-length
        auto& my_length = m_value.get<Length>();
        auto& other_length = other.m_value.get<Length>();
        if (!my_length.is_absolute() || !other_length.is_absolute()) {
            dbgln("TODO: Support relative lengths in media queries!");
            return false;
        }
        return my_length.absolute_length_to_px() == other_length.absolute_length_to_px();
    }
    if (is_number() && other.is_number())
        return m_value.get<double>() == other.m_value.get<double>();

    VERIFY_NOT_REACHED();
}

String MediaFeature::to_string() const
{
    switch (m_type) {
    case Type::IsTrue:
        return m_name;
    case Type::ExactValue:
        return String::formatted("{}:{}", m_name, m_value->to_string());
    case Type::MinValue:
        return String::formatted("min-{}:{}", m_name, m_value->to_string());
    case Type::MaxValue:
        return String::formatted("max-{}:{}", m_name, m_value->to_string());
    }

    VERIFY_NOT_REACHED();
}

bool MediaFeature::evaluate(DOM::Window const& window) const
{
    auto maybe_queried_value = window.query_media_feature(m_name);
    if (!maybe_queried_value.has_value())
        return false;
    auto queried_value = maybe_queried_value.release_value();

    switch (m_type) {
    case Type::IsTrue:
        if (queried_value.is_number())
            return queried_value.number() != 0;
        if (queried_value.is_length())
            return queried_value.length().raw_value() != 0;
        if (queried_value.is_ident())
            return queried_value.ident() != "none";
        return false;

    case Type::ExactValue:
        return queried_value.equals(*m_value);

    case Type::MinValue:
        if (!m_value->is_same_type(queried_value))
            return false;

        if (m_value->is_number())
            return queried_value.number() >= m_value->number();

        if (m_value->is_length()) {
            auto& queried_length = queried_value.length();
            auto& value_length = m_value->length();
            // FIXME: Handle relative lengths. https://www.w3.org/TR/mediaqueries-4/#ref-for-relative-length
            if (!value_length.is_absolute()) {
                dbgln("Media feature was given a non-absolute length! {}", value_length.to_string());
                return false;
            }
            return queried_length.absolute_length_to_px() >= value_length.absolute_length_to_px();
        }

        return false;

    case Type::MaxValue:
        if (!m_value->is_same_type(queried_value))
            return false;

        if (m_value->is_number())
            return queried_value.number() <= m_value->number();

        if (m_value->is_length()) {
            auto& queried_length = queried_value.length();
            auto& value_length = m_value->length();
            // FIXME: Handle relative lengths. https://www.w3.org/TR/mediaqueries-4/#ref-for-relative-length
            if (!value_length.is_absolute()) {
                dbgln("Media feature was given a non-absolute length! {}", value_length.to_string());
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
        builder.append(feature->to_string());
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
        return as_match_result(feature->evaluate(window));
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
