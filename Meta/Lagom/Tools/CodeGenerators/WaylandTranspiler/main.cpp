#include <AK/Format.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <LibXML/Parser/Parser.h>

// Note: this doesn't yet forward declare interfaces, which aren't in the same xml file.

namespace Wayland {
extern StringView wayland_dtd_xml;

static StringView parse_copyright(const XML::Node::Element& element)
{
    auto const& text_node = element.children.first();
    VERIFY(text_node->is_text());

    return text_node->as_text().builder.string_view();
}

class NodeDescription {
public:
    static ErrorOr<NodeDescription> parse(const XML::Node::Element& element)
    {
        auto const& summary = element.attributes.get("summary");
        VERIFY(summary.has_value());

        auto self = NodeDescription(summary.value());

        if (element.children.size() > 0) {
            auto const& text_node = element.children.first();
            VERIFY(text_node->is_text());
            VERIFY(element.children.size() == 1);
            self.m_text = text_node->as_text().builder.string_view();
        }

        return self;
    }

private:
    NodeDescription(ByteString summary)
        : m_summary(move(summary))
    {
    }

    ByteString m_summary;
    Optional<StringView> m_text;
};

enum ArgTypePrimitive {
    UnsignedInteger,
    Integer,
    Fixed,
};

enum ArgTypeEnum {
    Primitive,
    Array,
    Enum,
    String,
    Object,
    NewId,
    FileDescriptor,
};

static StringBuilder to_code_name(StringView name)
{
    StringBuilder builder;
    auto vec = name.split_view('_');

    for (auto iter = vec.begin(); !iter.is_end(); ++iter) {
        auto split = (*iter);
        if (split == "wl")
            continue;
        builder.append(split.to_titlecase_string());
    }

    return builder;
}

class ArgType {
public:
    ArgType(ArgTypePrimitive type)
        : m_type(ArgTypeEnum::Primitive)
        , m_primitive(type)
        , m_nullable(false)
    {
        m_type = ArgTypeEnum::Primitive;
    }

    static ArgType* create_enum(StringView enum_name, bool signed_integer)
    {
        auto* self = new ArgType(Enum);
        self->m_type_name = enum_name;
        self->m_primitive = signed_integer ? ArgTypePrimitive::Integer : ArgTypePrimitive::UnsignedInteger;
        return self;
    }

    static ArgType* create_new_id(Optional<ByteString> const& interface)
    {
        auto* self = new ArgType(NewId);
        if (interface.has_value()) {
            self->m_type_name = interface.value();
        }
        return self;
    }

    static ArgType* create_object(Optional<ByteString> const& interface)
    {
        auto* self = new ArgType(Object);
        if (interface.has_value()) {
            self->m_type_name = interface.value();
        }
        return self;
    }

    static ArgType* create_file_descriptor()
    {
        auto* self = new ArgType(FileDescriptor);
        return self;
    }

    static ArgType* create_string()
    {
        return new ArgType(String);
    }

    static ArgType* create_array()
    {
        return new ArgType(Array);
    }

    bool nullable_type() const
    {
        return m_type == String || m_type == Object;
    }

    bool interface_type() const
    {
        return m_type == Object || m_type == NewId;
    }

    void set_nullable(bool value)
    {
        VERIFY(nullable_type());
        m_nullable = value;
    }

    bool nullable() const
    {
        VERIFY(nullable_type());
        return m_nullable;
    }

    bool is_primitive() const
    {
        return m_type == Primitive;
    }

    bool is_signed_integer() const
    {
        return m_primitive.has_value() && m_primitive.value() == Integer;
    }

    bool is_unsigned_integer() const
    {
        return m_primitive.has_value() && m_primitive.value() == UnsignedInteger;
    }

    bool is_new_id() const
    {
        return m_type == NewId;
    }

    bool is_enum() const
    {
        return m_type == Enum;
    }

    bool is_object() const
    {
        return m_type == Object;
    }

    bool has_type_name() const
    {
        return m_type_name.has_value();
    }

    ByteString& type_name()
    {
        return m_type_name.value();
    }

    bool can_reference()
    {
        return m_type == Array || m_type == Object || m_type == NewId || (m_type == String && !m_nullable);
    }

    ByteString get_binding_symbol()
    {
        if (is_primitive()) {
            switch (m_primitive.value()) {
            case UnsignedInteger:
                return "uint32_t"sv;
            case Integer:
                return "int32_t"sv;
            case Fixed:
                return "FixedFloat"sv;
            }
        } else if (m_type == FileDescriptor) {
            return "int"sv;
        } else if (m_type == Array) {
            return "ByteBuffer"sv;
        } else if (m_type == String) {
            if (m_nullable) {
                return "Optional<ByteString>"sv;
            }
            return "ByteString"sv;
        } else if (m_type == Object || m_type == NewId) {
            if (m_type_name.has_value()) {
                auto code_name = to_code_name(m_type_name.value());

                return code_name.to_byte_string();
            }

            // should only really happen on registry.bind, hopefully
            return "Object"sv;
        } else if (m_type == Enum) {
            // The interface that contains this enum can be inferred implcitly
            // (the parent interface) or referenced directly (seperation by '.' in m_type_name);
            // We don't have that information here yet, so handle it in the CodeGenerator.

            // auto code_name = to_code_name(m_type_name.value());
            // return code_name.to_byte_string();
            VERIFY_NOT_REACHED();
        }

        VERIFY_NOT_REACHED();
    }

