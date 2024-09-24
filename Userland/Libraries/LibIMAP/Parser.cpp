/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <LibIMAP/Parser.h>

namespace IMAP {

ParseStatus Parser::parse(ByteBuffer&& buffer, bool expecting_tag)
{
    auto response_or_error = try_parse(move(buffer), expecting_tag);
    if (response_or_error.is_error())
        return { false, {} };

    auto response = response_or_error.release_value();
    return response;
}

ErrorOr<ParseStatus> Parser::try_parse(ByteBuffer&& buffer, bool expecting_tag)
{
    dbgln_if(IMAP_PARSER_DEBUG, "Parser received {} bytes:\n\"{}\"", buffer.size(), StringView(buffer.data(), buffer.size()));
    if (m_incomplete) {
        m_buffer += buffer;
        m_incomplete = false;
    } else {
        m_buffer = move(buffer);
        m_position = 0;
        m_response = SolidResponse();
    }

    if (consume_if("+"sv)) {
        TRY(consume(" "sv));
        auto data = consume_until_end_of_line();
        TRY(consume(" "sv));
        return ParseStatus { true, { ContinueRequest { data } } };
    }

    while (consume_if("*"sv)) {
        TRY(parse_untagged());
    }

    if (expecting_tag) {
        if (at_end()) {
            m_incomplete = true;
            return ParseStatus { true, {} };
        }
        TRY(parse_response_done());
    }

    return ParseStatus { true, { move(m_response) } };
}

bool Parser::consume_if(StringView x)
{
    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, consume({})", m_position, x);
    size_t i = 0;
    auto previous_position = m_position;
    while (i < x.length() && !at_end() && to_ascii_lowercase(x[i]) == to_ascii_lowercase(m_buffer[m_position])) {
        i++;
        m_position++;
    }
    if (i != x.length()) {
        // We didn't match the full string.
        m_position = previous_position;
        dbgln_if(IMAP_PARSER_DEBUG, "ret false");
        return false;
    }

    dbgln_if(IMAP_PARSER_DEBUG, "ret true");
    return true;
}

ErrorOr<void> Parser::parse_response_done()
{
    TRY(consume("A"sv));
    auto tag = TRY(parse_number());
    TRY(consume(" "sv));

    ResponseStatus status = TRY(parse_status());
    TRY(consume(" "sv));

    m_response.m_tag = tag;
    m_response.m_status = status;

    StringBuilder response_data;

    while (!at_end() && m_buffer[m_position] != '\r') {
        response_data.append((char)m_buffer[m_position]);
        m_position += 1;
    }

    TRY(consume("\r\n"sv));
    m_response.m_response_text = response_data.to_byte_string();
    return {};
}

ErrorOr<void> Parser::consume(StringView x)
{
    if (!consume_if(x)) {
        dbgln("\"{}\" not matched at {}, (buffer length {})", x, m_position, m_buffer.size());
        return Error::from_string_literal("Token not matched");
    }

    return {};
}

Optional<unsigned> Parser::try_parse_number()
{
    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, try_parse_number()", m_position);
    auto number_matched = 0;
    while (!at_end() && 0 <= m_buffer[m_position] - '0' && m_buffer[m_position] - '0' <= 9) {
        number_matched++;
        m_position++;
    }
    if (number_matched == 0) {
        dbgln_if(IMAP_PARSER_DEBUG, "p: {}, ret empty", m_position);
        return {};
    }

    auto number = StringView(m_buffer.data() + m_position - number_matched, number_matched);

    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, ret \"{}\"", m_position, number.to_number<unsigned>());
    return number.to_number<unsigned>();
}

ErrorOr<unsigned> Parser::parse_number()
{
    auto number = try_parse_number();
    if (!number.has_value()) {
        dbgln("Failed to parse number at {}, (buffer length {})", m_position, m_buffer.size());
        return Error::from_string_view("Failed to parse expected number"sv);
    }

    return number.value();
}

