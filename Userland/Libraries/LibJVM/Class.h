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
private:
    short m_minor_version { 45 };
    short m_major_version { 0 };
    FixedArray<CPEntry> constant_pool;
    short access_flags { 0 };
    short this_class_index { 0 };
    short super_class_index { 0 };
    FixedArray<short> interfaces;
    FixedArray<FieldInfo> fields;
    FixedArray<MethodInfo> methods;
    FixedArray<AttributeInfo> attributes;

};

}
