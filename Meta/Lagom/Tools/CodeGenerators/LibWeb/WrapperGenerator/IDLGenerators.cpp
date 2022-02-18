/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "IDLTypes.h"
#include <AK/LexicalPath.h>
#include <AK/Queue.h>

Vector<StringView> s_header_search_paths;

namespace IDL {

static bool is_wrappable_type(Type const& type)
{
    if (type.name == "EventTarget")
        return true;
    if (type.name == "Node")
        return true;
    if (type.name == "Document")
        return true;
    if (type.name == "Text")
        return true;
    if (type.name == "DocumentType")
        return true;
    if (type.name.ends_with("Element"))
        return true;
    if (type.name.ends_with("Event"))
        return true;
    if (type.name == "ImageData")
        return true;
    if (type.name == "Window")
        return true;
    if (type.name == "Range")
        return true;
    if (type.name == "Selection")
        return true;
    if (type.name == "Attribute")
        return true;
    if (type.name == "NamedNodeMap")
        return true;
    if (type.name == "TextMetrics")
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

CppType idl_type_name_to_cpp_type(Type const& type)
{
    if (is_wrappable_type(type)) {
        if (type.nullable)
            return { .name = String::formatted("RefPtr<{}>", type.name), .sequence_storage_type = SequenceStorageType::Vector };

        return { .name = String::formatted("NonnullRefPtr<{}>", type.name), .sequence_storage_type = SequenceStorageType::Vector };
    }

    if (type.is_string())
        return { .name = "String", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name == "double" && !type.nullable)
        return { .name = "double", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name == "boolean" && !type.nullable)
        return { .name = "bool", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name == "unsigned long" && !type.nullable)
        return { .name = "u32", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name == "unsigned short" && !type.nullable)
        return { .name = "u16", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name == "long" && !type.nullable)
        return { .name = "i32", .sequence_storage_type = SequenceStorageType::Vector };

    if (type.name == "any")
        return { .name = "JS::Value", .sequence_storage_type = SequenceStorageType::MarkedVector };

    if (type.name == "sequence") {
        auto& parameterized_type = verify_cast<ParameterizedType>(type);
        auto& sequence_type = parameterized_type.parameters.first();
        auto sequence_cpp_type = idl_type_name_to_cpp_type(sequence_type);
        auto storage_type_name = sequence_storage_type_to_cpp_storage_type_name(sequence_cpp_type.sequence_storage_type);

        if (sequence_cpp_type.sequence_storage_type == SequenceStorageType::MarkedVector)
            return { .name = storage_type_name, .sequence_storage_type = SequenceStorageType::Vector };

        return { .name = String::formatted("{}<{}>", storage_type_name, sequence_cpp_type.name), .sequence_storage_type = SequenceStorageType::Vector };
    }

    if (type.name == "record") {
        auto& parameterized_type = verify_cast<ParameterizedType>(type);
        auto& record_key_type = parameterized_type.parameters[0];
        auto& record_value_type = parameterized_type.parameters[1];
        auto record_key_cpp_type = idl_type_name_to_cpp_type(record_key_type);
        auto record_value_cpp_type = idl_type_name_to_cpp_type(record_value_type);

        return { .name = String::formatted("OrderedHashMap<{}, {}>", record_key_cpp_type.name, record_value_cpp_type.name), .sequence_storage_type = SequenceStorageType::Vector };
    }

    if (is<UnionType>(type)) {
        auto& union_type = verify_cast<UnionType>(type);
        return { .name = union_type.to_variant(), .sequence_storage_type = SequenceStorageType::Vector };
    }

    dbgln("Unimplemented type for idl_type_name_to_cpp_type: {}{}", type.name, type.nullable ? "?" : "");
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

    return input.replace("-", "_");
}

static void generate_include_for_wrapper(auto& generator, auto& wrapper_name)
{
    auto wrapper_generator = generator.fork();
    wrapper_generator.set("wrapper_class", wrapper_name);
    // FIXME: These may or may not exist, because REASONS.
    wrapper_generator.append(R"~~~(
#if __has_include(<LibWeb/Bindings/@wrapper_class@.h>)
#   include <LibWeb/Bindings/@wrapper_class@.h>
#endif
#if __has_include(<LibWeb/Bindings/@wrapper_class@Factory.h>)
#   include <LibWeb/Bindings/@wrapper_class@Factory.h>
#endif
)~~~");
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
#if __has_include(<LibWeb/@iterator_class.path@Factory.h>)
#   include <LibWeb/@iterator_class.path@Factory.h>
#endif
#if __has_include(<LibWeb/Bindings/@iterator_class.name@Wrapper.h>)
#   include <LibWeb/Bindings/@iterator_class.name@Wrapper.h>
#endif
#if __has_include(<LibWeb/Bindings/@iterator_class.name@WrapperFactory.h>)
#   include <LibWeb/Bindings/@iterator_class.name@WrapperFactory.h>
#endif
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

static void emit_includes_for_all_imports(auto& interface, auto& generator, bool is_header, bool is_iterator = false)
{
    Queue<RemoveCVReference<decltype(interface)> const*> interfaces;
    HashTable<String> paths_imported;
    if (is_header)
        paths_imported.set(interface.module_own_path);

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

        generate_include_for(generator, interface->module_own_path);

        if (is_iterator) {
            auto iterator_name = String::formatted("{}Iterator", interface->name);
            auto iterator_path = String::formatted("{}Iterator", interface->fully_qualified_name.replace("::", "/"));
            generate_include_for_iterator(generator, iterator_path, iterator_name);
        }

        if (interface->wrapper_class != "Wrapper")
            generate_include_for_wrapper(generator, interface->wrapper_class);
    }
}

static bool should_emit_wrapper_factory(IDL::Interface const& interface)
{
    // FIXME: This is very hackish.
    if (interface.name == "Event")
        return false;
    if (interface.name == "EventTarget")
        return false;
    if (interface.name == "Node")
        return false;
    if (interface.name == "Text")
        return false;
    if (interface.name == "Document")
        return false;
    if (interface.name == "DocumentType")
        return false;
    if (interface.name.ends_with("Element"))
        return false;
    if (interface.name.starts_with("CSS") && interface.name.ends_with("Rule"))
        return false;
    return true;
}

template<typename ParameterType>
static void generate_to_cpp(SourceGenerator& generator, ParameterType& parameter, String const& js_name, String const& js_suffix, String const& cpp_name, IDL::Interface const& interface, bool legacy_null_to_empty_string = false, bool optional = false, Optional<String> optional_default_value = {}, bool variadic = false, size_t recursion_depth = 0, bool used_as_argument = false)
{
    auto scoped_generator = generator.fork();
    auto acceptable_cpp_name = make_input_acceptable_cpp(cpp_name);
    scoped_generator.set("cpp_name", acceptable_cpp_name);
    scoped_generator.set("js_name", js_name);
    scoped_generator.set("js_suffix", js_suffix);
    scoped_generator.set("legacy_null_to_empty_string", legacy_null_to_empty_string ? "true" : "false");
    scoped_generator.set("parameter.type.name", parameter.type->name);
    if (parameter.type->name == "Window")
        scoped_generator.set("wrapper_name", "WindowObject");
    else
        scoped_generator.set("wrapper_name", String::formatted("{}Wrapper", parameter.type->name));

    if (optional_default_value.has_value())
        scoped_generator.set("parameter.optional_default_value", *optional_default_value);

    // FIXME: Add support for optional, variadic, nullable and default values to all types
    if (parameter.type->is_string()) {
        if (variadic) {
            scoped_generator.append(R"~~~(
    Vector<String> @cpp_name@;
    @cpp_name@.ensure_capacity(vm.argument_count() - @js_suffix@);

    for (size_t i = @js_suffix@; i < vm.argument_count(); ++i) {
        auto to_string_result = TRY(vm.argument(i).to_string(global_object));
        @cpp_name@.append(move(to_string_result));
    }
)~~~");
        } else if (!optional) {
            if (!parameter.type->nullable) {
                scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (@js_name@@js_suffix@.is_null() && @legacy_null_to_empty_string@) {
        @cpp_name@ = String::empty();
    } else {
        @cpp_name@ = TRY(@js_name@@js_suffix@.to_string(global_object));
    }
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (!@js_name@@js_suffix@.is_nullish())
        @cpp_name@ = TRY(@js_name@@js_suffix@.to_string(global_object));
)~~~");
            }
        } else {
            scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (!@js_name@@js_suffix@.is_undefined()) {
        if (@js_name@@js_suffix@.is_null() && @legacy_null_to_empty_string@)
            @cpp_name@ = String::empty();
        else
            @cpp_name@ = TRY(@js_name@@js_suffix@.to_string(global_object));
    })~~~");
            if (optional_default_value.has_value() && (!parameter.type->nullable || optional_default_value.value() != "null")) {
                scoped_generator.append(R"~~~( else {
        @cpp_name@ = @parameter.optional_default_value@;
    }
)~~~");
            } else {
                scoped_generator.append(R"~~~(
)~~~");
            }
        }
    } else if (parameter.type->name == "EventListener") {
        // FIXME: Replace this with support for callback interfaces. https://heycam.github.io/webidl/#idl-callback-interface

        if (parameter.type->nullable) {
            scoped_generator.append(R"~~~(
    RefPtr<IDLEventListener> @cpp_name@;
    if (!@js_name@@js_suffix@.is_nullish()) {
        if (!@js_name@@js_suffix@.is_object())
            return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObject, @js_name@@js_suffix@.to_string_without_side_effects());

        CallbackType callback_type(JS::make_handle(&@js_name@@js_suffix@.as_object()), HTML::incumbent_settings_object());
        @cpp_name@ = adopt_ref(*new IDLEventListener(move(callback_type)));
    }
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObject, @js_name@@js_suffix@.to_string_without_side_effects());

    CallbackType callback_type(JS::make_handle(&@js_name@@js_suffix@.as_object()), HTML::incumbent_settings_object());
    auto @cpp_name@ = adopt_ref(*new IDLEventListener(move(callback_type)));
)~~~");
        }
    } else if (IDL::is_wrappable_type(*parameter.type)) {
        if (!parameter.type->nullable) {
            scoped_generator.append(R"~~~(
    auto @cpp_name@_object = TRY(@js_name@@js_suffix@.to_object(global_object));

    if (!is<@wrapper_name@>(@cpp_name@_object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

    auto& @cpp_name@ = static_cast<@wrapper_name@*>(@cpp_name@_object)->impl();
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    @parameter.type.name@* @cpp_name@ = nullptr;
    if (!@js_name@@js_suffix@.is_nullish()) {
        auto @cpp_name@_object = TRY(@js_name@@js_suffix@.to_object(global_object));

        if (!is<@wrapper_name@>(@cpp_name@_object))
            return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

        @cpp_name@ = &static_cast<@wrapper_name@*>(@cpp_name@_object)->impl();
    }
)~~~");
        }
    } else if (parameter.type->name == "double") {
        if (!optional) {
            scoped_generator.append(R"~~~(
    double @cpp_name@ = TRY(@js_name@@js_suffix@.to_double(global_object));
)~~~");
        } else {
            if (optional_default_value.has_value()) {
                scoped_generator.append(R"~~~(
    double @cpp_name@;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    Optional<double> @cpp_name@;
)~~~");
            }
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
        @cpp_name@ = TRY(@js_name@@js_suffix@.to_double(global_object));
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
    } else if (parameter.type->name == "boolean") {
        if (!optional) {
            scoped_generator.append(R"~~~(
    bool @cpp_name@ = @js_name@@js_suffix@.to_boolean();
)~~~");
        } else {
            if (optional_default_value.has_value()) {
                scoped_generator.append(R"~~~(
    bool @cpp_name@;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    Optional<bool> @cpp_name@;
)~~~");
            }
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
        @cpp_name@ = @js_name@@js_suffix@.to_boolean();)~~~");
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
    } else if (parameter.type->name == "unsigned long") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = TRY(@js_name@@js_suffix@.to_u32(global_object));
)~~~");
    } else if (parameter.type->name == "unsigned short") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = TRY(@js_name@@js_suffix@.to_u16(global_object));
)~~~");
    } else if (parameter.type->name == "long") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = TRY(@js_name@@js_suffix@.to_i32(global_object));
)~~~");
    } else if (parameter.type->name == "EventHandler") {
        // x.onfoo = function() { ... }, x.onfoo = () => { ... }, x.onfoo = {}
        // NOTE: Anything else than an object will be treated as null. This is because EventHandler has the [LegacyTreatNonObjectAsNull] extended attribute.
        //       Yes, you can store objects in event handler attributes. They just get ignored when there's any attempt to invoke them.
        // FIXME: Replace this with proper support for callback function types.

        scoped_generator.append(R"~~~(
    Optional<Bindings::CallbackType> @cpp_name@;
    if (@js_name@@js_suffix@.is_object()) {
        @cpp_name@ = Bindings::CallbackType { JS::make_handle(&@js_name@@js_suffix@.as_object()), HTML::incumbent_settings_object() };
    }
)~~~");
    } else if (parameter.type->name == "Promise") {
        // NOTE: It's not clear to me where the implicit wrapping of non-Promise values in a resolved
        // Promise is defined in the spec; https://webidl.spec.whatwg.org/#idl-promise doesn't say
        // anything of this sort. Both Gecko and Blink do it, however, so I'm sure it's correct.
        scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object() || !is<JS::Promise>(@js_name@@js_suffix@.as_object())) {
        auto* new_promise = JS::Promise::create(global_object);
        new_promise->fulfill(@js_name@@js_suffix@);
        @js_name@@js_suffix@ = new_promise;
    }
    auto @cpp_name@ = JS::make_handle(&static_cast<JS::Promise&>(@js_name@@js_suffix@.as_object()));
)~~~");
    } else if (parameter.type->name == "BufferSource") {
        scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object() || !(is<JS::TypedArrayBase>(@js_name@@js_suffix@.as_object()) || is<JS::ArrayBuffer>(@js_name@@js_suffix@.as_object()) || is<JS::DataView>(@js_name@@js_suffix@.as_object())))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

    // TODO: Should we make this a Variant?
    auto @cpp_name@ = JS::make_handle(&@js_name@@js_suffix@.as_object());
)~~~");
    } else if (parameter.type->name == "any") {
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
    } else if (interface.enumerations.contains(parameter.type->name)) {
        auto enum_generator = scoped_generator.fork();
        auto& enumeration = interface.enumerations.find(parameter.type->name)->value;
        enum_generator.set("enum.default.cpp_value", *enumeration.translated_cpp_names.get(optional_default_value.value_or(enumeration.first_member)));
        enum_generator.set("js_name.as_string", String::formatted("{}{}_string", enum_generator.get("js_name"), enum_generator.get("js_suffix")));
        enum_generator.append(R"~~~(
    @parameter.type.name@ @cpp_name@ { @parameter.type.name@::@enum.default.cpp_value@ };
    auto @js_name.as_string@ = TRY(@js_name@@js_suffix@.to_string(global_object));
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

        if (used_as_argument) {
            enum_generator.append(R"~~~(
    @else@
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::InvalidEnumerationValue, @js_name.as_string@, "@parameter.type.name@");
)~~~");
        }
    } else if (interface.dictionaries.contains(parameter.type->name)) {
        if (optional_default_value.has_value() && optional_default_value != "{}")
            TODO();
        auto dictionary_generator = scoped_generator.fork();
        dictionary_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_nullish() && !@js_name@@js_suffix@.is_object())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

    @parameter.type.name@ @cpp_name@ {};
)~~~");
        auto* current_dictionary = &interface.dictionaries.find(parameter.type->name)->value;
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
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::MissingRequiredProperty, "@member_key@");
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
    } else if (parameter.type->name == "sequence") {
        // https://webidl.spec.whatwg.org/#es-sequence

        auto sequence_generator = scoped_generator.fork();
        auto& parameterized_type = verify_cast<IDL::ParameterizedType>(*parameter.type);
        sequence_generator.set("recursion_depth", String::number(recursion_depth));

        // An ECMAScript value V is converted to an IDL sequence<T> value as follows:
        // 1. If Type(V) is not Object, throw a TypeError.
        // 2. Let method be ? GetMethod(V, @@iterator).
        // 3. If method is undefined, throw a TypeError.
        // 4. Return the result of creating a sequence from V and method.

        sequence_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObject, @js_name@@js_suffix@.to_string_without_side_effects());

    auto* iterator_method@recursion_depth@ = TRY(@js_name@@js_suffix@.get_method(global_object, *vm.well_known_symbol_iterator()));
    if (!iterator_method@recursion_depth@)
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotIterable, @js_name@@js_suffix@.to_string_without_side_effects());
)~~~");

        parameterized_type.generate_sequence_from_iterable(sequence_generator, acceptable_cpp_name, String::formatted("{}{}", js_name, js_suffix), String::formatted("iterator_method{}", recursion_depth), interface, recursion_depth + 1);
    } else if (parameter.type->name == "record") {
        // https://webidl.spec.whatwg.org/#es-record

        auto record_generator = scoped_generator.fork();
        auto& parameterized_type = verify_cast<IDL::ParameterizedType>(*parameter.type);
        record_generator.set("recursion_depth", String::number(recursion_depth));

        // A record can only have two types: key type and value type.
        VERIFY(parameterized_type.parameters.size() == 2);

        // A record only allows the key to be a string.
        VERIFY(parameterized_type.parameters[0].is_string());

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

        auto record_cpp_type = IDL::idl_type_name_to_cpp_type(parameterized_type);
        record_generator.set("record.type", record_cpp_type.name);

        // If this is a recursive call to generate_to_cpp, assume that the caller has already handled converting the JS value to an object for us.
        // This affects record types in unions for example.
        if (recursion_depth == 0) {
            record_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObject, @js_name@@js_suffix@.to_string_without_side_effects());

    auto& @js_name@@js_suffix@_object = @js_name@@js_suffix@.as_object();
)~~~");
        }

        record_generator.append(R"~~~(
    @record.type@ @cpp_name@;

    auto record_keys@recursion_depth@ = TRY(@js_name@@js_suffix@_object.internal_own_property_keys());

    for (auto& key@recursion_depth@ : record_keys@recursion_depth@) {
        auto property_key@recursion_depth@ = MUST(JS::PropertyKey::from_value(global_object, key@recursion_depth@));

        auto descriptor@recursion_depth@ = TRY(@js_name@@js_suffix@_object.internal_get_own_property(property_key@recursion_depth@));

        if (!descriptor@recursion_depth@.has_value() || !descriptor@recursion_depth@->enumerable.has_value() || !descriptor@recursion_depth@->enumerable.value())
            continue;
)~~~");

        IDL::Parameter key_parameter { .type = parameterized_type.parameters[0], .name = acceptable_cpp_name, .optional_default_value = {}, .extended_attributes = {} };
        generate_to_cpp(record_generator, key_parameter, "key", String::number(recursion_depth), String::formatted("typed_key{}", recursion_depth), interface, false, false, {}, false, recursion_depth + 1);

        record_generator.append(R"~~~(
        auto value@recursion_depth@ = TRY(@js_name@@js_suffix@_object.get(property_key@recursion_depth@));
)~~~");

        // FIXME: Record value types should be TypeWithExtendedAttributes, which would allow us to get [LegacyNullToEmptyString] here.
        IDL::Parameter value_parameter { .type = parameterized_type.parameters[1], .name = acceptable_cpp_name, .optional_default_value = {}, .extended_attributes = {} };
        generate_to_cpp(record_generator, value_parameter, "value", String::number(recursion_depth), String::formatted("typed_value{}", recursion_depth), interface, false, false, {}, false, recursion_depth + 1);

        record_generator.append(R"~~~(
        @cpp_name@.set(typed_key@recursion_depth@, typed_value@recursion_depth@);
    }
)~~~");
    } else if (is<IDL::UnionType>(*parameter.type)) {
        // https://webidl.spec.whatwg.org/#es-union

        auto union_generator = scoped_generator.fork();

        auto& union_type = verify_cast<IDL::UnionType>(*parameter.type);
        union_generator.set("union_type", union_type.to_variant());
        union_generator.set("recursion_depth", String::number(recursion_depth));

        // A lambda is used because Variants without "Empty" can't easily be default initialized.
        // Plus, this would require the user of union types to always accept a Variant with an Empty type.

        // Additionally, it handles the case of unconditionally throwing a TypeError at the end if none of the types match.
        // This is because we cannot unconditionally throw in generate_to_cpp as generate_to_cpp is supposed to assign to a variable and then continue.
        // Note that all the other types only throw on a condition.

        // The lambda must take the JS::Value to convert as a parameter instead of capturing it in order to support union types being variadic.
        union_generator.append(R"~~~(
    auto @js_name@@js_suffix@_to_variant = [&global_object, &vm](JS::Value @js_name@@js_suffix@) -> JS::ThrowCompletionOr<@union_type@> {
        // These might be unused.
        (void)global_object;
        (void)vm;
)~~~");

        // 1. If the union type includes undefined and V is undefined, then return the unique undefined value.
        if (union_type.includes_undefined()) {
            scoped_generator.append(R"~~~(
        if (@js_name@@js_suffix@.is_undefined())
            return Empty {};
)~~~");
        }

        // 3. Let types be the flattened member types of the union type.
        auto types = union_type.flattened_member_types();

        bool contains_dictionary_type = false;
        for (auto& dictionary : interface.dictionaries) {
            for (auto& type : types) {
                if (type.name == dictionary.key) {
                    contains_dictionary_type = true;
                    break;
                }
            }

            if (contains_dictionary_type)
                break;
        }

        // FIXME: 2. If the union type includes a nullable type and V is null or undefined, then return the IDL value null.
        if (union_type.includes_nullable_type()) {
            TODO();
        } else if (contains_dictionary_type) {
            // FIXME: 4. If V is null or undefined, then
            //              4.1 If types includes a dictionary type, then return the result of converting V to that dictionary type.
            TODO();
        }

        bool includes_object = false;
        for (auto& type : types) {
            if (type.name == "object") {
                includes_object = true;
                break;
            }
        }

        // FIXME: Don't generate this if the union type doesn't include any object types.
        union_generator.append(R"~~~(
        if (@js_name@@js_suffix@.is_object()) {
            [[maybe_unused]] auto& @js_name@@js_suffix@_object = @js_name@@js_suffix@.as_object();
)~~~");

        bool includes_wrappable_type = false;
        for (auto& type : types) {
            if (IDL::is_wrappable_type(type)) {
                includes_wrappable_type = true;
                break;
            }
        }

        if (includes_wrappable_type) {
            // 5. If V is a platform object, then:
            union_generator.append(R"~~~(
            if (is<Wrapper>(@js_name@@js_suffix@_object)) {
)~~~");

            //    1. If types includes an interface type that V implements, then return the IDL value that is a reference to the object V.
            for (auto& type : types) {
                if (!IDL::is_wrappable_type(type))
                    continue;

                auto union_platform_object_type_generator = union_generator.fork();
                union_platform_object_type_generator.set("platform_object_type", String::formatted("{}Wrapper", type.name));
                auto cpp_type = IDL::idl_type_name_to_cpp_type(type);
                union_platform_object_type_generator.set("refptr_type", cpp_type.name);

                union_platform_object_type_generator.append(R"~~~(
                if (is<@platform_object_type@>(@js_name@@js_suffix@_object))
                    return @refptr_type@ { static_cast<@platform_object_type@&>(@js_name@@js_suffix@_object).impl() };
)~~~");
            }

            //    2. If types includes object, then return the IDL value that is a reference to the object V.
            if (includes_object) {
                union_generator.append(R"~~~(
                return @js_name@@js_suffix@_object;
)~~~");
            }

            union_generator.append(R"~~~(
            }
)~~~");
        }

        // FIXME: 6. If Type(V) is Object and V has an [[ArrayBufferData]] internal slot, then
        //           1. If types includes ArrayBuffer, then return the result of converting V to ArrayBuffer.
        //           2. If types includes object, then return the IDL value that is a reference to the object V.

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
            if (type.name == "sequence") {
                sequence_type = verify_cast<IDL::ParameterizedType>(type);
                break;
            }
        }

        if (sequence_type) {
            // 1. Let method be ? GetMethod(V, @@iterator).
            union_generator.append(R"~~~(
        auto* method = TRY(@js_name@@js_suffix@.get_method(global_object, *vm.well_known_symbol_iterator()));
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

        // FIXME: 3. If types includes a dictionary type, then return the result of converting V to that dictionary type.
        if (contains_dictionary_type)
            TODO();

        // 4. If types includes a record type, then return the result of converting V to that record type.
        RefPtr<IDL::ParameterizedType> record_type;
        for (auto& type : types) {
            if (type.name == "record") {
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
            if (type.name == "boolean") {
                includes_boolean = true;
                break;
            }
        }

        if (includes_boolean) {
            union_generator.append(R"~~~(
        if (@js_name@@js_suffix@.is_boolean())
            return @js_name@@js_suffix@.as_boolean();
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
            if (type.name == "bigint") {
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
        return TRY(@js_name@@js_suffix@.to_string(global_object));
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
            auto cpp_type = IDL::idl_type_name_to_cpp_type(*numeric_type);
            union_numeric_type_generator.set("numeric_type", cpp_type.name);

            union_numeric_type_generator.append(R"~~~(
        auto x = TRY(@js_name@@js_suffix@.to_numeric(global_object));
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
        return @js_name@@js_suffix@.to_boolean();
)~~~");
        } else if (includes_bigint) {
            // 18. If types includes bigint, then return the result of converting V to bigint.
            union_generator.append(R"~~~(
        return TRY(@js_name@@js_suffix@.to_bigint(global_object));
)~~~");
        } else {
            // 19. Throw a TypeError.
            // FIXME: Replace the error message with something more descriptive.
            union_generator.append(R"~~~(
        return vm.throw_completion<JS::TypeError>(global_object, "No union types matched");
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
                if (!optional_default_value.has_value()) {
                    union_generator.append(R"~~~(
        Optional<@union_type@> @cpp_name@;
        if (!@js_name@@js_suffix@.is_undefined())
            @cpp_name@ = TRY(@js_name@@js_suffix@_to_variant(@js_name@@js_suffix@));
    )~~~");
                } else {
                    if (optional_default_value != "\"\"")
                        TODO();

                    union_generator.append(R"~~~(
        @union_type@ @cpp_name@ = @js_name@@js_suffix@.is_undefined() ? String::empty() : TRY(@js_name@@js_suffix@_to_variant(@js_name@@js_suffix@));
    )~~~");
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
        dbgln("Unimplemented JS-to-C++ conversion: {}", parameter.type->name);
        VERIFY_NOT_REACHED();
    }
}

template<typename FunctionType>
static void generate_argument_count_check(SourceGenerator& generator, FunctionType& function)
{
    auto argument_count_check_generator = generator.fork();
    argument_count_check_generator.set("function.name", function.name);
    argument_count_check_generator.set("function.nargs", String::number(function.length()));

    if (function.length() == 0)
        return;
    if (function.length() == 1) {
        argument_count_check_generator.set(".bad_arg_count", "JS::ErrorType::BadArgCountOne");
        argument_count_check_generator.set(".arg_count_suffix", "");
    } else {
        argument_count_check_generator.set(".bad_arg_count", "JS::ErrorType::BadArgCountMany");
        argument_count_check_generator.set(".arg_count_suffix", String::formatted(", \"{}\"", function.length()));
    }

    argument_count_check_generator.append(R"~~~(
    if (vm.argument_count() < @function.nargs@)
        return vm.throw_completion<JS::TypeError>(global_object, @.bad_arg_count@, "@function.name@"@.arg_count_suffix@);
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
        generate_to_cpp(generator, parameter, "arg", String::number(argument_index), parameter.name.to_snakecase(), interface, legacy_null_to_empty_string, parameter.optional, parameter.optional_default_value, parameter.variadic, 0, true);
        ++argument_index;
    }

    arguments_builder.join(", ", parameter_names);
}

// https://webidl.spec.whatwg.org/#create-sequence-from-iterable
void IDL::ParameterizedType::generate_sequence_from_iterable(SourceGenerator& generator, String const& cpp_name, String const& iterable_cpp_name, String const& iterator_method_cpp_name, IDL::Interface const& interface, size_t recursion_depth) const
{
    auto sequence_generator = generator.fork();
    sequence_generator.set("cpp_name", cpp_name);
    sequence_generator.set("iterable_cpp_name", iterable_cpp_name);
    sequence_generator.set("iterator_method_cpp_name", iterator_method_cpp_name);
    sequence_generator.set("recursion_depth", String::number(recursion_depth));
    auto sequence_cpp_type = idl_type_name_to_cpp_type(parameters.first());
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
    auto iterator@recursion_depth@ = TRY(JS::get_iterator(global_object, @iterable_cpp_name@, JS::IteratorHint::Sync, @iterator_method_cpp_name@));
)~~~");

    if (sequence_cpp_type.sequence_storage_type == SequenceStorageType::Vector) {
        sequence_generator.append(R"~~~(
    @sequence.storage_type@<@sequence.type@> @cpp_name@;
)~~~");
    } else {
        sequence_generator.append(R"~~~(
    @sequence.storage_type@ @cpp_name@ { global_object.heap() };
)~~~");
    }

    sequence_generator.append(R"~~~(
    for (;;) {
        auto* next@recursion_depth@ = TRY(JS::iterator_step(global_object, iterator@recursion_depth@));
        if (!next@recursion_depth@)
            break;

        auto next_item@recursion_depth@ = TRY(JS::iterator_value(global_object, *next@recursion_depth@));
)~~~");

    // FIXME: Sequences types should be TypeWithExtendedAttributes, which would allow us to get [LegacyNullToEmptyString] here.
    IDL::Parameter parameter { .type = parameters.first(), .name = iterable_cpp_name, .optional_default_value = {}, .extended_attributes = {} };
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
    scoped_generator.set("type", type.name);
    scoped_generator.set("result_expression", result_expression);
    scoped_generator.set("recursion_depth", String::number(recursion_depth));

    if (type.name == "undefined") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::js_undefined();
)~~~");
        return;
    }

    if (type.nullable) {
        if (type.is_string()) {
            scoped_generator.append(R"~~~(
    if (@value@.is_null()) {
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
    } else if (type.name == "sequence") {
        // https://webidl.spec.whatwg.org/#es-sequence
        auto& sequence_generic_type = verify_cast<IDL::ParameterizedType>(type);

        scoped_generator.append(R"~~~(
    auto* new_array@recursion_depth@ = MUST(JS::Array::create(global_object, 0));

    for (size_t i@recursion_depth@ = 0; i@recursion_depth@ < @value@.size(); ++i@recursion_depth@) {
        auto& element@recursion_depth@ = @value@.at(i@recursion_depth@);
)~~~");

        generate_wrap_statement(scoped_generator, String::formatted("element{}", recursion_depth), sequence_generic_type.parameters.first(), interface, String::formatted("auto wrapped_element{} =", recursion_depth), WrappingReference::Yes, recursion_depth + 1);

        scoped_generator.append(R"~~~(
        auto property_index@recursion_depth@ = JS::PropertyKey { i@recursion_depth@ };
        MUST(new_array@recursion_depth@->create_data_property(property_index@recursion_depth@, wrapped_element@recursion_depth@));
    }

    @result_expression@ new_array@recursion_depth@;
)~~~");
    } else if (type.name == "boolean" || type.name == "double") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value(@value@);
)~~~");
    } else if (type.name == "short" || type.name == "unsigned short" || type.name == "long" || type.name == "unsigned long") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value((i32)@value@);
)~~~");
    } else if (type.name == "Location" || type.name == "Promise" || type.name == "Uint8Array" || type.name == "Uint8ClampedArray" || type.name == "any") {
        scoped_generator.append(R"~~~(
    @result_expression@ @value@;
)~~~");
    } else if (type.name == "EventHandler") {
        // FIXME: Replace this with proper support for callback function types.

        scoped_generator.append(R"~~~(
    if (!@value@) {
        @result_expression@ JS::js_null();
    } else {
        VERIFY(!@value@->callback.is_null());
        @result_expression@ @value@->callback.cell();
    }
)~~~");
    } else if (is<IDL::UnionType>(type)) {
        TODO();
    } else if (interface.enumerations.contains(type.name)) {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::js_string(global_object.heap(), Bindings::idl_enum_to_string(@value@));
)~~~");
    } else {
        if (wrapping_reference == WrappingReference::No) {
            scoped_generator.append(R"~~~(
    @result_expression@ wrap(global_object, const_cast<@type@&>(*@value@));
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    @result_expression@ wrap(global_object, const_cast<@type@&>(@value@));
)~~~");
        }
    }

    if (type.nullable) {
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

    if (function.extended_attributes.contains("ImplementedAs")) {
        auto implemented_as = function.extended_attributes.get("ImplementedAs").value();
        function_generator.set("function.cpp_name", implemented_as);
    } else {
        function_generator.set("function.cpp_name", make_input_acceptable_cpp(function.name.to_snakecase()));
    }

    function_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@class_name@::@function.name:snakecase@)
{
)~~~");

    if (is_static_function == StaticFunction::No) {
        function_generator.append(R"~~~(
    auto* impl = TRY(impl_from(vm, global_object));
)~~~");
    }

    generate_argument_count_check(generator, function);

    StringBuilder arguments_builder;
    generate_arguments(generator, function.parameters, arguments_builder, interface);
    function_generator.set(".arguments", arguments_builder.string_view());

    if (is_static_function == StaticFunction::No) {
        function_generator.append(R"~~~(
    [[maybe_unused]] auto retval = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl->@function.cpp_name@(@.arguments@); }));
)~~~");
    } else {
        function_generator.append(R"~~~(
    [[maybe_unused]] auto retval = TRY(throw_dom_exception_if_needed(global_object, [&] { return @interface_fully_qualified_name@::@function.cpp_name@(@.arguments@); }));
)~~~");
    }

    generate_return_statement(generator, *function.return_type, interface);

    function_generator.append(R"~~~(
}
)~~~");
}