ErrorOr<void> Parser::parse_untagged()
{
    TRY(consume(" "sv));

    // Certain messages begin with a number like:
    // * 15 EXISTS
    auto number = try_parse_number();
    if (number.has_value()) {
        TRY(consume(" "sv));
        auto data_type = TRY(parse_atom());
        if (data_type == "EXISTS"sv) {
            m_response.data().set_exists(number.value());
            TRY(consume("\r\n"sv));
        } else if (data_type == "RECENT"sv) {
            m_response.data().set_recent(number.value());
            TRY(consume("\r\n"sv));
        } else if (data_type == "FETCH"sv) {
            auto fetch_response = TRY(parse_fetch_response());
            m_response.data().add_fetch_response(number.value(), move(fetch_response));
        } else if (data_type == "EXPUNGE"sv) {
            m_response.data().add_expunged(number.value());
            TRY(consume("\r\n"sv));
        }
        return {};
    }

    if (consume_if("CAPABILITY"sv)) {
        TRY(parse_capability_response());
    } else if (consume_if("LIST"sv)) {
        auto item = TRY(parse_list_item());
        m_response.data().add_list_item(move(item));
    } else if (consume_if("LSUB"sv)) {
        auto item = TRY(parse_list_item());
        m_response.data().add_lsub_item(move(item));
    } else if (consume_if("FLAGS"sv)) {
        TRY(consume(" "sv));
        auto flags = TRY(parse_list(+[](StringView x) { return ByteString(x); }));
        m_response.data().set_flags(move(flags));
        TRY(consume("\r\n"sv));
    } else if (consume_if("OK"sv)) {
        TRY(consume(" "sv));
        if (consume_if("["sv)) {
            auto actual_type = TRY(parse_atom());
            if (actual_type == "CLOSED"sv) {
                // No-op.
            } else if (actual_type == "UIDNEXT"sv) {
                TRY(consume(" "sv));
                auto n = TRY(parse_number());
                m_response.data().set_uid_next(n);
            } else if (actual_type == "UIDVALIDITY"sv) {
                TRY(consume(" "sv));
                auto n = TRY(parse_number());
                m_response.data().set_uid_validity(n);
            } else if (actual_type == "UNSEEN"sv) {
                TRY(consume(" "sv));
                auto n = TRY(parse_number());
                m_response.data().set_unseen(n);
            } else if (actual_type == "PERMANENTFLAGS"sv) {
                TRY(consume(" "sv));
                auto flags = TRY(parse_list(+[](StringView x) { return ByteString(x); }));
                m_response.data().set_permanent_flags(move(flags));
            } else if (actual_type == "HIGHESTMODSEQ"sv) {
                TRY(consume(" "sv));
                TRY(parse_number());
                // No-op for now.
            } else {
                dbgln("Unknown: {}", actual_type);
                consume_while([](u8 x) { return x != ']'; });
            }
            TRY(consume("]"sv));
        }
        consume_until_end_of_line();
        TRY(consume("\r\n"sv));
    } else if (consume_if("SEARCH"sv)) {
        Vector<unsigned> ids;
        while (!consume_if("\r\n"sv)) {
            TRY(consume(" "sv));
            auto id = TRY(parse_number());
            ids.append(id);
        }
        m_response.data().set_search_results(move(ids));
    } else if (consume_if("BYE"sv)) {
        auto message = consume_until_end_of_line();
        TRY(consume("\r\n"sv));
        m_response.data().set_bye(message.is_empty() ? Optional<ByteString>() : Optional<ByteString>(message));
    } else if (consume_if("STATUS"sv)) {
        TRY(consume(" "sv));
        auto mailbox = TRY(parse_astring());
        TRY(consume(" ("sv));
        auto status_item = StatusItem();
        status_item.set_mailbox(mailbox);
        while (!consume_if(")"sv)) {
            auto status_att = TRY(parse_atom());
            TRY(consume(" "sv));
            auto value = TRY(parse_number());

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
                return Error::from_string_literal("Failed to parse status attribute");
            }

            status_item.set(type, value);

            if (!at_end() && m_buffer[m_position] != ')')
                TRY(consume(" "sv));
        }
        m_response.data().add_status_item(move(status_item));
        consume_if(" "sv); // Not in the spec but the Outlook server sends a space for some reason.
        TRY(consume("\r\n"sv));
    } else {
        auto x = consume_until_end_of_line();
        TRY(consume("\r\n"sv));
        dbgln("ignored {}", x);
    }

    return {};
}

