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
    // To retain state if parsing is not finished
    ByteBuffer m_buffer;
    SolidResponse m_response;
    unsigned position { 0 };
    bool m_incomplete { false };
    bool m_parsing_failed { false };

    bool try_consume(StringView);
    bool at_end() { return position >= m_buffer.size(); };
    void parse_response_done();
    void consume(StringView x);
    unsigned parse_number();
    Optional<unsigned> try_parse_number();
    void parse_untagged();
    StringView parse_atom();
    ResponseStatus parse_status();
    StringView parse_while(Function<bool(u8)> should_consume);
};
}