void generate_header(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <LibWeb/Bindings/Wrapper.h>
)~~~");

    for (auto& path : interface.imported_paths)
        generate_include_for(generator, path);

    emit_includes_for_all_imports(interface, generator, true);
    generator.set("name", interface.name);
    generator.set("fully_qualified_name", interface.fully_qualified_name);
    generator.set("wrapper_base_class", interface.wrapper_base_class);
    generator.set("wrapper_class", interface.wrapper_class);
    generator.set("wrapper_class:snakecase", interface.wrapper_class.to_snakecase());

    if (interface.wrapper_base_class != "Wrapper")
        generate_include_for_wrapper(generator, interface.wrapper_base_class);

    generator.append(R"~~~(
namespace Web::Bindings {

class @wrapper_class@ : public @wrapper_base_class@ {
    JS_OBJECT(@name@, @wrapper_base_class@);
public:
    static @wrapper_class@* create(JS::GlobalObject&, @fully_qualified_name@&);

    @wrapper_class@(JS::GlobalObject&, @fully_qualified_name@&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~@wrapper_class@() override;
)~~~");

    if (interface.extended_attributes.contains("CustomGet")) {
        generator.append(R"~~~(
    virtual JS::ThrowCompletionOr<JS::Value> internal_get(JS::PropertyKey const&, JS::Value receiver) const override;
)~~~");
    }
    if (interface.extended_attributes.contains("CustomSet")) {
        generator.append(R"~~~(
    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value, JS::Value receiver) override;
)~~~");
    }

    if (interface.extended_attributes.contains("CustomHasProperty")) {
        generator.append(R"~~~(
    virtual JS::ThrowCompletionOr<bool> internal_has_property(JS::PropertyKey const&) const override;
)~~~");
    }

    if (interface.extended_attributes.contains("CustomVisit")) {
        generator.append(R"~~~(
    virtual void visit_edges(JS::Cell::Visitor&) override;
)~~~");
    }

    if (interface.is_legacy_platform_object()) {
        generator.append(R"~~~(
    virtual JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> internal_get_own_property(JS::PropertyKey const&) const override;
    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value, JS::Value) override;
    virtual JS::ThrowCompletionOr<bool> internal_define_own_property(JS::PropertyKey const&, JS::PropertyDescriptor const&) override;
    virtual JS::ThrowCompletionOr<bool> internal_delete(JS::PropertyKey const&) override;
    virtual JS::ThrowCompletionOr<bool> internal_prevent_extensions() override;
    virtual JS::ThrowCompletionOr<JS::MarkedVector<JS::Value>> internal_own_property_keys() const override;
)~~~");
    }

    if (interface.wrapper_base_class == "Wrapper") {
        generator.append(R"~~~(
    @fully_qualified_name@& impl() { return *m_impl; }
    @fully_qualified_name@ const& impl() const { return *m_impl; }
)~~~");
    } else {
        generator.append(R"~~~(
    @fully_qualified_name@& impl() { return static_cast<@fully_qualified_name@&>(@wrapper_base_class@::impl()); }
    @fully_qualified_name@ const& impl() const { return static_cast<@fully_qualified_name@ const&>(@wrapper_base_class@::impl()); }
)~~~");
    }

    generator.append(R"~~~(
private:
)~~~");

    if (interface.is_legacy_platform_object()) {
        generator.append(R"~~~(
    JS::ThrowCompletionOr<bool> is_named_property_exposed_on_object(JS::PropertyKey const&) const;
    JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> legacy_platform_object_get_own_property_for_get_own_property_slot(JS::PropertyKey const&) const;
    JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> legacy_platform_object_get_own_property_for_set_slot(JS::PropertyKey const&) const;
)~~~");
    }

    if (interface.wrapper_base_class == "Wrapper") {
        generator.append(R"~~~(
    NonnullRefPtr<@fully_qualified_name@> m_impl;
        )~~~");
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

    if (should_emit_wrapper_factory(interface)) {
        generator.append(R"~~~(
@wrapper_class@* wrap(JS::GlobalObject&, @fully_qualified_name@&);
)~~~");
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

void generate_implementation(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("wrapper_class", interface.wrapper_class);
    generator.set("wrapper_base_class", interface.wrapper_base_class);
    generator.set("prototype_class", interface.prototype_class);
    generator.set("fully_qualified_name", interface.fully_qualified_name);

    generator.append(R"~~~(
#include <AK/FlyString.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
)~~~");

    emit_includes_for_all_imports(interface, generator, false);

    generator.append(R"~~~(
// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;

namespace Web::Bindings {

@wrapper_class@* @wrapper_class@::create(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
{
    return global_object.heap().allocate<@wrapper_class@>(global_object, global_object, impl);
}

)~~~");

    if (interface.wrapper_base_class == "Wrapper") {
        generator.append(R"~~~(
@wrapper_class@::@wrapper_class@(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
    : Wrapper(static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_class@>("@name@"))
    , m_impl(impl)
{
}
)~~~");
    } else {
        generator.append(R"~~~(
@wrapper_class@::@wrapper_class@(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
    : @wrapper_base_class@(global_object, impl)
{
    set_prototype(&static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_class@>("@name@"));
}
)~~~");
    }

    generator.append(R"~~~(
void @wrapper_class@::initialize(JS::GlobalObject& global_object)
{
    @wrapper_base_class@::initialize(global_object);
}

@wrapper_class@::~@wrapper_class@()
{
}
)~~~");

    if (should_emit_wrapper_factory(interface)) {
        generator.append(R"~~~(
@wrapper_class@* wrap(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
{
    return static_cast<@wrapper_class@*>(wrap_impl(global_object, impl));
}
)~~~");
    }

    if (interface.extended_attributes.contains("CustomVisit")) {
        generator.append(R"~~~(
void @wrapper_class@::visit_edges(JS::Cell::Visitor& visitor)
{
    @wrapper_base_class@::visit_edges(visitor);
    impl().visit_edges(visitor);
}
)~~~");
    }

    if (interface.is_legacy_platform_object()) {
        auto scoped_generator = generator.fork();
        scoped_generator.set("class_name", interface.wrapper_class);
        scoped_generator.set("fully_qualified_name", interface.fully_qualified_name);

        // FIXME: This is a hack to avoid duplicating/refactoring a lot of code.
        scoped_generator.append(R"~~~(
static JS::Value wrap_for_legacy_platform_object_get_own_property(JS::GlobalObject& global_object, [[maybe_unused]] auto& retval)
{
    [[maybe_unused]] auto& vm = global_object.vm();
)~~~");

        if (interface.named_property_getter.has_value()) {
            generate_return_statement(scoped_generator, *interface.named_property_getter->return_type, interface);
        } else {
            VERIFY(interface.indexed_property_getter.has_value());
            generate_return_statement(scoped_generator, *interface.indexed_property_getter->return_type, interface);
        }

        scoped_generator.append(R"~~~(
}
)~~~");

        if (interface.supports_named_properties()) {
            // https://webidl.spec.whatwg.org/#dfn-named-property-visibility

            scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::is_named_property_exposed_on_object(JS::PropertyKey const& property_key) const
{
    [[maybe_unused]] auto& vm = this->vm();

    // The spec doesn't say anything about the type of the property name here.
    // Numbers can be converted to a string, which is fine and what other engines do.
    // However, since a symbol cannot be converted to a string, it cannot be a supported property name. Return early if it's a symbol.
    if (property_key.is_symbol())
        return false;

    // 1. If P is not a supported property name of O, then return false.
    // NOTE: This is in it's own variable to enforce the type.
    // FIXME: Can this throw?
    Vector<String> supported_property_names = impl().supported_property_names();
    auto property_key_string = property_key.to_string();
    if (!supported_property_names.contains_slow(property_key_string))
        return false;

    // 2. If O has an own property named P, then return false.
    // NOTE: This has to be done manually instead of using Object::has_own_property, as that would use the overridden internal_get_own_property.
    auto own_property_named_p = MUST(Object::internal_get_own_property(property_key));

    if (own_property_named_p.has_value())
        return false;
)~~~");

            if (interface.extended_attributes.contains("LegacyOverrideBuiltIns")) {
                scoped_generator.append(R"~~~(
    // 3. If O implements an interface that has the [LegacyOverrideBuiltIns] extended attribute, then return true.
    return true;
}
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    // NOTE: Step 3 is not here as the interface doesn't have the LegacyOverrideBuiltIns extended attribute.
    // 4. Let prototype be O.[[GetPrototypeOf]]().
    auto* prototype = TRY(internal_get_prototype_of());

    // 5. While prototype is not null:
    while (prototype) {
        // FIXME: 1. If prototype is not a named properties object, and prototype has an own property named P, then return false.
        //           (It currently does not check for named property objects)
        bool prototype_has_own_property_named_p = TRY(prototype->has_own_property(property_key));
        if (prototype_has_own_property_named_p)
            return false;

        // 2. Set prototype to prototype.[[GetPrototypeOf]]().
        prototype = TRY(prototype->internal_get_prototype_of());
    }

    // 6. Return true.
    return true;
}
)~~~");
            }
        }

        enum class IgnoreNamedProps {
            No,
            Yes,
        };

        auto generate_legacy_platform_object_get_own_property_function = [&](IgnoreNamedProps ignore_named_props, String const& for_which_internal_method) {
            // https://webidl.spec.whatwg.org/#LegacyPlatformObjectGetOwnProperty

            auto get_own_property_generator = scoped_generator.fork();

            get_own_property_generator.set("internal_method"sv, for_which_internal_method);

            get_own_property_generator.append(R"~~~(
JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> @class_name@::legacy_platform_object_get_own_property_for_@internal_method@_slot(JS::PropertyKey const& property_name) const
{
)~~~");

            get_own_property_generator.append(R"~~~(
    [[maybe_unused]] auto& global_object = this->global_object();
)~~~");

            // 1. If O supports indexed properties...
            if (interface.supports_indexed_properties()) {
                // ...and P is an array index, then:
                get_own_property_generator.append(R"~~~(
    if (property_name.is_number()) {
        // 1. Let index be the result of calling ToUint32(P).
        u32 index = property_name.as_number();

        // 2. If index is a supported property index, then:
        // FIXME: Can this throw?
        if (impl().is_supported_property_index(index)) {
)~~~");
                // 1. Let operation be the operation used to declare the indexed property getter. (NOTE: Not necessary)
                // 2. Let value be an uninitialized variable. (NOTE: Not necessary)

                // 3. If operation was defined without an identifier, then set value to the result of performing the steps listed in the interface description to determine the value of an indexed property with index as the index.
                if (interface.indexed_property_getter->name.is_empty()) {
                    get_own_property_generator.append(R"~~~(
            auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().determine_value_of_indexed_property(index); }));
)~~~");
                }

                // 4. Otherwise, operation was defined with an identifier. Set value to the result of performing the method steps of operation with O as this and « index » as the argument values.
                else {
                    auto function_scoped_generator = get_own_property_generator.fork();

                    function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.indexed_property_getter->name.to_snakecase()));

                    function_scoped_generator.append(R"~~~(
            auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().@function.cpp_name@(index); }));
)~~~");
                }

                get_own_property_generator.append(R"~~~(
            // 5. Let desc be a newly created Property Descriptor with no fields.
            JS::PropertyDescriptor descriptor;

            // 6. Set desc.[[Value]] to the result of converting value to an ECMAScript value.
            descriptor.value = wrap_for_legacy_platform_object_get_own_property(global_object, value);
)~~~");

                // 7. If O implements an interface with an indexed property setter, then set desc.[[Writable]] to true, otherwise set it to false.
                if (interface.indexed_property_setter.has_value()) {
                    get_own_property_generator.append(R"~~~(
            descriptor.writable = true;
)~~~");
                } else {
                    get_own_property_generator.append(R"~~~(
            descriptor.writable = false;
)~~~");
                }

                get_own_property_generator.append(R"~~~(

            // 8. Set desc.[[Enumerable]] and desc.[[Configurable]] to true.
            descriptor.enumerable = true;
            descriptor.configurable = true;

            // 9. Return desc.
            return descriptor;
        }

        // 3. Set ignoreNamedProps to true.
        // NOTE: To reduce complexity of WrapperGenerator, this just returns early instead of keeping track of another variable.
        return TRY(Object::internal_get_own_property(property_name));
    }
)~~~");
            }

            // 2. If O supports named properties and ignoreNamedProps is false, then:
            if (interface.supports_named_properties() && ignore_named_props == IgnoreNamedProps::No) {
                get_own_property_generator.append(R"~~~(
    // 1. If the result of running the named property visibility algorithm with property name P and object O is true, then:
    if (TRY(is_named_property_exposed_on_object(property_name))) {
        // FIXME: It's unfortunate that this is done twice, once in is_named_property_exposed_on_object and here.
        auto property_name_string = property_name.to_string();
)~~~");

                // 1. Let operation be the operation used to declare the named property getter. (NOTE: Not necessary)
                // 2. Let value be an uninitialized variable. (NOTE: Not necessary)

                // 3. If operation was defined without an identifier, then set value to the result of performing the steps listed in the interface description to determine the value of a named property with P as the name.
                if (interface.named_property_getter->name.is_empty()) {
                    get_own_property_generator.append(R"~~~(
        auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().determine_value_of_named_property(property_name_string); }));
)~~~");
                }

                // 4. Otherwise, operation was defined with an identifier. Set value to the result of performing the method steps of operation with O as this and « index » as the argument values.
                else {
                    auto function_scoped_generator = get_own_property_generator.fork();
                    function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.named_property_getter->name.to_snakecase()));

                    function_scoped_generator.append(R"~~~(
        auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().@function.cpp_name@(property_name_string); }));
)~~~");
                }

                get_own_property_generator.append(R"~~~(
        // 5. Let desc be a newly created Property Descriptor with no fields.
        JS::PropertyDescriptor descriptor;

        // 6. Set desc.[[Value]] to the result of converting value to an ECMAScript value.
        descriptor.value = wrap_for_legacy_platform_object_get_own_property(global_object, value);
)~~~");

                // 7. If O implements an interface with a named property setter, then set desc.[[Writable]] to true, otherwise set it to false.
                if (interface.named_property_setter.has_value()) {
                    get_own_property_generator.append(R"~~~(
        descriptor.writable = true;
)~~~");
                } else {
                    get_own_property_generator.append(R"~~~(
        descriptor.writable = false;
)~~~");
                }

                // 8. If O implements an interface with the [LegacyUnenumerableNamedProperties] extended attribute, then set desc.[[Enumerable]] to false, otherwise set it to true.
                if (interface.extended_attributes.contains("LegacyUnenumerableNamedProperties")) {
                    get_own_property_generator.append(R"~~~(
        descriptor.enumerable = false;
)~~~");
                } else {
                    get_own_property_generator.append(R"~~~(
        descriptor.enumerable = true;
)~~~");
                }

                get_own_property_generator.append(R"~~~(
        // 9. Set desc.[[Configurable]] to true.
        descriptor.configurable = true;

        // 10. Return desc.
        return descriptor;
    }
)~~~");
            }

            // 3. Return OrdinaryGetOwnProperty(O, P).
            get_own_property_generator.append(R"~~~(
    return TRY(Object::internal_get_own_property(property_name));
}
)~~~");
        };

        // Step 1 of [[GetOwnProperty]]: Return LegacyPlatformObjectGetOwnProperty(O, P, false).
        generate_legacy_platform_object_get_own_property_function(IgnoreNamedProps::No, "get_own_property");

        // Step 2 of [[Set]]: Let ownDesc be LegacyPlatformObjectGetOwnProperty(O, P, true).
        generate_legacy_platform_object_get_own_property_function(IgnoreNamedProps::Yes, "set");

        if (interface.named_property_setter.has_value()) {
            // https://webidl.spec.whatwg.org/#invoke-named-setter
            // NOTE: All users of invoke_named_property_setter check that JS::PropertyKey is a String before calling it.
            // FIXME: It's not necessary to determine "creating" if the named property setter specifies an identifier.
            //        Try avoiding it somehow, e.g. by enforcing supported_property_names doesn't have side effects so it can be skipped.
            scoped_generator.append(R"~~~(
static JS::ThrowCompletionOr<void> invoke_named_property_setter(JS::GlobalObject& global_object, @fully_qualified_name@& impl, String const& property_name, JS::Value value)
{
    // 1. Let creating be true if P is not a supported property name, and false otherwise.
    // NOTE: This is in it's own variable to enforce the type.
    // FIXME: Can this throw?
    Vector<String> supported_property_names = impl.supported_property_names();
    [[maybe_unused]] bool creating = !supported_property_names.contains_slow(property_name);
)~~~");
            // 2. Let operation be the operation used to declare the named property setter. (NOTE: Not necessary)
            // 3. Let T be the type of the second argument of operation. (NOTE: Not necessary)

            // 4. Let value be the result of converting V to an IDL value of type T.
            // NOTE: This takes the last parameter as it's enforced that there's only two parameters.
            generate_to_cpp(scoped_generator, interface.named_property_setter->parameters.last(), "value", "", "converted_value", interface);

            // 5. If operation was defined without an identifier, then:
            if (interface.named_property_setter->name.is_empty()) {
                scoped_generator.append(R"~~~(
    if (creating) {
        // 5.1. If creating is true, then perform the steps listed in the interface description to set the value of a new named property with P as the name and value as the value.
        TRY(throw_dom_exception_if_needed(global_object, [&] { impl.set_value_of_new_named_property(property_name, converted_value); }));
    } else {
        // 5.2 Otherwise, creating is false. Perform the steps listed in the interface description to set the value of an existing named property with P as the name and value as the value.
        TRY(throw_dom_exception_if_needed(global_object, [&] { impl.set_value_of_existing_named_property(property_name, converted_value); }));
    }
)~~~");
            } else {
                // 6. Otherwise, operation was defined with an identifier.
                //    Perform the method steps of operation with O as this and « P, value » as the argument values.
                auto function_scoped_generator = scoped_generator.fork();
                function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.named_property_setter->name.to_snakecase()));

                function_scoped_generator.append(R"~~~(
    TRY(throw_dom_exception_if_needed(global_object, [&] { impl.@function.cpp_name@(property_name, converted_value); }));
)~~~");
            }

            scoped_generator.append(R"~~~(
    return {};
}
)~~~");
        }

        if (interface.indexed_property_setter.has_value()) {
            // https://webidl.spec.whatwg.org/#invoke-indexed-setter
            // NOTE: All users of invoke_indexed_property_setter check if property name is an IDL array index before calling it.
            // FIXME: It's not necessary to determine "creating" if the indexed property setter specifies an identifier.
            //        Try avoiding it somehow, e.g. by enforcing supported_property_indices doesn't have side effects so it can be skipped.
            scoped_generator.append(R"~~~(
static JS::ThrowCompletionOr<void> invoke_indexed_property_setter(JS::GlobalObject& global_object, @fully_qualified_name@& impl, JS::PropertyKey const& property_name, JS::Value value)
{
    // 1. Let index be the result of calling ToUint32(P).
    u32 index = property_name.as_number();

    // 2. Let creating be true if index is not a supported property index, and false otherwise.
    // FIXME: Can this throw?
    [[maybe_unused]] bool creating = !impl.is_supported_property_index(index);
)~~~");

            // 3. Let operation be the operation used to declare the named property setter. (NOTE: Not necessary)
            // 4. Let T be the type of the second argument of operation. (NOTE: Not necessary)

            // 5. Let value be the result of converting V to an IDL value of type T.
            // NOTE: This takes the last parameter as it's enforced that there's only two parameters.
            generate_to_cpp(scoped_generator, interface.named_property_setter->parameters.last(), "value", "", "converted_value", interface);

            // 6. If operation was defined without an identifier, then:
            if (interface.indexed_property_setter->name.is_empty()) {
                scoped_generator.append(R"~~~(
    if (creating) {
        // 6.1 If creating is true, then perform the steps listed in the interface description to set the value of a new indexed property with index as the index and value as the value.
        TRY(throw_dom_exception_if_needed(global_object, [&] { impl.set_value_of_new_indexed_property(index, converted_value); }));
    } else {
        // 6.2 Otherwise, creating is false. Perform the steps listed in the interface description to set the value of an existing indexed property with index as the index and value as the value.
        TRY(throw_dom_exception_if_needed(global_object, [&] { impl.set_value_of_existing_indexed_property(index, converted_value); }));
    }
)~~~");
            } else {
                // 7. Otherwise, operation was defined with an identifier.
                //    Perform the method steps of operation with O as this and « index, value » as the argument values.
                auto function_scoped_generator = scoped_generator.fork();
                function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.indexed_property_setter->name.to_snakecase()));

                function_scoped_generator.append(R"~~~(
    TRY(throw_dom_exception_if_needed(global_object, [&] { impl.@function.cpp_name@(index, converted_value); }));
)~~~");
            }

            scoped_generator.append(R"~~~(
}
)~~~");
        }

        // == Internal Slot Generation ==

        // 3.9.1. [[GetOwnProperty]], https://webidl.spec.whatwg.org/#legacy-platform-object-getownproperty
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> @class_name@::internal_get_own_property(JS::PropertyKey const& property_name) const
{
    // 1. Return LegacyPlatformObjectGetOwnProperty(O, P, false).
    return TRY(legacy_platform_object_get_own_property_for_get_own_property_slot(property_name));
}
)~~~");

        // 3.9.2. [[Set]], https://webidl.spec.whatwg.org/#legacy-platform-object-set
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::internal_set(JS::PropertyKey const& property_name, JS::Value value, JS::Value receiver)
{
    [[maybe_unused]] auto& global_object = this->global_object();
)~~~");

        // The step 1 if statement will be empty if the interface has no setters, so don't generate the if statement if there's no setters.
        if (interface.named_property_setter.has_value() || interface.indexed_property_setter.has_value()) {
            scoped_generator.append(R"~~~(
    // 1. If O and Receiver are the same object, then:
    if (JS::same_value(this, receiver)) {
)~~~");

            // 1. If O implements an interface with an indexed property setter...
            if (interface.indexed_property_setter.has_value()) {
                // ...and P is an array index, then:
                scoped_generator.append(R"~~~(
        if (property_name.is_number()) {
            // 1. Invoke the indexed property setter on O with P and V.
            TRY(invoke_indexed_property_setter(global_object, impl(), property_name, value));

            // 2. Return true.
            return true;
        }
)~~~");
            }

            // 2. If O implements an interface with a named property setter...
            if (interface.named_property_setter.has_value()) {
                // ... and Type(P) is String, then:
                scoped_generator.append(R"~~~(
        if (property_name.is_string()) {
            // 1. Invoke the named property setter on O with P and V.
            TRY(invoke_named_property_setter(global_object, impl(), property_name.as_string(), value));

            // 2. Return true.
            return true;
        }
)~~~");
            }

            scoped_generator.append(R"~~~(
    }
)~~~");
        }

        scoped_generator.append(R"~~~(
    // 2. Let ownDesc be LegacyPlatformObjectGetOwnProperty(O, P, true).
    auto own_descriptor = TRY(legacy_platform_object_get_own_property_for_set_slot(property_name));

    // 3. Perform ? OrdinarySetWithOwnDescriptor(O, P, V, Receiver, ownDesc).
    // NOTE: The spec says "perform" instead of "return", meaning nothing will be returned on this path according to the spec, which isn't possible to do.
    //       Let's treat it as though it says "return" instead of "perform".
    return ordinary_set_with_own_descriptor(property_name, value, receiver, own_descriptor);
}
)~~~");

        // 3.9.3. [[DefineOwnProperty]], https://webidl.spec.whatwg.org/#legacy-platform-object-defineownproperty
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::internal_define_own_property(JS::PropertyKey const& property_name, JS::PropertyDescriptor const& property_descriptor)
{
    [[maybe_unused]] auto& vm = this->vm();
    [[maybe_unused]] auto& global_object = this->global_object();
)~~~");

        // 1. If O supports indexed properties...
        if (interface.supports_indexed_properties()) {
            // ...and P is an array index, then:
            scoped_generator.append(R"~~~(
    if (property_name.is_number()) {
        // 1. If the result of calling IsDataDescriptor(Desc) is false, then return false.
        if (!property_descriptor.is_data_descriptor())
            return false;
)~~~");

            // 2. If O does not implement an interface with an indexed property setter, then return false.
            if (!interface.indexed_property_setter.has_value()) {
                scoped_generator.append(R"~~~(
        return false;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
        // 3. Invoke the indexed property setter on O with P and Desc.[[Value]].
        TRY(invoke_indexed_property_setter(global_object, impl(), property_name, *property_descriptor.value));

        // 4. Return true.
        return true;
)~~~");
            }

            scoped_generator.append(R"~~~(
    }
)~~~");
        }

        // 2. If O supports named properties, O does not implement an interface with the [Global] extended attribute,
        if (interface.supports_named_properties() && !interface.extended_attributes.contains("Global")) {
            // Type(P) is String,
            // FIXME: and P is not an unforgeable property name of O, then:
            // FIXME: It's not necessary to determine "creating" if the named property setter specifies an identifier.
            //        Try avoiding it somehow, e.g. by enforcing supported_property_names doesn't have side effects so it can be skipped.
            scoped_generator.append(R"~~~(
    if (property_name.is_string()) {
        auto& property_name_as_string = property_name.as_string();

        // 1. Let creating be true if P is not a supported property name, and false otherwise.
        // NOTE: This is in it's own variable to enforce the type.
        // FIXME: Can this throw?
        Vector<String> supported_property_names = impl().supported_property_names();
        [[maybe_unused]] bool creating = !supported_property_names.contains_slow(property_name_as_string);
)~~~");

            // 2. If O implements an interface with the [LegacyOverrideBuiltIns] extended attribute or O does not have an own property named P, then:
            if (!interface.extended_attributes.contains("LegacyOverrideBuiltIns")) {
                scoped_generator.append(R"~~~(
        // NOTE: This has to be done manually instead of using Object::has_own_property, as that would use the overridden internal_get_own_property.
        auto own_property_named_p = TRY(Object::internal_get_own_property(property_name));

        if (!own_property_named_p.has_value()))~~~");
            }

            // A scope is created regardless of the fact that the interface may have [LegacyOverrideBuiltIns] specified to prevent code duplication.
            scoped_generator.append(R"~~~(
        {
)~~~");

            // 1. If creating is false and O does not implement an interface with a named property setter, then return false.
            if (!interface.named_property_setter.has_value()) {
                scoped_generator.append(R"~~~(
            if (!creating)
                return false;
)~~~");
            } else {
                // 2. If O implements an interface with a named property setter, then:
                scoped_generator.append(R"~~~(
            // 1. If the result of calling IsDataDescriptor(Desc) is false, then return false.
            if (!property_descriptor.is_data_descriptor())
                return false;

            // 2. Invoke the named property setter on O with P and Desc.[[Value]].
            TRY(invoke_named_property_setter(global_object, impl(), property_name_as_string, *property_descriptor.value));

            // 3. Return true.
            return true;
)~~~");
            }

            scoped_generator.append(R"~~~(
        }
)~~~");

            scoped_generator.append(R"~~~(
    }
)~~~");
        }

        // 3. If O does not implement an interface with the [Global] extended attribute, then set Desc.[[Configurable]] to true.
        if (!interface.extended_attributes.contains("Global")) {
            scoped_generator.append(R"~~~(
    // property_descriptor is a const&, thus we need to create a copy here to set [[Configurable]]
    JS::PropertyDescriptor descriptor_copy(property_descriptor);
    descriptor_copy.configurable = true;

    // 4. Return OrdinaryDefineOwnProperty(O, P, Desc).
    return Object::internal_define_own_property(property_name, descriptor_copy);
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    // 4. Return OrdinaryDefineOwnProperty(O, P, Desc).
    return Object::internal_define_own_property(property_name, property_descriptor);
)~~~");
        }

        scoped_generator.append(R"~~~(
}
)~~~");

        // 3.9.4. [[Delete]], https://webidl.spec.whatwg.org/#legacy-platform-object-delete
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::internal_delete(JS::PropertyKey const& property_name)
{
    [[maybe_unused]] auto& global_object = this->global_object();
)~~~");

        // 1. If O supports indexed properties...
        if (interface.supports_indexed_properties()) {
            // ...and P is an array index, then:
            scoped_generator.append(R"~~~(
    if (property_name.is_number()) {
        // 1. Let index be the result of calling ToUint32(P).
        u32 index = property_name.as_number();

        // 2. If index is not a supported property index, then return true.
        // FIXME: Can this throw?
        if (!impl().is_supported_property_index(index))
            return true;

        // 3. Return false.
        return false;
    }
)~~~");
        }

        // 2. If O supports named properties, O does not implement an interface with the [Global] extended attribute...
        if (interface.supports_named_properties() && !interface.extended_attributes.contains("Global")) {
            // ...and the result of calling the named property visibility algorithm with property name P and object O is true, then:
            scoped_generator.append(R"~~~(
    if (TRY(is_named_property_exposed_on_object(property_name))) {
)~~~");

            // 1. If O does not implement an interface with a named property deleter, then return false.
            if (!interface.named_property_deleter.has_value()) {
                scoped_generator.append(R"~~~(
        return false;
)~~~");
            } else {
                // 2. Let operation be the operation used to declare the named property deleter. (NOTE: Not necessary)

                scoped_generator.append(R"~~~(
        // FIXME: It's unfortunate that this is done twice, once in is_named_property_exposed_on_object and here.
        auto property_name_string = property_name.to_string();
)~~~");

                // 3. If operation was defined without an identifier, then:
                if (interface.named_property_deleter->name.is_empty()) {
                    scoped_generator.append(R"~~~(
        // 1. Perform the steps listed in the interface description to delete an existing named property with P as the name.
        bool succeeded = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().delete_existing_named_property(property_name_string); }));

        // 2. If the steps indicated that the deletion failed, then return false.
        if (!succeeded)
            return false;
)~~~");
                } else {
                    // 4. Otherwise, operation was defined with an identifier:
                    auto function_scoped_generator = scoped_generator.fork();
                    function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.named_property_deleter->name.to_snakecase()));

                    function_scoped_generator.append(R"~~~(
        // 1. Perform method steps of operation with O as this and « P » as the argument values.
        [[maybe_unused]] auto result = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().@function.cpp_name@(property_name_string); }));
)~~~");

                    // 2. If operation was declared with a return type of boolean and the steps returned false, then return false.
                    if (interface.named_property_deleter->return_type->name == "boolean") {
                        function_scoped_generator.append(R"~~~(
        if (!result)
            return false;
)~~~");
                    }
                }

                scoped_generator.append(R"~~~(
        // 5. Return true.
        return true;
)~~~");
            }

            scoped_generator.append(R"~~~(
    }
)~~~");
        }

        scoped_generator.append(R"~~~(
    // 3. If O has an own property with name P, then:
    auto own_property_named_p_descriptor = TRY(Object::internal_get_own_property(property_name));

    if (own_property_named_p_descriptor.has_value()) {
        // 1. If the property is not configurable, then return false.
        // 2. Otherwise, remove the property from O.
        if (*own_property_named_p_descriptor->configurable)
            storage_delete(property_name);
        else
            return false;
    }

    // 4. Return true.
    return true;
}
)~~~");

        // 3.9.5. [[PreventExtensions]], https://webidl.spec.whatwg.org/#legacy-platform-object-preventextensions
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::internal_prevent_extensions()
{
    // 1. Return false.
    return false;
}
)~~~");

        // 3.9.6. [[OwnPropertyKeys]], https://webidl.spec.whatwg.org/#legacy-platform-object-ownpropertykeys
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<JS::MarkedVector<JS::Value>> @class_name@::internal_own_property_keys() const
{
    auto& vm = this->vm();

    // 1. Let keys be a new empty list of ECMAScript String and Symbol values.
    JS::MarkedVector<JS::Value> keys { heap() };

)~~~");

        // 2. If O supports indexed properties, then for each index of O’s supported property indices, in ascending numerical order, append ! ToString(index) to keys.
        if (interface.supports_indexed_properties()) {
            scoped_generator.append(R"~~~(
    for (u64 index = 0; index <= NumericLimits<u32>::max(); ++index) {
        if (impl().is_supported_property_index(index))
            keys.append(js_string(vm, String::number(index)));
        else
            break;
    }
)~~~");
        }

        // 3. If O supports named properties, then for each P of O’s supported property names that is visible according to the named property visibility algorithm, append P to keys.
        if (interface.supports_named_properties()) {
            scoped_generator.append(R"~~~(
    for (auto& named_property : impl().supported_property_names()) {
        if (TRY(is_named_property_exposed_on_object(named_property)))
            keys.append(js_string(vm, named_property));
    }
)~~~");
        }

        scoped_generator.append(R"~~~(
    // 4. For each P of O’s own property keys that is a String, in ascending chronological order of property creation, append P to keys.
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_string())
            keys.append(it.key.to_value(vm));
    }

    // 5. For each P of O’s own property keys that is a Symbol, in ascending chronological order of property creation, append P to keys.
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_symbol())
            keys.append(it.key.to_value(vm));
    }

    // FIXME: 6. Assert: keys has no duplicate items.

    // 7. Return keys.
    return { move(keys) };
}
)~~~");
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
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
    explicit @constructor_class@(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~@constructor_class@() override;

    virtual JS::ThrowCompletionOr<JS::Value> call() override;
    virtual JS::ThrowCompletionOr<JS::Object*> construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
)~~~");

    for (auto& function : interface.static_functions) {
        auto function_generator = generator.fork();
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
        function_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@function.name:snakecase@);
)~~~");
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
    generator.set("wrapper_class", interface.wrapper_class);
    generator.set("constructor_class", interface.constructor_class);
    generator.set("prototype_class:snakecase", interface.prototype_class.to_snakecase());
    generator.set("fully_qualified_name", interface.fully_qualified_name);

    generator.append(R"~~~(
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibWeb/Bindings/@constructor_class@.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/CSSRuleWrapperFactory.h>
#include <LibWeb/Bindings/EventTargetWrapperFactory.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/WindowObject.h>
#if __has_include(<LibWeb/Crypto/@name@.h>)
#    include <LibWeb/Crypto/@name@.h>
#elif __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/Encoding/@name@.h>)
#    include <LibWeb/Encoding/@name@.h>
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
#elif __has_include(<LibWeb/XHR/@name@.h>)
#    include <LibWeb/XHR/@name@.h>
#elif __has_include(<LibWeb/URL/@name@.h>)
#    include <LibWeb/URL/@name@.h>
#endif

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;
using namespace Web::UIEvents;
using namespace Web::XHR;

namespace Web::Bindings {

@constructor_class@::@constructor_class@(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

@constructor_class@::~@constructor_class@()
{
}

JS::ThrowCompletionOr<JS::Value> @constructor_class@::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "@name@");
}

JS::ThrowCompletionOr<JS::Object*> @constructor_class@::construct(FunctionObject&)
{
)~~~");

    if (interface.constructors.is_empty()) {
        // No constructor
        generator.set("constructor.length", "0");
        generator.append(R"~~~(
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::NotAConstructor, "@name@");
)~~~");
    } else if (interface.constructors.size() == 1) {
        // Single constructor

        auto& constructor = interface.constructors[0];
        generator.set("constructor.length", String::number(constructor.length()));

        generator.append(R"~~~(
    [[maybe_unused]] auto& vm = this->vm();
    [[maybe_unused]] auto& global_object = this->global_object();

    auto& window = static_cast<WindowObject&>(global_object);
)~~~");

        if (!constructor.parameters.is_empty()) {
            generate_argument_count_check(generator, constructor);

            StringBuilder arguments_builder;
            generate_arguments(generator, constructor.parameters, arguments_builder, interface);
            generator.set(".constructor_arguments", arguments_builder.string_view());

            generator.append(R"~~~(
    auto impl = TRY(throw_dom_exception_if_needed(global_object, [&] { return @fully_qualified_name@::create_with_global_object(window, @.constructor_arguments@); }));
)~~~");
        } else {
            generator.append(R"~~~(
    auto impl = TRY(throw_dom_exception_if_needed(global_object, [&] { return @fully_qualified_name@::create_with_global_object(window); }));
)~~~");
        }
        generator.append(R"~~~(
    return wrap(global_object, *impl);
)~~~");
    } else {
        // Multiple constructor overloads - can't do that yet.
        TODO();
    }

    generator.append(R"~~~(
}

