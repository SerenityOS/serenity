#pragma once
#include <LibJVM/Attributes.h>
#include <LibJVM/ConstantPool.h>
#include <AK/FixedArray.h>
#include <AK/Utf8View.h>

namespace JVM {

struct FieldInfo {
    short access_flags;
    short name_index;
    short descriptor_index;
    FixedArray<AttributeInfo> attributes;
};

struct MethodInfo {
    short access_flags;
    short name_index;
    short descriptor_index;
    FixedArray<AttributeInfo> attributes;
};

class Class {
public:
    bool load_from_file(AK::StringView path, bool check_file);
    bool verify_const_pool();
    CPEntry cp_entry(short index) {
        VERIFY( !(index >= m_constant_pool.size() || index == 0) );
        return m_constant_pool[index - 1]; //The Constant Pool is indexed starting from 1.
    }

private:
    short m_minor_version { 45 };
    short m_major_version { 0 };
    FixedArray<CPEntry> m_constant_pool;
    short access_flags { 0 };
    short m_this_class_index { 0 };
    short m_super_class_index { 0 };
    FixedArray<short> m_interfaces;
    FixedArray<FieldInfo> m_fields;
    FixedArray<MethodInfo> m_methods;
    FixedArray<AttributeInfo> m_attributes;

};

}
