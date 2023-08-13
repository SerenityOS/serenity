/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Forward.h>
#include <AK/HashTable.h>
#include <AK/LexicalPath.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/Try.h>
#include <AK/Utf8View.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/GML/Parser.h>
#include <LibGUI/UIDimensions.h>
#include <LibMain/Main.h>

enum class UseObjectConstructor : bool {
    No,
    Yes,
};

// Classes whose header doesn't have the same name as the class.
static Optional<StringView> map_class_to_file(StringView class_)
{
    static HashMap<StringView, StringView> class_file_mappings {
        { "GUI::HorizontalSplitter"sv, "GUI/Splitter"sv },
        { "GUI::VerticalSplitter"sv, "GUI/Splitter"sv },
        { "GUI::HorizontalSeparator"sv, "GUI/SeparatorWidget"sv },
        { "GUI::VerticalSeparator"sv, "GUI/SeparatorWidget"sv },
        { "GUI::HorizontalBoxLayout"sv, "GUI/BoxLayout"sv },
        { "GUI::VerticalBoxLayout"sv, "GUI/BoxLayout"sv },
        { "GUI::HorizontalProgressbar"sv, "GUI/Progressbar"sv },
        { "GUI::VerticalProgressbar"sv, "GUI/Progressbar"sv },
        { "GUI::DialogButton"sv, "GUI/Button"sv },
        { "GUI::PasswordBox"sv, "GUI/TextBox"sv },
        // Map Layout::Spacer to the Layout header even though it's a pseudo class.
        { "GUI::Layout::Spacer"sv, "GUI/Layout"sv },
    };
    return class_file_mappings.get(class_);
}

// Properties which don't take a direct JSON-like primitive (StringView, int, bool, Array etc) as arguments and need the arguments to be wrapped in a constructor call.
static Optional<StringView> map_property_to_type(StringView property)
{
    static HashMap<StringView, StringView> property_to_type_mappings {
        { "container_margins"sv, "GUI::Margins"sv },
        { "margins"sv, "GUI::Margins"sv },
    };
    return property_to_type_mappings.get(property);
}

// Properties which take a UIDimension which can handle JSON directly.
static bool is_ui_dimension_property(StringView property)
{
    static HashTable<StringView> ui_dimension_properties;
    if (ui_dimension_properties.is_empty()) {
        ui_dimension_properties.set("min_width"sv);
        ui_dimension_properties.set("max_width"sv);
        ui_dimension_properties.set("preferred_width"sv);
        ui_dimension_properties.set("min_height"sv);
        ui_dimension_properties.set("max_height"sv);
        ui_dimension_properties.set("preferred_height"sv);
    }
    return ui_dimension_properties.contains(property);
}

// FIXME: Since normal string-based properties take either String or StringView (and the latter can be implicitly constructed from the former),
//        we need to special-case DeprecatedString property setters while those still exist.
//        Please remove a setter from this list once it uses StringView or String.
static bool takes_deprecated_string(StringView property)
{
    static HashTable<StringView> deprecated_string_properties;
    if (deprecated_string_properties.is_empty()) {
        deprecated_string_properties.set("name"sv);
    }
    return deprecated_string_properties.contains(property);
}

static ErrorOr<String> include_path_for(StringView class_name, LexicalPath const& gml_file_name)
{
    String pathed_name;
    if (auto mapping = map_class_to_file(class_name); mapping.has_value())
        pathed_name = TRY(String::from_utf8(mapping.value()));
    else
        pathed_name = TRY(TRY(String::from_utf8(class_name)).replace("::"sv, "/"sv, ReplaceMode::All));

    if (class_name.starts_with("GUI::"sv) || class_name.starts_with("WebView::"sv))
        return String::formatted("<Lib{}.h>", pathed_name);

    // We assume that all other paths are within the current application, for now.
    // To figure out what kind of userland program this is (application, service, ...) we consider the path to the original GML file.
    auto const& paths = gml_file_name.parts_view();
    auto path_iter = paths.find("Userland"sv);
    path_iter++;
    auto const userland_subdirectory = (path_iter == paths.end()) ? "Applications"_string : TRY(String::from_utf8(*path_iter));
    return String::formatted("<{}/{}.h>", userland_subdirectory, pathed_name);
}

