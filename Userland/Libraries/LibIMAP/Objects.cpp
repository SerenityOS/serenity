/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibIMAP/Objects.h>

namespace IMAP {

String Sequence::serialize() const
{
    if (start == end) {
        return AK::String::formatted("{}", start);
    } else {
        auto start_char = start != -1 ? String::formatted("{}", start) : "*";
        auto end_char = end != -1 ? String::formatted("{}", end) : "*";
        return String::formatted("{}:{}", start_char, end_char);
    }
}

String FetchCommand::DataItem::Section::serialize() const
{
    StringBuilder headers_builder;
    switch (type) {
    case SectionType::Header:
        return "HEADER";
    case SectionType::HeaderFields:
    case SectionType::HeaderFieldsNot: {
        if (type == SectionType::HeaderFields)
            headers_builder.append("HEADER.FIELDS (");
        else
            headers_builder.append("HEADERS.FIELDS.NOT (");

        bool first = true;
        for (auto& field : headers.value()) {
            if (!first)
                headers_builder.append(" ");
            headers_builder.append(field);
            first = false;
        }
        headers_builder.append(")");
        return headers_builder.build();
    }
    case SectionType::Text:
        return "TEXT";
    case SectionType::Parts: {
        StringBuilder sb;
        bool first = true;
        for (int part : parts.value()) {
            if (!first)
                sb.append(".");
            sb.appendff("{}", part);
            first = false;
        }
        if (ends_with_mime) {
            sb.append(".MIME");
        }
        return sb.build();
    }
    }
    VERIFY_NOT_REACHED();
}
String FetchCommand::DataItem::serialize() const
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
        TODO();
    case DataItemType::BodySection: {
        StringBuilder sb;
        sb.appendff("BODY[{}]", section.value().serialize());
        if (partial_fetch) {
            sb.appendff("<{}.{}>", start, octets);
        }

        return sb.build();
    }
    case DataItemType::BodyStructure:
        return "BODYSTRUCTURE";
    }
    VERIFY_NOT_REACHED();
}
String FetchCommand::serialize()
{
    StringBuilder sequence_builder;
    bool first = true;
    for (auto& sequence : sequence_set) {
        if (!first) {
            sequence_builder.append(",");
        }
        sequence_builder.append(sequence.serialize());
        first = false;
    }

    StringBuilder data_items_builder;
    first = true;
    for (auto& data_item : data_items) {
        if (!first) {
            data_items_builder.append(" ");
        }
        data_items_builder.append(data_item.serialize());
        first = false;
    }

    return AK::String::formatted("{} ({})", sequence_builder.build(), data_items_builder.build());
}
String serialize_astring(StringView string)
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
        auto escaped_str = string.replace("\\", "\\\\").replace("\"", "\\\"");
        return String::formatted("\"{}\"", escaped_str);
    }

    // Just send a literal
    return String::formatted("{{{}}}\r\n{}", string.length(), string);
}
String SearchKey::serialize() const
{
    return data.visit(
        [&](All const&) { return String("ALL"); },
        [&](Answered const&) { return String("ANSWERED"); },
        [&](Bcc const& x) { return String::formatted("BCC {}", serialize_astring(x.bcc)); },
        [&](Cc const& x) { return String::formatted("CC {}", serialize_astring(x.cc)); },
        [&](Deleted const&) { return String("DELETED"); },
        [&](Draft const&) { return String("DRAFT"); },
        [&](From const& x) { return String::formatted("FROM {}", serialize_astring(x.from)); },
        [&](Header const& x) { return String::formatted("HEADER {} {}", serialize_astring(x.header), serialize_astring(x.value)); },
        [&](Keyword const& x) { return String::formatted("KEYWORD {}", x.keyword); },
        [&](Larger const& x) { return String::formatted("LARGER {}", x.number); },
        [&](New const&) { return String("NEW"); },
        [&](Not const& x) { return String::formatted("NOT {}", x.operand->serialize()); },
        [&](Old const&) { return String("OLD"); },
        [&](On const& x) { return String::formatted("ON {}", x.date.to_string("%d-%b-%Y")); },
        [&](Or const& x) { return String::formatted("OR {} {}", x.lhs->serialize(), x.rhs->serialize()); },
        [&](Recent const&) { return String("RECENT"); },
        [&](SearchKeys const& x) {
            StringBuilder sb;
            sb.append("(");
            bool first = true;
            for (const auto& item : x.keys) {
                if (!first)
                    sb.append(", ");
                sb.append(item->serialize());
                first = false;
            }
            return sb.build();
        },
        [&](Seen const&) { return String("SEEN"); },
        [&](SentBefore const& x) { return String::formatted("SENTBEFORE {}", x.date.to_string("%d-%b-%Y")); },
        [&](SentOn const& x) { return String::formatted("SENTON {}", x.date.to_string("%d-%b-%Y")); },
        [&](SentSince const& x) { return String::formatted("SENTSINCE {}", x.date.to_string("%d-%b-%Y")); },
        [&](SequenceSet const& x) { return x.sequence.serialize(); },
        [&](Since const& x) { return String::formatted("SINCE {}", x.date.to_string("%d-%b-%Y")); },
        [&](Smaller const& x) { return String::formatted("SMALLER {}", x.number); },
        [&](Subject const& x) { return String::formatted("SUBJECT {}", serialize_astring(x.subject)); },
        [&](Text const& x) { return String::formatted("TEXT {}", serialize_astring(x.text)); },
        [&](To const& x) { return String::formatted("TO {}", serialize_astring(x.to)); },
        [&](UID const& x) { return String::formatted("UID {}", x.uid); },
        [&](Unanswered const&) { return String("UNANSWERED"); },
        [&](Undeleted const&) { return String("UNDELETED"); },
        [&](Undraft const&) { return String("UNDRAFT"); },
        [&](Unkeyword const& x) { return String::formatted("UNKEYWORD {}", serialize_astring(x.flag_keyword)); },
        [&](Unseen const&) { return String("UNSEEN"); });
}
}
