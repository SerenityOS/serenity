/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AST.h"
#include "Utils.h"

namespace AST {

String Annotation::cpp_return_type() const
{
    switch (m_type) {
    case Type::String:
        return "String";
    case Type::I32:
        return "i32";
    case Type::Bool:
        return "bool";
    }
    VERIFY_NOT_REACHED();
}

String Annotation::cpp_argument_type() const
{
    switch (m_type) {
    case Type::String:
        return "String const&";
    case Type::I32:
        return "i32";
    case Type::Bool:
        return "bool";
    }
    VERIFY_NOT_REACHED();
}

String Annotation::config_type_name() const
{
    switch (m_type) {
    case Type::String:
        return "string";
    case Type::I32:
        return "i32";
    case Type::Bool:
        return "bool";
    }
    VERIFY_NOT_REACHED();
}

String Annotation::cpp_value(String const& value) const
{
    // TODO: Escape strings
    if (m_type == Type::String)
        return String::formatted("\"{}\"", value);
    return value;
}

void Annotation::generate_reader(SourceGenerator& generator) const
{
    generator.set("option.config_type", config_type_name());
    generator.appendln(R"~~~(    auto value = ::Config::read_@option.config_type@("@config.domain@", "@group.name@", "@option.name@", @option.default_value@);)~~~");

    if (!m_allowed_values.is_empty()) {
        generator.append("    if (!(");
        for (size_t i = 0; auto const& value : m_allowed_values) {
            generator.set("option.allowed_value", cpp_value(value));
            generator.append(R"~~~(
        value == @option.allowed_value@)~~~");
            if (i != m_allowed_values.size() - 1)
                generator.append(" || ");
            i++;
        }
        generator.appendln(R"~~~(
    )) {
        dbgln("@config.domain@: Invalid value read for @group.name@::@option.name@");
        return @option.default_value@;
    }
)~~~");
    }

    generator.appendln("    return value;");
}

void Annotation::generate_writer(SourceGenerator& generator) const
{
    generator.set("option.config_type", config_type_name());

    if (!m_allowed_values.is_empty()) {
        generator.append("    if (!(");
        for (size_t i = 0; auto const& value : m_allowed_values) {
            generator.set("option.allowed_value", cpp_value(value));
            generator.append(R"~~~(
        value == @option.allowed_value@)~~~");
            if (i != m_allowed_values.size() - 1)
                generator.append(" || ");
            i++;
        }
        generator.appendln(R"~~~(
    )) {
        dbgln("@config.domain@: Tried to write invalid value for @group.name@::@option.name@");
        return;
    }
)~~~");
    }

    generator.appendln(R"~~~(    ::Config::write_@option.config_type@("@config.domain@", "@group.name@", "@option.name@", value);)~~~");
}

void Option::generate_header(SourceGenerator& generator) const
{
    generator.set("option.name", m_name);
    if (!is_valid_cpp_identifier(m_name)) {
        generator.appendln("\n// Option @option.name@ not generated because its name is not a valid C++ identifier.");
        return;
    }
    generator.set("option.name.snakecase", m_name.to_snakecase());
    generator.set("option.cpp_argument_type", m_annotation.cpp_argument_type());
    generator.set("option.cpp_return_type", m_annotation.cpp_return_type());

    generator.append(R"~~~(
@option.cpp_return_type@ @option.name.snakecase@();
void set_@option.name.snakecase@(@option.cpp_argument_type@);
)~~~");
}

void Option::generate_source(SourceGenerator& generator) const
{
    generator.set("option.name", m_name);
    if (!is_valid_cpp_identifier(m_name)) {
        generator.appendln("\n// Option @option.name@ not generated because its name is not a valid C++ identifier.");
        return;
    }
    generator.set("option.name.snakecase", m_name.to_snakecase());
    generator.set("option.default_value", m_annotation.cpp_value(m_default_value));
    generator.set("option.cpp_return_type", m_annotation.cpp_return_type());

    generator.append(R"~~~(
@option.cpp_return_type@ @option.name.snakecase@()
{
)~~~");

    SourceGenerator reader_generator = generator.fork();
    m_annotation.generate_reader(reader_generator);

    generator.appendln("}");

    generator.set("option.cpp_argument_type", m_annotation.cpp_argument_type());

    generator.append(R"~~~(
void set_@option.name.snakecase@(@option.cpp_argument_type@ value)
{
)~~~");

    SourceGenerator writer_generator = generator.fork();
    m_annotation.generate_writer(writer_generator);

    generator.appendln("}");
}

void Group::generate_header(SourceGenerator& generator) const
{
    generator.set("group.name", m_name);
    if (!is_valid_cpp_identifier(m_name)) {
        generator.appendln("\n// Group @group.name@ not generated because its name is not a valid C++ identifier.");
        return;
    }
    generator.append(R"~~~(
namespace @group.name@ {
)~~~");

    for (auto const& option : m_options) {
        auto option_generator = generator.fork();
        option.generate_header(option_generator);
    }

    generator.append(R"~~~(
} // namespace @group.name@
)~~~");
}
void Group::generate_source(SourceGenerator& generator) const
{
    generator.set("group.name", m_name);
    if (!is_valid_cpp_identifier(m_name)) {
        generator.appendln("\n// Group @group.name@ not generated because its name is not a valid C++ identifier.");
        return;
    }
    generator.append(R"~~~(
namespace @group.name@ {
)~~~");

    for (auto const& option : m_options) {
        auto option_generator = generator.fork();
        option.generate_source(option_generator);
    }

    generator.append(R"~~~(
} // namespace @group.name@
)~~~");
}

void ConfigFile::generate_header(SourceGenerator& generator) const
{
    // <domain>Config.h
    generator.set("config.domain", m_domain);
    generator.append(R"~~~(#pragma once

#include <AK/String.h>
#include <AK/Types.h>

namespace @config.domain@::Config {
    )~~~");

    for (auto const& group : m_groups) {
        auto group_generator = generator.fork();
        group.generate_header(group_generator);
    }

    generator.append(R"~~~(
})~~~");
}
void ConfigFile::generate_source(SourceGenerator& generator) const
{
    // <domain>Config.cpp
    generator.set("config.domain", m_domain);
    generator.append(R"~~~(#include "@config.domain@Config.h"

#include <LibConfig/Client.h>

namespace @config.domain@::Config {
    )~~~");

    for (auto const& group : m_groups) {
        auto group_generator = generator.fork();
        group.generate_source(group_generator);
    }

    generator.append(R"~~~(
})~~~");
}

}
