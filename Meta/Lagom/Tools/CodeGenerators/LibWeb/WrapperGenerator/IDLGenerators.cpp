/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Queue.h>
#include <AK/QuickSort.h>
#include <LibIDL/Types.h>

Vector<StringView> s_header_search_paths;

namespace IDL {

// FIXME: Generate this automatically somehow.
static bool is_platform_object(Type const& type)
{
    // NOTE: This is a hand-curated subset of platform object types that are actually relevant
    // in places where this function is used. If you add IDL code and get compile errors, you
    // might simply need to add another type here.
    static constexpr Array types = {
        "AbortSignal"sv,
        "Attr"sv,
        "Blob"sv,
        "CanvasRenderingContext2D"sv,
        "Document"sv,
        "DocumentType"sv,
        "EventTarget"sv,
        "ImageData"sv,
        "MutationRecord"sv,
        "NamedNodeMap"sv,
        "Node"sv,
        "Path2D"sv,
        "Range"sv,
        "Selection"sv,
        "Text"sv,
        "TextMetrics"sv,
        "URLSearchParams"sv,
        "WebGLRenderingContext"sv,
        "Window"sv,
    };
    if (type.name().ends_with("Element"sv))
        return true;
    if (type.name().ends_with("Event"sv))
        return true;
    if (types.span().contains_slow(type.name()))
        return true;
    return false;
}

static StringView sequence_storage_type_to_cpp_storage_type_name(SequenceStorageType sequence_storage_type)
{
    switch (sequence_storage_type) {
    case SequenceStorageType::Vector:
        return "Vector"sv;
    case SequenceStorageType::MarkedVector:
        return "JS::MarkedVector"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

CppType idl_type_name_to_cpp_type(Type const& type, Interface const& interface);

static String union_type_to_variant(UnionType const& union_type, Interface const& interface)
{
    StringBuilder builder;
    builder.append("Variant<"sv);

    auto flattened_types = union_type.flattened_member_types();
    for (size_t type_index = 0; type_index < flattened_types.size(); ++type_index) {
        auto& type = flattened_types.at(type_index);

        if (type_index > 0)
            builder.append(", "sv);

        auto cpp_type = idl_type_name_to_cpp_type(type, interface);
        builder.append(cpp_type.name);
    }

    if (union_type.includes_undefined())
        builder.append(", Empty"sv);

    builder.append('>');
    return builder.to_string();
}

CppType idl_type_name_to_cpp_type(Type const& type, Interface const& interface)
{
    if (is_platform_object(type))
        return { .name = String::formatted("JS::Handle<{}>", type.name()), .sequence_storage_type = SequenceStorageType::MarkedVector };

    if (type.is_string())
        return { .name = "String", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name() == "double" && !type.is_nullable())
        return { .name = "double", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name() == "float" && !type.is_nullable())
        return { .name = "float", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name() == "boolean" && !type.is_nullable())
        return { .name = "bool", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name() == "unsigned long" && !type.is_nullable())
        return { .name = "u32", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name() == "unsigned short" && !type.is_nullable())
        return { .name = "u16", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name() == "long long" && !type.is_nullable())
        return { .name = "i64", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name() == "unsigned long long" && !type.is_nullable())
        return { .name = "u64", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name() == "long" && !type.is_nullable())
        return { .name = "i32", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name() == "any")
        return { .name = "JS::Value", .sequence_storage_type = SequenceStorageType::MarkedVector };

    if (type.name() == "BufferSource")
        return { .name = "JS::Handle<JS::Object>", .sequence_storage_type = SequenceStorageType::MarkedVector };

    if (type.name() == "sequence") {
        auto& parameterized_type = verify_cast<ParameterizedType>(type);
        auto& sequence_type = parameterized_type.parameters().first();
        auto sequence_cpp_type = idl_type_name_to_cpp_type(sequence_type, interface);
        auto storage_type_name = sequence_storage_type_to_cpp_storage_type_name(sequence_cpp_type.sequence_storage_type);

        if (sequence_cpp_type.sequence_storage_type == SequenceStorageType::MarkedVector)
            return { .name = storage_type_name, .sequence_storage_type = SequenceStorageType::Vector };

        return { .name = String::formatted("{}<{}>", storage_type_name, sequence_cpp_type.name), .sequence_storage_type = SequenceStorageType::Vector };
    }

    if (type.name() == "record") {
        auto& parameterized_type = verify_cast<ParameterizedType>(type);
        auto& record_key_type = parameterized_type.parameters()[0];
        auto& record_value_type = parameterized_type.parameters()[1];
        auto record_key_cpp_type = idl_type_name_to_cpp_type(record_key_type, interface);
        auto record_value_cpp_type = idl_type_name_to_cpp_type(record_value_type, interface);

        return { .name = String::formatted("OrderedHashMap<{}, {}>", record_key_cpp_type.name, record_value_cpp_type.name), .sequence_storage_type = SequenceStorageType::Vector };
    }

    if (is<UnionType>(type)) {
        auto& union_type = verify_cast<UnionType>(type);
        return { .name = union_type_to_variant(union_type, interface), .sequence_storage_type = SequenceStorageType::Vector };
    }

    if (!type.is_nullable()) {
        for (auto& dictionary : interface.dictionaries) {
            if (type.name() == dictionary.key)
                return { .name = type.name(), .sequence_storage_type = SequenceStorageType::Vector };
        }
    }

    dbgln("Unimplemented type for idl_type_name_to_cpp_type: {}{}", type.name(), type.is_nullable() ? "?" : "");
    TODO();
}

static String make_input_acceptable_cpp(String const& input)
{
    if (input.is_one_of("class", "template", "for", "default", "char", "namespace", "delete")) {
        StringBuilder builder;
        builder.append(input);
        builder.append('_');
        return builder.to_string();
    }

    return input.replace("-"sv, "_"sv, ReplaceMode::All);
}

static void generate_include_for_iterator(auto& generator, auto& iterator_path, auto& iterator_name)
{
    auto iterator_generator = generator.fork();
    iterator_generator.set("iterator_class.path", iterator_path);
    iterator_generator.set("iterator_class.name", iterator_name);
    // FIXME: These may or may not exist, because REASONS.
    iterator_generator.append(R"~~~(
//#if __has_include(<LibWeb/@iterator_class.path@.h>)
#   include <LibWeb/@iterator_class.path@.h>
//#endif
)~~~");
}

static void generate_include_for(auto& generator, auto& path)
{
    auto forked_generator = generator.fork();
    auto path_string = path;
    for (auto& search_path : s_header_search_paths) {
        if (!path.starts_with(search_path))
            continue;
        auto relative_path = LexicalPath::relative_path(path, search_path);
        if (relative_path.length() < path_string.length())
            path_string = relative_path;
    }

    LexicalPath include_path { path_string };
    forked_generator.set("include.path", String::formatted("{}/{}.h", include_path.dirname(), include_path.title()));
    forked_generator.append(R"~~~(
#include <@include.path@>
)~~~");
}

static void emit_includes_for_all_imports(auto& interface, auto& generator, bool is_iterator = false)
{
    Queue<RemoveCVReference<decltype(interface)> const*> interfaces;
    HashTable<String> paths_imported;

    interfaces.enqueue(&interface);

    while (!interfaces.is_empty()) {
        auto interface = interfaces.dequeue();
        if (paths_imported.contains(interface->module_own_path))
            continue;

        paths_imported.set(interface->module_own_path);
        for (auto& imported_interface : interface->imported_modules) {
            if (!paths_imported.contains(imported_interface.module_own_path))
                interfaces.enqueue(&imported_interface);
        }

        if (!interface->will_generate_code())
            continue;

        generate_include_for(generator, interface->module_own_path);

        if (is_iterator) {
            auto iterator_name = String::formatted("{}Iterator", interface->name);
            auto iterator_path = String::formatted("{}Iterator", interface->fully_qualified_name.replace("::"sv, "/"sv, ReplaceMode::All));
            generate_include_for_iterator(generator, iterator_path, iterator_name);
        }
    }
}

template<typename ParameterType>
static void generate_to_cpp(SourceGenerator& generator, ParameterType& parameter, String const& js_name, String const& js_suffix, String const& cpp_name, IDL::Interface const& interface, bool legacy_null_to_empty_string = false, bool optional = false, Optional<String> optional_default_value = {}, bool variadic = false, size_t recursion_depth = 0)
{
    auto scoped_generator = generator.fork();
    auto acceptable_cpp_name = make_input_acceptable_cpp(cpp_name);
    scoped_generator.set("cpp_name", acceptable_cpp_name);
    scoped_generator.set("js_name", js_name);
    scoped_generator.set("js_suffix", js_suffix);
    scoped_generator.set("legacy_null_to_empty_string", legacy_null_to_empty_string ? "true" : "false");
    scoped_generator.set("parameter.type.name", parameter.type->name());

    if (optional_default_value.has_value())
        scoped_generator.set("parameter.optional_default_value", *optional_default_value);

    // FIXME: Add support for optional, variadic, nullable and default values to all types
    if (parameter.type->is_string()) {
        if (variadic) {
            scoped_generator.append(R"~~~(
    Vector<String> @cpp_name@;
    @cpp_name@.ensure_capacity(vm.argument_count() - @js_suffix@);

    for (size_t i = @js_suffix@; i < vm.argument_count(); ++i) {
        auto to_string_result = TRY(vm.argument(i).to_string(vm));
        @cpp_name@.append(move(to_string_result));
    }
)~~~");
        } else if (!optional) {
            if (!parameter.type->is_nullable()) {
                scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (@js_name@@js_suffix@.is_null() && @legacy_null_to_empty_string@) {
        @cpp_name@ = String::empty();
    } else {
        @cpp_name@ = TRY(@js_name@@js_suffix@.to_string(vm));
    }
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (!@js_name@@js_suffix@.is_nullish())
        @cpp_name@ = TRY(@js_name@@js_suffix@.to_string(vm));
)~~~");
            }
        } else {
            scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (!@js_name@@js_suffix@.is_undefined()) {
        if (@js_name@@js_suffix@.is_null() && @legacy_null_to_empty_string@)
            @cpp_name@ = String::empty();
        else
            @cpp_name@ = TRY(@js_name@@js_suffix@.to_string(vm));
    })~~~");
            if (optional_default_value.has_value() && (!parameter.type->is_nullable() || optional_default_value.value() != "null")) {
                scoped_generator.append(R"~~~( else {
        @cpp_name@ = @parameter.optional_default_value@;
    }
)~~~");
            } else {
                scoped_generator.append(R"~~~(
)~~~");
            }
        }
    } else if (parameter.type->name().is_one_of("EventListener", "NodeFilter")) {
        // FIXME: Replace this with support for callback interfaces. https://heycam.github.io/webidl/#idl-callback-interface

        if (parameter.type->name() == "EventListener")
            scoped_generator.set("cpp_type", "IDLEventListener");
        else
            scoped_generator.set("cpp_type", parameter.type->name());

        if (parameter.type->is_nullable()) {
            scoped_generator.append(R"~~~(
    @cpp_type@* @cpp_name@ = nullptr;
    if (!@js_name@@js_suffix@.is_nullish()) {
        if (!@js_name@@js_suffix@.is_object())
            return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObject, @js_name@@js_suffix@.to_string_without_side_effects());

        auto* callback_type = vm.heap().allocate_without_realm<CallbackType>(@js_name@@js_suffix@.as_object(), HTML::incumbent_settings_object());
        @cpp_name@ = @cpp_type@::create(realm, *callback_type).ptr();
    }
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObject, @js_name@@js_suffix@.to_string_without_side_effects());

    auto* callback_type = vm.heap().allocate_without_realm<CallbackType>(@js_name@@js_suffix@.as_object(), HTML::incumbent_settings_object());
    auto @cpp_name@ = adopt_ref(*new @cpp_type@(move(callback_type)));
)~~~");
        }
    } else if (IDL::is_platform_object(*parameter.type)) {
        if (!parameter.type->is_nullable()) {
            if (!optional) {
                scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object() || !is<@parameter.type.name@>(@js_name@@js_suffix@.as_object()))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

    auto& @cpp_name@ = static_cast<@parameter.type.name@&>(@js_name@@js_suffix@.as_object()).impl();
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    Optional<JS::NonnullGCPtr<@parameter.type.name@>> @cpp_name@;
    if (!@js_name@@js_suffix@.is_undefined()) {
        if (!@js_name@@js_suffix@.is_object() || !is<@parameter.type.name@>(@js_name@@js_suffix@.as_object()))
            return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

        @cpp_name@ = static_cast<@parameter.type.name@&>(@js_name@@js_suffix@.as_object()).impl();
    }
)~~~");
            }
        } else {
            scoped_generator.append(R"~~~(
    @parameter.type.name@* @cpp_name@ = nullptr;
    if (!@js_name@@js_suffix@.is_nullish()) {
        if (!@js_name@@js_suffix@.is_object() || !is<@parameter.type.name@>(@js_name@@js_suffix@.as_object()))
            return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

        @cpp_name@ = &static_cast<@parameter.type.name@&>(@js_name@@js_suffix@.as_object()).impl();
    }
)~~~");
        }
    } else if (parameter.type->name() == "double" || parameter.type->name() == "float") {
        if (!optional) {
            scoped_generator.append(R"~~~(
    @parameter.type.name@ @cpp_name@ = TRY(@js_name@@js_suffix@.to_double(vm));
)~~~");
        } else {
            if (optional_default_value.has_value()) {
                scoped_generator.append(R"~~~(
    @parameter.type.name@ @cpp_name@;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    Optional<@parameter.type.name@> @cpp_name@;
)~~~");
            }
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
        @cpp_name@ = TRY(@js_name@@js_suffix@.to_double(vm));
)~~~");
            if (optional_default_value.has_value()) {
                scoped_generator.append(R"~~~(
    else
        @cpp_name@ = @parameter.optional_default_value@;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
)~~~");
            }
        }
    } else if (parameter.type->name() == "boolean") {
        if (!optional || optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    bool @cpp_name@;
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    Optional<bool> @cpp_name@;
)~~~");
        }
        if (optional) {
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
)~~~");
        }
        scoped_generator.append(R"~~~(
    @cpp_name@ = @js_name@@js_suffix@.to_boolean();
)~~~");
        if (optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    else
        @cpp_name@ = @parameter.optional_default_value@;
)~~~");
        }
    } else if (parameter.type->name() == "unsigned long") {
        if (!optional || optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    u32 @cpp_name@;
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    Optional<u32> @cpp_name@;
)~~~");
        }
        if (optional) {
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
)~~~");
        }
        scoped_generator.append(R"~~~(
    @cpp_name@ = TRY(@js_name@@js_suffix@.to_u32(vm));
)~~~");
        if (optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    else
        @cpp_name@ = @parameter.optional_default_value@UL;
)~~~");
        }
    } else if (parameter.type->name() == "unsigned short") {
        if (!optional || optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    u16 @cpp_name@;
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    Optional<u16> @cpp_name@;
)~~~");
        }
        if (optional) {
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
)~~~");
        }
        scoped_generator.append(R"~~~(
    @cpp_name@ = TRY(@js_name@@js_suffix@.to_u16(vm));
)~~~");
        if (optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    else
        @cpp_name@ = @parameter.optional_default_value@;
)~~~");
        }
    } else if (parameter.type->name() == "long") {
        if (!optional || optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    i32 @cpp_name@;
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    Optional<i32> @cpp_name@;
)~~~");
        }
        if (optional) {
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
)~~~");
        }
        scoped_generator.append(R"~~~(
    @cpp_name@ = TRY(@js_name@@js_suffix@.to_i32(vm));
)~~~");
        if (optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    else
        @cpp_name@ = @parameter.optional_default_value@L;
)~~~");
        }
    } else if (parameter.type->name() == "long long") {
        if (!optional || optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    i64 @cpp_name@;
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    Optional<i64> @cpp_name@;
)~~~");
        }
        if (optional) {
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
)~~~");
        }
        scoped_generator.append(R"~~~(
    @cpp_name@ = TRY(@js_name@@js_suffix@.to_bigint_int64(vm));
)~~~");
        if (optional_default_value.has_value()) {
            scoped_generator.append(R"~~~(
    else
        @cpp_name@ = @parameter.optional_default_value@L;
)~~~");
        }
    } else if (parameter.type->name() == "Promise") {
        // NOTE: It's not clear to me where the implicit wrapping of non-Promise values in a resolved
        // Promise is defined in the spec; https://webidl.spec.whatwg.org/#idl-promise doesn't say
        // anything of this sort. Both Gecko and Blink do it, however, so I'm sure it's correct.
        scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object() || !is<JS::Promise>(@js_name@@js_suffix@.as_object())) {
        auto* new_promise = JS::Promise::create(realm);
        new_promise->fulfill(@js_name@@js_suffix@);
        @js_name@@js_suffix@ = new_promise;
    }
    auto @cpp_name@ = JS::make_handle(&static_cast<JS::Promise&>(@js_name@@js_suffix@.as_object()));
)~~~");
    } else if (parameter.type->name() == "BufferSource") {
        scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object() || !(is<JS::TypedArrayBase>(@js_name@@js_suffix@.as_object()) || is<JS::ArrayBuffer>(@js_name@@js_suffix@.as_object()) || is<JS::DataView>(@js_name@@js_suffix@.as_object())))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

    // TODO: Should we make this a Variant?
    auto @cpp_name@ = JS::make_handle(&@js_name@@js_suffix@.as_object());
)~~~");
    } else if (parameter.type->name() == "any") {
        if (!optional) {
            scoped_generator.append(R"~~~(
    auto @cpp_name@ = @js_name@@js_suffix@;
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    JS::Value @cpp_name@ = JS::js_undefined();
    if (!@js_name@@js_suffix@.is_undefined())
        @cpp_name@ = @js_name@@js_suffix@;
)~~~");
            if (optional_default_value.has_value()) {
                if (optional_default_value == "null") {
                    scoped_generator.append(R"~~~(
    else
        @cpp_name@ = JS::js_null();
)~~~");
                } else if (optional_default_value->to_int().has_value() || optional_default_value->to_uint().has_value()) {
                    scoped_generator.append(R"~~~(
    else
        @cpp_name@ = JS::Value(@parameter.optional_default_value@);
)~~~");
                } else {
                    TODO();
                }
            }
        }
    } else if (interface.enumerations.contains(parameter.type->name())) {
        auto enum_generator = scoped_generator.fork();
        auto& enumeration = interface.enumerations.find(parameter.type->name())->value;
        StringView enum_member_name;
        if (optional_default_value.has_value()) {
            VERIFY(optional_default_value->length() >= 2 && (*optional_default_value)[0] == '"' && (*optional_default_value)[optional_default_value->length() - 1] == '"');
            enum_member_name = optional_default_value->substring_view(1, optional_default_value->length() - 2);
        } else {
            enum_member_name = enumeration.first_member;
        }
        auto default_value_cpp_name = enumeration.translated_cpp_names.get(enum_member_name);
        VERIFY(default_value_cpp_name.has_value());
        enum_generator.set("enum.default.cpp_value", *default_value_cpp_name);
        enum_generator.set("js_name.as_string", String::formatted("{}{}_string", enum_generator.get("js_name"sv), enum_generator.get("js_suffix"sv)));
        enum_generator.append(R"~~~(
    @parameter.type.name@ @cpp_name@ { @parameter.type.name@::@enum.default.cpp_value@ };
)~~~");

        if (optional) {
            enum_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined()) {
)~~~");
        }

        enum_generator.append(R"~~~(
    auto @js_name.as_string@ = TRY(@js_name@@js_suffix@.to_string(vm));
)~~~");
        auto first = true;
        for (auto& it : enumeration.translated_cpp_names) {
            enum_generator.set("enum.alt.name", it.key);
            enum_generator.set("enum.alt.value", it.value);
            enum_generator.set("else", first ? "" : "else ");
            first = false;

            enum_generator.append(R"~~~(
    @else@if (@js_name.as_string@ == "@enum.alt.name@"sv)
        @cpp_name@ = @parameter.type.name@::@enum.alt.value@;
)~~~");
        }

        // NOTE: Attribute setters return undefined instead of throwing when the string doesn't match an enum value.
        if constexpr (!IsSame<Attribute, RemoveConst<ParameterType>>) {
            enum_generator.append(R"~~~(
    @else@
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::InvalidEnumerationValue, @js_name.as_string@, "@parameter.type.name@");
)~~~");
        } else {
            enum_generator.append(R"~~~(
    @else@
        return JS::js_undefined();
)~~~");
        }

        if (optional) {
            enum_generator.append(R"~~~(
    }
)~~~");
        }
    } else if (interface.dictionaries.contains(parameter.type->name())) {
        if (optional_default_value.has_value() && optional_default_value != "{}")
            TODO();
        auto dictionary_generator = scoped_generator.fork();
        dictionary_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_nullish() && !@js_name@@js_suffix@.is_object())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

    @parameter.type.name@ @cpp_name@ {};
)~~~");
        auto* current_dictionary = &interface.dictionaries.find(parameter.type->name())->value;
        while (true) {
            for (auto& member : current_dictionary->members) {
                dictionary_generator.set("member_key", member.name);
                auto member_js_name = make_input_acceptable_cpp(member.name.to_snakecase());
                dictionary_generator.set("member_name", member_js_name);
                dictionary_generator.append(R"~~~(
    JS::Value @member_name@;
    if (@js_name@@js_suffix@.is_nullish()) {
        @member_name@ = JS::js_undefined();
    } else {
        @member_name@ = TRY(@js_name@@js_suffix@.as_object().get("@member_key@"));
    }
)~~~");
                if (member.required) {
                    dictionary_generator.append(R"~~~(
    if (@member_name@.is_undefined())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::MissingRequiredProperty, "@member_key@");
)~~~");
                }

                auto member_value_name = String::formatted("{}_value", member_js_name);
                dictionary_generator.set("member_value_name", member_value_name);
                generate_to_cpp(dictionary_generator, member, member_js_name, "", member_value_name, interface, member.extended_attributes.contains("LegacyNullToEmptyString"), !member.required, member.default_value);
                dictionary_generator.append(R"~~~(
    @cpp_name@.@member_name@ = @member_value_name@;
)~~~");
            }
            if (current_dictionary->parent_name.is_null())
                break;
            VERIFY(interface.dictionaries.contains(current_dictionary->parent_name));
            current_dictionary = &interface.dictionaries.find(current_dictionary->parent_name)->value;
        }
    } else if (interface.callback_functions.contains(parameter.type->name())) {
        // https://webidl.spec.whatwg.org/#es-callback-function

        auto callback_function_generator = scoped_generator.fork();
        auto& callback_function = interface.callback_functions.find(parameter.type->name())->value;

        // An ECMAScript value V is converted to an IDL callback function type value by running the following algorithm:
        // 1. If the result of calling IsCallable(V) is false and the conversion to an IDL value is not being performed due to V being assigned to an attribute whose type is a nullable callback function that is annotated with [LegacyTreatNonObjectAsNull], then throw a TypeError.
        if (!callback_function.is_legacy_treat_non_object_as_null) {
            callback_function_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_function())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAFunction, @js_name@@js_suffix@.to_string_without_side_effects());
)~~~");
        }
        // 2. Return the IDL callback function type value that represents a reference to the same object that V represents, with the incumbent settings object as the callback context.
        if (callback_function.is_legacy_treat_non_object_as_null) {
            callback_function_generator.append(R"~~~(
    Bindings::CallbackType* @cpp_name@ = nullptr;
    if (@js_name@@js_suffix@.is_object())
        @cpp_name@ = vm.heap().allocate_without_realm<CallbackType>(@js_name@@js_suffix@.as_object(), HTML::incumbent_settings_object());
)~~~");
        } else {
            callback_function_generator.append(R"~~~(
    auto @cpp_name@ = vm.heap().allocate_without_realm<CallbackType>(@js_name@@js_suffix@.as_object(), HTML::incumbent_settings_object());
)~~~");
        }
    } else if (parameter.type->name() == "sequence") {
        // https://webidl.spec.whatwg.org/#es-sequence

        auto sequence_generator = scoped_generator.fork();
        auto& parameterized_type = verify_cast<IDL::ParameterizedType>(*parameter.type);
        sequence_generator.set("recursion_depth", String::number(recursion_depth));

        // An ECMAScript value V is converted to an IDL sequence<T> value as follows:
        // 1. If Type(V) is not Object, throw a TypeError.
        // 2. Let method be ? GetMethod(V, @@iterator).
        // 3. If method is undefined, throw a TypeError.
        // 4. Return the result of creating a sequence from V and method.

        if (optional) {
            auto sequence_cpp_type = idl_type_name_to_cpp_type(parameterized_type.parameters().first(), interface);
            sequence_generator.set("sequence.type", sequence_cpp_type.name);
            sequence_generator.set("sequence.storage_type", sequence_storage_type_to_cpp_storage_type_name(sequence_cpp_type.sequence_storage_type));

            if (!optional_default_value.has_value()) {
                if (sequence_cpp_type.sequence_storage_type == IDL::SequenceStorageType::Vector) {
                    sequence_generator.append(R"~~~(
    Optional<@sequence.storage_type@<@sequence.type@>> @cpp_name@;
)~~~");
                } else {
                    sequence_generator.append(R"~~~(
    Optional<@sequence.storage_type@> @cpp_name@;
)~~~");
                }
            } else {
                if (optional_default_value != "[]")
                    TODO();

                if (sequence_cpp_type.sequence_storage_type == IDL::SequenceStorageType::Vector) {
                    sequence_generator.append(R"~~~(
    @sequence.storage_type@<@sequence.type@> @cpp_name@;
)~~~");
                } else {
                    sequence_generator.append(R"~~~(
    @sequence.storage_type@ @cpp_name@ { global_object.heap() };
)~~~");
                }
            }

            sequence_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined()) {
)~~~");
        }

        sequence_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObject, @js_name@@js_suffix@.to_string_without_side_effects());

    auto* iterator_method@recursion_depth@ = TRY(@js_name@@js_suffix@.get_method(vm, *vm.well_known_symbol_iterator()));
    if (!iterator_method@recursion_depth@)
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotIterable, @js_name@@js_suffix@.to_string_without_side_effects());
)~~~");

        parameterized_type.generate_sequence_from_iterable(sequence_generator, String::formatted("{}{}", acceptable_cpp_name, optional ? "_non_optional" : ""), String::formatted("{}{}", js_name, js_suffix), String::formatted("iterator_method{}", recursion_depth), interface, recursion_depth + 1);

        if (optional) {
            sequence_generator.append(R"~~~(
        @cpp_name@ = move(@cpp_name@_non_optional);
    }
)~~~");
        }
    } else if (parameter.type->name() == "record") {
        // https://webidl.spec.whatwg.org/#es-record

        auto record_generator = scoped_generator.fork();
        auto& parameterized_type = verify_cast<IDL::ParameterizedType>(*parameter.type);
        record_generator.set("recursion_depth", String::number(recursion_depth));

        // A record can only have two types: key type and value type.
        VERIFY(parameterized_type.parameters().size() == 2);

        // A record only allows the key to be a string.
        VERIFY(parameterized_type.parameters()[0].is_string());

        // An ECMAScript value O is converted to an IDL record<K, V> value as follows:
        // 1. If Type(O) is not Object, throw a TypeError.
        // 2. Let result be a new empty instance of record<K, V>.
        // 3. Let keys be ? O.[[OwnPropertyKeys]]().
        // 4. For each key of keys:
        //    1. Let desc be ? O.[[GetOwnProperty]](key).
        //    2. If desc is not undefined and desc.[[Enumerable]] is true:
        //       1. Let typedKey be key converted to an IDL value of type K.
        //       2. Let value be ? Get(O, key).
        //       3. Let typedValue be value converted to an IDL value of type V.
        //       4. Set result[typedKey] to typedValue.
        // 5. Return result.

        auto record_cpp_type = IDL::idl_type_name_to_cpp_type(parameterized_type, interface);
        record_generator.set("record.type", record_cpp_type.name);

        // If this is a recursive call to generate_to_cpp, assume that the caller has already handled converting the JS value to an object for us.
        // This affects record types in unions for example.
        if (recursion_depth == 0) {
            record_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObject, @js_name@@js_suffix@.to_string_without_side_effects());

    auto& @js_name@@js_suffix@_object = @js_name@@js_suffix@.as_object();
)~~~");
        }

        record_generator.append(R"~~~(
    @record.type@ @cpp_name@;

    auto record_keys@recursion_depth@ = TRY(@js_name@@js_suffix@_object.internal_own_property_keys());

    for (auto& key@recursion_depth@ : record_keys@recursion_depth@) {
        auto property_key@recursion_depth@ = MUST(JS::PropertyKey::from_value(vm, key@recursion_depth@));

        auto descriptor@recursion_depth@ = TRY(@js_name@@js_suffix@_object.internal_get_own_property(property_key@recursion_depth@));

        if (!descriptor@recursion_depth@.has_value() || !descriptor@recursion_depth@->enumerable.has_value() || !descriptor@recursion_depth@->enumerable.value())
            continue;
)~~~");

        IDL::Parameter key_parameter { .type = parameterized_type.parameters()[0], .name = acceptable_cpp_name, .optional_default_value = {}, .extended_attributes = {} };
        generate_to_cpp(record_generator, key_parameter, "key", String::number(recursion_depth), String::formatted("typed_key{}", recursion_depth), interface, false, false, {}, false, recursion_depth + 1);

        record_generator.append(R"~~~(
        auto value@recursion_depth@ = TRY(@js_name@@js_suffix@_object.get(property_key@recursion_depth@));
)~~~");

        // FIXME: Record value types should be TypeWithExtendedAttributes, which would allow us to get [LegacyNullToEmptyString] here.
        IDL::Parameter value_parameter { .type = parameterized_type.parameters()[1], .name = acceptable_cpp_name, .optional_default_value = {}, .extended_attributes = {} };
        generate_to_cpp(record_generator, value_parameter, "value", String::number(recursion_depth), String::formatted("typed_value{}", recursion_depth), interface, false, false, {}, false, recursion_depth + 1);

        record_generator.append(R"~~~(
        @cpp_name@.set(typed_key@recursion_depth@, typed_value@recursion_depth@);
    }
)~~~");
    } else if (is<IDL::UnionType>(*parameter.type)) {
        // https://webidl.spec.whatwg.org/#es-union

        auto union_generator = scoped_generator.fork();

        auto& union_type = verify_cast<IDL::UnionType>(*parameter.type);
        union_generator.set("union_type", union_type_to_variant(union_type, interface));
        union_generator.set("recursion_depth", String::number(recursion_depth));

        // NOTE: This is handled out here as we need the dictionary conversion code for the {} optional default value.
        // 3. Let types be the flattened member types of the union type.
        auto types = union_type.flattened_member_types();

        RefPtr<Type> dictionary_type;
        for (auto& dictionary : interface.dictionaries) {
            for (auto& type : types) {
                if (type.name() == dictionary.key) {
                    dictionary_type = type;
                    break;
                }
            }

            if (dictionary_type)
                break;
        }

        if (dictionary_type) {
            auto dictionary_generator = union_generator.fork();
            dictionary_generator.set("dictionary.type", dictionary_type->name());

            // The lambda must take the JS::Value to convert as a parameter instead of capturing it in order to support union types being variadic.
            dictionary_generator.append(R"~~~(
    auto @js_name@@js_suffix@_to_dictionary = [&vm](JS::Value @js_name@@js_suffix@) -> JS::ThrowCompletionOr<@dictionary.type@> {
)~~~");

            IDL::Parameter dictionary_parameter { .type = *dictionary_type, .name = acceptable_cpp_name, .optional_default_value = {}, .extended_attributes = {} };
            generate_to_cpp(dictionary_generator, dictionary_parameter, js_name, js_suffix, "dictionary_union_type"sv, interface, false, false, {}, false, recursion_depth + 1);

            dictionary_generator.append(R"~~~(
        return dictionary_union_type;
    };
)~~~");
        }

        // A lambda is used because Variants without "Empty" can't easily be default initialized.
        // Plus, this would require the user of union types to always accept a Variant with an Empty type.

        // Additionally, it handles the case of unconditionally throwing a TypeError at the end if none of the types match.
        // This is because we cannot unconditionally throw in generate_to_cpp as generate_to_cpp is supposed to assign to a variable and then continue.
        // Note that all the other types only throw on a condition.

        // The lambda must take the JS::Value to convert as a parameter instead of capturing it in order to support union types being variadic.

        StringBuilder to_variant_captures;
        to_variant_captures.append("&vm, &realm"sv);

        if (dictionary_type)
            to_variant_captures.append(String::formatted(", &{}{}_to_dictionary", js_name, js_suffix));

        union_generator.set("to_variant_captures", to_variant_captures.to_string());

        union_generator.append(R"~~~(
    auto @js_name@@js_suffix@_to_variant = [@to_variant_captures@](JS::Value @js_name@@js_suffix@) -> JS::ThrowCompletionOr<@union_type@> {
        // These might be unused.
        (void)vm;
        (void)realm;
)~~~");

        // 1. If the union type includes undefined and V is undefined, then return the unique undefined value.
        if (union_type.includes_undefined()) {
            scoped_generator.append(R"~~~(
        if (@js_name@@js_suffix@.is_undefined())
            return Empty {};
)~~~");
        }

        // FIXME: 2. If the union type includes a nullable type and V is null or undefined, then return the IDL value null.
        if (union_type.includes_nullable_type()) {
            dbgln("FIXME: 2. If the union type includes a nullable type and V is null or undefined, then return the IDL value null.");
        } else if (dictionary_type) {
            // 4. If V is null or undefined, then
            //    4.1 If types includes a dictionary type, then return the result of converting V to that dictionary type.
            union_generator.append(R"~~~(
        if (@js_name@@js_suffix@.is_nullish())
            return @union_type@ { TRY(@js_name@@js_suffix@_to_dictionary(@js_name@@js_suffix@)) };
)~~~");
        }

        bool includes_object = false;
        for (auto& type : types) {
            if (type.name() == "object") {
                includes_object = true;
                break;
            }
        }

        // FIXME: Don't generate this if the union type doesn't include any object types.
        union_generator.append(R"~~~(
        if (@js_name@@js_suffix@.is_object()) {
            [[maybe_unused]] auto& @js_name@@js_suffix@_object = @js_name@@js_suffix@.as_object();
)~~~");

        bool includes_platform_object = false;
        for (auto& type : types) {
            if (IDL::is_platform_object(type)) {
                includes_platform_object = true;
                break;
            }
        }

        if (includes_platform_object) {
            // 5. If V is a platform object, then:
            union_generator.append(R"~~~(
            if (is<PlatformObject>(@js_name@@js_suffix@_object)) {
)~~~");

            // NOTE: This codegen assumes that all union types are cells or values we can create a handle for.

            //    1. If types includes an interface type that V implements, then return the IDL value that is a reference to the object V.
            for (auto& type : types) {
                if (!IDL::is_platform_object(type))
                    continue;

                auto union_platform_object_type_generator = union_generator.fork();
                union_platform_object_type_generator.set("platform_object_type", type.name());

                union_platform_object_type_generator.append(R"~~~(
                if (is<@platform_object_type@>(@js_name@@js_suffix@_object))
                    return JS::make_handle(static_cast<@platform_object_type@&>(@js_name@@js_suffix@_object));
)~~~");
            }

            //    2. If types includes object, then return the IDL value that is a reference to the object V.
            if (includes_object) {
                union_generator.append(R"~~~(
                return JS::make_handle(@js_name@@js_suffix@_object);
)~~~");
            }

            union_generator.append(R"~~~(
            }
)~~~");
        }

        // 6. If Type(V) is Object and V has an [[ArrayBufferData]] internal slot, then
        //    1. If types includes ArrayBuffer, then return the result of converting V to ArrayBuffer.
        for (auto& type : types) {
            if (type.name() == "BufferSource") {
                union_generator.append(R"~~~(
            if (is<JS::ArrayBuffer>(@js_name@@js_suffix@_object))
                return JS::make_handle(@js_name@@js_suffix@_object);
)~~~");
            }
        }
        //    2. If types includes object, then return the IDL value that is a reference to the object V.
        if (includes_object) {
            union_generator.append(R"~~~(
            return @js_name@@js_suffix@_object;
)~~~");
        }

        // FIXME: 7. If Type(V) is Object and V has a [[DataView]] internal slot, then:
        //           1. If types includes DataView, then return the result of converting V to DataView.
        //           2. If types includes object, then return the IDL value that is a reference to the object V.

        // FIXME: 8. If Type(V) is Object and V has a [[TypedArrayName]] internal slot, then:
        //           1. If types includes a typed array type whose name is the value of V’s [[TypedArrayName]] internal slot, then return the result of converting V to that type.
        //           2. If types includes object, then return the IDL value that is a reference to the object V.

        // FIXME: 9. If IsCallable(V) is true, then:
        //           1. If types includes a callback function type, then return the result of converting V to that callback function type.
        //           2. If types includes object, then return the IDL value that is a reference to the object V.

        // 10. If Type(V) is Object, then:
        //     1. If types includes a sequence type, then:
        RefPtr<IDL::ParameterizedType> sequence_type;
        for (auto& type : types) {
            if (type.name() == "sequence") {
                sequence_type = verify_cast<IDL::ParameterizedType>(type);
                break;
            }
        }

        if (sequence_type) {
            // 1. Let method be ? GetMethod(V, @@iterator).
            union_generator.append(R"~~~(
        auto* method = TRY(@js_name@@js_suffix@.get_method(vm, *vm.well_known_symbol_iterator()));
)~~~");

            // 2. If method is not undefined, return the result of creating a sequence of that type from V and method.
            union_generator.append(R"~~~(
        if (method) {
)~~~");

            sequence_type->generate_sequence_from_iterable(union_generator, acceptable_cpp_name, String::formatted("{}{}", js_name, js_suffix), "method", interface, recursion_depth + 1);

            union_generator.append(R"~~~(

            return @cpp_name@;
        }
)~~~");
        }

        // FIXME: 2. If types includes a frozen array type, then
        //           1. Let method be ? GetMethod(V, @@iterator).
        //           2. If method is not undefined, return the result of creating a frozen array of that type from V and method.

        // 3. If types includes a dictionary type, then return the result of converting V to that dictionary type.
        if (dictionary_type) {
            union_generator.append(R"~~~(
        return @union_type@ { TRY(@js_name@@js_suffix@_to_dictionary(@js_name@@js_suffix@)) };
)~~~");
        }

        // 4. If types includes a record type, then return the result of converting V to that record type.
        RefPtr<IDL::ParameterizedType> record_type;
        for (auto& type : types) {
            if (type.name() == "record") {
                record_type = verify_cast<IDL::ParameterizedType>(type);
                break;
            }
        }

        if (record_type) {
            IDL::Parameter record_parameter { .type = *record_type, .name = acceptable_cpp_name, .optional_default_value = {}, .extended_attributes = {} };
            generate_to_cpp(union_generator, record_parameter, js_name, js_suffix, "record_union_type"sv, interface, false, false, {}, false, recursion_depth + 1);

            union_generator.append(R"~~~(
        return record_union_type;
)~~~");
        }

        // FIXME: 5. If types includes a callback interface type, then return the result of converting V to that callback interface type.

        // 6. If types includes object, then return the IDL value that is a reference to the object V.
        if (includes_object) {
            union_generator.append(R"~~~(
        return @js_name@@js_suffix@_object;
)~~~");
        }

        // End of is_object.
        union_generator.append(R"~~~(
        }
)~~~");

        // 11. If Type(V) is Boolean, then:
        //     1. If types includes boolean, then return the result of converting V to boolean.
        bool includes_boolean = false;
        for (auto& type : types) {
            if (type.name() == "boolean") {
                includes_boolean = true;
                break;
            }
        }

        if (includes_boolean) {
            union_generator.append(R"~~~(
        if (@js_name@@js_suffix@.is_boolean())
            return @union_type@ { @js_name@@js_suffix@.as_bool() };
)~~~");
        }

        RefPtr<IDL::Type> numeric_type;
        for (auto& type : types) {
            if (type.is_numeric()) {
                numeric_type = type;
                break;
            }
        }

        // 12. If Type(V) is Number, then:
        //     1. If types includes a numeric type, then return the result of converting V to that numeric type.
        if (numeric_type) {
            union_generator.append(R"~~~(
        if (@js_name@@js_suffix@.is_number()) {
)~~~");
            // NOTE: generate_to_cpp doesn't use the parameter name.
            // NOTE: generate_to_cpp will use to_{u32,etc.} which uses to_number internally and will thus use TRY, but it cannot throw as we know we are dealing with a number.
            IDL::Parameter parameter { .type = *numeric_type, .name = String::empty(), .optional_default_value = {}, .extended_attributes = {} };
            generate_to_cpp(union_generator, parameter, js_name, js_suffix, String::formatted("{}{}_number", js_name, js_suffix), interface, false, false, {}, false, recursion_depth + 1);

            union_generator.append(R"~~~(
            return @js_name@@js_suffix@_number;
        }
)~~~");
        }

        // 13. If Type(V) is BigInt, then:
        //     1. If types includes bigint, then return the result of converting V to bigint
        bool includes_bigint = false;
        for (auto& type : types) {
            if (type.name() == "bigint") {
                includes_bigint = true;
                break;
            }
        }

        if (includes_bigint) {
            union_generator.append(R"~~~(
        if (@js_name@@js_suffix@.is_bigint())
            return @js_name@@js_suffix@.as_bigint();
)~~~");
        }

        bool includes_string = false;
        for (auto& type : types) {
            if (type.is_string()) {
                includes_string = true;
                break;
            }
        }

        if (includes_string) {
            // 14. If types includes a string type, then return the result of converting V to that type.
            // NOTE: Currently all string types are converted to String.
            union_generator.append(R"~~~(
        return TRY(@js_name@@js_suffix@.to_string(vm));
)~~~");
        } else if (numeric_type && includes_bigint) {
            // 15. If types includes a numeric type and bigint, then return the result of converting V to either that numeric type or bigint.
            // https://webidl.spec.whatwg.org/#converted-to-a-numeric-type-or-bigint
            // NOTE: This algorithm is only used here.

            // An ECMAScript value V is converted to an IDL numeric type T or bigint value by running the following algorithm:
            // 1. Let x be ? ToNumeric(V).
            // 2. If Type(x) is BigInt, then
            //    1. Return the IDL bigint value that represents the same numeric value as x.
            // 3. Assert: Type(x) is Number.
            // 4. Return the result of converting x to T.

            auto union_numeric_type_generator = union_generator.fork();
            auto cpp_type = IDL::idl_type_name_to_cpp_type(*numeric_type, interface);
            union_numeric_type_generator.set("numeric_type", cpp_type.name);

            union_numeric_type_generator.append(R"~~~(
        auto x = TRY(@js_name@@js_suffix@.to_numeric(vm));
        if (x.is_bigint())
            return x.as_bigint();
        VERIFY(x.is_number());
)~~~");

            // NOTE: generate_to_cpp doesn't use the parameter name.
            // NOTE: generate_to_cpp will use to_{u32,etc.} which uses to_number internally and will thus use TRY, but it cannot throw as we know we are dealing with a number.
            IDL::Parameter parameter { .type = *numeric_type, .name = String::empty(), .optional_default_value = {}, .extended_attributes = {} };
            generate_to_cpp(union_numeric_type_generator, parameter, "x", String::empty(), "x_number", interface, false, false, {}, false, recursion_depth + 1);

            union_numeric_type_generator.append(R"~~~(
        return x_number;
)~~~");
        } else if (numeric_type) {
            // 16. If types includes a numeric type, then return the result of converting V to that numeric type.

            // NOTE: generate_to_cpp doesn't use the parameter name.
            // NOTE: generate_to_cpp will use to_{u32,etc.} which uses to_number internally and will thus use TRY, but it cannot throw as we know we are dealing with a number.
            IDL::Parameter parameter { .type = *numeric_type, .name = String::empty(), .optional_default_value = {}, .extended_attributes = {} };
            generate_to_cpp(union_generator, parameter, js_name, js_suffix, String::formatted("{}{}_number", js_name, js_suffix), interface, false, false, {}, false, recursion_depth + 1);

            union_generator.append(R"~~~(
        return @js_name@@js_suffix@_number;
)~~~");
        } else if (includes_boolean) {
            // 17. If types includes boolean, then return the result of converting V to boolean.
            union_generator.append(R"~~~(
        return @union_type@ { @js_name@@js_suffix@.to_boolean() };
)~~~");
        } else if (includes_bigint) {
            // 18. If types includes bigint, then return the result of converting V to bigint.
            union_generator.append(R"~~~(
        return TRY(@js_name@@js_suffix@.to_bigint(vm));
)~~~");
        } else {
            // 19. Throw a TypeError.
            // FIXME: Replace the error message with something more descriptive.
            union_generator.append(R"~~~(
        return vm.throw_completion<JS::TypeError>("No union types matched");
)~~~");
        }

        // Close the lambda and then perform the conversion.
        union_generator.append(R"~~~(
    };
)~~~");

        if (!variadic) {
            if (!optional) {
                union_generator.append(R"~~~(
    @union_type@ @cpp_name@ = TRY(@js_name@@js_suffix@_to_variant(@js_name@@js_suffix@));
)~~~");
            } else {
                if (!optional_default_value.has_value() || optional_default_value == "null"sv) {
                    union_generator.append(R"~~~(
    Optional<@union_type@> @cpp_name@;
    if (!@js_name@@js_suffix@.is_nullish())
        @cpp_name@ = TRY(@js_name@@js_suffix@_to_variant(@js_name@@js_suffix@));
)~~~");
                } else {
                    if (optional_default_value == "\"\"") {
                        union_generator.append(R"~~~(
    @union_type@ @cpp_name@ = @js_name@@js_suffix@.is_undefined() ? String::empty() : TRY(@js_name@@js_suffix@_to_variant(@js_name@@js_suffix@));
)~~~");
                    } else if (optional_default_value == "{}") {
                        VERIFY(dictionary_type);
                        union_generator.append(R"~~~(
    @union_type@ @cpp_name@ = @js_name@@js_suffix@.is_undefined() ? TRY(@js_name@@js_suffix@_to_dictionary(@js_name@@js_suffix@)) : TRY(@js_name@@js_suffix@_to_variant(@js_name@@js_suffix@));
)~~~");
                    } else if (optional_default_value->to_int().has_value() || optional_default_value->to_uint().has_value()) {
                        union_generator.append(R"~~~(
    @union_type@ @cpp_name@ = @js_name@@js_suffix@.is_undefined() ? @parameter.optional_default_value@ : TRY(@js_name@@js_suffix@_to_variant(@js_name@@js_suffix@));
)~~~");
                    } else {
                        TODO();
                    }
                }
            }
        } else {
            union_generator.append(R"~~~(
        Vector<@union_type@> @cpp_name@;
        @cpp_name@.ensure_capacity(vm.argument_count() - @js_suffix@);

        for (size_t i = @js_suffix@; i < vm.argument_count(); ++i) {
            auto result = TRY(@js_name@@js_suffix@_to_variant(vm.argument(i)));
            @cpp_name@.append(move(result));
        }
    )~~~");
        }
    } else {
        dbgln("Unimplemented JS-to-C++ conversion: {}", parameter.type->name());
        VERIFY_NOT_REACHED();
    }
}

