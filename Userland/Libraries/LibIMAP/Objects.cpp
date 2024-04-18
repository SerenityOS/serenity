/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibIMAP/Objects.h>

namespace IMAP {

ByteString Sequence::serialize() const
{
    if (start == end) {
        return AK::ByteString::formatted("{}", start);
    } else {
        auto start_char = start != -1 ? ByteString::formatted("{}", start) : "*";
        auto end_char = end != -1 ? ByteString::formatted("{}", end) : "*";
        return ByteString::formatted("{}:{}", start_char, end_char);
    }
}

ByteString FetchCommand::DataItem::Section::serialize() const
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
        return headers_builder.to_byte_string();
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
        return sb.to_byte_string();
    }
    }
    VERIFY_NOT_REACHED();
}
ByteString FetchCommand::DataItem::serialize() const
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

        return sb.to_byte_string();
    }
    case DataItemType::BodyStructure:
        return "BODYSTRUCTURE";
    }
    VERIFY_NOT_REACHED();
}
ByteString FetchCommand::serialize()
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

    return AK::ByteString::formatted("{} ({})", sequence_builder.to_byte_string(), data_items_builder.to_byte_string());
}
ByteString serialize_astring(StringView string)
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
        return ByteString::formatted("\"{}\"", escaped_str);
    }

    // Just send a literal
    return ByteString::formatted("{{{}}}\r\n{}", string.length(), string);
}
ByteString SearchKey::serialize() const
{
    return data.visit(
        [&](All const&) { return ByteString("ALL"); },
        [&](Answered const&) { return ByteString("ANSWERED"); },
        [&](Bcc const& x) { return ByteString::formatted("BCC {}", serialize_astring(x.bcc)); },
        [&](Cc const& x) { return ByteString::formatted("CC {}", serialize_astring(x.cc)); },
        [&](Deleted const&) { return ByteString("DELETED"); },
        [&](Draft const&) { return ByteString("DRAFT"); },
        [&](From const& x) { return ByteString::formatted("FROM {}", serialize_astring(x.from)); },
        [&](Header const& x) { return ByteString::formatted("HEADER {} {}", serialize_astring(x.header), serialize_astring(x.value)); },
        [&](Keyword const& x) { return ByteString::formatted("KEYWORD {}", x.keyword); },
        [&](Larger const& x) { return ByteString::formatted("LARGER {}", x.number); },
        [&](New const&) { return ByteString("NEW"); },
        [&](Not const& x) { return ByteString::formatted("NOT {}", x.operand->serialize()); },
        [&](Old const&) { return ByteString("OLD"); },
        [&](On const& x) { return ByteString::formatted("ON {}", x.date.to_byte_string("%d-%b-%Y"sv)); },
        [&](Or const& x) { return ByteString::formatted("OR {} {}", x.lhs->serialize(), x.rhs->serialize()); },
        [&](Recent const&) { return ByteString("RECENT"); },
        [&](SearchKeys const& x) {
            StringBuilder sb;
            sb.append('(');
            bool first = true;
            for (auto const& item : x.keys) {
                if (!first)
                    sb.append(", "sv);
                sb.append(item->serialize());
                first = false;
            }
            return sb.to_byte_string();
        },
        [&](Seen const&) { return ByteString("SEEN"); },
        [&](SentBefore const& x) { return ByteString::formatted("SENTBEFORE {}", x.date.to_byte_string("%d-%b-%Y"sv)); },
        [&](SentOn const& x) { return ByteString::formatted("SENTON {}", x.date.to_byte_string("%d-%b-%Y"sv)); },
        [&](SentSince const& x) { return ByteString::formatted("SENTSINCE {}", x.date.to_byte_string("%d-%b-%Y"sv)); },
        [&](SequenceSet const& x) { return x.sequence.serialize(); },
        [&](Since const& x) { return ByteString::formatted("SINCE {}", x.date.to_byte_string("%d-%b-%Y"sv)); },
        [&](Smaller const& x) { return ByteString::formatted("SMALLER {}", x.number); },
        [&](Subject const& x) { return ByteString::formatted("SUBJECT {}", serialize_astring(x.subject)); },
        [&](Text const& x) { return ByteString::formatted("TEXT {}", serialize_astring(x.text)); },
        [&](To const& x) { return ByteString::formatted("TO {}", serialize_astring(x.to)); },
        [&](UID const& x) { return ByteString::formatted("UID {}", x.uid); },
        [&](Unanswered const&) { return ByteString("UNANSWERED"); },
        [&](Undeleted const&) { return ByteString("UNDELETED"); },
        [&](Undraft const&) { return ByteString("UNDRAFT"); },
        [&](Unkeyword const& x) { return ByteString::formatted("UNKEYWORD {}", serialize_astring(x.flag_keyword)); },
        [&](Unseen const&) { return ByteString("UNSEEN"); });
}
}
