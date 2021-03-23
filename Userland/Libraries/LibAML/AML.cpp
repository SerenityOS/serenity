/*
 * Copyright (c) 2021, the SerenityOS developers.
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
#define AML_DEBUG 1
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include "AML.h"

namespace AML {

#define AML_TODO() \
    do {           \
        dbgln("AML: Not implemented at {}:{}", __FILE__, __LINE__); \
        VERIFY_NOT_REACHED(); \
    } while (0)

OpCode CodeStream::do_read_op(const u8*& current) const
{
    dbgln_if(AML_DEBUG, "AML: reading op code at offset {:x}", current - m_bytes);
    if (current >= m_end)
        return OpCode::Invalid;
    u8 op = *(current++);
    if (op == 0x5b) {
        if (current >= m_end)
            return OpCode::Invalid;
        return (OpCode)((op << 8) | *(current++));
    }
    if (op == 0x92 && current < m_end) {
        u8 next_op = *current;
        if (next_op >= 0x93 && next_op <= 0x95) {
            current++;
            return (OpCode)((op << 8) | next_op);
        }
    }
    return (OpCode)op;
}

PkgLength CodeStream::do_read_pkg_length(const u8*& current) const
{
    if (current >= m_end)
        return {};
    const u8* pkg_start = current;
    u8 lead_byte = *(current++);
    u8 following_bytes = (lead_byte >> 6) & 3;
    if (following_bytes == 0)
        return PkgLength(pkg_start, lead_byte & 0x3f);
    u32 length = 0;
    for (u8 i = 0; i < following_bytes; i++) {
        length |= (u32)(*(current++)) << (i * 8);
        if (current >= m_end)
            return {};
    }
    length <<= 4;
    length |= lead_byte & 0xf;
    return PkgLength(pkg_start, length);
}

String CodeStream::do_read_name_string(const u8*& current) const
{
    if (current >= m_end)
        return {};
    const u8* str_start = current;
    while (*current == '\\' || *current == '^') {
        if (++current >= m_end)
            return {};
    }
    StringBuilder str;
    if (current != str_start)
        str.append(reinterpret_cast<const char*>(str_start), current - str_start);

    int names;
    switch (*current) {
    case '\0':
        // NullName
        if (++current > m_end)
            return {};
        names = 0;
        break;
    case 0x2e:
        // DualNamePrefix
        if (++current >= m_end)
            return {};
        names = 2;
        break;
    case 0x2f: {
        // MultiNamePrefix
        if (++current >= m_end)
            return {};
        names = *(current++);
        if (names == 0 || current >= m_end)
            return {};
        break;
    }
    default:
        // One NameSeg
        names = 1;
        break;
    }
    const u8* str_end = current + (names * 4);
    if (str_end > m_end)
        return {};
    if (names > 0)
        str.append(reinterpret_cast<const char*>(current), str_end - current);
    current = str_end;
    return str.build();
}

Node* CodeStreamContext::find_node_ensure_path(const StringView& path, Node*& parent, StringView& node_name)
{
    if (auto* scope_node = node_by_path(path)) {
        dbgln("find_node_ensure_path {} found node {:p} (type 0x{:x}) {}", path, scope_node, (unsigned)scope_node->node_type(), scope_node->pretty_path());
        parent = scope_node->parent();
        node_name = scope_node->name();
        return scope_node;
    }
    node_name = path_basename(path);
    if (node_name.is_null() || node_name.is_empty()) {
        dbgln_if(AML_DEBUG, "AML: Failed to determine basename from {}", path);
        parent = nullptr;
        return nullptr;
    }

    parent = ensure_node_parents(path);
    if (!parent)
        dbgln_if(AML_DEBUG, "AML: Failed to ensure parents");
    return nullptr;
}

bool CodeStreamContext::read_termlist()
{
    while (stream().has_bytes()) {
        const u8* current = stream().current_bytes();
        auto op = stream().read_op();
        switch (op) {
        case OpCode::Invalid:
            dbgln_if(AML_DEBUG, "AML: TermList failed to read op code at offset 0x{:x}", stream().byte_offset(current));
            return false;
        case OpCode::AliasOp: {
            auto name = stream().read_name_string();
            if (name.is_null()) {
                dbgln_if(AML_DEBUG, "DefAlias failed to read name");
                return false;
            }
            auto target = stream().read_name_string();
            if (target.is_null()) {
                dbgln_if(AML_DEBUG, "DefAlias failed to read target");
                return false;
            }
            auto* target_node = node_by_path(target);
            if (!target_node) {
                dbgln_if(AML_DEBUG, "DefAlias failed to resolve target: {}", target);
                return false;
            }
            Node* parent = nullptr;
            StringView node_name;
            auto* alias_node = find_node_ensure_path(name, parent, node_name);
            if (!alias_node && parent)
                alias_node = parent->add_child(adopt_own(*new Alias(node_name, *target_node)));
            if (!alias_node)
                return false;
            break;
        }
        case OpCode::ScopeOp: {
            auto pkg_length = stream().read_pkg_length();
            if (!pkg_length.is_valid()) {
                dbgln_if(AML_DEBUG, "DefScope failed to read pkg length");
                return false;
            }
            auto name = stream().read_name_string();
            if (name.is_null()) {
                dbgln_if(AML_DEBUG, "DefScope failed to read name");
                return false;
            }
            Node* parent = nullptr;
            StringView node_name;
            auto* scope_node = find_node_ensure_path(name, parent, node_name);
            if (!scope_node && parent)
                scope_node = parent->add_child(adopt_own(*new Namespace(node_name)));
            if (!scope_node)
                return false;
            CodeStreamContext::ScopedChangeScope new_scope(*this, *scope_node);
            dbgln("DefScope {} package 0x{:x}-0x{:x} -->", scope_node->pretty_path(), stream().byte_offset(pkg_length.start()), stream().byte_offset(pkg_length.end()));
            auto parse_result = with_package(pkg_length, [&] {
                return read_termlist();
            });
            dbgln("<-- DefScope {}", scope_node->pretty_path());
            if (!parse_result)
                return false;
            break;
        }
        case OpCode::MethodOp:
            if (!parse<Method>()) {
                dbgln_if(AML_DEBUG, "AML: TermList failed to parse DefMethod at offset 0x{:x}", stream().byte_offset(current));
                return false;
            }
            break;
        case OpCode::DeviceOp:
            if (!parse<Device>()) {
                dbgln_if(AML_DEBUG, "AML: TermList failed to parse DefDevice at offset 0x{:x}", stream().byte_offset(current));
                return false;
            }
            break;
        case OpCode::RegionOp:
            if (!parse<Region>()) {
                dbgln_if(AML_DEBUG, "AML: TermList failed to parse DefRegion at offset 0x{:x}", stream().byte_offset(current));
                return false;
            }
            break;
        case OpCode::FieldOp: {
            auto pkg_length = stream().read_pkg_length();
            if (!pkg_length.is_valid()) {
                dbgln_if(AML_DEBUG, "DefField failed to read pkg length");
                return false;
            }
            auto target_name = stream().read_name_string();
            if (target_name.is_null()) {
                dbgln_if(AML_DEBUG, "DefField failed to read target name");
                return false;
            }
            auto field_flags = stream().read_value<u8>();
            if (!field_flags.has_value()) {
                dbgln_if(AML_DEBUG, "DefField failed to read flags");
                return false;
            }
            u8 access_type_raw = field_flags.value() & 0xf;
            if (access_type_raw > 5)
                return false;
            auto access_type = (Field::AccessType)access_type_raw;
            bool do_lock = false;
            if (field_flags.value() & (1 << 4))
                do_lock = true;
            u8 update_rule_raw = (field_flags.value() >> 5) & 3;
            if (update_rule_raw > 2)
                return false;
            auto update_rule = (Field::UpdateRule)update_rule_raw;

            auto* target_node = node_by_path(target_name);
            if (!target_node) {
                dbgln_if(AML_DEBUG, "DefField cannot resolve target {}", target_name);
                return false;
            }

            dbgln("DefField package 0x{:x}-0x{:x} -->", stream().byte_offset(pkg_length.start()), stream().byte_offset(pkg_length.end()));
            bool parse_result = with_package(pkg_length, [&] {
                return read_fieldlist(access_type, [&](const String& name, u32 offset) {
                    Node* parent = nullptr;
                    StringView node_name;
                    auto* existing_node = find_node_ensure_path(name, parent, node_name);
                    if (existing_node) {
                        dbgln_if(AML_DEBUG, "DefField: Field with name {} already defined", name);
                        return false;
                    } else if (!parent) {
                        dbgln_if(AML_DEBUG, "DefField: Field failed to find scope for name {}", name);
                        return false;
                    }
                    ScopedChangeScope new_scope(*this, *parent);
                    auto* field_node = parse<Field>(node_name, access_type, do_lock, update_rule);
                    if (!field_node) {
                        dbgln_if(AML_DEBUG, "DefField: Failed to add field {}", name);
                        return false;
                    }
                    dbgln("  Added field: {} at offset {} path: {}", name, offset, field_node->pretty_path());
                    return true;
                });
            });
            dbgln("<-- DefField");
            if (!parse_result)
                return false;
            break;
        }
        default:
            // Try reading a name, it may be a method call
            stream().unread_op(op);
            if (Type2Opcode::parse(*this))
                return true;

            dbgln_if(AML_DEBUG, "AML: TermList did not handle op code 0x{:x} at offset 0x{:x}", (unsigned)op, stream().byte_offset(current));
            return false;
        }
    }
    return true;
}

bool CodeStreamContext::read_fieldlist(Field::AccessType& access_type, Function<bool(const String&, u32)> f)
{
    u32 offset = 0;
    while (stream().has_bytes()) {
        auto first_byte = stream().read_value<u8>();
        if (!first_byte.has_value())
            return false;
        switch (first_byte.value()) {
        case 0x0: {
            // ReservedField
            auto pkg_length = stream().read_pkg_length();
            if (!pkg_length.is_valid()) {
                dbgln_if(AML_DEBUG, "DefField failed to read pkg length");
                return false;
            }
            offset += pkg_length.length();
            break;
        }
        case 0x1: {
            // AccessField
            auto new_access_type = stream().read_value<u8>();
            if (!new_access_type.has_value())
                return false;
            auto new_access_attrib = stream().read_value<u8>();
            if (!new_access_attrib.has_value())
                return false;
            auto new_access_type_raw = new_access_type.value() & 0xf;
            if (new_access_type_raw > 5)
                return false;
            access_type = (Field::AccessType)new_access_type_raw;
            switch ((new_access_type_raw >> 6) & 3) {
            case 0:
                // AccessAttrib = Normal Access Attributes
                break;
            case 1:
                // AccessAttrib = AttribBytes (x)
                dbgln("AML: Field AccessAttrib AttribBytes({}) not implemented!", new_access_attrib.value());
                break;
            case 2:
                // AccessAttrib = AttribRawBytes (x)
                dbgln("AML: Field AccessAttrib AttribRawBytes({}) not implemented!", new_access_attrib.value());
                break;
            case 3:
                // AccessAttrib = AttribRawProcessBytes (x)
                dbgln("AML: Field AccessAttrib AttribRawProcessBytes({}) not implemented!", new_access_attrib.value());
                break;
            default:
                break;
            }
            break;
        }
        case 0x2: {
            // ConnectField
            AML_TODO();
            break;
        }
        case 0x3: {
            // ExtendedAccessField
            AML_TODO();
            break;
        }
        default: {
            // NamedField
            stream().unread_value<u8>();
            auto name = stream().read_name_string();
            if (name.is_null()) {
                dbgln_if(AML_DEBUG,"DefField failed to read NamedField");
                return false;
            }
            auto pkg_length = stream().read_pkg_length();
            if (!pkg_length.is_valid()) {
                dbgln_if(AML_DEBUG, "DefField failed to read NamedField pkg length");
                return false;
            }
            if (!f(name, offset))
                return false;
            break;
        }
        }
    }
    return true;
}

bool ComputationalData::load(CodeStreamContext& context)
{
    auto op = context.stream().peek_op();
    if (op == OpCode::Invalid)
        return false;
    auto read_basic_value = [&]<typename BasicType, DataType data_type>() {
        context.stream().skip_op(op);
        auto value = context.stream().read_value<BasicType>();
        if (!value.has_value())
            return false;
        m_type = data_type;
        m_value = value.value();
        return true;
    };
    switch (op) {
    case OpCode::ZeroOp:
        context.stream().skip_op(op);
        m_value = 0;
        m_type = DataType::ConstObj;
        break;
    case OpCode::OneOp:
        context.stream().skip_op(op);
        m_type = DataType::ConstObj;
        m_value = 1;
        break;
    case OpCode::BufferOp: {
        CodeStreamContext::ScopedChangeScope new_scope(context, *this);
        auto buffer = context.parse<DefBuffer, true>();
        if (!buffer)
            return false;
        m_type = DataType::Buffer;
        m_buffer = buffer;
        break;
    }
    case OpCode::BytePrefix:
        if (!read_basic_value.template operator()<u8, DataType::U8>())
            return false;
        break;
    case OpCode::WordPrefix:
        if (!read_basic_value.template operator()<u16, DataType::U16>())
            return false;
        break;
    case OpCode::DWordPrefix:
        if (!read_basic_value.template operator()<u32, DataType::U32>())
            return false;
        break;
    case OpCode::StringPrefix: {
        context.stream().skip_op(op);
        const u8* str_start = context.stream().current_bytes();
        size_t length = 0;
        for (;;) {
            auto byte = context.stream().read_value<u8>();
            if (!byte.has_value())
                return false;
            if (byte.value() == 0)
                break;
            length++;
        }
        m_string = new StringView(ReadonlyBytes(str_start, length));
        break;
    }
    case OpCode::QWordPrefix:
        if (!read_basic_value.template operator()<u64, DataType::U64>())
            return false;
        break;
    case OpCode::OnesOp:
        context.stream().skip_op(op);
        m_type = DataType::ConstObj;
        m_value = ~0ull;
        break;
    case OpCode::RevisionOp:
        context.stream().skip_op(op);
        m_type = DataType::Revision;
        break;
    default:
        return false;
    }
    return true;
}

ComputationalData::~ComputationalData()
{
    switch (m_type) {
    case DataType::String:
        if (m_string)
            delete m_string;
        break;
    case DataType::Buffer:
        if (m_buffer)
            delete m_buffer;
        break;
    default:
        break;
    }
}

bool MethodInvocation::load(CodeStreamContext& context)
{
    const u8* prev_current = context.stream().current_bytes();
    ArmedScopeGuard guard([&] {
        context.stream().set_current_bytes(prev_current);
    });
    auto name = context.stream().read_name_string();
    if (!name.is_null()) {
        auto* node = context.node_by_path(name);
        if (!node) {
            dbgln_if(AML_DEBUG, "MethodInvocation: Failed to resolve: {}", name);
            return false;
        }
        if (node->node_type() == NodeType::Method) {
            m_method = static_cast<Method*>(node);

            auto arg_count = m_method->arg_count();
            dbgln_if(AML_DEBUG, "MethodInvocation: Method call to {} with {} args -->", m_method->pretty_path(), arg_count);
            ScopeGuard log_guard([&] {
                dbgln_if(AML_DEBUG, "<-- TermArg: Method call to {}", m_method->pretty_path());
            });
            for (unsigned i = 0; i < arg_count; i++) {
                if (!context.parse<TermArg>()) {
                    dbgln_if(AML_DEBUG, "MethodInvocation: Failed to parse argument {}/{} for call to {}", i + 1, arg_count, m_method->pretty_path());
                    return false;
                }
            }
            dbgln_if(AML_DEBUG, "MethodInvocation: Call {}({})", m_method->pretty_path(), [&] {
                StringBuilder str;
                bool first = true;
                for_each_child([&](const Node& child) {
                    if (node->node_type() != NodeType::TermArg)
                        return IterationDecision::Continue;
                    const TermArg& arg = static_cast<const TermArg&>(child);
                    if (first)
                        first = false;
                    else
                        str.append(", ");
                    str.append(arg.pretty_debug());
                    return IterationDecision::Continue;
                });
                return str.build();
            }());
            guard.disarm();
            return true;
        }
    }
    return false;
}

bool ArgObj::load(CodeStreamContext& context)
{
    auto op = context.stream().read_op();
    if (op == OpCode::Invalid)
        return false;
    if (op >= OpCode::Arg0Op && op <= OpCode::Arg6Op) {
        m_index = (int)op - (int)OpCode::Arg0Op;
        return true;
    }
    context.stream().unread_op(op);
    return false;
}

bool LocalObj::load(CodeStreamContext& context)
{
    auto op = context.stream().read_op();
    if (op == OpCode::Invalid)
        return false;
    if (op >= OpCode::Local0Op && op <= OpCode::Local7Op) {
        m_index = (int)op - (int)OpCode::Local0Op;
        return true;
    }
    context.stream().unread_op(op);
    return false;
}

bool SimpleName::load(CodeStreamContext& context)
{
    m_arg = context.parse<ArgObj>();
    if (m_arg)
        return true;
    m_local = context.parse<LocalObj>();
    if (m_local)
        return true;
    auto name = context.stream().read_name_string();
    if (!name.is_null()) {
        m_target = context.node_by_path(name);
        return m_target != nullptr;
    }
    return false;
}

bool SuperName::load(CodeStreamContext& context)
{
    m_simple_name = context.parse<SimpleName>();
    if (m_simple_name)
        return true;
    auto op = context.stream().read_op();
    if (op == OpCode::Invalid)
        return false;
    if (op == OpCode::DebugOp) {
        m_debug_obj = true;
        return true;
    }
    if (auto* reference = Type6Opcode::parse(context, op)) {
        m_reference = reference;
        return true;
    }
    context.stream().unread_op(op);
    return false;
}

bool Target::load(CodeStreamContext& context)
{
    auto first_byte = context.stream().read_value<u8>();
    if (!first_byte.has_value())
        return false;
    if (first_byte.value() == 0) {
        // NullName
        return true;
    }
    context.stream().unread_value<u8>();
    return SuperName::load(context);
}

bool RefOf::load(CodeStreamContext& context)
{
    m_target = context.parse<SuperName>();
    return m_target != nullptr;
}

bool DerefOf::load(CodeStreamContext& context)
{
    m_obj_ref = context.parse<TermArg>();
    // TODO: validate ObjectReference
    return m_obj_ref != nullptr;
}

bool Index::load(CodeStreamContext& context)
{
    m_buf_pkg_str = context.parse<TermArg>();
    // TODO: Validate BuffPkgStrObj
    if (!m_buf_pkg_str)
        return false;
    m_index = context.parse<TermArg>();
    if (!m_index)
        return false;
    m_target = context.parse<Target>();
    return m_target != nullptr;
}

bool AcquireMutex::load(CodeStreamContext& context)
{
    m_mutex = context.parse<SuperName>();
    auto timeout = context.stream().read_value<u16>();
    if (!timeout.has_value())
        return false;
    m_timeout = timeout.value();
    return m_mutex;
}

Node* Type6Opcode::parse(CodeStreamContext& context, OpCode op)
{
    switch (op) {
    case OpCode::RefOfOp:
        if (auto* node = context.parse<RefOf>())
            return node;
        break;
    case OpCode::DerefOfOp:
        if (auto* node = context.parse<DerefOf>())
            return node;
        break;
    case OpCode::IndexOp:
        if (auto* node = context.parse<Index>())
            return node;
        break;
    default:
        // TODO: UserTermObj ?
        break;
    }
    return nullptr;
}

Node* Type2Opcode::parse(CodeStreamContext& context)
{
    if (auto* method_invocation = context.parse<MethodInvocation>())
        return method_invocation;
    auto op = context.stream().read_op();
    if (op == OpCode::Invalid)
        return nullptr;
    switch (op) {
    case OpCode::AcquireOp:
        if (auto* node = context.parse<AcquireMutex>())
            return node;
        dbgln_if(AML_DEBUG, "Type2Opcode: DefAcquire failed");
        break;
    //case OpCode::ToHexStringOp:
    //    if (auto* node = context.parse<ToHexString>())
    //        return node;
    //    dbgln_if(AML_DEBUG, "Type2Opcode: DefToHexString failed");
    //    break;
    default:
        if (auto* node = Type6Opcode::parse(context, op))
            return node;
        dbgln_if(AML_DEBUG, "Type2Opcode did not handle op 0x{:x}", (unsigned)op);
        break;
    }
    context.stream().unread_op(op);
    return nullptr;
}

String TermArg::pretty_debug() const
{
    switch (m_arg_type) {
    case ArgType::ComputationalData:
        return String::formatted("[TermArg {}]", m_data->pretty_debug());
    case ArgType::MethodInvocation:
        return String::formatted("[TermArg: {}]", m_method_invocation->pretty_debug());
    case ArgType::ValueOrReference:
        return "[TermArg: ValueOrReference]";
    case ArgType::ArgObj:
        return String::formatted("[TermArg: {}]", m_arg_obj->pretty_debug());
    case ArgType::LocalObj:
        return String::formatted("[TermArg: {}]", m_local_obj->pretty_debug());
    default:
        return "[TermArg: Invalid]";
    }
}

bool TermArg::load(CodeStreamContext& context)
{
    if (auto* node = context.parse<ComputationalData>(false)) {
        m_arg_type = ArgType::ComputationalData;
        m_data = node;
        return true;
    }
    if (auto* node = Type2Opcode::parse(context)) {
        if (node->node_type() == NodeType::MethodInvocation) {
            m_arg_type = ArgType::MethodInvocation;
            m_method_invocation = static_cast<Method*>(node);
            return true;
        }
        m_arg_type = ArgType::ValueOrReference;
        return true;
    }
    if (auto* node = context.parse<ArgObj>()) {
        m_arg_type = ArgType::ArgObj;
        m_arg_obj = node;
        return true;
    }
    if (auto* node = context.parse<LocalObj>()) {
        m_arg_type = ArgType::LocalObj;
        m_local_obj = node;
        return true;
    }
    return false;
}

bool DefBuffer::load(CodeStreamContext& context)
{
    auto pkg_length = context.stream().read_pkg_length();
    if (!pkg_length.is_valid())
        return false;
    context.stream().set_current_bytes(pkg_length.end());
    return m_arg;
}

bool Device::load(CodeStreamContext& context)
{
    auto pkg_length = context.stream().read_pkg_length();
    if (!pkg_length.is_valid()) {
        dbgln_if(AML_DEBUG, "DefDevice failed to read pkg length");
        return false;
    }
    auto name = context.stream().read_name_string();
    if (name.is_null()) {
        dbgln_if(AML_DEBUG, "DefDevice failed to read name");
        return false;
    }

    Node* parent = nullptr;
    StringView node_name;
    auto* existing_node = context.find_node_ensure_path(name, parent, node_name);
    if (existing_node) {
        dbgln_if(AML_DEBUG, "DefDevice with name {} already defined", name);
        return false;
    } else if (!parent) {
        dbgln_if(AML_DEBUG, "DefDevice failed to find scope for name {}", name);
        return false;
    }
    set_name(node_name);

    dbgln("DefDevice {} package 0x{:x}-0x{:x} -->", m_name, context.stream().byte_offset(pkg_length.start()), context.stream().byte_offset(pkg_length.end()));
    bool parse_result = context.with_package(pkg_length, [&] {
        return context.read_termlist();
    });
    dbgln("<-- DefDevice {}", m_name);

    context.set_scope(*parent); // We want to be added to parent
    return parse_result;
}

bool Region::load(CodeStreamContext& context)
{
    auto name = context.stream().read_name_string();
    if (name.is_null()) {
        dbgln_if(AML_DEBUG, "DefRegion failed to read name");
        return false;
    }
    auto space = context.stream().read_value<u8>();
    if (!space.has_value()) {
        dbgln_if(AML_DEBUG, "DefRegion failed to read space");
        return false;
    }
    m_space = space.value();
    if (m_space <= (u8)RegionSpace::LastDefined) {
        m_region_space = (RegionSpace)m_space;
    } else {
        m_region_space = RegionSpace::OEMDefined;
    }
    auto* region_offset = context.parse<ComputationalData>(true);
    if (!region_offset) {
        dbgln_if(AML_DEBUG, "DefRegion failed to read region offset");
        return false;
    }
    auto* region_length = context.parse<ComputationalData>(true);
    if (!region_length) {
        dbgln_if(AML_DEBUG, "DefRegion failed to read region length");
        return false;
    }

    Node* parent = nullptr;
    StringView node_name;
    auto* existing_node = context.find_node_ensure_path(name, parent, node_name);
    if (existing_node) {
        dbgln_if(AML_DEBUG, "DefRegion with name {} already defined", name);
        return false;
    } else if (!parent) {
        dbgln_if(AML_DEBUG, "DefRegion failed to find scope for name {}", name);
        return false;
    }
    set_name(node_name);

    dbgln_if(AML_DEBUG, "DefRegion {} at {:p}, path: {}", node_name, this, pretty_path());

    context.set_scope(*parent); // We want to be added to parent
    return true;
}

bool Method::load(CodeStreamContext& context)
{
    auto pkg_length = context.stream().read_pkg_length();
    if (!pkg_length.is_valid()) {
        dbgln_if(AML_DEBUG, "DefMethod failed to read pkg length");
        return false;
    }
    auto name = context.stream().read_name_string();
    if (name.is_null()) {
        dbgln_if(AML_DEBUG, "DefMethod failed to read name");
        return false;
    }
    auto flags = context.stream().read_value<u8>();
    if (!flags.has_value()) {
        dbgln_if(AML_DEBUG, "DefMethod failed to read flags");
        return false;
    }
    Node* parent = nullptr;
    StringView node_name;
    auto* existing_node = context.find_node_ensure_path(name, parent, node_name);
    if (existing_node) {
        dbgln_if(AML_DEBUG, "DefMethod with name {} already defined", name);
        return false;
    } else if (!parent) {
        dbgln_if(AML_DEBUG, "DefMethod failed to find scope for name {}", name);
        return false;
    }
    set_name(node_name);
    m_flags = flags.value();

    // Save pointers to terms for delayed parsing
    m_terms = context.stream().current_bytes();
    m_terms_end = pkg_length.end();

    context.set_scope(*parent); // We want to be added to parent
    context.stream().set_current_bytes(pkg_length.end()); // Skip the terms until we need to actually parse them
    return true;
}

bool Method::parse_terms(CodeStreamContext& context)
{
    return context.stream().within_substream(m_terms, m_terms_end, [&] {
        dbgln("DefMethod {} package 0x{:x}-0x{:x} -->", m_name, context.stream().byte_offset(m_terms), context.stream().byte_offset(m_terms_end));

        // Clear terms pointer, we only want to parse them once!
        m_terms = nullptr;

        bool parse_result = context.read_termlist();
        dbgln("<-- DefMethod {}", m_name);

        // Now also clear the end pointer, this indicates we're done
        // parsing. This allows us to detect recursion into ourselves
        // while parsing
        m_terms_end = nullptr;
        return parse_result;
    });
}

String Node::pretty_path() const
{
    if (!parent())
        return "\\";
    Vector<Node*, 32> nodes;
    for (const Node* node = this; node && node->parent(); node = node->parent())
        nodes.append(const_cast<Node*>(node));
    size_t nodes_count = nodes.size();
    StringBuilder str;
    for (size_t i = nodes_count; i > 0; i--) {
        str.append('\\');
        str.append(nodes[i - 1]->name());
    }
    return str.build();
}

String Node::pretty_debug() const
{
    return String::formatted("[Node: type 0x{:x}]", (unsigned)node_type());
}

bool Node::evaluate(CodeEvaluationContext& context) const
{
    dbgln_if(AML_DEBUG, "AML: Evaluate node {} {:p} children: {} -->", (unsigned)m_type, this, m_children.size());
    for_each_child([&](const Node& node) {
        dbgln("AML: Evaluate child {:p}...", &node);
        if (!node.evaluate(context))
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    dbgln_if(AML_DEBUG, "AML: <-- Evaluate node {} {:p}", (unsigned)m_type, this);
    return true;
}

CodeEvaluationContext::CodeEvaluationContext(const Node& node)
{
    node.evaluate(*this);
}

CodeStreamContext::CodeStreamContext(CodeStream& stream, OwnPtr<Node>& root)
    : m_root(root)
    , m_stream(&stream)
{
    m_root = adopt_own(*new Namespace("ROOT"));
    m_root->add_child(adopt_own(*new OSIMethod()));
    TemporaryChange change(m_scope, m_root.ptr());
    if (!read_termlist())
        m_root = nullptr;
}

Node* CodeStreamContext::node_by_path(const StringView& path)
{
    if (path.is_null() || path.is_empty())
        return nullptr;
    dbgln("node_by_path {}", path);
    auto path_length = path.length();
    VERIFY(m_scope);
    Node* current = m_scope;
    size_t i = 0;
    if (path[0] == '\\') {
        current = m_root.ptr();
        VERIFY(current);
        i++;
    }
    while (i < path_length && path[i] == '^') {
        if (auto* parent = current->parent())
            current = parent;
        i++;
    }
    bool can_search = (i == 0 && path_length == 4);
    while (i + 4 <= path_length) {
        auto component = path.substring_view(i, 4);
        if (auto* child = current->child_by_name(component)) {
            dbgln("component {} = {:p} {}", component, child, child->pretty_path());
            current = child;
        } else if (can_search) {
            while (current->parent()) {
                current = current->parent();
                child = current->child_by_name(component);
                if (child)
                    return child;
            }
            dbgln("searching for component {} found nothing", component);
            return nullptr;
        } else {
            dbgln("component {} not found", component);
            return nullptr;
        }
        i += 4;
    }
    return current;
}

Node* CodeStreamContext::ensure_node_parents(const StringView& path)
{
    if (path.is_null() || path.is_empty())
        return nullptr;
    auto path_length = path.length();
    Node* current = m_scope;
    size_t i = 0;
    if (path[0] == '\\') {
        current = m_root.ptr();
        i++;
    }
    while (i < path_length && path[i] == '^') {
        if (auto* parent = current->parent())
            current = parent;
        i++;
    }
    auto remaining_length = path_length - i;
    if (remaining_length <= 4)
        return current;
    path_length -= 4;

    // NOTE: we're not searching here
    while (i + 4 <= path_length) {
        auto component = path.substring_view(i, 4);
        if (auto* child = current->child_by_name(component)) {
            current = child;
        } else {
            current = current->add_child(adopt_own(*new Namespace(component)));
        }
        i += 4;
    }
    return current;
}

StringView CodeStreamContext::path_basename(const StringView& path)
{
    if (path.is_null() || path.is_empty())
        return {};
    auto path_length = path.length();
    size_t i = 0;
    if (path[0] == '\\')
        i++;
    while (i < path_length && path[i] == '^')
        i++;
    auto remaining_length = path_length - i;
    if (remaining_length < 4)
        return String::empty();
    return path.substring_view(path_length - 4, 4);
}

CodeTable::CodeTable(const u8* bytes, size_t size)
{
    if (size <= sizeof(DefBlockHeader)) {
        dbgln_if(AML_DEBUG, "AML: Not enough bytes for table: {}", size);
        return;
    }

    const DefBlockHeader& header = *reinterpret_cast<const DefBlockHeader*>(bytes);
    m_name = StringView(ReadonlyBytes(reinterpret_cast<const u8*>(&header.table_signature), sizeof(header.table_signature)));
    dbgln_if(AML_DEBUG, "AML: Parsing table {} with {} bytes", m_name, header.table_length - sizeof(header));

    m_bytes = bytes + sizeof(header);
    m_end = m_bytes + size - sizeof(header);
}

bool CodeTable::evaluate()
{
    CodeStream code(m_bytes, m_end - m_bytes);
    CodeStreamContext context(code, m_root);
    if (!m_root)
        dbgln_if(AML_DEBUG, "AML: Failed to parse table {}", m_name);
    return m_root;
}

String CodeTable::dump_namespace() const
{
    if (!m_root)
        return {};
    StringBuilder builder;
    dump_node(*m_root, builder);
    return builder.build();
}

void CodeTable::dump_node(const Node& node, StringBuilder& builder, size_t indent) const
{
    auto node_type_string = [](NodeType node_type) {
        switch (node_type) {
        case NodeType::Method:
            return "method";
        case NodeType::Device:
            return "device";
        default:
            return "";
        }
    };
    bool dump_children = false;
    switch (node.node_type()) {
    case NodeType::Namespace:
        builder.appendff("{}\\{}\n", String::repeated(' ', indent), node.name());
        dump_children = true;
        break;
    case NodeType::Device:
    case NodeType::Method:
        builder.appendff("{}{} ({})\n", String::repeated(' ', indent), node.name(), node_type_string(node.node_type()));
        dump_children = true;
        break;
    default:
        break;
    }

    if (dump_children && node.has_children()) {
        TemporaryChange change(indent, indent + 4);
        node.for_each_child([&](const Node& child_node) {
            dump_node(child_node, builder, indent);
            return IterationDecision::Continue;
        });
    }
}

}
