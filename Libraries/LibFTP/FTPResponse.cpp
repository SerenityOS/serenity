/*
 * Copyright (c) 2020, sppmacd <sppmacd@gmail.com>
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

#include <LibFTP/FTPResponse.h>

namespace FTP {

FTPResponse::FTPResponse(ByteBuffer&& payload)
    : Core::NetworkResponse(move(payload))
{
    if(!parse())
        dbg() << "Failed to parse FTP response :(";
}

FTPResponse::~FTPResponse()
{
}

bool FTPResponse::parse()
{
    auto& buffer = payload();
    String data((const char*)(buffer.data()), buffer.size());

    auto space_pos = data.index_of(" ");
    if(!space_pos.has_value())
    {
        dbg() << "FTPResponse::parse(): expected space (' ') in response";
        ASSERT_NOT_REACHED();
        return false;
    }

    bool ok = false;
    unsigned result_code = data.substring(0, space_pos.value()).to_uint(ok);
    if(!ok)
    {
        dbg() << "FTPResponse::parse(): expected result code";
        ASSERT_NOT_REACHED();
        return false;
    }
    m_code = result_code;
    auto message = data.substring(space_pos.value() + 1, data.length() - space_pos.value() - 1);
    m_message = message;
    return true;
}

}