    ByteString get_resolved_argument_caster()
    {
        ByteString value;

        switch (m_type) {

        case Primitive:
            switch (m_primitive.value()) {
            case UnsignedInteger:
                value = "as_unsigned";
                break;
            case Integer:
                value = "as_signed";
                break;
            case Fixed:
                value = "as_fixed";
                break;
            }
            break;
        case Array:
            value = "as_buffer";
            break;
        case Enum:
            switch (m_primitive.value()) {
            case UnsignedInteger:
                value = "as_unsigned";
                break;
            case Integer:
                value = "as_signed";
                break;
            default:
                VERIFY_NOT_REACHED();
            };
            break;
        case String:
            if (m_nullable) {
                value = "as_opt_string";
            } else {
                value = "as_string";
            }
            break;
        case Object:
        case NewId:
            if (m_nullable) {
                value = ByteString::formatted("as_opt_object<{}>", get_binding_symbol());
            } else {
                value = ByteString::formatted("as_object<{}>", get_binding_symbol());
            }
            break;
        case FileDescriptor:
            value = "as_fd";
            break;
        }
        return value;
    }

    ByteString get_wire_argument_type()
    {
        ByteString value;

        switch (m_type) {
        case Primitive:
            switch (m_primitive.value()) {
            case UnsignedInteger:
                value = "UnsignedInteger";
                break;
            case Integer:
                value = "Integer";
                break;
            case Fixed:
                value = "FixedFloat";
                break;
            }
            break;
        case Array:
            value = "Array";
            break;
        case Enum:
            switch (m_primitive.value()) {
            case UnsignedInteger:
                value = "UnsignedInteger";
                break;
            case Integer:
                value = "Integer";
                break;
            default:
                VERIFY_NOT_REACHED();
            };
            break;
        case String:
            value = "String";
            break;
        case Object:
            value = "Object";
            break;
        case NewId:
            value = "NewId";
            break;
        case FileDescriptor:
            value = "FileDescriptor";
            break;
        }
        return value;
    }

    ArgTypeEnum type() const
    {
        return m_type;
    }

private:
    ArgType(ArgTypeEnum type)
        : m_type(type)
    {
    }

    ArgTypeEnum m_type;
    Optional<ArgTypePrimitive> m_primitive;
    Optional<ByteString> m_type_name;

    bool m_nullable;
};

static bool optional_boolean_string_to_bool(Optional<ByteString> bs, bool value = false)
{
    if (bs.has_value()) {
        auto const& string = bs.value();
        if (string == "true") {
            value = true;
        } else {
            VERIFY(string == "false");
            value = false;
        }
    }
    return value;
}

struct NodeArg {
    Optional<ByteString> name;
    ArgType* type;

    Optional<ByteString> summary;

    static ErrorOr<struct NodeArg> parse(const XML::Node::Element& element)
    {
        auto const& name = element.attributes.get("name");
        auto const& type = element.attributes.get("type");
        auto const& summary = element.attributes.get("summary");
        auto const& interface = element.attributes.get("interface");
        auto const& enum_name = element.attributes.get("enum");

        NodeArg self;

        self = NodeArg {
            .name = name,
            .type = nullptr,
            .summary = summary,
        };

        if (enum_name.has_value()) {
            if (type == "uint") {
                self.type = ArgType::create_enum(enum_name->view(), false);
            } else if (type == "int") {
                self.type = ArgType::create_enum(enum_name->view(), true);
            } else {
                VERIFY_NOT_REACHED();
            }
        } else if (type == "uint") {
            self.type = new ArgType(ArgTypePrimitive::UnsignedInteger);
        } else if (type == "int") {
            self.type = new ArgType(ArgTypePrimitive::Integer);
        } else if (type == "fixed") {
            self.type = new ArgType(ArgTypePrimitive::Fixed);
        } else if (type == "object") {
            self.type = ArgType::create_object(interface);
        } else if (type == "new_id") {
            self.type = ArgType::create_new_id(interface);
        } else if (type == "fd") {
            self.type = ArgType::create_file_descriptor();
        } else if (type == "string") {
            self.type = ArgType::create_string();
        } else if (type == "array") {
            self.type = ArgType::create_array();
        } else {
            VERIFY_NOT_REACHED();
        }

        VERIFY(self.type != nullptr);

        auto allow_null_string = element.attributes.get("allow-null");
        if (allow_null_string.has_value()) {
            VERIFY(self.type->nullable_type());

            bool nullable = optional_boolean_string_to_bool(allow_null_string);
            self.type->set_nullable(nullable);
        };

        return self;
    }
};

struct NodeMethod {
    ByteString name;
    Optional<ByteString> type;
    Optional<ByteString> since;

    Optional<NodeDescription> description;
    Vector<struct NodeArg> args;

    static ErrorOr<struct NodeMethod> parse(const XML::Node::Element& element)
    {
        auto const& name = element.attributes.get("name");
        if (!name.has_value()) {
            return Error::from_string_literal("A method should have a `name` attribute.");
        }

        auto const& type = element.attributes.get("type");

        auto const& since = element.attributes.get("since");

        auto method = NodeMethod {
            .name = name.value(),
            .type = type,
            .since = since,
            .description = {},
            .args = {},
        };

        for (auto const& iter : element.children) {
            if (!iter->is_element()) {
                continue;
            }

            auto const& element = iter->as_element();

            if (element.name == "arg") {
                method.args.append(TRY(NodeArg::parse(element)));
            } else if (element.name == "description") {
                if (method.description.has_value()) {
                    return Error::from_string_literal("A method shouldn't have multiple `description` elements");
                }
                method.description = TRY(NodeDescription::parse(element));
            } else {
                VERIFY_NOT_REACHED();
            }
        }

        return method;
    }
};

struct NodeEnumEntry {
    ByteString name;
    ByteString value;

    Optional<ByteString> summary;
    Optional<ByteString> since;
};

struct NodeEnum {
    ByteString name;
    Optional<ByteString> since;
    bool bitfield;

    Optional<NodeDescription> description;
    Vector<struct NodeEnumEntry> entries;

