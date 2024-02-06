/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibWeb/Painting/PaintingCommandExecutorCPU.h>
#include <WebContent/RenderLoopExecutor.h>

#ifdef HAS_ACCELERATED_GRAPHICS
#    include <LibWeb/Painting/PaintingCommandExecutorGPU.h>
#endif

namespace WebContent {

RenderLoopExecutor::RenderLoopExecutor(bool use_gpu_painter)
    : m_main_thread_event_loop(Core::EventLoop::current())
    , m_use_gpu_painter(use_gpu_painter)
{
}

RenderLoopExecutor::~RenderLoopExecutor()
{
    m_exit = true;
    m_paint_task_ready_wake_condition.signal();
}

void RenderLoopExecutor::paint(NonnullOwnPtr<Web::Painting::RecordingPainter> recording_painter, Function<void(i32)> on_completion)
{
    if (!m_backing_stores.back_bitmap)
        return;

    enqueue_paint_task([this, recording_painter = move(recording_painter), on_completion = move(on_completion)]() mutable {
        i32 front_bitmap_id;
        {
            Threading::MutexLocker locker { m_backing_stores_mutex };
            auto& bitmap = *m_backing_stores.back_bitmap;

            if (m_use_gpu_painter) {
#ifdef HAS_ACCELERATED_GRAPHICS
                Web::Painting::PaintingCommandExecutorGPU painting_command_executor(*m_accelerated_graphics_context,
                    bitmap);
                recording_painter->execute(painting_command_executor);
#else
                static bool has_warned_about_configuration = false;

                if (!has_warned_about_configuration) {
                    warnln("\033[31;1mConfigured to use GPU painter, but current platform does not have accelerated graphics\033[0m");
                    has_warned_about_configuration = true;
                }
#endif
            } else {
                Web::Painting::PaintingCommandExecutorCPU painting_command_executor(bitmap);
                recording_painter->execute(painting_command_executor);
            }

            swap(m_backing_stores.front_bitmap, m_backing_stores.back_bitmap);
            swap(m_backing_stores.front_bitmap_id, m_backing_stores.back_bitmap_id);
            front_bitmap_id = m_backing_stores.front_bitmap_id;
        }

        m_main_thread_event_loop.deferred_invoke([on_completion = move(on_completion), front_bitmap_id]() {
            on_completion(front_bitmap_id);
        });

        {
            Threading::MutexLocker locker { m_ready_to_paint_mutex };
            m_ready_to_paint_wake_condition.wait();
        }
    });
}

void RenderLoopExecutor::ready_to_paint()
{
    Threading::MutexLocker locker { m_ready_to_paint_mutex };
    m_ready_to_paint_wake_condition.signal();
}

void RenderLoopExecutor::start()
{
    m_thread = Threading::Thread::construct([this]() mutable -> intptr_t {
#ifdef HAS_ACCELERATED_GRAPHICS
        if (m_use_gpu_painter) {
            auto context = AccelGfx::Context::create();
            if (context.is_error()) {
                dbgln("Failed to create AccelGfx context: {}", context.error());
                VERIFY_NOT_REACHED();
            }
            m_accelerated_graphics_context = context.release_value();
        }
#endif
        repaint_loop();
        return (intptr_t)0;
    },
        "RenderLoopExecutor"sv);
    m_thread->start();
}

void RenderLoopExecutor::add_backing_store(i32 front_bitmap_id, Gfx::ShareableBitmap const& front_bitmap, i32 back_bitmap_id, Gfx::ShareableBitmap const& back_bitmap)
{
    Threading::MutexLocker locker { m_backing_stores_mutex };
    m_backing_stores.front_bitmap_id = front_bitmap_id;
    m_backing_stores.back_bitmap_id = back_bitmap_id;
    m_backing_stores.front_bitmap = *const_cast<Gfx::ShareableBitmap&>(front_bitmap).bitmap();
    m_backing_stores.back_bitmap = *const_cast<Gfx::ShareableBitmap&>(back_bitmap).bitmap();
}

void RenderLoopExecutor::enqueue_paint_task(Function<void()>&& task)
{
    Threading::MutexLocker locker { m_paint_task_mutex };
    m_paint_task.emplace(forward<Function<void()>>(task));
    m_paint_task_ready_wake_condition.signal();
}

void RenderLoopExecutor::repaint_loop()
{
    while (true) {
        auto task = [this]() -> Function<void()> {
            Threading::MutexLocker locker { m_paint_task_mutex };
            while (!m_paint_task.has_value() && !m_exit) {
                m_paint_task_ready_wake_condition.wait();
            }
            if (m_exit)
                return nullptr;
            return m_paint_task.release_value();
        }();

        if (!task) {
            VERIFY(m_exit);
            break;
        }
        task();
    }
}

}
