/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/Browser/URLBox.h>
#include <LibGfx/Palette.h>
#include <LibGfx/TextAttributes.h>
#include <LibURL/URL.h>
#include <LibWebView/URL.h>

namespace Browser {

URLBox::URLBox()
{
    set_auto_focusable(false);

    on_change = [this] {
        highlight_url();
    };
}

void URLBox::focusout_event(GUI::FocusEvent& event)
{
    set_focus_transition(true);

    highlight_url();
    GUI::TextBox::focusout_event(event);
}

void URLBox::focusin_event(GUI::FocusEvent& event)
{
    highlight_url();
    GUI::TextBox::focusin_event(event);
}

void URLBox::mousedown_event(GUI::MouseEvent& event)
{
    if (is_displayonly())
        return;

    if (event.button() != GUI::MouseButton::Primary)
        return;

    if (is_focus_transition()) {
        GUI::TextBox::select_current_line();

        set_focus_transition(false);
    } else {
        GUI::TextBox::mousedown_event(event);
    }
}

void URLBox::highlight_url()
{
    Vector<GUI::TextDocumentSpan> spans;

    if (auto url_parts = WebView::break_url_into_parts(text()); url_parts.has_value()) {
        Gfx::TextAttributes dark_attributes;
        dark_attributes.color = palette().color(Gfx::ColorRole::PlaceholderText);

        Gfx::TextAttributes highlight_attributes;
        highlight_attributes.color = palette().color(Gfx::ColorRole::BaseText);

        spans.append({
            { { 0, 0 }, { 0, url_parts->scheme_and_subdomain.length() } },
            dark_attributes,
        });

        spans.append({
            { { 0, url_parts->scheme_and_subdomain.length() }, { 0, url_parts->scheme_and_subdomain.length() + url_parts->effective_tld_plus_one.length() } },
            highlight_attributes,
        });

        spans.append({
            {
                { 0, url_parts->scheme_and_subdomain.length() + url_parts->effective_tld_plus_one.length() },
                { 0, url_parts->scheme_and_subdomain.length() + url_parts->effective_tld_plus_one.length() + url_parts->remainder.length() },
            },
            dark_attributes,
        });
    }

    document().set_spans(0, move(spans));
    update();
}

}
