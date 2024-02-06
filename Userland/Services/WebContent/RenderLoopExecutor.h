/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Forward.h>
#include <LibThreading/ConditionVariable.h>
#include <LibThreading/MutexProtected.h>
#include <LibThreading/Thread.h>
#include <LibWeb/Painting/RecordingPainter.h>
#include <WebContent/Forward.h>

#ifdef HAS_ACCELERATED_GRAPHICS
#    include <LibAccelGfx/Context.h>
#endif

namespace WebContent {

class RenderLoopExecutor {
    AK_MAKE_NONCOPYABLE(RenderLoopExecutor);
    AK_MAKE_NONMOVABLE(RenderLoopExecutor);

public:
    struct BackingStores {
        i32 front_bitmap_id { -1 };
        i32 back_bitmap_id { -1 };
        RefPtr<Gfx::Bitmap> front_bitmap;
        RefPtr<Gfx::Bitmap> back_bitmap;
    };

    void paint(NonnullOwnPtr<Web::Painting::RecordingPainter> recording_painter, Function<void(i32)> on_completion);
    void start();
    void add_backing_store(i32 front_bitmap_id, Gfx::ShareableBitmap const& front_bitmap, i32 back_bitmap_id, Gfx::ShareableBitmap const& back_bitmap);
    void ready_to_paint();

    RenderLoopExecutor(bool use_gpu_painter);
    ~RenderLoopExecutor();

private:
    void enqueue_paint_task(Function<void()>&& task);
    void repaint_loop();

    RefPtr<Threading::Thread> m_thread;
    Atomic<bool> m_exit { false };

    Optional<Function<void()>> m_paint_task;
    Threading::Mutex m_paint_task_mutex;
    Threading::ConditionVariable m_paint_task_ready_wake_condition { m_paint_task_mutex };

    Threading::Mutex m_ready_to_paint_mutex;
    Threading::ConditionVariable m_ready_to_paint_wake_condition { m_ready_to_paint_mutex };

    Core::EventLoop& m_main_thread_event_loop;

    BackingStores m_backing_stores;
    Threading::Mutex m_backing_stores_mutex;

    bool m_use_gpu_painter { false };

#ifdef HAS_ACCELERATED_GRAPHICS
    OwnPtr<AccelGfx::Context> m_accelerated_graphics_context;
#endif
};

}