static void generate_argument_count_check(SourceGenerator& generator, String const& function_name, size_t argument_count)
{
    if (argument_count == 0)
        return;

    auto argument_count_check_generator = generator.fork();
    argument_count_check_generator.set("function.name", function_name);
    argument_count_check_generator.set("function.nargs", String::number(argument_count));

    if (argument_count == 1) {
        argument_count_check_generator.set(".bad_arg_count", "JS::ErrorType::BadArgCountOne");
        argument_count_check_generator.set(".arg_count_suffix", "");
    } else {
        argument_count_check_generator.set(".bad_arg_count", "JS::ErrorType::BadArgCountMany");
        argument_count_check_generator.set(".arg_count_suffix", String::formatted(", \"{}\"", argument_count));
    }

    argument_count_check_generator.append(R"~~~(
    if (vm.argument_count() < @function.nargs@)
        return vm.throw_completion<JS::TypeError>(@.bad_arg_count@, "@function.name@"@.arg_count_suffix@);
)~~~");
}

static void generate_arguments(SourceGenerator& generator, Vector<IDL::Parameter> const& parameters, StringBuilder& arguments_builder, IDL::Interface const& interface)
{
    auto arguments_generator = generator.fork();

    Vector<String> parameter_names;
    size_t argument_index = 0;
    for (auto& parameter : parameters) {
        parameter_names.append(make_input_acceptable_cpp(parameter.name.to_snakecase()));

        if (!parameter.variadic) {
            arguments_generator.set("argument.index", String::number(argument_index));
            arguments_generator.append(R"~~~(
    auto arg@argument.index@ = vm.argument(@argument.index@);
)~~~");
        }

        bool legacy_null_to_empty_string = parameter.extended_attributes.contains("LegacyNullToEmptyString");
        generate_to_cpp(generator, parameter, "arg", String::number(argument_index), parameter.name.to_snakecase(), interface, legacy_null_to_empty_string, parameter.optional, parameter.optional_default_value, parameter.variadic, 0);
        ++argument_index;
    }

    arguments_builder.join(", "sv, parameter_names);
}

