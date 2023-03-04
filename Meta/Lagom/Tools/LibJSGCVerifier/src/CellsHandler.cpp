/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CellsHandler.h"
#include <clang/AST/DeclCXX.h>
#include <clang/AST/Type.h>
#include <clang/ASTMatchers/ASTMatchFinder.h>
#include <clang/ASTMatchers/ASTMatchers.h>
#include <clang/Basic/Diagnostic.h>
#include <clang/Basic/Specifiers.h>
#include <clang/Frontend/CompilerInstance.h>
#include <filesystem>
#include <llvm/Support/Casting.h>
#include <llvm/Support/raw_ostream.h>
#include <unordered_set>
#include <vector>

CollectCellsHandler::CollectCellsHandler()
{
    using namespace clang::ast_matchers;

    m_finder.addMatcher(
        traverse(
            clang::TK_IgnoreUnlessSpelledInSource,
            cxxRecordDecl(decl().bind("record-decl"))),
        this);
}

bool CollectCellsHandler::handleBeginSource(clang::CompilerInstance& ci)
{
    auto const& source_manager = ci.getSourceManager();
    ci.getFileManager().getNumUniqueRealFiles();
    auto file_id = source_manager.getMainFileID();
    auto const* file_entry = source_manager.getFileEntryForID(file_id);
    if (!file_entry)
        return false;

    auto current_filepath = std::filesystem::canonical(file_entry->getName().str());
    llvm::outs() << "Processing " << current_filepath.string() << "\n";

    return true;
}

bool record_inherits_from_cell(clang::CXXRecordDecl const& record)
{
    if (!record.isCompleteDefinition())
        return false;

    bool inherits_from_cell = record.getQualifiedNameAsString() == "JS::Cell";
    record.forallBases([&](clang::CXXRecordDecl const* base) -> bool {
        if (base->getQualifiedNameAsString() == "JS::Cell") {
            inherits_from_cell = true;
            return false;
        }
        return true;
    });
    return inherits_from_cell;
}

std::vector<clang::QualType> get_all_qualified_types(clang::QualType const& type)
{
    std::vector<clang::QualType> qualified_types;

    if (auto const* template_specialization = type->getAs<clang::TemplateSpecializationType>()) {
        auto specialization_name = template_specialization->getTemplateName().getAsTemplateDecl()->getQualifiedNameAsString();
        // Do not unwrap GCPtr/NonnullGCPtr
        if (specialization_name == "JS::GCPtr" || specialization_name == "JS::NonnullGCPtr") {
            qualified_types.push_back(type);
        } else {
            for (size_t i = 0; i < template_specialization->getNumArgs(); i++) {
                auto const& template_arg = template_specialization->getArg(i);
                if (template_arg.getKind() == clang::TemplateArgument::Type) {
                    auto template_qualified_types = get_all_qualified_types(template_arg.getAsType());
                    std::move(template_qualified_types.begin(), template_qualified_types.end(), std::back_inserter(qualified_types));
                }
            }
        }
    } else {
        qualified_types.push_back(type);
    }

    return qualified_types;
}

struct FieldValidationResult {
    bool is_valid { false };
    bool is_wrapped_in_gcptr { false };
};

FieldValidationResult validate_field(clang::FieldDecl const* field_decl)
{
    auto type = field_decl->getType();
    if (auto const* elaborated_type = llvm::dyn_cast<clang::ElaboratedType>(type.getTypePtr()))
        type = elaborated_type->desugar();

    FieldValidationResult result { .is_valid = true };

    for (auto const& qualified_type : get_all_qualified_types(type)) {
        if (auto const* pointer_decl = qualified_type->getAs<clang::PointerType>()) {
            if (auto const* pointee = pointer_decl->getPointeeCXXRecordDecl()) {
                if (record_inherits_from_cell(*pointee)) {
                    result.is_valid = false;
                    result.is_wrapped_in_gcptr = false;
                    return result;
                }
            }
        } else if (auto const* reference_decl = qualified_type->getAs<clang::ReferenceType>()) {
            if (auto const* pointee = reference_decl->getPointeeCXXRecordDecl()) {
                if (record_inherits_from_cell(*pointee)) {
                    result.is_valid = false;
                    result.is_wrapped_in_gcptr = false;
                    return result;
                }
            }
        } else if (auto const* specialization = qualified_type->getAs<clang::TemplateSpecializationType>()) {
            auto template_type_name = specialization->getTemplateName().getAsTemplateDecl()->getName();
            if (template_type_name != "GCPtr" && template_type_name != "NonnullGCPtr")
                return result;

            if (specialization->getNumArgs() != 1)
                return result; // Not really valid, but will produce a compilation error anyway

            auto const& type_arg = specialization->getArg(0);
            auto const* record_type = type_arg.getAsType()->getAs<clang::RecordType>();
            if (!record_type)
                return result;

            auto const* record_decl = record_type->getAsCXXRecordDecl();
            if (!record_decl->hasDefinition())
                return result;

            result.is_wrapped_in_gcptr = true;
            result.is_valid = record_inherits_from_cell(*record_decl);
        }
    }

    return result;
}

void CollectCellsHandler::run(clang::ast_matchers::MatchFinder::MatchResult const& result)
{
    clang::CXXRecordDecl const* record = result.Nodes.getNodeAs<clang::CXXRecordDecl>("record-decl");
    if (!record || !record->isCompleteDefinition() || (!record->isClass() && !record->isStruct()))
        return;

    auto& diag_engine = result.Context->getDiagnostics();

    for (clang::FieldDecl const* field : record->fields()) {
        auto const& type = field->getType();

        auto validation_results = validate_field(field);
        if (!validation_results.is_valid) {
            if (validation_results.is_wrapped_in_gcptr) {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Warning, "Specialization type must inherit from JS::Cell");
                diag_engine.Report(field->getLocation(), diag_id);
            } else {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Warning, "%0 to JS::Cell type should be wrapped in %1");
                auto builder = diag_engine.Report(field->getLocation(), diag_id);
                if (type->isReferenceType()) {
                    builder << "reference"
                            << "JS::NonnullGCPtr";
                } else {
                    builder << "pointer"
                            << "JS::GCPtr";
                }
            }
        }
    }
}
