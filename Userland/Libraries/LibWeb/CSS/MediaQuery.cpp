/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/MediaQuery.h>
#include <LibWeb/CSS/Serialize.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Window.h>

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
        [](ValueID const& ident) { return String { string_from_value_id(ident) }; },
        [](Length const& length) { return length.to_string(); },
        [](Ratio const& ratio) { return ratio.to_string(); },
        [](Resolution const& resolution) { return resolution.to_string(); },
        [](float number) { return String::number(number); });
}

bool MediaFeatureValue::is_same_type(MediaFeatureValue const& other) const
{
    return m_value.visit(
        [&](ValueID const&) { return other.is_ident(); },
        [&](Length const&) { return other.is_length(); },
        [&](Ratio const&) { return other.is_ratio(); },
        [&](Resolution const&) { return other.is_resolution(); },
        [&](float) { return other.is_number(); });
}

String MediaFeature::to_string() const
{
    auto comparison_string = [](Comparison comparison) -> StringView {
        switch (comparison) {
        case Comparison::Equal:
            return "="sv;
        case Comparison::LessThan:
            return "<"sv;
        case Comparison::LessThanOrEqual:
            return "<="sv;
        case Comparison::GreaterThan:
            return ">"sv;
        case Comparison::GreaterThanOrEqual:
            return ">="sv;
        }
        VERIFY_NOT_REACHED();
    };

    switch (m_type) {
    case Type::IsTrue:
        return string_from_media_feature_id(m_id);
    case Type::ExactValue:
        return String::formatted("{}:{}", string_from_media_feature_id(m_id), m_value->to_string());
    case Type::MinValue:
        return String::formatted("min-{}:{}", string_from_media_feature_id(m_id), m_value->to_string());
    case Type::MaxValue:
        return String::formatted("max-{}:{}", string_from_media_feature_id(m_id), m_value->to_string());
    case Type::Range:
        if (!m_range->right_comparison.has_value())
            return String::formatted("{} {} {}", m_range->left_value.to_string(), comparison_string(m_range->left_comparison), string_from_media_feature_id(m_id));

        return String::formatted("{} {} {} {} {}", m_range->left_value.to_string(), comparison_string(m_range->left_comparison), string_from_media_feature_id(m_id), comparison_string(*m_range->right_comparison), m_range->right_value->to_string());
    }

    VERIFY_NOT_REACHED();
}

bool MediaFeature::evaluate(HTML::Window const& window) const
{
    auto maybe_queried_value = window.query_media_feature(m_id);
    if (!maybe_queried_value.has_value())
        return false;
    auto queried_value = maybe_queried_value.release_value();

    switch (m_type) {
    case Type::IsTrue:
        if (queried_value.is_number())
            return queried_value.number() != 0;
        if (queried_value.is_length())
            return queried_value.length().raw_value() != 0;
        // FIXME: I couldn't figure out from the spec how ratios should be evaluated in a boolean context.
        if (queried_value.is_ratio())
            return !queried_value.ratio().is_degenerate();
        if (queried_value.is_resolution())
            return queried_value.resolution().to_dots_per_pixel() != 0;
        if (queried_value.is_ident()) {
            // NOTE: It is not technically correct to always treat `no-preference` as false, but every
            //       media-feature that accepts it as a value treats it as false, so good enough. :^)
            //       If other features gain this property for other identifiers in the future, we can
            //       add more robust handling for them then.
            return queried_value.ident() != ValueID::None
                && queried_value.ident() != ValueID::NoPreference;
        }
        return false;

    case Type::ExactValue:
        return compare(window, *m_value, Comparison::Equal, queried_value);

    case Type::MinValue:
        return compare(window, queried_value, Comparison::GreaterThanOrEqual, *m_value);

    case Type::MaxValue:
        return compare(window, queried_value, Comparison::LessThanOrEqual, *m_value);

    case Type::Range:
        if (!compare(window, m_range->left_value, m_range->left_comparison, queried_value))
            return false;

        if (m_range->right_comparison.has_value())
            if (!compare(window, queried_value, *m_range->right_comparison, *m_range->right_value))
                return false;

        return true;
    }

    VERIFY_NOT_REACHED();
}

