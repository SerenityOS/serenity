#include <AK/Format.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <LibWayland/Interface.h>
#include <LibXML/Parser/Parser.h>
#include <fcntl.h>

namespace Wayland {

static StringView parse_copyright(const XML::Node::Element& element)
{
    auto const& text_node = element.children.first();
    VERIFY(text_node->is_text());

    return text_node->as_text().builder.string_view();
}

struct NodeDescription {
    ByteString summary;
    Optional<StringView> text;
};

static struct NodeDescription parse_description(const XML::Node::Element& element)
{
    auto const& summary = element.attributes.get("summary");
    VERIFY(summary.has_value());

    Optional<StringView> text;

    if (element.children.size() > 0) {
        auto const& text_node = element.children.first();
        VERIFY(text_node->is_text());
        VERIFY(element.children.size() == 1);
        text = text_node->as_text().builder.string_view();
    }

    return (struct NodeDescription) {
        .summary = summary.value(),
        .text = text,
    };
}

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

    static ArgType* create_new_id(const Optional<ByteString>& interface)
    {
        auto* self = new ArgType(NewId);
        if (interface.has_value()) {
            self->m_type_name = interface.value();
        }
        return self;
    }

    static ArgType* create_object(const Optional<ByteString>& interface)
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

    bool is_new_id() const
    {
        return m_type == NewId;
    }

    bool is_enum() const
    {
        return m_type == Enum;
    }

    ByteString& type_name()
    {
        return m_type_name.value();
    }

    bool can_reference()
    {
        return (is_primitive() && m_primitive.value() == Fixed) || m_type == Array || m_type == Object || m_type == NewId || m_type == String;
    }

    StringView get_binding_symbol()
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
            return "List"sv;
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
            auto code_name = to_code_name(m_type_name.value());
            return code_name.to_byte_string().view();
        }

        VERIFY_NOT_REACHED();
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

struct NodeArg {
    Optional<ByteString> name;
    ArgType* type;

    Optional<ByteString> summary;
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

static struct NodeArg parse_arg(const XML::Node::Element& element)
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

struct NodeMethod {
    ByteString name;
    Optional<ByteString> type;
    Optional<ByteString> since;

    Optional<struct NodeDescription> description;
    Vector<struct NodeArg> args;
};

static struct NodeMethod parse_method(const XML::Node::Element& element)
{
    auto const& name = element.attributes.get("name").value();

    auto const& type = element.attributes.get("type");

    auto const& since = element.attributes.get("since");

    Optional<struct NodeDescription> description;
    Vector<struct NodeArg> args;

    for (auto const& iter : element.children) {
        if (!iter->is_element()) {
            continue;
        }

        auto const& element = iter->as_element();

        if (element.name == "arg") {
            args.append(parse_arg(element));
        } else if (element.name == "description") {
            VERIFY(!description.has_value());
            description = parse_description(element);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    return {
        .name = name,
        .type = type,
        .since = since,
        .description = description,
        .args = args,
    };
}

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

    Optional<struct NodeDescription> description;
    Vector<struct NodeEnumEntry> entries;
};

static struct NodeEnum parse_enum(const XML::Node::Element& element)
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
            _enum.description = parse_description(child);
        }
    }

    return _enum;
}

struct NodeInterface {
    ByteString name;
    ByteString version;

    Optional<struct NodeDescription> description;

    Vector<struct NodeMethod> requests;
    Vector<struct NodeMethod> events;
    Vector<struct NodeEnum> enums;
};

static struct NodeInterface parse_interface(const XML::Node::Element& element)
{
    auto name = element.attributes.get("name");
    VERIFY(name.has_value());

    auto version = element.attributes.get("version");
    VERIFY(version.has_value());

    auto interface = NodeInterface {
        .name = name.value(),
        .version = version.value(),
        .description = {},

        .requests = Vector<struct NodeMethod>(),
        .events = Vector<struct NodeMethod>(),
        .enums = Vector<struct NodeEnum>(),
    };