void @constructor_class@::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);
    [[maybe_unused]] u8 default_attributes = JS::Attribute::Enumerable;

    NativeFunction::initialize(global_object);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<@prototype_class@>("@name@"), 0);
    define_direct_property(vm.names.length, JS::Value(@constructor.length@), JS::Attribute::Configurable);

)~~~");

    for (auto& constant : interface.constants) {
        auto constant_generator = generator.fork();
        constant_generator.set("constant.name", constant.name);
        constant_generator.set("constant.value", constant.value);

        constant_generator.append(R"~~~(
define_direct_property("@constant.name@", JS::Value((i32)@constant.value@), JS::Attribute::Enumerable);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-operations
    for (auto& function : interface.static_functions) {
        auto function_generator = generator.fork();
        function_generator.set("function.name", function.name);
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
        function_generator.set("function.length", String::number(function.length()));

        function_generator.append(R"~~~(
    define_native_function("@function.name@", @function.name:snakecase@, @function.length@, default_attributes);
)~~~");
    }

    generator.append(R"~~~(
}
)~~~");

    // Implementation: Static Functions
    for (auto& function : interface.static_functions) {
        generate_function(generator, function, StaticFunction::Yes, interface.constructor_class, interface.fully_qualified_name, interface);
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
    explicit @prototype_class@(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~@prototype_class@() override;
private:
)~~~");

    for (auto& function : interface.functions) {
        auto function_generator = generator.fork();
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
        function_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@function.name:snakecase@);
        )~~~");
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
    generator.set("wrapper_class", interface.wrapper_class);
    generator.set("constructor_class", interface.constructor_class);
    generator.set("prototype_class:snakecase", interface.prototype_class.to_snakecase());
    generator.set("fully_qualified_name", interface.fully_qualified_name);

    if (interface.pair_iterator_types.has_value()) {
        generator.set("iterator_name", String::formatted("{}Iterator", interface.name));
        generator.set("iterator_wrapper_class", String::formatted("{}IteratorWrapper", interface.name));
    }

    generator.append(R"~~~(
#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/WorkerLocationWrapper.h>
#include <LibWeb/Bindings/WorkerNavigatorWrapper.h>
#include <LibWeb/Bindings/WorkerWrapper.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/IDLEventListener.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/Origin.h>

#if __has_include(<LibWeb/Bindings/@prototype_base_class@.h>)
#    include <LibWeb/Bindings/@prototype_base_class@.h>
#endif

)~~~");

    for (auto& path : interface.imported_paths)
        generate_include_for(generator, path);

    emit_includes_for_all_imports(interface, generator, false, interface.pair_iterator_types.has_value());

    generator.append(R"~~~(

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::Crypto;
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::NavigationTiming;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;
using namespace Web::XHR;
using namespace Web::URL;

namespace Web::Bindings {

@prototype_class@::@prototype_class@([[maybe_unused]] JS::GlobalObject& global_object))~~~");
    if (interface.name == "DOMException") {
        // https://webidl.spec.whatwg.org/#es-DOMException-specialness
        // Object.getPrototypeOf(DOMException.prototype) === Error.prototype
        generator.append(R"~~~(
    : Object(*global_object.error_prototype())
)~~~");
    } else if (!interface.parent_name.is_empty()) {
        generator.append(R"~~~(
    : Object(static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_base_class@>("@parent_name@"))
)~~~");
    } else {
        generator.append(R"~~~(
    : Object(*global_object.object_prototype())
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

void @prototype_class@::initialize(JS::GlobalObject& global_object)
{
    [[maybe_unused]] auto& vm = this->vm();
    [[maybe_unused]] u8 default_attributes = JS::Attribute::Enumerable | JS::Attribute::Configurable | JS::Attribute::Writable;

)~~~");

    if (interface.has_unscopable_member) {
        generator.append(R"~~~(
    auto* unscopable_object = JS::Object::create(global_object, nullptr);
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
    define_native_accessor("@attribute.name@", @attribute.getter_callback@, @attribute.setter_callback@, default_attributes);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-constants
    for (auto& constant : interface.constants) {
        // FIXME: Do constants need to be added to the unscopable list?

        auto constant_generator = generator.fork();
        constant_generator.set("constant.name", constant.name);
        constant_generator.set("constant.value", constant.value);

        constant_generator.append(R"~~~(
    define_direct_property("@constant.name@", JS::Value((i32)@constant.value@), JS::Attribute::Enumerable);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-operations
    for (auto& function : interface.functions) {
        auto function_generator = generator.fork();
        function_generator.set("function.name", function.name);
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
        function_generator.set("function.length", String::number(function.length()));

        if (function.extended_attributes.contains("Unscopable")) {
            function_generator.append(R"~~~(
    MUST(unscopable_object->create_data_property("@function.name@", JS::Value(true)));
)~~~");
        }

        function_generator.append(R"~~~(
    define_native_function("@function.name@", @function.name:snakecase@, @function.length@, default_attributes);
)~~~");
    }

    if (interface.has_stringifier) {
        // FIXME: Do stringifiers need to be added to the unscopable list?

        auto stringifier_generator = generator.fork();
        stringifier_generator.append(R"~~~(
    define_native_function("toString", to_string, 0, default_attributes);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#define-the-iteration-methods
    // This applies to this if block and the following if block.
    if (interface.indexed_property_getter.has_value()) {
        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
    define_direct_property(*vm.well_known_symbol_iterator(), global_object.array_prototype()->get_without_side_effects(vm.names.values), JS::Attribute::Configurable | JS::Attribute::Writable);
)~~~");

        if (interface.value_iterator_type.has_value()) {
            iterator_generator.append(R"~~~(
    define_direct_property(vm.names.entries, global_object.array_prototype()->get_without_side_effects(vm.names.entries), default_attributes);
    define_direct_property(vm.names.keys, global_object.array_prototype()->get_without_side_effects(vm.names.keys), default_attributes);
    define_direct_property(vm.names.values, global_object.array_prototype()->get_without_side_effects(vm.names.values), default_attributes);
    define_direct_property(vm.names.forEach, global_object.array_prototype()->get_without_side_effects(vm.names.forEach), default_attributes);
)~~~");
        }
    }

    if (interface.pair_iterator_types.has_value()) {
        // FIXME: Do pair iterators need to be added to the unscopable list?

        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
    define_native_function(vm.names.entries, entries, 0, default_attributes);
    define_native_function(vm.names.forEach, for_each, 1, default_attributes);
    define_native_function(vm.names.keys, keys, 0, default_attributes);
    define_native_function(vm.names.values, values, 0, default_attributes);

    define_direct_property(*vm.well_known_symbol_iterator(), get_without_side_effects(vm.names.entries), JS::Attribute::Configurable | JS::Attribute::Writable);
)~~~");
    }

    if (interface.has_unscopable_member) {
        generator.append(R"~~~(
    define_direct_property(*vm.well_known_symbol_unscopables(), unscopable_object, JS::Attribute::Configurable);
)~~~");
    }

    generator.append(R"~~~(
    Object::initialize(global_object);
}
)~~~");

    if (!interface.attributes.is_empty() || !interface.functions.is_empty() || interface.has_stringifier) {
        generator.append(R"~~~(
static JS::ThrowCompletionOr<@fully_qualified_name@*> impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
)~~~");

        if (interface.name == "EventTarget") {
            generator.append(R"~~~(
    if (is<WindowObject>(this_object)) {
        return &static_cast<WindowObject*>(this_object)->impl();
    }
)~~~");
        }

        generator.append(R"~~~(
    if (!is<@wrapper_class@>(this_object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@fully_qualified_name@");

    return &static_cast<@wrapper_class@*>(this_object)->impl();
}
)~~~");
    }

    for (auto& attribute : interface.attributes) {
        auto attribute_generator = generator.fork();
        attribute_generator.set("attribute.getter_callback", attribute.getter_callback_name);
        attribute_generator.set("attribute.setter_callback", attribute.setter_callback_name);
        attribute_generator.set("attribute.name:snakecase", attribute.name.to_snakecase());

        if (attribute.extended_attributes.contains("ImplementedAs")) {
            auto implemented_as = attribute.extended_attributes.get("ImplementedAs").value();
            attribute_generator.set("attribute.cpp_getter_name", implemented_as);
        } else {
            attribute_generator.set("attribute.cpp_getter_name", attribute.name.to_snakecase());
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
    auto* impl = TRY(impl_from(vm, global_object));
)~~~");

        if (attribute.extended_attributes.contains("Reflect")) {
            if (attribute.type->name != "boolean") {
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
    auto retval = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl->@attribute.cpp_getter_name@(); }));
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
    auto* impl = TRY(impl_from(vm, global_object));

    auto value = vm.argument(0);
)~~~");

            generate_to_cpp(generator, attribute, "value", "", "cpp_value", interface, attribute.extended_attributes.contains("LegacyNullToEmptyString"));

            if (attribute.extended_attributes.contains("Reflect")) {
                if (attribute.type->name != "boolean") {
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
    TRY(throw_dom_exception_if_needed(global_object, [&] { return impl->set_@attribute.name:snakecase@(cpp_value); }));
)~~~");
            }

            attribute_generator.append(R"~~~(
    return JS::js_undefined();
}
)~~~");
        }
    }

    // Implementation: Functions
    for (auto& function : interface.functions) {
        generate_function(generator, function, StaticFunction::No, interface.prototype_class, interface.fully_qualified_name, interface);
    }

    if (interface.has_stringifier) {
        auto stringifier_generator = generator.fork();
        stringifier_generator.set("class_name", interface.prototype_class);
        if (interface.stringifier_attribute.has_value())
            stringifier_generator.set("attribute.cpp_getter_name", interface.stringifier_attribute->to_snakecase());

        stringifier_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@class_name@::to_string)
{
    auto* impl = TRY(impl_from(vm, global_object));

)~~~");
        if (interface.stringifier_attribute.has_value()) {
            stringifier_generator.append(R"~~~(
    auto retval = impl->@attribute.cpp_getter_name@();
)~~~");
        } else {
            stringifier_generator.append(R"~~~(
    auto retval = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl->to_string(); }));
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
    auto* impl = TRY(impl_from(vm, global_object));

    return wrap(global_object, @iterator_name@::create(*impl, Object::PropertyKind::KeyAndValue));
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::for_each)
{
    auto* impl = TRY(impl_from(vm, global_object));

    auto callback = vm.argument(0);
    if (!callback.is_function())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAFunction, callback.to_string_without_side_effects());

    auto this_value = vm.this_value(global_object);
    TRY(impl->for_each([&](auto key, auto value) -> JS::ThrowCompletionOr<void> {
)~~~");
        generate_variable_statement(iterator_generator, "wrapped_key", interface.pair_iterator_types->get<0>(), "key", interface);
        generate_variable_statement(iterator_generator, "wrapped_value", interface.pair_iterator_types->get<1>(), "value", interface);
        iterator_generator.append(R"~~~(
        TRY(call(global_object, callback.as_function(), vm.argument(1), wrapped_value, wrapped_key, this_value));
        return {};
    }));

    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::keys)
{
    auto* impl = TRY(impl_from(vm, global_object));

    return wrap(global_object, @iterator_name@::create(*impl, Object::PropertyKind::Key));
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::values)
{
    auto* impl = TRY(impl_from(vm, global_object));

    return wrap(global_object, @iterator_name@::create(*impl, Object::PropertyKind::Value));
}
)~~~");
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

