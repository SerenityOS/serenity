/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibIDL/IDLParser.h>
#include <LibIDL/Types.h>
#include <LibMain/Main.h>

static ErrorOr<void> add_to_interface_sets(IDL::Interface&, Vector<IDL::Interface&>& window_exposed, Vector<IDL::Interface&>& dedicated_worker_exposed, Vector<IDL::Interface&>& shared_worker_exposed);
static String s_error_string;

static ErrorOr<void> generate_exposed_interface_header(StringView class_name, StringView output_path)
{
    StringBuilder builder;
    SourceGenerator generator(builder);

    generator.set("global_object_snake_name", String(class_name).to_snakecase());
    generator.append(R"~~~(
#pragma once

#include <LibJS/Forward.h>

namespace Web::Bindings {

void add_@global_object_snake_name@_exposed_interfaces(JS::Object&, JS::Realm&);

}

)~~~");

    auto generated_header_path = LexicalPath(output_path).append(String::formatted("{}ExposedInterfaces.h", class_name)).string();
    auto generated_header_file = TRY(Core::Stream::File::open(generated_header_path, Core::Stream::OpenMode::Write));
    TRY(generated_header_file->write(generator.as_string_view().bytes()));

    return {};
}

static ErrorOr<void> generate_exposed_interface_implementation(StringView class_name, StringView output_path, Vector<IDL::Interface&>& exposed_interfaces)
{
    StringBuilder builder;
    SourceGenerator generator(builder);

    generator.set("global_object_name", class_name);
    generator.set("global_object_snake_name", String(class_name).to_snakecase());

    generator.append(R"~~~(
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/@global_object_name@ExposedInterfaces.h>
)~~~");
    for (auto& interface : exposed_interfaces) {
        auto gen = generator.fork();
        gen.set("prototype_class", interface.prototype_class);
        gen.set("constructor_class", interface.constructor_class);

        gen.append(R"~~~(#include <LibWeb/Bindings/@constructor_class@.h>
)~~~");
        if (interface.parent_name != "[Synthetic Interface]"sv)
            gen.append(R"~~~(#include <LibWeb/Bindings/@prototype_class@.h>
)~~~");
    }

    // FIXME: Special case window. We should convert Window and Location to use IDL
    if (class_name == "Window"sv) {
        generator.append(R"~~~(#include <LibWeb/Bindings/WindowConstructor.h>
#include <LibWeb/Bindings/WindowPrototype.h>
#include <LibWeb/Bindings/LocationConstructor.h>
#include <LibWeb/Bindings/LocationPrototype.h>
)~~~");
    }

    generator.append(R"~~~(
namespace Web::Bindings {

void add_@global_object_snake_name@_exposed_interfaces(JS::Object& global, JS::Realm& realm)
{
    auto& vm = global.vm();
    // FIXME: Should we use vm.current_realm() here?
)~~~");

    auto add_interface = [](SourceGenerator& gen, StringView name, StringView prototype_class, StringView constructor_class) {
        gen.set("interface_name", name);
        gen.set("prototype_class", prototype_class);
        gen.set("constructor_class", constructor_class);

        gen.append(R"~~~(    {
        auto& prototype = Bindings::ensure_web_prototype<Bindings::@prototype_class@>(realm, "@interface_name@");
        auto& constructor = Bindings::ensure_web_constructor<Bindings::@constructor_class@>(realm, "@interface_name@");
        global.define_direct_property("@interface_name@", &constructor, JS::Attribute::Writable | JS::Attribute::Configurable);
        prototype.define_direct_property(vm.names.constructor, &constructor, JS::Attribute::Writable | JS::Attribute::Configurable);
        constructor.define_direct_property(vm.names.name, js_string(vm, "@interface_name@"), JS::Attribute::Configurable);
    }
)~~~"); };

    for (auto& interface : exposed_interfaces) {
        auto gen = generator.fork();
        add_interface(gen, interface.name, interface.prototype_class, interface.constructor_class);
    }

    // FIXME: Special case window. We should convert Window and Location to use IDL
    if (class_name == "Window"sv) {
        auto gen = generator.fork();
        add_interface(gen, "Window"sv, "WindowPrototype"sv, "WindowConstructor"sv);
        add_interface(gen, "Location"sv, "LocationPrototype"sv, "LocationConstructor"sv);
    }

    generator.append(R"~~~(
}
}
)~~~");
    auto generated_implementation_path = LexicalPath(output_path).append(String::formatted("{}ExposedInterfaces.cpp", class_name)).string();
    auto generated_implementation_file = TRY(Core::Stream::File::open(generated_implementation_path, Core::Stream::OpenMode::Write));
    TRY(generated_implementation_file->write(generator.as_string_view().bytes()));

    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView output_path;
    StringView base_path;
    Vector<String> paths;

    args_parser.add_option(output_path, "Path to output generated files into", "output-path", 'o', "output-path");
    args_parser.add_option(base_path, "Path to root of IDL file tree", "base-path", 'b', "base-path");
    args_parser.add_positional_argument(paths, "Paths of every IDL file that could be Exposed", "paths");
    args_parser.parse(arguments);

    VERIFY(!paths.is_empty());
    VERIFY(!base_path.is_empty());

    const LexicalPath lexical_base(base_path);

    // Read in all IDL files, we must own the storage for all of these for the lifetime of the program
    Vector<String> file_contents;
    for (String const& path : paths) {
        auto file_or_error = Core::Stream::File::open(path, Core::Stream::OpenMode::Read);
        if (file_or_error.is_error()) {
            s_error_string = String::formatted("Unable to open file {}", path);
            return Error::from_string_view(s_error_string);
        }
        auto file = file_or_error.release_value();
        auto string = MUST(file->read_all());
        file_contents.append(String(ReadonlyBytes(string)));
    }
    VERIFY(paths.size() == file_contents.size());

    Vector<IDL::Parser> parsers;
    Vector<IDL::Interface&> window_exposed;
    Vector<IDL::Interface&> dedicated_worker_exposed;
    Vector<IDL::Interface&> shared_worker_exposed;
    // TODO: service_worker_exposed

    for (size_t i = 0; i < paths.size(); ++i) {
        IDL::Parser parser(paths[i], file_contents[i], lexical_base.string());
        TRY(add_to_interface_sets(parser.parse(), window_exposed, dedicated_worker_exposed, shared_worker_exposed));
        parsers.append(move(parser));
    }

    TRY(generate_exposed_interface_header("Window"sv, output_path));
    TRY(generate_exposed_interface_header("DedicatedWorker"sv, output_path));
    TRY(generate_exposed_interface_header("SharedWorker"sv, output_path));
    // TODO: ServiceWorkerExposed.h

    TRY(generate_exposed_interface_implementation("Window"sv, output_path, window_exposed));
    TRY(generate_exposed_interface_implementation("DedicatedWorker"sv, output_path, dedicated_worker_exposed));
    TRY(generate_exposed_interface_implementation("SharedWorker"sv, output_path, shared_worker_exposed));
    // TODO: ServiceWorkerExposed.cpp

    return 0;
}

