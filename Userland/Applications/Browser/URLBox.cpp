/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <Applications/Browser/URLBox.h>
#include <LibGfx/Palette.h>
#include <LibGfx/TextAttributes.h>

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
    auto url = URL::create_with_url_or_path(text());
    Vector<GUI::TextDocumentSpan> spans;

    if (url.is_valid() && !is_focused()) {
        if (url.scheme() == "http" || url.scheme() == "https" || url.scheme() == "gemini") {
            auto serialized_host = url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string();
            auto host_start = url.scheme().bytes_as_string_view().length() + 3;
            auto host_length = serialized_host.length();

            // FIXME: Maybe add a generator to use https://publicsuffix.org/list/public_suffix_list.dat
            //        for now just highlight the whole host

            Gfx::TextAttributes default_format;
            default_format.color = palette().color(Gfx::ColorRole::PlaceholderText);
            spans.append({
                { { 0, 0 }, { 0, host_start } },
                default_format,
            });

            Gfx::TextAttributes host_format;
            host_format.color = palette().color(Gfx::ColorRole::BaseText);
            spans.append({
                { { 0, host_start }, { 0, host_start + host_length } },
                host_format,
            });

            spans.append({
                { { 0, host_start + host_length }, { 0, text().length() } },
                default_format,
            });
        } else if (url.scheme() == "file") {
            Gfx::TextAttributes scheme_format;
            scheme_format.color = palette().color(Gfx::ColorRole::PlaceholderText);
            spans.append({
                { { 0, 0 }, { 0, url.scheme().bytes_as_string_view().length() + 3 } },
                scheme_format,
            });
        }
    }

    document().set_spans(0, move(spans));
    update();
}

}