ErrorOr<StringView> Parser::parse_quoted_string()
{
    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, parse_quoted_string()", m_position);
    auto start_position = m_position;
    while (!at_end() && m_buffer[m_position] != '"')
        switch (m_buffer[m_position]) {
        // https://datatracker.ietf.org/doc/html/rfc2683#section-3.4.2
        // 3.4.2. Special Characters

        //    Certain characters, currently the double-quote and the backslash, may
        //    not be sent as-is inside a quoted string.  These characters must be
        //    preceded by the escape character if they are in a quoted string, or
        //    else the string must be sent as a literal.
        case '\\':
            ++m_position;
            if (at_end())
                return Error::from_string_literal("unterminated \\ escape");
            ++m_position;
            break;
        default:
            ++m_position;
        }
    auto str = StringView(m_buffer.data() + start_position, m_position - start_position);
    //   The CR and LF characters may be sent ONLY in literals; they are not
    //   allowed, even if escaped, inside quoted strings.
    if (str.contains("\r"sv))
        return Error::from_string_literal("CR character not allowed inside quoted string");
    if (str.contains("\n"sv))
        return Error::from_string_literal("LF character not allowed inside quoted string");
    TRY(consume("\""sv));
    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, ret \"{}\"", m_position, str);
    return str;
}

ErrorOr<StringView> Parser::parse_string()
{
    if (consume_if("\""sv))
        return parse_quoted_string();

    return parse_literal_string();
}

ErrorOr<StringView> Parser::parse_nstring()
{
    dbgln_if(IMAP_PARSER_DEBUG, "p: {} parse_nstring()", m_position);
    if (consume_if("NIL"sv))
        return StringView {};

    return { TRY(parse_string()) };
}

ErrorOr<FetchResponseData> Parser::parse_fetch_response()
{
    TRY(consume(" ("sv));
    auto fetch_response = FetchResponseData();

    while (!consume_if(")"sv)) {
        auto data_item = TRY(parse_fetch_data_item());
        switch (data_item.type) {
        case FetchCommand::DataItemType::BodyStructure: {
            TRY(consume(" ("sv));
            auto structure = TRY(parse_body_structure());
            fetch_response.set_body_structure(move(structure));
            break;
        }
        case FetchCommand::DataItemType::Envelope: {
            TRY(consume(" "sv));
            fetch_response.set_envelope(TRY(parse_envelope()));
            break;
        }
        case FetchCommand::DataItemType::Flags: {
            TRY(consume(" "sv));
            auto flags = TRY(parse_list(+[](StringView x) { return ByteString(x); }));
            fetch_response.set_flags(move(flags));
            break;
        }
        case FetchCommand::DataItemType::InternalDate: {
            TRY(consume(" \""sv));
            auto date_view = consume_while([](u8 x) { return x != '"'; });
            TRY(consume("\""sv));
            auto date = Core::DateTime::parse("%d-%b-%Y %H:%M:%S %z"sv, date_view).value();
            fetch_response.set_internal_date(date);
            break;
        }
        case FetchCommand::DataItemType::UID: {
            TRY(consume(" "sv));
            fetch_response.set_uid(TRY(parse_number()));
            break;
        }
        case FetchCommand::DataItemType::PeekBody:
            // Spec doesn't allow for this in a response.
            return Error::from_string_literal("Unexpected fetch command type");
        case FetchCommand::DataItemType::BodySection: {
            auto body = TRY(parse_nstring());
            fetch_response.add_body_data(move(data_item), body);
            break;
        }
        }
        if (!at_end() && m_buffer[m_position] != ')')
            TRY(consume(" "sv));
    }
    TRY(consume("\r\n"sv));
    return fetch_response;
}

