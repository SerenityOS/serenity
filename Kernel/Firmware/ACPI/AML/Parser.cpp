/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Format.h>
#include <AK/StringBuilder.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/ACPI/AML/Opcodes.h>
#include <Kernel/Firmware/ACPI/AML/Parser.h>
#include <Kernel/Version.h>

namespace Kernel::ACPI::AML {

#define TRY_IF_MATCHES(expression)                                                                                                                                       \
    ({                                                                                                                                                                   \
        /* Ignore -Wshadow to allow nesting the macro. */                                                                                                                \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                                                                                                                                 \
            auto&& _temporary_result = (expression));                                                                                                                    \
        if (_temporary_result.is_error() && _temporary_result.error().code() != ENOTSUP)                                                                                 \
            return _temporary_result.release_error();                                                                                                                    \
        _temporary_result.is_error() ? Optional<decltype(expression)::ResultType> {} : Optional<decltype(expression)::ResultType> { _temporary_result.release_value() }; \
    })

Parser::Parser(Namespace& root_namespace, ReadonlyBytes bytecode, IntegerBitness integer_bitness)
    : m_root_namespace(root_namespace)
    , m_bytecode(bytecode)
    , m_integer_bitness(integer_bitness)
{
}

ErrorOr<Vector<StringView>> Parser::resolve_path(StringView path)
{
    Vector<StringView> resolved_path;
    if (path.starts_with(RootChar)) {
        path = path.substring_view(1);
    } else {
        size_t parent_prefix_count = 0;
        while (path.starts_with('^')) {
            path = path.substring_view(1);
            parent_prefix_count += 1;
        }
        if (parent_prefix_count > m_current_scope.size()) {
            dbgln("AML Error: Invalid parent prefix count {} for path {} in scope {}", parent_prefix_count, path, m_current_scope);
            return EINVAL; // The root has no parent
        }
        TRY(resolved_path.try_append(m_current_scope.data(), m_current_scope.size() - parent_prefix_count));
    }
    while (!path.is_empty()) {
        size_t segment_length = 0;
        for (; segment_length < 4; ++segment_length) {
            if (segment_length >= path.length())
                break;
            if (path[segment_length] == '.')
                break;
        }
        if (segment_length == 0) {
            dbgln("AML Error: Invalid segment in path {} in scope {}", path, m_current_scope);
            return EINVAL; // Segments must be at least 1 character long
        }
        TRY(resolved_path.try_append(path.substring_view(0, segment_length)));
        path = path.substring_view(min(segment_length + 1, path.length())); // + 1 to skip the separating period
    }
    return resolved_path;
}

ErrorOr<NonnullRefPtr<ASTNode>> Parser::get_object_at_path(StringView path)
{
    if ((path[0] != RootChar) && (path[0] != ParentPrefixChar) && (path.count("."sv) == 0)) {
        // Special namespace search rules apply to single name segment paths that are used for
        // referencing existing objects: The current scope is searched first, and then its parent
        // and then its grandparent, and so on.
        return m_root_namespace.search_node(m_current_scope, path);
    }
    auto resolved_path = TRY(resolve_path(path));
    return m_root_namespace.get_node(resolved_path);
}

u32 Parser::parse_package_length()
{
    // PkgLength := PkgLeadByte |
    //              <PkgLeadByte ByteData> |
    //              <PkgLeadByte ByteData ByteData> |
    //              <PkgLeadByte ByteData ByteData ByteData>
    // PkgLeadByte := <bit 7-6: ByteData count that follows (0-3)>
    //                <bit 5-4: Only used if PkgLength < 63>
    //                <bit 3-0: Least significant package length nybble>
    auto lead_byte = consume();

    auto trailing_bytes = lead_byte >> 6;
    if (trailing_bytes == 0)
        return lead_byte & 0x3F;

    u32 length = lead_byte & 0xF;
    for (auto i = 0; i < trailing_bytes; ++i) {
        length |= parse_byte_data() << (4 + (i * 8));
    }
    return length;
}

static bool is_lead_name_character(u8 character)
{
    // LeadNameChar := ‘A’-‘Z’ | ‘_’
    return (character >= 'A' && character <= 'Z') || (character == '_');
}

static bool is_name_character(u8 character)
{
    // NameChar := DigitChar | LeadNameChar
    // DigitChar := ‘0’-‘9’
    return (character >= '0' && character <= '9') || is_lead_name_character(character);
}