// https://webidl.spec.whatwg.org/#create-sequence-from-iterable
void IDL::ParameterizedType::generate_sequence_from_iterable(SourceGenerator& generator, String const& cpp_name, String const& iterable_cpp_name, String const& iterator_method_cpp_name, IDL::Interface const& interface, size_t recursion_depth) const
{
    auto sequence_generator = generator.fork();
    sequence_generator.set("cpp_name", cpp_name);
    sequence_generator.set("iterable_cpp_name", iterable_cpp_name);
    sequence_generator.set("iterator_method_cpp_name", iterator_method_cpp_name);
    sequence_generator.set("recursion_depth", String::number(recursion_depth));
    auto sequence_cpp_type = idl_type_name_to_cpp_type(parameters().first(), interface);
    sequence_generator.set("sequence.type", sequence_cpp_type.name);
    sequence_generator.set("sequence.storage_type", sequence_storage_type_to_cpp_storage_type_name(sequence_cpp_type.sequence_storage_type));

    // To create an IDL value of type sequence<T> given an iterable iterable and an iterator getter method, perform the following steps:
    // 1. Let iter be ? GetIterator(iterable, sync, method).
    // 2. Initialize i to be 0.
    // 3. Repeat
    //      1. Let next be ? IteratorStep(iter).
    //      2. If next is false, then return an IDL sequence value of type sequence<T> of length i, where the value of the element at index j is Sj.
    //      3. Let nextItem be ? IteratorValue(next).
    //      4. Initialize Si to the result of converting nextItem to an IDL value of type T.
    //      5. Set i to i + 1.

    sequence_generator.append(R"~~~(
    auto iterator@recursion_depth@ = TRY(JS::get_iterator(vm, @iterable_cpp_name@, JS::IteratorHint::Sync, @iterator_method_cpp_name@));
)~~~");

    if (sequence_cpp_type.sequence_storage_type == SequenceStorageType::Vector) {
        sequence_generator.append(R"~~~(
    @sequence.storage_type@<@sequence.type@> @cpp_name@;
)~~~");
    } else {
        sequence_generator.append(R"~~~(
    @sequence.storage_type@ @cpp_name@ { vm.heap() };
)~~~");
    }

    sequence_generator.append(R"~~~(
    for (;;) {
        auto* next@recursion_depth@ = TRY(JS::iterator_step(vm, iterator@recursion_depth@));
        if (!next@recursion_depth@)
            break;

        auto next_item@recursion_depth@ = TRY(JS::iterator_value(vm, *next@recursion_depth@));
)~~~");

    // FIXME: Sequences types should be TypeWithExtendedAttributes, which would allow us to get [LegacyNullToEmptyString] here.
    IDL::Parameter parameter { .type = parameters().first(), .name = iterable_cpp_name, .optional_default_value = {}, .extended_attributes = {} };
    generate_to_cpp(sequence_generator, parameter, "next_item", String::number(recursion_depth), String::formatted("sequence_item{}", recursion_depth), interface, false, false, {}, false, recursion_depth);

    sequence_generator.append(R"~~~(
        @cpp_name@.append(sequence_item@recursion_depth@);
    }
)~~~");
}

