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
    static NonnullOwnPtr<Task> create(DOM::Document* document, Function<void()> steps)
    {
        return adopt_own(*new Task(document, move(steps)));
    }
    ~Task();

    void execute();

    DOM::Document* document() { return m_document; }
    DOM::Document const* document() const { return m_document; }

private:
    Task(DOM::Document*, Function<void()> steps);

    Function<void()> m_steps;
    RefPtr<DOM::Document> m_document;
};

}