ErrorOr<StringView> Parser::parse_name_segment()
{
    // NameSeg := <LeadNameChar NameChar NameChar NameChar>
    //     Notice that NameSegs shorter than 4 characters are filled with
    //     trailing underscores (‘_’s).
    size_t segment_length = 4;
    while (segment_length > 1 && m_bytecode[m_offset + segment_length - 1] == '_')
        segment_length--;

    if (!is_lead_name_character(m_bytecode[m_offset])) {
        dbgln("AML Error: Invalid lead name character {:#02x}", m_bytecode[m_offset]);
        return EINVAL;
    }
    for (auto i = 1u; i < segment_length; ++i) {
        if (!is_name_character(m_bytecode[m_offset + i])) {
            dbgln("AML Error: Invalid name character {:#02x}", m_bytecode[m_offset + i]);
            return EINVAL;
        }
    }

    auto segment = m_bytecode.slice(m_offset, segment_length);
    m_offset += 4;
    return segment;
}

ErrorOr<NonnullOwnPtr<KString>> Parser::parse_name_string()
{
    // NameString := <RootChar NamePath> | <PrefixPath NamePath>
    StringBuilder name_builder;
    if (current() == RootChar) {
        TRY(name_builder.try_append(consume()));
    } else if (current() == ParentPrefixChar) {
        // PrefixPath := Nothing | <ParentPrefixChar PrefixPath>
        while (current() == ParentPrefixChar)
            TRY(name_builder.try_append(consume()));
    }

    auto first_segment = true;
    auto append_name_segment = [&]() -> ErrorOr<void> {
        auto name_segment = TRY(parse_name_segment());
        if (first_segment) {
            first_segment = false;
        } else {
            TRY(name_builder.try_append('.'));
        }
        TRY(name_builder.try_append(name_segment));
        return {};
    };

    // NamePath := NameSeg | DualNamePath | MultiNamePath | NullName
    if (current() == DualNamePrefix) {
        // DualNamePath := DualNamePrefix NameSeg NameSeg
        skip();
        TRY(append_name_segment());
        TRY(append_name_segment());
    } else if (current() == MultiNamePrefix) {
        // MultiNamePath := MultiNamePrefix SegCount NameSeg(SegCount)
        skip();
        // SegCount := ByteData
        auto segment_count = parse_byte_data();
        if (segment_count == 0) {
            dbgln("AML Error: Non-positive multi name path segment count");
            return EINVAL;
        }
        for (auto i = 0u; i < segment_count; ++i)
            TRY(append_name_segment());
    } else if (is_lead_name_character(current())) {
        TRY(append_name_segment());
    } else {
        auto indicator = consume();
        if (indicator != NullName) {
            dbgln("AML Error: Expected null name indicator, but found: {:#02x}", indicator);
            return EINVAL;
        }
    }

    return KString::try_create(name_builder.string_view());
}

u64 Parser::parse_byte_data()
{
    // ByteData := 0x00 - 0xFF
    return consume();
}

u64 Parser::parse_word_data()
{
    // WordData := ByteData[0:7] ByteData[8:15]
    return parse_byte_data() | (parse_byte_data() << 8);
}

u64 Parser::parse_dword_data()
{
    // DWordData := WordData[0:15] WordData[16:31]
    return parse_word_data() | (parse_word_data() << 16);
}

u64 Parser::parse_qword_data()
{
    // QWordData := DWordData[0:31] DWordData[32:63]
    return parse_dword_data() | (parse_dword_data() << 32);
}

ErrorOr<u64> Parser::parse_integer()
{
    // Integer := ByteConst | WordConst | DWordConst | QWordConst | RevisionOp | ConstObj
    switch (current()) {
    case BytePrefix: {
        // ByteConst := BytePrefix ByteData
        skip();
        return parse_byte_data();
    }
    case WordPrefix: {
        // WordConst := WordPrefix WordData
        skip();
        return parse_word_data();
    }
    case DWordPrefix: {
        // DWordConst := DWordPrefix DWordData
        skip();
        return parse_dword_data();
    }
    case QWordPrefix: {
        // QWordConst := QWordPrefix QWordData
        skip();
        return parse_qword_data();
    }
    case ExtOpPrefix: {
        skip();
        if (current() == RevisionOp) {
            skip();
            return SERENITY_MINOR_REVISION | (SERENITY_MAJOR_REVISION << 16);
        }
        m_offset--; // undo the previous skip, to let the parent try to parse it instead
        return Error::from_errno(ENOTSUP);
    }
    // ConstObj := ZeroOp | OneOp | OnesOp
    case ZeroOp: {
        skip();
        return 0;
    }
    case OneOp: {
        skip();
        return 1;
    }
    case OnesOp: {
        skip();
        return m_integer_bitness == IntegerBitness::IntegersAre64Bit ? 0xFFFFFFFFFFFFFFFFul : 0xFFFFFFFFul;
    }
    default:
        return Error::from_errno(ENOTSUP);
    }
}

