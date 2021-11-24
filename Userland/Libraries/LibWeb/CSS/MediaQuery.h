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

    // https://www.w3.org/TR/mediaqueries-4/#mq-features
    struct MediaFeature {
        // FIXME: Implement range syntax: https://www.w3.org/TR/mediaqueries-4/#mq-ranges
        enum class Type {
            IsTrue,
            ExactValue,
            MinValue,
            MaxValue,
        };

        Type type;
        FlyString name;
        RefPtr<StyleValue> value { nullptr };

        bool evaluate(DOM::Window const&) const;
        String to_string() const;
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
        MediaFeature feature;
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
struct Formatter<Web::CSS::MediaQuery::MediaFeature> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::MediaQuery::MediaFeature const& media_feature)
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
