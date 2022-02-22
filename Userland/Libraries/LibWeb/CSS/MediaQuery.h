/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <LibWeb/CSS/GeneralEnclosed.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

// https://www.w3.org/TR/mediaqueries-4/#typedef-mf-value
class MediaFeatureValue {
public:
    explicit MediaFeatureValue(String ident)
        : m_value(move(ident))
    {
    }

    explicit MediaFeatureValue(Length length)
        : m_value(move(length))
    {
    }

    explicit MediaFeatureValue(Resolution resolution)
        : m_value(move(resolution))
    {
    }

    explicit MediaFeatureValue(double number)
        : m_value(number)
    {
    }

    String to_string() const;

    bool is_ident() const { return m_value.has<String>(); }
    bool is_length() const { return m_value.has<Length>(); }
    bool is_number() const { return m_value.has<double>(); }
    bool is_resolution() const { return m_value.has<Resolution>(); }
    bool is_same_type(MediaFeatureValue const& other) const;

    String const& ident() const
    {
        VERIFY(is_ident());
        return m_value.get<String>();
    }

    Length const& length() const
    {
        VERIFY(is_length());
        return m_value.get<Length>();
    }

    Resolution const& resolution() const
    {
        VERIFY(is_resolution());
        return m_value.get<Resolution>();
    }

    double number() const
    {
        VERIFY(is_number());
        return m_value.get<double>();
    }

private:
    // TODO: Support <ratio> once we have that.
    Variant<String, Length, Resolution, double> m_value;
};

// https://www.w3.org/TR/mediaqueries-4/#mq-features
class MediaFeature {
public:
    enum class Comparison {
        Equal,
        LessThan,
        LessThanOrEqual,
        GreaterThan,
        GreaterThanOrEqual,
    };

    // Corresponds to `<mf-boolean>` grammar
    static MediaFeature boolean(String const& name)
    {
        return MediaFeature(Type::IsTrue, name);
    }

    // Corresponds to `<mf-plain>` grammar
    static MediaFeature plain(String const& name, MediaFeatureValue value)
    {
        if (name.starts_with("min-", CaseSensitivity::CaseInsensitive))
            return MediaFeature(Type::MinValue, name.substring_view(4), move(value));
        if (name.starts_with("max-", CaseSensitivity::CaseInsensitive))
            return MediaFeature(Type::MaxValue, name.substring_view(4), move(value));
        return MediaFeature(Type::ExactValue, move(name), move(value));
    }

    // Corresponds to `<mf-range>` grammar, with a single comparison
    static MediaFeature half_range(MediaFeatureValue value, Comparison comparison, String const& name)
    {
        MediaFeature feature { Type::Range, name };
        feature.m_range = Range {
            .left_value = value,
            .left_comparison = comparison,
        };
        return feature;
    }

    // Corresponds to `<mf-range>` grammar, with two comparisons
    static MediaFeature range(MediaFeatureValue left_value, Comparison left_comparison, String const& name, Comparison right_comparison, MediaFeatureValue right_value)
    {
        MediaFeature feature { Type::Range, name };
        feature.m_range = Range {
            .left_value = left_value,
            .left_comparison = left_comparison,
            .right_comparison = right_comparison,
            .right_value = right_value,
        };
        return feature;
    }

    bool evaluate(DOM::Window const&) const;
    String to_string() const;

private:
    enum class Type {
        IsTrue,
        ExactValue,
        MinValue,
        MaxValue,
        Range,
    };

    MediaFeature(Type type, FlyString name, Optional<MediaFeatureValue> value = {})
        : m_type(type)
        , m_name(move(name))
        , m_value(move(value))
    {
    }

    static bool compare(DOM::Window const& window, MediaFeatureValue left, Comparison comparison, MediaFeatureValue right);

    struct Range {
        MediaFeatureValue left_value;
        Comparison left_comparison;
        Optional<Comparison> right_comparison {};
        Optional<MediaFeatureValue> right_value {};
    };

    Type m_type;
    FlyString m_name;
    Optional<MediaFeatureValue> m_value {};
    Optional<Range> m_range {};
};

// https://www.w3.org/TR/mediaqueries-4/#media-conditions
struct MediaCondition {
    enum class Type {
        Single,
        And,
        Or,
        Not,
        GeneralEnclosed,
    };

    // Only used in parsing
    enum class AllowOr {
        No = 0,
        Yes = 1,
    };

    static NonnullOwnPtr<MediaCondition> from_general_enclosed(GeneralEnclosed&&);
    static NonnullOwnPtr<MediaCondition> from_feature(MediaFeature&&);
    static NonnullOwnPtr<MediaCondition> from_not(NonnullOwnPtr<MediaCondition>&&);
    static NonnullOwnPtr<MediaCondition> from_and_list(NonnullOwnPtrVector<MediaCondition>&&);
    static NonnullOwnPtr<MediaCondition> from_or_list(NonnullOwnPtrVector<MediaCondition>&&);

    MatchResult evaluate(DOM::Window const&) const;
    String to_string() const;

private:
    MediaCondition() { }
    Type type;
    Optional<MediaFeature> feature;
    NonnullOwnPtrVector<MediaCondition> conditions;
    Optional<GeneralEnclosed> general_enclosed;
};

class MediaQuery : public RefCounted<MediaQuery> {
    friend class Parser;

public:
    ~MediaQuery() = default;

    // https://www.w3.org/TR/mediaqueries-4/#media-types
    enum class MediaType {
        All,
        Print,
        Screen,

        // Deprecated, must never match:
        TTY,
        TV,
        Projection,
        Handheld,
        Braille,
        Embossed,
        Aural,
        Speech,
    };

    static NonnullRefPtr<MediaQuery> create_not_all();
    static NonnullRefPtr<MediaQuery> create() { return adopt_ref(*new MediaQuery); }

    bool matches() const { return m_matches; }
    bool evaluate(DOM::Window const&);
    String to_string() const;

private:
    MediaQuery() = default;

    // https://www.w3.org/TR/mediaqueries-4/#mq-not
    bool m_negated { false };
    MediaType m_media_type { MediaType::All };
    OwnPtr<MediaCondition> m_media_condition { nullptr };

    // Cached value, updated by evaluate()
    bool m_matches { false };
};

String serialize_a_media_query_list(NonnullRefPtrVector<MediaQuery> const&);

bool is_media_feature_name(StringView name);

}

namespace AK {

template<>
struct Formatter<Web::CSS::MediaFeature> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::MediaFeature const& media_feature)
    {
        return Formatter<StringView>::format(builder, media_feature.to_string());
    }
};

template<>
struct Formatter<Web::CSS::MediaCondition> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::MediaCondition const& media_condition)
    {
        return Formatter<StringView>::format(builder, media_condition.to_string());
    }
};

template<>
struct Formatter<Web::CSS::MediaQuery> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::MediaQuery const& media_query)
    {
        return Formatter<StringView>::format(builder, media_query.to_string());
    }
};

}
