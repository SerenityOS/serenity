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
#include <llvm/Support/JSON.h>
#include <llvm/Support/raw_ostream.h>
#include <unordered_set>
#include <vector>

AST_MATCHER_P(clang::Decl, hasAnnotation, std::string, name)
{
    (void)Builder;
    (void)Finder;
    for (auto const* attr : Node.attrs()) {
        if (auto const* annotate_attr = llvm::dyn_cast<clang::AnnotateAttr>(attr)) {
            if (annotate_attr->getAnnotation() == name)
                return true;
        }
    }
    return false;
}

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

    auto non_capturable_var_decl = varDecl(
        hasLocalStorage(),
        unless(
            anyOf(
                // The declaration has an annotation:
                //     IGNORE_USE_IN_ESCAPING_LAMBDA Foo foo;
                hasAnnotation("serenity::ignore_use_in_escaping_lambda"),
                // The declaration is a reference:
                //     Foo& foo_ref = get_foo_ref();
                //     Foo* foo_ptr = get_foo_ptr();
                //     do_something([&foo_ref, &foo_ptr] {
                //         foo_ref.foo();  // Fine, foo_ref references the underlying Foo instance
                //         foo_ptr->foo(); // Bad, foo_ptr references the pointer on the stack above
                //     });
                hasType(references(TypeMatcher(anything()))))));

    auto bad_lambda_capture = lambdaCapture(anyOf(capturesThis(), capturesVar(non_capturable_var_decl))).bind("lambda-capture");

    auto lambda_with_bad_capture = lambdaExpr(
        anyOf(
            // These are both required as they have slightly different behavior.
            //
            // We need forEachLambdaCapture because we need to go over every explicit capture in the capture list, as
            // hasAnyCapture will just take the first capture in the list that matches the criteria (usually the `this`
            // capture). Without it, if the first capture in the list was flagged as bad but is actually fine (e.g. the
            // `this` capture, or a var capture by value), but there was a second capture in the list that was invalid,
            // it would be skipped.
            //
            // But forEachLambdaCapture doesn't seem to find implicit captures, so we also need hasAnyCapture to handle
            // captures that aren't explicitly listed in the capture list, but are still invalid.
            forEachLambdaCapture(bad_lambda_capture),
            hasAnyCapture(bad_lambda_capture)));

    // Bind this varDecl so we can reference it later to make sure it isn't being called
    auto lambda_with_bad_capture_decl = varDecl(hasInitializer(lambda_with_bad_capture)).bind("lambda");

    m_finder.addMatcher(
        traverse(
            clang::TK_IgnoreUnlessSpelledInSource,
            callExpr(
                forEachArgumentWithParam(
                    anyOf(
                        // Match a lambda given directly in the function call
                        lambda_with_bad_capture,
                        // Matches an expression with a possibly-deeply-nested reference to a variable with a lambda type, e.g:
                        //     auto lambda = [...] { ... };
                        //     some_func(move(lambda));
                        has(declRefExpr(
                            to(lambda_with_bad_capture_decl),
                            // Avoid immediately invoked lambdas (i.e. match `move(lambda)` but not `move(lambda())`)
                            unless(hasParent(
                                // <lambda struct>::operator()(...)
                                cxxOperatorCallExpr(has(declRefExpr(to(equalsBoundNode("lambda")))))))))),
                    parmVarDecl(
                        allOf(
                            // It's important that the parameter has a RecordType, as a templated type can never escape its function
                            hasType(cxxRecordDecl()),
                            unless(hasAnnotation("serenity::noescape"))))
                        .bind("lambda-param-ref")))),
        this);
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
        // Do not unwrap GCPtr/NonnullGCPtr/MarkedVector
        if (specialization_name == "JS::GCPtr" || specialization_name == "JS::NonnullGCPtr" || specialization_name == "JS::RawGCPtr" || specialization_name == "JS::MarkedVector") {
            qualified_types.push_back(type);
        } else {
            auto const template_arguments = template_specialization->template_arguments();
            for (size_t i = 0; i < template_arguments.size(); i++) {
                auto const& template_arg = template_arguments[i];
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
            if (template_type_name != "GCPtr" && template_type_name != "NonnullGCPtr" && template_type_name != "RawGCPtr")
                return result;

            auto const template_args = specialization->template_arguments();
            if (template_args.size() != 1)
                return result; // Not really valid, but will produce a compilation error anyway

            auto const& type_arg = template_args[0];
            auto const* record_type = type_arg.getAsType()->getAs<clang::RecordType>();
            if (!record_type)
                return result;

            auto const* record_decl = record_type->getAsCXXRecordDecl();
            if (!record_decl->hasDefinition())
                return result;

            result.is_wrapped_in_gcptr = true;
            result.is_valid = record_inherits_from_cell(*record_decl);
            result.needs_visiting = template_type_name != "RawGCPtr";
        } else if (auto const* record = qualified_type->getAsCXXRecordDecl()) {
            if (record->getQualifiedNameAsString() == "JS::Value") {
                result.needs_visiting = true;
            }
        }
    }

    return result;
}

