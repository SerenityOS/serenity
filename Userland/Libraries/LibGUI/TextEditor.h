/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Function.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/Timer.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Action.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Forward.h>
#include <LibGUI/TextDocument.h>
#include <LibGfx/TextAlignment.h>
#include <LibSyntax/Forward.h>
#include <LibSyntax/HighlighterClient.h>
#include <LibSyntax/TextRange.h>

namespace GUI {

class TextEditor
    : public AbstractScrollableWidget
    , public TextDocument::Client
    , public Syntax::HighlighterClient
    , public Clipboard::ClipboardClient {
    C_OBJECT(TextEditor);

public:
    enum Type {
        MultiLine,
        SingleLine
    };

    enum Mode {
        Editable,
        ReadOnly,
        DisplayOnly
    };

    enum WrappingMode {
        NoWrap,
        WrapAnywhere,
        WrapAtWords
    };

    virtual ~TextEditor() override;

    TextDocument const& document() const { return *m_document; }
    TextDocument& document() { return *m_document; }
    bool has_document() const { return !!m_document; }

    virtual void set_document(TextDocument&);

    ByteString const& placeholder() const { return m_placeholder; }
    void set_placeholder(StringView placeholder) { m_placeholder = placeholder; }

    TextDocumentLine& current_line() { return line(m_cursor.line()); }
    TextDocumentLine const& current_line() const { return line(m_cursor.line()); }

    void set_visualize_trailing_whitespace(bool);
    bool visualize_trailing_whitespace() const { return m_visualize_trailing_whitespace; }

    void set_visualize_leading_whitespace(bool);
    bool visualize_leading_whitespace() const { return m_visualize_leading_whitespace; }

    bool is_cursor_line_highlighted() const { return m_cursor_line_highlighting; }
    void set_cursor_line_highlighting(bool);

    virtual bool is_automatic_indentation_enabled() const final { return m_automatic_indentation_enabled; }
    void set_automatic_indentation_enabled(bool enabled) { m_automatic_indentation_enabled = enabled; }

    virtual int soft_tab_width() const final { return m_soft_tab_width; }
    void set_soft_tab_width(int width) { m_soft_tab_width = width; }

    WrappingMode wrapping_mode() const { return m_wrapping_mode; }
    bool is_wrapping_enabled() const { return m_wrapping_mode != WrappingMode::NoWrap; }
    void set_wrapping_mode(WrappingMode);

    Gfx::TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(Gfx::TextAlignment);

    Type type() const { return m_type; }
    bool is_single_line() const { return m_type == SingleLine; }
    bool is_multi_line() const { return m_type == MultiLine; }

    Mode mode() const { return m_mode; }
    bool is_editable() const { return m_mode == Editable; }
    bool is_readonly() const { return m_mode == ReadOnly; }
    bool is_displayonly() const { return m_mode == DisplayOnly; }
    void set_mode(Mode const);

    void set_editing_cursor();

    bool is_relative_line_number() const { return m_relative_line_number; }
    void set_relative_line_number(bool);

    bool is_ruler_visible() const { return m_ruler_visible; }
    void set_ruler_visible(bool);

    bool is_gutter_visible() const { return m_gutter_visible; }
    void set_gutter_visible(bool);

    AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, GutterIndicatorID, CastToUnderlying, Comparison);
    using PaintGutterIndicator = Function<void(Gfx::Painter&, Gfx::IntRect, size_t line)>;
    using OnGutterIndicatorClick = Function<void(size_t line, unsigned modifiers)>;
    ErrorOr<GutterIndicatorID> register_gutter_indicator(PaintGutterIndicator, OnGutterIndicatorClick = nullptr);
    void add_gutter_indicator(GutterIndicatorID, size_t line);
    void remove_gutter_indicator(GutterIndicatorID, size_t line);
    void clear_gutter_indicators(GutterIndicatorID);
    Gfx::IntRect gutter_indicator_rect(size_t line_number, int indicator_position) const;

    void set_icon(Gfx::Bitmap const*);
    Gfx::Bitmap const* icon() const { return m_icon; }

    Function<void()> on_cursor_change;
    Function<void()> on_selection_change;
    Function<void()> on_focusin;
    Function<void()> on_focusout;
    Function<void()> on_highlighter_change;
    Function<void(size_t line, unsigned modifiers)> on_gutter_click;

