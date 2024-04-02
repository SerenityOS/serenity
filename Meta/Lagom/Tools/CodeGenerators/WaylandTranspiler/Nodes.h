/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ArgumentType.h"
#include "Utils.h"
#include <LibXML/Parser/Parser.h>

namespace Wayland {

class NodeCopyright {
public:
    static ErrorOr<NonnullOwnPtr<NodeCopyright>> parse(const XML::Node::Element& element)
    {
        auto const& text_node = element.children.first();
        VERIFY(text_node->is_text());

        auto* self = new NodeCopyright(text_node->as_text().builder.string_view());
        return adopt_own(*self);
    }

private:
    NodeCopyright(StringView text)
        : m_text(text)
    {
    }

    StringView m_text;
};

class NodeDescription {
public:
    static ErrorOr<NonnullOwnPtr<NodeDescription>> parse(const XML::Node::Element& element)
    {
        auto const& summary = element.attributes.get("summary");
        VERIFY(summary.has_value());

        auto* self = new NodeDescription(summary.value());

        if (element.children.size() > 0) {
            auto const& text_node = element.children.first();
            VERIFY(text_node->is_text());
            VERIFY(element.children.size() == 1);
            self->m_text = text_node->as_text().builder.string_view();
        }

        return AK::adopt_nonnull_own_or_enomem(self);
    }

private:
    NodeDescription(ByteString summary)
        : m_summary(move(summary))
    {
    }

    ByteString m_summary;
    Optional<StringView> m_text;
};

class NodeArg {
private:
    Optional<ByteString> m_name;
    NonnullOwnPtr<ArgumentType> m_type;
    Optional<ByteString> m_summary;

    NodeArg(Optional<ByteString> name, NonnullOwnPtr<ArgumentType> type, Optional<ByteString> summary)
        : m_name(move(name))
        , m_type(move(type))
        , m_summary(move(summary))
    {
    }

public:
    Optional<ByteString> name() const
    {
        return m_name;
    }

    ArgumentType& type() const
    {
        return *m_type;
    }

    static ErrorOr<NonnullOwnPtr<NodeArg>> parse(const XML::Node::Element& element)
    {

        auto const& name = element.attributes.get("name");
        auto const& type = element.attributes.get("type");
        auto const& summary = element.attributes.get("summary");
        auto const& interface = element.attributes.get("interface");
        auto const& enum_name = element.attributes.get("enum");

        NonnullOwnPtr<ArgumentType> arg_type = [&] {
            if (enum_name.has_value()) {
                if (type == "uint") {
                    return ArgumentType::create_enum(enum_name->view(), false);
                }
                if (type == "int") {
                    return ArgumentType::create_enum(enum_name->view(), true);
                }
                VERIFY_NOT_REACHED();
            } else if (type == "uint") {
                return make<ArgumentType>(PrimitiveType::UnsignedInteger);
            } else if (type == "int") {
                return make<ArgumentType>(PrimitiveType::Integer);
            } else if (type == "fixed") {
                return make<ArgumentType>(PrimitiveType::Fixed);
            } else if (type == "object") {
                return ArgumentType::create_object(interface);
            } else if (type == "new_id") {
                return ArgumentType::create_new_id(interface);
            } else if (type == "fd") {
                return ArgumentType::create_file_descriptor();
            } else if (type == "string") {
                return ArgumentType::create_string();
            } else if (type == "array") {
                return ArgumentType::create_array();
            } else {
                VERIFY_NOT_REACHED();
            }
        }();

        auto* self = new NodeArg(name, move(arg_type), summary);

        auto allow_null_string = element.attributes.get("allow-null");
        if (allow_null_string.has_value()) {
            VERIFY(self->m_type->nullable_type());

            bool nullable = optional_boolean_string_to_bool(allow_null_string);
            self->m_type->set_nullable(nullable);
        };

        return AK::adopt_nonnull_own_or_enomem(self);
    }
};

class NodeMethod {
private:
    ByteString m_name;
    Optional<ByteString> m_type;
    Optional<ByteString> m_since;

    Optional<NonnullOwnPtr<NodeDescription>> m_description;
    Vector<NonnullOwnPtr<NodeArg>> m_args;

    NodeMethod(ByteString name, Optional<ByteString> type, Optional<ByteString> since)
        : m_name(move(name))
        , m_type(move(type))
        , m_since(move(since))
    {
    }

public:
    StringView name() const
    {
        return m_name.view();
    }

