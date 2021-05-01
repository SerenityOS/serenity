/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWasm/Types.h>

namespace Wasm {

struct Printer {
    explicit Printer(OutputStream& stream, size_t initial_indent = 0)
        : m_stream(stream)
        , m_indent(initial_indent)
    {
    }

    void print(const Wasm::BlockType&);
    void print(const Wasm::CodeSection&);
    void print(const Wasm::CodeSection::Code&);
    void print(const Wasm::CodeSection::Func&);
    void print(const Wasm::CustomSection&);
    void print(const Wasm::DataCountSection&);
    void print(const Wasm::DataSection&);
    void print(const Wasm::DataSection::Data&);
    void print(const Wasm::ElementSection&);
    void print(const Wasm::ElementSection::SegmentType0&);
    void print(const Wasm::ElementSection::SegmentType1&);
    void print(const Wasm::ElementSection::SegmentType2&);
    void print(const Wasm::ElementSection::SegmentType3&);
    void print(const Wasm::ElementSection::SegmentType4&);
    void print(const Wasm::ElementSection::SegmentType5&);
    void print(const Wasm::ElementSection::SegmentType6&);
    void print(const Wasm::ElementSection::SegmentType7&);
    void print(const Wasm::ExportSection&);
    void print(const Wasm::ExportSection::Export&);
    void print(const Wasm::Expression&);
    void print(const Wasm::FunctionSection&);
    void print(const Wasm::FunctionType&);
    void print(const Wasm::GlobalSection&);
    void print(const Wasm::GlobalSection::Global&);
    void print(const Wasm::GlobalType&);
    void print(const Wasm::ImportSection&);
    void print(const Wasm::ImportSection::Import&);
    void print(const Wasm::Instruction&);
    void print(const Wasm::Limits&);
    void print(const Wasm::Locals&);
    void print(const Wasm::MemorySection&);
    void print(const Wasm::MemorySection::Memory&);
    void print(const Wasm::MemoryType&);
    void print(const Wasm::Module&);
    void print(const Wasm::Module::Function&);
    void print(const Wasm::StartSection&);
    void print(const Wasm::StartSection::StartFunction&);
    void print(const Wasm::TableSection&);
    void print(const Wasm::TableSection::Table&);
    void print(const Wasm::TableType&);
    void print(const Wasm::TypeSection&);
    void print(const Wasm::ValueType&);

private:
    void print_indent();
    template<typename... Args>
    void print(CheckedFormatString<Args...> fmt, Args&&... args)
    {
        StringBuilder builder;
        builder.appendff(fmt.view(), forward<Args>(args)...);
        m_stream.write_or_error(builder.string_view().bytes());
    }

    OutputStream& m_stream;
    size_t m_indent { 0 };
};

}