    void set_text(StringView, AllowCallback = AllowCallback::Yes);
    void scroll_cursor_into_view();
    void scroll_position_into_view(TextPosition const&);
    size_t line_count() const { return document().line_count(); }
    TextDocumentLine& line(size_t index) { return document().line(index); }
    TextDocumentLine const& line(size_t index) const { return document().line(index); }
    Vector<NonnullOwnPtr<TextDocumentLine>>& lines() { return document().lines(); }
    Vector<NonnullOwnPtr<TextDocumentLine>> const& lines() const { return document().lines(); }
    int line_height() const;
    TextPosition cursor() const { return m_cursor; }
    TextRange normalized_selection() const { return m_selection.normalized(); }

    void insert_at_cursor_or_replace_selection(StringView);
    void replace_all_text_without_resetting_undo_stack(StringView text, StringView action_text);
    ErrorOr<void> write_to_file(StringView path);
    ErrorOr<void> write_to_file(Core::File&);
    bool has_selection() const { return m_selection.is_valid(); }
    ByteString selected_text() const;
    size_t number_of_words() const;
    size_t number_of_selected_words() const;
    void set_selection(TextRange const&);
    void clear_selection();
    bool can_undo() const { return document().can_undo(); }
    bool can_redo() const { return document().can_redo(); }

    ByteString text() const;

    void clear();

    void cut();
    void copy();
    void paste();
    void do_delete();
    void delete_current_line();
    void delete_previous_word();
    void delete_previous_char();
    void delete_from_line_start_to_cursor();
    void select_all();
    void insert_emoji();
    void set_or_clear_emoji_input_callback();
    void select_current_line();
    virtual void undo();
    virtual void redo();
    bool is_indenting_selection();
    void indent_selection();
    void unindent_selection();
    void unindent_line();

    Function<void()> on_change;
    Function<void(bool modified)> on_modified_change;
    Function<void()> on_mousedown;
    Function<void()> on_return_pressed;
    Function<void()> on_shift_return_pressed;
    Function<void()> on_ctrl_return_pressed;
    Function<void()> on_escape_pressed;
    Function<void()> on_up_pressed;
    Function<void()> on_down_pressed;
    Function<void()> on_pageup_pressed;
    Function<void()> on_pagedown_pressed;

    Action& undo_action() { return *m_undo_action; }
    Action& redo_action() { return *m_redo_action; }
    Action& cut_action() { return *m_cut_action; }
    Action& copy_action() { return *m_copy_action; }
    Action& paste_action() { return *m_paste_action; }
    Action& go_to_line_or_column_action() { return *m_go_to_line_or_column_action; }
    Action& select_all_action() { return *m_select_all_action; }
    Action& insert_emoji_action() { return *m_insert_emoji_action; }

    void add_custom_context_menu_action(Action&);

    void set_cursor_and_focus_line(size_t line, size_t column);
    void set_cursor(size_t line, size_t column);
    virtual void set_cursor(TextPosition const&);
    void set_cursor_to_text_position(Gfx::IntPoint);
    void set_cursor_to_end_of_visual_line();

    Syntax::Highlighter* syntax_highlighter();
    Syntax::Highlighter const* syntax_highlighter() const;
    void set_syntax_highlighter(OwnPtr<Syntax::Highlighter>);

    AutocompleteProvider const* autocomplete_provider() const;
    void set_autocomplete_provider(OwnPtr<AutocompleteProvider>&&);

    EditingEngine const* editing_engine() const;
    void set_editing_engine(OwnPtr<EditingEngine>);

    bool should_autocomplete_automatically() const { return m_autocomplete_timer; }
    void set_should_autocomplete_automatically(bool);
    void hide_autocomplete();

    Optional<u32> const& substitution_code_point() const { return m_substitution_code_point; }
    void set_substitution_code_point(Optional<u32> code_point);

    bool is_in_drag_select() const { return m_in_drag_select; }

    TextRange& selection() { return m_selection; }
    void did_update_selection();
    void did_change(AllowCallback = AllowCallback::Yes);
    void update_cursor();

    void add_code_point(u32 code_point);
    void reset_cursor_blink();
    void update_selection(bool is_selecting);

    int number_of_visible_lines() const;
    Gfx::IntRect cursor_content_rect() const;
    TextPosition text_position_at_content_position(Gfx::IntPoint) const;

    void delete_text_range(TextRange);

    bool text_is_secret() const { return m_text_is_secret; }
    void set_text_is_secret(bool text_is_secret);
    void force_rehighlight();

