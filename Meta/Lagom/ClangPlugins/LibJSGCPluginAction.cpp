/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LibJSGCPluginAction.h"
#include <clang/ASTMatchers/ASTMatchFinder.h>
#include <clang/ASTMatchers/ASTMatchers.h>
#include <clang/Basic/SourceManager.h>
#include <clang/Frontend/CompilerInstance.h>
#include <clang/Frontend/FrontendPluginRegistry.h>
#include <clang/Lex/MacroArgs.h>
#include <unordered_set>

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
        }
    }

    return result;
}

bool LibJSGCVisitor::VisitCXXRecordDecl(clang::CXXRecordDecl* record)
{
    using namespace clang::ast_matchers;

    if (!record || !record->isCompleteDefinition() || (!record->isClass() && !record->isStruct()))
        return true;

    // Cell triggers a bunch of warnings for its empty visit_edges implementation, but
    // it doesn't have any members anyways so it's fine to just ignore.
    auto qualified_name = record->getQualifiedNameAsString();
    if (qualified_name == "JS::Cell")
        return true;

    auto& diag_engine = m_context.getDiagnostics();
    std::vector<clang::FieldDecl const*> fields_that_need_visiting;

    for (clang::FieldDecl const* field : record->fields()) {
        auto validation_results = validate_field(field);
        if (!validation_results.is_valid) {
            if (validation_results.is_wrapped_in_gcptr) {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "Specialization type must inherit from JS::Cell");
                diag_engine.Report(field->getLocation(), diag_id);
            } else {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "%0 to JS::Cell type should be wrapped in %1");
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
        return true;

    validate_record_macros(*record);

    clang::DeclarationName name = &m_context.Idents.get("visit_edges");
    auto const* visit_edges_method = record->lookup(name).find_first<clang::CXXMethodDecl>();
    if (!visit_edges_method && !fields_that_need_visiting.empty()) {
        auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "JS::Cell-inheriting class %0 contains a GC-allocated member %1 but has no visit_edges method");
        auto builder = diag_engine.Report(record->getLocation(), diag_id);
        builder << record->getName()
                << fields_that_need_visiting[0];
    }
    if (!visit_edges_method || !visit_edges_method->getBody())
        return true;

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
    base_visit_edges_finder.matchAST(m_context);

    bool call_to_base_visit_edges_found = false;

    for (auto const* call_expr : base_visit_edges_callback.matches()) {
        // FIXME: Can we constrain the matcher above to avoid looking directly at the source code?
        auto const* source_chars = m_context.getSourceManager().getCharacterData(call_expr->getBeginLoc());
        if (strncmp(source_chars, "Base::", 6) == 0) {
            call_to_base_visit_edges_found = true;
            break;
        }
    }

    if (!call_to_base_visit_edges_found) {
        auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "Missing call to Base::visit_edges");
        diag_engine.Report(visit_edges_method->getBeginLoc(), diag_id);
    }

    // Search for uses of all fields that need visiting. We don't ensure they are _actually_ visited
    // with a call to visitor.visit(...), as that is too complex. Instead, we just assume that if the
    // field is accessed at all, then it is visited.

    if (fields_that_need_visiting.empty())
        return true;

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

    auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "GC-allocated member is not visited in %0::visit_edges");

    for (auto const* field : fields_that_need_visiting) {
        if (!fields_that_are_visited.contains(field->getNameAsString())) {
            auto builder = diag_engine.Report(field->getBeginLoc(), diag_id);
            builder << record->getName();
        }
    }

    return true;
}

struct CellTypeWithOrigin {
    clang::CXXRecordDecl const& base_origin;
    LibJSCellMacro::Type type;
};

std::optional<CellTypeWithOrigin> find_cell_type_with_origin(clang::CXXRecordDecl const& record)
{
    for (auto const& base : record.bases()) {
        if (auto const* base_record = base.getType()->getAsCXXRecordDecl()) {
            auto base_name = base_record->getQualifiedNameAsString();

            if (base_name == "JS::Cell")
                return CellTypeWithOrigin { *base_record, LibJSCellMacro::Type::JSCell };

            if (base_name == "JS::Object")
                return CellTypeWithOrigin { *base_record, LibJSCellMacro::Type::JSObject };

            if (base_name == "JS::Environment")
                return CellTypeWithOrigin { *base_record, LibJSCellMacro::Type::JSEnvironment };

            if (base_name == "JS::PrototypeObject")
                return CellTypeWithOrigin { *base_record, LibJSCellMacro::Type::JSPrototypeObject };

            if (base_name == "Web::Bindings::PlatformObject")
                return CellTypeWithOrigin { *base_record, LibJSCellMacro::Type::WebPlatformObject };

            if (auto origin = find_cell_type_with_origin(*base_record))
                return CellTypeWithOrigin { *base_record, origin->type };
        }
    }

    return {};
}