// Each entry is an include path, without the "#include" itself.
static ErrorOr<HashTable<String>> extract_necessary_includes(GUI::GML::Object const& gml_hierarchy, LexicalPath const& gml_file_name)
{
    HashTable<String> necessary_includes;
    TRY(necessary_includes.try_set(TRY(include_path_for(gml_hierarchy.name(), gml_file_name))));
    if (gml_hierarchy.layout_object() != nullptr)
        TRY(necessary_includes.try_set(TRY(include_path_for(gml_hierarchy.layout_object()->name(), gml_file_name))));

    TRY(gml_hierarchy.try_for_each_child_object([&](auto const& object) -> ErrorOr<void> {
        auto necessary_child_includes = TRY(extract_necessary_includes(object, gml_file_name));
        for (auto const& include : necessary_child_includes)
            TRY(necessary_includes.try_set(include));
        return {};
    }));

    return necessary_includes;
}

static char const header[] = R"~~~(
/*
 * Auto-generated by the GML compiler
 */

)~~~";

static char const function_start[] = R"~~~(
// Creates a @main_class_name@ and initializes it.
// This function was auto-generated by the GML compiler.
ErrorOr<NonnullRefPtr<@main_class_name@>> @main_class_name@::try_create()
{
    RefPtr<::@main_class_name@> main_object;

)~~~";

static char const footer[] = R"~~~(
    return main_object.release_nonnull();
}
)~~~";

static ErrorOr<String> escape_string(JsonValue to_escape)
{
    auto string = TRY(String::from_deprecated_string(to_escape.as_string()));

    // All C++ simple escape sequences; see https://en.cppreference.com/w/cpp/language/escape
    // Other commonly-escaped characters are hard-to-type Unicode and therefore fine to include verbatim in UTF-8 coded strings.
    static HashMap<StringView, StringView> escape_sequences = {
        { "\0"sv, "\\0"sv },
        { "\'"sv, "\\'"sv },
        { "\""sv, "\\\""sv },
        { "\\"sv, "\\\\"sv },
        { "\a"sv, "\\a"sv },
        { "\b"sv, "\\b"sv },
        { "\f"sv, "\\f"sv },
        { "\n"sv, "\\n"sv },
        { "\r"sv, "\\r"sv },
        { "\t"sv, "\\t"sv },
        { "\v"sv, "\\v"sv },
    };

    for (auto const& entries : escape_sequences)
        string = TRY(string.replace(entries.key, entries.value, ReplaceMode::All));

    return string;
}

// This function assumes that the string is already the same as its enum constant's name.
// Therefore, it does not handle UI dimensions.
static ErrorOr<Optional<String>> generate_enum_initializer_for(StringView property_name, JsonValue value)
{
    // The value is the enum's type name.
    static HashMap<StringView, StringView> enum_properties = {
        { "text_alignment"sv, "Gfx::TextAlignment"sv },
        { "focus_policy"sv, "GUI::FocusPolicy"sv },
        { "foreground_role"sv, "Gfx::ColorRole"sv },
        { "text_wrapping"sv, "Gfx::TextWrapping"sv },
    };

    auto const& enum_type_name = enum_properties.get(property_name);
    if (!enum_type_name.has_value())
        return Optional<String> {};

    return String::formatted("{}::{}", *enum_type_name, value.as_string());
}

