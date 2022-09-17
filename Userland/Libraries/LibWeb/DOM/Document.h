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
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleSheetList.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/NonElementParentNode.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/HTML/CrossOrigin/CrossOriginOpenerPolicy.h>
#include <LibWeb/HTML/DocumentReadyState.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/History.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>

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
    WEB_PLATFORM_OBJECT(Document, ParentNode);

public:
    enum class Type {
        XML,
        HTML
    };

    static JS::NonnullGCPtr<Document> create_and_initialize(Type, String content_type, HTML::NavigationParams);

    static JS::NonnullGCPtr<Document> create(HTML::Window&, AK::URL const& url = "about:blank"sv);
    static JS::NonnullGCPtr<Document> create_with_global_object(HTML::Window&);
    virtual ~Document() override;

    size_t next_layout_node_serial_id(Badge<Layout::Node>) { return m_next_layout_node_serial_id++; }
    size_t layout_node_count() const { return m_next_layout_node_serial_id; }

    String cookie(Cookie::Source = Cookie::Source::NonHttp);
    void set_cookie(String const&, Cookie::Source = Cookie::Source::NonHttp);

    String referrer() const;
    void set_referrer(String);

    bool should_invalidate_styles_on_attribute_changes() const { return m_should_invalidate_styles_on_attribute_changes; }
    void set_should_invalidate_styles_on_attribute_changes(bool b) { m_should_invalidate_styles_on_attribute_changes = b; }

    void set_url(const AK::URL& url) { m_url = url; }
    AK::URL url() const { return m_url; }
    AK::URL fallback_base_url() const;
    AK::URL base_url() const;

    JS::GCPtr<HTML::HTMLBaseElement> first_base_element_with_href_in_tree_order() const;

    String url_string() const { return m_url.to_string(); }
    String document_uri() const { return m_url.to_string(); }

    HTML::Origin origin() const;
    void set_origin(HTML::Origin const& origin);

    HTML::CrossOriginOpenerPolicy const& cross_origin_opener_policy() const { return m_cross_origin_opener_policy; }
    void set_cross_origin_opener_policy(HTML::CrossOriginOpenerPolicy policy) { m_cross_origin_opener_policy = move(policy); }

    AK::URL parse_url(String const&) const;

    CSS::StyleComputer& style_computer() { return *m_style_computer; }
    const CSS::StyleComputer& style_computer() const { return *m_style_computer; }

    CSS::StyleSheetList& style_sheets();
    CSS::StyleSheetList const& style_sheets() const;

    CSS::StyleSheetList* style_sheets_for_bindings() { return &style_sheets(); }

    virtual FlyString node_name() const override { return "#document"; }

    void set_hovered_node(Node*);
    Node* hovered_node() { return m_hovered_node.ptr(); }
    Node const* hovered_node() const { return m_hovered_node.ptr(); }

    void set_inspected_node(Node*);
    Node* inspected_node() { return m_inspected_node.ptr(); }
    Node const* inspected_node() const { return m_inspected_node.ptr(); }

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

    JS::NonnullGCPtr<HTMLCollection> get_elements_by_name(String const&);
    JS::NonnullGCPtr<HTMLCollection> get_elements_by_class_name(FlyString const&);

    JS::NonnullGCPtr<HTMLCollection> applets();
    JS::NonnullGCPtr<HTMLCollection> anchors();
    JS::NonnullGCPtr<HTMLCollection> images();
    JS::NonnullGCPtr<HTMLCollection> embeds();
    JS::NonnullGCPtr<HTMLCollection> plugins();
    JS::NonnullGCPtr<HTMLCollection> links();
    JS::NonnullGCPtr<HTMLCollection> forms();
    JS::NonnullGCPtr<HTMLCollection> scripts();

    String const& source() const { return m_source; }
    void set_source(String const& source) { m_source = source; }

    HTML::EnvironmentSettingsObject& relevant_settings_object();

    JS::Value run_javascript(StringView source, StringView filename = "(unknown)"sv);

    ExceptionOr<JS::NonnullGCPtr<Element>> create_element(FlyString const& local_name);
    ExceptionOr<JS::NonnullGCPtr<Element>> create_element_ns(String const& namespace_, String const& qualified_name);
    JS::NonnullGCPtr<DocumentFragment> create_document_fragment();
    JS::NonnullGCPtr<Text> create_text_node(String const& data);
    JS::NonnullGCPtr<Comment> create_comment(String const& data);
    ExceptionOr<JS::NonnullGCPtr<Event>> create_event(String const& interface);
    JS::NonnullGCPtr<Range> create_range();

    void set_pending_parsing_blocking_script(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement*);
    HTML::HTMLScriptElement* pending_parsing_blocking_script() { return m_pending_parsing_blocking_script.ptr(); }
    JS::NonnullGCPtr<HTML::HTMLScriptElement> take_pending_parsing_blocking_script(Badge<HTML::HTMLParser>);

    void add_script_to_execute_when_parsing_has_finished(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement&);
    Vector<JS::Handle<HTML::HTMLScriptElement>> take_scripts_to_execute_when_parsing_has_finished(Badge<HTML::HTMLParser>);
    Vector<JS::Handle<HTML::HTMLScriptElement>>& scripts_to_execute_when_parsing_has_finished() { return m_scripts_to_execute_when_parsing_has_finished; }

    void add_script_to_execute_as_soon_as_possible(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement&);
    Vector<JS::Handle<HTML::HTMLScriptElement>> take_scripts_to_execute_as_soon_as_possible(Badge<HTML::HTMLParser>);
    Vector<JS::Handle<HTML::HTMLScriptElement>>& scripts_to_execute_as_soon_as_possible() { return m_scripts_to_execute_as_soon_as_possible; }

    void add_script_to_execute_in_order_as_soon_as_possible(Badge<HTML::HTMLScriptElement>, HTML::HTMLScriptElement&);
    Vector<JS::Handle<HTML::HTMLScriptElement>> take_scripts_to_execute_in_order_as_soon_as_possible(Badge<HTML::HTMLParser>);
    Vector<JS::Handle<HTML::HTMLScriptElement>>& scripts_to_execute_in_order_as_soon_as_possible() { return m_scripts_to_execute_in_order_as_soon_as_possible; }

    QuirksMode mode() const { return m_quirks_mode; }
    bool in_quirks_mode() const { return m_quirks_mode == QuirksMode::Yes; }
    void set_quirks_mode(QuirksMode mode) { m_quirks_mode = mode; }

    Type document_type() const { return m_type; }
    void set_document_type(Type type) { m_type = type; }

    // https://dom.spec.whatwg.org/#xml-document
    bool is_xml_document() const { return m_type == Type::XML; }

    ExceptionOr<JS::NonnullGCPtr<Node>> import_node(JS::NonnullGCPtr<Node> node, bool deep);
    void adopt_node(Node&);
    ExceptionOr<JS::NonnullGCPtr<Node>> adopt_node_binding(JS::NonnullGCPtr<Node>);

    DocumentType const* doctype() const;
    String const& compat_mode() const;

    void set_editable(bool editable) { m_editable = editable; }
    virtual bool is_editable() const final;

    Element* focused_element() { return m_focused_element.ptr(); }
    Element const* focused_element() const { return m_focused_element.ptr(); }

    void set_focused_element(Element*);

    Element const* active_element() const { return m_active_element.ptr(); }

    void set_active_element(Element*);

    bool created_for_appropriate_template_contents() const { return m_created_for_appropriate_template_contents; }
    void set_created_for_appropriate_template_contents(bool value) { m_created_for_appropriate_template_contents = value; }

    Document* associated_inert_template_document() { return m_associated_inert_template_document.ptr(); }
    Document const* associated_inert_template_document() const { return m_associated_inert_template_document.ptr(); }
    void set_associated_inert_template_document(Document& document) { m_associated_inert_template_document = &document; }

    String ready_state() const;
    void update_readiness(HTML::DocumentReadyState);

    HTML::Window& window() const { return const_cast<HTML::Window&>(*m_window); }

    void set_window(Badge<HTML::BrowsingContext>, HTML::Window&);

    ExceptionOr<void> write(Vector<String> const& strings);
    ExceptionOr<void> writeln(Vector<String> const& strings);

    ExceptionOr<Document*> open(String const& = "", String const& = "");
    ExceptionOr<void> close();

    HTML::Window* default_view() { return m_window.ptr(); }

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

    DOMImplementation* implementation();

    JS::GCPtr<HTML::HTMLScriptElement> current_script() const { return m_current_script.ptr(); }
    void set_current_script(Badge<HTML::HTMLScriptElement>, JS::GCPtr<HTML::HTMLScriptElement> script) { m_current_script = move(script); }

    u32 ignore_destructive_writes_counter() const { return m_ignore_destructive_writes_counter; }
    void increment_ignore_destructive_writes_counter() { m_ignore_destructive_writes_counter++; }
    void decrement_ignore_destructive_writes_counter() { m_ignore_destructive_writes_counter--; }

    virtual EventTarget* get_parent(Event const&) override;

    String dump_dom_tree_as_json() const;

    bool has_a_style_sheet_that_is_blocking_scripts() const;

    bool is_fully_active() const;
    bool is_active() const;

    JS::NonnullGCPtr<HTML::History> history();

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
    void add_media_query_list(JS::NonnullGCPtr<CSS::MediaQueryList>);

    bool has_focus() const;

    void set_parser(Badge<HTML::HTMLParser>, HTML::HTMLParser&);
    void detach_parser(Badge<HTML::HTMLParser>);

    static bool is_valid_name(String const&);

    struct PrefixAndTagName {
        FlyString prefix;
        FlyString tag_name;
    };
    static ExceptionOr<PrefixAndTagName> validate_qualified_name(JS::Object& global_object, String const& qualified_name);

    JS::NonnullGCPtr<NodeIterator> create_node_iterator(Node& root, unsigned what_to_show, JS::GCPtr<NodeFilter>);
    JS::NonnullGCPtr<TreeWalker> create_tree_walker(Node& root, unsigned what_to_show, JS::GCPtr<NodeFilter>);

    void register_node_iterator(Badge<NodeIterator>, NodeIterator&);
    void unregister_node_iterator(Badge<NodeIterator>, NodeIterator&);

    template<typename Callback>
    void for_each_node_iterator(Callback callback)
    {
        for (auto& node_iterator : m_node_iterators)
            callback(*node_iterator);
    }

    bool needs_full_style_update() const { return m_needs_full_style_update; }
    void set_needs_full_style_update(bool b) { m_needs_full_style_update = b; }

    bool has_active_favicon() const { return m_active_favicon; }
    void check_favicon_after_loading_link_resource();

    // https://html.spec.whatwg.org/multipage/dom.html#is-initial-about:blank
    bool is_initial_about_blank() const { return m_is_initial_about_blank; }
    void set_is_initial_about_blank(bool b) { m_is_initial_about_blank = b; }

    String domain() const;
    void set_domain(String const& domain);

