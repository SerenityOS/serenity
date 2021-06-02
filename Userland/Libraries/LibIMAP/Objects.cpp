/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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
    case DataItemType::BodySection:
        StringBuilder sb;
        sb.appendff("BODY[{}]", section.value().serialize());
        if (partial_fetch) {
            sb.appendff("<{}.{}>", start, octets);
        }

        return sb.build();
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
}
