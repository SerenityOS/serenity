/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CellsHandler.h"
#include <clang/Tooling/CommonOptionsParser.h>
#include <clang/Tooling/Tooling.h>
#include <llvm/Support/CommandLine.h>

int main(int argc, char const** argv)
{
    llvm::cl::OptionCategory s_tool_category("LibJSGCVerifier options");
    auto maybe_parser = clang::tooling::CommonOptionsParser::create(argc, argv, s_tool_category);
    if (!maybe_parser) {
        llvm::errs() << maybe_parser.takeError();
        return 1;
    }

    auto& parser = maybe_parser.get();
    clang::tooling::ClangTool tool(parser.getCompilations(), parser.getSourcePathList());

    CollectCellsHandler collect_handler;
    auto collect_action = clang::tooling::newFrontendActionFactory(&collect_handler.finder(), &collect_handler);
    return tool.run(collect_action.get());
}