    static ErrorOr<struct NodeEnum> parse(const XML::Node::Element& element)
    {
        auto const& name = element.attributes.get("name").value();

        auto const& since = element.attributes.get("since");

        auto const& bitfield = element.attributes.get("bitfield");

        auto _enum = NodeEnum {
            .name = name,
            .since = since,
            .bitfield = optional_boolean_string_to_bool(bitfield),
            .description = {},
            .entries = {},
        };

        for (auto const& iter : element.children) {
            if (!iter->is_element()) {
                continue;
            }

            auto const& child = iter->as_element();

            if (child.name == "entry") {
                _enum.entries.append(NodeEnumEntry {
                    .name = child.attributes.get("name").value(),
                    .value = child.attributes.get("value").value(),
                    .summary = child.attributes.get("summary"),
                    .since = child.attributes.get("since"),
                });
            } else if (child.name == "description") {
                VERIFY(!_enum.description.has_value());
                _enum.description = TRY(NodeDescription::parse(child));
            }
        }

        return _enum;
    }
};

static ByteString titlecase_with_split(ByteString const& string, char seperator = '_')
{
    StringBuilder builder;
    auto parts = string.split(seperator);
    for (auto& part : parts) {
        builder.append(part.to_titlecase());
    }

    return builder.to_byte_string();
}

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

    static ErrorOr<NodeInterface*> parse(const XML::Node::Element& element)
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

        return interface;
    }

    ErrorOr<void> add_description(const XML::Node::Element& element)
    {
        if (m_description.has_value()) {
            return Error::from_string_literal("An interface shouldn't have multiple `description` elements");
        }
        add_description(TRY(NodeDescription::parse(element)));

        return {};
    }

    void add_description(NodeDescription desc)
    {
        VERIFY(!m_description.has_value());
        m_description = move(desc);
    }

    ErrorOr<void> add_request(const XML::Node::Element& element)
    {
        add_request(TRY(NodeMethod::parse(element)));

        return {};
    }

    void add_request(struct NodeMethod request)
    {
        m_requests.append(request);
    }

    ErrorOr<void> add_event(const XML::Node::Element& element)
    {
        add_event(TRY(NodeMethod::parse(element)));

        return {};
    }

    void add_event(struct NodeMethod event)
    {
        m_events.append(event);
    }

    ErrorOr<void> add_enum(const XML::Node::Element& element)
    {
        add_enum(TRY(NodeEnum::parse(element)));

        return {};
    }

    void add_enum(struct NodeEnum _enum)
    {
        m_enums.append(_enum);
    }

    Vector<struct NodeMethod> requests()
    {
        return m_requests;
    }

    Vector<struct NodeMethod> events()
    {
        return m_events;
    }

    Vector<struct NodeEnum> enums()
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

    Optional<NodeDescription> m_description;

    Vector<struct NodeMethod> m_requests;
    Vector<struct NodeMethod> m_events;
    Vector<struct NodeEnum> m_enums;

    Optional<ByteString> m_c_name;
    Optional<ByteString> m_interface_struct_c_name;
};

struct NodeProtocol {
    ByteString name;
    Optional<ByteString> copyright;
    Optional<NodeDescription> description;
    Vector<NodeInterface*> interfaces;

    static ErrorOr<struct NodeProtocol> parse(const XML::Document& document)
    {
        VERIFY(document.root().is_element());
        auto const& root = document.root().as_element();
        VERIFY(root.name == "protocol");

        auto self = NodeProtocol {
            .name = root.attributes.get("name").value(),
            .copyright = {},
            .description = {},
            .interfaces = {},
        };

        // TODO: Make failing validation nicer: Printing where the error exactly occured (line, column, snippet)
        for (auto const& iter : root.children) {
            // Ignore comments and texts in <protocol> (text is not even in the spec)
            if (!iter->is_element()) {
                continue;
            }

            auto const& element = iter->as_element();
            if (element.name == "copyright") {
                VERIFY(!self.copyright.has_value());
                self.copyright = parse_copyright(element);
            } else if (element.name == "description") {
                VERIFY(!self.description.has_value());
                self.description = TRY(NodeDescription::parse(element));

            } else if (element.name == "interface") {
                self.interfaces.append(TRY(NodeInterface::parse(element)));
            } else {
                VERIFY_NOT_REACHED();
            }
        }

        return self;
    }
};

class CodeGenerator {
private:
    struct {
        StringBuilder initial;
        StringBuilder forward;

        Vector<StringBuilder> interfaces;

        StringBuilder end;
    } header;

    struct {
        StringBuilder initial;

        StringBuilder signal_enums;
        StringBuilder interfaces;

        StringBuilder end;
    } header_private;

    struct {
        StringBuilder initial;
        Vector<StringBuilder> interfaces;
        Vector<StringBuilder> functions;

        StringBuilder end;
    } code;
    struct NodeProtocol m_protocol;

    static void indent(StringBuilder& builder, size_t times = 1)
    {
        for (; times > 0; --times)
            builder.append("    "sv);
    }

    static void ln(StringBuilder& builder, StringView line = ""sv)
    {
        builder.append(line);
        builder.append('\n');
    }

    static void ln(StringBuilder& builder, char character)
    {
        builder.append(character);
        builder.append('\n');
    }

    template<typename... Parameters>
    static void lnf(StringBuilder& builder, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        StringBuilder fmt_builder;
        AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::No, Parameters...> variadic_format_params { parameters... };
        MUST(vformat(fmt_builder, fmtstr.view(), variadic_format_params));

        ln(builder, fmt_builder.string_view());
    }

    static bool is_char_number(char c)
    {
        return c >= '0' && c <= '9';
    }

    static ByteString prefix_when_starting_with_number(StringView view)
    {
        VERIFY(view.length() > 0);

        char val = view.characters_without_null_termination()[0];

        if (is_char_number(val)) {
            StringBuilder b;
            b.append('_');
            b.append(view);
            return b.to_byte_string();
        }

        return view.to_byte_string();
    }

