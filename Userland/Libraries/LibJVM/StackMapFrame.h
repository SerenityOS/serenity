#pragma once

#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/Forward.h>
#include <AK/FixedArray.h>
#include <AK/NonnullOwnPtr.h>
#include <LibJVM/Verification.h>



namespace JVM {

enum class StackMapFrameKind {
    Same,
    SameLocals1StackItem,
    SameLocals1StackItemExtended,
    Chop,
    SameExtended,
    Append,
    Full,
};

class StackMapFrame {
public:
    virtual StackMapFrameKind kind() const;
    virtual short offset_delta() const {
        return m_offset_delta;
    };
    virtual ~StackMapFrame();
protected:
    short m_offset_delta;
    u8 m_tag;
};

class SameFrame final : public StackMapFrame {
public:
    SameFrame (u8 tag) {
        m_tag = tag;
        m_offset_delta = tag;
    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::Same;
    }
};

class SameLocals1StackItemFrame final : public StackMapFrame {
public:
    SameLocals1StackItemFrame (u8 tag, VerificationType only_local) {
        m_tag = tag;
        m_offset_delta = tag - 64;
        m_only_local = only_local;
    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::SameLocals1StackItem;
    }
private:
    VerificationType  m_only_local;
};

class SameLocals1StackItemFrameExtended final : public StackMapFrame {
public:
    SameLocals1StackItemFrameExtended (u8 tag, short offset_delta, VerificationType only_local) {
        m_tag = tag;
        m_offset_delta = offset_delta;
        m_only_local = only_local;
    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::SameLocals1StackItemExtended;
    }
private:
    VerificationType  m_only_local;
};

class ChopFrame final : public StackMapFrame {
public:
    ChopFrame (u8 tag, short offset_delta) {
        m_tag = tag;
        m_offset_delta = offset_delta;
        m_chopped = 251 - tag;
    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::Chop;
    }
private:
    u8 m_chopped;
};

class SameFrameExtended final : public StackMapFrame {
public:
    SameFrameExtended (u8 tag, short offset_delta) {
        m_offset_delta = offset_delta;
    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::SameExtended;
    }
};

//These next 2 classes build FixedArrays out of variable-sized arrays, but don't verify that the memory was allocated properly.
//FIXME: implement 'try_create' functions for these types instead of using the dirty constructors.


class AppendFrame final : public StackMapFrame {
public:
    AppendFrame (u8 tag, short offset_delta, AK::Span<VerificationType> locals) {
        m_offset_delta = offset_delta;
        m_tag = tag;
        m_additional_locals.try_create(locals);
    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::Append;
    }
private:
    FixedArray<VerificationType> m_additional_locals;
};

class FullFrame final : public StackMapFrame {
public:
    FullFrame (u8 tag, short offset_delta, AK::Span<VerificationType> locals, AK::Span<VerificationType> stack) {
        m_tag = tag;
        m_offset_delta = offset_delta;
        m_locals.try_create(locals);
        m_stack.try_create(stack);
    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::Full;
    }
private:
    FixedArray<VerificationType> m_locals;
    FixedArray<VerificationType> m_stack;
};

struct StackMapTable {
    AK::FixedArray<StackMapFrame> frames;
    StackMapTable() {

    }
    ~StackMapTable() {
        frames.~FixedArray();
    }
    StackMapTable(StackMapTable const &other) {
        frames.try_create(other.frames.span());
    }

    StackMapTable& operator=(StackMapTable const &other) {
        frames.try_create(other.frames.span());
        return *this;
    }

};

}