ErrorOr<ByteBuffer> Parser::parse_byte_list(size_t end_offset)
{
    // ByteList := Nothing | <ByteData ByteList>
    auto byte_list = ByteBuffer::copy({ m_bytecode.data(), end_offset - m_offset });
    m_offset = end_offset;
    return byte_list;
}

ErrorOr<Vector<NonnullRefPtr<ASTNode>>> Parser::parse_term_list(size_t end_offset)
{
    // TermList := Nothing | <TermObj TermList>
    Vector<NonnullRefPtr<ASTNode>> terms;
    while (m_offset < end_offset) {
        // TermObj := Object | StatementOpcode | ExpressionOpcode
        auto maybe_object = TRY_IF_MATCHES(parse_object());
        if (maybe_object.has_value()) {
            TRY(terms.try_append(maybe_object.release_value()));
            continue;
        }
        // TODO: Parse StatementOpcode
        auto maybe_expression_opcode = TRY_IF_MATCHES(parse_expression_opcode());
        if (maybe_expression_opcode.has_value()) {
            TRY(terms.try_append(maybe_expression_opcode.release_value()));
            continue;
        }
        // TODO: Once we implemented parsing for all opcodes, change this print to an AML Error
        dbgln("FIXME: Unimplemented opcode {}", m_bytecode.slice(m_offset, 2));
        return EINVAL;
    }
    return terms;
}

ErrorOr<Vector<NonnullRefPtr<ASTNode>>> Parser::parse_field_list(size_t end_offset)
{
    // FieldList := Nothing | <FieldElement FieldList>
    Vector<NonnullRefPtr<ASTNode>> fields;

    while (m_offset < end_offset) {
        // FieldElement := NamedField | ReservedField | AccessField | ExtendedAccessField | ConnectField
        switch (current()) {
        case 0x00: {
            // ReservedField := 0x00 PkgLength
            skip();
            auto size = parse_package_length();
            auto field = TRY(try_make_ref_counted<ReservedField>(size));
            TRY(fields.try_append(move(field)));
            break;
        }
        case 0x01: {
            // AccessField := 0x01 AccessType AccessAttrib
            skip();
            return Error::from_errno(ENOTSUP); // TODO
        }
        case 0x02: {
            // ConnectField := <0x02 NameString> | <0x02 BufferData>
            // FIXME: BufferData is an ASL type, not an encoded AML type, based on real-world examples, this is actually DefBuffer
            skip();
            return Error::from_errno(ENOTSUP); // TODO
        }
        case 0x03: {
            // ExtendedAccessField := 0x03 AccessType ExtendedAccessAttrib AccessLength
            skip();
            return Error::from_errno(ENOTSUP); // TODO
        }
        default: {
            // NamedField := NameSeg PkgLength
            auto raw_name = TRY(parse_name_segment());
            auto name = TRY(KString::try_create(raw_name));
            auto size = parse_package_length();
            auto field = TRY(try_make_ref_counted<NamedField>(move(name), size));
            TRY(fields.try_append(field));

            auto path = TRY(resolve_path(raw_name));
            TRY(m_root_namespace.insert_node(path, move(field)));
            break;
        }
        }
    }

    return fields;
}

ErrorOr<Vector<NonnullRefPtr<ASTNode>>> Parser::parse_package_element_list(size_t end_offset)
{
    // PackageElementList := Nothing | <PackageElement PackageElementList>
    Vector<NonnullRefPtr<ASTNode>> package_element_list;

    while (m_offset < end_offset) {
        // PackageElement := DataRefObject | NameString
        auto maybe_data_reference_object = TRY_IF_MATCHES(parse_data_reference_object());
        if (maybe_data_reference_object.has_value()) {
            TRY(package_element_list.try_append(maybe_data_reference_object.release_value()));
            continue;
        }

        // TODO: Parse NameString
        return ENOTSUP;
    }

    return package_element_list;
}

