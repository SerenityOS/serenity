#pragma once
#include <AK/Assertions.h>

namespace JVM {

enum class VerificationKind
{
    Top,
    Integer,
    Float,
    Double,
    Long,
    Null,
    UninitializedThis,
    Object,
    UninitializedVariable,

};

class VerificationType {
public:
    VerificationType(VerificationKind kind, short val)
        :m_kind(kind)
    {
        if(kind == VerificationKind::Object) {
            m_value.cpool_index = val;
        }
        else if(kind == VerificationKind::UninitializedVariable) {
            m_value.offset = val;
        }
        else { VERIFY_NOT_REACHED(); }
    }
    VerificationType(VerificationKind kind)
        : m_kind(kind)
    {}
private:
    VerificationKind m_kind;
    union {
        short cpool_index;
        short offset;
    } m_value;
};

}