ErrorOr<Envelope> Parser::parse_envelope()
{
    TRY(consume("("sv));
    auto date = TRY(parse_nstring());
    TRY(consume(" "sv));
    auto subject = TRY(parse_nstring());
    TRY(consume(" "sv));
    auto from = TRY(parse_address_list());
    TRY(consume(" "sv));
    auto sender = TRY(parse_address_list());
    TRY(consume(" "sv));
    auto reply_to = TRY(parse_address_list());
    TRY(consume(" "sv));
    auto to = TRY(parse_address_list());
    TRY(consume(" "sv));
    auto cc = TRY(parse_address_list());
    TRY(consume(" "sv));
    auto bcc = TRY(parse_address_list());
    TRY(consume(" "sv));
    auto in_reply_to = TRY(parse_nstring());
    TRY(consume(" "sv));
    auto message_id = TRY(parse_nstring());
    TRY(consume(")"sv));
    Envelope envelope = {
        date,
        subject,
        from,
        sender,
        reply_to,
        to,
        cc,
        bcc,
        in_reply_to,
        message_id
    };
    return envelope;
}

ErrorOr<BodyStructure> Parser::parse_body_structure()
{
    if (!at_end() && m_buffer[m_position] == '(') {
        auto data = MultiPartBodyStructureData();
        while (consume_if("("sv)) {
            auto child = TRY(parse_body_structure());
            data.bodies.append(make<BodyStructure>(move(child)));
        }
        TRY(consume(" "sv));
        data.multipart_subtype = TRY(parse_string());

        if (!consume_if(")"sv)) {
            TRY(consume(" "sv));
            data.params = consume_if("NIL"sv) ? HashMap<ByteString, ByteString> {} : TRY(parse_body_fields_params());
            if (!consume_if(")"sv)) {
                TRY(consume(" "sv));
                if (!consume_if("NIL"sv)) {
                    data.disposition = { TRY(parse_disposition()) };
                }

                if (!consume_if(")"sv)) {
                    TRY(consume(" "sv));
                    if (!consume_if("NIL"sv)) {
                        data.langs = { TRY(parse_langs()) };
                    }

                    if (!consume_if(")"sv)) {
                        TRY(consume(" "sv));
                        data.location = consume_if("NIL"sv) ? ByteString {} : ByteString(TRY(parse_string()));

                        if (!consume_if(")"sv)) {
                            TRY(consume(" "sv));
                            Vector<BodyExtension> extensions;
                            while (!consume_if(")"sv)) {
                                extensions.append(TRY(parse_body_extension()));
                                consume_if(" "sv);
                            }
                            data.extensions = { move(extensions) };
                        }
                    }
                }
            }
        }

        return BodyStructure(move(data));
    }

    return parse_one_part_body();
}

// body-type-1part
ErrorOr<BodyStructure> Parser::parse_one_part_body()
{
    // NOTE: We share common parts between body-type-basic, body-type-msg and body-type-text types for readability.
    BodyStructureData data;

    // media-basic / media-message / media-text
    data.type = TRY(parse_string());
    TRY(consume(" "sv));
    data.subtype = TRY(parse_string());
    TRY(consume(" "sv));

    // body-fields
    data.fields = TRY(parse_body_fields_params());
    TRY(consume(" "sv));
    data.id = TRY(parse_nstring());
    TRY(consume(" "sv));
    data.desc = TRY(parse_nstring());
    TRY(consume(" "sv));
    data.encoding = TRY(parse_string());
    TRY(consume(" "sv));
    data.bytes = TRY(parse_number());

    if (data.type.equals_ignoring_ascii_case("TEXT"sv)) {
        // body-type-text
        // NOTE: "media-text SP body-fields" part is already parsed.
        TRY(consume(" "sv));
        data.lines = TRY(parse_number());
    } else if (data.type.equals_ignoring_ascii_case("MESSAGE"sv) && data.subtype.is_one_of_ignoring_ascii_case("RFC822"sv, "GLOBAL"sv)) {
        // body-type-msg
        // NOTE: "media-message SP body-fields" part is already parsed.
        TRY(consume(" "sv));
        auto envelope = TRY(parse_envelope());

        TRY(consume(" ("sv));
        auto body = TRY(parse_body_structure());
        data.contanied_message = Tuple { move(envelope), make<BodyStructure>(move(body)) };

        TRY(consume(" "sv));
        data.lines = TRY(parse_number());
    } else {
        // body-type-basic
        // NOTE: "media-basic SP body-fields" is already parsed.
    }

    if (!consume_if(")"sv)) {
        TRY(consume(" "sv));

        // body-ext-1part
        TRY([&]() -> ErrorOr<void> {
            data.md5 = TRY(parse_nstring());

            if (consume_if(")"sv))
                return {};
            TRY(consume(" "sv));
            if (!consume_if("NIL"sv)) {
                data.disposition = { TRY(parse_disposition()) };
            }

            if (consume_if(")"sv))
                return {};
            TRY(consume(" "sv));
            if (!consume_if("NIL"sv)) {
                data.langs = { TRY(parse_langs()) };
            }

            if (consume_if(")"sv))
                return {};
            TRY(consume(" "sv));
            data.location = TRY(parse_nstring());

            Vector<BodyExtension> extensions;
            while (!consume_if(")"sv)) {
                extensions.append(TRY(parse_body_extension()));
                consume_if(" "sv);
            }
            data.extensions = { move(extensions) };
            return {};
        }());
    }

    return BodyStructure(move(data));
}