    Vector<NonnullOwnPtr<NodeArg>>& args()
    {
        return m_args;
    }

    static ErrorOr<NonnullOwnPtr<NodeMethod>> parse(const XML::Node::Element& element)
    {
        auto const& name = element.attributes.get("name");
        if (!name.has_value()) {
            return Error::from_string_literal("A method should have a `name` attribute.");
        }

        auto const& type = element.attributes.get("type");

        auto const& since = element.attributes.get("since");

        auto* self = new NodeMethod(name.value(), type, since);

        for (auto const& iter : element.children) {
            if (!iter->is_element()) {
                continue;
            }

            auto const& element = iter->as_element();

            if (element.name == "arg") {
                self->m_args.append(TRY(NodeArg::parse(element)));
            } else if (element.name == "description") {
                if (self->m_description.has_value()) {
                    return Error::from_string_literal("A method shouldn't have multiple `description` elements");
                }
                self->m_description = TRY(NodeDescription::parse(element));
            } else {
                VERIFY_NOT_REACHED();
            }
        }

        return adopt_nonnull_own_or_enomem(self);
    }
};

class NodeEnumEntry {
    friend class NodeEnum;

private:
    ByteString m_name;
    ByteString m_value;

    Optional<ByteString> m_summary;
    Optional<ByteString> m_since;

    NodeEnumEntry(ByteString name, ByteString value, Optional<ByteString> summary, Optional<ByteString> since)
        : m_name(move(name))
        , m_value(move(value))
        , m_summary(move(summary))
        , m_since(move(since))
    {
    }

public:
    StringView name() const
    {
        return m_name.view();
    }

    StringView value() const
    {
        return m_value.view();
    }
};

class NodeEnum {
private:
    ByteString m_name;
    Optional<ByteString> m_since;
    bool m_bitfield;

    Optional<NonnullOwnPtr<NodeDescription>> m_description;
    Vector<NonnullOwnPtr<NodeEnumEntry>> m_entries;

    NodeEnum(ByteString name, Optional<ByteString> since, bool bitfield)
        : m_name(move(name))
        , m_since(move(since))
        , m_bitfield(bitfield)
    {
    }

public:
    StringView name() const
    {
        return m_name;
    }

    Vector<NonnullOwnPtr<NodeEnumEntry>>& entries()
    {
        return m_entries;
    }

    static ErrorOr<NonnullOwnPtr<NodeEnum>> parse(const XML::Node::Element& element)
    {
        auto const& name = element.attributes.get("name").value();

        auto const& since = element.attributes.get("since");

        auto const& bitfield = element.attributes.get("bitfield");

        auto* self = new NodeEnum(name, since, optional_boolean_string_to_bool(bitfield));

        for (auto const& iter : element.children) {
            if (!iter->is_element()) {
                continue;
            }

            auto const& child = iter->as_element();

            if (child.name == "entry") {
                self->m_entries.append(MUST(adopt_nonnull_own_or_enomem(new NodeEnumEntry {
                    child.attributes.get("name").value(),
                    child.attributes.get("value").value(),
                    child.attributes.get("summary"),
                    child.attributes.get("since"),
                })));
            } else if (child.name == "description") {
                VERIFY(!self->m_description.has_value());
                self->m_description = TRY(NodeDescription::parse(child));
            }
        }

        return adopt_nonnull_own_or_enomem(self);
    }
};

class NodeInterface {
public:
    NodeInterface(ByteString name, ByteString version)
        : m_name(move(name))
        , m_version(move(version))
    {
        m_c_name = to_code_name(m_name).to_byte_string();

        {
            StringBuilder b;
            b.append(c_name());
            b.append("Interface"sv);

            m_interface_struct_c_name = b.to_byte_string();
        }
    }

    static ErrorOr<NonnullOwnPtr<NodeInterface>> parse(const XML::Node::Element& element)
    {
        auto name = element.attributes.get("name");
        if (!name.has_value()) {
            return Error::from_string_literal("An interface should have a `name` attribute");
        }

        auto version = element.attributes.get("version");
        if (!name.has_value()) {
            return Error::from_string_literal("An interface should have a `version` attribute");
        }

        auto* interface = new NodeInterface(name.value(), version.value());

        for (auto const& iter : element.children) {
            if (!(*iter).is_element()) {
                continue;
            }

            auto const& element = (*iter).as_element();
            if (element.name == "request") {
                TRY(interface->add_request(element));
            } else if (element.name == "event") {
                TRY(interface->add_event(element));
            } else if (element.name == "enum") {
                TRY(interface->add_enum(element));
            } else if (element.name == "description") {
                TRY(interface->add_description(element));
            } else {
                VERIFY_NOT_REACHED();
            }
        }

        return adopt_nonnull_own_or_enomem(interface);
    }

