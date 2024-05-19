/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <clang/AST/ASTConsumer.h>
#include <clang/AST/RecursiveASTVisitor.h>
#include <clang/Frontend/FrontendAction.h>

struct LibJSCellMacro {
    enum class Type {
        JSCell,
        JSObject,
        JSEnvironment,
        JSPrototypeObject,
        WebPlatformObject,
    };

    struct Arg {
        std::string text;
        clang::SourceLocation location;
    };

    clang::SourceRange range;
    Type type;
    std::vector<Arg> args;

    static char const* type_name(Type);
};

using LibJSCellMacroMap = std::unordered_map<unsigned int, std::vector<LibJSCellMacro>>;

class LibJSPPCallbacks : public clang::PPCallbacks {
public:
    LibJSPPCallbacks(clang::Preprocessor& preprocessor, LibJSCellMacroMap& macro_map)
        : m_preprocessor(preprocessor)
        , m_macro_map(macro_map)
    {
    }

    virtual void LexedFileChanged(clang::FileID curr_fid, LexedFileChangeReason, clang::SrcMgr::CharacteristicKind, clang::FileID, clang::SourceLocation) override;

    virtual void MacroExpands(clang::Token const& name_token, clang::MacroDefinition const& definition, clang::SourceRange range, clang::MacroArgs const* args) override;

private:
    clang::Preprocessor& m_preprocessor;
    std::vector<unsigned int> m_curr_fid_hash_stack;
    LibJSCellMacroMap& m_macro_map;
};

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
