/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibIMAP/Objects.h>

namespace IMAP {

DeprecatedString Sequence::serialize() const
{
    if (start == end) {
        return AK::DeprecatedString::formatted("{}", start);
    } else {
        auto start_char = start != -1 ? DeprecatedString::formatted("{}", start) : "*";
        auto end_char = end != -1 ? DeprecatedString::formatted("{}", end) : "*";
        return DeprecatedString::formatted("{}:{}", start_char, end_char);
    }
}

DeprecatedString FetchCommand::DataItem::Section::serialize() const
{
    StringBuilder headers_builder;
    switch (type) {
    case SectionType::Header:
        return "HEADER";
    case SectionType::HeaderFields:
    case SectionType::HeaderFieldsNot: {
        if (type == SectionType::HeaderFields)
            headers_builder.append("HEADER.FIELDS ("sv);
        else
            headers_builder.append("HEADERS.FIELDS.NOT ("sv);

        bool first = true;
        for (auto& field : headers.value()) {
            if (!first)
                headers_builder.append(' ');
            headers_builder.append(field);
            first = false;
        }
        headers_builder.append(')');
        return headers_builder.to_deprecated_string();
    }
    case SectionType::Text:
        return "TEXT";
    case SectionType::Parts: {
        StringBuilder sb;
        bool first = true;
        for (int part : parts.value()) {
            if (!first)
                sb.append('.');
            sb.appendff("{}", part);
            first = false;
        }
        if (ends_with_mime) {
            sb.append(".MIME"sv);
        }
        return sb.to_deprecated_string();
    }
    }
    VERIFY_NOT_REACHED();
}
DeprecatedString FetchCommand::DataItem::serialize() const
{
    switch (type) {
    case DataItemType::Envelope:
        return "ENVELOPE";
    case DataItemType::Flags:
        return "FLAGS";
    case DataItemType::InternalDate:
        return "INTERNALDATE";
    case DataItemType::UID:
        return "UID";
    case DataItemType::PeekBody:
    case DataItemType::BodySection: {
        StringBuilder sb;
        if (type == DataItemType::BodySection)
            sb.appendff("BODY[{}]", section.value().serialize());
        else
            sb.appendff("BODY.PEEK[{}]", section.value().serialize());
        if (partial_fetch) {
            sb.appendff("<{}.{}>", start, octets);
        }

        return sb.to_deprecated_string();
    }
    case DataItemType::BodyStructure:
        return "BODYSTRUCTURE";
    }
    VERIFY_NOT_REACHED();
}
DeprecatedString FetchCommand::serialize()
{
    StringBuilder sequence_builder;
    bool first = true;
    for (auto& sequence : sequence_set) {
        if (!first) {
            sequence_builder.append(',');
        }
        sequence_builder.append(sequence.serialize());
        first = false;
    }

    StringBuilder data_items_builder;
    first = true;
    for (auto& data_item : data_items) {
        if (!first) {
            data_items_builder.append(' ');
        }
        data_items_builder.append(data_item.serialize());
        first = false;
    }

    return AK::DeprecatedString::formatted("{} ({})", sequence_builder.to_deprecated_string(), data_items_builder.to_deprecated_string());
}
DeprecatedString serialize_astring(StringView string)
{
    // Try to send an atom
    auto is_non_atom_char = [](char x) {
        auto non_atom_chars = { '(', ')', '{', ' ', '%', '*', '"', '\\', ']' };
        return AK::find(non_atom_chars.begin(), non_atom_chars.end(), x) != non_atom_chars.end();
    };
    auto is_atom = all_of(string, [&](auto ch) { return is_ascii_control(ch) && !is_non_atom_char(ch); });
    if (is_atom) {
        return string;
    }

    // Try to quote
    auto can_be_quoted = !(string.contains('\n') || string.contains('\r'));
    if (can_be_quoted) {
        auto escaped_str = string.replace("\\"sv, "\\\\"sv, ReplaceMode::All).replace("\""sv, "\\\""sv, ReplaceMode::All);
        return DeprecatedString::formatted("\"{}\"", escaped_str);
    }

    // Just send a literal
    return DeprecatedString::formatted("{{{}}}\r\n{}", string.length(), string);
}
DeprecatedString SearchKey::serialize() const
{
    return data.visit(
        [&](All const&) { return DeprecatedString("ALL"); },
        [&](Answered const&) { return DeprecatedString("ANSWERED"); },
        [&](Bcc const& x) { return DeprecatedString::formatted("BCC {}", serialize_astring(x.bcc)); },
        [&](Cc const& x) { return DeprecatedString::formatted("CC {}", serialize_astring(x.cc)); },
        [&](Deleted const&) { return DeprecatedString("DELETED"); },
        [&](Draft const&) { return DeprecatedString("DRAFT"); },
        [&](From const& x) { return DeprecatedString::formatted("FROM {}", serialize_astring(x.from)); },
        [&](Header const& x) { return DeprecatedString::formatted("HEADER {} {}", serialize_astring(x.header), serialize_astring(x.value)); },
        [&](Keyword const& x) { return DeprecatedString::formatted("KEYWORD {}", x.keyword); },
        [&](Larger const& x) { return DeprecatedString::formatted("LARGER {}", x.number); },
        [&](New const&) { return DeprecatedString("NEW"); },
        [&](Not const& x) { return DeprecatedString::formatted("NOT {}", x.operand->serialize()); },
        [&](Old const&) { return DeprecatedString("OLD"); },
        [&](On const& x) { return DeprecatedString::formatted("ON {}", x.date.to_deprecated_string("%d-%b-%Y"sv)); },
        [&](Or const& x) { return DeprecatedString::formatted("OR {} {}", x.lhs->serialize(), x.rhs->serialize()); },
        [&](Recent const&) { return DeprecatedString("RECENT"); },
        [&](SearchKeys const& x) {
            StringBuilder sb;
            sb.append('(');
            bool first = true;
            for (const auto& item : x.keys) {
                if (!first)
                    sb.append(", "sv);
                sb.append(item->serialize());
                first = false;
            }
            return sb.to_deprecated_string();
        },
        [&](Seen const&) { return DeprecatedString("SEEN"); },
        [&](SentBefore const& x) { return DeprecatedString::formatted("SENTBEFORE {}", x.date.to_deprecated_string("%d-%b-%Y"sv)); },
        [&](SentOn const& x) { return DeprecatedString::formatted("SENTON {}", x.date.to_deprecated_string("%d-%b-%Y"sv)); },
        [&](SentSince const& x) { return DeprecatedString::formatted("SENTSINCE {}", x.date.to_deprecated_string("%d-%b-%Y"sv)); },
        [&](SequenceSet const& x) { return x.sequence.serialize(); },
        [&](Since const& x) { return DeprecatedString::formatted("SINCE {}", x.date.to_deprecated_string("%d-%b-%Y"sv)); },
        [&](Smaller const& x) { return DeprecatedString::formatted("SMALLER {}", x.number); },
        [&](Subject const& x) { return DeprecatedString::formatted("SUBJECT {}", serialize_astring(x.subject)); },
        [&](Text const& x) { return DeprecatedString::formatted("TEXT {}", serialize_astring(x.text)); },
        [&](To const& x) { return DeprecatedString::formatted("TO {}", serialize_astring(x.to)); },
        [&](UID const& x) { return DeprecatedString::formatted("UID {}", x.uid); },
        [&](Unanswered const&) { return DeprecatedString("UNANSWERED"); },
        [&](Undeleted const&) { return DeprecatedString("UNDELETED"); },
        [&](Undraft const&) { return DeprecatedString("UNDRAFT"); },
        [&](Unkeyword const& x) { return DeprecatedString::formatted("UNKEYWORD {}", serialize_astring(x.flag_keyword)); },
        [&](Unseen const&) { return DeprecatedString("UNSEEN"); });
}
}
