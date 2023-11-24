/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWebView/InspectorClient.h>
#include <LibWebView/SourceHighlighter.h>

namespace WebView {

static ErrorOr<JsonValue> parse_json_tree(StringView json)
{
    auto parsed_tree = TRY(JsonValue::from_string(json));
    if (!parsed_tree.is_object())
        return Error::from_string_literal("Expected tree to be a JSON object");

    return parsed_tree;
}

InspectorClient::InspectorClient(ViewImplementation& content_web_view, ViewImplementation& inspector_web_view)
    : m_content_web_view(content_web_view)
    , m_inspector_web_view(inspector_web_view)
{
    m_content_web_view.on_received_dom_tree = [this](auto const& dom_tree) {
        if (auto result = parse_json_tree(dom_tree); result.is_error())
            dbgln("Failed to load DOM tree: {}", result.error());
        else
            m_dom_tree = result.release_value();

        maybe_load_inspector();
    };

    m_content_web_view.on_received_accessibility_tree = [this](auto const& dom_tree) {
        if (auto result = parse_json_tree(dom_tree); result.is_error())
            dbgln("Failed to load accessibility tree: {}", result.error());
        else
            m_accessibility_tree = result.release_value();

        maybe_load_inspector();
    };

    m_inspector_web_view.enable_inspector_prototype();
    m_inspector_web_view.use_native_user_style_sheet();

    m_inspector_web_view.on_inspector_loaded = [this]() {
        m_inspector_loaded = true;

        if (m_pending_selection.has_value())
            select_node(m_pending_selection.release_value());
        else
            select_default_node();
    };

    m_inspector_web_view.on_inspector_selected_dom_node = [this](auto node_id, auto const& pseudo_element) {
        auto inspected_node_properties = m_content_web_view.inspect_dom_node(node_id, pseudo_element);

        if (on_dom_node_properties_received)
            on_dom_node_properties_received(move(inspected_node_properties));
    };

    inspect();
}

InspectorClient::~InspectorClient()
{
    m_content_web_view.on_received_dom_tree = nullptr;
    m_content_web_view.on_received_accessibility_tree = nullptr;
}

void InspectorClient::inspect()
{
    m_content_web_view.inspect_dom_tree();
    m_content_web_view.inspect_accessibility_tree();
}

void InspectorClient::reset()
{
    m_dom_tree.clear();
    m_accessibility_tree.clear();

    m_body_node_id.clear();
    m_pending_selection.clear();

    m_inspector_loaded = false;
}

void InspectorClient::select_hovered_node()
{
    auto hovered_node_id = m_content_web_view.get_hovered_node_id();
    select_node(hovered_node_id);
}

void InspectorClient::select_default_node()
{
    if (m_body_node_id.has_value())
        select_node(*m_body_node_id);
}

void InspectorClient::clear_selection()
{
    m_content_web_view.clear_inspected_dom_node();

    static constexpr auto script = "inspector.clearInspectedDOMNode();"sv;
    m_inspector_web_view.run_javascript(script);
}

void InspectorClient::select_node(i32 node_id)
{
    if (!m_inspector_loaded) {
        m_pending_selection = node_id;
        return;
    }

    auto script = MUST(String::formatted("inspector.inspectDOMNodeID({});", node_id));
    m_inspector_web_view.run_javascript(script);
}

void InspectorClient::maybe_load_inspector()
{
    if (!m_dom_tree.has_value() || !m_accessibility_tree.has_value())
        return;

    StringBuilder builder;

    builder.append(R"~~~(
<!DOCTYPE html>
<html>
<head>
    <meta name="color-scheme" content="dark light">
    <style type="text/css">
)~~~"sv);

    builder.append(HTML_HIGHLIGHTER_STYLE);

    builder.append(R"~~~(
        body {
            margin: 0;
        }

        .tab-controls-container {
            position: fixed;
            width: 100%;
            top: 0;

            padding: 4px;

            display: flex;
            align-items: center;
            justify-content: center;

            z-index: 100;
        }

        .tab-controls {
            overflow: hidden;
            flex-shrink: 0;
        }

        .tab-controls button {
            font-size: 12px;
            font-weight: 600;
            font-family: system-ui, sans-serif;

            float: left;
            border: none;
            outline: none;
            cursor: pointer;

            padding: 4px 8px;
        }

        .tab-controls :first-child {
            border-radius: 0.5rem 0 0 0.5rem;
        }

        .tab-controls :last-child {
            border-radius: 0 0.5rem 0.5rem 0;
        }

        .tab-content-container {
            margin-top: 30px;
        }

        .tab-content {
            display: none;
            border-radius: 0.5rem;

            margin-top: 30px;
            padding: 6px 12px 6px 12px;
        }

        @media (prefers-color-scheme: dark) {
            html {
                background-color: rgb(23, 23, 23);
            }

            .tab-controls-container {
                background-color: rgb(57, 57, 57);
            }

            .tab-controls button {
                color: white;
                background-color: rgb(43, 42, 50);
            }

            .tab-controls button.active {
                background-color: rgb(22 100 219);
            }

            .tab-controls button + button {
                border-left: 1px solid rgb(96, 96, 96);
            }
        }

        @media (prefers-color-scheme: light) {
            .tab-controls-container {
                background-color: rgb(229, 229, 229);
            }

            .tab-controls button {
                color: black;
                background-color: white;
            }

            .tab-controls button.active {
                color: white;
                background-color: rgb(28, 138, 255);
            }

            .tab-controls button + button {
                border-left: 1px solid rgb(242, 242, 242);
            }
        }

        details > :not(:first-child) {
            display: list-item;
            list-style: none inside;
            margin-left: 1em;
        }

        .hoverable {
            display: block;
            padding: 1px;
        }

        @media (prefers-color-scheme: dark) {
            .hoverable:hover {
                background-color: #31383e;
            }

            .selected {
                border: 1px dashed cyan;
                padding: 0;
            }
        }

        @media (prefers-color-scheme: light) {
            .hoverable:hover {
                background-color: rgb(236, 236, 236);
            }

            .selected {
                border: 1px dashed blue;
                padding: 0;
            }
        }
    </style>
</head>
<body>
    <div class="tab-controls-container">
        <div class="tab-controls">
            <button id="dom-tree-button" onclick="selectTab(this, 'dom-tree')">DOM Tree</button>
            <button id="accessibility-tree-button" onclick="selectTab(this, 'accessibility-tree')">Accessibility Tree</button>
        </div>
    </div>

    <div class="tab-content-container">
        <div id="dom-tree" class="tab-content html">
)~~~"sv);

    generate_dom_tree(builder);

    builder.append(R"~~~(
        </div>
        <div id="accessibility-tree" class="tab-content">
)~~~"sv);

    generate_accessibility_tree(builder);

    builder.append(R"~~~(
        </div>
    </div>

    <script type="text/javascript">
        let selectedTab = null;
        let selectedTabButton = null;
        let selectedDOMNode = null;

        const selectTab = (tabButton, tabID) => {
            let tab = document.getElementById(tabID);

            if (selectedTab === tab) {
                return;
            }
            if (selectedTab !== null) {
                selectedTab.style.display = "none";
                selectedTabButton.classList.remove("active");
            }

            tab.style.display = "block";
            selectedTab = tab;

            tabButton.classList.add("active");
            selectedTabButton = tabButton;
        };

        let initialTabButton = document.getElementById("dom-tree-button");
        selectTab(initialTabButton, "dom-tree");

        const scrollToElement = (element) => {
            // Include an offset to prevent the element being placed behind the fixed `tab-controls` header.
            const offset = 45;

            let position = element.getBoundingClientRect().top;
            position += window.pageYOffset - offset;

            window.scrollTo(0, position);
        }

        inspector.inspectDOMNodeID = nodeID => {
            let domNodes = document.querySelectorAll(`[data-id="${nodeID}"]`);
            if (domNodes.length !== 1) {
                return;
            }

            for (let domNode = domNodes[0]; domNode; domNode = domNode.parentNode) {
                if (domNode.tagName === "DETAILS") {
                    domNode.setAttribute("open", "");
                }
            }

            inspectDOMNode(domNodes[0]);
            scrollToElement(selectedDOMNode);
        };

        inspector.clearInspectedDOMNode = () => {
            if (selectedDOMNode !== null) {
                selectedDOMNode.classList.remove("selected");
                selectedDOMNode = null;
            }
        };


        const inspectDOMNode = domNode => {
            if (selectedDOMNode === domNode) {
                return;
            }

            inspector.clearInspectedDOMNode();

            domNode.classList.add("selected");
            selectedDOMNode = domNode;

            inspector.inspectDOMNode(domNode.dataset.id, domNode.dataset.pseudoElement);
        };

        document.addEventListener("DOMContentLoaded", () => {
            let domTree = document.getElementById("dom-tree");
            let domNodes = domTree.getElementsByClassName("hoverable");

            for (let domNode of domNodes) {
                domNode.addEventListener("click", event => {
                    inspectDOMNode(domNode);
                    event.preventDefault();
                });
            }

            inspector.inspectorLoaded();
        });
    </script>
</body>
</html>
)~~~"sv);

    m_inspector_web_view.load_html(builder.string_view());
}

template<typename Generator>
static void generate_tree(StringBuilder& builder, JsonObject const& node, Generator&& generator)
{
    if (auto children = node.get_array("children"sv); children.has_value() && !children->is_empty()) {
        auto name = node.get_deprecated_string("name"sv).value_or({});
        builder.append("<details>"sv);

        builder.append("<summary>"sv);
        generator(node);
        builder.append("</summary>"sv);

        children->for_each([&](auto const& child) {
            builder.append("<div>"sv);
            generate_tree(builder, child.as_object(), generator);
            builder.append("</div>"sv);
        });

        builder.append("</details>"sv);
    } else {
        generator(node);
    }
}

void InspectorClient::generate_dom_tree(StringBuilder& builder)
{
    generate_tree(builder, m_dom_tree->as_object(), [&](JsonObject const& node) {
        auto type = node.get_deprecated_string("type"sv).value_or("unknown"sv);
        auto name = node.get_deprecated_string("name"sv).value_or({});

        StringBuilder data_attributes;
        auto append_data_attribute = [&](auto name, auto value) {
            if (!data_attributes.is_empty())
                data_attributes.append(' ');
            data_attributes.appendff("data-{}=\"{}\"", name, value);
        };

        if (auto pseudo_element = node.get_integer<i32>("pseudo-element"sv); pseudo_element.has_value()) {
            append_data_attribute("id"sv, node.get_integer<i32>("parent-id"sv).value());
            append_data_attribute("pseudo-element"sv, *pseudo_element);
        } else {
            append_data_attribute("id"sv, node.get_integer<i32>("id"sv).value());
        }

        if (type == "text"sv) {
            auto deprecated_text = node.get_deprecated_string("text"sv).release_value();
            deprecated_text = escape_html_entities(deprecated_text);

            if (auto text = MUST(Web::Infra::strip_and_collapse_whitespace(deprecated_text)); text.is_empty()) {
                builder.appendff("<span class=\"hoverable internal\" {}>", data_attributes.string_view());
                builder.append(name);
                builder.append("</span>"sv);
            } else {
                builder.appendff("<span class=\"hoverable\" {}>", data_attributes.string_view());
                builder.append(text);
                builder.append("</span>"sv);
            }

            return;
        }

        if (type == "comment"sv) {
            auto comment = node.get_deprecated_string("data"sv).release_value();
            comment = escape_html_entities(comment);

            builder.appendff("<span class=\"hoverable comment\" {}>", data_attributes.string_view());
            builder.appendff("&lt;!--{}--&gt;", comment);
            builder.append("</span>"sv);
            return;
        }

        if (type == "shadow-root"sv) {
            auto mode = node.get_deprecated_string("mode"sv).release_value();

            builder.appendff("<span class=\"hoverable internal\" {}>", data_attributes.string_view());
            builder.appendff("{} ({})", name, mode);
            builder.append("</span>"sv);
            return;
        }

        if (type != "element"sv) {
            builder.appendff("<span class=\"hoverable internal\" {}>", data_attributes.string_view());
            builder.appendff(name);
            builder.append("</span>"sv);
            return;
        }

        if (name.equals_ignoring_ascii_case("BODY"sv))
            m_body_node_id = node.get_integer<i32>("id"sv).value();

        builder.appendff("<span class=\"hoverable\" {}>", data_attributes.string_view());
        builder.append("<span>&lt;</span>"sv);
        builder.appendff("<span class=\"tag\">{}</span>", name.to_lowercase());

        if (auto attributes = node.get_object("attributes"sv); attributes.has_value()) {
            attributes->for_each_member([&builder](auto const& name, auto const& value) {
                builder.append("&nbsp;"sv);
                builder.appendff("<span class=\"attribute-name\">{}</span>", name);
                builder.append('=');
                builder.appendff("<span class=\"attribute-value\">\"{}\"</span>", value);
            });
        }

        builder.append("<span>&gt;</span>"sv);
        builder.append("</span>"sv);
    });
}

void InspectorClient::generate_accessibility_tree(StringBuilder& builder)
{
    generate_tree(builder, m_accessibility_tree->as_object(), [&](JsonObject const& node) {
        auto type = node.get_deprecated_string("type"sv).value_or("unknown"sv);
        auto role = node.get_deprecated_string("role"sv).value_or({});

        if (type == "text"sv) {
            auto text = node.get_deprecated_string("text"sv).release_value();
            text = escape_html_entities(text);

            builder.appendff("<span class=\"hoverable\">");
            builder.append(MUST(Web::Infra::strip_and_collapse_whitespace(text)));
            builder.append("</span>"sv);
            return;
        }

        if (type != "element"sv) {
            builder.appendff("<span class=\"hoverable internal\">");
            builder.appendff(role.to_lowercase());
            builder.append("</span>"sv);
            return;
        }

        auto name = node.get_deprecated_string("name"sv).value_or({});
        auto description = node.get_deprecated_string("description"sv).value_or({});

        builder.appendff("<span class=\"hoverable\">");
        builder.append(role.to_lowercase());
        builder.appendff(" name: \"{}\", description: \"{}\"", name, description);
        builder.append("</span>"sv);
    });
}

}