    ErrorOr<void> add_description(const XML::Node::Element& element)
    {
        if (m_description.has_value()) {
            return Error::from_string_literal("An interface shouldn't have multiple `description` elements");
        }
        add_description(TRY(NodeDescription::parse(element)));

        return {};
    }

    void add_description(NonnullOwnPtr<NodeDescription> desc)
    {
        VERIFY(!m_description.has_value());
        m_description = move(desc);
    }

    ErrorOr<void> add_request(const XML::Node::Element& element)
    {
        add_request(TRY(NodeMethod::parse(element)));

        return {};
    }

    void add_request(NonnullOwnPtr<NodeMethod> request)
    {
        m_requests.append(move(request));
    }

    ErrorOr<void> add_event(const XML::Node::Element& element)
    {
        add_event(TRY(NodeMethod::parse(element)));

        return {};
    }

    void add_event(NonnullOwnPtr<NodeMethod> event)
    {
        m_events.append(move(event));
    }

    ErrorOr<void> add_enum(const XML::Node::Element& element)
    {
        add_enum(TRY(NodeEnum::parse(element)));

        return {};
    }

    void add_enum(NonnullOwnPtr<NodeEnum> _enum)
    {
        m_enums.append(move(_enum));
    }

    Vector<NonnullOwnPtr<NodeMethod>>& requests()
    {
        return m_requests;
    }

    Vector<NonnullOwnPtr<NodeMethod>>& events()
    {
        return m_events;
    }

    Vector<NonnullOwnPtr<NodeEnum>>& enums()
    {
        return m_enums;
    }

    StringView name() const
    {
        return m_name.view();
    }

    StringView version() const
    {
        return m_version.view();
    }

    StringView c_name() const
    {
        return m_c_name->view();
    }

    StringView interface_struct_c_name() const
    {
        return m_interface_struct_c_name->view();
    }

private:
    ByteString m_name;
    ByteString m_version;

    Optional<NonnullOwnPtr<NodeDescription>> m_description;

    Vector<NonnullOwnPtr<NodeMethod>> m_requests;
    Vector<NonnullOwnPtr<NodeMethod>> m_events;
    Vector<NonnullOwnPtr<NodeEnum>> m_enums;

    Optional<ByteString> m_c_name;
    Optional<ByteString> m_interface_struct_c_name;
};

class NodeProtocol {
    ByteString m_name;
    Optional<NonnullOwnPtr<NodeCopyright>> m_copyright;
    Optional<NonnullOwnPtr<NodeDescription>> m_description;
    Vector<NonnullOwnPtr<NodeInterface>> m_interfaces;

    NodeProtocol(ByteString name)
        : m_name(move(name))
    {
    }

public:
    StringView name() const
    {
        return m_name.view();
    }

    Vector<NonnullOwnPtr<NodeInterface>>& interfaces()
    {
        return m_interfaces;
    }

    static ErrorOr<NonnullOwnPtr<NodeProtocol>> parse(const XML::Document& document)
    {
        VERIFY(document.root().is_element());
        auto const& root = document.root().as_element();
        VERIFY(root.name == "protocol");

        auto* self = new NodeProtocol(root.attributes.get("name").value());
        // TODO: Make failing validation nicer: Printing where the error exactly occured (line, column, snippet)
        for (auto const& iter : root.children) {
            // Ignore comments and texts in <protocol> (text is not even in the spec)
            if (!iter->is_element()) {
                continue;
            }

            auto const& element = iter->as_element();
            if (element.name == "copyright") {
                VERIFY(!self->m_copyright.has_value());
                self->m_copyright = TRY(NodeCopyright::parse(element));
            } else if (element.name == "description") {
                VERIFY(!self->m_description.has_value());
                self->m_description = TRY(NodeDescription::parse(element));

            } else if (element.name == "interface") {
                self->m_interfaces.append(TRY(NodeInterface::parse(element)));
            } else {
                VERIFY_NOT_REACHED();
            }
        }

        return adopt_nonnull_own_or_enomem(self);
    }
};

}