enum class WrappingReference {
    No,
    Yes,
};

static void generate_wrap_statement(SourceGenerator& generator, String const& value, IDL::Type const& type, IDL::Interface const& interface, StringView result_expression, WrappingReference wrapping_reference = WrappingReference::No, size_t recursion_depth = 0)
{
    auto scoped_generator = generator.fork();
    scoped_generator.set("value", value);
    scoped_generator.set("type", type.name());
    scoped_generator.set("result_expression", result_expression);
    scoped_generator.set("recursion_depth", String::number(recursion_depth));

    if (type.name() == "undefined") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::js_undefined();
)~~~");
        return;
    }

    if (type.is_nullable() && !is<UnionType>(type)) {
        if (type.is_string()) {
            scoped_generator.append(R"~~~(
    if (@value@.is_null()) {
        @result_expression@ JS::js_null();
    } else {
)~~~");
        } else if (type.name() == "sequence") {
            scoped_generator.append(R"~~~(
    if (!@value@.has_value()) {
        @result_expression@ JS::js_null();
    } else {
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    if (!@value@) {
        @result_expression@ JS::js_null();
    } else {
)~~~");
        }
    }

    if (type.is_string()) {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::js_string(vm, @value@);
)~~~");
    } else if (type.name() == "sequence") {
        // https://webidl.spec.whatwg.org/#es-sequence
        auto& sequence_generic_type = verify_cast<IDL::ParameterizedType>(type);

        scoped_generator.append(R"~~~(
    auto* new_array@recursion_depth@ = MUST(JS::Array::create(realm, 0));
)~~~");

        if (!type.is_nullable()) {
            scoped_generator.append(R"~~~(
    for (size_t i@recursion_depth@ = 0; i@recursion_depth@ < @value@.size(); ++i@recursion_depth@) {
        auto& element@recursion_depth@ = @value@.at(i@recursion_depth@);
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    auto& @value@_non_optional = @value@.value();
    for (size_t i@recursion_depth@ = 0; i@recursion_depth@ < @value@_non_optional.size(); ++i@recursion_depth@) {
        auto& element@recursion_depth@ = @value@_non_optional.at(i@recursion_depth@);
)~~~");
        }

        // If the type is a platform object we currently return a Vector<JS::Handle<T>> from the
        // C++ implementation, thus allowing us to unwrap the element (a handle) like below.
        // This might need to change if we switch to a MarkedVector.
        if (is_platform_object(sequence_generic_type.parameters().first())) {
            scoped_generator.append(R"~~~(
            auto* wrapped_element@recursion_depth@ = &(*element@recursion_depth@);
)~~~");
        } else {
            generate_wrap_statement(scoped_generator, String::formatted("element{}", recursion_depth), sequence_generic_type.parameters().first(), interface, String::formatted("auto wrapped_element{} =", recursion_depth), WrappingReference::Yes, recursion_depth + 1);
        }

        scoped_generator.append(R"~~~(
        auto property_index@recursion_depth@ = JS::PropertyKey { i@recursion_depth@ };
        MUST(new_array@recursion_depth@->create_data_property(property_index@recursion_depth@, wrapped_element@recursion_depth@));
    }

    @result_expression@ new_array@recursion_depth@;
)~~~");
    } else if (type.name() == "boolean" || type.name() == "double" || type.name() == "float") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value(@value@);
)~~~");
    } else if (type.name() == "short" || type.name() == "long" || type.name() == "unsigned short") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value((i32)@value@);
)~~~");
    } else if (type.name() == "unsigned long") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value((u32)@value@);
)~~~");
    } else if (type.name() == "long long") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value((double)@value@);
)~~~");
    } else if (type.name() == "unsigned long long") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value((double)@value@);
)~~~");
    } else if (type.name() == "Location" || type.name() == "Promise" || type.name() == "Uint8Array" || type.name() == "Uint8ClampedArray" || type.name() == "any") {
        scoped_generator.append(R"~~~(
    @result_expression@ @value@;
)~~~");
    } else if (is<IDL::UnionType>(type)) {
        auto& union_type = verify_cast<IDL::UnionType>(type);
        auto union_types = union_type.flattened_member_types();
        auto union_generator = scoped_generator.fork();

        union_generator.append(R"~~~(
    @result_expression@ @value@.visit(
)~~~");

        for (size_t current_union_type_index = 0; current_union_type_index < union_types.size(); ++current_union_type_index) {
            auto& current_union_type = union_types.at(current_union_type_index);
            auto cpp_type = IDL::idl_type_name_to_cpp_type(current_union_type, interface);
            union_generator.set("current_type", cpp_type.name);
            union_generator.append(R"~~~(
        [&vm, &realm](@current_type@ const& visited_union_value@recursion_depth@) -> JS::Value {
            // These may be unused.
            (void)vm;
            (void) realm;
)~~~");

            // NOTE: While we are using const&, the underlying type for wrappable types in unions is (Nonnull)RefPtr, which are not references.
            generate_wrap_statement(union_generator, String::formatted("visited_union_value{}", recursion_depth), current_union_type, interface, "return"sv, WrappingReference::No, recursion_depth + 1);

            // End of current visit lambda.
            // The last lambda cannot have a trailing comma on the closing brace, unless the type is nullable, where an extra lambda will be generated for the Empty case.
            if (current_union_type_index != union_types.size() - 1 || type.is_nullable()) {
                union_generator.append(R"~~~(
        },
)~~~");
            } else {
                union_generator.append(R"~~~(
        }
)~~~");
            }
        }

        if (type.is_nullable()) {
            union_generator.append(R"~~~(
        [](Empty) -> JS::Value {
            return JS::js_null();
        }
)~~~");
        }

        // End of visit.
        union_generator.append(R"~~~(
    );
)~~~");
    } else if (interface.enumerations.contains(type.name())) {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::js_string(vm, Bindings::idl_enum_to_string(@value@));
)~~~");
    } else if (interface.callback_functions.contains(type.name())) {
        // https://webidl.spec.whatwg.org/#es-callback-function

        auto& callback_function = interface.callback_functions.find(type.name())->value;

        // The result of converting an IDL callback function type value to an ECMAScript value is a reference to the same object that the IDL callback function type value represents.

        if (callback_function.is_legacy_treat_non_object_as_null && !type.is_nullable()) {
            scoped_generator.append(R"~~~(
  if (!@value@) {
      @result_expression@ JS::js_null();
  } else {
      @result_expression@ &@value@->callback;
  }
)~~~");
        } else {
            scoped_generator.append(R"~~~(
  @result_expression@ &@value@->callback;
)~~~");
        }
    } else if (interface.dictionaries.contains(type.name())) {
        // https://webidl.spec.whatwg.org/#es-dictionary
        auto dictionary_generator = scoped_generator.fork();

        dictionary_generator.append(R"~~~(
    auto* dictionary_object@recursion_depth@ = JS::Object::create(realm, realm.intrinsics().object_prototype());
)~~~");

        auto* current_dictionary = &interface.dictionaries.find(type.name())->value;
        while (true) {
            for (auto& member : current_dictionary->members) {
                dictionary_generator.set("member_key", member.name);
                auto member_key_js_name = String::formatted("{}{}", make_input_acceptable_cpp(member.name.to_snakecase()), recursion_depth);
                dictionary_generator.set("member_name", member_key_js_name);
                auto member_value_js_name = String::formatted("{}_value", member_key_js_name);
                dictionary_generator.set("member_value", member_value_js_name);

                auto wrapped_value_name = String::formatted("auto wrapped_{}", member_value_js_name);
                dictionary_generator.set("wrapped_value_name", wrapped_value_name);
                generate_wrap_statement(dictionary_generator, String::formatted("{}.{}", value, member.name), member.type, interface, wrapped_value_name, WrappingReference::No, recursion_depth + 1);

                dictionary_generator.append(R"~~~(
    MUST(dictionary_object@recursion_depth@->create_data_property("@member_key@", @wrapped_value_name@));
)~~~");
            }

            if (current_dictionary->parent_name.is_null())
                break;
            VERIFY(interface.dictionaries.contains(current_dictionary->parent_name));
            current_dictionary = &interface.dictionaries.find(current_dictionary->parent_name)->value;
        }

        dictionary_generator.append(R"~~~(
    @result_expression@ dictionary_object@recursion_depth@;
)~~~");
    } else if (type.name() == "object") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value(const_cast<JS::Object*>(@value@));
)~~~");
    } else {
        if (wrapping_reference == WrappingReference::No) {
            scoped_generator.append(R"~~~(
    @result_expression@ &const_cast<@type@&>(*@value@);
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    @result_expression@ &const_cast<@type@&>(@value@);
)~~~");
        }
    }

    if (type.is_nullable() && !is<UnionType>(type)) {
        scoped_generator.append(R"~~~(
    }
)~~~");
    }
}