bool MediaFeature::compare(HTML::Window const& window, MediaFeatureValue left, Comparison comparison, MediaFeatureValue right)
{
    if (!left.is_same_type(right))
        return false;

    if (left.is_ident()) {
        if (comparison == Comparison::Equal)
            return left.ident() == right.ident();
        return false;
    }

    if (left.is_number()) {
        switch (comparison) {
        case Comparison::Equal:
            return left.number() == right.number();
        case Comparison::LessThan:
            return left.number() < right.number();
        case Comparison::LessThanOrEqual:
            return left.number() <= right.number();
        case Comparison::GreaterThan:
            return left.number() > right.number();
        case Comparison::GreaterThanOrEqual:
            return left.number() >= right.number();
        }
        VERIFY_NOT_REACHED();
    }

    if (left.is_length()) {
        float left_px;
        float right_px;
        // Save ourselves some work if neither side is a relative length.
        if (left.length().is_absolute() && right.length().is_absolute()) {
            left_px = left.length().absolute_length_to_px();
            right_px = right.length().absolute_length_to_px();
        } else {
            Gfx::IntRect viewport_rect { 0, 0, window.inner_width(), window.inner_height() };

            auto const& initial_font = window.associated_document().style_computer().initial_font();
            Gfx::FontPixelMetrics const& initial_font_metrics = initial_font.pixel_metrics();
            float initial_font_size = initial_font.presentation_size();

            left_px = left.length().to_px(viewport_rect, initial_font_metrics, initial_font_size, initial_font_size);
            right_px = right.length().to_px(viewport_rect, initial_font_metrics, initial_font_size, initial_font_size);
        }

        switch (comparison) {
        case Comparison::Equal:
            return left_px == right_px;
        case Comparison::LessThan:
            return left_px < right_px;
        case Comparison::LessThanOrEqual:
            return left_px <= right_px;
        case Comparison::GreaterThan:
            return left_px > right_px;
        case Comparison::GreaterThanOrEqual:
            return left_px >= right_px;
        }

        VERIFY_NOT_REACHED();
    }

    if (left.is_ratio()) {
        auto left_decimal = left.ratio().value();
        auto right_decimal = right.ratio().value();

        switch (comparison) {
        case Comparison::Equal:
            return left_decimal == right_decimal;
        case Comparison::LessThan:
            return left_decimal < right_decimal;
        case Comparison::LessThanOrEqual:
            return left_decimal <= right_decimal;
        case Comparison::GreaterThan:
            return left_decimal > right_decimal;
        case Comparison::GreaterThanOrEqual:
            return left_decimal >= right_decimal;
        }
        VERIFY_NOT_REACHED();
    }

    if (left.is_resolution()) {
        auto left_dppx = left.resolution().to_dots_per_pixel();
        auto right_dppx = right.resolution().to_dots_per_pixel();

        switch (comparison) {
        case Comparison::Equal:
            return left_dppx == right_dppx;
        case Comparison::LessThan:
            return left_dppx < right_dppx;
        case Comparison::LessThanOrEqual:
            return left_dppx <= right_dppx;
        case Comparison::GreaterThan:
            return left_dppx > right_dppx;
        case Comparison::GreaterThanOrEqual:
            return left_dppx >= right_dppx;
        }
        VERIFY_NOT_REACHED();
    }

    VERIFY_NOT_REACHED();
}

NonnullOwnPtr<MediaCondition> MediaCondition::from_general_enclosed(GeneralEnclosed&& general_enclosed)
{
    auto result = new MediaCondition;
    result->type = Type::GeneralEnclosed;
    result->general_enclosed = move(general_enclosed);

    return adopt_own(*result);
}

NonnullOwnPtr<MediaCondition> MediaCondition::from_feature(MediaFeature&& feature)
{
    auto result = new MediaCondition;
    result->type = Type::Single;
    result->feature = move(feature);

    return adopt_own(*result);
}

NonnullOwnPtr<MediaCondition> MediaCondition::from_not(NonnullOwnPtr<MediaCondition>&& condition)
{
    auto result = new MediaCondition;
    result->type = Type::Not;
    result->conditions.append(move(condition));

    return adopt_own(*result);
}

NonnullOwnPtr<MediaCondition> MediaCondition::from_and_list(NonnullOwnPtrVector<MediaCondition>&& conditions)
{
    auto result = new MediaCondition;
    result->type = Type::And;
    result->conditions = move(conditions);

    return adopt_own(*result);
}

NonnullOwnPtr<MediaCondition> MediaCondition::from_or_list(NonnullOwnPtrVector<MediaCondition>&& conditions)
{
    auto result = new MediaCondition;
    result->type = Type::Or;
    result->conditions = move(conditions);

    return adopt_own(*result);
}

String MediaCondition::to_string() const
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