    enum class SearchDirection {
        Forward,
        Backward,
    };
    TextRange find_text(StringView needle, SearchDirection, GUI::TextDocument::SearchShouldWrap, bool use_regex, bool match_case);
    void highlight_all_occurances_of(ByteString const selected_text);
    void reset_search_results();
    Optional<size_t> search_result_index() const { return m_search_result_index; }
    Vector<TextRange> const& search_results() const { return m_search_results; }

    virtual Optional<UISize> calculated_min_size() const override;

    template<class T, class... Args>
    inline void execute(Badge<EditingEngine>, Args&&... args)
    {
        execute<T>(forward<Args>(args)...);
    }

protected:
    explicit TextEditor(Type = Type::MultiLine);

    virtual void did_change_font() override;
    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void focusin_event(FocusEvent&) override;
    virtual void focusout_event(FocusEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void theme_change_event(ThemeChangeEvent&) override;
    virtual void cursor_did_change();
    Gfx::IntRect gutter_content_rect(size_t line) const;
    Gfx::IntRect ruler_content_rect(size_t line) const;
    Gfx::IntRect folding_indicator_rect(size_t line) const;

    TextPosition text_position_at(Gfx::IntPoint) const;
    bool ruler_visible() const { return m_ruler_visible; }
    bool gutter_visible() const { return m_gutter_visible; }
    Gfx::IntRect content_rect_for_position(TextPosition const&) const;
    int gutter_width() const;
    int ruler_width() const;
    int folding_indicator_width() const;
    int fixed_elements_width() const { return gutter_width() + ruler_width() + folding_indicator_width(); }

    virtual void highlighter_did_set_spans(Vector<TextDocumentSpan> spans) final { document().set_spans(Syntax::HighlighterClient::span_collection_index, move(spans)); }
    virtual void highlighter_did_set_folding_regions(Vector<TextDocumentFoldingRegion> folding_regions) final;

private:
    friend TextDocumentLine;

    // ^TextDocument::Client
    virtual void document_did_append_line() override;
    virtual void document_did_insert_line(size_t) override;
    virtual void document_did_remove_line(size_t) override;
    virtual void document_did_remove_all_lines() override;
    virtual void document_did_change(AllowCallback = AllowCallback::Yes) override;
    virtual void document_did_set_text(AllowCallback = AllowCallback::Yes) override;
    virtual void document_did_set_cursor(TextPosition const&) override;
    virtual void document_did_update_undo_stack() override;

    // ^Syntax::HighlighterClient
    virtual Vector<TextDocumentSpan> const& spans() const final { return document().spans(); }
    virtual void set_span_at_index(size_t index, TextDocumentSpan span) final { document().set_span_at_index(index, move(span)); }
    virtual Vector<GUI::TextDocumentFoldingRegion>& folding_regions() final { return document().folding_regions(); }
    virtual Vector<GUI::TextDocumentFoldingRegion> const& folding_regions() const final { return document().folding_regions(); }
    virtual void highlighter_did_request_update() final { update(); }
    virtual ByteString highlighter_did_request_text() const final { return text(); }
    virtual GUI::TextDocument& highlighter_did_request_document() final { return document(); }
    virtual GUI::TextPosition highlighter_did_request_cursor() const final { return m_cursor; }

    // ^Clipboard::ClipboardClient
    virtual void clipboard_content_did_change(ByteString const& mime_type) override;

    // ^GUI::AbstractScrollableWidget
    virtual void automatic_scrolling_timer_did_fire() override;

    void create_actions();
    void update_content_size();
    int fixed_glyph_width() const;

    void defer_reflow();
    void undefer_reflow();

    enum UserRequestedAutocomplete {
        No,
        Yes
    };
    void try_show_autocomplete(UserRequestedAutocomplete);
    void try_update_autocomplete(Function<void()> callback = {});
    void force_update_autocomplete(Function<void()> callback = {});
    void hide_autocomplete_if_needed();

    int icon_size() const { return 16; }
    int icon_padding() const { return 2; }

    class ReflowDeferrer {
    public:
        ReflowDeferrer(TextEditor& editor)
            : m_editor(editor)
        {
            m_editor.defer_reflow();
        }
        ~ReflowDeferrer()
        {
            m_editor.undefer_reflow();
        }

    private:
        TextEditor& m_editor;
    };

    int text_width_for_font(auto const& text_view, Gfx::Font const&) const;
    Utf32View substitution_code_point_view(size_t length) const;

    Gfx::IntRect line_content_rect(size_t item_index) const;
    Gfx::IntRect line_widget_rect(size_t line_index) const;
    void delete_selection();
    int content_x_for_position(TextPosition const&) const;
    Gfx::IntRect gutter_rect_in_inner_coordinates() const;
    Gfx::IntRect ruler_rect_in_inner_coordinates() const;
    Gfx::IntRect folding_indicator_rect_in_inner_coordinates() const;
    Gfx::IntRect visible_text_rect_in_inner_coordinates() const;
    void recompute_all_visual_lines();
    void ensure_cursor_is_valid();
    void rehighlight_if_needed();

    size_t visual_line_containing(size_t line_index, size_t column) const;
    void recompute_visual_lines(size_t line_index, Vector<TextDocumentFoldingRegion const&>::Iterator& folded_region_iterator);
    void populate_line_data();

    template<class T, class... Args>
    inline void execute(Args&&... args)
    {
        auto command = make<T>(*m_document, forward<Args>(args)...);
        command->perform_formatting(*this);
        command->execute_from(*this);
        after_execute(*command);
        m_document->add_to_undo_stack(move(command));
    }

    virtual void after_execute(TextDocumentUndoCommand const&) { }
    void on_search_results(GUI::TextRange current, Vector<GUI::TextRange> all_results);

    static constexpr auto search_results_span_collection_index = 1;
    static constexpr auto highlight_selected_text_span_collection_index = 2;

    Type m_type { MultiLine };
    Mode m_mode { Editable };

    TextPosition m_cursor;
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::CenterLeft };
    bool m_cursor_state { true };
    bool m_in_drag_select { false };
    bool m_relative_line_number { false };
    bool m_ruler_visible { false };
    bool m_gutter_visible { false };
    bool m_needs_rehighlight { false };
    bool m_automatic_indentation_enabled { false };
    WrappingMode m_wrapping_mode { WrappingMode::NoWrap };
    bool m_visualize_trailing_whitespace { true };
    bool m_visualize_leading_whitespace { false };
    bool m_cursor_line_highlighting { true };
    size_t m_soft_tab_width { 4 };
    int m_horizontal_content_padding { 3 };
    TextRange m_selection {};

