/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibACPI/OpcodeFilters.h>

namespace ACPI::AML {

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#namespacemodifierobj
bool namespace_modifier_object_filter(Opcode opcode)
{
    // NameSpaceModifierObj := DefAlias | DefName | DefScope
    switch (opcode) {
    case Opcode::AliasOp:
    case Opcode::NameOp:
    case Opcode::ScopeOp:
        return true;

    default:
        return false;
    }
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#namedobj
bool named_object_filter(Opcode opcode)
{
    // NamedObj := DefBankField | DefCreateBitField | DefCreateByteField | DefCreateDWordField | DefCreateField | DefCreateQWordField | DefCreateWordField | DefDataRegion | DefExternal | DefOpRegion | DefPowerRes | DefThermalZone

    // FieldOp missing from 6.6 docs, but given the presence of the DefXYZField
    // opcodes here, I'm adding it in here.
    // DeviceOp, MethodOp and MutexOp are also missing. Adding it here too beacuse they also create an object.

    // ProcessorOp found here in 6.3 docs.
    switch (opcode) {
    case Opcode::BankFieldOp:
    case Opcode::CreateBitFieldOp:
    case Opcode::CreateByteFieldOp:
    case Opcode::CreateDWordFieldOp:
    case Opcode::CreateFieldOp:
    case Opcode::CreateQWordFieldOp:
    case Opcode::CreateWordFieldOp:
    case Opcode::DataRegionOp:
    case Opcode::DeviceOp:
    case Opcode::ExternalOp:
    case Opcode::FieldOp:
    case Opcode::MethodOp:
    case Opcode::MutexOp:
    case Opcode::OpRegionOp:
    case Opcode::PowerResOp:
    case Opcode::ProcessorOp:
    case Opcode::ThermalZoneOp:
        return true;

    default:
        return false;
    }
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#object
bool object_filter(Opcode opcode)
{
    // Object := NameSpaceModifierObj | NamedObj
    return namespace_modifier_object_filter(opcode) || named_object_filter(opcode);
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#statementopcode
bool statement_filter(Opcode opcode)
{
    // StatementOpcode := DefBreak | DefBreakPoint | DefContinue | DefFatal | DefIfElse | DefNoop | DefNotify | DefRelease | DefReset | DefReturn | DefSignal | DefSleep | DefStall | DefWhile
    switch (opcode) {
    case Opcode::BreakOp:
    case Opcode::BreakPointOp:
    case Opcode::ContinueOp:
    case Opcode::FatalOp:
    case Opcode::IfOp:
    case Opcode::ElseOp:
    case Opcode::NoopOp:
    case Opcode::NotifyOp:
    case Opcode::ReleaseOp:
    case Opcode::ResetOp:
    case Opcode::ReturnOp:
    case Opcode::SignalOp:
    case Opcode::SleepOp:
    case Opcode::StallOp:
    case Opcode::WhileOp:
        return true;

    default:
        return false;
    }
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#expressionopcode
bool expression_filter(Opcode opcode)
{
    // ExpressionOpcode := DefAcquire | DefAdd | DefAnd | DefBuffer | DefConcat | DefConcatRes | DefCondRefOf | DefCopyObject | DefDecrement | DefDerefOf | DefDivide | DefFindSetLeftBit | DefFindSetRightBit | DefFromBCD | DefIncrement | DefIndex | DefLAnd | DefLEqual | DefLGreater | DefLGreaterEqual | DefLLess | DefLLessEqual | DefMid | DefLNot | DefLNotEqual | DefLoadTable | DefLOr | DefMatch | DefMod | DefMultiply | DefNAnd | DefNOr | DefNot | DefObjectType | DefOr | DefPackage | DefVarPackage | DefRefOf | DefShiftLeft | DefShiftRight | DefSizeOf | DefStore | DefSubtract | DefTimer | DefToBCD | DefToBuffer | DefToDecimalString | DefToHexString | DefToInteger | DefToString | DefWait | DefXOr | MethodInvocation
    // FIXME: Add support for MethodInvocation.

    switch (opcode) {
    case Opcode::AcquireOp:
    case Opcode::AddOp:
    case Opcode::AndOp:
    case Opcode::BufferOp:
    case Opcode::ConcatOp:
    case Opcode::ConcatResOp:
    case Opcode::CondRefOfOp:
    case Opcode::CopyObjectOp:
    case Opcode::DecrementOp:
    case Opcode::DerefOfOp:
    case Opcode::DivideOp:
    case Opcode::FindSetLeftBitOp:
    case Opcode::FindSetRightBitOp:
    case Opcode::FromBCDOp:
    case Opcode::IncrementOp:
    case Opcode::IndexOp:
    case Opcode::LAndOp:
    case Opcode::LEqualOp:
    case Opcode::LGreaterOp:
    case Opcode::LLessOp:
    case Opcode::LNotOp:
    case Opcode::LoadTableOp:
    case Opcode::LOrOp:
    case Opcode::MatchOp:
    case Opcode::MidOp:
    case Opcode::ModOp:
    case Opcode::MultiplyOp:
    case Opcode::NandOp:
    case Opcode::NorOp:
    case Opcode::NotOp:
    case Opcode::ObjectTypeOp:
    case Opcode::OrOp:
    case Opcode::PackageOp:
    case Opcode::VarPackageOp:
    case Opcode::RefOfOp:
    case Opcode::ShiftLeftOp:
    case Opcode::ShiftRightOp:
    case Opcode::SizeOfOp:
    case Opcode::StoreOp:
    case Opcode::SubtractOp:
    case Opcode::TimerOp:
    case Opcode::ToBCDOp:
    case Opcode::ToBufferOp:
    case Opcode::ToDecimalStringOp:
    case Opcode::ToHexStringOp:
    case Opcode::ToIntegerOp:
    case Opcode::ToStringOp:
    case Opcode::WaitOp:
    case Opcode::XorOp:
        return true;

    default:
        return false;
    }
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#constobj
bool const_object_filter(Opcode opcode)
{
    // ConstObj := ZeroOp | OneOp | OnesOp
    switch (opcode) {
    case Opcode::ZeroOp:
    case Opcode::OneOp:
    case Opcode::OnesOp:
        return true;

    default:
        return false;
    }
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#computationaldata
bool computational_data_filter(Opcode opcode)
{
    // ComputationalData := ByteConst | WordConst | DWordConst | QWordConst | String | ConstObj | RevisionOp | DefBuffer
    switch (opcode) {
    case Opcode::BytePrefix:
    case Opcode::WordPrefix:
    case Opcode::DWordPrefix:
    case Opcode::QWordPrefix:
    case Opcode::StringPrefix:
    case Opcode::RevisionOp:
    case Opcode::BufferOp:
        return true;

    default:
        return const_object_filter(opcode);
    }
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#dataobject
bool data_object_filter(Opcode opcode)
{
    // DataObject := ComputationalData | DefPackage | DefVarPackage
    switch (opcode) {
    case Opcode::PackageOp:
    case Opcode::VarPackageOp:
        return true;

    default:
        return computational_data_filter(opcode);
    }
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#datarefobject
bool data_ref_object_filter(Opcode opcode)
{
    // DataRefObject := DataObject | ObjectReference
    // FIXME: Add support for ObjectReference.
    return data_object_filter(opcode);
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#termobj
bool term_filter(Opcode opcode)
{
    // TermObj := Object | StatementOpcode | ExpressionOpcode
    return object_filter(opcode) || statement_filter(opcode) || expression_filter(opcode);
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#termarg
bool term_arg_filter(Opcode opcode)
{
    // TermArg := ExpressionOpcode | DataObject | ArgObj | LocalObj
    return expression_filter(opcode) || data_object_filter(opcode) || arg_obj_filter(opcode) || local_obj_filter(opcode);
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#argobj
bool arg_obj_filter(Opcode opcode)
{
    // ArgObj := Arg0Op | Arg1Op | Arg2Op | Arg3Op | Arg4Op | Arg5Op | Arg6Op
    return opcode >= Opcode::Arg0Op && opcode <= Opcode::Arg6Op;
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#localobj
bool local_obj_filter(Opcode opcode)
{
    // LocalObj := Local0Op | Local1Op | Local2Op | Local3Op | Local4Op | Local5Op | Local6Op | Local7Op
    return opcode >= Opcode::Local0Op && opcode <= Opcode::Local7Op;
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#leadnamechar
bool is_lead_name_char(u8 c)
{
    // LeadNameChar := ‘A’-‘Z’ | ‘_’
    return (c == '_') || (c >= 'A' && c <= 'Z');
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#namechar
bool is_name_char(u8 c)
{
    // NameChar := DigitChar | LeadNameChar
    return is_lead_name_char(c) || (c >= '0' && c <= '9');
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#namestring
bool is_namestring_start(u8 c)
{
    return is_lead_name_char(c) || (c == '^') || (c == '\\') || (c == DUAL_NAME_PREFIX) || (c == MULTI_NAME_PREFIX);
}

}