    ByteString _enum_c_name(ByteString const& interface, ByteString const& name)
    {
        StringBuilder builder;
        builder.append(to_code_name(interface).to_byte_string());
        builder.append(titlecase_with_split(name));

        return builder.to_byte_string();
    }

    ByteString enum_c_name(NodeInterface const& interface, ByteString const& name)
    {
        StringBuilder builder;
        builder.append(interface.c_name());
        builder.append(titlecase_with_split(name));

        return builder.to_byte_string();
    }

    static void add_enum(StringBuilder& builder, ByteString const& enum_name, Function<bool(StringBuilder&)> function, bool unscoped = false)
    {
        lnf(builder, "enum{} {} {}", unscoped ? "" : " class", enum_name, "{");

        while (function(builder)) { }

        builder.append("};\n\n"sv);
    }

    static void add_enum_value(StringBuilder& builder, StringView value_name, StringView value_data)
    {
        lnf(builder, "    {} = {},", prefix_when_starting_with_number(titlecase_with_split(value_name)), value_data);
    }

    static void add_enum_value_default(StringBuilder& builder, StringView value_name)
    {
        builder.append("    "sv);
        builder.append(prefix_when_starting_with_number(titlecase_with_split(value_name)));
        builder.append(",\n"sv);
    }

    ByteString handle_enum_arg(const struct NodeArg& arg, NodeInterface const& interface)
    {
        auto const& name = arg.type->type_name();
        ByteString translated_name;
        if (name.contains('.')) {
            auto splits = name.split_view('.');
            VERIFY(splits.size() == 2);
            translated_name = _enum_c_name(splits[0].to_byte_string(), splits[1].to_byte_string());
        } else {
            translated_name = enum_c_name(interface, name);
        }

        return translated_name;
    }

    // If we have a new_id, we have to do some special handling.
    enum class ImplementationMode {
        Default,
        NewIdTemplate,
        NewIdSubmit
    };

    void add_request_implementation(StringBuilder& builder, const struct NodeMethod& request, NodeInterface const& interface, ByteString& returns, StringView args_string, Optional<ByteString> new_object, ImplementationMode mode = ImplementationMode::Default)
    {
        if (mode == ImplementationMode::NewIdTemplate) {
            lnf(builder, "template<class A>");
            lnf(builder, "{} {}({})", returns, request.name, args_string);
        } else if (mode == ImplementationMode::NewIdSubmit) {
            lnf(builder, "void {}::submit_{}({})", interface.c_name(), request.name, args_string);
        } else {
            lnf(builder, "{} {}::{}({})", returns, interface.c_name(), request.name, args_string);
        }
        ln(builder, '{');

        if (new_object.has_value()) {
            indent(builder);
            lnf(builder, "auto new_object = m_connection.make_object_own_id<{}>();", new_object.value());
            indent(builder);
            lnf(builder, "uint32_t new_object_id = new_object->id();");
            ln(builder);
        }

        if (mode == ImplementationMode::NewIdTemplate) {
            indent(builder);
            lnf(builder, "submit_{}(name, A::name(), A::version(), new_object_id);", request.name);
        } else {
            indent(builder);
            lnf(builder, "Vector<NonnullOwnPtr<ResolvedArgument>> args;");

            size_t length = request.args.size();
            auto add_append_arg = [&](ByteString& arg_name, ByteString& second_param) {
                indent(builder);
                lnf(builder, "args.append(make<ResolvedArgument>(&{}, {}));", arg_name, second_param);
                indent(builder);
                lnf(builder, "warnln(\"{}: {{}}\", {});", arg_name, second_param);
            };
            for (size_t idx = 0; idx < length; ++idx) {
                auto const& arg = request.args.at(idx);
                auto const& a = arg.name;
                auto arg_i = ByteString::formatted("{}", idx);
                auto arg_name = ByteString::formatted("{}Request{}{}", interface.c_name(), titlecase_with_split(request.name), titlecase_with_split(a.value_or(arg_i)));
                ByteString second_param;

                if (arg.type->is_new_id() && mode == ImplementationMode::NewIdSubmit) {
                    auto arg_name_name = ByteString::formatted("{}Request{}{}", interface.c_name(), titlecase_with_split(request.name), "InterfaceName");
                    auto variable_name = "interface_name"sv.to_byte_string();
                    add_append_arg(arg_name_name, variable_name);
                    auto arg_name_version = ByteString::formatted("{}Request{}{}", interface.c_name(), titlecase_with_split(request.name), "InterfaceVersion");
                    auto variable_version = "interface_version"sv.to_byte_string();
                    add_append_arg(arg_name_version, variable_version);
                }
                if (arg.type->is_new_id()) {
                    second_param = "new_object_id";
                } else {
                    second_param = arg.name.value();
                    if (arg.type->is_object()) {
                        second_param = ByteString::formatted("{}.id()", second_param);
                    }
                    if (arg.type->is_enum()) {
                        second_param = ByteString::formatted("static_cast<uint32_t>({})", second_param);
                    }
                }
                add_append_arg(arg_name, second_param);
            }
            if (length > 0) {
                ln(builder);
            }

            indent(builder);
            auto message_constructor = ByteString::formatted("make<MessageOutgoing>(this->id(), static_cast<uint32_t>({}::{}), AK::move(args))"sv, enum_c_name(interface, "Request"), prefix_when_starting_with_number(titlecase_with_split(request.name)));
            lnf(builder, "this->m_connection.submit_message({});", message_constructor);
        }

        if (new_object.has_value()) {
            indent(builder);
            lnf(builder, "return new_object;");
        }

        ln(builder, '}');
    }

