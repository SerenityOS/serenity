/*
 * Copyright (c) 2022, Ben Abraham <ben.d.abraham@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibJS/Console.h>

namespace Web::HTML {

// NOTE: Temporary class to handle console messages from inside Workers

class WorkerDebugConsoleClient final
    : public JS::ConsoleClient
    , public RefCounted<WorkerDebugConsoleClient>
    , public Weakable<WorkerDebugConsoleClient> {
public:
    WorkerDebugConsoleClient(JS::Console& console);

    virtual void clear() override;
    virtual void end_group() override;
    virtual JS::ThrowCompletionOr<JS::Value> printer(JS::Console::LogLevel log_level, PrinterArguments arguments) override;

private:
    int m_group_stack_depth { 0 };
};

} // namespace Web::HTML
