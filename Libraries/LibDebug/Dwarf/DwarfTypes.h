/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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
#include <AK/Types.h>

namespace Dwarf {

struct [[gnu::packed]] CompilationUnitHeader
{
    u32 length;
    u16 version;
    u32 abbrev_offset;
    u8 address_size;
};

enum class EntryTag : u32 {
    None = 0,
    LexicalBlock = 0xb,
    Member = 0xd,
    SubProgram = 0x2e,
    Variable = 0x34,
};

enum class Attribute : u32 {
    None = 0,
    Sibling = 0x1,
    Location = 0x2,
    Name = 0x3,
    LowPc = 0x11,
    HighPc = 0x12,
    Inline = 0x20,
    MemberLocation = 0x38,
    Type = 0x49,
    Ranges = 0x55,
};

enum class AttributeDataForm : u32 {
    None = 0,
    Addr = 0x1,
    Data2 = 0x5,
    Data4 = 0x6,
    String = 0x8,
    Data1 = 0xb,
    StringPointer = 0xe,
    Ref4 = 0x13,
    SecOffset = 0x17,
    ExprLoc = 0x18,
    FlagPresent = 0x19,
};

struct AttributeSpecification {
    Attribute attribute;
    AttributeDataForm form;
};

}
