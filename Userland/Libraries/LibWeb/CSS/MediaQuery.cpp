/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/MediaQuery.h>

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
    }
    builder.append(')');
    return builder.to_string();
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

}