static void consume_whitespace(GenericLexer& lexer)
{
    bool consumed = true;
    while (consumed) {
        consumed = lexer.consume_while(is_ascii_space).length() > 0;

        if (lexer.consume_specific("//")) {
            lexer.consume_until('\n');
            lexer.ignore();
            consumed = true;
        }
    }
}

enum ExposedTo {
    Nobody = 0x0,
    DedicatedWorker = 0x1,
    SharedWorker = 0x2,
    ServiceWorker = 0x4,
    AudioWorklet = 0x8,
    Window = 0x10,
    AllWorkers = 0xF, // FIXME: Is "AudioWorklet" a Worker? We'll assume it is for now
    All = 0x1F,
};
AK_ENUM_BITWISE_OPERATORS(ExposedTo);

static ErrorOr<ExposedTo> parse_exposure_set(IDL::Interface& interface)
{
    // NOTE: This roughly follows the definitions of https://webidl.spec.whatwg.org/#Exposed
    //       It does not remotely interpret all the abstract operations therein though.

    auto maybe_exposed = interface.extended_attributes.get("Exposed");
    if (!maybe_exposed.has_value()) {
        s_error_string = String::formatted("Interface {} is missing extended attribute Exposed", interface.name);
        return Error::from_string_view(s_error_string);
    }
    auto exposed = maybe_exposed.value().trim_whitespace();
    if (exposed == "*"sv)
        return ExposedTo::All;
    if (exposed == "Window"sv)
        return ExposedTo::Window;
    if (exposed == "Worker"sv)
        return ExposedTo::AllWorkers;
    if (exposed == "AudioWorklet"sv)
        return ExposedTo::AudioWorklet;

    if (exposed[0] == '(') {
        ExposedTo whom = Nobody;
        for (StringView candidate : exposed.substring_view(1, exposed.length() - 1).split_view(',')) {
            candidate = candidate.trim_whitespace();
            if (candidate == "Window"sv) {
                whom |= ExposedTo::Window;
            } else if (candidate == "Worker"sv) {
                whom |= ExposedTo::AllWorkers;
            } else if (candidate == "DedicatedWorker"sv) {
                whom |= ExposedTo::DedicatedWorker;
            } else if (candidate == "SharedWorker"sv) {
                whom |= ExposedTo::SharedWorker;
            } else if (candidate == "ServiceWorker"sv) {
                whom |= ExposedTo::ServiceWorker;
            } else if (candidate == "AudioWorklet"sv) {
                whom |= ExposedTo::AudioWorklet;
            } else {
                s_error_string = String::formatted("Unknown Exposed attribute candidate {} in {} in {}", candidate, exposed, interface.name);
                return Error::from_string_view(s_error_string);
            }
        }
        if (whom == ExposedTo::Nobody) {
            s_error_string = String::formatted("Unknown Exposed attribute {} in {}", exposed, interface.name);
            return Error::from_string_view(s_error_string);
        }
        return whom;
    }

    s_error_string = String::formatted("Unknown Exposed attribute {} in {}", exposed, interface.name);
    return Error::from_string_view(s_error_string);
}