ErrorOr<Vector<ByteString>> Parser::parse_langs()
{
    AK::Vector<ByteString> langs;
    if (!consume_if("("sv)) {
        langs.append(TRY(parse_string()));
    } else {
        while (!consume_if(")"sv)) {
            langs.append(TRY(parse_string()));
            consume_if(" "sv);
        }
    }
    return langs;
}

ErrorOr<Tuple<ByteString, HashMap<ByteString, ByteString>>> Parser::parse_disposition()
{
    TRY(consume("("sv));
    auto disposition_type = TRY(parse_string());
    TRY(consume(" "sv));
    auto disposition_vals = TRY(parse_body_fields_params());
    TRY(consume(")"sv));
    return Tuple<ByteString, HashMap<ByteString, ByteString>> { move(disposition_type), move(disposition_vals) };
}

ErrorOr<StringView> Parser::parse_literal_string()
{
    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, parse_literal_string()", m_position);
    TRY(consume("{"sv));
    auto num_bytes = TRY(parse_number());
    TRY(consume("}\r\n"sv));

    if (m_buffer.size() < m_position + num_bytes) {
        dbgln("Attempted to parse string with length: {} at position {} (buffer length {})", num_bytes, m_position, m_position);
        return Error::from_string_literal("Failed to parse string");
    }

    m_position += num_bytes;
    auto s = StringView(m_buffer.data() + m_position - num_bytes, num_bytes);
    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, ret \"{}\"", m_position, s);
    return s;
}

ErrorOr<ListItem> Parser::parse_list_item()
{
    TRY(consume(" "sv));
    auto flags_vec = TRY(parse_list(parse_mailbox_flag));
    unsigned flags = 0;
    for (auto flag : flags_vec) {
        flags |= static_cast<unsigned>(flag);
    }
    TRY(consume(" \""sv));
    auto reference = consume_while([](u8 x) { return x != '"'; });
    TRY(consume("\" "sv));
    auto mailbox = TRY(parse_astring());
    TRY(consume("\r\n"sv));
    return ListItem { flags, ByteString(reference), ByteString(mailbox) };
}

ErrorOr<void> Parser::parse_capability_response()
{
    auto capability = AK::Vector<ByteString>();
    while (!consume_if("\r\n"sv)) {
        TRY(consume(" "sv));
        capability.append(TRY(parse_atom()));
    }
    m_response.data().add_capabilities(move(capability));
    return {};
}

ErrorOr<StringView> Parser::parse_atom()
{
    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, parse_atom()", m_position);
    auto is_non_atom_char = [](u8 x) {
        auto non_atom_chars = { '(', ')', '{', ' ', '%', '*', '"', '\\', ']' };
        return AK::find(non_atom_chars.begin(), non_atom_chars.end(), x) != non_atom_chars.end();
    };

    auto start = m_position;
    auto count = 0;
    while (!at_end() && !is_ascii_control(m_buffer[m_position]) && !is_non_atom_char(m_buffer[m_position])) {
        count++;
        m_position++;
    }

    if (count == 0)
        return Error::from_string_literal("Invalid atom value");

    StringView s = StringView(m_buffer.data() + start, count);
    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, ret \"{}\"", m_position, s);
    return s;
}