LibJSGCVisitor::CellMacroExpectation LibJSGCVisitor::get_record_cell_macro_expectation(clang::CXXRecordDecl const& record)
{
    auto origin = find_cell_type_with_origin(record);
    assert(origin.has_value());

    // Need to iterate the bases again to turn the record into the exact text that the user used as
    // the class base, since it doesn't have to be qualified (but might be).
    for (auto const& base : record.bases()) {
        if (auto const* base_record = base.getType()->getAsCXXRecordDecl()) {
            if (base_record == &origin->base_origin) {
                auto& source_manager = m_context.getSourceManager();
                auto char_range = source_manager.getExpansionRange({ base.getBaseTypeLoc(), base.getEndLoc() });
                auto exact_text = clang::Lexer::getSourceText(char_range, source_manager, m_context.getLangOpts());
                return { origin->type, exact_text.str() };
            }
        }
    }

    assert(false);
}

void LibJSGCVisitor::validate_record_macros(clang::CXXRecordDecl const& record)
{
    auto& source_manager = m_context.getSourceManager();
    auto record_range = record.getSourceRange();

    // FIXME: The current macro detection doesn't recursively search through macro expansion,
    //        so if the record itself is defined in a macro, the JS_CELL/etc won't be found
    if (source_manager.isMacroBodyExpansion(record_range.getBegin()))
        return;

    auto [expected_cell_macro_type, expected_base_name] = get_record_cell_macro_expectation(record);
    auto file_id = m_context.getSourceManager().getFileID(record.getLocation());
    auto it = m_macro_map.find(file_id.getHashValue());
    auto& diag_engine = m_context.getDiagnostics();

    auto report_missing_macro = [&] {
        auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "Expected record to have a %0 macro invocation");
        auto builder = diag_engine.Report(record.getLocation(), diag_id);
        builder << LibJSCellMacro::type_name(expected_cell_macro_type);
    };

    if (it == m_macro_map.end()) {
        report_missing_macro();
        return;
    }

    std::vector<clang::SourceRange> sub_ranges;
    for (auto const& sub_decl : record.decls()) {
        if (auto const* sub_record = llvm::dyn_cast<clang::CXXRecordDecl>(sub_decl))
            sub_ranges.push_back(sub_record->getSourceRange());
    }

    bool found_macro = false;

    auto record_name = record.getDeclName().getAsString();
    if (record.getQualifier()) {
        // FIXME: There has to be a better way to get this info. getQualifiedNameAsString() gets too much info
        //        (outer namespaces that aren't part of the class identifier), and getNameAsString() doesn't get
        //        enough info (doesn't include parts before the namespace specifier).
        auto loc = record.getQualifierLoc();
        auto& sm = m_context.getSourceManager();
        auto begin_offset = sm.getFileOffset(loc.getBeginLoc());
        auto end_offset = sm.getFileOffset(loc.getEndLoc());
        auto const* file_buf = sm.getCharacterData(loc.getBeginLoc());
        auto prefix = std::string { file_buf, end_offset - begin_offset };
        record_name = prefix + "::" + record_name;
    }

    for (auto const& macro : it->second) {
        if (record_range.fullyContains(macro.range)) {
            bool macro_is_in_sub_decl = false;
            for (auto const& sub_range : sub_ranges) {
                if (sub_range.fullyContains(macro.range)) {
                    macro_is_in_sub_decl = true;
                    break;
                }
            }
            if (macro_is_in_sub_decl)
                continue;

            if (found_macro) {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "Record has multiple JS_CELL-like macro invocations");
                diag_engine.Report(record_range.getBegin(), diag_id);
            }

            found_macro = true;
            if (macro.type != expected_cell_macro_type) {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "Invalid JS-CELL-like macro invocation; expected %0");
                auto builder = diag_engine.Report(macro.range.getBegin(), diag_id);
                builder << LibJSCellMacro::type_name(expected_cell_macro_type);
            }

            // This is a compile error, no diagnostic needed
            if (macro.args.size() < 2)
                return;

            if (macro.args[0].text != record_name) {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "Expected first argument of %0 macro invocation to be %1");
                auto builder = diag_engine.Report(macro.args[0].location, diag_id);
                builder << LibJSCellMacro::type_name(expected_cell_macro_type) << record_name;
            }

            if (expected_cell_macro_type == LibJSCellMacro::Type::JSPrototypeObject) {
                // FIXME: Validate the args for this macro
            } else if (macro.args[1].text != expected_base_name) {
                auto diag_id = diag_engine.getCustomDiagID(clang::DiagnosticsEngine::Error, "Expected second argument of %0 macro invocation to be %1");
                auto builder = diag_engine.Report(macro.args[1].location, diag_id);
                builder << LibJSCellMacro::type_name(expected_cell_macro_type) << expected_base_name;
            }
        }
    }

    if (!found_macro)
        report_missing_macro();
}

