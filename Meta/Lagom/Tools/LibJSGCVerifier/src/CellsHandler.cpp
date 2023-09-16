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

template<typename T>
class SimpleCollectMatchesCallback : public clang::ast_matchers::MatchFinder::MatchCallback {
public:
    explicit SimpleCollectMatchesCallback(std::string name)
        : m_name(std::move(name))
    {
    }

    void run(clang::ast_matchers::MatchFinder::MatchResult const& result) override
    {
        if (auto const* node = result.Nodes.getNodeAs<T>(m_name))
            m_matches.push_back(node);
    }

    auto const& matches() const { return m_matches; }

private:
    std::string m_name;
    std::vector<T const*> m_matches;
};

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
            for (size_t i = 0; i < template_specialization->template_arguments().size(); i++) {
                auto const& template_arg = template_specialization->template_arguments()[i];
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
    bool needs_visiting { false };
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
                    result.needs_visiting = true;
                    return result;
                }
            }
        } else if (auto const* reference_decl = qualified_type->getAs<clang::ReferenceType>()) {
            if (auto const* pointee = reference_decl->getPointeeCXXRecordDecl()) {
                if (record_inherits_from_cell(*pointee)) {
                    result.is_valid = false;
                    result.is_wrapped_in_gcptr = false;
                    result.needs_visiting = true;
                    return result;
                }
            }
        } else if (auto const* specialization = qualified_type->getAs<clang::TemplateSpecializationType>()) {
            auto template_type_name = specialization->getTemplateName().getAsTemplateDecl()->getName();
            if (template_type_name != "GCPtr" && template_type_name != "NonnullGCPtr")
                return result;

            if (specialization->template_arguments().size() != 1)
                return result; // Not really valid, but will produce a compilation error anyway

            auto const& type_arg = specialization->template_arguments()[0];
            auto const* record_type = type_arg.getAsType()->getAs<clang::RecordType>();
            if (!record_type)
                return result;

            auto const* record_decl = record_type->getAsCXXRecordDecl();
            if (!record_decl->hasDefinition())
                return result;

            result.is_wrapped_in_gcptr = true;
            result.is_valid = record_inherits_from_cell(*record_decl);
            result.needs_visiting = true;
        }
    }

    return result;
}

void CollectCellsHandler::run(clang::ast_matchers::MatchFinder::MatchResult const& result)
{
    using namespace clang::ast_matchers;

    clang::CXXRecordDecl const* record = result.Nodes.getNodeAs<clang::CXXRecordDecl>("record-decl");
    if (!record || !record->isCompleteDefinition() || (!record->isClass() && !record->isStruct()))
        return;

    // Cell triggers a bunch of warnings for its empty visit_edges implementation, but
    // it doesn't have any members anyways so it's fine to just ignore.
    if (record->getQualifiedNameAsString() == "JS::Cell")
        return;

    auto& diag_engine = result.Context->getDiagnostics();
    std::vector<clang::FieldDecl const*> fields_that_need_visiting;

    for (clang::FieldDecl const* field : record->fields()) {
        auto validation_results = validate_field(field);
        if (!validation_results.is_valid) {
            if (validation_results.is_wrapped_in_gcptr) {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Warning, "Specialization type must inherit from JS::Cell");
                diag_engine.Report(field->getLocation(), diag_id);
            } else {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Warning, "%0 to JS::Cell type should be wrapped in %1");
                auto builder = diag_engine.Report(field->getLocation(), diag_id);
                if (field->getType()->isReferenceType()) {
                    builder << "reference"
                            << "JS::NonnullGCPtr";
                } else {
                    builder << "pointer"
                            << "JS::GCPtr";
                }
            }
        } else if (validation_results.needs_visiting) {
            fields_that_need_visiting.push_back(field);
        }
    }

    if (!record_inherits_from_cell(*record))
        return;

    clang::DeclarationName const name = &result.Context->Idents.get("visit_edges");
    auto const* visit_edges_method = record->lookup(name).find_first<clang::CXXMethodDecl>();
    if (!visit_edges_method || !visit_edges_method->getBody())
        return;

    // Search for a call to Base::visit_edges. Note that this also has the nice side effect of
    // ensuring the classes use JS_CELL/JS_OBJECT, as Base will not be defined if they do not.

    MatchFinder base_visit_edges_finder;
    SimpleCollectMatchesCallback<clang::CXXMemberCallExpr> base_visit_edges_callback("member-call");

    auto base_visit_edges_matcher = cxxMemberCallExpr(
        callee(memberExpr(member(hasName("visit_edges")))))
                                        .bind("member-call");

    base_visit_edges_finder.addMatcher(base_visit_edges_matcher, &base_visit_edges_callback);
    base_visit_edges_finder.matchAST(*result.Context);

    bool call_to_base_visit_edges_found = false;

    for (auto const* call_expr : base_visit_edges_callback.matches()) {
        // FIXME: Can we constrain the matcher above to avoid looking directly at the source code?
        auto const* source_chars = result.SourceManager->getCharacterData(call_expr->getBeginLoc());
        if (strncmp(source_chars, "Base::", 6) == 0) {
            call_to_base_visit_edges_found = true;
            break;
        }
    }

    if (!call_to_base_visit_edges_found) {
        auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Warning, "Missing call to Base::visit_edges");
        diag_engine.Report(visit_edges_method->getBeginLoc(), diag_id);
    }

    // Search for uses of all fields that need visiting. We don't ensure they are _actually_ visited
    // with a call to visitor.visit(...), as that is too complex. Instead, we just assume that if the
    // field is accessed at all, then it is visited.

    if (fields_that_need_visiting.empty())
        return;

    MatchFinder field_access_finder;
    SimpleCollectMatchesCallback<clang::MemberExpr> field_access_callback("member-expr");

    auto field_access_matcher = memberExpr(
        hasAncestor(cxxMethodDecl(hasName("visit_edges"))),
        hasObjectExpression(hasType(pointsTo(cxxRecordDecl(hasName(record->getName()))))))
                                    .bind("member-expr");

    field_access_finder.addMatcher(field_access_matcher, &field_access_callback);
    field_access_finder.matchAST(visit_edges_method->getASTContext());

    std::unordered_set<std::string> fields_that_are_visited;
    for (auto const* member_expr : field_access_callback.matches())
        fields_that_are_visited.insert(member_expr->getMemberNameInfo().getAsString());

    auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Warning, "GC-allocated member is not visited in %0::visit_edges");

    for (auto const* field : fields_that_need_visiting) {
        if (!fields_that_are_visited.contains(field->getNameAsString())) {
            auto builder = diag_engine.Report(field->getBeginLoc(), diag_id);
            builder << record->getName();
        }
    }
}
