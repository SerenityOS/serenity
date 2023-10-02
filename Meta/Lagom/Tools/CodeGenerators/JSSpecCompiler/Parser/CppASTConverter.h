/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibCpp/AST.h>
#include <LibCpp/Parser.h>

#include "CompilationPipeline.h"

namespace JSSpecCompiler {

class CppASTConverter {
public:
    CppASTConverter(RefPtr<Cpp::FunctionDeclaration> const& function)
        : m_function(function)
    {
    }

    NonnullRefPtr<FunctionDefinition> convert();

private:
    template<typename T>
    NullableTree convert_node(T const&);
    NullableTree as_nullable_tree(Cpp::Statement const* statement);
    Tree as_tree(Cpp::Statement const* statement);
    Tree as_possibly_empty_tree(Cpp::Statement const* statement);

    RefPtr<Cpp::FunctionDeclaration> m_function;
};

class CppParsingStep : public CompilationStep {
public:
    CppParsingStep();
    ~CppParsingStep();

    void run(TranslationUnitRef translation_unit) override;

private:
    OwnPtr<Cpp::Parser> m_parser;
    ByteBuffer m_input;
};

}
