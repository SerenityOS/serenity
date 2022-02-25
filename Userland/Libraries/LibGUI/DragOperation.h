/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibCore/MimeData.h>
#include <LibCore/Object.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>

namespace GUI {

class DragOperation : public Core::Object {
    C_OBJECT(DragOperation)
public:
    enum class Outcome {
        None,
        Accepted,
        Cancelled,
    };

    virtual ~DragOperation() override;

    void set_mime_data(RefPtr<Core::MimeData> mime_data) { m_mime_data = move(mime_data); }
    void set_text(const String& text);
    void set_bitmap(const Gfx::Bitmap* bitmap);
    void set_data(const String& data_type, const String& data);

    Outcome exec();
    Outcome outcome() const { return m_outcome; }

    static void notify_accepted(Badge<ConnectionToWindowServer>);
    static void notify_cancelled(Badge<ConnectionToWindowServer>);

protected:
    explicit DragOperation(Core::Object* parent = nullptr);

private:
    void done(Outcome);

    OwnPtr<Core::EventLoop> m_event_loop;
    Outcome m_outcome { Outcome::None };
    RefPtr<Core::MimeData> m_mime_data;
};

}
