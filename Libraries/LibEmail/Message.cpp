/*
 * Copyright (c) 2020, Cole Blakley <cblakley15@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <LibEmail/Message.h>
#include <ctype.h>
#include <stdlib.h>

namespace LibEmail {

// Some helper functions
static Optional<Field> parse_message_field(StringView);
static Optional<SystemFlag> parse_system_flag(StringView);
static String parse_address(StringView);

Message Message::create_from_imap_data(StringView source)
{
    enum class State : char {
        Start,
        InCommand,
        InString,
        InNumber,
        InSet,
        InBodyText
    };

    dbg() << "Msg before parsing: " << source << '\n';

    Message result;
    StringBuilder curr_value;
    State curr_state = State::Start;
    Vector<char> set_stack;

    Field curr_field_name{};
    if (!source.is_empty() && source[0] == '(')
        source = source.substring_view(1, source.length());
    for (size_t i = 0; i < source.length(); ++i) {
        switch (curr_state) {
        case State::Start:
            if (isalpha(source[i])) {
                curr_state = State::InCommand;
                --i;
            } else if (curr_field_name == Field::BodyText) {
                curr_state = State::InBodyText;
            } else if (source[i] == '"') {
                curr_state = State::InString;
            } else if (isdigit(source[i])) {
                curr_state = State::InNumber;
                --i;
            } else if (source[i] == '(') {
                set_stack.append('(');
                curr_state = State::InSet;
            }
            break;
        case State::InCommand: {
            if (source[i] != ' ') {
                curr_value.append(source[i]);
                break;
            }
            const auto message_field = parse_message_field(curr_value.string_view());
            if (message_field.has_value()) {
                curr_field_name = message_field.value();
            }
            curr_value.clear();
            curr_state = State::Start;
            break;
        }
        case State::InString: {
            if (source[i] != '"') {
                curr_value.append(source[i]);
                break;
            }
            switch (curr_field_name) {
            case Field::InternalDate:
                result.m_internal_date = curr_value.to_string();
                break;
            default:
                // No other fields use strings.
                break;
            }
            curr_value.clear();
            curr_state = State::Start;
            break;
        }
        case State::InNumber: {
            if (source[i] != ' ') {
                curr_value.append(source[i]);
                break;
            }
            switch (curr_field_name) {
            case Field::UID: {
                bool ok = false;
                result.m_uid = AK::StringUtils::convert_to_uint(curr_value.string_view(), ok);
                if (!ok) {
                    dbg() << "UID field: " << curr_value.string_view()
                          << " can't be formatted as a number";
                }
                break;
            }
            default:
                // No other fields use numbers.
                break;
            }
            curr_value.clear();
            curr_state = State::Start;
            break;
        }
        case State::InSet: {
            if (!set_stack.is_empty()) {
                if (source[i] == '(')
                    set_stack.append('(');
                else if (source[i] == ')')
                    set_stack.take_last();
                curr_value.append(source[i]);
                break;
            }
            // End of set
            switch (curr_field_name) {
            case Field::Envelope:
                result.m_envelope = Envelope::create_from_imap_data(curr_value.string_view());
                break;
            case Field::Flags:
                // Will set the m_system_flags and m_keyword_flags fields.
                result.load_flags(curr_value.string_view());
                break;
            default:
                // No other fields use sets.
                break;
            }

            curr_value.clear();
            set_stack.clear();
            curr_state = State::Start;
            break;
        }
        case State::InBodyText: {
            if (source[i] != ')') {
                curr_value.append(source[i]);
                break;
            }
            if (curr_field_name == Field::BodyText && !curr_value.is_empty()) {
                StringView curr_value_view = curr_value.string_view();
                for (unsigned int i = 0; i < curr_value_view.length(); ++i) {
                    if (curr_value_view[i] == '\n') {
                        result.m_text = curr_value_view.substring_view(i + 2,
                            curr_value.length());
                        break;
                    }
                }
            }
            curr_value.clear();
            curr_state = State::Start;
            break;
        }
        }
    }
    return result;
}

Message::~Message()
{
}

void Message::load_flags(StringView flag_list)
{
    enum class State : char {
        Start,
        InFlag
    };

    State curr_state = State::Start;
    StringBuilder curr_flag;
    for (size_t i = 0; i < flag_list.length(); ++i) {
        switch (curr_state) {
        case State::Start:
            if (isalpha(flag_list[i]) || flag_list[i] == '\\') {
                curr_state = State::InFlag;
                --i;
            }
            break;
        case State::InFlag:
            if (flag_list[i] != ' ') {
                curr_flag.append(flag_list[i]);
            } else {
                String curr_flag_value{ curr_flag.to_string().to_uppercase() };
                // First, check for the pre-defined system flags.
                if (!curr_flag_value.is_empty() && curr_flag_value[0] == '\\') {
                    const auto flag = parse_system_flag(curr_flag_value.view());
                    if (flag.has_value())
                        m_system_flags.append(flag.value());
                } else {
                    // If not one of the pre-defined flags, then it is
                    // probably a user-defined flag (called a "keyword").
                    m_keyword_flags.append(curr_flag.to_string());
                }
                curr_flag.clear();
                curr_state = State::Start;
            }
            break;
        }
    }

    if (!curr_flag.is_empty()) {
        String curr_flag_value{ curr_flag.to_string().to_uppercase() };
        // First, check for the pre-defined system flags.
        if (!curr_flag_value.is_empty() && curr_flag_value[0] == '\\') {
            const auto flag = parse_system_flag(curr_flag_value.view());
            if (flag.has_value())
                m_system_flags.append(flag.value());
        } else {
            // If not one of the pre-defined flags, then it is
            // probably a user-defined flag (called a "keyword").
            m_keyword_flags.append(curr_flag.to_string());
        }
        curr_flag.clear();
        curr_state = State::Start;
    }
}

Envelope Envelope::create_from_imap_data(StringView source)
{
    enum class State : char {
        Start,
        InNil,
        InString,
        InSet
    };

    dbg() << "Envelope before parsing: " << source << '\n';

    Envelope result;
    Vector<String> fields;

    StringBuilder curr_field;
    State curr_state = State::Start;
    for (size_t i = 0; i < source.length(); ++i) {
        switch (curr_state) {
        case State::Start:
            if (source[i] == '"') {
                curr_state = State::InString;
            } else if (source[i] == '(') {
                curr_state = State::InSet;
            } else if (isalpha(source[i])) {
                --i; // Put back the letter
                curr_state = State::InNil;
            }
            break;
        case State::InNil:
            if (source[i] != ' ') {
                curr_field.append(source[i]);
            } else {
                // End of NIL
                fields.append(curr_field.to_string());
                curr_field.clear();
                curr_state = State::Start;
            }
            break;
        case State::InString:
            if (source[i] != '"') {
                curr_field.append(source[i]);
            } else {
                // End of string
                fields.append(curr_field.to_string());
                curr_field.clear();
                curr_state = State::Start;
            }
            break;
        case State::InSet:
            if (source[i] != '(' && source[i] != ')') {
                curr_field.append(source[i]);
            } else if (source[i] == ')') {
                // End of set
                fields.append(curr_field.to_string());
                curr_field.clear();
                curr_state = State::Start;
            }
            break;
        }
    }

    if (!curr_field.is_empty()) {
        fields.append(curr_field.to_string());
    }

    // Try to set as many fields as possible from the parsed envelope.
    if (fields.is_empty())
        return result;
    else if (fields[0] != "NIL")
        result.m_date = fields[0];

    if (fields.size() < 2)
        return result;
    else if (fields[1] != "NIL")
        result.m_subject = fields[1];

    if (fields.size() < 3)
        return result;
    else if (fields[2] != "NIL")
        result.m_from = parse_address(fields[2]);

    if (fields.size() >= 6 && fields[5] != "NIL")
        result.m_to = parse_address(fields[5]);
    return result;
}

Envelope::~Envelope()
{
}

static Optional<Field> parse_message_field(StringView value)
{
    if (value == "INTERNALDATE") {
        return { Field::InternalDate };
    } else if (value == "FLAGS") {
        return { Field::Flags };
    } else if (value == "UID") {
        return { Field::UID };
    } else if (value == "ENVELOPE") {
        return { Field::Envelope };
    } else if (value == "BODY[TEXT]") {
        return { Field::BodyText };
    } else {
        return {};
    }
}

static Optional<SystemFlag> parse_system_flag(StringView value)
{
    if (value == "\\Seen") {
        return { SystemFlag::Seen };
    } else if (value == "\\Answered") {
        return { SystemFlag::Answered };
    } else if (value == "\\Flagged") {
        return { SystemFlag::Flagged };
    } else if (value == "\\Deleted") {
        return { SystemFlag::Deleted };
    } else if (value == "\\Draft") {
        return { SystemFlag::Draft };
    } else if (value == "\\Recent") {
        return { SystemFlag::Recent };
    } else {
        return {};
    }
}

static String parse_address(StringView source)
{
    enum class State : char {
        Start,
        InString,
        InNil
    };

    // Addresses have form: "(personal-name source-route email-username hostname)".
    Vector<String> fields;

    State curr_state = State::Start;
    StringBuilder curr_value;
    for (size_t i = 0; i < source.length(); ++i) {
        switch (curr_state) {
        case State::Start:
            if (source[i] == '"') {
                curr_state = State::InString;
            } else if (isalpha(source[i])) {
                --i; // Put back the letter
                curr_state = State::InNil;
            }
            break;
        case State::InString:
            if (source[i] != '"') {
                curr_value.append(source[i]);
            } else {
                fields.append(curr_value.to_string());
                curr_value.clear();
                curr_state = State::Start;
            }
            break;
        case State::InNil:
            if (source[i] != ' ') {
                curr_value.append(source[i]);
            } else {
                fields.append(curr_value.to_string());
                curr_value.clear();
                curr_state = State::Start;
            }
            break;
        }
    }

    if (!curr_value.is_empty()) {
        fields.append(curr_value.to_string());
    }

    // Rewrite the address into form: "name <user@hostname>".
    StringBuilder result;
    if (fields.is_empty())
        return result.to_string();
    else if (fields[0] != "NIL")
        result.append(fields[0]);

    if (fields.size() < 3)
        return result.to_string();
    else if (fields[2] != "NIL") {
        result.append(" <");
        result.append(fields[2]);
    }

    if (fields.size() < 4) {
        result.append(">");
        return result.to_string();
    } else if (fields[3] != "NIL") {
        result.append("@");
        result.append(fields[3]);
        result.append(">");
    }

    return result.to_string();
}

}