    void add_request_to_class(StringBuilder& builder_public, StringBuilder& builder_private, const struct NodeMethod& request, NodeInterface const& interface, ImplementationMode mode = ImplementationMode::Default)
    {
        Optional<ByteString> new_object;
        Vector<ByteString> args;
        bool new_id = false;

        for (auto const& arg : request.args) {
            StringBuilder arg_builder;

            if (arg.type->is_new_id() && mode == ImplementationMode::Default) {
                VERIFY(!new_object.has_value());
                VERIFY(!new_id);
                new_id = true;
                if (arg.type->has_type_name()) {
                    new_object = arg.type->get_binding_symbol();
                }
                continue;
            }
            if (arg.type->is_new_id() && mode == ImplementationMode::NewIdSubmit) {
                args.append("ByteString interface_name");
                args.append("uint32_t interface_version");
                args.append("uint32_t new_object_id");
                continue;
            }

            if (arg.type->is_enum()) {
                arg_builder.append(handle_enum_arg(arg, interface));
            } else {
                arg_builder.append(arg.type->get_binding_symbol());
                if (arg.type->can_reference()) {
                    arg_builder.append('&');
                }
            }

            arg_builder.append(' ');
            arg_builder.append(arg.name->view());
            args.append(arg_builder.to_byte_string());
        }

        ByteString returns = new_object.has_value() ? ByteString::formatted("NonnullRefPtr<{}>", new_object.value()) : "void";

        StringBuilder args_builder;
        args_builder.join(',', args, " {}"sv);
        auto args_string = args_builder.string_view().trim_whitespace();

        if (mode == ImplementationMode::NewIdSubmit) {
            lnf(builder_private, "void submit_{}({});", request.name, args_string);

            StringBuilder request_function;
            add_request_implementation(request_function, request, interface, returns, args_string, new_object, mode);
            code.functions.append(request_function);

            return;
        }

        if (new_id && !new_object.has_value()) {
            mode = ImplementationMode::NewIdTemplate;
            indent(builder_public);
            returns = "NonnullRefPtr<A>";
            new_object = "A";
            add_request_implementation(builder_public, request, interface, returns, args_string, new_object, mode);

            mode = ImplementationMode::NewIdSubmit;
            add_request_to_class(builder_public, builder_private, request, interface, mode);

        } else {
            // void name(args...)
            indent(builder_public);
            lnf(builder_public, "{} {}({});", returns, request.name, args_string);

            StringBuilder request_function;
            add_request_implementation(request_function, request, interface, returns, args_string, new_object);
            code.functions.append(request_function);
        }
    }

    ByteString event_args(const struct NodeMethod& event, NodeInterface const& interface)
    {
        Vector<ByteString> args;

        for (auto const& arg : event.args) {
            StringBuilder arg_builder;

            if (arg.type->is_enum()) {
                arg_builder.append(handle_enum_arg(arg, interface));
            } else if (arg.type->is_new_id() || arg.type->is_object()) {
                StringView format;
                if (arg.type->nullable_type() && arg.type->nullable()) {
                    format = "RefPtr<{}>"sv;
                } else {
                    format = "NonnullRefPtr<{}>"sv;
                }
                auto string = ByteString::formatted(format, arg.type->get_binding_symbol());
                arg_builder.append(string);
            } else {
                arg_builder.append(arg.type->get_binding_symbol());
            }

            arg_builder.append(' ');
            arg_builder.append(arg.name->view());
            args.append(arg_builder.to_byte_string());
        }

        StringBuilder args_builder;
        args_builder.join(',', args, " {}"sv);
        return args_builder.string_view().trim_whitespace();
    }

    void add_event_to_class_public(StringBuilder& builder, const struct NodeMethod& event, NodeInterface const& interface)
    {
        // Function<void()>
        // name: on_$EVENTNAME
        // Args can be a lot (Aaaa)

        indent(builder);
        lnf(builder, "Function<void({})> on_{};", event_args(event, interface), event.name);
    }

    void add_event_to_code(const struct NodeMethod& event, NodeInterface const& interface)
    {
        StringBuilder builder;

        Vector<ByteString> args;
        {
            size_t i = 0;
            for (auto const& arg : event.args) {
                StringBuilder b;

                bool should_cast = arg.type->has_type_name() && !(arg.type->is_object() || arg.type->is_new_id());

                if (arg.type->is_new_id()) {
                    b.appendff("{}_{}", arg.name.value(), i);
                } else {
                    // cast
                    if (should_cast) {
                        StringBuilder code_name_builder;
                        if (arg.type->is_enum()) {
                            code_name_builder.append(handle_enum_arg(arg, interface));
                        } else {
                            code_name_builder.append(arg.type->get_binding_symbol());
                            if (!arg.type->is_new_id() && arg.type->can_reference()) {
                                code_name_builder.append('&');
                            }
                        }

                        ByteString code_name;
                        if (arg.type->nullable_type() && arg.type->nullable()) {
                            StringBuilder b;
                            b.appendff("Optional<{}>", code_name_builder.string_view());
                            code_name = b.to_byte_string();
                        } else {
                            code_name = code_name_builder.to_byte_string();
                        }

                        b.appendff("static_cast<{}>(", code_name);
                    }
                    b.appendff("resolved.at({})->{}()", i, arg.type->get_resolved_argument_caster());

                    // end cast
                    if (should_cast)
                        b.append(')');
                }

                args.append(b.to_byte_string());
                ++i;
            }
        }

        StringBuilder arg_names_builder;
        arg_names_builder.join(',', args, " {}"sv);
        auto arg_names_string = arg_names_builder.string_view().trim_whitespace();

        lnf(builder, "void {}::handle_{}({}{})", interface.c_name(), event.name, "Object &object, Vector<NonnullOwnPtr<ResolvedArgument>>&", event.args.is_empty() ? "" : " resolved");

        // function start
        ln(builder, '{');

        indent(builder);
        lnf(builder, "auto& self = static_cast<{}&>(object);", interface.c_name());

        // Initialize new-id arguments if they exist, as we only have typedata here :/
        {
            size_t i = 0;
            for (auto const& arg : event.args) {
                i++;
                if (!arg.type->is_new_id())
                    continue;

                indent(builder);
                lnf(builder, "auto {}_{} = self.m_connection.make_object_foreign_id<{}>(resolved.at({})->as_new_id());", arg.name.value(), i - 1, arg.type->get_binding_symbol(), i - 1);
            }
        }

        // if
        indent(builder);
        lnf(builder, "if (self.on_{}) {}", event.name, '{');

        // on_$EVENTNAME
        indent(builder, 2);
        lnf(builder, "self.on_{}({});", event.name, arg_names_string);

        // if end
        indent(builder);
        ln(builder, '}');

        // function end
        ln(builder, '}');

        code.functions.append(builder);
    }

