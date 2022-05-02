#pragma once
#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/Utf8View.h>
#include <LibJVM/Forward.h>
#include <LibJVM/Module.h>
#include <LibJVM/StackMapFrame.h>
#include <LibJVM/Verification.h>

//FIXME: Remove useless structs and merge structs that are only used in another struct into nameless structs.
//FIXME: replace all 'short's with 'unsigned short's.

namespace JVM {


struct ConstantValue {
    short constant_value_index;
};

struct Exception {
    short start_pc;
    short end_pc;
    short handler_pc;
    short catch_type;
};

struct ExceptionTable {
    AK::FixedArray<short> exception_index_table;
};

struct Code {
    short max_stack;
    short max_locals;
    AK::FixedArray<u8> code;
    AK::FixedArray<Exception> exception_table;
    AK::FixedArray<AttributeInfo> attributes;
};

struct InnerClass {
    short inner_class_info_index;
    short outer_class_info_index;
    short inner_name_index;
    short inner_class_access_flags;
};

struct InnerClassTable {
    AK::FixedArray<InnerClass> classes;
};

struct EnclosingMethod {
    short class_index;
    short method_index;
};

struct Synthetic {
};

struct Signature {
    short sig_index;
};

struct SourceFile {
    short sourcefile_index;
};

struct SourceDebugExtension {
    AK::Utf8View debug_extension;
};

struct LineNumber {
    short start_pc;
    short line_number;
};

struct LineNumberTable {
    AK::FixedArray<LineNumber> line_number_table;
};

struct LocalVariable {
    short start_pc;
    short length;
    short name_index;
    short descriptor_index;
    short index;
};

struct LocalVariableTable {
    AK::FixedArray<LocalVariable> local_variable_table;
};

struct LocalVariableType {
    short start_pc;
    short length;
    short name_index;
    short signature_index;
    short index;
};

struct LocalVariableTypeTable {
    AK::FixedArray<LocalVariableType> local_variable_type_table;
};

struct Deprecated {
};

struct EnumConstValue {
    unsigned short type_name_index;
    unsigned short const_name_index;
};

struct ElementValue {
    char tag;
    union Val {
        unsigned short const_value_index;
        EnumConstValue enum_const_value;
        unsigned short class_info_index;
        AK::NonnullOwnPtr<JVM::Annotation> annotation_value; //This has to be a pointer to resolve circular dependencies
        //Technically, this could be circular, but in reality it will end when an ElementValue doesn't use the annotation_value.
        AK::FixedArray<ElementValue>* array_value;
        Val() {
            const_value_index = 0;
        }
        ~Val() {
            annotation_value.~NonnullOwnPtr();
        }

    } value;
    ElementValue()
        : tag(0)
    {
        value.const_value_index = 0;
    }

    ElementValue(const ElementValue &other) {
        if ((tag > 65 && tag < 91) || tag == 115) { //Ascii codes.
            value.const_value_index = other.value.const_value_index;
        }
        else if (tag == 'e') {
           value.enum_const_value = other.value.enum_const_value;
        }
        else if (tag == 'c') {
            value.class_info_index = other.value.class_info_index;
        }
        else if (tag == '@') {
            value.annotation_value = make<Annotation>(other.value.annotation_value);
            //We should confirm that we still hold on to the value of new_annot after leaving this function.
        }
        else if (tag == '[') {
            auto array = other.value.array_value->must_clone_but_fixme_should_propagate_errors();
            value.array_value = &(array);
        }
        else {
            VERIFY_NOT_REACHED();
        }
    }