enum class StaticFunction {
    No,
    Yes,
};

static void generate_return_statement(SourceGenerator& generator, IDL::Type const& return_type, IDL::Interface const& interface)
{
    return generate_wrap_statement(generator, "retval", return_type, interface, "return"sv);
}

static void generate_variable_statement(SourceGenerator& generator, String const& variable_name, IDL::Type const& value_type, String const& value_name, IDL::Interface const& interface)
{
    auto variable_generator = generator.fork();
    variable_generator.set("variable_name", variable_name);
    variable_generator.append(R"~~~(
    JS::Value @variable_name@;
)~~~");
    return generate_wrap_statement(generator, value_name, value_type, interface, String::formatted("{} = ", variable_name));
}

static void generate_function(SourceGenerator& generator, IDL::Function const& function, StaticFunction is_static_function, String const& class_name, String const& interface_fully_qualified_name, IDL::Interface const& interface)
{
    auto function_generator = generator.fork();
    function_generator.set("class_name", class_name);
    function_generator.set("interface_fully_qualified_name", interface_fully_qualified_name);
    function_generator.set("function.name", function.name);
    function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
    function_generator.set("overload_suffix", function.is_overloaded ? String::number(function.overload_index) : String::empty());

    if (function.extended_attributes.contains("ImplementedAs")) {
        auto implemented_as = function.extended_attributes.get("ImplementedAs").value();
        function_generator.set("function.cpp_name", implemented_as);
    } else {
        function_generator.set("function.cpp_name", make_input_acceptable_cpp(function.name.to_snakecase()));
    }

    function_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@class_name@::@function.name:snakecase@@overload_suffix@)
{
    [[maybe_unused]] auto& realm = *vm.current_realm();
)~~~");

    if (is_static_function == StaticFunction::No) {
        function_generator.append(R"~~~(
    auto* impl = TRY(impl_from(vm));
)~~~");
    }

    // Optimization: overloaded functions' arguments count is checked by the overload arbiter
    if (!function.is_overloaded)
        generate_argument_count_check(generator, function.name, function.shortest_length());

    StringBuilder arguments_builder;
    generate_arguments(generator, function.parameters, arguments_builder, interface);
    function_generator.set(".arguments", arguments_builder.string_view());

    if (is_static_function == StaticFunction::No) {
        function_generator.append(R"~~~(
    [[maybe_unused]] auto retval = TRY(throw_dom_exception_if_needed(vm, [&] { return impl->@function.cpp_name@(@.arguments@); }));
)~~~");
    } else {
        function_generator.append(R"~~~(
    [[maybe_unused]] auto retval = TRY(throw_dom_exception_if_needed(vm, [&] { return @interface_fully_qualified_name@::@function.cpp_name@(@.arguments@); }));
)~~~");
    }

    generate_return_statement(generator, *function.return_type, interface);

    function_generator.append(R"~~~(
}
)~~~");
}

