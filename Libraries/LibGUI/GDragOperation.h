/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibCore/CEventLoop.h>
#include <LibCore/CObject.h>

namespace Gfx {
class Bitmap;
}

namespace GUI {

class WindowServerConnection;

class DragOperation : public Core::Object {
    C_OBJECT(DragOperation)
public:
    enum class Outcome {
        None,
        Accepted,
        Cancelled,
    };

    virtual ~DragOperation() override;

    void set_text(const String& text) { m_text = text; }
    void set_bitmap(const Gfx::Bitmap* bitmap) { m_bitmap = bitmap; }
    void set_data(const String& data_type, const String& data)
    {
        m_data_type = data_type;
        m_data = data;
    }

    Outcome exec();
    Outcome outcome() const { return m_outcome; }

    static void notify_accepted(Badge<WindowServerConnection>);
    static void notify_cancelled(Badge<WindowServerConnection>);

protected:
    explicit DragOperation(Core::Object* parent = nullptr);

private:
    void done(Outcome);

    OwnPtr<Core::EventLoop> m_event_loop;
    Outcome m_outcome { Outcome::None };
    String m_text;
    String m_data_type;
    String m_data;
    RefPtr<Gfx::Bitmap> m_bitmap;
};

}
