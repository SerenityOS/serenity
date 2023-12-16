/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>

namespace GUI {

class DragOperation : public Core::EventReceiver {
    C_OBJECT(DragOperation)
public:
    enum class Outcome {
        None,
        Accepted,
        Cancelled,
    };

    virtual ~DragOperation() override = default;

    void set_mime_data(RefPtr<Core::MimeData> mime_data) { m_mime_data = move(mime_data); }
    void set_text(ByteString const& text);
    void set_bitmap(Gfx::Bitmap const* bitmap);
    void set_data(String const& data_type, ByteString const& data);

    Outcome exec();
    Outcome outcome() const { return m_outcome; }

    static void notify_accepted(Badge<ConnectionToWindowServer>);
    static void notify_cancelled(Badge<ConnectionToWindowServer>);

protected:
    explicit DragOperation(Core::EventReceiver* parent = nullptr);

private:
    void done(Outcome);

    OwnPtr<Core::EventLoop> m_event_loop;
    Outcome m_outcome { Outcome::None };
    RefPtr<Core::MimeData> m_mime_data;
};

}
