#include <LibHTML/Element.h>
#include <LibHTML/Parser.h>
#include <LibHTML/Text.h>
#include <ctype.h>

static Retained<Element> create_element(const String& tag_name)
{
    return adopt(*new Element(tag_name));
}

static bool is_self_closing_tag(const String& tag_name)
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

Retained<Document> parse(const String& html)
{
    Vector<Retained<ParentNode>> node_stack;

    auto doc = adopt(*new Document);
    node_stack.append(doc);

    enum class State {
        Free,
        BeforeTagName,
        InTagName,
        InAttributeList,
        InAttributeName,
        InAttributeValueNoQuote,
        InAttributeValueSingleQuote,
        InAttributeValueDoubleQuote,
    };

    auto state = State::Free;

    Vector<char, 256> buffer;

    bool is_slash_tag = false;

    auto move_to_state = [&](State new_state) {
        if (new_state == State::BeforeTagName)
            is_slash_tag = false;
        if (state == State::Free && !buffer.is_empty()) {
            auto text_node = adopt(*new Text(String::copy(buffer)));
            node_stack.last()->append_child(text_node);
        }
        state = new_state;
        buffer.clear();
    };

    auto close_tag = [&] {
        if (node_stack.size() > 1)
            node_stack.take_last();
    };

    auto open_tag = [&] {
        auto new_element = create_element(String::copy(buffer));
        node_stack.append(new_element);
        if (node_stack.size() != 1)
            node_stack[node_stack.size() - 2]->append_child(new_element);

        if (is_self_closing_tag(new_element->tag_name()))
            close_tag();
    };

    for (int i = 0; i < html.length(); ++i) {
        char ch = html[i];
        switch (state) {
        case State::Free:
            if (ch == '<') {
                move_to_state(State::BeforeTagName);
                break;
            }
            buffer.append(ch);
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
            if (!isascii(ch))
                break;
            move_to_state(State::InTagName);
            [[fallthrough]];
        case State::InTagName:
            if (ch == ' ') {
                move_to_state(State::InAttributeList);
                break;
            }
            if (ch == '>') {
                if (is_slash_tag)
                    close_tag();
                else
                    open_tag();
                move_to_state(State::Free);
                break;
            }
            buffer.append(ch);
            break;
        }
    }
    return doc;
}
