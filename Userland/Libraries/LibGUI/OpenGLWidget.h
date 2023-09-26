/*
 * Copyright (c) 2018-2023, Glenford Williams <hey@glenfordwilliams.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGL/GL/gl.h>
#include <LibGL/GLContext.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

class OpenGLWidget : public GUI::Frame {
    C_OBJECT(OpenGLWidget);

public:
    GL::GLContext& context()
    {
        return *m_context;
    }

    RefPtr<Gfx::Bitmap> bitmap()
    {
        return m_bitmap;
    }

    OpenGLWidget();

protected:
    virtual void paint_event(GUI::PaintEvent&) override;

    virtual void resize_event(ResizeEvent& event) override;

    void make_current()
    {
        GL::make_context_current(m_context);
    }

    virtual void initialize_gl();

    virtual void paint_gl();

    virtual void resize_gl(int, int);

private:
    RefPtr<Gfx::Bitmap> m_bitmap { 0 };
    OwnPtr<GL::GLContext> m_context { nullptr };

    bool initialized() const { return m_context; }
    void initialize();
};

}