ErrorOr<ResponseStatus> Parser::parse_status()
{
    auto atom = TRY(parse_atom());

    if (atom == "OK"sv) {
        return ResponseStatus::OK;
    } else if (atom == "BAD"sv) {
        return ResponseStatus::Bad;
    } else if (atom == "NO"sv) {
        return ResponseStatus::No;
    }

    dbgln("Invalid ResponseStatus value: {}", atom);
    return Error::from_string_literal("Failed to parse status type");
}

template<typename T>
ErrorOr<Vector<T>> Parser::parse_list(T converter(StringView))
{
    TRY(consume("("sv));
    Vector<T> x;
    bool first = true;
    while (!consume_if(")"sv)) {
        if (!first)
            TRY(consume(" "sv));
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
    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, consume_while()", m_position);
    int chars = 0;
    while (!at_end() && should_consume(m_buffer[m_position])) {
        m_position++;
        chars++;
    }
    auto s = StringView(m_buffer.data() + m_position - chars, chars);

    dbgln_if(IMAP_PARSER_DEBUG, "p: {}, ret \"{}\"", m_position, s);
    return s;
}

StringView Parser::consume_until_end_of_line()
{
    return consume_while([](u8 x) { return x != '\r'; });
}

ErrorOr<FetchCommand::DataItem> Parser::parse_fetch_data_item()
{
    auto msg_attr = consume_while([](u8 x) { return is_ascii_alpha(x) != 0; });
    if (msg_attr.equals_ignoring_ascii_case("BODY"sv) && consume_if("["sv)) {
        auto data_item = FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::BodySection,
            .section = { {} }
        };
        auto section_type = consume_while([](u8 x) { return x != ']' && x != ' '; });
        if (section_type.equals_ignoring_ascii_case("HEADER.FIELDS"sv)) {
            data_item.section->type = FetchCommand::DataItem::SectionType::HeaderFields;
            data_item.section->headers = Vector<ByteString>();
            TRY(consume(" "sv));
            auto headers = TRY(parse_list(+[](StringView x) { return x; }));
            for (auto& header : headers) {
                data_item.section->headers->append(header);
            }
            TRY(consume("]"sv));
        } else if (section_type.equals_ignoring_ascii_case("HEADER.FIELDS.NOT"sv)) {
            data_item.section->type = FetchCommand::DataItem::SectionType::HeaderFieldsNot;
            data_item.section->headers = Vector<ByteString>();
            TRY(consume(" ("sv));
            auto headers = TRY(parse_list(+[](StringView x) { return x; }));
            for (auto& header : headers) {
                data_item.section->headers->append(header);
            }
            TRY(consume("]"sv));
        } else if (is_ascii_digit(section_type[0])) {
            data_item.section->type = FetchCommand::DataItem::SectionType::Parts;
            data_item.section->parts = Vector<unsigned>();

            while (!consume_if("]"sv)) {
                auto num = try_parse_number();
                if (num.has_value()) {
                    data_item.section->parts->append(num.value());
                    continue;
                }
                auto atom = TRY(parse_atom());
                if (atom.equals_ignoring_ascii_case("MIME"sv)) {
                    data_item.section->ends_with_mime = true;
                    continue;
                }
            }
        } else if (section_type.equals_ignoring_ascii_case("TEXT"sv)) {
            data_item.section->type = FetchCommand::DataItem::SectionType::Text;
        } else if (section_type.equals_ignoring_ascii_case("HEADER"sv)) {
            data_item.section->type = FetchCommand::DataItem::SectionType::Header;
        } else {
            dbgln("Unmatched section type {}", section_type);
            return Error::from_string_literal("Failed to parse section type");
        }
        if (consume_if("<"sv)) {
            auto start = TRY(parse_number());
            data_item.partial_fetch = true;
            data_item.start = (int)start;
            TRY(consume(">"sv));
        }
        consume_if(" "sv);
        return data_item;
    } else if (msg_attr.equals_ignoring_ascii_case("FLAGS"sv)) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::Flags
        };
    } else if (msg_attr.equals_ignoring_ascii_case("UID"sv)) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::UID
        };
    } else if (msg_attr.equals_ignoring_ascii_case("INTERNALDATE"sv)) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::InternalDate
        };
    } else if (msg_attr.equals_ignoring_ascii_case("ENVELOPE"sv)) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::Envelope
        };
    } else if (msg_attr.equals_ignoring_ascii_case("BODY"sv) || msg_attr.equals_ignoring_ascii_case("BODYSTRUCTURE"sv)) {
        return FetchCommand::DataItem {
            .type = FetchCommand::DataItemType::BodyStructure
        };
    } else {
        dbgln("msg_attr not matched: {}", msg_attr);
        return Error::from_string_literal("Failed to parse msg_attr");
    }
}

