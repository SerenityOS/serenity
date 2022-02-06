/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class Task {
public:
    // https://html.spec.whatwg.org/multipage/webappapis.html#generic-task-sources
    enum class Source {
        Unspecified,
        DOMManipulation,
        UserInteraction,
        Networking,
        HistoryTraversal,
        IdleTask,
        PostedMessage,
        Microtask,
        TimerTask,
        JavaScriptEngine,
    };

    static NonnullOwnPtr<Task> create(Source source, DOM::Document* document, Function<void()> steps)
    {
        return adopt_own(*new Task(source, document, move(steps)));
    }
    ~Task();

    Source source() const { return m_source; }
    void execute();

    DOM::Document* document() { return m_document; }
    DOM::Document const* document() const { return m_document; }

    bool is_runnable() const;

private:
    Task(Source, DOM::Document*, Function<void()> steps);

    Source m_source { Source::Unspecified };
    Function<void()> m_steps;
    RefPtr<DOM::Document> m_document;
};

}
