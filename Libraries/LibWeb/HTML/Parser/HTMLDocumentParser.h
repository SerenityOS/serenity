/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/HTML/Parser/HTMLTokenizer.h>
#include <LibWeb/HTML/Parser/ListOfActiveFormattingElements.h>
#include <LibWeb/HTML/Parser/StackOfOpenElements.h>

namespace Web::HTML {

#define ENUMERATE_INSERTION_MODES               \
    __ENUMERATE_INSERTION_MODE(Initial)         \
    __ENUMERATE_INSERTION_MODE(BeforeHTML)      \
    __ENUMERATE_INSERTION_MODE(BeforeHead)      \
    __ENUMERATE_INSERTION_MODE(InHead)          \
    __ENUMERATE_INSERTION_MODE(InHeadNoscript)  \
    __ENUMERATE_INSERTION_MODE(AfterHead)       \
    __ENUMERATE_INSERTION_MODE(InBody)          \
    __ENUMERATE_INSERTION_MODE(Text)            \
    __ENUMERATE_INSERTION_MODE(InTable)         \
    __ENUMERATE_INSERTION_MODE(InTableText)     \
    __ENUMERATE_INSERTION_MODE(InCaption)       \
    __ENUMERATE_INSERTION_MODE(InColumnGroup)   \
    __ENUMERATE_INSERTION_MODE(InTableBody)     \
    __ENUMERATE_INSERTION_MODE(InRow)           \
    __ENUMERATE_INSERTION_MODE(InCell)          \
    __ENUMERATE_INSERTION_MODE(InSelect)        \
    __ENUMERATE_INSERTION_MODE(InSelectInTable) \
    __ENUMERATE_INSERTION_MODE(InTemplate)      \
    __ENUMERATE_INSERTION_MODE(AfterBody)       \
    __ENUMERATE_INSERTION_MODE(InFrameset)      \
    __ENUMERATE_INSERTION_MODE(AfterFrameset)   \
    __ENUMERATE_INSERTION_MODE(AfterAfterBody)  \
    __ENUMERATE_INSERTION_MODE(AfterAfterFrameset)

RefPtr<DOM::Document> parse_html_document(const StringView&, const URL&, const String& encoding);

class HTMLDocumentParser {
public:
    HTMLDocumentParser(const StringView& input, const String& encoding);
    HTMLDocumentParser(const StringView& input, const String& encoding, DOM::Document& existing_document);
    ~HTMLDocumentParser();

    void run(const URL&);

    DOM::Document& document();

    static NonnullRefPtrVector<DOM::Node> parse_html_fragment(DOM::Element& context_element, const StringView&);

    enum class InsertionMode {
#define __ENUMERATE_INSERTION_MODE(mode) mode,
        ENUMERATE_INSERTION_MODES
#undef __ENUMERATE_INSERTION_MODE
    };

    InsertionMode insertion_mode() const { return m_insertion_mode; }

    static bool is_special_tag(const FlyString& tag_name);

private:
    const char* insertion_mode_name() const;

    DOM::QuirksMode which_quirks_mode(const HTMLToken&) const;

    void handle_initial(HTMLToken&);
    void handle_before_html(HTMLToken&);
    void handle_before_head(HTMLToken&);
    void handle_in_head(HTMLToken&);
    void handle_in_head_noscript(HTMLToken&);
    void handle_after_head(HTMLToken&);
    void handle_in_body(HTMLToken&);
    void handle_after_body(HTMLToken&);
    void handle_after_after_body(HTMLToken&);
    void handle_text(HTMLToken&);
    void handle_in_table(HTMLToken&);
    void handle_in_table_body(HTMLToken&);
    void handle_in_row(HTMLToken&);
    void handle_in_cell(HTMLToken&);
    void handle_in_table_text(HTMLToken&);
    void handle_in_select_in_table(HTMLToken&);
    void handle_in_select(HTMLToken&);
    void handle_in_caption(HTMLToken&);
    void handle_in_column_group(HTMLToken&);
    void handle_in_template(HTMLToken&);
    void handle_in_frameset(HTMLToken&);
    void handle_after_frameset(HTMLToken&);
    void handle_after_after_frameset(HTMLToken&);

    void stop_parsing() { m_stop_parsing = true; }

    void generate_implied_end_tags(const FlyString& exception = {});
    void generate_all_implied_end_tags_thoroughly();
    bool stack_of_open_elements_has_element_with_tag_name_in_scope(const FlyString& tag_name);
    NonnullRefPtr<DOM::Element> create_element_for(const HTMLToken&);

    struct AdjustedInsertionLocation {
        RefPtr<DOM::Node> parent;
        RefPtr<DOM::Node> insert_before_sibling;
    };

    AdjustedInsertionLocation find_appropriate_place_for_inserting_node();

    DOM::Text* find_character_insertion_node();
    void flush_character_insertions();
    RefPtr<DOM::Element> insert_html_element(const HTMLToken&);
    DOM::Element& current_node();
    DOM::Element& node_before_current_node();
    void insert_character(u32 data);
    void insert_comment(HTMLToken&);
    void reconstruct_the_active_formatting_elements();
    void close_a_p_element();
    void process_using_the_rules_for(InsertionMode, HTMLToken&);
    void parse_generic_raw_text_element(HTMLToken&);
    void increment_script_nesting_level();
    void decrement_script_nesting_level();
    size_t script_nesting_level() const { return m_script_nesting_level; }
    void reset_the_insertion_mode_appropriately();

    void adjust_mathml_attributes(HTMLToken&);
    void adjust_svg_attributes(HTMLToken&);
    void adjust_foreign_attributes(HTMLToken&);

    enum AdoptionAgencyAlgorithmOutcome {
        DoNothing,
        RunAnyOtherEndTagSteps,
    };

    AdoptionAgencyAlgorithmOutcome run_the_adoption_agency_algorithm(HTMLToken&);
    void clear_the_stack_back_to_a_table_context();
    void clear_the_stack_back_to_a_table_body_context();
    void clear_the_stack_back_to_a_table_row_context();
    void close_the_cell();

    InsertionMode m_insertion_mode { InsertionMode::Initial };
    InsertionMode m_original_insertion_mode { InsertionMode::Initial };

    StackOfOpenElements m_stack_of_open_elements;
    Vector<InsertionMode> m_stack_of_template_insertion_modes;
    ListOfActiveFormattingElements m_list_of_active_formatting_elements;

    HTMLTokenizer m_tokenizer;

    bool m_foster_parenting { false };
    bool m_frameset_ok { true };
    bool m_parsing_fragment { false };
    bool m_scripting_enabled { true };
    bool m_invoked_via_document_write { false };
    bool m_aborted { false };
    bool m_parser_pause_flag { false };
    bool m_stop_parsing { false };
    size_t m_script_nesting_level { 0 };

    RefPtr<DOM::Document> m_document;
    RefPtr<HTMLHeadElement> m_head_element;
    RefPtr<HTMLFormElement> m_form_element;
    RefPtr<DOM::Element> m_context_element;

    Vector<HTMLToken> m_pending_table_character_tokens;

    RefPtr<DOM::Text> m_character_insertion_node;
    StringBuilder m_character_insertion_builder;
};

}