ErrorOr<NonnullRefPtr<StringData>> Parser::parse_string()
{
    // String := StringPrefix AsciiCharList NullChar
    skip();

    // AsciiCharList := Nothing | <AsciiChar AsciiCharList>
    // AsciiChar := 0x01 - 0x7F
    // NullChar := 0x00
    StringBuilder builder;
    while (current() != 0x00) {
        if (!is_ascii(current())) {
            dbgln("AML Error: Invalid ASCII character in String {:#02x}", current());
            return EINVAL;
        }
        builder.append(consume());
    }
    skip(); // Skip final NullChar
    auto value = TRY(KString::try_create(builder.string_view()));
    return try_make_ref_counted<StringData>(move(value));
}

ErrorOr<NonnullRefPtr<ASTNode>> Parser::parse_computational_data()
{
    // ComputationalData := ByteConst | WordConst | DWordConst | QWordConst | String | ConstObj | RevisionOp | DefBuffer
    if (current() == StringPrefix) {
        return parse_string();
    }
    if (current() == BufferOp) {
        return parse_define_buffer();
    }
    auto value = TRY(parse_integer());
    return try_make_ref_counted<IntegerData>(value);
}

ErrorOr<NonnullRefPtr<ASTNode>> Parser::parse_data_object()
{
    // DataObject := ComputationalData | DefPackage | DefVarPackage
    if (current() == PackageOp) {
        return parse_define_package();
    }
    // TODO: Parse DefVarPackage
    return parse_computational_data();
}

ErrorOr<NonnullRefPtr<ASTNode>> Parser::parse_data_reference_object()
{
    // DataRefObject := DataObject | ObjectReference
    // FIXME: What does it mean for an ObjectReference to be encoded? It's only created during dynamic execution
    return parse_data_object();
}

ErrorOr<RefPtr<ASTNode>> Parser::parse_target()
{
    // Target := SuperName | NullName
    if (current() == NullName) {
        skip();
        return nullptr;
    }

    // TODO: Parse SuperName
    return ENOTSUP;
}

ErrorOr<NonnullRefPtr<ASTNode>> Parser::parse_expression_opcode()
{
    // ExpressionOpcode := DefAcquire | DefAdd | DefAnd | DefBuffer | DefConcat | DefConcatRes | DefCondRefOf | DefCopyObject | DefDecrement | DefDerefOf
    //                     | DefDivide | DefFindSetLeftBit | DefFindSetRightBit | DefFromBCD | DefIncrement | DefIndex | DefLAnd | DefLEqual | DefLGreater
    //                     | DefLGreaterEqual | DefLLess | DefLLessEqual | DefMid | DefLNot | DefLNotEqual | DefLoadTable | DefLOr | DefMatch | DefMod
    //                     | DefMultiply | DefNAnd | DefNOr | DefNot | DefObjectType | DefOr | DefPackage | DefVarPackage | DefRefOf | DefShiftLeft
    //                     | DefShiftRight | DefSizeOf | DefStore | DefSubtract | DefTimer | DefToBCD | DefToBuffer | DefToDecimalString | DefToHexString
    //                     | DefToInteger | DefToString | DefWait | DefXOr | MethodInvocation
    switch (current()) {
    case BufferOp:
        return parse_define_buffer();
    case PackageOp:
        return parse_define_package();
    default:
        return ENOTSUP;
    }
}

ErrorOr<NonnullRefPtr<ASTNode>> Parser::parse_object()
{
    // Object := NameSpaceModifierObj | NamedObj
    // NameSpaceModifierObj := DefAlias | DefName | DefScope
    // NamedObj := DefField | DefBankField | DefCreateBitField | DefCreateByteField | DefCreateDWordField | DefCreateField | DefCreateQWordField | DefMethod
    //             | DefCreateWordField | DefDataRegion | DefExternal | DefOpRegion | DefPowerRes | DefProcessor | DefThermalZone | DefMutex | DefDevice
    switch (current()) {
    case NameOp:
        return parse_define_name();
    case ScopeOp:
        return parse_define_scope();
    case MethodOp:
        return parse_define_method();
    case ExtOpPrefix: {
        skip();
        switch (current()) {
        case MutexOp:
            return parse_define_mutex();
        case OpRegionOp:
            return parse_define_operation_region();
        case FieldOp:
            return parse_define_field();
        case DeviceOp:
            return parse_define_device();
        case ProcessorOp:
            return parse_define_processor();
        default:
            m_offset--; // undo the previous skip, to let the parent try to parse it instead
            return ENOTSUP;
        }
    }
    default:
        return ENOTSUP;
    }
}

