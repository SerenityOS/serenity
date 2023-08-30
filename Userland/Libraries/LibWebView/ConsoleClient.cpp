/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/MarkupGenerator.h>
#include <LibWebView/ConsoleClient.h>
#include <LibWebView/ViewImplementation.h>

namespace WebView {

static constexpr auto CONSOLE_HTML = "data:text/html,<html style=\"font: 10pt monospace;\"></html>"sv;

// FIXME: It should be sufficient to scrollTo a y value of document.documentElement.offsetHeight,
//        but due to an unknown bug offsetHeight seems to not be properly updated after spamming
//        a lot of document changes.
//
// The setTimeout makes the scrollTo async and allows the DOM to be updated.
static constexpr auto SCROLL_TO_BOTTOM = "setTimeout(function() { window.scrollTo(0, 1_000_000_000); }, 0);"sv;

ConsoleClient::ConsoleClient(ViewImplementation& content_web_view, ViewImplementation& console_web_view)
    : m_content_web_view(content_web_view)
    , m_console_web_view(console_web_view)
{
    m_content_web_view.on_received_console_message = [this](auto message_index) {
        handle_console_message(message_index);
    };

    m_content_web_view.on_received_console_messages = [this](auto start_index, auto const& message_types, auto const& messages) {
        handle_console_messages(start_index, message_types, messages);
    };

    // Wait until our output WebView is loaded, and then request any messages that occurred before we existed.
    m_console_web_view.on_load_finish = [this](auto const&) {
        m_content_web_view.js_console_request_messages(0);
    };

    m_console_web_view.use_native_user_style_sheet();
    m_console_web_view.load(CONSOLE_HTML);
}

ConsoleClient::~ConsoleClient()
{
    m_content_web_view.on_received_console_message = nullptr;
    m_content_web_view.on_received_console_messages = nullptr;
}

void ConsoleClient::execute(String script)
{
    print_source(script);
    m_content_web_view.js_console_input(script.to_deprecated_string());

    if (m_history.is_empty() || m_history.last() != script) {
        m_history.append(move(script));
        m_history_index = m_history.size();
    }
}

Optional<String> ConsoleClient::previous_history_item()
{
    if (m_history_index == 0)
        return {};

    --m_history_index;
    return m_history.at(m_history_index);
}

Optional<String> ConsoleClient::next_history_item()
{
    if (m_history.is_empty())
        return {};

    auto last_index = m_history.size() - 1;

    if (m_history_index < last_index) {
        ++m_history_index;
        return m_history.at(m_history_index);
    }

    if (m_history_index == last_index) {
        ++m_history_index;
        return String {};
    }

    return {};
}

void ConsoleClient::clear()
{
    m_console_web_view.run_javascript("document.body.innerHTML = \"\";"sv);
    m_group_stack.clear();
}

void ConsoleClient::reset()
{
    clear();

    m_highest_notified_message_index = -1;
    m_highest_received_message_index = -1;
    m_waiting_for_messages = false;
}

void ConsoleClient::handle_console_message(i32 message_index)
{
    if (message_index <= m_highest_received_message_index) {
        dbgln("Notified about console message we already have");
        return;
    }
    if (message_index <= m_highest_notified_message_index) {
        dbgln("Notified about console message we're already aware of");
        return;
    }

    m_highest_notified_message_index = message_index;

    if (!m_waiting_for_messages)
        request_console_messages();
}

void ConsoleClient::handle_console_messages(i32 start_index, ReadonlySpan<DeprecatedString> message_types, ReadonlySpan<DeprecatedString> messages)
{
    auto end_index = start_index + static_cast<i32>(message_types.size()) - 1;
    if (end_index <= m_highest_received_message_index) {
        dbgln("Received old console messages");
        return;
    }

    for (size_t i = 0; i < message_types.size(); ++i) {
        auto const& type = message_types[i];
        auto const& message = messages[i];

        if (type == "html"sv)
            print_html(message);
        else if (type == "clear"sv)
            clear();
        else if (type == "group"sv)
            begin_group(message, true);
        else if (type == "groupCollapsed"sv)
            begin_group(message, false);
        else if (type == "groupEnd"sv)
            end_group();
        else
            VERIFY_NOT_REACHED();
    }

    m_highest_received_message_index = end_index;
    m_waiting_for_messages = false;

    if (m_highest_received_message_index < m_highest_notified_message_index)
        request_console_messages();
}

void ConsoleClient::print_source(StringView source)
{
    StringBuilder builder;

    builder.append("<span class=\"repl-indicator\">&gt; </span>"sv);
    builder.append(MUST(JS::MarkupGenerator::html_from_source(source)));

    print_html(builder.string_view());
}

void ConsoleClient::print_html(StringView html)
{
    StringBuilder builder;

    if (m_group_stack.is_empty())
        builder.append("var parentGroup = document.body;"sv);
    else
        builder.appendff("var parentGroup = document.getElementById(\"group_{}\");", m_group_stack.last().id);

    builder.append(R"~~~(
        var p = document.createElement("p");
        p.innerHTML = ")~~~"sv);
    builder.append_escaped_for_json(html);
    builder.append(R"~~~("
        parentGroup.appendChild(p);
)~~~"sv);

    builder.append(SCROLL_TO_BOTTOM);

    m_console_web_view.run_javascript(builder.string_view());
}

void ConsoleClient::request_console_messages()
{
    VERIFY(!m_waiting_for_messages);

    m_content_web_view.js_console_request_messages(m_highest_received_message_index + 1);
    m_waiting_for_messages = true;
}

void ConsoleClient::begin_group(StringView label, bool start_expanded)
{
    StringBuilder builder;

    if (m_group_stack.is_empty())
        builder.append("var parentGroup = document.body;"sv);
    else
        builder.appendff("var parentGroup = document.getElementById(\"group_{}\");", m_group_stack.last().id);

    Group group;
    group.id = m_next_group_id++;
    group.label = label;

    builder.appendff(R"~~~(
        var group = document.createElement("details");
        group.id = "group_{}";
        var label = document.createElement("summary");
        label.innerHTML = ")~~~",
        group.id);
    builder.append_escaped_for_json(label);
    builder.append(R"~~~(";
        group.appendChild(label);
        parentGroup.appendChild(group);
)~~~"sv);

    if (start_expanded)
        builder.append("group.open = true;"sv);

    builder.append(SCROLL_TO_BOTTOM);

    m_console_web_view.run_javascript(builder.string_view());
    m_group_stack.append(group);
}

void ConsoleClient::end_group()
{
    m_group_stack.take_last();
}

}