// https://webidl.spec.whatwg.org/#compute-the-effective-overload-set
static EffectiveOverloadSet compute_the_effective_overload_set(auto const& overload_set)
{
    // 1. Let S be an ordered set.
    Vector<EffectiveOverloadSet::Item> overloads;

    // 2. Let F be an ordered set with items as follows, according to the kind of effective overload set:
    // Note: This is determined by the caller of generate_overload_arbiter()

    // 3. Let maxarg be the maximum number of arguments the operations, legacy factory functions, or
    //    callback functions in F are declared to take. For variadic operations and legacy factory functions,
    //    the argument on which the ellipsis appears counts as a single argument.
    auto overloaded_functions = overload_set.value;
    auto maximum_arguments = 0;
    for (auto const& function : overloaded_functions)
        maximum_arguments = max(maximum_arguments, static_cast<int>(function.parameters.size()));

    // 4. Let max be max(maxarg, N).
    // NOTE: We don't do this step. `N` is a runtime value, so we just use `maxarg` here instead.
    //       Later, `generate_overload_arbiter()` produces individual overload sets for each possible N.

    // 5. For each operation or extended attribute X in F:
    auto overload_id = 0;
    for (auto const& overload : overloaded_functions) {
        // 1. Let arguments be the list of arguments X is declared to take.
        auto const& arguments = overload.parameters;

        // 2. Let n be the size of arguments.
        int argument_count = (int)arguments.size();

        // 3. Let types be a type list.
        NonnullRefPtrVector<Type> types;

        // 4. Let optionalityValues be an optionality list.
        Vector<Optionality> optionality_values;

        bool overload_is_variadic = false;

        // 5. For each argument in arguments:
        for (auto const& argument : arguments) {
            // 1. Append the type of argument to types.
            types.append(argument.type);

            // 2. Append "variadic" to optionalityValues if argument is a final, variadic argument, "optional" if argument is optional, and "required" otherwise.
            if (argument.variadic) {
                optionality_values.append(Optionality::Variadic);
                overload_is_variadic = true;
            } else if (argument.optional) {
                optionality_values.append(Optionality::Optional);
            } else {
                optionality_values.append(Optionality::Required);
            }
        }

        // 6. Append the tuple (X, types, optionalityValues) to S.
        overloads.empend(overload_id, types, optionality_values);

        // 7. If X is declared to be variadic, then:
        if (overload_is_variadic) {
            // 1. For each i in the range n to max − 1, inclusive:
            for (auto i = argument_count; i < maximum_arguments; ++i) {
                // 1. Let t be a type list.
                // 2. Let o be an optionality list.
                // NOTE: We hold both of these in an Item instead.
                EffectiveOverloadSet::Item item;
                item.callable_id = overload_id;

                // 3. For each j in the range 0 to n − 1, inclusive:
                for (auto j = 0; j < argument_count; ++j) {
                    // 1. Append types[j] to t.
                    item.types.append(types[j]);

                    // 2. Append optionalityValues[j] to o.
                    item.optionality_values.append(optionality_values[j]);
                }

                // 4. For each j in the range n to i, inclusive:
                for (auto j = argument_count; j <= i; ++j) {
                    // 1. Append types[n − 1] to t.
                    item.types.append(types[argument_count - 1]);

                    // 2. Append "variadic" to o.
                    item.optionality_values.append(Optionality::Variadic);
                }

                // 5. Append the tuple (X, t, o) to S.
                overloads.append(move(item));
            }
        }

        // 8. Let i be n − 1.
        auto i = argument_count - 1;

        // 9. While i ≥ 0:
        while (i >= 0) {
            // 1. If arguments[i] is not optional (i.e., it is not marked as "optional" and is not a final, variadic argument), then break.
            if (!arguments[i].optional && !arguments[i].variadic)
                break;

            // 2. Let t be a type list.
            // 3. Let o be an optionality list.
            // NOTE: We hold both of these in an Item instead.
            EffectiveOverloadSet::Item item;
            item.callable_id = overload_id;

            // 4. For each j in the range 0 to i − 1, inclusive:
            for (auto j = 0; j < i; ++j) {
                // 1. Append types[j] to t.
                item.types.append(types[j]);

                // 2. Append optionalityValues[j] to o.
                item.optionality_values.append(optionality_values[j]);
            }

            // 5. Append the tuple (X, t, o) to S.
            overloads.append(move(item));

            // 6. Set i to i − 1.
            --i;
        }

        overload_id++;
    }

    return EffectiveOverloadSet { move(overloads) };
}

static String generate_constructor_for_idl_type(Type const& type)
{
    auto append_type_list = [](auto& builder, auto const& type_list) {
        bool first = true;
        for (auto const& child_type : type_list) {
            if (first) {
                first = false;
            } else {
                builder.append(", "sv);
            }

            builder.append(generate_constructor_for_idl_type(child_type));
        }
    };

    switch (type.kind()) {
    case Type::Kind::Plain:
        return String::formatted("make_ref_counted<IDL::Type>(\"{}\", {})", type.name(), type.is_nullable());
    case Type::Kind::Parameterized: {
        auto const& parameterized_type = type.as_parameterized();
        StringBuilder builder;
        builder.appendff("make_ref_counted<IDL::ParameterizedTypeType>(\"{}\", {}, NonnullRefPtrVector<IDL::Type> {{", type.name(), type.is_nullable());
        append_type_list(builder, parameterized_type.parameters());
        builder.append("})"sv);
        return builder.to_string();
    }
    case Type::Kind::Union: {
        auto const& union_type = type.as_union();
        StringBuilder builder;
        builder.appendff("make_ref_counted<IDL::UnionType>(\"{}\", {}, NonnullRefPtrVector<IDL::Type> {{", type.name(), type.is_nullable());
        append_type_list(builder, union_type.member_types());
        builder.append("})"sv);
        return builder.to_string();
    }
    }

    VERIFY_NOT_REACHED();
}

static void generate_overload_arbiter(SourceGenerator& generator, auto const& overload_set, String const& class_name)
{
    auto function_generator = generator.fork();
    function_generator.set("class_name", class_name);
    function_generator.set("function.name:snakecase", make_input_acceptable_cpp(overload_set.key.to_snakecase()));

    function_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@class_name@::@function.name:snakecase@)
{
    Optional<IDL::EffectiveOverloadSet> effective_overload_set;
)~~~");

    auto all_possible_effective_overloads = compute_the_effective_overload_set(overload_set);
    auto overloads_set = all_possible_effective_overloads.items();
    auto maximum_argument_count = 0u;
    for (auto const& overload : overloads_set)
        maximum_argument_count = max(maximum_argument_count, overload.types.size());
    function_generator.set("max_argument_count", String::number(maximum_argument_count));
    function_generator.appendln("    switch (min(@max_argument_count@, vm.argument_count())) {");

    // Generate the effective overload set for each argument count.
    // This skips part of the Overload Resolution Algorithm https://webidl.spec.whatwg.org/#es-overloads
    // Namely, since that discards any overloads that don't have the exact number of arguments that were given,
    // we simply only provide the overloads that do have that number of arguments.
    for (auto argument_count = 0u; argument_count <= maximum_argument_count; ++argument_count) {
        // FIXME: Calculate the distinguishing argument index now instead of at runtime.

        auto effective_overload_count = 0;
        for (auto const& overload : overloads_set) {
            if (overload.types.size() == argument_count)
                effective_overload_count++;
        }

        if (effective_overload_count == 0)
            continue;

        function_generator.set("current_argument_count", String::number(argument_count));
        function_generator.set("overload_count", String::number(effective_overload_count));
        function_generator.appendln(R"~~~(
    case @current_argument_count@: {
        Vector<IDL::EffectiveOverloadSet::Item> overloads;
        overloads.ensure_capacity(@overload_count@);
)~~~");

        for (auto& overload : overloads_set) {
            if (overload.types.size() != argument_count)
                continue;

            StringBuilder types_builder;
            types_builder.append("NonnullRefPtrVector<IDL::Type> { "sv);
            StringBuilder optionality_builder;
            optionality_builder.append("Vector<IDL::Optionality> { "sv);

            for (auto i = 0u; i < overload.types.size(); ++i) {
                if (i > 0) {
                    types_builder.append(", "sv);
                    optionality_builder.append(", "sv);
                }

                types_builder.append(generate_constructor_for_idl_type(overload.types[i]));

                optionality_builder.append("IDL::Optionality::"sv);
                switch (overload.optionality_values[i]) {
                case Optionality::Required:
                    optionality_builder.append("Required"sv);
                    break;
                case Optionality::Optional:
                    optionality_builder.append("Optional"sv);
                    break;
                case Optionality::Variadic:
                    optionality_builder.append("Variadic"sv);
                    break;
                }
            }

            types_builder.append("}"sv);
            optionality_builder.append("}"sv);

            function_generator.set("overload.callable_id", String::number(overload.callable_id));
            function_generator.set("overload.types", types_builder.to_string());
            function_generator.set("overload.optionality_values", optionality_builder.to_string());

            function_generator.appendln("        overloads.empend(@overload.callable_id@, @overload.types@, @overload.optionality_values@);");
        }

        function_generator.append(R"~~~(
        effective_overload_set.emplace(move(overloads));
        break;
    }
)~~~");
    }

    function_generator.append(R"~~~(
    }

    if (!effective_overload_set.has_value())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::OverloadResolutionFailed);

    auto chosen_overload = TRY(resolve_overload(vm, effective_overload_set.value()));
    switch (chosen_overload.callable_id) {
)~~~");

    for (auto i = 0u; i < overload_set.value.size(); ++i) {
        function_generator.set("overload_id", String::number(i));
        function_generator.append(R"~~~(
    case @overload_id@:
        return @function.name:snakecase@@overload_id@(vm);
)~~~");
    }

    function_generator.append(R"~~~(
    default:
        VERIFY_NOT_REACHED();
    }
}
)~~~");
}

void generate_constructor_header(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("fully_qualified_name", interface.fully_qualified_name);
    generator.set("constructor_class", interface.constructor_class);
    generator.set("constructor_class:snakecase", interface.constructor_class.to_snakecase());

    generator.append(R"~~~(
#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace Web::Bindings {

class @constructor_class@ : public JS::NativeFunction {
    JS_OBJECT(@constructor_class@, JS::NativeFunction);
public:
    explicit @constructor_class@(JS::Realm&);
    virtual void initialize(JS::Realm&) override;
    virtual ~@constructor_class@() override;

    virtual JS::ThrowCompletionOr<JS::Value> call() override;
    virtual JS::ThrowCompletionOr<JS::Object*> construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
)~~~");

    for (auto const& overload_set : interface.static_overload_sets) {
        auto function_generator = generator.fork();
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(overload_set.key.to_snakecase()));
        function_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@function.name:snakecase@);
)~~~");
        if (overload_set.value.size() > 1) {
            for (auto i = 0u; i < overload_set.value.size(); ++i) {
                function_generator.set("overload_suffix", String::number(i));
                function_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@function.name:snakecase@@overload_suffix@);
)~~~");
            }
        }
    }

    generator.append(R"~~~(
};

} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