    void add_event_handler_to_class(StringBuilder& builder, const struct NodeMethod& event)
    {
        /* void handle_$EVENTNAME(args)
         * {
         *     if(on_$EVENTNAME){
         *         on_$EVENTNAME(argsnames);
         *     }
         * }
         */

        // we could use any names here to parse the args to the callback function, but
        // keeping the original names for claritiy

        // .h
        indent(builder);
        lnf(builder, "static void handle_{}({});", event.name, "Object &object, Vector<NonnullOwnPtr<ResolvedArgument>>& resolved");
    }

    static void add_comment(StringBuilder& builder, StringView comment)
    {
        builder.append("/* "sv);
        builder.append(comment);
        builder.append(" */\n"sv);
    }

    template<typename... Parameters>
    static void add_comment_format(StringBuilder& builder, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        StringBuilder fmt_builder;
        AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::No, Parameters...> variadic_format_params { parameters... };
        MUST(vformat(fmt_builder, fmtstr.view(), variadic_format_params));

        add_comment(builder, fmt_builder.string_view());
    }

    void add_interface_methods_enum(NodeInterface& interface)
    {
        auto const& methods = interface.requests();

        if (methods.is_empty()) {
            return;
        }

        auto name = enum_c_name(interface, "Request");
        auto value = methods.begin();
        VERIFY(!value.is_end());

        size_t i = 0;
        add_enum(header_private.signal_enums, name, [&](StringBuilder& builder) {
            StringBuilder t;
            t.appendff("{}", i);
            add_enum_value(builder, value->name, t.string_view());
            ++value;
            ++i;
            return !value.is_end();
        });
    }

    void add_all_interface_enums(NodeInterface& interface)
    {
        for (auto& _enum : interface.enums()) {
            auto value = _enum.entries.begin();

            auto enum_name = enum_c_name(interface, _enum.name);

            // QUIRK: Make WlShm.Format uppercase
            if (enum_name == "ShmFormat") {
                // TODO: Turn this if into an error. enums with zero values don't make sense
                if (!value.is_end()) {
                    add_enum(header.forward, enum_name, [&](StringBuilder& builder) {
                        lnf(builder, "    {} = {},", prefix_when_starting_with_number(value->name.to_uppercase()), value->value);

                        ++value;

                        return !value.is_end();
                    });
                }
            } else {
                // TODO: Turn this if into an error. enums with zero values don't make sense
                if (!value.is_end()) {
                    add_enum(header.forward, enum_name, [&](StringBuilder& builder) {
                        add_enum_value(builder, value->name, value->value);
                        ++value;
                        return !value.is_end();
                    });
                }
            }
        }
    }

    void add_interface_declaration_forward(NodeInterface& interface)
    {
        lnf(header.forward, "class {};", interface.c_name());
    }

    void add_constructor_to_code(NodeInterface& interface)
    {
        StringBuilder function;
        /*
                Display::Display(Connection& connection, uint32_t id)
        : Object(connection, id, DisplayInterface)

        */
        lnf(function, "{}::{}(Connection &connection, uint32_t id)", interface.c_name(), interface.c_name());
        indent(function);
        lnf(function, ": Object(connection, id, {}) {{}}", interface.interface_struct_c_name());

        code.functions.append(function);
    }

    void add_interface_declaration(StringBuilder& builder, NodeInterface& interface)
    {
        lnf(builder, "class {} : public Object {}", interface.c_name(), '{');
        indent(builder);
        ln(builder, "friend Connection;"sv);

        StringBuilder _public;
        StringBuilder _private;

        {
            indent(_public);
            lnf(_public, "static StringView name() {{");
            indent(_public, 2);
            lnf(_public, "return \"{}\"sv;", interface.name());
            indent(_public);
            lnf(_public, "}}");
        }
        {
            indent(_public);
            lnf(_public, "static uint32_t version() {{");
            indent(_public, 2);
            lnf(_public, "return {};", interface.version());
            indent(_public);
            lnf(_public, "}}");
        }

        if (interface.requests().size() > 0) {
            indent(_public);
            add_comment(_public, "Requests:"sv);
            for (auto const& request : interface.requests()) {
                add_request_to_class(_public, _private, request, interface);
            }
            ln(_public);
        }
        if (interface.events().size() > 0) {
            indent(_public);
            add_comment(_public, "Events"sv);
            for (auto const& event : interface.events()) {
                add_event_to_class_public(_public, event, interface);
                add_event_handler_to_class(_public, event);
                add_event_to_code(event, interface);
            }
            ln(_public);
        }

        indent(_private);
        lnf(_private, "{}(Connection &connection, uint32_t id);", interface.c_name());
        add_constructor_to_code(interface);

        if (!_public.is_empty()) {
            ln(builder, "public:"sv);
            ln(builder, _public.string_view());
        }

        ln(builder, "private:"sv);
        ln(builder, _private.string_view());

        ln(builder, "};"sv);
    }

    void add_include_directive_quotation_marks(StringBuilder& builder, ByteString const& string)
    {
        lnf(builder, "#include \"{}\"", string);
    }

    void add_include_directive_angled_brackets(StringBuilder& builder, ByteString const& string)
    {
        lnf(builder, "#include <{}>", string);
    }