void generate_iterator_header(IDL::Interface const& interface)
{
    VERIFY(interface.pair_iterator_types.has_value());
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", String::formatted("{}Iterator", interface.name));
    generator.set("fully_qualified_name", String::formatted("{}Iterator", interface.fully_qualified_name));
    generator.set("wrapper_class", String::formatted("{}IteratorWrapper", interface.name));

    generator.append(R"~~~(
#pragma once

#include <LibWeb/Bindings/Wrapper.h>

namespace Web::Bindings {

class @wrapper_class@ : public Wrapper {
    JS_OBJECT(@name@, Wrapper);
public:
    static @wrapper_class@* create(JS::GlobalObject&, @fully_qualified_name@&);

    @wrapper_class@(JS::GlobalObject&, @fully_qualified_name@&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~@wrapper_class@() override;

    @fully_qualified_name@& impl() { return *m_impl; }
    @fully_qualified_name@ const& impl() const { return *m_impl; }

private:
    virtual void visit_edges(Cell::Visitor&) override; // The Iterator implementation has to visit the wrapper it's iterating

    NonnullRefPtr<@fully_qualified_name@> m_impl;
};

@wrapper_class@* wrap(JS::GlobalObject&, @fully_qualified_name@&);

} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

void generate_iterator_implementation(IDL::Interface const& interface)
{
    VERIFY(interface.pair_iterator_types.has_value());
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", String::formatted("{}Iterator", interface.name));
    generator.set("fully_qualified_name", String::formatted("{}Iterator", interface.fully_qualified_name));
    generator.set("prototype_class", String::formatted("{}IteratorPrototype", interface.name));
    generator.set("wrapper_class", String::formatted("{}IteratorWrapper", interface.name));

    generator.append(R"~~~(
#include <AK/FlyString.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/WindowObject.h>

)~~~");

    for (auto& path : interface.imported_paths)
        generate_include_for(generator, path);

    emit_includes_for_all_imports(interface, generator, false, true);

    generator.append(R"~~~(

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;

namespace Web::Bindings {

@wrapper_class@* @wrapper_class@::create(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
{
    return global_object.heap().allocate<@wrapper_class@>(global_object, global_object, impl);
}

@wrapper_class@::@wrapper_class@(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
    : Wrapper(static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_class@>("@name@"))
    , m_impl(impl)
{
}

void @wrapper_class@::initialize(JS::GlobalObject& global_object)
{
    Wrapper::initialize(global_object);
}

@wrapper_class@::~@wrapper_class@()
{
}

void @wrapper_class@::visit_edges(Cell::Visitor& visitor)
{
    Wrapper::visit_edges(visitor);
    impl().visit_edges(visitor);
}

@wrapper_class@* wrap(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
{
    return static_cast<@wrapper_class@*>(wrap_impl(global_object, impl));
}

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
    explicit @prototype_class@(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
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
    generator.set("wrapper_class", String::formatted("{}IteratorWrapper", interface.name));
    generator.set("fully_qualified_name", String::formatted("{}Iterator", interface.fully_qualified_name));
    generator.set("possible_include_path", String::formatted("{}Iterator", interface.name.replace("::", "/")));

    generator.append(R"~~~(
#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/WindowObject.h>

#if __has_include(<LibWeb/@possible_include_path@.h>)
#    include <LibWeb/@possible_include_path@.h>
#endif
)~~~");

    emit_includes_for_all_imports(interface, generator, false, true);

    generator.append(R"~~~(
// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::NavigationTiming;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;
using namespace Web::XHR;
using namespace Web::URL;

namespace Web::Bindings {

@prototype_class@::@prototype_class@(JS::GlobalObject& global_object)
    : Object(*global_object.iterator_prototype())
{
}

@prototype_class@::~@prototype_class@()
{
}

void @prototype_class@::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_native_function(vm.names.next, next, 0, JS::Attribute::Configurable | JS::Attribute::Writable);
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Iterator"), JS::Attribute::Configurable);
}

static JS::ThrowCompletionOr<@fully_qualified_name@*> impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    if (!is<@wrapper_class@>(this_object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@fully_qualified_name@");
    return &static_cast<@wrapper_class@*>(this_object)->impl();
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::next)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return TRY(throw_dom_exception_if_needed(global_object, [&] { return impl->next(); }));
}

} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

}