    for (auto const& iter : element.children) {
        if (!(*iter).is_element()) {
            continue;
        }

        auto const& element = (*iter).as_element();
        if (element.name == "request") {
            interface.requests.append(parse_method(element));
        } else if (element.name == "event") {
            interface.events.append(parse_method(element));
        } else if (element.name == "enum") {
            interface.enums.append(parse_enum(element));
        } else if (element.name == "description") {
            VERIFY(!interface.description.has_value());
            interface.description = parse_description(element);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    return interface;
}

namespace gen {
Optional<StringView> g_strip_prefix;

static void ln(StringBuilder& builder, StringView line = ""sv)
{
    builder.append(line);
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

bool is_char_number(char c)
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

static ByteString titlecase_with_split(ByteString const& string, char seperator = '_')
{
    StringBuilder builder;
    auto parts = string.split(seperator);
    for (auto& part : parts) {
        builder.append(part.to_titlecase());
    }

    return builder.to_byte_string();
}

static ByteString interface_c_name(const ByteString& interface)
{
    ByteString name;
    if (g_strip_prefix.has_value() && interface.view().compare(*g_strip_prefix) == 1) {
        name = interface.substring_view(g_strip_prefix->length());
    } else {
        name = interface;
    }

    return titlecase_with_split(name);
}
static ByteString interface_c_name(const struct NodeInterface& interface)
{
    return interface_c_name(interface.name);
}


static ByteString enum_c_name(ByteString const& interface, ByteString const& name)
{
    StringBuilder builder;
    builder.append(interface_c_name(interface));
    builder.append(titlecase_with_split(name));

    return builder.to_byte_string();
}

static ByteString enum_c_name(struct NodeInterface& interface, ByteString const& name)
{
    return enum_c_name(interface.name, name);
}

static void add_enum(StringBuilder& builder, ByteString const& enum_name, Function<bool(StringBuilder&)> function)
{
    lnf(builder, "enum class {} {}", enum_name, "{");

    while (function(builder)) { }

    builder.append("};\n\n"sv);
}

static void add_enum_value(StringBuilder& builder, StringView value_name, StringView value_data)
{
    lnf(builder, "    {} = {},", prefix_when_starting_with_number(value_name.to_uppercase_string()), value_data);
}

static void add_enum_value_default(StringBuilder& builder, StringView value_name)
{
    builder.append("    "sv);
    builder.append(prefix_when_starting_with_number(value_name.to_uppercase_string()));
    builder.append(",\n"sv);
}

static void add_request_to_class(StringBuilder& builder, struct NodeMethod& request, struct NodeInterface& interface)
{
    Optional<ByteString> new_object;
    Vector<ByteString> args;

    for (auto& arg : request.args) {
        StringBuilder child_builder;
        // fix for wl_registry.bind
        if (arg.type->is_new_id() && !(interface.name == "wl_registry" && request.name == "bind")) {
            VERIFY(!new_object.has_value());
            new_object = arg.type->get_binding_symbol();
            continue;
        } else if (arg.type->is_enum()) {
            auto const& name = arg.type->type_name();
            ByteString translated_name;
            if (name.contains('.')) {
                auto splits = name.split_view('.');
                VERIFY(splits.size() == 2);
                translated_name = enum_c_name(splits[0].to_byte_string(), splits[1].to_byte_string());
            } else {
                translated_name = enum_c_name(interface, name);
            }

            child_builder.append(translated_name);
            child_builder.append(' ');
            child_builder.append(arg.name->view());

            args.append(child_builder.to_byte_string());
            continue;
        }

        child_builder.append(arg.type->get_binding_symbol());
        if (arg.type->can_reference()) {
            child_builder.append('&');
        }
        child_builder.append(' ');
        child_builder.append(arg.name->view());

        args.append(child_builder.to_byte_string());
    }

    ByteString returns = new_object.has_value() ? new_object.value() : "void";

    builder.append("    "sv);
    builder.append(returns);
    builder.append(" "sv);
    builder.append(request.name);
    builder.append("("sv);
    StringBuilder args_builder;
    args_builder.join(',', args, " {}"sv);
    builder.append(args_builder.string_view().trim_whitespace());
    builder.append(");\n"sv);
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

enum class MethodType {
    Request,
    Event,
};
ByteString method_type_get_string(MethodType type)
{
    if (type == MethodType::Request) {
        return "Request";
    } else if (type == MethodType::Event) {
        return "Event";
    } else {
        VERIFY_NOT_REACHED();
    }
}

static void add_interface_methods_enum(StringBuilder& builder, struct NodeInterface& interface, Vector<struct NodeMethod>& methods, MethodType type)
{
    if (methods.is_empty()) {
        return;
    }

    auto name = enum_c_name(interface, method_type_get_string(type));
    auto value = methods.begin();
    VERIFY(!value.is_end());

    gen::add_enum(builder, name, [&](StringBuilder& builder) {
        gen::add_enum_value_default(builder, value->name);
        ++value;
        return !value.is_end();
    });
}

static void add_all_interface_enums(StringBuilder& builder, struct NodeInterface& interface)
{
    for (auto& _enum : interface.enums) {
        auto value = _enum.entries.begin();

        // TODO: Turn this if into an error. enums with zero values don't make sense
        if (!value.is_end()) {
            gen::add_enum(builder, enum_c_name(interface, _enum.name), [&](StringBuilder& builder) {
                gen::add_enum_value(builder, value->name, value->value);
                ++value;
                return !value.is_end();
            });
        }
    }
}

static void add_interface_declaration(StringBuilder& builder, struct NodeInterface& interface, bool forward = false)
{
    builder.append("class "sv);
    builder.append(interface_c_name(interface));
    if (forward) {
        ln(builder, ";"sv);
        return;
    }
    ln(builder, " : Object {"sv);

    ln(builder, "public:"sv);
    for (auto& request : interface.requests) {
        add_request_to_class(builder, request, interface);
    }
    ln(builder, "};\n"sv);
}

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView filename;
    Optional<StringView> dtd_filename;
    ByteString s_path;

    Core::ArgsParser parser;
    parser.set_general_help("Parse Wayland protocols and generate Serenity/C++ bindings for them");
    parser.add_option(dtd_filename, "Validate using the Document definition for wayland protocols", "dtd-path", 'd', "dtd");
    parser.add_positional_argument(filename, "File to read from", "file");
    parser.parse(arguments);

    s_path = TRY(FileSystem::real_path(filename));
    auto file = TRY(Core::File::open(s_path, Core::File::OpenMode::Read));
    auto contents = TRY(file->read_until_eof());

    if (dtd_filename.has_value()) {
        // Append the document definition inline in the protocol xml
        // So we don't have to add a resource locator and add a doctype,
        // which points to the dtd.
        auto dtd_path = TRY(FileSystem::real_path(dtd_filename.release_value()));
        auto dtd_file = TRY(Core::File::open(dtd_path, Core::File::OpenMode::Read));
        auto dtd_contents = TRY(file->read_until_eof());
        auto old_contents = contents;
        auto doctype_start = "<!DOCTYPE protocol ["sv.bytes();
        auto doctype_end = "] >"sv.bytes();
        contents = TRY(ByteBuffer::create_uninitialized(doctype_start.size() + dtd_contents.size() + doctype_end.size() + old_contents.size()));
        contents.append(doctype_start);
        contents.append(dtd_contents);
        contents.append(doctype_end);
        contents.append(old_contents);
    }

    // 3 basic steps:
    // 1. Parse XML
    auto xml_parser = XML::Parser {
        contents,
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
    VERIFY(document.root().is_element());
    auto const& root = document.root().as_element();
    VERIFY(root.name == "protocol");

    ByteString protocol_name = root.attributes.get("name").value();
    Optional<ByteString> protocol_copyright;
    Optional<struct NodeDescription> protocol_description;
    Vector<struct NodeInterface> interfaces;

    // TODO: Make failing validation nicer: Printing where the error exactly occured (line, column, snippet)
    for (auto const& iter : root.children) {
        // Ignore comments and texts in <protocol> (text is not even in the spec)
        if (!iter->is_element()) {
            continue;
        }

        auto const& element = iter->as_element();
        if (element.name == "copyright") {
            VERIFY(!protocol_copyright.has_value());
            protocol_copyright = parse_copyright(element);
        } else if (element.name == "description") {
            VERIFY(!protocol_description.has_value());
            protocol_description = parse_description(element);
        } else if (element.name == "interface") {
            interfaces.append(parse_interface(element));
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    // 3. Generate code
    StringBuilder header_generator;

    gen::ln(header_generator, "#pragma once"sv);
    gen::ln(header_generator, "#include <LibWayland/Object.h>"sv);
    header_generator.append("\nnamespace Wayland {\n"sv);

    bool xdg_namespace = false;

    if (protocol_name == "wayland") {
        gen::g_strip_prefix = "wl_"sv;
    }
    if (protocol_name.starts_with("xdg"sv)) {
        header_generator.append("namespace xdg {\n"sv);
        gen::g_strip_prefix = "xdg_"sv;
        xdg_namespace = true;
    }

    gen::ln(header_generator);

    // Forward declare all interfaces

    // TODO: forward declare types from protocols (requires tracking unknown types)
    gen::add_comment(header_generator, "Forward declaration"sv);

    for (auto& interface : interfaces) {
        gen::add_interface_declaration(header_generator, interface, true);
    }
    gen::ln(header_generator);

    // First all the enums from all interfaces, then the event and requests enum
    // (so a specific request/event id can be matched to an object)

    for (auto& interface : interfaces) {
        gen::add_all_interface_enums(header_generator, interface);
    }

    for (auto& interface : interfaces) {

        gen::add_comment_format(header_generator, "Interface: {}", interface.name);

        add_interface_methods_enum(header_generator, interface, interface.requests, gen::MethodType::Request);
        add_interface_methods_enum(header_generator, interface, interface.events, gen::MethodType::Event);


        gen::add_interface_declaration(header_generator, interface);
    }

    if (xdg_namespace) {
        header_generator.append("}\n"sv);
    }
    header_generator.append("}\n"sv);

    warnln("writing");
    {
        auto buffer = header_generator.to_byte_string();
        auto bytes = buffer.bytes();
        auto fd = MUST(Core::System::open("/tmp/wayland.h"sv, O_CREAT | O_WRONLY, 0100644));
        warnln("fd");
        write(fd, bytes.data(), bytes.size());
        warnln("wrote");
        (void)Core::System::close(fd);
    }

    return 0;
}
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    return Wayland::serenity_main(arguments);
}
