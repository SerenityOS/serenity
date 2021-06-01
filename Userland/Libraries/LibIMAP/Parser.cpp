/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibIMAP/Parser.h>

namespace IMAP {

ParseStatus Parser::parse(ByteBuffer&& buffer, bool expecting_tag)
{
    if (m_incomplete) {
        m_buffer += buffer;
        m_incomplete = false;
    } else {
        m_buffer = move(buffer);
        position = 0;
        m_response = SolidResponse();
    }

    if (try_consume("+")) {
        consume(" ");
        auto data = parse_while([](u8 x) { return x != '\r'; });
        consume("\r\n");
        return { true, { ContinueRequest { data } } };
    }

    while (try_consume("*")) {
        parse_untagged();
    }

    if (expecting_tag) {
        if (at_end()) {
            m_incomplete = true;
            return { true, {} };
        }
        parse_response_done();
    }

    if (m_parsing_failed) {
        return { false, {} };
    } else {
        return { true, { { move(m_response) } } };
    }
}

bool Parser::try_consume(StringView x)
{
    size_t i = 0;
    auto previous_position = position;
    while (i < x.length() && !at_end() && to_ascii_lowercase(x[i]) == to_ascii_lowercase(m_buffer[position])) {
        i++;
        position++;
    }
    if (i != x.length()) {
        // We didn't match the full string.
        position = previous_position;
        return false;
    }

    return true;
}

void Parser::parse_response_done()
{
    consume("A");
    auto tag = parse_number();
    consume(" ");

    ResponseStatus status = parse_status();
    consume(" ");

    m_response.m_tag = tag;
    m_response.m_status = status;

    StringBuilder response_data;

    while (!at_end() && m_buffer[position] != '\r') {
        response_data.append((char)m_buffer[position]);
        position += 1;
    }

    consume("\r\n");
    m_response.m_response_text = response_data.build();
}

void Parser::consume(StringView x)
{
    if (!try_consume(x)) {
        dbgln("{} not matched at {}, buffer: {}", x, position, StringView(m_buffer.data(), m_buffer.size()));

        m_parsing_failed = true;
    }
}

Optional<unsigned> Parser::try_parse_number()
{
    auto number_matched = 0;
    while (!at_end() && 0 <= m_buffer[position] - '0' && m_buffer[position] - '0' <= 9) {
        number_matched++;
        position++;
    }
    if (number_matched == 0)
        return {};

    auto number = StringView(m_buffer.data() + position - number_matched, number_matched);

    return number.to_uint();
}

unsigned Parser::parse_number()
{
    auto number = try_parse_number();
    if (!number.has_value()) {
        m_parsing_failed = true;
        return -1;
    }

    return number.value();
}

void Parser::parse_untagged()
{
    consume(" ");

    // Certain messages begin with a number like:
    // * 15 EXISTS
    auto number = try_parse_number();
    if (number.has_value()) {
        consume(" ");
        auto data_type = parse_atom().to_string();
        if (data_type.matches("EXISTS")) {
            m_response.data().set_exists(number.value());
            consume("\r\n");
        } else if (data_type.matches("RECENT")) {
            m_response.data().set_recent(number.value());
            consume("\r\n");
        }
        return;
    }

    if (try_consume("CAPABILITY")) {
        parse_capability_response();
    } else if (try_consume("LIST")) {
        auto item = parse_list_item();
        m_response.data().add_list_item(move(item));
    } else if (try_consume("FLAGS")) {
        consume(" ");
        auto flags = parse_list(+[](StringView x) { return String(x); });
        m_response.data().set_flags(move(flags));
        consume("\r\n");
    } else if (try_consume("OK")) {
        consume(" ");
        if (try_consume("[")) {
            auto actual_type = parse_atom();
            consume(" ");
            if (actual_type.matches("UIDNEXT")) {
                auto n = parse_number();
                m_response.data().set_uid_next(n);
            } else if (actual_type.matches("UIDVALIDITY")) {
                auto n = parse_number();
                m_response.data().set_uid_validity(n);
            } else if (actual_type.matches("UNSEEN")) {
                auto n = parse_number();
                m_response.data().set_unseen(n);
            } else if (actual_type.matches("PERMANENTFLAGS")) {
                auto flags = parse_list(+[](StringView x) { return String(x); });
                m_response.data().set_permanent_flags(move(flags));
            } else {
                dbgln("Unknown: {}", actual_type);
                parse_while([](u8 x) { return x != ']'; });
            }
            consume("]");
            parse_while([](u8 x) { return x != '\r'; });
            consume("\r\n");
        } else {
            parse_while([](u8 x) { return x != '\r'; });
            consume("\r\n");
        }
    } else {
        auto x = parse_while([](u8 x) { return x != '\r'; });
        consume("\r\n");
        dbgln("ignored {}", x);
    }
}

StringView Parser::parse_quoted_string()
{
    auto str = parse_while([](u8 x) { return x != '"'; });
    consume("\"");
    return str;
}

StringView Parser::parse_string()
{
    if (try_consume("\"")) {
        return parse_quoted_string();
    } else {
        return parse_literal_string();
    }
}

Optional<StringView> Parser::parse_nstring()
{
    if (try_consume("NIL"))
        return {};
    else
        return { parse_string() };
}

StringView Parser::parse_literal_string()
{
    consume("{");
    auto num_bytes = parse_number();
    consume("}\r\n");

    if (m_buffer.size() < position + num_bytes) {
        m_parsing_failed = true;
        return "";
    }

    position += num_bytes;
    return StringView(m_buffer.data() + position - num_bytes, num_bytes);
}

ListItem Parser::parse_list_item()
{
    consume(" ");
    auto flags_vec = parse_list(parse_mailbox_flag);
    unsigned flags = 0;
    for (auto flag : flags_vec) {
        flags |= static_cast<unsigned>(flag);
    }
    consume(" \"");
    auto reference = parse_while([](u8 x) { return x != '"'; });
    consume("\" ");
    auto mailbox = parse_astring();
    consume("\r\n");
    return ListItem { flags, String(reference), String(mailbox) };
}

void Parser::parse_capability_response()
{
    auto capability = AK::Vector<String>();
    while (!try_consume("\r\n")) {
        consume(" ");
        auto x = String(parse_atom());
        capability.append(x);
    }
    m_response.data().add_capabilities(move(capability));
}

StringView Parser::parse_atom()
{
    auto is_non_atom_char = [](u8 x) {
        auto non_atom_chars = { '(', ')', '{', ' ', '%', '*', '"', '\\', ']' };
        return AK::find(non_atom_chars.begin(), non_atom_chars.end(), x) != non_atom_chars.end();
    };

    auto start = position;
    auto count = 0;
    while (!at_end() && !is_ascii_control(m_buffer[position]) && !is_non_atom_char(m_buffer[position])) {
        count++;
        position++;
    }

    return StringView(m_buffer.data() + start, count);
}

ResponseStatus Parser::parse_status()
{
    auto atom = parse_atom();

    if (atom.matches("OK")) {
        return ResponseStatus::OK;
    } else if (atom.matches("BAD")) {
        return ResponseStatus::Bad;
    } else if (atom.matches("NO")) {
        return ResponseStatus::No;
    }

    m_parsing_failed = true;
    return ResponseStatus::Bad;
}

template<typename T>
Vector<T> Parser::parse_list(T converter(StringView))
{
    consume("(");
    Vector<T> x;
    bool first = true;
    while (!try_consume(")")) {
        if (!first)
            consume(" ");
        auto item = parse_while([](u8 x) {
            return x != ' ' && x != ')';
        });
        x.append(converter(item));
        first = false;
    }

    return x;
}

MailboxFlag Parser::parse_mailbox_flag(StringView s)
{
    if (s.matches("\\All"))
        return MailboxFlag::All;
    if (s.matches("\\Drafts"))
        return MailboxFlag::Drafts;
    if (s.matches("\\Flagged"))
        return MailboxFlag::Flagged;
    if (s.matches("\\HasChildren"))
        return MailboxFlag::HasChildren;
    if (s.matches("\\HasNoChildren"))
        return MailboxFlag::HasNoChildren;
    if (s.matches("\\Important"))
        return MailboxFlag::Important;
    if (s.matches("\\Junk"))
        return MailboxFlag::Junk;
    if (s.matches("\\Marked"))
        return MailboxFlag::Marked;
    if (s.matches("\\Noinferiors"))
        return MailboxFlag::NoInferiors;
    if (s.matches("\\Noselect"))
        return MailboxFlag::NoSelect;
    if (s.matches("\\Sent"))
        return MailboxFlag::Sent;
    if (s.matches("\\Trash"))
        return MailboxFlag::Trash;
    if (s.matches("\\Unmarked"))
        return MailboxFlag::Unmarked;

    dbgln("Unrecognized mailbox flag {}", s);
    return MailboxFlag::Unknown;
}

StringView Parser::parse_while(Function<bool(u8)> should_consume)
{
    int chars = 0;
    while (!at_end() && should_consume(m_buffer[position])) {
        position++;
        chars++;
    }
    return StringView(m_buffer.data() + position - chars, chars);
}

}
