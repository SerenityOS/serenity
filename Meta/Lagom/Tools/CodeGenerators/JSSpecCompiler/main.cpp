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
#include "Compiler/IfBranchMergingPass.h"
#include "Compiler/ReferenceResolvingPass.h"
#include "Function.h"
#include "Parser/SpecParser.h"

ErrorOr<int> serenity_main(Main::Arguments)
{
    using namespace JSSpecCompiler;

    ExecutionContext context;

    // Functions referenced in DifferenceISODate
    // TODO: This is here just for testing. In a long run, we need some place, which is not
    //       `serenity_main`, to store built-in functions.
    auto& functions = context.m_functions;
    functions.set("CompareISODate"sv, make_ref_counted<FunctionPointer>("CompareISODate"sv));
    functions.set("CreateDateDurationRecord"sv, make_ref_counted<FunctionPointer>("CreateDateDurationRecord"sv));
    functions.set("AddISODate"sv, make_ref_counted<FunctionPointer>("AddISODate"sv));
    functions.set("ISODaysInMonth"sv, make_ref_counted<FunctionPointer>("ISODaysInMonth"sv));
    functions.set("ISODateToEpochDays"sv, make_ref_counted<FunctionPointer>("ISODateToEpochDays"sv));
    functions.set("truncate"sv, make_ref_counted<FunctionPointer>("truncate"sv));
    functions.set("remainder"sv, make_ref_counted<FunctionPointer>("remainder"sv));

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

    for (auto const& argument : spec_function.m_arguments)
        function->m_local_variables.set(argument.name, make_ref_counted<VariableDeclaration>(argument.name));

    FunctionCallCanonicalizationPass(function).run();
    IfBranchMergingPass(function).run();
    ReferenceResolvingPass(function).run();

    out("{}", function->m_ast);
    return 0;
}
