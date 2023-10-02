/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

#include "Compiler/Passes/FunctionCallCanonicalizationPass.h"
#include "Compiler/Passes/IfBranchMergingPass.h"
#include "Compiler/Passes/ReferenceResolvingPass.h"
#include "Function.h"
#include "Parser/CppASTConverter.h"
#include "Parser/SpecParser.h"

using namespace JSSpecCompiler;

class CompilationPipeline {
public:
    template<typename T>
    void add_compilation_pass()
    {
        auto func = +[](TranslationUnitRef translation_unit) {
            T { translation_unit }.run();
        };
        add_step(adopt_own_if_nonnull(new NonOwningCompilationStep(T::name, func)));
    }

    template<typename T>
    void for_each_step_in(StringView pass_list, T&& func)
    {
        HashTable<StringView> selected_steps;
        for (auto pass : pass_list.split_view(',')) {
            if (pass == "all") {
                for (auto const& step : m_pipeline)
                    selected_steps.set(step->name());
            } else if (pass == "last") {
                selected_steps.set(m_pipeline.last()->name());
            } else if (pass.starts_with('-')) {
                VERIFY(selected_steps.remove(pass.substring_view(1)));
            } else {
                selected_steps.set(pass);
            }
        }

        for (auto& step : m_pipeline)
            if (selected_steps.contains(step->name()))
                func(step);
    }

    void add_step(OwnPtr<CompilationStep>&& step)
    {
        m_pipeline.append(move(step));
    }

    auto const& pipeline() const { return m_pipeline; }

private:
    Vector<OwnPtr<CompilationStep>> m_pipeline;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView filename;
    args_parser.add_positional_argument(filename, "File to compile", "file");

    constexpr StringView language_spec = "spec"sv;
    constexpr StringView language_cpp = "c++"sv;
    StringView language = language_spec;
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Optional,
        .help_string = "Specify the language of the input file.",
        .short_name = 'x',
        .value_name = "{c++|spec}",
        .accept_value = [&](StringView value) {
            language = value;
            return language.is_one_of(language_spec, language_cpp);
        },
    });

    args_parser.parse(arguments);

    CompilationPipeline pipeline;
    if (language == language_cpp)
        pipeline.add_step(adopt_own_if_nonnull(new CppParsingStep()));
    else
        pipeline.add_step(adopt_own_if_nonnull(new SpecParsingStep()));
    pipeline.add_compilation_pass<FunctionCallCanonicalizationPass>();
    pipeline.add_compilation_pass<IfBranchMergingPass>();
    pipeline.add_compilation_pass<ReferenceResolvingPass>();

    TranslationUnit translation_unit;
    translation_unit.filename = filename;

    // Functions referenced in DifferenceISODate
    // TODO: This is here just for testing. In a long run, we need some place, which is not
    //       `serenity_main`, to store built-in functions.
    auto& functions = translation_unit.function_index;
    functions.set("CompareISODate"sv, make_ref_counted<FunctionPointer>("CompareISODate"sv));
    functions.set("CreateDateDurationRecord"sv, make_ref_counted<FunctionPointer>("CreateDateDurationRecord"sv));
    functions.set("AddISODate"sv, make_ref_counted<FunctionPointer>("AddISODate"sv));
    functions.set("ISODaysInMonth"sv, make_ref_counted<FunctionPointer>("ISODaysInMonth"sv));
    functions.set("ISODateToEpochDays"sv, make_ref_counted<FunctionPointer>("ISODateToEpochDays"sv));
    functions.set("truncate"sv, make_ref_counted<FunctionPointer>("truncate"sv));
    functions.set("remainder"sv, make_ref_counted<FunctionPointer>("remainder"sv));

    for (auto const& step : pipeline.pipeline())
        step->run(&translation_unit);

    return 0;
}
