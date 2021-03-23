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

#pragma once

#include <AK/Function.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Types.h>

namespace AML {

class IHost {
public:
    virtual ~IHost() = default;
};

class CodeStream;
class CodeStreamContext;
class CodeEvaluationContext;

enum class OpCode : i32 {
    Invalid = -1,
    ZeroOp = 0x0,
    OneOp = 0x1,
    AliasOp = 0x6,
    NameOp = 0x8,
    ScopeOp = 0x10,
    BufferOp = 0x11,
    MethodOp = 0x14,
    BytePrefix = 0x0a,
    WordPrefix = 0x0b,
    DWordPrefix = 0x0c,
    StringPrefix = 0x0d,
    QWordPrefix = 0x0e,
    Local0Op = 0x60,
    Local7Op = 0x67,
    Arg0Op = 0x68,
    Arg6Op = 0x6e,
    RefOfOp = 0x71,
    DerefOfOp = 0x83,
    IndexOp = 0x88,
    ToHexStringOp = 0x98,
    OnesOp = 0xff,
    AcquireOp = 0x5b23,
    DebugOp = 0x5b31,
    RegionOp = 0x5b80,
    FieldOp = 0x5b81,
    DeviceOp = 0x5b82,
    RevisionOp = 0x5b30,
};

enum class NodeType {
    Namespace,
    Alias,
    Device,
    Method,
    MethodInvocation,
    Region,
    Field,
    SimpleName,
    SuperName,
    Target,
    RefOf,
    DerefOf,
    Index,
    ComputationalData,
    AcquireMutex,
    ToHexString,
    TermArg,
    DataRefObject,
    // TODO: get rid of these below
    DefBuffer,
    ArgObj,
    LocalObj,

};

class Alias;

class Node {
public:
    virtual ~Node()
    {
        dbgln("~Node() @ {:p}", this);
    }
    virtual bool load(CodeStreamContext&) = 0;
    virtual String pretty_debug() const;
    virtual bool evaluate(CodeEvaluationContext&) const;

    Node* add_child(NonnullOwnPtr<Node> child)
    {
        child->set_parent(this);
        auto* child_ptr = child.ptr();
        m_children.append(move(child));
        return child_ptr;
    }

    String pretty_path() const;
    Node* parent() const { return m_parent; }

