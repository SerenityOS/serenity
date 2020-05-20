/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Function.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Parser/HTMLParser.h>
#include <ctype.h>
#include <stdio.h>

namespace Web {

static bool is_valid_in_attribute_name(char ch)
{
    return isalnum(ch) || ch == '_' || ch == '-';
}

static bool is_void_element(const StringView& tag_name)
{
    return tag_name == "area"
        || tag_name == "base"
        || tag_name == "br"
        || tag_name == "col"
        || tag_name == "embed"
        || tag_name == "hr"
        || tag_name == "img"
        || tag_name == "input"
        || tag_name == "link"
        || tag_name == "meta"
        || tag_name == "param"
        || tag_name == "source"
        || tag_name == "track"
        || tag_name == "wbr";
}

static Vector<char> codepoint_to_bytes(const u32 codepoint)
{
    Vector<char, 0> bytes;

    if (codepoint < 0x80) {
        bytes.insert(0, (char)codepoint);
    } else if (codepoint < 0x800) {
        char b2 = (codepoint & 0x3F) + 0x80;
        char b1 = ((codepoint >> 6) & 0x1F) + +0xC0;

        bytes.insert(0, b1);
        bytes.insert(1, b2);
    }

    return bytes;
}

static bool parse_html_document(const StringView& html, Document& document, ParentNode& root)
{
    NonnullRefPtrVector<ParentNode> node_stack;
    node_stack.append(root);

    enum class State {
        Free = 0,
        BeforeTagName,
        InTagName,
        InDoctype,
        InComment,
        InAttributeList,
        InAttributeName,
        BeforeAttributeValue,
        InAttributeValueNoQuote,
        InAttributeValueSingleQuote,
        InAttributeValueDoubleQuote,
    };

    auto state = State::Free;

    StringBuilder text_buffer;

    Vector<char, 32> tag_name_buffer;

    Vector<Attribute> attributes;
    Vector<char, 256> attribute_name_buffer;
    Vector<char, 256> attribute_value_buffer;

    bool is_slash_tag = false;
    bool is_exclamation_tag = false;

    auto commit_text_node = [&] {
        auto text_node = adopt(*new Text(document, text_buffer.to_string()));
        node_stack.last().append_child(text_node);
        text_buffer.clear();
    };

    auto move_to_state = [&](State new_state) {
        if (new_state == State::BeforeTagName) {
            is_slash_tag = false;
            is_exclamation_tag = false;
            tag_name_buffer.clear();
            attributes.clear();
        }
        if (new_state == State::InAttributeName)
            attribute_name_buffer.clear();
        if (new_state == State::BeforeAttributeValue)
            attribute_value_buffer.clear();
        if (state == State::Free && !text_buffer.is_empty()) {
            commit_text_node();
        }
        state = new_state;
        text_buffer.clear();
    };

    auto close_tag = [&] {
        if (node_stack.size() > 1)
            node_stack.take_last();
    };

    auto open_tag = [&] {
        auto new_element = create_element(document, String::copy(tag_name_buffer));
        tag_name_buffer.clear();
        new_element->set_attributes(move(attributes));
        node_stack.append(new_element);
        if (node_stack.size() != 1) {
            node_stack[node_stack.size() - 2].append_child(new_element);
        }

        if (is_void_element(new_element->tag_name()))
            close_tag();
    };

    auto commit_doctype = [&] {
        node_stack.last().append_child(adopt(*new DocumentType(document)));
    };

    auto commit_comment = [&] {
        node_stack.last().append_child(adopt(*new Comment(document, text_buffer.to_string())));
    };

    auto commit_tag = [&] {
        if (is_slash_tag)
            close_tag();
        else
            open_tag();
    };

    auto commit_attribute = [&] {
        if (!attribute_name_buffer.is_empty()) {
            auto name = String::copy(attribute_name_buffer);
            String value;
            if (attribute_value_buffer.is_empty())
                value = String::empty();
            else
                value = String::copy(attribute_value_buffer);
            attributes.empend(name, value);
        }
    };

    for (size_t i = 0; i < html.length(); ++i) {
        auto peek = [&](size_t offset) -> char {
            if (i + offset >= html.length())
                return '\0';
            return html[i + offset];
        };
        char ch = html[i];
        switch (state) {
        case State::Free:
            if (ch == '<') {
                bool should_treat_as_text = false;
                if (node_stack.last().tag_name() == "script") {
                    bool is_script_close_tag = peek(1) == '/'
                        && tolower(peek(2)) == 's'
                        && tolower(peek(3)) == 'c'
                        && tolower(peek(4)) == 'r'
                        && tolower(peek(5)) == 'i'
                        && tolower(peek(6)) == 'p'
                        && tolower(peek(7)) == 't'
                        && tolower(peek(8)) == '>';
                    if (!is_script_close_tag)
                        should_treat_as_text = true;
                }
                if (!should_treat_as_text) {
                    is_slash_tag = false;
                    move_to_state(State::BeforeTagName);
                    break;
                }
            }

            if (ch != '&') {
                text_buffer.append(ch);
            } else {
                struct Escape {
                    const char* code;
                    const char* value;
                };
                static Escape escapes[] = {
                    { "&lt;", "<" },
                    { "&gt;", ">" },
                    { "&amp;", "&" },
                    { "&mdash;", "-" },
                    { "&nbsp;", " " }, // FIXME: Should actually be *non-breaking*
                    { "&raquo;", ">>" },
                    { "&laquo;", "<<" },
                };
                auto rest_of_html = html.substring_view(i, html.length() - i);
                bool found = false;

                for (auto& escape : escapes) {
                    if (rest_of_html.starts_with(escape.code)) {
                        text_buffer.append(escape.value);
                        found = true;
                        i += strlen(escape.code) - 1;
                        break;
                    }
                }

                if (!found) {
                    char num_sign = html[i + 1];
                    if (num_sign && num_sign == '#') {
                        int j = 2; // spip '&#' and search for ';'
                        while (html[i + j] != ';' && j < 7) {
                            j++;
                        }

                        if (j < 7) { // We found ; char
                            bool ok;
                            u32 codepoint;
                            String str_code_point = html.substring_view(i + 2, j - 2);
                            if (str_code_point.starts_with('x')) {
                                String str = str_code_point.substring(1, str_code_point.length() - 1);
                                codepoint = AK::StringUtils::convert_to_uint_from_hex(str, ok);
                            } else {
                                codepoint = str_code_point.to_uint(ok);
                            }

                            if (ok) {
                                Vector<char> bytes = codepoint_to_bytes(codepoint);
                                if (bytes.size() > 0) {
                                    for (size_t i = 0; i < bytes.size(); i++) {
                                        text_buffer.append(bytes.at(i));
                                    }
                                    found = true;
                                    i = i + j;
                                }
                            }
                        }
                    }
                }

                if (!found) {
                    dbg() << "Unhandled escape sequence:" << html.substring_view(i, min((size_t)5, html.length()));
                }
            }
            break;
        case State::BeforeTagName:
            if (ch == '/') {
                is_slash_tag = true;
                break;
            }
            if (ch == '!') {
                if (toupper(peek(1)) == 'D'
                    && toupper(peek(2)) == 'O'
                    && toupper(peek(3)) == 'C'
                    && toupper(peek(4)) == 'T'
                    && toupper(peek(5)) == 'Y'
                    && toupper(peek(6)) == 'P'
                    && toupper(peek(7)) == 'E') {
                    i += 7;
                    move_to_state(State::InDoctype);
                    break;
                }
                if (peek(1) == '-' && peek(2) == '-') {
                    i += 2;
                    move_to_state(State::InComment);
                    break;
                }
                break;
            }
            if (ch == '>') {
                move_to_state(State::Free);
                break;
            }
            if (!isalpha(ch))
                break;
            move_to_state(State::InTagName);
            [[fallthrough]];
        case State::InTagName:
            if (isspace(ch)) {
                move_to_state(State::InAttributeList);
                break;
            }
            if (ch == '/' && peek(1) == '>') {
                open_tag();
                close_tag();
                i += 1;
                move_to_state(State::Free);
                break;
            }
            if (ch == '>') {
                commit_tag();
                move_to_state(State::Free);
                break;
            }
            tag_name_buffer.append(ch);
            break;
        case State::InDoctype:
            if (ch == '>') {
                commit_doctype();
                move_to_state(State::Free);
                break;
            }
            break;
        case State::InComment:
            if (ch == '-' && peek(1) == '-' && peek(2) == '>') {
                commit_comment();
                i += 2;
                move_to_state(State::Free);
                break;
            }
            text_buffer.append(ch);
            break;
        case State::InAttributeList:
            if (ch == '>') {
                commit_tag();
                move_to_state(State::Free);
                break;
            }
            if (!isalpha(ch))
                break;
            move_to_state(State::InAttributeName);
            [[fallthrough]];
        case State::InAttributeName:
            if (is_valid_in_attribute_name(ch)) {
                attribute_name_buffer.append(ch);
                break;
            }
            if (isspace(ch)) {
                commit_attribute();
                break;
            }

            if (ch == '>') {
                commit_attribute();
                commit_tag();
                move_to_state(State::Free);
                break;
            }

            if (ch == '=') {
                move_to_state(State::BeforeAttributeValue);
                break;
            }
            break;
        case State::BeforeAttributeValue:
            if (ch == '\'') {
                move_to_state(State::InAttributeValueSingleQuote);
                break;
            }
            if (ch == '"') {
                move_to_state(State::InAttributeValueDoubleQuote);
                break;
            }
            if (ch == '>') {
                commit_tag();
                move_to_state(State::Free);
                break;
            }
            if (isspace(ch)) {
                commit_attribute();
                move_to_state(State::InAttributeList);
                break;
            }
            move_to_state(State::InAttributeValueNoQuote);
            [[fallthrough]];
        case State::InAttributeValueNoQuote:
            if (isspace(ch)) {
                commit_attribute();
                move_to_state(State::InAttributeList);
                break;
            }
            if (ch == '>') {
                commit_attribute();
                commit_tag();
                move_to_state(State::Free);
                break;
            }
            attribute_value_buffer.append(ch);
            break;
        case State::InAttributeValueSingleQuote:
            if (ch == '\'') {
                commit_attribute();
                move_to_state(State::InAttributeList);
                break;
            }
            attribute_value_buffer.append(ch);
            break;
        case State::InAttributeValueDoubleQuote:
            if (ch == '"') {
                commit_attribute();
                move_to_state(State::InAttributeList);
                break;
            }
            attribute_value_buffer.append(ch);
            break;
        default:
            fprintf(stderr, "Unhandled state %d\n", (int)state);
            ASSERT_NOT_REACHED();
        }
    }

    if (!text_buffer.is_empty())
        commit_text_node();

    return true;
}

String to_utf8(const StringView& input, const String& encoding)
{
    auto* decoder = TextCodec::decoder_for(encoding);
    ASSERT(decoder);
    return decoder->to_utf8(input);
}

RefPtr<DocumentFragment> parse_html_fragment(Document& document, const StringView& raw_html, const String& encoding)
{
    auto fragment = adopt(*new DocumentFragment(document));
    if (!parse_html_document(to_utf8(raw_html, encoding), document, *fragment))
        return nullptr;
    return fragment;
}

RefPtr<Document> parse_html_document(const StringView& raw_html, const URL& url, const String& encoding)
{
    String html = to_utf8(raw_html, encoding);

    auto document = adopt(*new Document(url));
    document->set_source(html);

    if (!parse_html_document(html, *document, *document))
        return nullptr;

    document->fixup();

#if 0
    Function<void(Node&)> fire_insertion_callbacks = [&](Node& node) {
        for (auto* child = node.first_child(); child; child = child->next_sibling()) {
            fire_insertion_callbacks(*child);
        }
        if (node.parent())
            node.inserted_into(*node.parent());
    };
    fire_insertion_callbacks(document);
#endif

    document->dispatch_event(Event::create("DOMContentLoaded"));

    return document;
}
}
