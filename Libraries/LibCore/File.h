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

#include <AK/String.h>
#include <LibCore/IODevice.h>

namespace Core {

class File final : public IODevice {
    C_OBJECT(File)
public:
    virtual ~File() override;

    String filename() const { return m_filename; }
    void set_filename(const StringView& filename) { m_filename = filename; }

    bool is_directory() const;
    static bool is_directory(const String& filename);

    static bool exists(const String& filename);

    virtual bool open(IODevice::OpenMode) override;

    enum class ShouldCloseFileDescription {
        No = 0,
        Yes
    };
    bool open(int fd, IODevice::OpenMode, ShouldCloseFileDescription);

private:
    File(Object* parent = nullptr)
        : IODevice(parent)
    {
    }
    explicit File(const StringView&, Object* parent = nullptr);

    String m_filename;
    ShouldCloseFileDescription m_should_close_file_descriptor { ShouldCloseFileDescription::Yes };
};

}
