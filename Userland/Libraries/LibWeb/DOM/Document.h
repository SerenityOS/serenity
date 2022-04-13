/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/Forward.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleSheetList.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/NonElementParentNode.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/HTML/DocumentReadyState.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/History.h>
#include <LibWeb/HTML/Scripting/Environments.h>

namespace Web::DOM {

enum class QuirksMode {
    No,
    Limited,
    Yes
};

class Document
    : public ParentNode
    , public NonElementParentNode<Document>
    , public HTML::GlobalEventHandlers {
public:
    using WrapperType = Bindings::DocumentWrapper;

    static NonnullRefPtr<Document> create(const AK::URL& url = "about:blank")
    {
        return adopt_ref(*new Document(url));
    }
    static NonnullRefPtr<Document> create_with_global_object(Bindings::WindowObject&)
    {
        return Document::create();
    }

    virtual ~Document() override;

    String cookie(Cookie::Source = Cookie::Source::NonHttp);
    void set_cookie(String const&, Cookie::Source = Cookie::Source::NonHttp);

    String referrer() const;

    bool should_invalidate_styles_on_attribute_changes() const { return m_should_invalidate_styles_on_attribute_changes; }
    void set_should_invalidate_styles_on_attribute_changes(bool b) { m_should_invalidate_styles_on_attribute_changes = b; }

    void set_url(const AK::URL& url) { m_url = url; }
    AK::URL url() const { return m_url; }

    String url_string() const { return m_url.to_string(); }
    String document_uri() const { return m_url.to_string(); }

    Origin origin() const;
    void set_origin(Origin const& origin);

    AK::URL parse_url(String const&) const;

    CSS::StyleComputer& style_computer() { return *m_style_computer; }
    const CSS::StyleComputer& style_computer() const { return *m_style_computer; }

    CSS::StyleSheetList& style_sheets() { return *m_style_sheets; }
    const CSS::StyleSheetList& style_sheets() const { return *m_style_sheets; }

    NonnullRefPtr<CSS::StyleSheetList> style_sheets_for_bindings() { return *m_style_sheets; }

    virtual FlyString node_name() const override { return "#document"; }

    void set_hovered_node(Node*);
    Node* hovered_node() { return m_hovered_node; }
    Node const* hovered_node() const { return m_hovered_node; }

    void set_inspected_node(Node*);
    Node* inspected_node() { return m_inspected_node; }
    Node const* inspected_node() const { return m_inspected_node; }

    Element* document_element();
    Element const* document_element() const;

    HTML::HTMLHtmlElement* html_element();
    HTML::HTMLHeadElement* head();
    HTML::HTMLElement* body();

    const HTML::HTMLHtmlElement* html_element() const
    {
        return const_cast<Document*>(this)->html_element();
    }

    const HTML::HTMLHeadElement* head() const
    {
        return const_cast<Document*>(this)->head();
    }

    const HTML::HTMLElement* body() const
    {
        return const_cast<Document*>(this)->body();
    }

    ExceptionOr<void> set_body(HTML::HTMLElement* new_body);

    String title() const;
    void set_title(String const&);

    void attach_to_browsing_context(Badge<HTML::BrowsingContext>, HTML::BrowsingContext&);
    void detach_from_browsing_context(Badge<HTML::BrowsingContext>, HTML::BrowsingContext&);

    HTML::BrowsingContext* browsing_context() { return m_browsing_context.ptr(); }
    HTML::BrowsingContext const* browsing_context() const { return m_browsing_context.ptr(); }

    Page* page();
    Page const* page() const;

    Color background_color(Gfx::Palette const&) const;
    Vector<CSS::BackgroundLayerData> const* background_layers() const;

    Color link_color() const;
    void set_link_color(Color);

    Color active_link_color() const;
    void set_active_link_color(Color);

    Color visited_link_color() const;
    void set_visited_link_color(Color);

    void force_layout();

    void update_style();
    void update_layout();

    void set_needs_layout();

    void invalidate_layout();
    void invalidate_stacking_context_tree();

    virtual bool is_child_allowed(Node const&) const override;

    Layout::InitialContainingBlock const* layout_node() const;
    Layout::InitialContainingBlock* layout_node();

    void schedule_style_update();
    void schedule_layout_update();

    NonnullRefPtr<HTMLCollection> get_elements_by_name(String const&);
    NonnullRefPtr<HTMLCollection> get_elements_by_class_name(FlyString const&);

    NonnullRefPtr<HTMLCollection> applets();
    NonnullRefPtr<HTMLCollection> anchors();
    NonnullRefPtr<HTMLCollection> images();
    NonnullRefPtr<HTMLCollection> embeds();
    NonnullRefPtr<HTMLCollection> plugins();
    NonnullRefPtr<HTMLCollection> links();
    NonnullRefPtr<HTMLCollection> forms();
    NonnullRefPtr<HTMLCollection> scripts();

    String const& source() const { return m_source; }
    void set_source(String const& source) { m_source = source; }

    HTML::EnvironmentSettingsObject& relevant_settings_object();
    JS::Realm& realm();
    JS::Interpreter& interpreter();

    JS::Value run_javascript(StringView source, StringView filename = "(unknown)");

    ExceptionOr<NonnullRefPtr<Element>> create_element(String const& tag_name);
    ExceptionOr<NonnullRefPtr<Element>> create_element_ns(String const& namespace_, String const& qualified_name);
    NonnullRefPtr<DocumentFragment> create_document_fragment();
    NonnullRefPtr<Text> create_text_node(String const& data);
    NonnullRefPtr<Comment> create_comment(String const& data);
    NonnullRefPtr<Range> create_range();
    NonnullRefPtr<Event> create_event(String const& interface);

    void set_pending_parsing_blocking_script(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement*);
    HTML::HTMLScriptElement* pending_parsing_blocking_script() { return m_pending_parsing_blocking_script; }
    NonnullRefPtr<HTML::HTMLScriptElement> take_pending_parsing_blocking_script(Badge<HTML::HTMLParser>);

    void add_script_to_execute_when_parsing_has_finished(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement&);
    NonnullRefPtrVector<HTML::HTMLScriptElement> take_scripts_to_execute_when_parsing_has_finished(Badge<HTML::HTMLParser>);
    NonnullRefPtrVector<HTML::HTMLScriptElement>& scripts_to_execute_when_parsing_has_finished() { return m_scripts_to_execute_when_parsing_has_finished; }

    void add_script_to_execute_as_soon_as_possible(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement&);
    NonnullRefPtrVector<HTML::HTMLScriptElement> take_scripts_to_execute_as_soon_as_possible(Badge<HTML::HTMLParser>);
    NonnullRefPtrVector<HTML::HTMLScriptElement>& scripts_to_execute_as_soon_as_possible() { return m_scripts_to_execute_as_soon_as_possible; }

    QuirksMode mode() const { return m_quirks_mode; }
    bool in_quirks_mode() const { return m_quirks_mode == QuirksMode::Yes; }
    void set_quirks_mode(QuirksMode mode) { m_quirks_mode = mode; }

    ExceptionOr<NonnullRefPtr<Node>> import_node(NonnullRefPtr<Node> node, bool deep);
    void adopt_node(Node&);
    ExceptionOr<NonnullRefPtr<Node>> adopt_node_binding(NonnullRefPtr<Node>);

    DocumentType const* doctype() const;
    String const& compat_mode() const;

    void set_editable(bool editable) { m_editable = editable; }
    virtual bool is_editable() const final;

    Element* focused_element() { return m_focused_element; }
    Element const* focused_element() const { return m_focused_element; }

    void set_focused_element(Element*);

    Element const* active_element() const { return m_active_element; }

    void set_active_element(Element*);

    bool created_for_appropriate_template_contents() const { return m_created_for_appropriate_template_contents; }
    void set_created_for_appropriate_template_contents(bool value) { m_created_for_appropriate_template_contents = value; }

    Document* associated_inert_template_document() { return m_associated_inert_template_document; }
    Document const* associated_inert_template_document() const { return m_associated_inert_template_document; }
    void set_associated_inert_template_document(Document& document) { m_associated_inert_template_document = document; }

    String ready_state() const;
    void update_readiness(HTML::DocumentReadyState);

    void ref_from_node(Badge<Node>)
    {
        increment_referencing_node_count();
    }

    void unref_from_node(Badge<Node>)
    {
        decrement_referencing_node_count();
    }

    void removed_last_ref();

    HTML::Window& window() { return *m_window; }
    HTML::Window const& window() const { return *m_window; }

    ExceptionOr<void> write(Vector<String> const& strings);
    ExceptionOr<void> writeln(Vector<String> const& strings);

    ExceptionOr<Document*> open(String const& = "", String const& = "");
    ExceptionOr<void> close();

    HTML::Window* default_view() { return m_window; }

    String const& content_type() const { return m_content_type; }
    void set_content_type(String const& content_type) { m_content_type = content_type; }

    bool has_encoding() const { return m_encoding.has_value(); }
    Optional<String> const& encoding() const { return m_encoding; }
    String encoding_or_default() const { return m_encoding.value_or("UTF-8"); }
    void set_encoding(Optional<String> const& encoding) { m_encoding = encoding; }

    // NOTE: These are intended for the JS bindings
    String character_set() const { return encoding_or_default(); }
    String charset() const { return encoding_or_default(); }
    String input_encoding() const { return encoding_or_default(); }

    bool ready_for_post_load_tasks() const { return m_ready_for_post_load_tasks; }
    void set_ready_for_post_load_tasks(bool ready) { m_ready_for_post_load_tasks = ready; }

    void completely_finish_loading();

    NonnullRefPtr<DOMImplementation> implementation() const;

    RefPtr<HTML::HTMLScriptElement> current_script() const { return m_current_script; }
    void set_current_script(Badge<HTML::HTMLScriptElement>, RefPtr<HTML::HTMLScriptElement> script) { m_current_script = move(script); }

    u32 ignore_destructive_writes_counter() const { return m_ignore_destructive_writes_counter; }
    void increment_ignore_destructive_writes_counter() { m_ignore_destructive_writes_counter++; }
    void decrement_ignore_destructive_writes_counter() { m_ignore_destructive_writes_counter--; }

    virtual EventTarget* get_parent(Event const&) override;

    String dump_dom_tree_as_json() const;

    bool has_a_style_sheet_that_is_blocking_scripts() const;

    bool is_fully_active() const;
    bool is_active() const;

    NonnullRefPtr<HTML::History> history() const { return m_history; }

    Bindings::LocationObject* location();

    size_t number_of_things_delaying_the_load_event() { return m_number_of_things_delaying_the_load_event; }
    void increment_number_of_things_delaying_the_load_event(Badge<DocumentLoadEventDelayer>);
    void decrement_number_of_things_delaying_the_load_event(Badge<DocumentLoadEventDelayer>);

    bool page_showing() const { return m_page_showing; }
    void set_page_showing(bool value) { m_page_showing = value; }

    bool hidden() const;
    String visibility_state() const;

    void run_the_resize_steps();

    void evaluate_media_queries_and_report_changes();
    void add_media_query_list(NonnullRefPtr<CSS::MediaQueryList>&);

    bool has_focus() const;

    void set_parser(Badge<HTML::HTMLParser>, HTML::HTMLParser&);
    void detach_parser(Badge<HTML::HTMLParser>);

    static bool is_valid_name(String const&);

    struct PrefixAndTagName {
        FlyString prefix;
        FlyString tag_name;
    };
    static ExceptionOr<PrefixAndTagName> validate_qualified_name(String const& qualified_name);

    NonnullRefPtr<NodeIterator> create_node_iterator(Node& root, unsigned what_to_show, RefPtr<NodeFilter>);
    NonnullRefPtr<TreeWalker> create_tree_walker(Node& root, unsigned what_to_show, RefPtr<NodeFilter>);

    void register_node_iterator(Badge<NodeIterator>, NodeIterator&);
    void unregister_node_iterator(Badge<NodeIterator>, NodeIterator&);

    template<typename Callback>
    void for_each_node_iterator(Callback callback)
    {
        for (auto* node_iterator : m_node_iterators)
            callback(*node_iterator);
    }

    bool needs_full_style_update() const { return m_needs_full_style_update; }
    void set_needs_full_style_update(bool b) { m_needs_full_style_update = b; }

    bool in_removed_last_ref() const { return m_in_removed_last_ref; }

    bool has_active_favicon() const { return m_active_favicon; }
    void check_favicon_after_loading_link_resource();

private:
    explicit Document(const AK::URL&);

    // ^HTML::GlobalEventHandlers
    virtual EventTarget& global_event_handlers_to_event_target() final { return *this; }

    void tear_down_layout_tree();

    void evaluate_media_rules();

    ExceptionOr<void> run_the_document_write_steps(String);

    void increment_referencing_node_count()
    {
        VERIFY(!m_deletion_has_begun);
        ++m_referencing_node_count;
    }

    void decrement_referencing_node_count()
    {
        VERIFY(!m_deletion_has_begun);
        VERIFY(m_referencing_node_count);
        --m_referencing_node_count;
        if (!m_referencing_node_count && !ref_count()) {
            m_deletion_has_begun = true;
            delete this;
        }
    }

    unsigned m_referencing_node_count { 0 };

    OwnPtr<CSS::StyleComputer> m_style_computer;
    RefPtr<CSS::StyleSheetList> m_style_sheets;
    RefPtr<Node> m_hovered_node;
    RefPtr<Node> m_inspected_node;
    RefPtr<Node> m_active_favicon;
    WeakPtr<HTML::BrowsingContext> m_browsing_context;
    AK::URL m_url;

    RefPtr<HTML::Window> m_window;

    RefPtr<Layout::InitialContainingBlock> m_layout_root;

    Optional<Color> m_link_color;
    Optional<Color> m_active_link_color;
    Optional<Color> m_visited_link_color;

    RefPtr<Core::Timer> m_style_update_timer;
    RefPtr<Core::Timer> m_layout_update_timer;

    RefPtr<HTML::HTMLParser> m_parser;
    bool m_active_parser_was_aborted { false };

    String m_source;

    OwnPtr<JS::Interpreter> m_interpreter;

    RefPtr<HTML::HTMLScriptElement> m_pending_parsing_blocking_script;
    NonnullRefPtrVector<HTML::HTMLScriptElement> m_scripts_to_execute_when_parsing_has_finished;
    NonnullRefPtrVector<HTML::HTMLScriptElement> m_scripts_to_execute_as_soon_as_possible;

    QuirksMode m_quirks_mode { QuirksMode::No };
    bool m_editable { false };

    WeakPtr<Element> m_focused_element;
    WeakPtr<Element> m_active_element;

    bool m_created_for_appropriate_template_contents { false };
    RefPtr<Document> m_associated_inert_template_document;

    HTML::DocumentReadyState m_readiness { HTML::DocumentReadyState::Loading };
    String m_content_type { "application/xml" };
    Optional<String> m_encoding;

    bool m_ready_for_post_load_tasks { false };

    NonnullOwnPtr<DOMImplementation> m_implementation;
    RefPtr<HTML::HTMLScriptElement> m_current_script;

    bool m_should_invalidate_styles_on_attribute_changes { true };

    u32 m_ignore_destructive_writes_counter { 0 };

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#unload-counter
    u32 m_unload_counter { 0 };

    // https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#throw-on-dynamic-markup-insertion-counter
    u32 m_throw_on_dynamic_markup_insertion_counter { 0 };

    // https://html.spec.whatwg.org/multipage/semantics.html#script-blocking-style-sheet-counter
    u32 m_script_blocking_style_sheet_counter { 0 };

    NonnullRefPtr<HTML::History> m_history;

    size_t m_number_of_things_delaying_the_load_event { 0 };

    // https://html.spec.whatwg.org/#page-showing
    bool m_page_showing { false };

    // Used by run_the_resize_steps().
    Gfx::IntSize m_last_viewport_size;

    // Used by evaluate_media_queries_and_report_changes().
    Vector<WeakPtr<CSS::MediaQueryList>> m_media_query_lists;

    bool m_needs_layout { false };

    bool m_needs_full_style_update { false };

    HashTable<NodeIterator*> m_node_iterators;
};

}
