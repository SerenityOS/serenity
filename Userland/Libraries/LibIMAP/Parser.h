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

    void consume(StringView);
    bool try_consume(StringView);
    StringView consume_while(Function<bool(u8)> should_consume);
    StringView consume_until_end_of_line();

    bool at_end() { return m_position >= m_buffer.size(); }

    unsigned parse_number();
    Optional<unsigned> try_parse_number();

    void parse_response_done();
    void parse_untagged();
    void parse_capability_response();

    StringView parse_atom();
    StringView parse_quoted_string();
    StringView parse_literal_string();
    StringView parse_string();
    StringView parse_astring();
    Optional<StringView> parse_nstring();

    ResponseStatus parse_status();
    ListItem parse_list_item();
    FetchCommand::DataItem parse_fetch_data_item();
    FetchResponseData parse_fetch_response();
    Optional<Vector<Address>> parse_address_list();
    Address parse_address();
    HashMap<DeprecatedString, DeprecatedString> parse_body_fields_params();
    BodyStructure parse_body_structure();
    BodyStructure parse_one_part_body();
    BodyExtension parse_body_extension();
    Tuple<DeprecatedString, HashMap<DeprecatedString, DeprecatedString>> parse_disposition();
    Vector<DeprecatedString> parse_langs();
    Envelope parse_envelope();

    template<typename T>
    Vector<T> parse_list(T (*converter)(StringView));

    // To retain state if parsing is not finished
    ByteBuffer m_buffer;
    SolidResponse m_response;
    unsigned m_position { 0 };
    bool m_incomplete { false };
    bool m_parsing_failed { false };
};
}
