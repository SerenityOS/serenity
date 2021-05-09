/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibAudio/Buffer.h>

class Visualization {
public:
    virtual void set_buffer(RefPtr<Audio::Buffer> buffer) = 0;
    virtual void set_samplerate(int) { }

protected:
    virtual ~Visualization() = default;
};
