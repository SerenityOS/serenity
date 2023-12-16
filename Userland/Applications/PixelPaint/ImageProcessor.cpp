/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageProcessor.h"
#include "Layer.h"

namespace PixelPaint {

FilterApplicationCommand::FilterApplicationCommand(NonnullRefPtr<Filter> filter, NonnullRefPtr<Layer> target_layer)
    : m_filter(move(filter))
    , m_target_layer(move(target_layer))
{
}

void FilterApplicationCommand::execute()
{
    if (m_target_layer->mask_type() == Layer::MaskType::EditingMask && m_target_layer->edit_mode() == Layer::EditMode::Content) {
        auto unchanged_source = m_target_layer->get_scratch_edited_bitmap().clone().release_value_but_fixme_should_propagate_errors();

        m_filter->apply(m_target_layer->get_scratch_edited_bitmap(), m_target_layer->get_scratch_edited_bitmap());

        for (int y = 0; y < m_target_layer->content_bitmap().height(); y++)
            for (int x = 0; x < m_target_layer->content_bitmap().width(); x++)
                m_target_layer->content_bitmap().set_pixel(x, y, m_target_layer->modify_pixel_with_editing_mask(x, y, m_target_layer->content_bitmap().get_pixel(x, y), unchanged_source->get_pixel(x, y)));
    } else {
        m_filter->apply(m_target_layer->get_scratch_edited_bitmap(), m_target_layer->get_scratch_edited_bitmap());
    }

    m_filter->m_editor->gui_event_loop().deferred_invoke([strong_this = NonnullRefPtr(*this)]() {
        strong_this->m_target_layer->did_modify_bitmap(strong_this->m_target_layer->rect());
        strong_this->m_filter->m_editor->did_complete_action(ByteString::formatted("Filter {}", strong_this->m_filter->filter_name()));
    });
}

static Singleton<ImageProcessor> s_image_processor;

ImageProcessor::ImageProcessor()
    : m_command_queue(MUST(Queue::create()))
    , m_processor_thread(Threading::Thread::construct([this]() {
        while (true) {
            if (auto next_command = m_command_queue.dequeue(); !next_command.is_error()) {
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
    if (auto queue_status = m_command_queue.enqueue(move(command)); queue_status.is_error())
        return ENOSPC;

    if (!m_processor_thread->is_started()) {
        m_processor_thread->start();
        m_processor_thread->detach();
    }

    Threading::MutexLocker const locker(m_wakeup_mutex);
    m_wakeup_variable.signal();
    return {};
}

}
