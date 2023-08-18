/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>
#include <LibXML/Parser/Parser.h>

#include "Compiler/FunctionCallCanonicalizationPass.h"
#include "Function.h"
#include "Parser/SpecParser.h"

ErrorOr<int> serenity_main(Main::Arguments)
{
    using namespace JSSpecCompiler;

    ExecutionContext context;

    auto input = TRY(TRY(Core::File::standard_input())->read_until_eof());
    XML::Parser parser { StringView(input.bytes()) };

    auto maybe_document = parser.parse();
    if (maybe_document.is_error()) {
        outln("{}", maybe_document.error());
        return 1;
    }
    auto document = maybe_document.release_value();

    auto maybe_function = JSSpecCompiler::SpecFunction::create(&document.root());
    if (maybe_function.is_error()) {
        outln("{}", maybe_function.error()->to_string());
        return 1;
    }
    auto spec_function = maybe_function.value();

    auto function = make_ref_counted<JSSpecCompiler::Function>(&context, spec_function.m_name, spec_function.m_algorithm.m_tree);

    FunctionCallCanonicalizationPass(function).run();

    out("{}", function->m_ast);
    return 0;
}
