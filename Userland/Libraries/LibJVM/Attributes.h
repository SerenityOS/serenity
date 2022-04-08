#pragma once
#include <AK/FixedArray.h>
#include <LibJVM/Forward.h>

namespace JVM {

struct Exception {
    short start_pc;
    short end_pc;
    short handler_pc;
    short catch_type;
};

//Yes, this is different that the 'Exception' struct right above it.
//This should be named differently to more easily differentiate it from 'Exception', but I don't know what to call it as of now.
struct Exceptions {

};

struct Code {
    short max_stack;
    short max_locals;
    AK::FixedArray<u8> code;
    AK::FixedArray<Exception> exception_table;
    AK::FixedArray<AttributeInfo> attributes;
};

struct StackMapTable {

};

enum class AttributeKind {
    ConstantValue,
    Code,
    StackMapTable,
    BootstrapMethods,
    NestHost,
    NestMembers,
    PermittedSubclasses,
    Execptions,
    InnerClasses,
    EnclosingMethod,
    Synthetic,
    Signature,
    Record,
    SourceFile, //optional
    LineNumberTable, //optional
    LocalVariableTable, //optional
    LocalVariableTypeTable, //optional
    SourceDebugExtention, //optional
    Deprecated, //optional
    RuntimeVisibleAnnotations,
    RuntimeInvisibleAnnotations,
    RuntimeVisibleParameterAnnotations,
    RuntimeInvisibleParameterAnnotations,
    RuntimeVisibleTypeAnnotations,
    RuntimeInvisibleTypeAnnotations,
    AnnotationDefault,
    MethodParameters,
    Module,
    ModulePackages,
    ModuleMainClass,
    Custom, //This isn't an attribute predefined by the spec, but instead a representation of a custom attribute that some compilers may emit as part of the .class file.
    //As of the writing of this comment, I have no plans to implement this, and this is simply there to catch custom attributes and ignore them.
};

class AttributeInfo {

public:

private:
    AttributeKind m_kind;
    union {
        short constantvalue_index;
        Code code;
        StackMapTable sm_table;
        Exceptions exceptions;

    } m_value;
};

}
