/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageProcessor.h"

namespace PixelPaint {

FilterApplicationCommand::FilterApplicationCommand(NonnullRefPtr<Filter> filter, NonnullRefPtr<Layer> target_layer)
    : m_filter(move(filter))
    , m_target_layer(move(target_layer))
{
}

void FilterApplicationCommand::execute()
{
    m_filter->apply(m_target_layer->content_bitmap(), m_target_layer->content_bitmap());
    m_filter->m_editor->gui_event_loop().deferred_invoke([strong_this = NonnullRefPtr(*this)]() {
        // HACK: we can't tell strong_this to not be const
        (*const_cast<NonnullRefPtr<Layer>*>(&strong_this->m_target_layer))->did_modify_bitmap(strong_this->m_target_layer->rect());
        strong_this->m_filter->m_editor->did_complete_action();
    });
}

static Singleton<ImageProcessor> s_image_processor;

ImageProcessor::ImageProcessor()
    : m_command_queue(MUST(Queue::try_create()))
    , m_processor_thread(Threading::Thread::construct([this]() {
        while (true) {
            if (auto next_command = m_command_queue.try_dequeue(); !next_command.is_error()) {
                next_command.value()->execute();
            } else {
                Threading::MutexLocker locker { m_wakeup_mutex };
                m_wakeup_variable.wait_while([this]() { return m_command_queue.weak_used() == 0; });
            }
        }
        return 0;
    },
          "Image Processor"sv))
    , m_wakeup_variable(m_wakeup_mutex)
{
}

ImageProcessor* ImageProcessor::the()
{
    return s_image_processor;
}

ErrorOr<void> ImageProcessor::enqueue_command(NonnullRefPtr<ImageProcessingCommand> command)
{
    if (auto queue_status = m_command_queue.try_enqueue(move(command)); queue_status.is_error())
        return ENOSPC;

    if (!m_processor_thread->is_started()) {
        m_processor_thread->start();
        m_processor_thread->detach();
    }

    m_wakeup_mutex.lock();
    m_wakeup_variable.signal();
    m_wakeup_mutex.unlock();

    return {};
}

}
