/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <clang/AST/ASTConsumer.h>
#include <clang/AST/DeclCXX.h>
#include <clang/AST/DeclTemplate.h>
#include <clang/AST/RecursiveASTVisitor.h>
#include <clang/Basic/DiagnosticFrontend.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Frontend/FrontendAction.h>
#include <clang/Frontend/FrontendPluginRegistry.h>
#include <clang/Tooling/Tooling.h>

#include <AK/CharacterTypes.h>

bool type_inherits_from_refcounted(clang::QualType const& type)
{
    if (auto const* template_type = type->getAs<clang::TemplateSpecializationType>()) {
        if (auto const* class_decl = template_type->getAsCXXRecordDecl()) {
            auto bases = class_decl->bases();
            for (auto const& base : bases) {
                if (type_inherits_from_refcounted(base.getType()))
                    return true;
            }
        }
    } else if (auto const* decl = type->getAsCXXRecordDecl()) {
        if (decl->getDeclName().getAsString() == "RefCountedBase")
            return true;
        if (decl->getDeclName().getAsString() == "AtomicRefCountedBase")
            return true;

        for (auto const& base : decl->bases()) {
            if (type_inherits_from_refcounted(base.getType()))
                return true;
        }
    }
    return false;
}

class FindRefPtrClassVisitor
    : public clang::RecursiveASTVisitor<FindRefPtrClassVisitor> {
public:
    explicit FindRefPtrClassVisitor(clang::ASTContext* Context)
        : Context(Context)
    {
    }

    bool VisitCXXRecordDecl(clang::CXXRecordDecl const* Declaration)
    {
        // FIXME: This does not seem to work with clangd
        if (!Declaration->getDefinition())
            return true;
        if (Declaration->getNumBases() == 0)
            return true;

        // Only check serenity Types, which are always TitleCase
        // Also ignore anonymous declarations
        if (Declaration->getName().empty() || !AK::is_ascii_upper_alpha(Declaration->getName().front()))
            return true;

        auto bases = Declaration->bases();

        int index = 0;
        for (auto const& base : bases) {
            if (type_inherits_from_refcounted(base.getType())) {
                if (index != 0) {
                    std::string err = Declaration->getNameAsString();
                    err.append(" inherits from [Atomic]RefCountedBase, but [Atomic]RefCountedBase is not the first inherited class, inherited through: '");
                    err.append(base.getType().getAsString());
                    err.append("'.");
                    Context->getDiagnostics().Report(base.getBeginLoc(), clang::diag::warn_fe_backend_plugin) << err;
                    return false;
                }
            }
            index++;
        }
        return true;
    }

private:
    clang::ASTContext* Context;
};

class FindNamedClassConsumer : public clang::ASTConsumer {
public:
    explicit FindNamedClassConsumer(clang::ASTContext* Context)
        : Visitor(Context)
    {
    }

    virtual void HandleTranslationUnit(clang::ASTContext& Context)
    {
        Visitor.TraverseDecl(Context.getTranslationUnitDecl());
    }

private:
    FindRefPtrClassVisitor Visitor;
};

class CheckRefPtrs : public clang::PluginASTAction {
public:
    virtual std::unique_ptr<clang::ASTConsumer> CreateASTConsumer(clang::CompilerInstance& Compiler, llvm::StringRef InFile) override
    {
        (void)InFile;
        return std::make_unique<FindNamedClassConsumer>(&Compiler.getASTContext());
    }

    virtual bool ParseArgs(clang::CompilerInstance const&, std::vector<std::string> const&) override
    {
        return true;
    };
    PluginASTAction::ActionType getActionType() override
    {
        return AddBeforeMainAction;
    }
};

static clang::FrontendPluginRegistry::Add<CheckRefPtrs> X("Check RefPtrs", "Check if RefPtr inheritance is done in the correct order");