void emit_record_json_data(clang::CXXRecordDecl const& record)
{
    llvm::json::Object obj;
    obj.insert({ "name", record.getQualifiedNameAsString() });

    std::vector<std::string> bases;
    record.forallBases([&](clang::CXXRecordDecl const* base) {
        bases.push_back(base->getQualifiedNameAsString());
        return true;
    });
    obj.insert({ "parents", bases });

    bool has_cell_allocator = false;
    bool has_js_constructor = false;
    for (auto const& decl : record.decls()) {
        if (auto* var_decl = llvm::dyn_cast<clang::VarDecl>(decl); var_decl && var_decl->getQualifiedNameAsString().ends_with("::cell_allocator")) {
            has_cell_allocator = true;
        } else if (auto* fn_decl = llvm::dyn_cast<clang::CXXMethodDecl>(decl); fn_decl && fn_decl->getQualifiedNameAsString().ends_with("::construct_impl")) {
            has_js_constructor = true;
        }
    }
    obj.insert({ "has_cell_allocator", has_cell_allocator });
    obj.insert({ "has_js_constructor", has_js_constructor });

    llvm::outs() << std::move(obj) << "\n";
}

void CollectCellsHandler::run(clang::ast_matchers::MatchFinder::MatchResult const& result)
{
    check_cells(result);
    check_lambda_captures(result);
}

void CollectCellsHandler::check_cells(clang::ast_matchers::MatchFinder::MatchResult const& result)
{
    using namespace clang::ast_matchers;

    clang::CXXRecordDecl const* record = result.Nodes.getNodeAs<clang::CXXRecordDecl>("record-decl");
    if (!record || !record->isCompleteDefinition() || (!record->isClass() && !record->isStruct()))
        return;

    // Cell triggers a bunch of warnings for its empty visit_edges implementation, but
    // it doesn't have any members anyways so it's fine to just ignore.
    auto qualified_name = record->getQualifiedNameAsString();
    if (qualified_name == "JS::Cell")
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

    emit_record_json_data(*record);

    bool has_base = false;
    for (auto const& decl : record->decls()) {
        if (auto* alias_decl = llvm::dyn_cast<clang::TypeAliasDecl>(decl); alias_decl && alias_decl->getQualifiedNameAsString().ends_with("::Base")) {
            has_base = true;
            break;
        }
    }

    if (!has_base) {
        auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Warning, "JS::Cell-inheriting class %0 is missing a JS_CELL() call in its header file");
        auto builder = diag_engine.Report(record->getLocation(), diag_id);
        builder << record->getName();
    }

    clang::DeclarationName const name = &result.Context->Idents.get("visit_edges");
    auto const* visit_edges_method = record->lookup(name).find_first<clang::CXXMethodDecl>();
    if (!visit_edges_method && !fields_that_need_visiting.empty()) {
        auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Warning, "JS::Cell-inheriting class %0 contains a GC-allocated member %1 but has no visit_edges method");
        auto builder = diag_engine.Report(record->getLocation(), diag_id);
        builder << record->getName()
                << fields_that_need_visiting[0];
    }
    if (!visit_edges_method || !visit_edges_method->getBody())
        return;

    // Search for a call to Base::visit_edges. Note that this also has the nice side effect of
    // ensuring the classes use JS_CELL/JS_OBJECT, as Base will not be defined if they do not.

    MatchFinder base_visit_edges_finder;
    SimpleCollectMatchesCallback<clang::MemberExpr> base_visit_edges_callback("member-call");

    auto base_visit_edges_matcher = cxxMethodDecl(
        ofClass(hasName(qualified_name)),
        functionDecl(hasName("visit_edges")),
        isOverride(),
        hasDescendant(memberExpr(member(hasName("visit_edges"))).bind("member-call")));

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

void CollectCellsHandler::check_lambda_captures(clang::ast_matchers::MatchFinder::MatchResult const& result)
{
    auto& diag_engine = result.Context->getDiagnostics();

    if (auto const* capture = result.Nodes.getNodeAs<clang::LambdaCapture>("lambda-capture")) {
        if (capture->capturesThis() || capture->getCaptureKind() != clang::LCK_ByRef)
            return;

        auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Warning, "Variable with local storage is captured by reference in a lambda that may be asynchronously executed");
        diag_engine.Report(capture->getLocation(), diag_id);

        clang::SourceLocation captured_var_location;
        if (auto const* var_decl = llvm::dyn_cast<clang::VarDecl>(capture->getCapturedVar())) {
            captured_var_location = var_decl->getTypeSourceInfo()->getTypeLoc().getBeginLoc();
        } else {
            captured_var_location = capture->getCapturedVar()->getLocation();
        }
        diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Note, "Annotate the variable declaration with IGNORE_USE_IN_ESCAPING_LAMBDA if it outlives the lambda");
        diag_engine.Report(captured_var_location, diag_id);

        auto const* param = result.Nodes.getNodeAs<clang::ParmVarDecl>("lambda-param-ref");
        diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Note, "Annotate the parameter with NOESCAPE if the lambda will not outlive the function call");
        diag_engine.Report(param->getTypeSourceInfo()->getTypeLoc().getBeginLoc(), diag_id);
    }
}