    struct GutterIndicator {
        PaintGutterIndicator draw_indicator;
        OnGutterIndicatorClick on_click;
    };
    Vector<GutterIndicator> m_gutter_indicators;
    unsigned m_most_gutter_indicators_displayed_on_one_line { 0 };

    Optional<u32> m_substitution_code_point;
    mutable OwnPtr<Vector<u32>> m_substitution_string_data; // Used to avoid repeated String construction.

    RefPtr<Menu> m_context_menu;
    RefPtr<Action> m_undo_action;
    RefPtr<Action> m_redo_action;
    RefPtr<Action> m_cut_action;
    RefPtr<Action> m_copy_action;
    RefPtr<Action> m_paste_action;
    RefPtr<Action> m_go_to_line_or_column_action;
    RefPtr<Action> m_select_all_action;
    RefPtr<Action> m_insert_emoji_action;
    Core::ElapsedTimer m_triple_click_timer;
    Vector<NonnullRefPtr<Action>> m_custom_context_menu_actions;

    size_t m_reflow_deferred { 0 };
    bool m_reflow_requested { false };

    bool is_visual_data_up_to_date() const { return !m_reflow_requested; }

    RefPtr<TextDocument> m_document;

    ByteString m_placeholder { "" };

    template<typename Callback>
    void for_each_visual_line(size_t line_index, Callback) const;

    struct LineData {
        Vector<Utf32View> visual_lines;
        Gfx::IntRect visual_rect;
        u32 gutter_indicators { 0 }; // A bitfield of which gutter indicators are present. (1 << GutterIndicatorID)
    };

    Vector<NonnullOwnPtr<LineData>> m_line_data;

    OwnPtr<Syntax::Highlighter> m_highlighter;
    OwnPtr<AutocompleteProvider> m_autocomplete_provider;
    OwnPtr<AutocompleteBox> m_autocomplete_box;
    bool m_should_keep_autocomplete_box { false };
    size_t m_automatic_autocomplete_delay_ms { 800 };
    RefPtr<Core::Timer> m_autocomplete_timer;

    OwnPtr<EditingEngine> m_editing_engine;

    Gfx::IntPoint m_last_mousemove_position;

    RefPtr<Gfx::Bitmap const> m_icon;

    bool m_text_is_secret { false };

    Optional<size_t> m_search_result_index;
    Vector<GUI::TextRange> m_search_results;
    RefPtr<IncrementalSearchBanner> m_search_banner;
};

}
