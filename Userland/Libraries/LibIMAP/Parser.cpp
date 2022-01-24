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
        auto data = consume_until_end_of_line();
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
        if (data_type == "EXISTS"sv) {
            m_response.data().set_exists(number.value());
            consume("\r\n");
        } else if (data_type == "RECENT"sv) {
            m_response.data().set_recent(number.value());
            consume("\r\n");
        } else if (data_type == "FETCH"sv) {
            auto fetch_response = parse_fetch_response();
            m_response.data().add_fetch_response(number.value(), move(fetch_response));
        } else if (data_type == "EXPUNGE"sv) {
            m_response.data().add_expunged(number.value());
            consume("\r\n");
        }
        return;
    }

    if (try_consume("CAPABILITY")) {
        parse_capability_response();
    } else if (try_consume("LIST")) {
        auto item = parse_list_item();
        m_response.data().add_list_item(move(item));
    } else if (try_consume("LSUB")) {
        auto item = parse_list_item();
        m_response.data().add_lsub_item(move(item));
    } else if (try_consume("FLAGS")) {
        consume(" ");
        auto flags = parse_list(+[](StringView x) { return String(x); });
        m_response.data().set_flags(move(flags));
        consume("\r\n");
    } else if (try_consume("OK")) {
        consume(" ");
        if (try_consume("[")) {
            auto actual_type = parse_atom();
            if (actual_type == "CLOSED"sv) {
                // No-op.
            } else if (actual_type == "UIDNEXT"sv) {
                consume(" ");
                auto n = parse_number();
                m_response.data().set_uid_next(n);
            } else if (actual_type == "UIDVALIDITY"sv) {
                consume(" ");
                auto n = parse_number();
                m_response.data().set_uid_validity(n);
            } else if (actual_type == "UNSEEN"sv) {
                consume(" ");
                auto n = parse_number();
                m_response.data().set_unseen(n);
            } else if (actual_type == "PERMANENTFLAGS"sv) {
                consume(" ");
                auto flags = parse_list(+[](StringView x) { return String(x); });
                m_response.data().set_permanent_flags(move(flags));
            } else if (actual_type == "HIGHESTMODSEQ"sv) {
                consume(" ");
                parse_number();
                // No-op for now.
            } else {
                dbgln("Unknown: {}", actual_type);
                consume_while([](u8 x) { return x != ']'; });
            }
            consume("]");
        }
        consume_until_end_of_line();
        consume("\r\n");
    } else if (try_consume("SEARCH")) {
        Vector<unsigned> ids;
        while (!try_consume("\r\n")) {
            consume(" ");
            auto id = parse_number();
            ids.append(id);
        }
        m_response.data().set_search_results(move(ids));
    } else if (try_consume("BYE")) {
        auto message = consume_until_end_of_line();
        consume("\r\n");
        m_response.data().set_bye(message.is_empty() ? Optional<String>() : Optional<String>(message));
    } else if (try_consume("STATUS")) {
        consume(" ");
        auto mailbox = parse_astring();
        consume(" (");
        auto status_item = StatusItem();
        status_item.set_mailbox(mailbox);
        while (!try_consume(")")) {
            auto status_att = parse_atom();
            consume(" ");
            auto value = parse_number();

            auto type = StatusItemType::Recent;
            if (status_att == "MESSAGES"sv) {
                type = StatusItemType::Messages;
            } else if (status_att == "UNSEEN"sv) {
                type = StatusItemType::Unseen;
            } else if (status_att == "UIDNEXT"sv) {
                type = StatusItemType::UIDNext;
            } else if (status_att == "UIDVALIDITY"sv) {
                type = StatusItemType::UIDValidity;
            } else if (status_att == "RECENT"sv) {
                type = StatusItemType::Recent;
            } else {
                dbgln("Unmatched status attribute: {}", status_att);
                m_parsing_failed = true;
            }

            status_item.set(type, value);

            if (!at_end() && m_buffer[position] != ')')
                consume(" ");
        }
        m_response.data().set_status(move(status_item));
        try_consume(" "); // Not in the spec but the Outlook server sends a space for some reason.
        consume("\r\n");
    } else {
        auto x = consume_until_end_of_line();
        consume("\r\n");
        dbgln("ignored {}", x);
    }
}