    ElementValue& operator= (ElementValue& other) {
        if ((tag > 65 && tag < 91) || tag == 115) { //Ascii codes.
            other.value.const_value_index = this->value.const_value_index;
        }
        else if (tag == 'e') {
           other.value.enum_const_value = this->value.enum_const_value;
        }
        else if (tag == 'c') {
            other.value.class_info_index = this->value.class_info_index;
        }
        else if (tag == '@') {
            other.value.annotation_value = make<Annotation>(this->value.annotation_value);
            //We should confirm that we still hold on to the value of new_annot after leaving this function.
        }
        else if (tag == '[') {
            auto array = value.array_value->must_clone_but_fixme_should_propagate_errors();
            other.value.array_value = &(array);
        }
        else {
            VERIFY_NOT_REACHED();
        }
    }
};

struct ElementValuePair {
    short element_name_index;
    ElementValue value;
};

struct Annotation {
    short type_index;
    AK::FixedArray<ElementValuePair> element_value_pairs;
};

struct RuntimeVisibleAnnotations {
    AK::FixedArray<Annotation> annotations;
};

struct RuntimeInvisibleAnnotations {
    AK::FixedArray<Annotation> annotations;
};

struct ParameterAnnotations {
    AK::FixedArray<Annotation> annotations;
};

struct RuntimeVisibleParameterAnnotations {
    AK::FixedArray<ParameterAnnotations> parameter_annotations;
};

struct RuntimeInvisibleParameterAnnotations {
    AK::FixedArray<ParameterAnnotations> parameter_annotations;
};

struct PathEntry {
    u8 type_path_kind;
    u8 type_argument_index;
};

struct TypePath {
    u8 path_length;
    AK::FixedArray<PathEntry> path;
};

struct Localvar_Target{
    short start_pc;
    short length;
    short index;
};

struct TypeAnnotation {
    u8 target_type;
    union {
        u8 type_parameter_target;
        short supertype_target;
        struct {
            u8 type_parameter_index;
            u8 bound_index;
        } type_parameter_bound_target;
        u8 formal_parameter_target;
        short throws_target;
        AK::NonnullOwnPtr<AK::FixedArray<Localvar_Target>> local_var_target;
        short catch_target;
        short offset_target;
        struct {
            short offset;
            u8 type_argument_index;
        } type_argument_target;
    } target_info;
    TypePath target_path;
    short type_index;
    AK::FixedArray<ElementValuePair> element_value_pairs;
};

struct RuntimeVisibleTypeAnnotations {
    AK::FixedArray<TypeAnnotation> annotations;
};

struct RuntimeInvisibleTypeAnnotations {
    AK::FixedArray<TypeAnnotation> annotations;
};

struct AnnotationDefault {
    ElementValue default_value;
};

struct BootstrapMethod {
    short bootstrap_method_ref;
    AK::FixedArray<short> bootstrap_arguments;
};

struct BootstrapMethods {
    AK::FixedArray<BootstrapMethod> bootstrap_methods;
};

struct MethodParameter {
    short name_index;
    short access_flags;
};

struct MethodParameters {
    AK::FixedArray<MethodParameters> parameters;
};

struct ModulePackages {
    AK::FixedArray<short> package_indeces;
};

struct ModuleMainClass {
    short main_class_index;
};

struct NestHost {
    short host_class_index;
};

struct RecordComponent {
    short name_index;
    short desc_index;
    short attributes_index;
    AK::FixedArray<AttributeInfo> attributes;
};

struct Record {
    AK::FixedArray<RecordComponent> components;
};

struct PermittedSubclasses {
    AK::FixedArray<short> classes;
};

struct Custom {
    short name_index;
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
    AttributeInfo(ConstantValue cv)
         : m_kind(AttributeKind::ConstantValue)
    {
        m_value.constantvalue_index = cv;
    }
    AttributeInfo(Signature sig)
         : m_kind(AttributeKind::Signature)
    {
        m_value.signature = sig;
    }
    AttributeInfo(AttributeKind kind)
        : m_kind(kind)
    {

    }
private:
    AttributeKind m_kind;
    union {
        //Should these all be structs, or should some of them be primitives (if that's what they are)?
        //Also, should tables be wrapper structs around AK::FixedArray, or should they just directly be that?
        //FIXME: Make more of these pointers to save on struct space. (Or don't do that if it's bad).
        //FIXME: Another problem here is that a lot of these are unecessarily pointers because the compiler complains about no default constructor otherwise. Fix this! (I don't know how)
        ConstantValue constantvalue_index;
        Code* code;
        AK::FixedArray<StackMapFrame>* sm_table;
        ExceptionTable* exceptions;
        InnerClassTable* inner_classes;
        EnclosingMethod enclosing_method;
        Synthetic synthetic;
        Signature signature;
        SourceFile source_file;
        LocalVariableTable* local_variable_table;
        LocalVariableTypeTable* local_variable_type_table;
        Deprecated deprecated;
        RuntimeVisibleAnnotations* runtime_visible_annotations;
        RuntimeInvisibleAnnotations* runtime_invisible_annotations;
        RuntimeVisibleParameterAnnotations* runtime_visible_parameter_annotations;
        RuntimeInvisibleParameterAnnotations* runtime_invisible_parameter_annotations;
        RuntimeVisibleTypeAnnotations* runtime_visible_type_annotations;
        RuntimeInvisibleTypeAnnotations* runtime_invisible_type_annotations;
        AnnotationDefault* annotation_default;
        BootstrapMethods* bootstrap_methods;
        MethodParameters* method_parameters;
        Module* module; //This is a NonnullOwnPtr because the struct is so large.
        ModulePackages* module_packages;
        ModuleMainClass module_main_class;
        NestHost nest_host;
        Record* record;
        PermittedSubclasses* permitted_subclasses;
        Custom custom;
    } m_value;
};

}
