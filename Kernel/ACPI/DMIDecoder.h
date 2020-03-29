/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/FixedArray.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/KBuffer.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/VM/Region.h>
#include <LibBareMetal/Memory/PhysicalAddress.h>
#include <LibBareMetal/Memory/VirtualAddress.h>
#include <LibHardware/SMBIOS/Definitions.h>

namespace Kernel {

class DMIDecoder {
public:
    static DMIDecoder& the();
    static void initialize();
    static void initialize_untrusted();
    bool is_reliable() const { return !m_untrusted; };
    void generate_data_raw_blob(KBufferBuilder&) const;
    void generate_entry_raw_blob(KBufferBuilder&) const;

private:
    explicit DMIDecoder(bool trusted);
    void initialize_parser();

    void set_64_bit_entry_initialization_values(PhysicalAddress);
    void set_32_bit_entry_initialization_values(PhysicalAddress);

    PhysicalAddress find_entry32bit_point();
    PhysicalAddress find_entry64bit_point();

    PhysicalAddress m_entry32bit_point;
    PhysicalAddress m_entry64bit_point;
    PhysicalAddress m_structure_table;
    u32 m_structures_count;
    u32 m_table_length;
    bool m_use_64bit_entry : 1;
    bool m_operable : 1;
    bool m_untrusted : 1;
};

}