ErrorOr<NonnullRefPtr<ASTNode>> Parser::parse_term_argument()
{
    // TermArg := ExpressionOpcode | DataObject | ArgObj | LocalObj
    // TODO: Parse ArgObj | LocalObj

    auto maybe_data_object = TRY_IF_MATCHES(parse_data_object());
    if (maybe_data_object.has_value())
        return maybe_data_object.release_value();

    return parse_expression_opcode();
}

ErrorOr<NonnullRefPtr<DefineName>> Parser::parse_define_name()
{
    // DefName := NameOp NameString DataRefObject
    skip();

    auto name = TRY(parse_name_string());
    auto object = TRY(parse_data_reference_object());

    auto path = TRY(resolve_path(name->view()));
    TRY(m_root_namespace.insert_node(path, object));

    return try_make_ref_counted<DefineName>(move(name), move(object));
}

ErrorOr<NonnullRefPtr<DefineScope>> Parser::parse_define_scope()
{
    // DefScope := ScopeOp PkgLength NameString TermList
    skip();

    auto current_offset = m_offset;
    auto length = parse_package_length();

    auto location = TRY(parse_name_string());

    auto expected_end_offset = current_offset + length;

    auto new_scope = TRY(resolve_path(location->view()));
    TRY(m_root_namespace.add_level(new_scope));

    auto previous_scope = exchange(m_current_scope, move(new_scope));
    auto terms = TRY(parse_term_list(expected_end_offset));
    m_current_scope = move(previous_scope);

    if (m_offset != expected_end_offset) {
        dbgln("AML Error: Expected end offset {} but found {}, correcting", expected_end_offset, m_offset);
        m_offset = expected_end_offset;
    }

    return try_make_ref_counted<DefineScope>(move(location), move(terms));
}

ErrorOr<NonnullRefPtr<DefineBuffer>> Parser::parse_define_buffer()
{
    // DefBuffer := BufferOp PkgLength BufferSize ByteList
    skip();

    auto current_offset = m_offset;
    auto length = parse_package_length();

    // BufferSize := TermArg => Integer
    auto size = TRY(parse_term_argument());

    auto expected_end_offset = current_offset + length;

    auto byte_list = TRY(parse_byte_list(expected_end_offset));

    if (m_offset != expected_end_offset) {
        dbgln("AML Error: Expected end offset {} but found {}, correcting", expected_end_offset, m_offset);
        m_offset = expected_end_offset;
    }

    return try_make_ref_counted<DefineBuffer>(move(size), move(byte_list));
}

ErrorOr<NonnullRefPtr<DefinePackage>> Parser::parse_define_package()
{
    // DefPackage := PackageOp PkgLength NumElements PackageElementList
    skip();

    auto current_offset = m_offset;
    auto length = parse_package_length();

    // NumElements := ByteData
    auto element_count = parse_byte_data();

    auto expected_end_offset = current_offset + length;

    auto element_list = TRY(parse_package_element_list(expected_end_offset));

    if (m_offset != expected_end_offset) {
        dbgln("AML Error: Expected end offset {} but found {}, correcting", expected_end_offset, m_offset);
        m_offset = expected_end_offset;
    }

    return try_make_ref_counted<DefinePackage>(element_count, move(element_list));
}

ErrorOr<NonnullRefPtr<DefineMethod>> Parser::parse_define_method()
{
    // DefMethod := MethodOp PkgLength NameString MethodFlags TermList
    skip();

    auto current_offset = m_offset;
    auto length = parse_package_length();

    auto name = TRY(parse_name_string());

    // MethodFlags := ByteData // bit 0-2: ArgCount (0-7)
    //                         // bit 3:   SerializeFlag
    //                         //            0 NotSerialized
    //                         //            1 Serialized
    //                         // bit 4-7: SyncLevel (0x00-0x0f)
    auto flags = consume();
    auto argument_count = flags & 0x7;
    auto serialized = (flags >> 3) & 0x1;
    auto synchronization_level = (flags >> 4) & 0xf;

    auto method_path = TRY(resolve_path(name->view()));
    TRY(m_root_namespace.add_level(method_path));

    // NOTE: Methods may contain method invocations, which can only be parsed given pre-existing knowledge of all static method definitions (unfortunately)
    // since these may appear after the actual method invocation, we have to defer parsing the method bodies until we parse all the static definitions.
    auto method = TRY(try_make_ref_counted<DefineMethod>(move(name), argument_count, serialized, synchronization_level, Vector<NonnullRefPtr<ASTNode>> {}));
    TRY(m_root_namespace.insert_node(method_path, method));

    auto expected_end_offset = current_offset + length;
    TRY(m_deferred_methods.try_append({ method,
        move(method_path),
        m_offset,
        expected_end_offset }));

    // Skip method body
    m_offset = expected_end_offset;

    return method;
}