    template<typename F>
    IterationDecision for_each_child(F f) const
    {
        for (const auto& child : m_children) {
            IterationDecision decision = f(child);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    Node* child_by_name(const StringView& name);

    bool has_children() const { return !m_children.is_empty(); }
    size_t child_count() const { return m_children.size(); }

    NodeType node_type() const { return m_type; }
    const String& name() const { return m_name; }

protected:
    Node(NodeType type)
        : m_type(type)
    {
        dbgln("Node() @ {:p} type 0x{:x}", this, (unsigned)m_type);
    }
    Node(NodeType type, const String& name)
        : m_type(type)
        , m_name(name)
    {
        dbgln("Node() @ {:p} type 0x{:x} name {}", this, (unsigned)m_type, name);
    }

    void set_name(String&& name) { m_name = move(name); }
    void set_parent(Node* parent) { m_parent = parent; }

    NodeType m_type;
    NonnullOwnPtrVector<Node, 2> m_children;
    Node* m_parent { nullptr };
    String m_name;
};

class TermArg;
class ComputationalData;

class Namespace : public Node {
public:
    Namespace(const StringView& name)
        : Node(NodeType::Namespace, name)
    {
    }
    virtual bool load(CodeStreamContext&) override { return false; }
};

class Alias : public Node {
public:
    Alias(const StringView& name, Node& target)
        : Node(NodeType::Alias, name)
        , m_target(target)
    {
    }
    virtual bool load(CodeStreamContext&) override { return false; }

    Node& target() { return m_target; }
    const Node& target() const { return m_target; }
private:
    Node& m_target;
};

inline Node* Node::child_by_name(const StringView& name)
{
    for (auto& child : m_children) {
        if (child.name() == name) {
            if (child.node_type() == NodeType::Alias) {
                auto& alias = static_cast<Alias&>(child);
                return &alias.target();
            } else {
                return &child;
            }
        }
    }
    return nullptr;
}

class Device : public Node {
public:
    Device()
        : Node(NodeType::Device)
    {
    }
    virtual bool load(CodeStreamContext&) override;
};

class Region : public Node {
public:
    Region()
        : Node(NodeType::Region)
    {
    }
    virtual bool load(CodeStreamContext&) override;

    enum class RegionSpace : u8 {
        SystemMemory = 0x0,
        SystemIO = 0x1,
        PCIConfig = 0x2,
        EmbeddedControl = 0x3,
        SMBus = 0x4,
        SystemCMOS = 0x5,
        PICBarTarget = 0x6,
        IPMI = 0x7,
        GeneralPurposeIO = 0x8,
        GenericSerialBus = 0x9,
        PCC = 0xa,
        LastDefined = PCC,

        OEMDefined = 0x80
    };
    RegionSpace space() const { return m_region_space; }
    u8 space_raw() const { return m_space; }
private:
    RegionSpace m_region_space { RegionSpace::SystemMemory };
    u8 m_space { 0 };
};

class Field : public Node {
public:
    enum class UpdateRule : u8 {
        Preserve = 0x0,
        WriteAsOnes = 0x1,
        WriteAsZeros = 0x2,
    };
    enum class AccessType : u8 {
        Any = 0x0,
        U8 = 0x1,
        U16 = 0x2,
        U32 = 0x3,
        U64 = 0x4,
        Buffer = 0x5,
    };
    Field(StringView name, AccessType access_type, bool do_lock, UpdateRule update_rule)
        : Node(NodeType::Field, name)
        , m_access_type(access_type)
        , m_do_lock(do_lock)
        , m_update_rule(update_rule)
    {
    }
    virtual bool load(CodeStreamContext&) override { return true; }

    AccessType access_type() const { return m_access_type; }
    bool do_lock() const { return m_do_lock; }

    UpdateRule update_rule() const { return m_update_rule; }
private:
    AccessType m_access_type { AccessType::Any };
    bool m_do_lock { false };
    UpdateRule m_update_rule { UpdateRule::Preserve };
};

class DataRefObject : public Node {
public:
    DataRefObject()
        : Node(NodeType::DataRefObject)
    {
    }
    virtual bool load(CodeStreamContext&) override;
    static Node* parse(OpCode, CodeStreamContext&);
};

class DefBuffer : public Node {
public:
    DefBuffer()
        : Node(NodeType::DefBuffer)
    {
    }
    virtual bool load(CodeStreamContext&) override;
private:
    Node* m_arg { nullptr };
};

class MethodBase : public Node {
protected:
    MethodBase()
        : Node(NodeType::Method)
    {
    }
};

class OSIMethod : public MethodBase {
public:
    OSIMethod()
        : MethodBase()
    {
        set_name("_OSI");
    }
    virtual bool load(CodeStreamContext&) override { return true; }
};

class Method : public MethodBase {
public:
    Method()
        : MethodBase()
    {
    }
    virtual bool load(CodeStreamContext&) override;

    unsigned arg_count() const { return m_flags & 7; }
    bool is_serialized() const { return (m_flags & (1 << 3)) != 0; }
    unsigned sync_level() const { return (m_flags >> 4) & 0xf; }

    bool is_parsed() const { return !m_terms && !m_terms_end; }
    bool is_parsing() const { return !m_terms && m_terms_end; }

    bool parse_terms(CodeStreamContext&);
private:
    u8 m_flags { 0 };
    const u8* m_terms { nullptr };
    const u8* m_terms_end { nullptr };
};

class MethodInvocation : public Node {
public:
    MethodInvocation()
        : Node(NodeType::MethodInvocation)
    {
    }
    virtual bool load(CodeStreamContext&) override;

    const Method& method() const { return *m_method; }
private:
    Method* m_method { nullptr };
};

struct Type2Opcode {
    static Node* parse(CodeStreamContext&);
};

struct Type6Opcode {
    static Node* parse(CodeStreamContext&, OpCode);
};

class ArgObj : public Node {
public:
    ArgObj()
        : Node(NodeType::ArgObj)
    {
    }
    virtual bool load(CodeStreamContext&) override;

    int index() const { return m_index; }
private:
    int m_index { 0 };
};

class LocalObj : public Node {
public:
    LocalObj()
        : Node(NodeType::LocalObj)
    {
    }
    virtual bool load(CodeStreamContext&) override;

    int index() const { return m_index; }
private:
    int m_index { 0 };
};

class SimpleName : public Node {
public:
    SimpleName()
        : Node(NodeType::SimpleName)
    {
    }
    virtual bool load(CodeStreamContext&) override;
private:
    Node* m_target { nullptr };
    ArgObj* m_arg { nullptr };
    LocalObj* m_local { nullptr };
};

class SuperName : public Node {
public:
    SuperName()
        : Node(NodeType::SuperName)
    {
    }
    virtual bool load(CodeStreamContext&) override;
protected:
    SuperName(NodeType node_type)
        : Node(node_type)
    {
    }
    SimpleName* m_simple_name { nullptr };
    Node* m_reference { nullptr };
    bool m_debug_obj { false };
};

class Target : public SuperName {
public:
    Target()
        : SuperName(NodeType::Target)
    {
    }
    virtual bool load(CodeStreamContext&) override;
    bool is_null() const { return !m_simple_name & !m_debug_obj; }
};

class RefOf : public Node {
public:
    RefOf()
        : Node(NodeType::RefOf)
    {
    }
    virtual bool load(CodeStreamContext&) override;
private:
    SuperName* m_target { nullptr };
};

class DerefOf : public Node {
public:
    DerefOf()
        : Node(NodeType::DerefOf)
    {
    }
    virtual bool load(CodeStreamContext&) override;
private:
    TermArg* m_obj_ref { nullptr };
};

class Index : public Node {
public:
    Index()
        : Node(NodeType::Index)
    {
    }
    virtual bool load(CodeStreamContext&) override;
private:
    TermArg* m_buf_pkg_str { nullptr };
    TermArg* m_index { nullptr };
    Target* m_target { nullptr };
};

class AcquireMutex : public Node {
public:
    AcquireMutex()
        : Node(NodeType::AcquireMutex)
    {
    }
    virtual bool load(CodeStreamContext&) override;
private:
    SuperName* m_mutex { nullptr };
    u16 m_timeout { 0 };
};

class TermArg : public Node {
public:
    TermArg()
        : Node(NodeType::TermArg)
    {
    }
    virtual bool load(CodeStreamContext&) override;
    virtual String pretty_debug() const override;
    enum class ArgType {
        Invalid = 0,
        ComputationalData,
        MethodInvocation,
        ValueOrReference,
        ArgObj,
        LocalObj
    };
    ArgType arg_type() const { return m_arg_type; }

private:
    ArgType m_arg_type { ArgType::Invalid };
    union {
        void* m_ptr { nullptr };
        ComputationalData* m_data;
        Method* m_method_invocation;
        ArgObj* m_arg_obj;
        LocalObj* m_local_obj;
    };
};

class ComputationalData : public Node {
public:
    ComputationalData(bool static_only)
        : Node(NodeType::ComputationalData)
        , m_static_only(static_only)
    {
    }

    enum class DataType
    {
        Invalid = 0,
        U8,
        U16,
        U32,
        U64,
        String,
        ConstObj,
        Revision,
        Buffer,
    };
    virtual bool load(CodeStreamContext&) override;
    virtual ~ComputationalData();
private:
    DataType m_type { DataType::Invalid };
    bool m_static_only { false };
    union {
        u64 m_value;
        StringView *m_string;
        DefBuffer *m_buffer;
    };
};

class PkgLength {
public:
    PkgLength() = default;
    PkgLength(const u8* start, u32 length)
        : m_start(start)
        , m_length(length)
    {
    }

    bool is_valid() const { return m_start; }
    const u8* start() const { return m_start; }
    const u8* end() const { return m_start + m_length; }
    u32 length() const { return m_length; }
private:
    const u8* m_start { nullptr };
    u32 m_length { 0 };
};

class CodeStream {
public:
    explicit CodeStream(const u8* bytes, size_t size)
        : m_bytes(bytes)
        , m_current(bytes)
        , m_end(bytes + size)
    {
    }
    OpCode read_op()
    {
        return do_read_op(m_current);
    }
    OpCode peek_op() const
    {
        const u8* current = m_current;
        return do_read_op(current);
    }
    void unread_op(OpCode op)
    {
        VERIFY(op != OpCode::Invalid);
        m_current -= ((unsigned)op <= 0xff) ? 1 : 2;
    }
    void skip_op(OpCode op)
    {
        VERIFY(op != OpCode::Invalid);
        m_current += ((unsigned)op <= 0xff) ? 1 : 2;
    }
    template<typename BasicType>
    Optional<BasicType> peek_value() const
    {
        if (m_current + sizeof(BasicType) > m_end) // TODO: Overflow
            return {};
        if constexpr (sizeof(BasicType) == 1) {
            return *m_current;
        } else {
            BasicType value;
            __builtin_memcpy(&value, m_current, sizeof(BasicType));
            return value;
        }
    }
    template<typename BasicType>
    Optional<BasicType> read_value()
    {
        auto value = peek_value<BasicType>();
        skip_value<BasicType>();
        return value;
    }
    template<typename BasicType>
    void skip_value()
    {
        m_current += sizeof(BasicType);
    }
    template<typename BasicType>
    void unread_value()
    {
        m_current -= sizeof(BasicType);
    }
    PkgLength read_pkg_length()
    {
        const u8* prev_current = m_current;
        auto pkg = do_read_pkg_length(m_current);
        if (pkg.is_valid())
            return pkg;
        m_current = prev_current;
        return {};
    }
    String read_name_string()
    {
        const u8* prev_current = m_current;
        auto name = do_read_name_string(m_current);
        if (!name.is_null())
            return name;
        m_current = prev_current;
        return {};
    }
    const u8* current_bytes() const { return m_current; }
    void set_current_bytes(const u8* current)
    {
        VERIFY(current >= m_bytes && current <= m_end);
        m_current = current;
    }
    bool has_bytes() const { return m_current < m_end; }
    size_t current_byte_offset() const { return byte_offset(m_current); }
    size_t byte_offset(const u8* byte) const
    {
        VERIFY(byte >= m_bytes && byte <= m_end);
        return byte - m_bytes;
    }

    template<typename F>
    auto within_substream(const u8* begin, const u8* end, F f)
    {
        const u8* prev_current = exchange(m_current, begin);
        const u8* prev_end = exchange(m_end, end);
        ScopeGuard guard([&] {
            m_current = prev_current;
            m_end = prev_end;
        });
        return f();
    }

protected:
    OpCode do_read_op(const u8*& current) const;
    PkgLength do_read_pkg_length(const u8*& current) const;
    String do_read_name_string(const u8*& current) const;

    const u8* m_bytes { nullptr };
    const u8* m_current { nullptr };
    const u8* m_end { nullptr };
};

class CodeEvaluationContext {
public:
    CodeEvaluationContext(const Node&);
};

class CodeStreamContext {
public:
    explicit CodeStreamContext(CodeStream&, OwnPtr<Node>&);

    template<typename NodeType, bool need_read_op = false, typename... Args>
    NodeType* parse(Args&&... args)
    {
        VERIFY(m_scope);
        const u8* previous_bytes = stream().current_bytes();
        auto* previous_scope = m_scope;
        ScopeGuard guard([&] {
            m_scope = previous_scope;
        });
        if constexpr(need_read_op)
            stream().read_op();
        auto node = adopt_own(*new NodeType(forward<Args>(args)...));
        if (!previous_scope)
            m_scope = node.ptr();
        if (!node->load(*this)) {
            stream().set_current_bytes(previous_bytes);
            return nullptr;
        }
        if (previous_scope) {
            auto* node_ptr = node.ptr();
            dbgln("AML: Add node {:p} name: {} to scope {:p} {}", node_ptr, node->name(), m_scope, m_scope->pretty_path());
            m_scope->add_child(move(node));
            return node_ptr;
        }
        return node.leak_ptr();
    }

    class ScopedChangeScope {
    public:
        ScopedChangeScope(CodeStreamContext& context, Node& node)
            : m_context(context)
            , m_previous(exchange(context.m_scope, &node))
        {
        }
        ~ScopedChangeScope()
        {
            m_context.m_scope = m_previous;
        }
    private:
        CodeStreamContext& m_context;
        Node* m_previous { nullptr };
    };
    friend class NodeScope;

    void set_scope(Node& node) { m_scope = &node; }

    static StringView path_basename(const StringView&);
    Node* find_node_ensure_path(const StringView&, Node*&, StringView&);
    Node* node_by_path(const StringView&);
    Node* ensure_node_parents(const StringView&);

    CodeStream& stream() { return *m_stream; }
    const CodeStream& stream() const { return *m_stream; }

    template<typename F>
    auto with_package(const PkgLength& pkg, F f)
    {
        ScopeGuard guard([&] {
            stream().set_current_bytes(pkg.end());
        });
        return stream().within_substream(stream().current_bytes(), pkg.end(), f);
    }

    bool read_termlist();
    bool read_fieldlist(Field::AccessType&, Function<bool(const String&, u32)>);

private:
    OwnPtr<Node>& m_root;
    Node* m_scope { nullptr };
    CodeStream* m_stream { nullptr };
};

class CodeTable {
public:
    struct [[gnu::packed]] DefBlockHeader {
        u32 table_signature;
        u32 table_length;
        u8 spec_compliance;
        u8 checksum;
        u8 oem_id[6];
        u8 oem_table_id[8];
        u32 oem_revision;
        u32 creator_id;
        u32 creator_revision;
    };
    static_assert(sizeof(DefBlockHeader) == 36);

    CodeTable() = default;
    CodeTable(const CodeTable&) = delete;
    CodeTable(CodeTable&& other)
        : m_name(move(other.m_name))
        , m_bytes(exchange(other.m_bytes, nullptr))
        , m_end(exchange(other.m_end, nullptr))
        , m_root(move(other.m_root))
    {
    }
    explicit CodeTable(const u8*, size_t);

    CodeTable& operator=(const CodeTable&) = delete;
    CodeTable& operator=(CodeTable&& other)
    {
        if (this != &other) {
            m_name = move(other.m_name);
            m_bytes = exchange(other.m_bytes, nullptr);
            m_end = exchange(other.m_end, nullptr);
            m_root = move(other.m_root);
        }
        return *this;
    }

    bool evaluate();

    bool is_null() const { return !m_bytes || !m_root; }
    const String& name() const { return m_name; }

private:
    String dump_namespace() const;
    void dump_node(const Node&, StringBuilder&, size_t indent = 0) const;

    String m_name;
    const u8* m_bytes { nullptr };
    const u8* m_end { nullptr };
    OwnPtr<Node> m_root;
};

}