LibJSGCASTConsumer::LibJSGCASTConsumer(clang::CompilerInstance& compiler)
    : m_compiler(compiler)
{
    auto& preprocessor = compiler.getPreprocessor();
    preprocessor.addPPCallbacks(std::make_unique<LibJSPPCallbacks>(preprocessor, m_macro_map));
}

void LibJSGCASTConsumer::HandleTranslationUnit(clang::ASTContext& context)
{
    LibJSGCVisitor visitor { context, m_macro_map };
    visitor.TraverseDecl(context.getTranslationUnitDecl());
}

char const* LibJSCellMacro::type_name(Type type)
{
    switch (type) {
    case Type::JSCell:
        return "JS_CELL";
    case Type::JSObject:
        return "JS_OBJECT";
    case Type::JSEnvironment:
        return "JS_ENVIRONMENT";
    case Type::JSPrototypeObject:
        return "JS_PROTOTYPE_OBJECT";
    case Type::WebPlatformObject:
        return "WEB_PLATFORM_OBJECT";
    default:
        __builtin_unreachable();
    }
}

void LibJSPPCallbacks::LexedFileChanged(clang::FileID curr_fid, LexedFileChangeReason reason, clang::SrcMgr::CharacteristicKind, clang::FileID, clang::SourceLocation)
{
    if (reason == LexedFileChangeReason::EnterFile) {
        m_curr_fid_hash_stack.push_back(curr_fid.getHashValue());
    } else {
        assert(!m_curr_fid_hash_stack.empty());
        m_curr_fid_hash_stack.pop_back();
    }
}

void LibJSPPCallbacks::MacroExpands(clang::Token const& name_token, clang::MacroDefinition const&, clang::SourceRange range, clang::MacroArgs const* args)
{
    if (auto* ident_info = name_token.getIdentifierInfo()) {
        static llvm::StringMap<LibJSCellMacro::Type> libjs_macro_types {
            { "JS_CELL", LibJSCellMacro::Type::JSCell },
            { "JS_OBJECT", LibJSCellMacro::Type::JSObject },
            { "JS_ENVIRONMENT", LibJSCellMacro::Type::JSEnvironment },
            { "JS_PROTOTYPE_OBJECT", LibJSCellMacro::Type::JSPrototypeObject },
            { "WEB_PLATFORM_OBJECT", LibJSCellMacro::Type::WebPlatformObject },
        };

        auto name = ident_info->getName();
        if (auto it = libjs_macro_types.find(name); it != libjs_macro_types.end()) {
            LibJSCellMacro macro { range, it->second, {} };

            for (size_t arg_index = 0; arg_index < args->getNumMacroArguments(); arg_index++) {
                auto const* first_token = args->getUnexpArgument(arg_index);
                auto stringified_token = clang::MacroArgs::StringifyArgument(first_token, m_preprocessor, false, range.getBegin(), range.getEnd());
                // The token includes leading and trailing quotes
                auto len = strlen(stringified_token.getLiteralData());
                std::string arg_text { stringified_token.getLiteralData() + 1, len - 2 };
                macro.args.push_back({ arg_text, first_token->getLocation() });
            }

            assert(!m_curr_fid_hash_stack.empty());
            auto curr_fid_hash = m_curr_fid_hash_stack.back();
            if (m_macro_map.find(curr_fid_hash) == m_macro_map.end())
                m_macro_map[curr_fid_hash] = {};
            m_macro_map[curr_fid_hash].push_back(macro);
        }
    }
}

static clang::FrontendPluginRegistry::Add<LibJSGCPluginAction> X("libjs_gc_scanner", "analyze LibJS GC usage");