ErrorOr<NonnullRefPtr<DefineMutex>> Parser::parse_define_mutex()
{
    // DefMutex := MutexOp NameString SyncFlags
    skip();

    auto name = TRY(parse_name_string());
    auto path = TRY(resolve_path(name->view()));

    // SyncFlags := ByteData // bit 0-3: SyncLevel (0x00-0x0f)
    //                       // bit 4-7: Reserved (must be 0)
    auto flags = parse_byte_data();
    auto synchronization_level = flags & 0xF;
    if ((flags >> 4) != 0) {
        dbgln("AML Error: Reserved sync flags field set");
        return EINVAL;
    }

    auto mutex = TRY(try_make_ref_counted<DefineMutex>(move(name), synchronization_level));
    TRY(m_root_namespace.insert_node(path, mutex));
    return mutex;
}

ErrorOr<NonnullRefPtr<DefineOperationRegion>> Parser::parse_define_operation_region()
{
    // DefOpRegion := OpRegionOp NameString RegionSpace RegionOffset RegionLen
    skip();

    auto name = TRY(parse_name_string());
    auto path = TRY(resolve_path(name->view()));

    // RegionSpace := ByteData // 0x00 SystemMemory
    //                         // 0x01 SystemIO
    //                         // 0x02 PCI_Config
    //                         // 0x03 EmbeddedControl
    //                         // 0x04 SMBus
    //                         // 0x05 SystemCMOS
    //                         // 0x06 PciBarTarget
    //                         // 0x07 IPMI
    //                         // 0x08 GeneralPurposeIO
    //                         // 0x09 GenericSerialBus
    //                         // 0x80-0xFF: OEM Defined
    auto region_space = static_cast<GenericAddressStructure::AddressSpace>(parse_byte_data());

    // RegionOffset := TermArg => Integer
    auto region_offset = TRY(parse_term_argument());

    // RegionLen := TermArg => Integer
    auto region_length = TRY(parse_term_argument());

    auto operation_region = TRY(try_make_ref_counted<DefineOperationRegion>(move(name), region_space, move(region_offset), move(region_length)));
    TRY(m_root_namespace.insert_node(path, operation_region));
    return operation_region;
}

ErrorOr<NonnullRefPtr<DefineField>> Parser::parse_define_field()
{
    // DefField := FieldOp PkgLength NameString FieldFlags FieldList
    skip();

    auto current_offset = m_offset;
    auto length = parse_package_length();

    auto operation_region_name = TRY(parse_name_string());

    // FieldFlags := ByteData // bit 0-3: AccessType
    //                          // 0 AnyAcc
    //                          // 1 ByteAcc
    //                          // 2 WordAcc
    //                          // 3 DWordAcc
    //                          // 4 QWordAcc
    //                          // 5 BufferAcc
    //                          // 6 Reserved
    //                          // 7-15 Reserved
    //                        // bit 4: LockRule
    //                          // 0 NoLock
    //                          // 1 Lock
    //                        // bit 5-6: UpdateRule
    //                          // 0 Preserve
    //                          // 1 WriteAsOnes
    //                          // 2 WriteAsZeros
    //                          // 3 Reserved
    //                        // bit 7: Reserved (must be 0)
    auto flags = parse_byte_data();
    auto access_type = flags & 0xF;
    if (access_type >= 6) {
        dbgln("AML Error: Reserved field access type");
        return EINVAL;
    }
    auto lock_rule = (flags >> 4) & 0x1;
    auto update_rule = (flags >> 5) & 0x3;
    if (update_rule == 3) {
        dbgln("AML Error: Reserved update rule type");
        return EINVAL;
    }
    if ((flags >> 7) != 0) {
        dbgln("AML Error: Reserved field flags bit set");
        return EINVAL;
    }

    auto expected_end_offset = current_offset + length;

    auto field_list = TRY(parse_field_list(expected_end_offset));

    if (m_offset != expected_end_offset) {
        dbgln("AML Error: Expected end offset {} but found {}, correcting", expected_end_offset, m_offset);
        m_offset = expected_end_offset;
    }

    return try_make_ref_counted<DefineField>(move(operation_region_name), static_cast<FieldAccessType>(access_type), static_cast<FieldLockRule>(lock_rule), static_cast<FieldUpdateRule>(update_rule), move(field_list));
}