StringView Parser::parse_quoted_string()
{
    auto str = consume_while([](u8 x) { return x != '"'; });
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

FetchResponseData Parser::parse_fetch_response()
{
    consume(" (");
    auto fetch_response = FetchResponseData();

    while (!try_consume(")")) {
        auto data_item = parse_fetch_data_item();
        switch (data_item.type) {
        case FetchCommand::DataItemType::BodyStructure: {
            consume(" (");
            auto structure = parse_body_structure();
            fetch_response.set_body_structure(move(structure));
            break;
        }
        case FetchCommand::DataItemType::Envelope: {
            fetch_response.set_envelope(parse_envelope());
            break;
        }
        case FetchCommand::DataItemType::Flags: {
            consume(" ");
            auto flags = parse_list(+[](StringView x) { return String(x); });
            fetch_response.set_flags(move(flags));
            break;
        }
        case FetchCommand::DataItemType::InternalDate: {
            consume(" \"");
            auto date_view = consume_while([](u8 x) { return x != '"'; });
            consume("\"");
            auto date = Core::DateTime::parse("%d-%b-%Y %H:%M:%S %z", date_view).value();
            fetch_response.set_internal_date(date);
            break;
        }
        case FetchCommand::DataItemType::UID: {
            consume(" ");
            fetch_response.set_uid(parse_number());
            break;
        }
        case FetchCommand::DataItemType::PeekBody:
            // Spec doesn't allow for this in a response.
            m_parsing_failed = true;
            break;
        case FetchCommand::DataItemType::BodySection: {
            auto body = parse_nstring();
            fetch_response.add_body_data(move(data_item), Optional<String>(move(body)));
            break;
        }
        }
        if (!at_end() && m_buffer[position] != ')')
            consume(" ");
    }
    consume("\r\n");
    return fetch_response;
}
Envelope Parser::parse_envelope()
{
    consume(" (");
    auto date = parse_nstring();
    consume(" ");
    auto subject = parse_nstring();
    consume(" ");
    auto from = parse_address_list();
    consume(" ");
    auto sender = parse_address_list();
    consume(" ");
    auto reply_to = parse_address_list();
    consume(" ");
    auto to = parse_address_list();
    consume(" ");
    auto cc = parse_address_list();
    consume(" ");
    auto bcc = parse_address_list();
    consume(" ");
    auto in_reply_to = parse_nstring();
    consume(" ");
    auto message_id = parse_nstring();
    consume(")");
    Envelope envelope = {
        date.has_value() ? AK::Optional<String>(date.value()) : AK::Optional<String>(),
        subject.has_value() ? AK::Optional<String>(subject.value()) : AK::Optional<String>(),
        from,
        sender,
        reply_to,
        to,
        cc,
        bcc,
        in_reply_to.has_value() ? AK::Optional<String>(in_reply_to.value()) : AK::Optional<String>(),
        message_id.has_value() ? AK::Optional<String>(message_id.value()) : AK::Optional<String>(),
    };
    return envelope;
}
BodyStructure Parser::parse_body_structure()
{
    if (!at_end() && m_buffer[position] == '(') {
        auto data = MultiPartBodyStructureData();
        while (try_consume("(")) {
            auto child = parse_body_structure();
            data.bodies.append(make<BodyStructure>(move(child)));
        }
        consume(" ");
        data.media_type = parse_string();

        if (!try_consume(")")) {
            consume(" ");
            data.params = try_consume("NIL") ? Optional<HashMap<String, String>>() : parse_body_fields_params();
            if (!try_consume(")")) {
                consume(" ");
                if (!try_consume("NIL")) {
                    data.disposition = { parse_disposition() };
                }

                if (!try_consume(")")) {
                    consume(" ");
                    if (!try_consume("NIL")) {
                        data.langs = { parse_langs() };
                    }

                    if (!try_consume(")")) {
                        consume(" ");
                        data.location = try_consume("NIL") ? Optional<String>() : Optional<String>(parse_string());

                        if (!try_consume(")")) {
                            consume(" ");
                            Vector<BodyExtension> extensions;
                            while (!try_consume(")")) {
                                extensions.append(parse_body_extension());
                                try_consume(" ");
                            }
                            data.extensions = { move(extensions) };
                        }
                    }
                }
            }
        }

        return BodyStructure(move(data));
    } else {
        return parse_one_part_body();
    }
}
BodyStructure Parser::parse_one_part_body()
{
    auto type = parse_string();
    consume(" ");
    auto subtype = parse_string();
    consume(" ");
    if (type.equals_ignoring_case("TEXT")) {
        // body-type-text
        auto params = parse_body_fields_params();
        consume(" ");
        auto id = parse_nstring();
        consume(" ");
        auto description = parse_nstring();
        consume(" ");
        auto encoding = parse_string();
        consume(" ");
        auto num_octets = parse_number();
        consume(" ");
        auto num_lines = parse_number();

        auto data = BodyStructureData {
            type,
            subtype,
            Optional<String>(move(id)),
            Optional<String>(move(description)),
            encoding,
            params,
            num_octets,
            num_lines,
            {}
        };

        if (!try_consume(")")) {
            consume(" ");
            auto md5 = parse_nstring();
            if (md5.has_value())
                data.md5 = { md5.value() };
            if (!try_consume(")")) {
                consume(" ");
                if (!try_consume("NIL")) {
                    auto disposition = parse_disposition();
                    data.disposition = { disposition };
                }

                if (!try_consume(")")) {
                    consume(" ");
                    if (!try_consume("NIL")) {
                        data.langs = { parse_langs() };
                    }

                    if (!try_consume(")")) {
                        consume(" ");
                        auto location = parse_nstring();
                        if (location.has_value())
                            data.location = { location.value() };

                        Vector<BodyExtension> extensions;
                        while (!try_consume(")")) {
                            extensions.append(parse_body_extension());
                            try_consume(" ");
                        }
                        data.extensions = { move(extensions) };
                    }
                }
            }
        }

        return BodyStructure(move(data));
    } else if (type.equals_ignoring_case("MESSAGE") && subtype.equals_ignoring_case("RFC822")) {
        // body-type-message
        auto params = parse_body_fields_params();
        consume(" ");
        auto id = parse_nstring();
        consume(" ");
        auto description = parse_nstring();
        consume(" ");
        auto encoding = parse_string();
        consume(" ");
        auto num_octets = parse_number();
        consume(" ");
        auto envelope = parse_envelope();

        BodyStructureData data {
            type,
            subtype,
            Optional<String>(move(id)),
            Optional<String>(move(description)),
            encoding,
            params,
            num_octets,
            0,
            envelope
        };

        return BodyStructure(move(data));
    } else {
        // body-type-basic
        auto params = parse_body_fields_params();
        consume(" ");
        auto id = parse_nstring();
        consume(" ");
        auto description = parse_nstring();
        consume(" ");
        auto encoding = parse_string();
        consume(" ");
        auto num_octets = parse_number();
        consume(" ");

        BodyStructureData data {
            type,
            subtype,
            Optional<String>(move(id)),
            Optional<String>(move(description)),
            encoding,
            params,
            num_octets,
            0,
            {}
        };

        return BodyStructure(move(data));
    }
}
Vector<String> Parser::parse_langs()
{
    AK::Vector<String> langs;
    if (!try_consume("(")) {
        langs.append(parse_string());
    } else {
        while (!try_consume(")")) {
            langs.append(parse_string());
            try_consume(" ");
        }
    }
    return langs;
}
Tuple<String, HashMap<String, String>> Parser::parse_disposition()
{
    auto disposition_type = parse_string();
    consume(" ");
    auto disposition_vals = parse_body_fields_params();
    consume(")");
    return { move(disposition_type), move(disposition_vals) };
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
    auto reference = consume_while([](u8 x) { return x != '"'; });
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

    if (atom == "OK"sv) {
        return ResponseStatus::OK;
    } else if (atom == "BAD"sv) {
        return ResponseStatus::Bad;
    } else if (atom == "NO"sv) {
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
        auto item = consume_while([](u8 x) {
            return x != ' ' && x != ')';
        });
        x.append(converter(item));
        first = false;
    }

    return x;
}

MailboxFlag Parser::parse_mailbox_flag(StringView s)
{
    if (s == "\\All"sv)
        return MailboxFlag::All;
    if (s == "\\Drafts"sv)
        return MailboxFlag::Drafts;
    if (s == "\\Flagged"sv)
        return MailboxFlag::Flagged;
    if (s == "\\HasChildren"sv)
        return MailboxFlag::HasChildren;
    if (s == "\\HasNoChildren"sv)
        return MailboxFlag::HasNoChildren;
    if (s == "\\Important"sv)
        return MailboxFlag::Important;
    if (s == "\\Junk"sv)
        return MailboxFlag::Junk;
    if (s == "\\Marked"sv)
        return MailboxFlag::Marked;
    if (s == "\\Noinferiors"sv)
        return MailboxFlag::NoInferiors;
    if (s == "\\Noselect"sv)
        return MailboxFlag::NoSelect;
    if (s == "\\Sent"sv)
        return MailboxFlag::Sent;
    if (s == "\\Trash"sv)
        return MailboxFlag::Trash;
    if (s == "\\Unmarked"sv)
        return MailboxFlag::Unmarked;

    dbgln("Unrecognized mailbox flag {}", s);
    return MailboxFlag::Unknown;
}

StringView Parser::consume_while(Function<bool(u8)> should_consume)
{
    int chars = 0;
    while (!at_end() && should_consume(m_buffer[position])) {
        position++;
        chars++;
    }
    return StringView(m_buffer.data() + position - chars, chars);
}

StringView Parser::consume_until_end_of_line()
{
    return consume_while([](u8 x) { return x != '\r'; });
}

FetchCommand::DataItem Parser::parse_fetch_data_item()
{
    auto msg_attr = consume_while([](u8 x) { return is_ascii_alpha(x) != 0; });
    if (msg_attr.equals_ignoring_case("BODY") && try_consume("[")) {
        auto data_item = FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::BodySection,
            .section = { {} }
        };
        auto section_type = consume_while([](u8 x) { return x != ']' && x != ' '; });
        if (section_type.equals_ignoring_case("HEADER.FIELDS")) {
            data_item.section->type = FetchCommand::DataItem::SectionType::HeaderFields;
            data_item.section->headers = Vector<String>();
            consume(" ");
            auto headers = parse_list(+[](StringView x) { return x; });
            for (auto& header : headers) {
                data_item.section->headers->append(header);
            }
            consume("]");
        } else if (section_type.equals_ignoring_case("HEADER.FIELDS.NOT")) {
            data_item.section->type = FetchCommand::DataItem::SectionType::HeaderFieldsNot;
            data_item.section->headers = Vector<String>();
            consume(" (");
            auto headers = parse_list(+[](StringView x) { return x; });
            for (auto& header : headers) {
                data_item.section->headers->append(header);
            }
            consume("]");
        } else if (is_ascii_digit(section_type[0])) {
            data_item.section->type = FetchCommand::DataItem::SectionType::Parts;
            data_item.section->parts = Vector<unsigned>();

            while (!try_consume("]")) {
                auto num = try_parse_number();
                if (num.has_value()) {
                    data_item.section->parts->append(num.value());
                    continue;
                }
                auto atom = parse_atom();
                if (atom.equals_ignoring_case("MIME")) {
                    data_item.section->ends_with_mime = true;
                    continue;
                }
            }
        } else if (section_type.equals_ignoring_case("TEXT")) {
            data_item.section->type = FetchCommand::DataItem::SectionType::Text;
        } else if (section_type.equals_ignoring_case("HEADER")) {
            data_item.section->type = FetchCommand::DataItem::SectionType::Header;
        } else {
            dbgln("Unmatched section type {}", section_type);
            m_parsing_failed = true;
        }
        if (try_consume("<")) {
            auto start = parse_number();
            data_item.partial_fetch = true;
            data_item.start = (int)start;
            consume(">");
        }
        try_consume(" ");
        return data_item;
    } else if (msg_attr.equals_ignoring_case("FLAGS")) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::Flags
        };
    } else if (msg_attr.equals_ignoring_case("UID")) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::UID
        };
    } else if (msg_attr.equals_ignoring_case("INTERNALDATE")) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::InternalDate
        };
    } else if (msg_attr.equals_ignoring_case("ENVELOPE")) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::Envelope
        };
    } else if (msg_attr.equals_ignoring_case("BODY") || msg_attr.equals_ignoring_case("BODYSTRUCTURE")) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::BodyStructure
        };
    } else {
        dbgln("msg_attr not matched: {}", msg_attr);
        m_parsing_failed = true;
        return FetchCommand::DataItem {};
    }
}
Optional<Vector<Address>> Parser::parse_address_list()
{
    if (try_consume("NIL"))
        return {};

    auto addresses = Vector<Address>();
    consume("(");
    while (!try_consume(")")) {
        addresses.append(parse_address());
        if (!at_end() && m_buffer[position] != ')')
            consume(" ");
    }
    return { addresses };
}

