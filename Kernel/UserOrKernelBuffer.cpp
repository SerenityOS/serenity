/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <Kernel/UserOrKernelBuffer.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

bool UserOrKernelBuffer::is_kernel_buffer() const
{
    return !is_user_address(VirtualAddress(m_buffer));
}

String UserOrKernelBuffer::copy_into_string(size_t size) const
{
    if (!m_buffer)
        return {};
    if (is_user_address(VirtualAddress(m_buffer))) {
        char* buffer;
        auto data_copy = StringImpl::create_uninitialized(size, buffer);
        if (!copy_from_user(buffer, m_buffer, size))
            return {};
        return data_copy;
    }

    return String(ReadonlyBytes { m_buffer, size });
}

bool UserOrKernelBuffer::write(const void* src, size_t offset, size_t len)
{
    if (!m_buffer)
        return false;

    if (is_user_address(VirtualAddress(m_buffer)))
        return copy_to_user(m_buffer + offset, src, len);

    memcpy(m_buffer + offset, src, len);
    return true;
}

bool UserOrKernelBuffer::read(void* dest, size_t offset, size_t len) const
{
    if (!m_buffer)
        return false;

    if (is_user_address(VirtualAddress(m_buffer)))
        return copy_from_user(dest, m_buffer + offset, len);

    memcpy(dest, m_buffer + offset, len);
    return true;
}

bool UserOrKernelBuffer::memset(int value, size_t offset, size_t len)
{
    if (!m_buffer)
        return false;

    if (is_user_address(VirtualAddress(m_buffer)))
        return memset_user(m_buffer + offset, value, len);

    ::memset(m_buffer + offset, value, len);
    return true;
}

}
