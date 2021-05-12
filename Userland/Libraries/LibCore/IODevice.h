/*
 * Copyright (c) 2021, the SerenityOS developers.
 * All rights reserved.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Stream.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>

namespace Core {

class IODevice
    : public Object
    , public virtual DuplexStream {
    C_OBJECT_ABSTRACT(IODevice);

public:
    ~IODevice() override
    {
        // If something gets here with an error, just drop it.
        Stream::handle_any_error();
    }

    // Makes a *new* notifier with the given mask.
    // Note that this MAY fail, in which case it will return nullptr.
    virtual RefPtr<Core::AbstractNotifier> make_notifier(unsigned event_mask = Core::Notifier::Event::Read) = 0;

    // ^DuplexStream
    size_t read(Bytes) override = 0;
    bool discard_or_error(size_t count) override = 0;
    bool unreliable_eof() const override = 0;
    size_t write(ReadonlyBytes) override = 0;

    bool read_or_error(Bytes bytes) override
    {
        if (read(bytes) != bytes.size()) {
            set_recoverable_error();
            return false;
        }
        return true;
    }
    bool write_or_error(ReadonlyBytes bytes) override
    {
        if (write(bytes) != bytes.size()) {
            set_recoverable_error();
            return false;
        }
        return true;
    }

protected:
    explicit IODevice(Object* parent)
        : Object(parent)
    {
    }
};

}
