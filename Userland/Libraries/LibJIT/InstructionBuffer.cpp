/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibJIT/InstructionBuffer.h>
#include <LibX86/Disassembler.h>
#include <LibX86/Instruction.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <syscall.h>
#include <unistd.h>

namespace JIT {

InstructionBuffer::InstructionBuffer(String&& name, size_t num_pages)
    : m_region_name(move(name))
    , m_region_size(num_pages * PAGE_SIZE)
    , m_used_space(0)
    , m_region_is_executable(false)
{
    VERIFY(m_region_size > 0);
    m_memory_region = static_cast<u8*>(serenity_mmap(nullptr, m_region_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, PAGE_SIZE, name.characters()));
    if (m_memory_region == MAP_FAILED) {
        outln("Failed to map region: {}", strerror(errno));
        VERIFY_NOT_REACHED();
    }
}

InstructionBuffer::~InstructionBuffer()
{
    munmap(m_memory_region, m_region_size);
}

void InstructionBuffer::finalize()
{
    m_region_is_executable = true;
    if (mprotect(m_memory_region, m_region_size, PROT_READ | PROT_EXEC) != 0) {
        perror("mprotect");
        VERIFY_NOT_REACHED();
    }
}

void InstructionBuffer::append_bytes(Bytes data)
{
    VERIFY(!m_region_is_executable);
    ensure_tail_capacity(data.size());
    memcpy(&m_memory_region[m_used_space], data.data(), data.size());
    m_used_space += data.size();
}

void InstructionBuffer::ensure_tail_capacity(size_t extra_space)
{
    while (m_used_space + extra_space > m_region_size)
        grow();
}

void InstructionBuffer::grow()
{
    VERIFY(m_can_grow);
    VERIFY(!m_region_is_executable);
    size_t m_old_region_size = m_region_size;
    m_region_size *= 2;
    void* new_region = static_cast<u8*>(serenity_mmap(nullptr, m_region_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, PAGE_SIZE, m_region_name.characters()));
    VERIFY(new_region != MAP_FAILED);
    memcpy(new_region, m_memory_region, m_used_space);
    munmap(m_memory_region, m_old_region_size);
    m_memory_region = (u8*)new_region;
}

void InstructionBuffer::enter_at_offset(JITLabel offset) const
{
    void* entry_point = &m_memory_region[offset.value()];
    asm volatile("call *%0"
                 :
                 : "r"(entry_point));
}

void InstructionBuffer::dump_encoded_instructions()
{
    X86::SimpleInstructionStream stream(m_memory_region, m_used_space);
    X86::Disassembler disassembler(stream);
    for (;;) {
        auto offset = stream.offset();
        auto ins = disassembler.next();
        if (!ins.has_value())
            break;
        size_t address = ((size_t)m_memory_region) + offset;
        outln("{:p}  {}", address, ins.value().to_string(address, nullptr));
    }
}

}