protected:
    virtual void visit_edges(Cell::Visitor&) override;

private:
    Document(HTML::Window&, AK::URL const&);

    // ^HTML::GlobalEventHandlers
    virtual EventTarget& global_event_handlers_to_event_target(FlyString const&) final { return *this; }

    void tear_down_layout_tree();

    void evaluate_media_rules();

    ExceptionOr<void> run_the_document_write_steps(String);

    size_t m_next_layout_node_serial_id { 0 };

    OwnPtr<CSS::StyleComputer> m_style_computer;
    JS::GCPtr<CSS::StyleSheetList> m_style_sheets;
    JS::GCPtr<Node> m_hovered_node;
    JS::GCPtr<Node> m_inspected_node;
    JS::GCPtr<Node> m_active_favicon;
    WeakPtr<HTML::BrowsingContext> m_browsing_context;
    AK::URL m_url;

    JS::GCPtr<HTML::Window> m_window;

    RefPtr<Layout::InitialContainingBlock> m_layout_root;

    Optional<Color> m_link_color;
    Optional<Color> m_active_link_color;
    Optional<Color> m_visited_link_color;

    RefPtr<Platform::Timer> m_style_update_timer;
    RefPtr<Platform::Timer> m_layout_update_timer;

    RefPtr<HTML::HTMLParser> m_parser;
    bool m_active_parser_was_aborted { false };

    String m_source;

    JS::GCPtr<HTML::HTMLScriptElement> m_pending_parsing_blocking_script;

    Vector<JS::Handle<HTML::HTMLScriptElement>> m_scripts_to_execute_when_parsing_has_finished;

    // https://html.spec.whatwg.org/multipage/scripting.html#list-of-scripts-that-will-execute-in-order-as-soon-as-possible
    Vector<JS::Handle<HTML::HTMLScriptElement>> m_scripts_to_execute_in_order_as_soon_as_possible;

    // https://html.spec.whatwg.org/multipage/scripting.html#set-of-scripts-that-will-execute-as-soon-as-possible
    Vector<JS::Handle<HTML::HTMLScriptElement>> m_scripts_to_execute_as_soon_as_possible;

    QuirksMode m_quirks_mode { QuirksMode::No };

    // https://dom.spec.whatwg.org/#concept-document-type
    Type m_type { Type::HTML };

    bool m_editable { false };

    JS::GCPtr<Element> m_focused_element;
    JS::GCPtr<Element> m_active_element;

    bool m_created_for_appropriate_template_contents { false };
    JS::GCPtr<Document> m_associated_inert_template_document;

    HTML::DocumentReadyState m_readiness { HTML::DocumentReadyState::Loading };
    String m_content_type { "application/xml" };
    Optional<String> m_encoding;

    bool m_ready_for_post_load_tasks { false };

    JS::GCPtr<DOMImplementation> m_implementation;
    JS::GCPtr<HTML::HTMLScriptElement> m_current_script;

    bool m_should_invalidate_styles_on_attribute_changes { true };

    u32 m_ignore_destructive_writes_counter { 0 };

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#unload-counter
    u32 m_unload_counter { 0 };

    // https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#throw-on-dynamic-markup-insertion-counter
    u32 m_throw_on_dynamic_markup_insertion_counter { 0 };

    // https://html.spec.whatwg.org/multipage/semantics.html#script-blocking-style-sheet-counter
    u32 m_script_blocking_style_sheet_counter { 0 };

    JS::GCPtr<HTML::History> m_history;

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

    // https://html.spec.whatwg.org/multipage/dom.html#is-initial-about:blank
    bool m_is_initial_about_blank { false };

    // https://html.spec.whatwg.org/multipage/dom.html#concept-document-coop
    HTML::CrossOriginOpenerPolicy m_cross_origin_opener_policy;

    // https://html.spec.whatwg.org/multipage/dom.html#the-document's-referrer
    String m_referrer { "" };

    // https://dom.spec.whatwg.org/#concept-document-origin
    HTML::Origin m_origin;
};

}

WRAPPER_HACK(Document, Web::DOM)