// FIXME: In case of error, propagate the precise array+property that triggered the error.
static ErrorOr<String> generate_initializer_for(Optional<StringView> property_name, JsonValue value)
{
    if (value.is_string()) {
        if (property_name.has_value()) {
            if (takes_deprecated_string(*property_name))
                return String::formatted(R"~~~("{}"sv)~~~", TRY(escape_string(value)));

            if (auto const enum_value = TRY(generate_enum_initializer_for(*property_name, value)); enum_value.has_value())
                return String::formatted("{}", *enum_value);

            if (*property_name == "bitmap"sv)
                return String::formatted(R"~~~(TRY(Gfx::Bitmap::load_from_file("{}"sv)))~~~", TRY(escape_string(value)));
        }

        return String::formatted(R"~~~("{}"_string)~~~", TRY(escape_string(value)));
    }
    // No need to handle the smaller integer types separately.
    if (value.is_integer<i64>())
        return String::formatted("static_cast<i64>({})", value.as_integer<i64>());
    if (value.is_integer<u64>())
        return String::formatted("static_cast<u64>({})", value.as_integer<u64>());
    if (value.is_bool())
        return String::formatted("{}", value.as_bool());
    if (value.is_double())
        return String::formatted("static_cast<double>({})", value.as_double());
    if (value.is_array()) {
        auto const& array = value.as_array();
        auto child_type = Optional<StringView> {};
        for (auto const& child_value : array.values()) {
            if (child_value.is_array())
                return Error::from_string_view("Nested arrays are not supported"sv);

#define HANDLE_TYPE(type_name, is_type)                                                             \
    if (child_value.is_type() && (!child_type.has_value() || child_type.value() == #type_name##sv)) \
        child_type = #type_name##sv;                                                                \
    else

            HANDLE_TYPE(StringView, is_string)
            HANDLE_TYPE(i64, is_integer<i64>)
            HANDLE_TYPE(u64, is_integer<u64>)
            HANDLE_TYPE(bool, is_bool)
            HANDLE_TYPE(double, is_double)
            return Error::from_string_view("Inconsistent contained type in JSON array"sv);
#undef HANDLE_TYPE
        }
        if (!child_type.has_value())
            return Error::from_string_view("Empty JSON array; cannot deduce type."sv);

        StringBuilder initializer;
        initializer.appendff("Array<{}, {}> {{ "sv, child_type.release_value(), array.size());
        for (auto const& child_value : array.values())
            initializer.appendff("{}, ", TRY(generate_initializer_for({}, child_value)));
        initializer.append("}"sv);
        return initializer.to_string();
    }
    return Error::from_string_view("Unsupported JSON value"sv);
}

// Loads an object and assigns it to the RefPtr<Widget> variable named object_name.
// All loading happens in a separate block.
static ErrorOr<void> generate_loader_for_object(GUI::GML::Object const& gml_object, SourceGenerator generator, String object_name, size_t indentation, UseObjectConstructor use_object_constructor)
{
    generator.set("object_name", object_name.to_deprecated_string());
    generator.set("class_name", gml_object.name());

    auto append = [&]<size_t N>(auto& generator, char const(&text)[N]) -> ErrorOr<void> {
        generator.append(TRY(String::repeated(' ', indentation * 4)).bytes_as_string_view());
        generator.appendln(text);
        return {};
    };

    generator.append(TRY(String::repeated(' ', (indentation - 1) * 4)).bytes_as_string_view());
    generator.appendln("{");
    if (use_object_constructor == UseObjectConstructor::Yes)
        TRY(append(generator, "@object_name@ = TRY(@class_name@::try_create());"));
    else
        TRY(append(generator, "@object_name@ = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ::@class_name@()));"));

    // Properties
    TRY(gml_object.try_for_each_property([&](StringView key, NonnullRefPtr<GUI::GML::JsonValueNode> value) -> ErrorOr<void> {
        auto value_code = TRY(generate_initializer_for(key, value));
        if (is_ui_dimension_property(key)) {
            if (auto ui_dimension = GUI::UIDimension::construct_from_json_value(value); ui_dimension.has_value())
                value_code = TRY(ui_dimension->as_cpp_source());
            else
                // FIXME: propagate precise error cause
                return Error::from_string_view("UI dimension invalid"sv);
        } else {
            // Wrap value in an extra constructor call if necessary.
            if (auto type = map_property_to_type(key); type.has_value())
                value_code = TRY(String::formatted("{} {{ {} }}", type.release_value(), value_code));
        }

        auto property_generator = TRY(generator.fork());
        property_generator.set("key", key);
        property_generator.set("value", value_code.bytes_as_string_view());
        TRY(append(property_generator, R"~~~(@object_name@->set_@key@(@value@);)~~~"));
        return {};
    }));
    generator.appendln("");

    // Layout
    if (gml_object.layout_object() != nullptr) {
        TRY(append(generator, "RefPtr<GUI::Layout> layout;"));
        TRY(generate_loader_for_object(*gml_object.layout_object(), TRY(generator.fork()), TRY(String::from_utf8("layout"sv)), indentation + 1, UseObjectConstructor::Yes));
        TRY(append(generator, "@object_name@->set_layout(layout.release_nonnull());"));
        generator.appendln("");
    }

    // Children
    size_t current_child_index = 0;
    auto next_child_name = [&]() {
        return String::formatted("{}_child_{}", object_name, current_child_index++);
    };
    TRY(gml_object.try_for_each_child_object([&](auto const& child) -> ErrorOr<void> {
        // Spacer is a pseudo-class that insteads causes a call to `Widget::add_spacer` on the parent object.
        if (child.name() == "GUI::Layout::Spacer"sv) {
            TRY(append(generator, "@object_name@->add_spacer();"));
            return {};
        }

        auto child_generator = TRY(generator.fork());
        auto child_variable_name = TRY(next_child_name());
        child_generator.set("child_variable_name", child_variable_name.bytes_as_string_view());
        child_generator.set("child_class_name", child.name());
        TRY(append(child_generator, "RefPtr<@child_class_name@> @child_variable_name@;"));
        TRY(generate_loader_for_object(child, TRY(child_generator.fork()), child_variable_name, indentation + 1, UseObjectConstructor::Yes));

        // Handle the current two special cases of child adding.
        if (gml_object.name() == "GUI::ScrollableContainerWidget"sv)
            TRY(append(child_generator, "static_ptr_cast<GUI::ScrollableContainerWidget>(@object_name@)->set_widget(*@child_variable_name@);"));
        else if (gml_object.name() == "GUI::TabWidget"sv)
            TRY(append(child_generator, "static_ptr_cast<GUI::TabWidget>(@object_name@)->add_widget(*@child_variable_name@);"));
        else
            TRY(append(child_generator, "TRY(@object_name@->try_add_child(*@child_variable_name@));"));
        child_generator.appendln("");
        return {};
    }));

    generator.append(TRY(String::repeated(' ', (indentation - 1) * 4)).bytes_as_string_view());
    generator.appendln("}");

    return {};
}

static ErrorOr<String> generate_cpp(NonnullRefPtr<GUI::GML::GMLFile> gml, LexicalPath const& gml_file_name)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(header);

    auto& main_class = gml->main_class();
    auto necessary_includes = TRY(extract_necessary_includes(main_class, gml_file_name));
    static String const always_necessary_includes[] = {
        TRY(String::from_utf8("<AK/Error.h>"sv)),
        TRY(String::from_utf8("<AK/JsonValue.h>"sv)),
        TRY(String::from_utf8("<AK/NonnullRefPtr.h>"sv)),
        TRY(String::from_utf8("<AK/RefPtr.h>"sv)),
        // For Gfx::ColorRole
        TRY(String::from_utf8("<LibGfx/SystemTheme.h>"sv)),
        TRY(String::from_utf8("<LibGUI/Widget.h>"sv)),
    };
    TRY(necessary_includes.try_set_from(always_necessary_includes));
    for (auto const& include : necessary_includes)
        generator.appendln(TRY(String::formatted("#include {}", include)).bytes_as_string_view());

    // FIXME: Use a UTF-8 aware function once possible.
    generator.set("main_class_name", main_class.name());
    generator.append(function_start);
    TRY(generate_loader_for_object(main_class, TRY(generator.fork()), "main_object"_string, 2, UseObjectConstructor::No));

    generator.append(footer);
    return builder.to_string();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser argument_parser;
    StringView gml_file_name;
    argument_parser.add_positional_argument(gml_file_name, "GML file to compile", "GML_FILE", Core::ArgsParser::Required::Yes);
    argument_parser.parse(arguments);

    auto gml_text = TRY(TRY(Core::File::open(gml_file_name, Core::File::OpenMode::Read))->read_until_eof());
    auto parsed_gml = TRY(GUI::GML::parse_gml(gml_text));
    auto generated_cpp = TRY(generate_cpp(parsed_gml, LexicalPath { gml_file_name }));
    outln("{}", generated_cpp);
    return 0;
}
