/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CharacterTypes.h>
#include <AK/Optional.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace AST {

class Annotation {
public:
    enum class Type {
        String,
        I32,
        Bool
    };

    explicit Annotation(Type type)
        : m_type(type)
    {
    }

    Type type() const { return m_type; }
    String cpp_return_type() const;
    String cpp_argument_type() const;

    void generate_reader(SourceGenerator&) const;
    void generate_writer(SourceGenerator&) const;

private:
    String config_type_name() const;

    Type m_type {};
};

class Option {
public:
    Option(Annotation annotation, String name, String default_value)
        : m_annotation(annotation)
        , m_name(move(name))
        , m_default_value(move(default_value))
    {
    }

    void generate_header(SourceGenerator&) const;
    void generate_source(SourceGenerator&) const;

private:
    Annotation m_annotation;
    String m_name;
    String m_default_value;
};

class Group {
public:
    explicit Group(String name)
        : m_name(move(name))
    {
    }

    void add_option(Option opt) { m_options.append(move(opt)); }

    void generate_header(SourceGenerator&) const;
    void generate_source(SourceGenerator&) const;

private:
    String m_name;
    Vector<Option> m_options;
};

class ConfigFile {
public:
    ConfigFile(String domain)
        : m_domain(move(domain))
    {
    }

    void add_group(Group grp)
    {
        m_groups.append(move(grp));
    }

    void generate_header(SourceGenerator&) const;
    void generate_source(SourceGenerator&) const;

private:
    String m_domain;
    Vector<Group> m_groups;
};

}
