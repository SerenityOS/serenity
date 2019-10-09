#include <AK/Function.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/StringBuilder.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOM/HTMLAnchorElement.h>
#include <LibHTML/DOM/HTMLBodyElement.h>
#include <LibHTML/DOM/HTMLFontElement.h>
#include <LibHTML/DOM/HTMLHRElement.h>
#include <LibHTML/DOM/HTMLHeadElement.h>
#include <LibHTML/DOM/HTMLHeadingElement.h>
#include <LibHTML/DOM/HTMLHtmlElement.h>
#include <LibHTML/DOM/HTMLImageElement.h>
#include <LibHTML/DOM/HTMLStyleElement.h>
#include <LibHTML/DOM/HTMLTitleElement.h>
#include <LibHTML/DOM/HTMLLinkElement.h>
#include <LibHTML/DOM/Text.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <ctype.h>
#include <stdio.h>

static NonnullRefPtr<Element> create_element(Document& document, const String& tag_name)
{
    auto lowercase_tag_name = tag_name.to_lowercase();
    if (lowercase_tag_name == "a")
        return adopt(*new HTMLAnchorElement(document, tag_name));
    if (lowercase_tag_name == "html")
        return adopt(*new HTMLHtmlElement(document, tag_name));
    if (lowercase_tag_name == "head")
        return adopt(*new HTMLHeadElement(document, tag_name));
    if (lowercase_tag_name == "body")
        return adopt(*new HTMLBodyElement(document, tag_name));
    if (lowercase_tag_name == "font")
        return adopt(*new HTMLFontElement(document, tag_name));
    if (lowercase_tag_name == "hr")
        return adopt(*new HTMLHRElement(document, tag_name));
    if (lowercase_tag_name == "style")
        return adopt(*new HTMLStyleElement(document, tag_name));
    if (lowercase_tag_name == "title")
        return adopt(*new HTMLTitleElement(document, tag_name));
    if (lowercase_tag_name == "link")
        return adopt(*new HTMLLinkElement(document, tag_name));
    if (lowercase_tag_name == "img")
        return adopt(*new HTMLImageElement(document, tag_name));
    if (lowercase_tag_name == "h1"
        || lowercase_tag_name == "h2"
        || lowercase_tag_name == "h3"
        || lowercase_tag_name == "h4"
        || lowercase_tag_name == "h5"
        || lowercase_tag_name == "h6") {
        return adopt(*new HTMLHeadingElement(document, tag_name));
    }
    return adopt(*new Element(document, tag_name));
}

static bool is_valid_in_attribute_name(char ch)
{
    return isalnum(ch) || ch == '_' || ch == '-';
}

static bool is_self_closing_tag(const StringView& tag_name)
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

NonnullRefPtr<Document> parse_html(const StringView& html, const URL& url)
{
    NonnullRefPtrVector<ParentNode> node_stack;

    auto document = adopt(*new Document);
    document->set_url(url);
    node_stack.append(document);

    enum class State {
        Free = 0,
        BeforeTagName,
        InTagName,
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

    auto move_to_state = [&](State new_state) {
        if (new_state == State::BeforeTagName) {
            is_slash_tag = false;
            tag_name_buffer.clear();
            attributes.clear();
        }
        if (new_state == State::InAttributeName)
            attribute_name_buffer.clear();
        if (new_state == State::BeforeAttributeValue)
            attribute_value_buffer.clear();
        if (state == State::Free && !text_buffer.string_view().is_empty()) {
            auto text_node = adopt(*new Text(document, text_buffer.to_string()));
            node_stack.last().append_child(text_node, false);
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
        if (node_stack.size() != 1)
            node_stack[node_stack.size() - 2].append_child(new_element, false);

        if (is_self_closing_tag(new_element->tag_name()))
            close_tag();
    };

    auto commit_tag = [&] {
        if (is_slash_tag)
            close_tag();
        else
            open_tag();
    };

    auto commit_attribute = [&] {
        attributes.append({ String::copy(attribute_name_buffer), String::copy(attribute_value_buffer) });
    };

    for (int i = 0; i < html.length(); ++i) {
        char ch = html[i];
        switch (state) {
        case State::Free:
            if (ch == '<') {
                is_slash_tag = false;
                move_to_state(State::BeforeTagName);
                break;
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
                    { "&amp;", "&" }
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
                if (!found)
                    dbg() << "Unhandled escape sequence";
            }
            break;
        case State::BeforeTagName:
            if (ch == '/') {
                is_slash_tag = true;
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
            if (ch == '>') {
                commit_tag();
                move_to_state(State::Free);
                break;
            }
            tag_name_buffer.append(ch);
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
        case State::InAttributeValueNoQuote:
            if (isspace(ch)) {
                commit_attribute();
                move_to_state(State::InAttributeList);
                break;
            }
            if (ch == '>') {
                commit_tag();
                move_to_state(State::Free);
                break;
            }
            attribute_value_buffer.append(ch);
            break;
        default:
            fprintf(stderr, "Unhandled state %d\n", (int)state);
            ASSERT_NOT_REACHED();
        }
    }

    Function<void(Node&)> fire_insertion_callbacks = [&](Node& node) {
        for (auto* child = node.first_child(); child; child = child->next_sibling()) {
            fire_insertion_callbacks(*child);
        }
        if (node.parent())
            node.inserted_into(*node.parent());
    };

    fire_insertion_callbacks(*document);

    document->fixup();

    return document;
}
