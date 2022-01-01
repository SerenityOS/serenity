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

    explicit MediaFeatureValue(double number)
        : m_value(number)
    {
    }

    String to_string() const;

    bool is_ident() const { return m_value.has<String>(); }
    bool is_length() const { return m_value.has<Length>(); }
    bool is_number() const { return m_value.has<double>(); }
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

    double number() const
    {
        VERIFY(is_number());
        return m_value.get<double>();
    }

    bool operator==(MediaFeatureValue const& other) const { return equals(other); }
    bool operator!=(MediaFeatureValue const& other) const { return !(*this == other); }
    bool equals(MediaFeatureValue const& other) const;

private:
    // TODO: Support <ratio> once we have that.
    Variant<String, Length, double> m_value;
};

// https://www.w3.org/TR/mediaqueries-4/#mq-features
class MediaFeature {
public:
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

    bool evaluate(DOM::Window const&) const;
    String to_string() const;

private:
    // FIXME: Implement range syntax: https://www.w3.org/TR/mediaqueries-4/#mq-ranges
    enum class Type {
        IsTrue,
        ExactValue,
        MinValue,
        MaxValue,
    };

    MediaFeature(Type type, FlyString name, Optional<MediaFeatureValue> value = {})
        : m_type(type)
        , m_name(move(name))
        , m_value(move(value))
    {
    }

    Type m_type;
    FlyString m_name;
    Optional<MediaFeatureValue> m_value {};
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

    // https://www.w3.org/TR/mediaqueries-4/#media-conditions
    struct MediaCondition {
        enum class Type {
            Single,
            And,
            Or,
            Not,
            GeneralEnclosed,
        };

        Type type;
        Optional<MediaFeature> feature;
        NonnullOwnPtrVector<MediaCondition> conditions;
        Optional<GeneralEnclosed> general_enclosed;

        MatchResult evaluate(DOM::Window const&) const;
        String to_string() const;
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
struct Formatter<Web::CSS::MediaQuery::MediaCondition> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::MediaQuery::MediaCondition const& media_condition)
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
