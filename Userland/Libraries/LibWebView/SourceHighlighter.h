/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>

namespace WebView {

String highlight_source(URL::URL const&, StringView);

constexpr inline StringView HTML_HIGHLIGHTER_STYLE = R"~~~(
    .html {
        font-size: 10pt;
        font-family: Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
    }

    .tag {
        font-weight: 600;
    }

    @media (prefers-color-scheme: dark) {
        /* FIXME: We should be able to remove the HTML style when "color-scheme" is supported */
        html {
            background-color: rgb(30, 30, 30);
            color: white;
        }
        .comment {
            color: lightgreen;
        }
        .tag {
            color: orangered;
        }
        .attribute-name {
            color: orange;
        }
        .attribute-value {
            color: deepskyblue;
        }
        .internal {
            color: darkgrey;
        }
    }

    @media (prefers-color-scheme: light) {
        .comment {
            color: green;
        }
        .tag {
            color: red;
        }
        .attribute-name {
            color: darkorange;
        }
        .attribute-value {
            color: blue;
        }
        .internal {
            color: dimgray;
        }
    }
)~~~"sv;

}