static IDL::Interface& add_synthetic_interface(IDL::Interface& reference_interface)
{
    static Vector<NonnullOwnPtr<IDL::Interface>> s_synthetic_interfaces;

    GenericLexer function_lexer(reference_interface.extended_attributes.get("LegacyFactoryFunction").value());
    consume_whitespace(function_lexer);
    auto name = function_lexer.consume_until([](auto ch) { return is_ascii_space(ch) || ch == '('; });

    auto new_interface = make<IDL::Interface>();
    new_interface->name = name;
    new_interface->constructor_class = String::formatted("{}Constructor", new_interface->name);
    new_interface->prototype_class = reference_interface.prototype_class;
    new_interface->parent_name = "[Synthetic Interface]";

    s_synthetic_interfaces.append(move(new_interface));
    return *s_synthetic_interfaces.last();
}

ErrorOr<void> add_to_interface_sets(IDL::Interface& interface, Vector<IDL::Interface&>& window_exposed, Vector<IDL::Interface&>& dedicated_worker_exposed, Vector<IDL::Interface&>& shared_worker_exposed)
{
    // TODO: Add service worker exposed and audio worklet exposed
    auto whom = TRY(parse_exposure_set(interface));
    VERIFY(whom != ExposedTo::Nobody);

    if (whom & ExposedTo::Window)
        window_exposed.append(interface);

    if (whom & ExposedTo::DedicatedWorker)
        dedicated_worker_exposed.append(interface);

    if (whom & ExposedTo::SharedWorker)
        shared_worker_exposed.append(interface);

    if (interface.extended_attributes.contains("LegacyFactoryFunction")) {
        auto& synthetic_interface = add_synthetic_interface(interface);
        if (whom & ExposedTo::Window)
            window_exposed.append(synthetic_interface);

        if (whom & ExposedTo::DedicatedWorker)
            dedicated_worker_exposed.append(synthetic_interface);

        if (whom & ExposedTo::SharedWorker)
            shared_worker_exposed.append(synthetic_interface);
    }

    return {};
}
