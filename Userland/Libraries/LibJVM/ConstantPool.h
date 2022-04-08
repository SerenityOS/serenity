#pragma once
#include <AK/Assertions.h>
#include <AK/FixedArray.h>
#include <AK/Utf8View.h>
#include <AK/OwnPtr.h>

namespace JVM {


enum class ReferenceKind {
    RefGetField,
    RefGetStatic,
    RefPutField,
    RefPutStatic,
    RefInvokeVirtual,
    RefInvokeStatic,
    RefInvokeSpecial,
    RefNewInvokeSpecial,
    RefInvokeInterface,
};
struct Utf8Info {
    short length;
    const char* bytes;
};

struct MethodHandleInfo {
    ReferenceKind ref_kind;
    short ref_index;
};

enum class ConstantKind {
    //The filler types exist because the tags for ConstantKinds aren't sequential: there are gaps.
    //This enum class has fillers so that tags are correctly mapped.
    //The fillers are unused, but are different than the unusable tag.
    Utf8,
    Filler1,
    Integer,
    Float,
    Long,
    Double,
    Class,
    String,
    FieldRef,
    MethodRef,
    InterfaceMethodRef,
    NameAndType,
    MethodHandle,
    Filler2,
    Filler3,
    Filler4,
    MethodType,
    Dynamic,
    InvokeDynamic,
    Module,
    Package,
    Unusable, //This is a custom kind introduced to account for the fact that long and double are mandated to occupy 2 entries, even though they only use one.
    //It's a very weird decision, but this implementation follows it for now.
};

//What should we when someone passes an illegal combination of some values and a ConsantKind?
//I don't know enought about c++ to fix this, but I know that what we do currently is not correct.
//FIXME: Read the previous 2 comments and fix these constructors please.

class CPEntry {
public:

    explicit CPEntry(ConstantKind constant_kind, short value)
        : m_kind(constant_kind)
    {
        if (constant_kind == ConstantKind::Class) {
            m_value.class_info = value;
        }

        else if (constant_kind == ConstantKind::String) {
            m_value.string_info = value;
        }

        else if (constant_kind == ConstantKind::MethodType) {
            m_value.method_type_info = value;
        }

        else if (constant_kind == ConstantKind::Module) {
            m_value.module_info = value;
        }

        else if (constant_kind == ConstantKind::Package) {
            m_value.package_info = value;
        }

    }

    explicit CPEntry(ConstantKind constant_kind, short value[2])
        : m_kind(constant_kind)
    {
        auto array_assign = [](short a[2], short b[2]) { a[0] = b[0]; a[1] = b[1]; };
        if (constant_kind == ConstantKind::FieldRef || constant_kind == ConstantKind::MethodRef || constant_kind == ConstantKind::InterfaceMethodRef) {
            array_assign(m_value.ref_info, value);
        }

        else if (constant_kind == ConstantKind::NameAndType) {
            array_assign(m_value.name_and_type_info, value);
        }

        else if (constant_kind == ConstantKind::Dynamic || constant_kind == ConstantKind::InvokeDynamic) {
            array_assign(m_value.dynamic_info, value);
        }
    }

    explicit CPEntry(ConstantKind constant_kind, int value)
        : m_kind(constant_kind)
    {
        if (constant_kind == ConstantKind::Integer) {
            m_value.int_info = value;
        }

        else if (constant_kind == ConstantKind::Float) {
            m_value.float_info = *((float*)&value);
        }
    }

    explicit CPEntry(ConstantKind constant_kind, long long value)
        : m_kind(constant_kind)
    {
        if (constant_kind == ConstantKind::Long) {
            m_value.long_info = value;
        }

        else if (constant_kind == ConstantKind::Double) {

           m_value.double_info = *((double*)&value);
        }
    }

    explicit CPEntry(short length, const char* value)
        : m_kind(ConstantKind::Utf8)
    {
        m_value.utf8_info = {length, value};
    }

    explicit CPEntry(MethodHandleInfo value)
        : m_kind(ConstantKind::MethodHandle)
    {
        m_value.method_handle_info = value;
    }

    explicit CPEntry(ConstantKind constant_kind) //This is used for the 'Unused' ConstantKind.
        : m_kind(constant_kind)
    {    }

    ConstantKind kind() const { return m_kind; }


private:
    ConstantKind m_kind;
    union {
        short class_info;
        short ref_info[2]; //Used for FieldRef, MethodRef, and InterfaceRef.
        short string_info;
        int int_info;
        float float_info;
        long long long_info;
        double double_info;
        short name_and_type_info[2];
        Utf8Info utf8_info;
        MethodHandleInfo method_handle_info;
        short method_type_info;
        short dynamic_info[2]; //Used for both Dynamic and InvokeDynamic.
        short module_info;
        short package_info;
    } m_value;
};
}