void generate_constructor_implementation(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("prototype_class", interface.prototype_class);
    generator.set("constructor_class", interface.constructor_class);
    generator.set("prototype_class:snakecase", interface.prototype_class.to_snakecase());
    generator.set("fully_qualified_name", interface.fully_qualified_name);

    generator.append(R"~~~(
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibWeb/Bindings/@constructor_class@.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/HTML/Window.h>
#if __has_include(<LibWeb/Crypto/@name@.h>)
#    include <LibWeb/Crypto/@name@.h>
#elif __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/Encoding/@name@.h>)
#    include <LibWeb/Encoding/@name@.h>
#elif __has_include(<LibWeb/Fetch/@name@.h>)
#    include <LibWeb/Fetch/@name@.h>
#elif __has_include(<LibWeb/FileAPI/@name@.h>)
#    include <LibWeb/FileAPI/@name@.h>
#elif __has_include(<LibWeb/Geometry/@name@.h>)
#    include <LibWeb/Geometry/@name@.h>
#elif __has_include(<LibWeb/HTML/@name@.h>)
#    include <LibWeb/HTML/@name@.h>
#elif __has_include(<LibWeb/UIEvents/@name@.h>)
#    include <LibWeb/UIEvents/@name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@name@.h>)
#    include <LibWeb/HighResolutionTime/@name@.h>
#elif __has_include(<LibWeb/IntersectionObserver/@name@.h>)
#    include <LibWeb/IntersectionObserver/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@name@.h>)
#    include <LibWeb/NavigationTiming/@name@.h>
#elif __has_include(<LibWeb/RequestIdleCallback/@name@.h>)
#    include <LibWeb/RequestIdleCallback/@name@.h>
#elif __has_include(<LibWeb/ResizeObserver/@name@.h>)
#    include <LibWeb/ResizeObserver/@name@.h>
#elif __has_include(<LibWeb/SVG/@name@.h>)
#    include <LibWeb/SVG/@name@.h>
#elif __has_include(<LibWeb/Selection/@name@.h>)
#    include <LibWeb/Selection/@name@.h>
#elif __has_include(<LibWeb/WebSockets/@name@.h>)
#    include <LibWeb/WebSockets/@name@.h>
#elif __has_include(<LibWeb/XHR/@name@.h>)
#    include <LibWeb/XHR/@name@.h>
#elif __has_include(<LibWeb/URL/@name@.h>)
#    include <LibWeb/URL/@name@.h>
#endif

)~~~");

    for (auto& path : interface.required_imported_paths)
        generate_include_for(generator, path);

    emit_includes_for_all_imports(interface, generator, interface.pair_iterator_types.has_value());

    generator.append(R"~~~(
// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::DOMParsing;
using namespace Web::Fetch;
using namespace Web::FileAPI;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;
using namespace Web::UIEvents;
using namespace Web::XHR;
using namespace Web::WebGL;

namespace Web::Bindings {

@constructor_class@::@constructor_class@(JS::Realm& realm)
    : NativeFunction(*realm.intrinsics().function_prototype())
{
}

@constructor_class@::~@constructor_class@()
{
}

JS::ThrowCompletionOr<JS::Value> @constructor_class@::call()
{
    return vm().throw_completion<JS::TypeError>(JS::ErrorType::ConstructorWithoutNew, "@name@");
}

JS::ThrowCompletionOr<JS::Object*> @constructor_class@::construct(FunctionObject&)
{
)~~~");

    if (interface.constructors.is_empty()) {
        // No constructor
        generator.set("constructor.length", "0");
        generator.append(R"~~~(
    return vm().throw_completion<JS::TypeError>(JS::ErrorType::NotAConstructor, "@name@");
)~~~");
    } else if (interface.constructors.size() == 1) {
        // Single constructor

        auto& constructor = interface.constructors[0];
        generator.set("constructor.length", String::number(constructor.shortest_length()));

        generator.append(R"~~~(
    auto& vm = this->vm();
    [[maybe_unused]] auto& realm = *vm.current_realm();

    auto& window = verify_cast<HTML::Window>(realm.global_object());
)~~~");

        if (!constructor.parameters.is_empty()) {
            generate_argument_count_check(generator, constructor.name, constructor.shortest_length());

            StringBuilder arguments_builder;
            generate_arguments(generator, constructor.parameters, arguments_builder, interface);
            generator.set(".constructor_arguments", arguments_builder.string_view());

            generator.append(R"~~~(
    auto impl = TRY(throw_dom_exception_if_needed(vm, [&] { return @fully_qualified_name@::create_with_global_object(window, @.constructor_arguments@); }));
)~~~");
        } else {
            generator.append(R"~~~(
    auto impl = TRY(throw_dom_exception_if_needed(vm, [&] { return @fully_qualified_name@::create_with_global_object(window); }));
)~~~");
        }
        generator.append(R"~~~(
    return &(*impl);
)~~~");
    } else {
        // Multiple constructor overloads - can't do that yet.
        TODO();
    }

    generator.append(R"~~~(
}

void @constructor_class@::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();
    auto& window = verify_cast<HTML::Window>(realm.global_object());
    [[maybe_unused]] u8 default_attributes = JS::Attribute::Enumerable;

    NativeFunction::initialize(realm);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<@prototype_class@>("@name@"), 0);
    define_direct_property(vm.names.length, JS::Value(@constructor.length@), JS::Attribute::Configurable);

)~~~");

    for (auto& constant : interface.constants) {
        auto constant_generator = generator.fork();
        constant_generator.set("constant.name", constant.name);

        generate_wrap_statement(constant_generator, constant.value, constant.type, interface, String::formatted("auto constant_{}_value =", constant.name));

        constant_generator.append(R"~~~(
    define_direct_property("@constant.name@", constant_@constant.name@_value, JS::Attribute::Enumerable);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-operations
    for (auto const& overload_set : interface.static_overload_sets) {
        auto function_generator = generator.fork();
        function_generator.set("function.name", overload_set.key);
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(overload_set.key.to_snakecase()));
        function_generator.set("function.length", String::number(get_shortest_function_length(overload_set.value)));

        function_generator.append(R"~~~(
    define_native_function(realm, "@function.name@", @function.name:snakecase@, @function.length@, default_attributes);
)~~~");
    }

    generator.append(R"~~~(
}
)~~~");

    // Implementation: Static Functions
    for (auto& function : interface.static_functions)
        generate_function(generator, function, StaticFunction::Yes, interface.constructor_class, interface.fully_qualified_name, interface);
    for (auto const& overload_set : interface.static_overload_sets) {
        if (overload_set.value.size() == 1)
            continue;
        generate_overload_arbiter(generator, overload_set, interface.constructor_class);
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

void generate_prototype_header(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("fully_qualified_name", interface.fully_qualified_name);
    generator.set("prototype_class", interface.prototype_class);
    generator.set("prototype_class:snakecase", interface.prototype_class.to_snakecase());

    generator.append(R"~~~(
#pragma once

#include <LibJS/Runtime/Object.h>

namespace Web::Bindings {

class @prototype_class@ : public JS::Object {
    JS_OBJECT(@prototype_class@, JS::Object);
public:
    explicit @prototype_class@(JS::Realm&);
    virtual void initialize(JS::Realm&) override;
    virtual ~@prototype_class@() override;
private:
)~~~");

    for (auto const& overload_set : interface.overload_sets) {
        auto function_generator = generator.fork();
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(overload_set.key.to_snakecase()));
        function_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@function.name:snakecase@);
        )~~~");
        if (overload_set.value.size() > 1) {
            for (auto i = 0u; i < overload_set.value.size(); ++i) {
                function_generator.set("overload_suffix", String::number(i));
                function_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@function.name:snakecase@@overload_suffix@);
)~~~");
            }
        }
    }

    if (interface.has_stringifier) {
        auto stringifier_generator = generator.fork();
        stringifier_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(to_string);
        )~~~");
    }

    if (interface.pair_iterator_types.has_value()) {
        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(entries);
    JS_DECLARE_NATIVE_FUNCTION(for_each);
    JS_DECLARE_NATIVE_FUNCTION(keys);
    JS_DECLARE_NATIVE_FUNCTION(values);
        )~~~");
    }

    for (auto& attribute : interface.attributes) {
        auto attribute_generator = generator.fork();
        attribute_generator.set("attribute.name:snakecase", attribute.name.to_snakecase());
        attribute_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@attribute.name:snakecase@_getter);
)~~~");

        if (!attribute.readonly) {
            attribute_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@attribute.name:snakecase@_setter);
)~~~");
        }
    }

    generator.append(R"~~~(

};

)~~~");

    for (auto& it : interface.enumerations) {
        if (!it.value.is_original_definition)
            continue;
        auto enum_generator = generator.fork();
        enum_generator.set("enum.type.name", it.key);
        enum_generator.append(R"~~~(
enum class @enum.type.name@ {
)~~~");
        for (auto& entry : it.value.translated_cpp_names) {
            enum_generator.set("enum.entry", entry.value);
            enum_generator.append(R"~~~(
    @enum.entry@,
)~~~");
        }

        enum_generator.append(R"~~~(
};
inline String idl_enum_to_string(@enum.type.name@ value) {
    switch(value) {
)~~~");
        for (auto& entry : it.value.translated_cpp_names) {
            enum_generator.set("enum.entry", entry.value);
            enum_generator.set("enum.string", entry.key);
            enum_generator.append(R"~~~(
    case @enum.type.name@::@enum.entry@: return "@enum.string@";
)~~~");
        }
        enum_generator.append(R"~~~(
    default: return "<unknown>";
    };
}
)~~~");
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
    )~~~");

    outln("{}", generator.as_string_view());
}