ErrorOr<NonnullRefPtr<DefineDevice>> Parser::parse_define_device()
{
    // DefDevice := DeviceOp PkgLength NameString TermList
    skip();

    auto current_offset = m_offset;
    auto length = parse_package_length();

    auto name = TRY(parse_name_string());

    auto expected_end_offset = current_offset + length;

    auto new_scope = TRY(resolve_path(name->view()));
    TRY(m_root_namespace.add_level(new_scope));

    auto previous_scope = exchange(m_current_scope, move(new_scope));
    auto terms = TRY(parse_term_list(expected_end_offset));
    auto device_path = exchange(m_current_scope, move(previous_scope));

    if (m_offset != expected_end_offset) {
        dbgln("AML Error: Expected end offset {} but found {}, correcting", expected_end_offset, m_offset);
        m_offset = expected_end_offset;
    }

    auto device = TRY(try_make_ref_counted<DefineDevice>(move(name), move(terms)));
    TRY(m_root_namespace.insert_node(device_path, device));
    return device;
}

ErrorOr<NonnullRefPtr<DefineProcessor>> Parser::parse_define_processor()
{
    // DefProcessor := ProcessorOp PkgLength NameString ProcID PblkAddr PblkLen TermList
    skip();

    auto current_offset = m_offset;
    auto length = parse_package_length();

    auto name = TRY(parse_name_string());

    // ProcID := ByteData
    auto processor_id = parse_byte_data();

    // PblkAddr := DWordData
    auto processor_block_address = parse_dword_data();

    // PblkLen := ByteData
    auto processor_block_length = parse_byte_data();

    auto expected_end_offset = current_offset + length;

    auto new_scope = TRY(resolve_path(name->view()));
    TRY(m_root_namespace.add_level(new_scope));

    auto previous_scope = exchange(m_current_scope, move(new_scope));
    auto terms = TRY(parse_term_list(expected_end_offset));
    auto processor_path = exchange(m_current_scope, move(previous_scope));

    if (m_offset != expected_end_offset) {
        dbgln("AML Error: Expected end offset {} but found {}, correcting", expected_end_offset, m_offset);
        m_offset = expected_end_offset;
    }

    auto processor = TRY(try_make_ref_counted<DefineProcessor>(move(name), processor_id, processor_block_address, processor_block_length, move(terms)));
    TRY(m_root_namespace.insert_node(processor_path, processor));
    return processor;
}

ErrorOr<void> Parser::process_deferred_methods()
{
    bool encountered_errors = false;

    for (auto const& deferred_method : m_deferred_methods) {
        auto previous_offset = exchange(m_offset, deferred_method.start);

        auto previous_scope = move(m_current_scope);
        m_current_scope = TRY(deferred_method.scope.clone());

        auto terms_or_error = parse_term_list(deferred_method.end);
        if (terms_or_error.is_error()) {
            dbgln_if(AML_DEBUG, "AML Error: Failed to parse method {}", deferred_method.scope);
            encountered_errors = true;
        } else {
            if (m_offset != deferred_method.end)
                dbgln("AML Error: Expected end offset {} but found {}", deferred_method.end, m_offset);

            deferred_method.method->set_terms({}, terms_or_error.release_value());
        }

        m_current_scope = move(previous_scope);
        m_offset = previous_offset;
    }

    if (encountered_errors) {
        dbgln("AML Error: Some methods failed to parse");
    }

    m_deferred_methods.clear();
    return {};
}

ErrorOr<void> Parser::populate_namespace()
{
    auto terms = TRY(parse_term_list(m_bytecode.size()));
    TRY(process_deferred_methods());
    if constexpr (AML_DEBUG) {
        auto block = TRY(try_make_ref_counted<DefinitionBlock>(move(terms)));
        block->dump(0);
    }
    return {};
}

}
