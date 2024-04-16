/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <clang/AST/ASTConsumer.h>
#include <clang/AST/RecursiveASTVisitor.h>
#include <clang/Frontend/FrontendAction.h>

class LibJSGCVisitor : public clang::RecursiveASTVisitor<LibJSGCVisitor> {
public:
    explicit LibJSGCVisitor(clang::ASTContext& context)
        : m_context(context)
    {
    }

    bool VisitCXXRecordDecl(clang::CXXRecordDecl*);

private:
    clang::ASTContext& m_context;
};

class LibJSGCASTConsumer : public clang::ASTConsumer {
public:
    virtual void HandleTranslationUnit(clang::ASTContext& context) override;
};

class LibJSGCPluginAction : public clang::PluginASTAction {
public:
    virtual bool ParseArgs(clang::CompilerInstance const&, std::vector<std::string> const&) override
    {
        return true;
    }

    virtual std::unique_ptr<clang::ASTConsumer> CreateASTConsumer(clang::CompilerInstance&, llvm::StringRef) override
    {
        return std::make_unique<LibJSGCASTConsumer>();
    }

    ActionType getActionType() override
    {
        return AddAfterMainAction;
    }
};