ErrorOr<Vector<Address>> Parser::parse_address_list()
{
    if (consume_if("NIL"sv))
        return Vector<Address> {};

    auto addresses = Vector<Address>();
    TRY(consume("("sv));
    while (!consume_if(")"sv))
        addresses.append(TRY(parse_address()));
    return { addresses };
}

ErrorOr<Address> Parser::parse_address()
{
    TRY(consume("("sv));
    auto address = Address();
    auto name = TRY(parse_nstring());
    address.name = name;
    TRY(consume(" "sv));
    auto source_route = TRY(parse_nstring());
    address.source_route = source_route;
    TRY(consume(" "sv));
    auto mailbox = TRY(parse_nstring());
    address.mailbox = mailbox;
    TRY(consume(" "sv));
    auto host = TRY(parse_nstring());
    address.host = host;
    TRY(consume(")"sv));
    // [RFC-2822] group syntax is indicated by a special form of
    //          address structure in which the host name field is NIL.  If the
    //          mailbox name field is also NIL, this is an end of group marker
    //          (semi-colon in RFC 822 syntax).  If the mailbox name field is
    //          non-NIL, this is a start of group marker, and the mailbox name
    //          field holds the group name phrase.
    if (!address.mailbox.is_empty() && address.host.is_empty()) {
        // FIXME: Implement Group addresses per RFC-2822. For now, we just consume the group
        // members, and return an Address object with the group name phrase in the mailbox field.
        auto group_address = TRY(parse_address());
        while (!group_address.mailbox.is_empty() && !group_address.host.is_empty())
            group_address = TRY(parse_address());
    }
    return address;
}

ErrorOr<StringView> Parser::parse_astring()
{
    if (!at_end() && (m_buffer[m_position] == '{' || m_buffer[m_position] == '"'))
        return parse_string();

    return parse_atom();
}

ErrorOr<HashMap<ByteString, ByteString>> Parser::parse_body_fields_params()
{
    if (consume_if("NIL"sv))
        return HashMap<ByteString, ByteString> {};

    HashMap<ByteString, ByteString> fields;
    TRY(consume("("sv));
    while (!consume_if(")"sv)) {
        auto key = TRY(parse_string());
        TRY(consume(" "sv));
        auto value = TRY(parse_string());
        fields.set(key, value);
        consume_if(" "sv);
    }

    return fields;
}

ErrorOr<BodyExtension> Parser::parse_body_extension()
{
    if (consume_if("NIL"sv))
        return BodyExtension { Optional<ByteString> {} };

    if (consume_if("("sv)) {
        Vector<OwnPtr<BodyExtension>> extensions;
        while (!consume_if(")"sv)) {
            extensions.append(make<BodyExtension>(TRY(parse_body_extension())));
            consume_if(" "sv);
        }
        return BodyExtension { move(extensions) };
    }

    if (!at_end() && (m_buffer[m_position] == '"' || m_buffer[m_position] == '{'))
        return BodyExtension { { TRY(parse_string()) } };

    return BodyExtension { TRY(parse_number()) };
}

}