    static void add_namespace(StringBuilder& start, StringBuilder& end, ByteString const& name)
    {
        lnf(start, "namespace {} {{", name);
        ln(end, '}');
    }

    void add_interface_struct(NodeInterface& interface)
    {
        ByteString struct_name = interface.interface_struct_c_name();

        StringBuilder methods_builder;
        Vector<ByteString> requests_name;
        Vector<ByteString> events_name;

        auto test = [&](char const* method_type, Vector<NodeMethod> methods, Vector<ByteString>& method_name_list, bool should_add_handle_function) {
            for (auto& method : methods) {
                auto name = ByteString::formatted("{}{}{}", interface.c_name(), method_type, titlecase_with_split(method.name));
                method_name_list.append(name);

                Vector<ByteString> arg_names;

                size_t i = 0;
                for (auto& arg : method.args) {
                    auto b = [&](Optional<ByteString> arg_name, StringView arg_wire_type_kind) {
                        auto arg_i = ByteString::formatted("{}", i);
                        auto arg_name_c = ByteString::formatted("{}{}", name, titlecase_with_split(arg_name.value_or(arg_i)));
                        arg_names.append(arg_name_c);

                        lnf(methods_builder, "static struct Argument {} {{", arg_name_c);

                        indent(methods_builder);
                        lnf(methods_builder, ".name = \"{}\",", arg_name.value_or(arg_i));

                        auto const* nullable_string = arg.type->nullable_type() && arg.type->nullable() ? "true" : "false";
                        indent(methods_builder);
                        lnf(methods_builder, ".type = WireArgumentType {{");
                        indent(methods_builder, 2);
                        lnf(methods_builder, ".kind = WireArgumentType::{},", arg_wire_type_kind);
                        indent(methods_builder, 2);
                        lnf(methods_builder, ".nullable = {},", nullable_string);
                        indent(methods_builder);
                        lnf(methods_builder, "}},");

                        ln(methods_builder, "};"sv);

                        i++;
                    };


                    if (arg.type->is_new_id() && !arg.type->has_type_name()) {
                        b("interface_name"sv.to_byte_string(), "String"sv);
                        b("interface_version"sv.to_byte_string(), "UnsignedInteger"sv);
                    }
                    auto kind_string = arg.type->get_wire_argument_type();
                    b(arg.name, kind_string);
                }
                lnf(methods_builder, "static struct Method {} {{", name);
                indent(methods_builder);
                lnf(methods_builder, ".name = \"{}\",", method.name);
                indent(methods_builder);
                lnf(methods_builder, ".amount_args = {},", method.args.size());
                indent(methods_builder);
                ln(methods_builder, ".arg = new Argument* [] {"sv);
                for (auto const& arg_name : arg_names) {
                    indent(methods_builder, 2);
                    lnf(methods_builder, "&{},", arg_name);
                }
                indent(methods_builder, 2);
                ln(methods_builder, "nullptr,"sv);
                indent(methods_builder);
                ln(methods_builder, "},"sv);
                indent(methods_builder);
                if (should_add_handle_function) {
                    lnf(methods_builder, ".handler = {}::handle_{},", interface.c_name(), method.name);
                } else {
                    lnf(methods_builder, ".handler = nullptr,");
                }
                ln(methods_builder, "};"sv);
                ln(methods_builder);
            };
        };

        test("Request", interface.requests(), requests_name, false);
        test("Event", interface.events(), events_name, true);

        StringBuilder final_builder;
        final_builder.append(methods_builder.string_view());

        lnf(final_builder, "static const struct Interface {} {{", struct_name);

        indent(final_builder);
        lnf(final_builder, ".name = \"{}\",", interface.name());

        indent(final_builder);
        ln(final_builder, ".requests = new Method* [] {"sv);
        for (auto& method_name : requests_name) {
            indent(final_builder, 2);
            lnf(final_builder, "&{},"sv, method_name);
        }

        indent(final_builder, 2);
        ln(final_builder, "nullptr,"sv);

        indent(final_builder);
        ln(final_builder, "},"sv);

        indent(final_builder);
        ln(final_builder, ".events = new Method* [] {"sv);
        for (auto& method_name : events_name) {
            indent(final_builder, 2);
            lnf(final_builder, "&{},"sv, method_name);
        }

        indent(final_builder, 2);
        ln(final_builder, "nullptr,"sv);

        indent(final_builder);
        ln(final_builder, "},"sv);

        ln(final_builder, "};"sv);
        ln(final_builder);

        /*
        struct NodeInterface {
            ByteString name;
            ByteString version;

            Optional<struct NodeDescription> description;

            Vector<struct NodeMethod> requests;
            Vector<struct NodeMethod> events;
            Vector<struct NodeEnum> enums;
        };
        */

        code.interfaces.append(final_builder);
    }

public:
    CodeGenerator(struct NodeProtocol protocol)
        : m_protocol(move(protocol))
    {
        add_include_directive_angled_brackets(header.initial, "LibWayland/Object.h");
        add_include_directive_angled_brackets(header.initial, "AK/Function.h");

        add_include_directive_angled_brackets(code.initial, "AK/NonnullOwnPtr.h");
        add_include_directive_angled_brackets(code.initial, "LibWayland/Connection.h");
        add_include_directive_quotation_marks(code.initial, header_name().string_view());
        add_include_directive_quotation_marks(code.initial, header_name_private().string_view());

        ln(header.initial, "#pragma once"sv);

        // Namespacing
        add_namespace(header.initial, header.end, "Wayland");
        add_namespace(code.initial, code.end, "Wayland");

        // Forward declare all interfaces
        // TODO: forward declare types from protocols (requires tracking unknown types)
        add_comment(header.forward, "Forward declaration"sv);

        for (auto& interface : m_protocol.interfaces) {
            add_interface_declaration_forward(*interface);
        }

        ln(header.forward);

        // First all the enums from all interfaces, then the event and requests enum
        // (so a specific request/event id can be matched to an object)
        for (auto& interface : m_protocol.interfaces) {
            add_all_interface_enums(*interface);
            add_interface_struct(*interface);
        }

        for (auto& interface : m_protocol.interfaces) {
            StringBuilder builder;

            add_comment_format(builder, "Interface: {}", interface->name());

            add_interface_declaration(builder, *interface);
            header.interfaces.append(builder);
        }

        // private header
        for (auto& interface : m_protocol.interfaces) {
            add_interface_methods_enum(*interface);
        }
    }

