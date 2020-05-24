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
#include <LibWeb/Parser/HTMLTokenizer.h>
#include <LibWeb/Parser/StackOfOpenElements.h>

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

namespace Web {

class HTMLDocumentParser {
public:
    explicit HTMLDocumentParser(const StringView& input);
    ~HTMLDocumentParser();

    void run();

    Document& document();

    enum class InsertionMode {
#define __ENUMERATE_INSERTION_MODE(mode) mode,
        ENUMERATE_INSERTION_MODES
#undef __ENUMERATE_INSERTION_MODE
    };

    InsertionMode insertion_mode() const { return m_insertion_mode; }

private:
    const char* insertion_mode_name() const;

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

    void generate_implied_end_tags();
    bool stack_of_open_elements_has_element_with_tag_name_in_scope(const FlyString& tag_name);
    NonnullRefPtr<Element> create_element_for(HTMLToken&);
    RefPtr<Node> find_appropriate_place_for_inserting_node();
    RefPtr<Element> insert_html_element(HTMLToken&);
    Element& current_node();

    InsertionMode m_insertion_mode { InsertionMode::Initial };
    StackOfOpenElements m_stack_of_open_elements;

    HTMLTokenizer m_tokenizer;

    bool m_foster_parenting { false };
    bool m_frameset_ok { true };
    bool m_parsing_fragment { false };

    RefPtr<Document> m_document;
    RefPtr<HTMLHeadElement> m_head_element;
    RefPtr<HTMLFormElement> m_form_element;
};

}
