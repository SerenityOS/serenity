/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filters/Filter.h"
#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Singleton.h>
#include <LibCore/SharedCircularQueue.h>
#include <LibThreading/ConditionVariable.h>
#include <LibThreading/Thread.h>

namespace PixelPaint {

class ImageProcessingCommand : public RefCounted<ImageProcessingCommand> {
public:
    virtual void execute() = 0;
    virtual ~ImageProcessingCommand() = default;
};

// A command applying a filter from a source to a target bitmap.
class FilterApplicationCommand : public ImageProcessingCommand {
public:
    FilterApplicationCommand(NonnullRefPtr<Filter>, NonnullRefPtr<Layer>);

    virtual void execute() override;
    virtual ~FilterApplicationCommand() = default;

private:
    NonnullRefPtr<Filter> m_filter;
    NonnullRefPtr<Layer> m_target_layer;
};

// A command based on a custom user function.
class FunctionCommand : public ImageProcessingCommand {
public:
    FunctionCommand(Function<void()> function)
        : m_function(move(function))
    {
    }

    virtual void execute() override { m_function(); }

private:
    Function<void()> m_function;
};

// A utility class that allows various PixelPaint systems to execute image processing commands asynchronously on another thread.
class ImageProcessor final {
    friend struct AK::SingletonInstanceCreator<ImageProcessor>;

public:
    static ImageProcessor* the();

    ErrorOr<void> enqueue_command(NonnullRefPtr<ImageProcessingCommand> command);

private:
    ImageProcessor();

    // Only the memory in the queue is in shared memory, i.e. the smart pointers themselves.
    // The actual data will remain in normal memory, but for this application we're not using multiple processes so it's fine.
    using Queue = Core::SharedSingleProducerCircularQueue<RefPtr<ImageProcessingCommand>>;
    Queue m_command_queue;

    NonnullRefPtr<Threading::Thread> m_processor_thread;
    Threading::Mutex m_wakeup_mutex {};
    Threading::ConditionVariable m_wakeup_variable;
};

}
