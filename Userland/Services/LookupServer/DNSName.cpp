/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
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

#include "DNSName.h"

namespace LookupServer {

DNSName::DNSName(const String& name)
{
    if (name.ends_with('.'))
        m_name = name.substring(0, name.length() - 1);
    else
        m_name = name;
}

DNSName DNSName::parse(const u8* data, size_t& offset, size_t max_offset, size_t recursion_level)
{
    if (recursion_level > 4)
        return DNSName({});

    StringBuilder builder;
    while (true) {
        if (offset >= max_offset)
            return DNSName({});
        u8 b = data[offset++];
        if (b == '\0') {
            // This terminates the name.
            return builder.to_string();
        } else if ((b & 0xc0) == 0xc0) {
            // The two bytes tell us the offset when to continue from.
            if (offset >= max_offset)
                return DNSName({});
            size_t dummy = (b & 0x3f) << 8 | data[offset++];
            auto rest_of_name = parse(data, dummy, max_offset, recursion_level + 1);
            builder.append(rest_of_name.as_string());
            return builder.to_string();
        } else {
            // This is the length of a part.
            if (offset + b >= max_offset)
                return DNSName({});
            builder.append((const char*)&data[offset], (size_t)b);
            builder.append('.');
            offset += b;
        }
    }
}

}