void generate_prototype_implementation(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("parent_name", interface.parent_name);
    generator.set("prototype_class", interface.prototype_class);
    generator.set("prototype_base_class", interface.prototype_base_class);
    generator.set("constructor_class", interface.constructor_class);
    generator.set("prototype_class:snakecase", interface.prototype_class.to_snakecase());
    generator.set("fully_qualified_name", interface.fully_qualified_name);

    if (interface.pair_iterator_types.has_value()) {
        generator.set("iterator_name", String::formatted("{}Iterator", interface.name));
    }

    generator.append(R"~~~(
#include <AK/Function.h>
#include <LibIDL/Types.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/IDLOverloadResolution.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/IDLEventListener.h>
#include <LibWeb/DOM/NodeFilter.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>

#if __has_include(<LibWeb/Bindings/@prototype_base_class@.h>)
#    include <LibWeb/Bindings/@prototype_base_class@.h>
#endif

)~~~");

    for (auto& path : interface.required_imported_paths)
        generate_include_for(generator, path);

    emit_includes_for_all_imports(interface, generator, interface.pair_iterator_types.has_value());

    generator.append(R"~~~(

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::Crypto;
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::DOMParsing;
using namespace Web::Fetch;
using namespace Web::FileAPI;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::NavigationTiming;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;
using namespace Web::SVG;
using namespace Web::UIEvents;
using namespace Web::URL;
using namespace Web::WebSockets;
using namespace Web::XHR;
using namespace Web::WebGL;

namespace Web::Bindings {

@prototype_class@::@prototype_class@([[maybe_unused]] JS::Realm& realm))~~~");
    if (interface.name == "DOMException") {
        // https://webidl.spec.whatwg.org/#es-DOMException-specialness
        // Object.getPrototypeOf(DOMException.prototype) === Error.prototype
        generator.append(R"~~~(
    : Object(*realm.intrinsics().error_prototype())
)~~~");
    } else if (!interface.parent_name.is_empty()) {
        generator.append(R"~~~(
    : Object(verify_cast<HTML::Window>(realm.global_object()).ensure_web_prototype<@prototype_base_class@>("@parent_name@"))
)~~~");
    } else {
        generator.append(R"~~~(
    : Object(*realm.intrinsics().object_prototype())
)~~~");
    }

    // FIXME: Currently almost everything gets default_attributes but it should be configurable per attribute.
    //        See the spec links for details
    generator.append(R"~~~(
{
}

@prototype_class@::~@prototype_class@()
{
}

void @prototype_class@::initialize(JS::Realm& realm)
{
    [[maybe_unused]] auto& vm = this->vm();
    [[maybe_unused]] u8 default_attributes = JS::Attribute::Enumerable | JS::Attribute::Configurable | JS::Attribute::Writable;

)~~~");

    if (interface.has_unscopable_member) {
        generator.append(R"~~~(
    auto* unscopable_object = JS::Object::create(realm, nullptr);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-attributes
    for (auto& attribute : interface.attributes) {
        auto attribute_generator = generator.fork();
        attribute_generator.set("attribute.name", attribute.name);
        attribute_generator.set("attribute.getter_callback", attribute.getter_callback_name);

        if (attribute.readonly)
            attribute_generator.set("attribute.setter_callback", "nullptr");
        else
            attribute_generator.set("attribute.setter_callback", attribute.setter_callback_name);

        if (attribute.extended_attributes.contains("Unscopable")) {
            attribute_generator.append(R"~~~(
    MUST(unscopable_object->create_data_property("@attribute.name@", JS::Value(true)));
)~~~");
        }

        attribute_generator.append(R"~~~(
    define_native_accessor(realm, "@attribute.name@", @attribute.getter_callback@, @attribute.setter_callback@, default_attributes);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-constants
    for (auto& constant : interface.constants) {
        // FIXME: Do constants need to be added to the unscopable list?

        auto constant_generator = generator.fork();
        constant_generator.set("constant.name", constant.name);

        generate_wrap_statement(constant_generator, constant.value, constant.type, interface, String::formatted("auto constant_{}_value =", constant.name));

        constant_generator.append(R"~~~(
    define_direct_property("@constant.name@", constant_@constant.name@_value, JS::Attribute::Enumerable);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-operations
    for (auto const& overload_set : interface.overload_sets) {
        auto function_generator = generator.fork();
        function_generator.set("function.name", overload_set.key);
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(overload_set.key.to_snakecase()));
        function_generator.set("function.length", String::number(get_shortest_function_length(overload_set.value)));

        // FIXME: What if only some of the overloads are Unscopable?
        if (any_of(overload_set.value, [](auto const& function) { return function.extended_attributes.contains("Unscopable"); })) {
            function_generator.append(R"~~~(
    MUST(unscopable_object->create_data_property("@function.name@", JS::Value(true)));
)~~~");
        }

        function_generator.append(R"~~~(
    define_native_function(realm, "@function.name@", @function.name:snakecase@, @function.length@, default_attributes);
)~~~");
    }

    if (interface.has_stringifier) {
        // FIXME: Do stringifiers need to be added to the unscopable list?

        auto stringifier_generator = generator.fork();
        stringifier_generator.append(R"~~~(
    define_native_function(realm, "toString", to_string, 0, default_attributes);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#define-the-iteration-methods
    // This applies to this if block and the following if block.
    if (interface.indexed_property_getter.has_value()) {
        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
    define_direct_property(*vm.well_known_symbol_iterator(), realm.intrinsics().array_prototype()->get_without_side_effects(vm.names.values), JS::Attribute::Configurable | JS::Attribute::Writable);
)~~~");

        if (interface.value_iterator_type.has_value()) {
            iterator_generator.append(R"~~~(
    define_direct_property(vm.names.entries, realm.intrinsics().array_prototype()->get_without_side_effects(vm.names.entries), default_attributes);
    define_direct_property(vm.names.keys, realm.intrinsics().array_prototype()->get_without_side_effects(vm.names.keys), default_attributes);
    define_direct_property(vm.names.values, realm.intrinsics().array_prototype()->get_without_side_effects(vm.names.values), default_attributes);
    define_direct_property(vm.names.forEach, realm.intrinsics().array_prototype()->get_without_side_effects(vm.names.forEach), default_attributes);
)~~~");
        }
    }

    if (interface.pair_iterator_types.has_value()) {
        // FIXME: Do pair iterators need to be added to the unscopable list?

        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
    define_native_function(realm, vm.names.entries, entries, 0, default_attributes);
    define_native_function(realm, vm.names.forEach, for_each, 1, default_attributes);
    define_native_function(realm, vm.names.keys, keys, 0, default_attributes);
    define_native_function(realm, vm.names.values, values, 0, default_attributes);

    define_direct_property(*vm.well_known_symbol_iterator(), get_without_side_effects(vm.names.entries), JS::Attribute::Configurable | JS::Attribute::Writable);
)~~~");
    }

    if (interface.has_unscopable_member) {
        generator.append(R"~~~(
    define_direct_property(*vm.well_known_symbol_unscopables(), unscopable_object, JS::Attribute::Configurable);
)~~~");
    }

    generator.append(R"~~~(
    Object::initialize(realm);
}
)~~~");

    if (!interface.attributes.is_empty() || !interface.functions.is_empty() || interface.has_stringifier) {
        generator.append(R"~~~(
static JS::ThrowCompletionOr<@fully_qualified_name@*> impl_from(JS::VM& vm)
{
    auto this_value = vm.this_value();
    JS::Object* this_object = nullptr;
    if (this_value.is_nullish())
        this_object = &vm.current_realm()->global_object();
    else
        this_object = TRY(this_value.to_object(vm));
)~~~");

        if (interface.name == "EventTarget") {
            generator.append(R"~~~(
    if (is<HTML::Window>(this_object)) {
        return &static_cast<HTML::Window*>(this_object)->impl();
    }
)~~~");
        }

        generator.append(R"~~~(
    if (!is<@fully_qualified_name@>(this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "@fully_qualified_name@");

    return &static_cast<@fully_qualified_name@*>(this_object)->impl();
}
)~~~");
    }

    for (auto& attribute : interface.attributes) {
        auto attribute_generator = generator.fork();
        attribute_generator.set("attribute.getter_callback", attribute.getter_callback_name);
        attribute_generator.set("attribute.setter_callback", attribute.setter_callback_name);

        if (attribute.extended_attributes.contains("ImplementedAs")) {
            auto implemented_as = attribute.extended_attributes.get("ImplementedAs").value();
            attribute_generator.set("attribute.cpp_name", implemented_as);
        } else {
            attribute_generator.set("attribute.cpp_name", attribute.name.to_snakecase());
        }

        if (attribute.extended_attributes.contains("Reflect")) {
            auto attribute_name = attribute.extended_attributes.get("Reflect").value();
            if (attribute_name.is_null())
                attribute_name = attribute.name;
            attribute_name = make_input_acceptable_cpp(attribute_name);

            attribute_generator.set("attribute.reflect_name", attribute_name);
        } else {
            attribute_generator.set("attribute.reflect_name", attribute.name.to_snakecase());
        }

        attribute_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::@attribute.getter_callback@)
{
    [[maybe_unused]] auto& realm = *vm.current_realm();
    auto* impl = TRY(impl_from(vm));
)~~~");

        if (attribute.extended_attributes.contains("Reflect")) {
            if (attribute.type->name() != "boolean") {
                attribute_generator.append(R"~~~(
    auto retval = impl->attribute(HTML::AttributeNames::@attribute.reflect_name@);
)~~~");
            } else {
                attribute_generator.append(R"~~~(
    auto retval = impl->has_attribute(HTML::AttributeNames::@attribute.reflect_name@);
)~~~");
            }
        } else {
            attribute_generator.append(R"~~~(
    auto retval = TRY(throw_dom_exception_if_needed(vm, [&] { return impl->@attribute.cpp_name@(); }));
)~~~");
        }

        generate_return_statement(generator, *attribute.type, interface);

        attribute_generator.append(R"~~~(
}
)~~~");

        if (!attribute.readonly) {
            attribute_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::@attribute.setter_callback@)
{
    [[maybe_unused]] auto& realm = *vm.current_realm();
    auto* impl = TRY(impl_from(vm));

    auto value = vm.argument(0);
)~~~");

            generate_to_cpp(generator, attribute, "value", "", "cpp_value", interface, attribute.extended_attributes.contains("LegacyNullToEmptyString"));

            if (attribute.extended_attributes.contains("Reflect")) {
                if (attribute.type->name() != "boolean") {
                    attribute_generator.append(R"~~~(
    impl->set_attribute(HTML::AttributeNames::@attribute.reflect_name@, cpp_value);
)~~~");
                } else {
                    attribute_generator.append(R"~~~(
    if (!cpp_value)
        impl->remove_attribute(HTML::AttributeNames::@attribute.reflect_name@);
    else
        impl->set_attribute(HTML::AttributeNames::@attribute.reflect_name@, String::empty());
)~~~");
                }
            } else {
                attribute_generator.append(R"~~~(
    TRY(throw_dom_exception_if_needed(vm, [&] { return impl->set_@attribute.cpp_name@(cpp_value); }));
)~~~");
            }

            attribute_generator.append(R"~~~(
    return JS::js_undefined();
}
)~~~");
        }
    }

    // Implementation: Functions
    for (auto& function : interface.functions)
        generate_function(generator, function, StaticFunction::No, interface.prototype_class, interface.fully_qualified_name, interface);
    for (auto const& overload_set : interface.overload_sets) {
        if (overload_set.value.size() == 1)
            continue;
        generate_overload_arbiter(generator, overload_set, interface.prototype_class);
    }

    if (interface.has_stringifier) {
        auto stringifier_generator = generator.fork();
        stringifier_generator.set("class_name", interface.prototype_class);
        if (interface.stringifier_attribute.has_value())
            stringifier_generator.set("attribute.cpp_getter_name", interface.stringifier_attribute->to_snakecase());

        stringifier_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@class_name@::to_string)
{
    [[maybe_unused]] auto& realm = *vm.current_realm();
    auto* impl = TRY(impl_from(vm));

)~~~");
        if (interface.stringifier_attribute.has_value()) {
            stringifier_generator.append(R"~~~(
    auto retval = impl->@attribute.cpp_getter_name@();
)~~~");
        } else {
            stringifier_generator.append(R"~~~(
    auto retval = TRY(throw_dom_exception_if_needed(vm, [&] { return impl->to_string(); }));
)~~~");
        }
        stringifier_generator.append(R"~~~(

    return JS::js_string(vm, move(retval));
}
)~~~");
    }

    if (interface.pair_iterator_types.has_value()) {
        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::entries)
{
    auto* impl = TRY(impl_from(vm));

    return @iterator_name@::create(*impl, Object::PropertyKind::KeyAndValue).ptr();
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::for_each)
{
    [[maybe_unused]] auto& realm = *vm.current_realm();
    auto* impl = TRY(impl_from(vm));

    auto callback = vm.argument(0);
    if (!callback.is_function())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAFunction, callback.to_string_without_side_effects());

    auto this_value = vm.this_value();
    TRY(impl->for_each([&](auto key, auto value) -> JS::ThrowCompletionOr<void> {
)~~~");
        generate_variable_statement(iterator_generator, "wrapped_key", interface.pair_iterator_types->get<0>(), "key", interface);
        generate_variable_statement(iterator_generator, "wrapped_value", interface.pair_iterator_types->get<1>(), "value", interface);
        iterator_generator.append(R"~~~(
        TRY(call(vm, callback.as_function(), vm.argument(1), wrapped_value, wrapped_key, this_value));
        return {};
    }));

    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::keys)
{
    auto* impl = TRY(impl_from(vm));

    return @iterator_name@::create(*impl, Object::PropertyKind::Key).ptr();
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::values)
{
    auto* impl = TRY(impl_from(vm));

    return @iterator_name@::create(*impl, Object::PropertyKind::Value).ptr();
}
)~~~");
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

void generate_iterator_prototype_header(IDL::Interface const& interface)
{
    VERIFY(interface.pair_iterator_types.has_value());
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("prototype_class", String::formatted("{}IteratorPrototype", interface.name));

    generator.append(R"~~~(
#pragma once

#include <LibJS/Runtime/Object.h>

namespace Web::Bindings {

class @prototype_class@ : public JS::Object {
    JS_OBJECT(@prototype_class@, JS::Object);
public:
    explicit @prototype_class@(JS::Realm&);
    virtual void initialize(JS::Realm&) override;
    virtual ~@prototype_class@() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

} // namespace Web::Bindings
    )~~~");

    outln("{}", generator.as_string_view());
}

void generate_iterator_prototype_implementation(IDL::Interface const& interface)
{
    VERIFY(interface.pair_iterator_types.has_value());
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", String::formatted("{}Iterator", interface.name));
    generator.set("prototype_class", String::formatted("{}IteratorPrototype", interface.name));
    generator.set("fully_qualified_name", String::formatted("{}Iterator", interface.fully_qualified_name));
    generator.set("possible_include_path", String::formatted("{}Iterator", interface.name.replace("::"sv, "/"sv, ReplaceMode::All)));

    generator.append(R"~~~(
#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/HTML/Window.h>

#if __has_include(<LibWeb/@possible_include_path@.h>)
#    include <LibWeb/@possible_include_path@.h>
#endif
)~~~");

    emit_includes_for_all_imports(interface, generator, true);

    generator.append(R"~~~(
// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::DOMParsing;
using namespace Web::Fetch;
using namespace Web::FileAPI;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::NavigationTiming;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;
using namespace Web::XHR;
using namespace Web::UIEvents;
using namespace Web::URL;
using namespace Web::WebGL;

namespace Web::Bindings {

@prototype_class@::@prototype_class@(JS::Realm& realm)
    : Object(*realm.intrinsics().iterator_prototype())
{
}

@prototype_class@::~@prototype_class@()
{
}

void @prototype_class@::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();
    Object::initialize(realm);

    define_native_function(realm, vm.names.next, next, 0, JS::Attribute::Writable | JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Iterator"), JS::Attribute::Configurable);
}

static JS::ThrowCompletionOr<@fully_qualified_name@*> impl_from(JS::VM& vm)
{
    auto* this_object = TRY(vm.this_value().to_object(vm));
    if (!is<@fully_qualified_name@>(this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "@fully_qualified_name@");
    return &static_cast<@fully_qualified_name@*>(this_object)->impl();
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::next)
{
    auto* impl = TRY(impl_from(vm));
    return TRY(throw_dom_exception_if_needed(vm, [&] { return impl->next(); }));
}

} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}
}
