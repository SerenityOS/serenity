/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <LibCore/Forward.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/ScriptExecutionContext.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/CSS/StyleSheetList.h>
#include <LibWeb/DOM/NonElementParentNode.h>
#include <LibWeb/DOM/ParentNode.h>

namespace Web::DOM {

enum class QuirksMode {
    No,
    Limited,
    Yes
};

class Document
    : public ParentNode
    , public NonElementParentNode<Document>
    , public Bindings::ScriptExecutionContext {
public:
    using WrapperType = Bindings::DocumentWrapper;

    explicit Document(const URL& = {});
    virtual ~Document() override;

    void set_url(const URL& url) { m_url = url; }
    URL url() const { return m_url; }

    Origin origin() const;

    bool is_scripting_enabled() const { return true; }

    URL complete_url(const String&) const;

    void fixup();

    CSS::StyleResolver& style_resolver() { return *m_style_resolver; }
    const CSS::StyleResolver& style_resolver() const { return *m_style_resolver; }

    CSS::StyleSheetList& style_sheets() { return *m_style_sheets; }
    const CSS::StyleSheetList& style_sheets() const { return *m_style_sheets; }

    virtual FlyString node_name() const override { return "#document"; }

    void set_hovered_node(Node*);
    Node* hovered_node() { return m_hovered_node; }
    const Node* hovered_node() const { return m_hovered_node; }

    void set_inspected_node(Node*);
    Node* inspected_node() { return m_inspected_node; }
    const Node* inspected_node() const { return m_inspected_node; }

    const Element* document_element() const;
    const HTML::HTMLHtmlElement* html_element() const;
    const HTML::HTMLHeadElement* head() const;
    const HTML::HTMLElement* body() const;
    void set_body(HTML::HTMLElement& new_body);

    String title() const;

    void attach_to_frame(Badge<Frame>, Frame&);
    void detach_from_frame(Badge<Frame>, Frame&);

    Frame* frame() { return m_frame.ptr(); }
    const Frame* frame() const { return m_frame.ptr(); }

    Color background_color(const Gfx::Palette&) const;
    RefPtr<Gfx::Bitmap> background_image() const;

    Color link_color() const;
    void set_link_color(Color);

    Color active_link_color() const;
    void set_active_link_color(Color);

    Color visited_link_color() const;
    void set_visited_link_color(Color);

    void layout();
    void force_layout();
    void invalidate_layout();

    void update_style();
    void update_layout();

    virtual bool is_child_allowed(const Node&) const override;

    const LayoutDocument* layout_node() const;
    LayoutDocument* layout_node();

    void schedule_style_update();

    NonnullRefPtrVector<Element> get_elements_by_name(const String&) const;
    NonnullRefPtrVector<Element> get_elements_by_tag_name(const FlyString&) const;

    const String& source() const { return m_source; }
    void set_source(const String& source) { m_source = source; }

    virtual JS::Interpreter& interpreter() override;

    JS::Value run_javascript(const StringView&);

    NonnullRefPtr<Element> create_element(const String& tag_name);
    NonnullRefPtr<DocumentFragment> create_document_fragment();
    NonnullRefPtr<Text> create_text_node(const String& data);
    NonnullRefPtr<Comment> create_comment(const String& data);

    void set_pending_parsing_blocking_script(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement*);
    HTML::HTMLScriptElement* pending_parsing_blocking_script() { return m_pending_parsing_blocking_script; }
    NonnullRefPtr<HTML::HTMLScriptElement> take_pending_parsing_blocking_script(Badge<HTML::HTMLDocumentParser>);

    void add_script_to_execute_when_parsing_has_finished(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement&);
    NonnullRefPtrVector<HTML::HTMLScriptElement> take_scripts_to_execute_when_parsing_has_finished(Badge<HTML::HTMLDocumentParser>);

    void add_script_to_execute_as_soon_as_possible(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement&);
    NonnullRefPtrVector<HTML::HTMLScriptElement> take_scripts_to_execute_as_soon_as_possible(Badge<HTML::HTMLDocumentParser>);

    QuirksMode mode() const { return m_quirks_mode; }
    bool in_quirks_mode() const { return m_quirks_mode == QuirksMode::Yes; }
    void set_quirks_mode(QuirksMode mode) { m_quirks_mode = mode; }

    void adopt_node(Node&);

    const DocumentType* doctype() const;
    const String& compat_mode() const;

    void set_editable(bool editable) { m_editable = editable; }
    virtual bool is_editable() const final;

    Element* focused_element() { return m_focused_element; }
    const Element* focused_element() const { return m_focused_element; }

    void set_focused_element(Element*);

    bool created_for_appropriate_template_contents() const { return m_created_for_appropriate_template_contents; }
    void set_created_for_appropriate_template_contents(bool value) { m_created_for_appropriate_template_contents = value; }

    Document* associated_inert_template_document() { return m_associated_inert_template_document; }
    const Document* associated_inert_template_document() const { return m_associated_inert_template_document; }
    void set_associated_inert_template_document(Document& document) { m_associated_inert_template_document = document; }

    const String& ready_state() const { return m_ready_state; }
    void set_ready_state(const String&);

private:
    virtual RefPtr<LayoutNode> create_layout_node(const CSS::StyleProperties* parent_style) override;

    OwnPtr<CSS::StyleResolver> m_style_resolver;
    RefPtr<CSS::StyleSheetList> m_style_sheets;
    RefPtr<Node> m_hovered_node;
    RefPtr<Node> m_inspected_node;
    WeakPtr<Frame> m_frame;
    URL m_url;

    RefPtr<Window> m_window;

    RefPtr<LayoutDocument> m_layout_root;

    Optional<Color> m_link_color;
    Optional<Color> m_active_link_color;
    Optional<Color> m_visited_link_color;

    RefPtr<Core::Timer> m_style_update_timer;

    String m_source;

    OwnPtr<JS::Interpreter> m_interpreter;

    RefPtr<HTML::HTMLScriptElement> m_pending_parsing_blocking_script;
    NonnullRefPtrVector<HTML::HTMLScriptElement> m_scripts_to_execute_when_parsing_has_finished;
    NonnullRefPtrVector<HTML::HTMLScriptElement> m_scripts_to_execute_as_soon_as_possible;

    QuirksMode m_quirks_mode { QuirksMode::No };
    bool m_editable { false };

    WeakPtr<Element> m_focused_element;

    bool m_created_for_appropriate_template_contents { false };
    RefPtr<Document> m_associated_inert_template_document;

    String m_ready_state { "loading" };
};

}

AK_BEGIN_TYPE_TRAITS(Web::DOM::Document)
static bool is_type(const Web::DOM::Node& node) { return node.is_document(); }
AK_END_TYPE_TRAITS()
