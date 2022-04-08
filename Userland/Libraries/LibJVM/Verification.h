#pragma once

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

private:
    VerificationKind m_kind;
    union {
        short cpool_index;
        short offset;
    };
};

}
