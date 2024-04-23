/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Concepts.h>
#include <AK/Error.h>
#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonValue.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
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

    ByteString to_byte_string() const
    {
        StringBuilder builder;
        format(builder, 0, false);
        return builder.to_byte_string();
    }

    // Format this AST node with the builder at the given indentation level.
    // is_inline controls whether we are starting on a new line.
    virtual void format(StringBuilder& builder, size_t indentation, bool is_inline) const = 0;

    // FIXME: We can't change the kind of indentation right now.
    static void indent(StringBuilder& builder, size_t indentation)
    {
        for (size_t i = 0; i < indentation; ++i)
            builder.append("    "sv);
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
    Comment(ByteString text)
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
        builder.append('\n');
    }
    virtual ~Comment() override = default;

private:
    ByteString m_text {};
};

// Any JSON-like key: value pair.
class KeyValuePair : public Node {
public:
    KeyValuePair(ByteString key, NonnullRefPtr<ValueNode> value)
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
            builder.append('\n');
    }

    ByteString key() const { return m_key; }
    NonnullRefPtr<ValueNode> value() const { return m_value; }

private:
    ByteString m_key;
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
            builder.append('[');
            auto first = true;
            as_array().for_each([&](auto& value) {
                if (!first)
                    builder.append(", "sv);
                first = false;
                value.serialize(builder);
            });
            builder.append(']');
        } else {
            serialize(builder);
        }
        if (!is_inline)
            builder.append('\n');
    }
};

// GML class declaration, starting with '@'
class Object : public ValueNode {
public:
    Object() = default;
    Object(ByteString name, Vector<NonnullRefPtr<Node const>> properties, Vector<NonnullRefPtr<Node const>> sub_objects)
        : m_properties(move(properties))
        , m_sub_objects(move(sub_objects))
        , m_name(move(name))
    {
    }

    virtual ~Object() override = default;

    StringView name() const { return m_name; }
    void set_name(ByteString name) { m_name = move(name); }

    ErrorOr<void> add_sub_object_child(NonnullRefPtr<Node const> child)
    {
        VERIFY(is<Object>(child.ptr()) || is<Comment>(child.ptr()));
        return m_sub_objects.try_append(move(child));
    }

    ErrorOr<void> add_property_child(NonnullRefPtr<Node const> child)
    {
        VERIFY(is<KeyValuePair>(child.ptr()) || is<Comment>(child.ptr()));
        return m_properties.try_append(move(child));
    }

    // Does not return key-value pair `layout: ...`!
    template<typename Callback>
    void for_each_property(Callback callback) const
    {
        for (auto const& child : m_properties) {
            if (is<KeyValuePair>(child)) {
                auto const& property = static_cast<KeyValuePair const&>(*child);
                if (property.key() != "layout" && is<JsonValueNode>(property.value().ptr()))
                    callback(property.key(), static_ptr_cast<JsonValueNode>(property.value()));
            }
        }
    }

    template<typename Callback>
    void for_each_object_property(Callback callback) const
    {
        for (auto const& child : m_properties) {
            if (is<KeyValuePair>(child)) {
                auto const& property = static_cast<KeyValuePair const&>(*child);
                if (is<Object>(property.value().ptr()))
                    callback(property.key(), static_ptr_cast<Object>(property.value()));
            }
        }
    }

    template<FallibleFunction<StringView, NonnullRefPtr<JsonValueNode>> Callback>
    ErrorOr<void> try_for_each_property(Callback callback) const
    {
        for (auto const& child : m_properties) {
            if (is<KeyValuePair>(child)) {
                auto const& property = static_cast<KeyValuePair const&>(*child);
                if (property.key() != "layout" && is<JsonValueNode>(property.value().ptr()))
                    TRY(callback(property.key(), static_ptr_cast<JsonValueNode>(property.value())));
            }
        }
        return {};
    }

    template<FallibleFunction<StringView, NonnullRefPtr<Object>> Callback>
    ErrorOr<void> try_for_each_object_property(Callback callback) const
    {
        for (auto const& child : m_properties) {
            if (is<KeyValuePair>(child)) {
                auto const& property = static_cast<KeyValuePair const&>(*child);
                if (is<Object>(property.value().ptr()))
                    TRY(callback(property.key(), static_ptr_cast<Object>(property.value())));
            }
        }
        return {};
    }

