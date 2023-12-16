/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Result.h>
#include <LibIMAP/Objects.h>

namespace IMAP {
class Client;

struct ParseStatus {
    bool successful;
    Optional<Response> response;
};

class Parser {
public:
    ParseStatus parse(ByteBuffer&& buffer, bool expecting_tag);

private:
    static MailboxFlag parse_mailbox_flag(StringView);

    ErrorOr<ParseStatus> try_parse(ByteBuffer&& buffer, bool expecting_tag);

    ErrorOr<void> consume(StringView);
    bool consume_if(StringView);
    StringView consume_while(Function<bool(u8)> should_consume);
    StringView consume_until_end_of_line();

    bool at_end() { return m_position >= m_buffer.size(); }

    ErrorOr<unsigned> parse_number();
    Optional<unsigned> try_parse_number();

    ErrorOr<void> parse_response_done();
    ErrorOr<void> parse_untagged();
    ErrorOr<void> parse_capability_response();

    ErrorOr<StringView> parse_atom();
    ErrorOr<StringView> parse_quoted_string();
    ErrorOr<StringView> parse_literal_string();
    ErrorOr<StringView> parse_string();
    ErrorOr<StringView> parse_astring();
    ErrorOr<StringView> parse_nstring();

    ErrorOr<ResponseStatus> parse_status();
    ErrorOr<ListItem> parse_list_item();
    ErrorOr<FetchCommand::DataItem> parse_fetch_data_item();
    ErrorOr<FetchResponseData> parse_fetch_response();
    ErrorOr<Vector<Address>> parse_address_list();
    ErrorOr<Address> parse_address();
    ErrorOr<HashMap<ByteString, ByteString>> parse_body_fields_params();
    ErrorOr<BodyStructure> parse_body_structure();
    ErrorOr<BodyStructure> parse_one_part_body();
    ErrorOr<BodyExtension> parse_body_extension();
    ErrorOr<Tuple<ByteString, HashMap<ByteString, ByteString>>> parse_disposition();
    ErrorOr<Vector<ByteString>> parse_langs();
    ErrorOr<Envelope> parse_envelope();

    template<typename T>
    ErrorOr<Vector<T>> parse_list(T (*converter)(StringView));

    // To retain state if parsing is not finished
    ByteBuffer m_buffer;
    SolidResponse m_response;
    unsigned m_position { 0 };
    bool m_incomplete { false };
};
}