MatchResult MediaCondition::evaluate(HTML::Window const& window) const
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
        builder.append(CSS::to_string(m_media_type));
        if (m_media_condition)
            builder.append(" and ");
    }

    if (m_media_condition) {
        builder.append(m_media_condition->to_string());
    }

    return builder.to_string();
}

bool MediaQuery::evaluate(HTML::Window const& window)
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

bool is_media_feature_name(StringView name)
{
    // MEDIAQUERIES-4 - https://www.w3.org/TR/mediaqueries-4/#media-descriptor-table
    if (name.equals_ignoring_case("any-hover"sv))
        return true;
    if (name.equals_ignoring_case("any-pointer"sv))
        return true;
    if (name.equals_ignoring_case("aspect-ratio"sv))
        return true;
    if (name.equals_ignoring_case("color"sv))
        return true;
    if (name.equals_ignoring_case("color-gamut"sv))
        return true;
    if (name.equals_ignoring_case("color-index"sv))
        return true;
    if (name.equals_ignoring_case("device-aspect-ratio"sv))
        return true;
    if (name.equals_ignoring_case("device-height"sv))
        return true;
    if (name.equals_ignoring_case("device-width"sv))
        return true;
    if (name.equals_ignoring_case("grid"sv))
        return true;
    if (name.equals_ignoring_case("height"sv))
        return true;
    if (name.equals_ignoring_case("hover"sv))
        return true;
    if (name.equals_ignoring_case("monochrome"sv))
        return true;
    if (name.equals_ignoring_case("orientation"sv))
        return true;
    if (name.equals_ignoring_case("overflow-block"sv))
        return true;
    if (name.equals_ignoring_case("overflow-inline"sv))
        return true;
    if (name.equals_ignoring_case("pointer"sv))
        return true;
    if (name.equals_ignoring_case("resolution"sv))
        return true;
    if (name.equals_ignoring_case("scan"sv))
        return true;
    if (name.equals_ignoring_case("update"sv))
        return true;
    if (name.equals_ignoring_case("width"sv))
        return true;

    // MEDIAQUERIES-5 - https://www.w3.org/TR/mediaqueries-5/#media-descriptor-table
    if (name.equals_ignoring_case("prefers-color-scheme"sv))
        return true;
    // FIXME: Add other level 5 feature names

    return false;
}

Optional<MediaQuery::MediaType> media_type_from_string(StringView name)
{
    if (name.equals_ignoring_case("all"sv))
        return MediaQuery::MediaType::All;
    if (name.equals_ignoring_case("aural"sv))
        return MediaQuery::MediaType::Aural;
    if (name.equals_ignoring_case("braille"sv))
        return MediaQuery::MediaType::Braille;
    if (name.equals_ignoring_case("embossed"sv))
        return MediaQuery::MediaType::Embossed;
    if (name.equals_ignoring_case("handheld"sv))
        return MediaQuery::MediaType::Handheld;
    if (name.equals_ignoring_case("print"sv))
        return MediaQuery::MediaType::Print;
    if (name.equals_ignoring_case("projection"sv))
        return MediaQuery::MediaType::Projection;
    if (name.equals_ignoring_case("screen"sv))
        return MediaQuery::MediaType::Screen;
    if (name.equals_ignoring_case("speech"sv))
        return MediaQuery::MediaType::Speech;
    if (name.equals_ignoring_case("tty"sv))
        return MediaQuery::MediaType::TTY;
    if (name.equals_ignoring_case("tv"sv))
        return MediaQuery::MediaType::TV;
    return {};
}

StringView to_string(MediaQuery::MediaType media_type)
{
    switch (media_type) {
    case MediaQuery::MediaType::All:
        return "all"sv;
    case MediaQuery::MediaType::Aural:
        return "aural"sv;
    case MediaQuery::MediaType::Braille:
        return "braille"sv;
    case MediaQuery::MediaType::Embossed:
        return "embossed"sv;
    case MediaQuery::MediaType::Handheld:
        return "handheld"sv;
    case MediaQuery::MediaType::Print:
        return "print"sv;
    case MediaQuery::MediaType::Projection:
        return "projection"sv;
    case MediaQuery::MediaType::Screen:
        return "screen"sv;
    case MediaQuery::MediaType::Speech:
        return "speech"sv;
    case MediaQuery::MediaType::TTY:
        return "tty"sv;
    case MediaQuery::MediaType::TV:
        return "tv"sv;
    }
    VERIFY_NOT_REACHED();
}

}