    template<typename Callback>
    void for_each_child_object(Callback callback) const
    {
        for (NonnullRefPtr<Node const> child : m_sub_objects) {
            // doesn't capture layout as intended, as that's behind a kv-pair
            if (is<Object>(child.ptr())) {
                auto object = static_ptr_cast<Object const>(child);
                callback(object);
            }
        }
    }

    template<FallibleFunction<NonnullRefPtr<Object>> Callback>
    ErrorOr<void> try_for_each_child_object(Callback callback) const
    {
        for (auto const& child : m_sub_objects) {
            // doesn't capture layout as intended, as that's behind a kv-pair
            if (is<Object>(child)) {
                TRY(callback(static_cast<Object const&>(*child)));
            }
        }

        return {};
    }

    RefPtr<Object const> layout_object() const
    {
        for (auto const& child : m_properties) {
            if (is<KeyValuePair>(child)) {
                auto const& property = static_cast<KeyValuePair const&>(*child);
                if (property.key() == "layout") {
                    VERIFY(is<Object>(property.value().ptr()));
                    return static_cast<Object const&>(*property.value());
                }
            }
        }
        return nullptr;
    }

    RefPtr<ValueNode const> get_property(StringView property_name) const
    {
        for (auto const& child : m_properties) {
            if (is<KeyValuePair>(child)) {
                auto const& property = static_cast<KeyValuePair const&>(*child);
                if (property.key() == property_name)
                    return property.value();
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
        builder.append(" {"sv);
        if (!m_properties.is_empty() || !m_sub_objects.is_empty()) {
            builder.append('\n');

            for (auto const& property : m_properties)
                property->format(builder, indentation + 1, false);

            if (!m_properties.is_empty() && !m_sub_objects.is_empty())
                builder.append('\n');

            // This loop is necessary as we need to know what the last child is.
            for (size_t i = 0; i < m_sub_objects.size(); ++i) {
                auto const& child = m_sub_objects[i];
                child->format(builder, indentation + 1, false);

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
    Vector<NonnullRefPtr<Node const>> m_properties;
    // Sub objects and comments
    Vector<NonnullRefPtr<Node const>> m_sub_objects;
    ByteString m_name {};
};

class GMLFile : public Node {
public:
    virtual ~GMLFile() override = default;

    ErrorOr<void> add_child(NonnullRefPtr<Node const> child)
    {
        if (!has_main_class()) {
            if (is<Comment>(child.ptr())) {
                return m_leading_comments.try_append(*static_ptr_cast<Comment const>(child));
            }
            if (is<Object>(child.ptr())) {
                m_main_class = static_ptr_cast<Object const>(child);
                return {};
            }
            return Error::from_string_literal("Unexpected data before main class");
        }
        // After the main class, only comments are allowed.
        if (!is<Comment>(child.ptr()))
            return Error::from_string_literal("Data not allowed after main class");
        return m_trailing_comments.try_append(*static_ptr_cast<Comment const>(child));
    }

    bool has_main_class() const { return m_main_class != nullptr; }

    Vector<NonnullRefPtr<Comment const>> leading_comments() const { return m_leading_comments; }
    Object const& main_class() const
    {
        VERIFY(!m_main_class.is_null());
        return *m_main_class.ptr();
    }
    Vector<NonnullRefPtr<Comment const>> trailing_comments() const { return m_trailing_comments; }

    virtual void format(StringBuilder& builder, size_t indentation, [[maybe_unused]] bool is_inline) const override
    {
        for (auto const& comment : m_leading_comments)
            comment->format(builder, indentation, false);

        if (!m_leading_comments.is_empty())
            builder.append('\n');
        m_main_class->format(builder, indentation, false);
        if (!m_trailing_comments.is_empty())
            builder.append('\n');

        for (auto const& comment : m_trailing_comments)
            comment->format(builder, indentation, false);
    }

private:
    Vector<NonnullRefPtr<Comment const>> m_leading_comments;
    RefPtr<Object const> m_main_class;
    Vector<NonnullRefPtr<Comment const>> m_trailing_comments;
};

}
