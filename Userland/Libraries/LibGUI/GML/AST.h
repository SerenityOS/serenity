/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Error.h>
#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/IterationDecision.h>
#include <AK/JsonArray.h>
#include <AK/JsonValue.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/TypeCasts.h>
#include <LibGUI/GML/Lexer.h>

namespace GUI::GML {

class Comment;
class JsonValueNode;

// Base of the GML Abstract Syntax Tree (AST).
class Node : public RefCounted<Node> {
public:
    virtual ~Node() = default;
    template<typename NodeT>
    requires(IsBaseOf<Node, NodeT>) static ErrorOr<NonnullRefPtr<NodeT>> from_token(Token token)
    {
        return try_make_ref_counted<NodeT>(token.m_view);
    }

    String to_string() const
    {
        StringBuilder builder;
        format(builder, 0, false);
        return builder.to_string();
    }

    // Format this AST node with the builder at the given indentation level.
    // is_inline controls whether we are starting on a new line.
    virtual void format(StringBuilder& builder, size_t indentation, bool is_inline) const = 0;

    // FIXME: We can't change the kind of indentation right now.
    static void indent(StringBuilder& builder, size_t indentation)
    {
        for (size_t i = 0; i < indentation; ++i)
            builder.append("    ");
    }
};

// AST nodes that actually hold data and can be in a KeyValuePair.
class ValueNode
    : public Node {
public:
    virtual ~ValueNode() = default;
};

// Single line comments with //.
class Comment : public Node {
public:
    Comment(String text)
        : m_text(move(text))
    {
    }

    virtual void format(StringBuilder& builder, size_t indentation, bool is_inline) const override
    {
        if (is_inline) {
            builder.append(m_text);
        } else {
            indent(builder, indentation);
            builder.append(m_text);
        }
        builder.append("\n");
    }
    virtual ~Comment() override = default;

private:
    String m_text {};
};

// Any JSON-like key: value pair.
class KeyValuePair : public Node {
public:
    KeyValuePair(String key, NonnullRefPtr<ValueNode> value)
        : m_key(move(key))
        , m_value(move(value))
    {
    }
    virtual ~KeyValuePair() override = default;
    virtual void format(StringBuilder& builder, size_t indentation, bool is_inline) const override
    {
        if (!is_inline)
            indent(builder, indentation);
        builder.appendff("{}: ", m_key);
        m_value->format(builder, indentation, true);
        if (!is_inline)
            builder.append("\n");
    }

    String key() const { return m_key; }
    NonnullRefPtr<ValueNode> value() const { return m_value; }

private:
    String m_key;
    NonnullRefPtr<ValueNode> m_value;
};

// Just a mixin so that we can use JSON values in the AST
// FIXME: Use a specialized value type for all the possible GML property values. Right now that's all possible JSON values (?)
class JsonValueNode : public ValueNode
    , public JsonValue {

public:
    JsonValueNode(JsonValue const& value)
        : JsonValue(value)
    {
    }

    virtual void format(StringBuilder& builder, size_t indentation, bool is_inline) const override
    {
        if (!is_inline)
            indent(builder, indentation);
        if (is_array()) {
            // custom array serialization as AK's doesn't pretty-print
            // objects and arrays (we only care about arrays (for now))
            builder.append("[");
            auto first = true;
            as_array().for_each([&](auto& value) {
                if (!first)
                    builder.append(", ");
                first = false;
                value.serialize(builder);
            });
            builder.append("]");
        } else {
            serialize(builder);
        }
        if (!is_inline)
            builder.append("\n");
    }
};

// GML class declaration, starting with '@'
class Object : public ValueNode {
public:
    Object() = default;
    Object(String name, NonnullRefPtrVector<Node> properties, NonnullRefPtrVector<Node> sub_objects)
        : m_properties(move(properties))
        , m_sub_objects(move(sub_objects))
        , m_name(move(name))
    {
    }

    virtual ~Object() override = default;

    StringView name() const { return m_name; }
    void set_name(String name) { m_name = move(name); }

    ErrorOr<void> add_sub_object_child(NonnullRefPtr<Node> child)
    {
        VERIFY(is<Object>(child.ptr()) || is<Comment>(child.ptr()));
        return m_sub_objects.try_append(move(child));
    }

    ErrorOr<void> add_property_child(NonnullRefPtr<Node> child)
    {
        VERIFY(is<KeyValuePair>(child.ptr()) || is<Comment>(child.ptr()));
        return m_properties.try_append(move(child));
    }

    // Does not return key-value pair `layout: ...`!
    template<typename Callback>
    void for_each_property(Callback callback)
    {
        for (auto const& child : m_properties) {
            if (is<KeyValuePair>(child)) {
                auto const& property = static_cast<KeyValuePair const&>(child);
                if (property.key() != "layout" && is<JsonValueNode>(property.value().ptr()))
                    callback(property.key(), static_ptr_cast<JsonValueNode>(property.value()));
            }
        }
    }

