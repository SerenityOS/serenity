/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#history-handling-behavior
enum class HistoryHandlingBehavior {
    Default,     // FIXME: This is no longer part of the spec. Remove.
    EntryUpdate, // FIXME: This is no longer part of the spec. Remove.
    Reload,      // FIXME: This is no longer part of the spec. Remove.
    Push,
    Replace,
};

}