    ByteString generate_header() const
    {
        StringBuilder builder;

        ln(builder, header.initial.string_view());
        ln(builder, header.forward.string_view());

        for (auto const& interface : header.interfaces) {
            ln(builder, interface.string_view());
        }

        ln(builder);

        builder.append(header.end.string_view());

        return builder.to_byte_string();
    }

    ByteString generate_header_private() const
    {
        StringBuilder builder;

        ln(builder, header_private.initial.string_view());
        ln(builder, header_private.signal_enums.string_view());
        ln(builder, header_private.interfaces.string_view());
        builder.append(header_private.end.string_view());

        return builder.to_byte_string();
    }

    ByteString generate_code() const
    {
        StringBuilder builder;

        ln(builder, code.initial.string_view());

        for (auto const& interface : code.interfaces) {
            ln(builder, interface.string_view());
        }

        for (auto const& function : code.functions) {
            ln(builder, function.string_view());
        }

        ln(builder, code.end.string_view());

        return builder.to_byte_string();
    }

    StringBuilder header_name() const
    {
        StringBuilder builder;

        builder.append(m_protocol.name.view());
        builder.append("-protocol.h"sv);

        return builder;
    }

    StringBuilder header_name_private() const
    {
        StringBuilder builder;

        builder.append(m_protocol.name.view());
        builder.append("-private-protocol.h"sv);

        return builder;
    }

    StringBuilder code_name() const
    {
        StringBuilder builder;

        builder.append(m_protocol.name.view());
        builder.append("-protocol.cpp"sv);

        return builder;
    }
};
}

ErrorOr<bool>
write_file(ByteString const& string, StringView s_path)
{
    warnln("path:{}", s_path);

    warnln("writing");
    auto bytes = string.bytes();
    int fd = TRY(Core::System::open(s_path, O_WRONLY | O_CREAT | O_SYNC, 0100644));
    warnln("fd");
    write(fd, bytes.data(), bytes.size());
    warnln("wrote");

    TRY(Core::System::close(fd));

    return true;
}

ErrorOr<int>
serenity_main(Main::Arguments arguments)
{
    StringView filename;
    StringView outdir;
    ByteString s_path;
    ByteString s_outdir;

    Core::ArgsParser parser;
    parser.set_general_help("Parse Wayland protocols and generate Serenity/C++ bindings for them");
    parser.add_positional_argument(filename, "File to read from", "file");
    parser.add_positional_argument(outdir, "Directory to output codegen", "outdir");

    parser.parse(arguments);

    s_path = TRY(FileSystem::real_path(filename));
    s_outdir = TRY(FileSystem::real_path(outdir));

    if (!FileSystem::is_regular_file(s_path)) {
        warnln("Specified file is not a file: `{}`", s_path);
        return 1;
    }

    if (!FileSystem::is_directory(s_outdir)) {
        warnln("Specified outdir is not a directoy: `{}`", s_outdir);
        return 1;
    }

    ByteString xml_document;

    {
        auto doctype_start = "<!DOCTYPE protocol ["sv.bytes();
        auto doctype_end = "] >"sv.bytes();
        auto needle = "?>"sv;

        auto file = TRY(Core::File::open(s_path, Core::File::OpenMode::Read));
        auto contents = TRY(file->read_until_eof());
        auto contents_str = ByteString(contents.bytes());
        auto location = contents_str.find(needle).value();

        // Append the document definition inline in the protocol xml
        // So we don't have to add a resource locator and add a doctype,
        // which points to the dtd.
        StringBuilder builder;
        builder.append(contents_str.substring_view(0, location + needle.length()));
        builder.append(doctype_start);
        builder.append(Wayland::wayland_dtd_xml);
        builder.append(doctype_end);
        builder.append(contents_str.substring_view(location + needle.length()));
        xml_document = builder.to_byte_string();
    }

    // 3 basic steps:
    // 1. Parse XML
    auto xml_parser = XML::Parser {
        xml_document,
        {
            .preserve_comments = true,
        }
    };

    // error handling yoinked from Utilities/xml.cpp
    auto result = xml_parser.parse();
    if (result.is_error()) {
        if (xml_parser.parse_error_causes().is_empty()) {
            warnln("{}", result.error());
        } else {
            warnln("{}; caused by:", result.error());
            for (auto const& cause : xml_parser.parse_error_causes())
                warnln("    {}", cause);
        }
        return 1;
    }

    auto document = result.release_value();

    // 2. Build Syntax tree
    auto protocol = TRY(Wayland::NodeProtocol::parse(document));

    // 3. Generate code
    auto* gen = new Wayland::CodeGenerator(protocol);

    auto concat = [&](StringView name) {
        StringBuilder builder;

        builder.append(s_outdir);
        builder.append('/');
        builder.append(name);
        builder.append(".tmp"sv);

        return builder.to_byte_string();
    };

    auto header = gen->header_name();
    TRY(write_file(gen->generate_header(), concat(header.string_view())));
    sync();
    auto header_private = gen->header_name_private();
    TRY(write_file(gen->generate_header_private(), concat(header_private.string_view())));
    sync();
    auto code_name = gen->code_name();
    TRY(write_file(gen->generate_code(), concat(code_name.string_view())));
    sync();

    return 0;
}