Address Parser::parse_address()
{
    consume("(");
    auto address = Address();
    auto name = parse_nstring();
    address.name = Optional<String>(move(name));
    consume(" ");
    auto source_route = parse_nstring();
    address.source_route = Optional<String>(move(source_route));
    consume(" ");
    auto mailbox = parse_nstring();
    address.mailbox = Optional<String>(move(mailbox));
    consume(" ");
    auto host = parse_nstring();
    address.host = Optional<String>(move(host));
    consume(")");
    return address;
}
StringView Parser::parse_astring()
{
    if (!at_end() && (m_buffer[position] == '{' || m_buffer[position] == '"'))
        return parse_string();
    else
        return parse_atom();
}
HashMap<String, String> Parser::parse_body_fields_params()
{
    if (try_consume("NIL"))
        return {};

    HashMap<String, String> fields;
    consume("(");
    while (!try_consume(")")) {
        auto key = parse_string();
        consume(" ");
        auto value = parse_string();
        fields.set(key, value);
        try_consume(" ");
    }

    return fields;
}
BodyExtension Parser::parse_body_extension()
{
    if (try_consume("NIL")) {
        return BodyExtension { Optional<String> {} };
    } else if (try_consume("(")) {
        Vector<OwnPtr<BodyExtension>> extensions;
        while (!try_consume(")")) {
            extensions.append(make<BodyExtension>(parse_body_extension()));
            try_consume(" ");
        }
        return BodyExtension { move(extensions) };
    } else if (!at_end() && (m_buffer[position] == '"' || m_buffer[position] == '{')) {
        return BodyExtension { { parse_string() } };
    } else {
        return BodyExtension { parse_number() };
    }
}
}
