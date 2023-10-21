/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibDisassembly/riscv64/Instruction.h>

// F and D extensions.
namespace Disassembly::RISCV64 {

// Table 11.3; IEEE 754 floating-point "format" or width field.
enum class FloatWidth : u8 {
    // binary32
    Single = 0,
    // binary64
    Double = 1,
    // binary16
    Half = 2,
    // binary128
    Quad = 3,
};

constexpr DataWidth float_width_to_data_width(FloatWidth width)
{
    switch (width) {
    case FloatWidth::Single:
        return DataWidth::Word;
    case FloatWidth::Double:
        return DataWidth::DoubleWord;
    case FloatWidth::Half:
        return DataWidth::Halfword;
    case FloatWidth::Quad:
        return DataWidth::QuadWord;
    }
    VERIFY_NOT_REACHED();
}

constexpr FloatWidth data_width_to_float_width(DataWidth width)
{
    switch (width) {
    case DataWidth::Word:
        return FloatWidth::Single;
    case DataWidth::DoubleWord:
        return FloatWidth::Double;
    case DataWidth::Halfword:
        return FloatWidth::Half;
    case DataWidth::QuadWord:
        return FloatWidth::Quad;
    case DataWidth::Byte:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

class FloatInstruction : public InstructionImpl {
public:
    virtual ~FloatInstruction() = default;
    bool operator==(FloatInstruction const&) const = default;

    // When used in loads and stores, we can coerce a float width to its equivalent memory access type.
    MemoryAccessMode memory_width() const
    {
        return { .width = float_width_to_data_width(m_width), .signedness = Signedness::Signed };
    }
    FloatWidth width() const { return m_width; }

    FloatInstruction(FloatWidth width)
        : m_width(width)
    {
    }

private:
    FloatWidth m_width;
};

class FloatComputationInstruction : public FloatInstruction {
public:
    virtual ~FloatComputationInstruction() = default;
    virtual i32 immediate() const override { return 0; }
    bool operator==(FloatComputationInstruction const&) const = default;
    RoundingMode rounding_mode() const { return m_rounding_mode; }
    String format_rounding_mode(DisplayStyle display_style) const;

    FloatComputationInstruction(RoundingMode rounding_mode, FloatWidth width)
        : FloatInstruction(width)
        , m_rounding_mode(rounding_mode)
    {
    }

private:
    RoundingMode m_rounding_mode;
};

class FloatTwoArgumentInstruction : public FloatComputationInstruction {
public:
    bool operator==(FloatTwoArgumentInstruction const&) const = default;
    FloatRegister source_register_1() const { return m_rs1; }
    FloatRegister source_register_2() const { return m_rs2; }

    FloatTwoArgumentInstruction(RoundingMode rounding_mode, FloatWidth width, FloatRegister rs1, FloatRegister rs2)
        : FloatComputationInstruction(rounding_mode, width)
        , m_rs1(rs1)
        , m_rs2(rs2)
    {
    }

private:
    FloatRegister m_rs1;
    FloatRegister m_rs2;
};

class FloatRTypeInstruction : public FloatTwoArgumentInstruction {
public:
    bool operator==(FloatRTypeInstruction const&) const = default;
    FloatRegister destination_register() const { return m_rd; }

    FloatRTypeInstruction(RoundingMode rounding_mode, FloatWidth width, FloatRegister rs1, FloatRegister rs2, FloatRegister rd)
        : FloatTwoArgumentInstruction(rounding_mode, width, rs1, rs2)
        , m_rd(rd)
    {
    }

private:
    FloatRegister m_rd;
};

class FloatArithmeticInstruction : public FloatRTypeInstruction {
public:
    enum class Operation : u8 {
        Add,
        Subtract,
        Multiply,
        Divide,
        Min,
        Max,
        // Always use rs1's value except the sign:
        // Copy sign from rs2.
        SignInject,
        // Copy inverted sign from rs2.
        SignInjectNegate,
        // Xor both signs.
        SignInjectXor,
    };

    virtual ~FloatArithmeticInstruction() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(FloatArithmeticInstruction const&) const = default;
    Operation operation() const { return m_operation; }

    FloatArithmeticInstruction(Operation operation, RoundingMode rounding_mode, FloatWidth width, FloatRegister rs1, FloatRegister rs2, FloatRegister rd)
        : FloatRTypeInstruction(rounding_mode, width, rs1, rs2, rd)
        , m_operation(operation)
    {
    }

private:
    Operation m_operation;
};

class FloatSquareRoot : public FloatComputationInstruction {
public:
    virtual ~FloatSquareRoot() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(FloatSquareRoot const&) const = default;

    FloatSquareRoot(RoundingMode rounding_mode, FloatWidth width, FloatRegister rs, FloatRegister rd)
        : FloatComputationInstruction(rounding_mode, width)
        , m_rs(rs)
        , m_rd(rd)
    {
    }

private:
    FloatRegister m_rs;
    FloatRegister m_rd;
};

class FloatCompare : public FloatTwoArgumentInstruction {
public:
    enum class Operation : u8 {
        Equals,
        LessThan,
        LessThanEquals,
    };

    virtual ~FloatCompare() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(FloatCompare const&) const = default;

    FloatCompare(Operation operation, FloatWidth width, FloatRegister rs1, FloatRegister rs2, Register rd)
        : FloatTwoArgumentInstruction(RoundingMode::DYN, width, rs1, rs2)
        , m_rd(rd)
        , m_operation(operation)
    {
    }

private:
    Register m_rd;
    Operation m_operation;
};

class ConvertFloatAndInteger : public FloatComputationInstruction {
public:
    virtual ~ConvertFloatAndInteger() = default;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(ConvertFloatAndInteger const&) const = default;
    MemoryAccessMode integer_width() const { return m_integer_width; }
    String integer_width_suffix() const;

    ConvertFloatAndInteger(RoundingMode rounding_mode, MemoryAccessMode integer_width, FloatWidth float_width)
        : FloatComputationInstruction(rounding_mode, float_width)
        , m_integer_width(integer_width)
    {
    }

private:
    MemoryAccessMode m_integer_width;
};

class ConvertFloatToInteger : public ConvertFloatAndInteger {
public:
    virtual ~ConvertFloatToInteger() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(ConvertFloatToInteger const&) const = default;

    ConvertFloatToInteger(RoundingMode rounding_mode, MemoryAccessMode integer_width, FloatWidth float_width, FloatRegister rs, Register rd)
        : ConvertFloatAndInteger(rounding_mode, integer_width, float_width)
        , m_rs(rs)
        , m_rd(rd)
    {
    }

private:
    FloatRegister m_rs;
    Register m_rd;
};

class ConvertIntegerToFloat : public ConvertFloatAndInteger {
public:
    virtual ~ConvertIntegerToFloat() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(ConvertIntegerToFloat const&) const = default;

    ConvertIntegerToFloat(RoundingMode rounding_mode, MemoryAccessMode integer_width, FloatWidth float_width, Register rs, FloatRegister rd)
        : ConvertFloatAndInteger(rounding_mode, integer_width, float_width)
        , m_rs(rs)
        , m_rd(rd)
    {
    }

private:
    Register m_rs;
    FloatRegister m_rd;
};

class ConvertFloat : public FloatComputationInstruction {
public:
    enum class Operation {
        SingleToDouble,
        DoubleToSingle,
    };

    virtual ~ConvertFloat() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(ConvertFloat const&) const = default;

    ConvertFloat(Operation operation, RoundingMode rounding_mode, FloatRegister rs, FloatRegister rd)
        : FloatComputationInstruction(rounding_mode, FloatWidth::Double)
        , m_rs(rs)
        , m_rd(rd)
        , m_operation(operation)
    {
    }

private:
    FloatRegister m_rs;
    FloatRegister m_rd;
    Operation m_operation;
};

class FloatClassify : public FloatInstruction {
public:
    virtual ~FloatClassify() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual i32 immediate() const override { return 0; }
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(FloatClassify const&) const = default;

    FloatClassify(FloatWidth width, FloatRegister rs, Register rd)
        : FloatInstruction(width)
        , m_rs(rs)
        , m_rd(rd)
    {
    }

private:
    FloatRegister m_rs;
    Register m_rd;
};

class MoveFloatToInteger : public FloatInstruction {
public:
    virtual ~MoveFloatToInteger() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual i32 immediate() const override { return 0; }
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(MoveFloatToInteger const&) const = default;

    MoveFloatToInteger(FloatWidth width, FloatRegister rs, Register rd)
        : FloatInstruction(width)
        , m_rs(rs)
        , m_rd(rd)
    {
    }

private:
    FloatRegister m_rs;
    Register m_rd;
};

class MoveIntegerToFloat : public FloatInstruction {
public:
    virtual ~MoveIntegerToFloat() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual i32 immediate() const override { return 0; }
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(MoveIntegerToFloat const&) const = default;

    MoveIntegerToFloat(FloatWidth width, Register rs, FloatRegister rd)
        : FloatInstruction(width)
        , m_rs(rs)
        , m_rd(rd)
    {
    }

private:
    Register m_rs;
    FloatRegister m_rd;
};

class FloatFusedMultiplyAdd : public FloatRTypeInstruction {
public:
    enum class Operation : u8 {
        // (rs1 · rs2) + rs3
        MultiplyAdd,
        // (rs1 · rs2) - rs3
        MultiplySubtract,
        // - (rs1 · rs2) + rs3
        NegatedMultiplyAdd,
        // - (rs1 · rs2) - rs3
        NegatedMultiplySubtract,
    };

    virtual ~FloatFusedMultiplyAdd() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(FloatFusedMultiplyAdd const&) const = default;
    FloatRegister source_register_3() const { return m_rs3; }

    FloatFusedMultiplyAdd(Operation operation, RoundingMode rounding_mode, FloatWidth width, FloatRegister rs1, FloatRegister rs2, FloatRegister rs3, FloatRegister rd)
        : FloatRTypeInstruction(rounding_mode, width, rs1, rs2, rd)
        , m_operation(operation)
        , m_rs3(rs3)
    {
    }

private:
    Operation m_operation;
    FloatRegister m_rs3;
};

class FloatMemoryInstruction : public FloatInstruction {
public:
    virtual ~FloatMemoryInstruction() = default;
    virtual i32 immediate() const override { return m_offset; }
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(FloatMemoryInstruction const&) const = default;
    Register base() const { return m_base; }

    FloatMemoryInstruction(i32 offset, Register base, FloatWidth width)
        : FloatInstruction(width)
        , m_base(base)
        , m_offset(offset)
    {
    }

private:
    Register m_base;
    i32 m_offset;
};

class FloatMemoryLoad : public FloatMemoryInstruction {
public:
    virtual ~FloatMemoryLoad() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(FloatMemoryLoad const&) const = default;
    FloatRegister destination_register() const { return m_rd; }

    FloatMemoryLoad(i32 offset, Register base, FloatWidth width, FloatRegister rd)
        : FloatMemoryInstruction(offset, base, width)
        , m_rd(rd)
    {
    }

private:
    FloatRegister m_rd;
};

class FloatMemoryStore : public FloatMemoryInstruction {
public:
    virtual ~FloatMemoryStore() = default;
    virtual String to_string(DisplayStyle display_style, u32 origin, Optional<SymbolProvider const&> symbol_provider) const override;
    virtual String mnemonic() const override;
    virtual bool instruction_equals(InstructionImpl const&) const override;
    bool operator==(FloatMemoryStore const&) const = default;
    FloatRegister source_register() const { return m_source; }

    FloatMemoryStore(i32 offset, FloatRegister source, Register base, FloatWidth width)
        : FloatMemoryInstruction(offset, base, width)
        , m_source(source)
    {
    }

private:
    FloatRegister m_source;
};

NonnullOwnPtr<InstructionImpl> parse_op_fp(u32 instruction);
NonnullOwnPtr<InstructionImpl> parse_fma(u32 instruction);
NonnullOwnPtr<InstructionImpl> parse_load_fp(u32 instruction);
NonnullOwnPtr<InstructionImpl> parse_store_fp(u32 instruction);

}
