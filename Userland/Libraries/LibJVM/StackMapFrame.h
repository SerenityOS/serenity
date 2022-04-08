#pragma once

#include <LibJVM/Verification.h>
#include <AK/Forward.h>

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
protected:
    short m_offset_delta;
    u8 m_tag;
};

class SameFrame : StackMapFrame {
public:
    SameFrame (u8 tag) {
        m_tag = tag;
        m_offset_delta = tag;
    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::Same;
    }
};

class SameLocals1StackItemFrame : StackMapFrame {
public:
    SameLocals1StackItemFrame (u8 tag, VerificationType entry_type) {
        m_tag = tag;
        m_offset_delta = tag - 64;
        m_entry_type = entry_type;
    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::SameLocals1StackItem;
    }
private:
    VerificationType m_entry_type;
};

class SameLocals1StackItemFrameExtended : StackMapFrame {
public:
    SameLocals1StackItemFrameExtended (u8 tag, short offset_delta, VerificationType entry_type) {

    }

    StackMapFrameKind kind() const override {
        return StackMapFrameKind::SameLocals1StackItemExtended;
    }
private:
    VerificationType m_entry_type;
};

}