    template<typename Callback>
    void for_each_child_object(Callback callback)
    {
        for (NonnullRefPtr<Node> child : m_sub_objects) {
            // doesn't capture layout as intended, as that's behind a kv-pair
            if (is<Object>(child.ptr())) {
                auto object = static_ptr_cast<Object>(child);
                callback(object);
            }
        }
    }

    // Uses IterationDecision to allow the callback to interrupt the iteration, like a for-loop break.
    template<IteratorFunction<NonnullRefPtr<Object>> Callback>
    void for_each_child_object_interruptible(Callback callback)
    {
        for (NonnullRefPtr<Node> child : m_sub_objects) {
            // doesn't capture layout as intended, as that's behind a kv-pair
            if (is<Object>(child.ptr())) {
                auto object = static_ptr_cast<Object>(child);
                if (callback(object) == IterationDecision::Break)
                    return;
            }
        }
    }

    RefPtr<Object> layout_object() const
    {
        for (NonnullRefPtr<Node> child : m_properties) {
            if (is<KeyValuePair>(child.ptr())) {
                auto property = static_ptr_cast<KeyValuePair>(child);
                if (property->key() == "layout") {
                    VERIFY(is<Object>(property->value().ptr()));
                    return static_ptr_cast<Object>(property->value());
                }
            }
        }
        return nullptr;
    }

    RefPtr<ValueNode> get_property(StringView property_name)
    {
        for (NonnullRefPtr<Node> child : m_properties) {
            if (is<KeyValuePair>(child.ptr())) {
                auto property = static_ptr_cast<KeyValuePair>(child);
                if (property->key() == property_name)
                    return property->value();
            }
        }
        return nullptr;
    }

    virtual void format(StringBuilder& builder, size_t indentation, bool is_inline) const override
    {
        if (!is_inline)
            indent(builder, indentation);
        builder.append('@');
        builder.append(m_name);
        builder.append(" {");
        if (!m_properties.is_empty() || !m_sub_objects.is_empty()) {
            builder.append('\n');

            for (auto const& property : m_properties)
                property.format(builder, indentation + 1, false);

            if (!m_properties.is_empty() && !m_sub_objects.is_empty())
                builder.append('\n');

            // This loop is necessary as we need to know what the last child is.
            for (size_t i = 0; i < m_sub_objects.size(); ++i) {
                auto const& child = m_sub_objects[i];
                child.format(builder, indentation + 1, false);

                if (is<Object>(child) && i != m_sub_objects.size() - 1)
                    builder.append('\n');
            }

            indent(builder, indentation);
        }
        builder.append('}');
        if (!is_inline)
            builder.append('\n');
    }

private:
    // Properties and comments
    NonnullRefPtrVector<Node> m_properties;
    // Sub objects and comments
    NonnullRefPtrVector<Node> m_sub_objects;
    String m_name {};
};

class GMLFile : public Node {
public:
    virtual ~GMLFile() override = default;

    ErrorOr<void> add_child(NonnullRefPtr<Node> child)
    {
        if (!has_main_class()) {
            if (is<Comment>(child.ptr())) {
                return m_leading_comments.try_append(*static_ptr_cast<Comment>(child));
            }
            if (is<Object>(child.ptr())) {
                m_main_class = static_ptr_cast<Object>(child);
                return {};
            }
            return Error::from_string_literal("Unexpected data before main class");
        }
        // After the main class, only comments are allowed.
        if (!is<Comment>(child.ptr()))
            return Error::from_string_literal("Data not allowed after main class");
        return m_trailing_comments.try_append(*static_ptr_cast<Comment>(child));
    }

    bool has_main_class() const { return m_main_class != nullptr; }

    NonnullRefPtrVector<Comment> leading_comments() const { return m_leading_comments; }
    Object& main_class()
    {
        VERIFY(!m_main_class.is_null());
        return *m_main_class.ptr();
    }
    NonnullRefPtrVector<Comment> trailing_comments() const { return m_trailing_comments; }

    virtual void format(StringBuilder& builder, size_t indentation, [[maybe_unused]] bool is_inline) const override
    {
        for (auto const& comment : m_leading_comments)
            comment.format(builder, indentation, false);

        if (!m_leading_comments.is_empty())
            builder.append('\n');
        m_main_class->format(builder, indentation, false);
        if (!m_trailing_comments.is_empty())
            builder.append('\n');

        for (auto const& comment : m_trailing_comments)
            comment.format(builder, indentation, false);
    }

private:
    NonnullRefPtrVector<Comment> m_leading_comments;
    RefPtr<Object> m_main_class;
    NonnullRefPtrVector<Comment> m_trailing_comments;
};

}
