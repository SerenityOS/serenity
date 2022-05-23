#pragma once
#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/Utf8View.h>
#include <LibJVM/Forward.h>
#include <LibJVM/Module.h>
#include <LibJVM/StackMapFrame.h>
#include <LibJVM/Verification.h>

//FIXME: Remove useless structs and merge structs that are only used in another struct into nameless structs.
//FIXME: Replace all 'short's with 'unsigned short's.
//FIXME: Actually handle errors! (specifically those from try_create())
//FIXME: A lot of these structs have both FixedArrays and copy constructors. For these, I have just used try_create on a span of the other FixedArray.
//Does this leak memory? Do we need to deallocate the other FixedArray? I need answers!


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
    ExceptionTable(ExceptionTable const &other) {
        exception_index_table.try_create(other.exception_index_table.span());
    }
    ExceptionTable() {}
};

struct Code {
    short max_stack;
    short max_locals;
    AK::FixedArray<u8> code;
    AK::FixedArray<Exception> exception_table;
    AK::FixedArray<AttributeInfo> attributes;
    Code(Code const &other) {
        max_stack = other.max_stack;
        max_locals = other.max_locals;
        code.try_create(other.code.span());
        exception_table.try_create(other.exception_table.span());
        attributes.try_create(other.attributes.span());
    }
    Code() {}
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
    LineNumberTable(LineNumberTable const &other) {
        line_number_table.try_create(other.line_number_table.span());
    }
    LineNumberTable () {}
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
    LocalVariableTable(LocalVariableTable const &other) {
        local_variable_table.try_create(other.local_variable_table.span());
    }
    LocalVariableTable() {}
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
    LocalVariableTypeTable(LocalVariableTypeTable const &other) {
        local_variable_type_table.try_create(other.local_variable_type_table.span());
    }
    LocalVariableTypeTable () {}
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
    RuntimeVisibleAnnotations() { }
    RuntimeVisibleAnnotations(RuntimeVisibleAnnotations const &annos) {
        annotations.try_create(annos.annotations.span()); //I think this should be done with .try_clone(), but the compiler doesn't allow that :(
    }
};

struct RuntimeInvisibleAnnotations {
    AK::FixedArray<Annotation> annotations;
    RuntimeInvisibleAnnotations() { }
    RuntimeInvisibleAnnotations(RuntimeInvisibleAnnotations const &annos) {
        annotations.try_create(annos.annotations.span()); //I think this should be done with .try_clone(), but the compiler doesn't allow that :(
    }
};

struct ParameterAnnotations {
    AK::FixedArray<Annotation> annotations;
};

struct RuntimeVisibleParameterAnnotations {
    AK::FixedArray<ParameterAnnotations> parameter_annotations;
    RuntimeVisibleParameterAnnotations() { }
    RuntimeVisibleParameterAnnotations(RuntimeVisibleParameterAnnotations const &annos) {
        parameter_annotations.try_create(annos.parameter_annotations.span()); //I think this should be done with .try_clone(), but the compiler doesn't allow that :(
    }
};

struct RuntimeInvisibleParameterAnnotations {
    AK::FixedArray<ParameterAnnotations> parameter_annotations;
    RuntimeInvisibleParameterAnnotations() { }
    RuntimeInvisibleParameterAnnotations(RuntimeInvisibleParameterAnnotations const &annos) {
        parameter_annotations.try_create(annos.parameter_annotations.span()); //I think this should be done with .try_clone(), but the compiler doesn't allow that :(
    }
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
    union Target {
        u8 type_parameter_target;
        short supertype_target;
        struct {
            u8 type_parameter_index;
            u8 bound_index;
        } type_parameter_bound_target;
        u8 formal_parameter_target;
        short throws_target;
        AK::FixedArray<Localvar_Target> local_var_target;
        short catch_target;
        short offset_target;
        struct {
            short offset;
            u8 type_argument_index;
        } type_argument_target;
        Target() {
            type_parameter_target = 0;
        }
        ~Target() {
            local_var_target.~FixedArray();
        }
    } target_info;
    TypePath target_path;
    short type_index;
    AK::FixedArray<ElementValuePair> element_value_pairs;

    TypeAnnotation()
        : target_type(0)
    {
        target_info.type_parameter_target = 0;
    }
};

struct RuntimeVisibleTypeAnnotations {
    AK::FixedArray<TypeAnnotation> annotations;
    RuntimeVisibleTypeAnnotations() { }
    RuntimeVisibleTypeAnnotations(RuntimeVisibleTypeAnnotations const &annos) {
        annotations.try_create(annos.annotations.span()); //I think this should be done with .try_clone(), but the compiler doesn't allow that :(
    }
};

struct RuntimeInvisibleTypeAnnotations {
    AK::FixedArray<TypeAnnotation> annotations;
    RuntimeInvisibleTypeAnnotations() { }
    RuntimeInvisibleTypeAnnotations(RuntimeInvisibleTypeAnnotations const &annos) {
        annotations.try_create(annos.annotations.span()); //I think this should be done with .try_clone(), but the compiler doesn't allow that :(
    }
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
    MethodParameters(MethodParameters const &other) {
        parameters.try_create(other.parameters.span());
    }
    MethodParameters() {}
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

//A lot ofthe union values are actually pointers to values, because the pointer is much smaller than some of the structures.
//However, I don't actually know if these values will live or if they will be deallocated.
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
    AttributeInfo(Custom cus)
         : m_kind(AttributeKind::Custom)
    {
        m_value.custom = cus;
    }
    AttributeInfo(RuntimeVisibleAnnotations annos)
         : m_kind(AttributeKind::RuntimeVisibleAnnotations)
    {
        m_value.runtime_visible_annotations = &annos;
    }
    AttributeInfo(RuntimeInvisibleAnnotations annos)
         : m_kind(AttributeKind::RuntimeInvisibleAnnotations)
    {
        m_value.runtime_invisible_annotations = &annos;
    }
    AttributeInfo(RuntimeVisibleTypeAnnotations annos)
         : m_kind(AttributeKind::RuntimeVisibleTypeAnnotations)
    {
        m_value.runtime_visible_type_annotations = &annos;
    }
    AttributeInfo(RuntimeInvisibleTypeAnnotations annos)
         : m_kind(AttributeKind::RuntimeInvisibleTypeAnnotations)
    {
        m_value.runtime_invisible_type_annotations = &annos;
    }
    AttributeInfo(RuntimeVisibleParameterAnnotations annos)
         : m_kind(AttributeKind::RuntimeVisibleParameterAnnotations)
    {
        m_value.runtime_visible_parameter_annotations = &annos;
    }
    AttributeInfo(RuntimeInvisibleParameterAnnotations annos)
         : m_kind(AttributeKind::RuntimeInvisibleParameterAnnotations)
    {
        m_value.runtime_invisible_parameter_annotations = &annos;
    }
    AttributeInfo(MethodParameters params)
        : m_kind(AttributeKind::MethodParameters)
    {
        m_value.method_parameters = &params;
    }
    AttributeInfo(ExceptionTable table)
        :m_kind(AttributeKind::Execptions)
    {
        m_value.exceptions = &table;
    }
    AttributeInfo(LocalVariableTypeTable table)
        :m_kind(AttributeKind::LocalVariableTypeTable)
    {
        m_value.local_variable_type_table = &table;
    }
    AttributeInfo(LocalVariableTable table)
        :m_kind(AttributeKind::LocalVariableTable)
    {
        m_value.local_variable_table = &table;
    }
    AttributeInfo(LineNumberTable table)
        :m_kind(AttributeKind::LineNumberTable)
    {
        m_value.line_number_table = &table;
    }
    AttributeInfo(StackMapTable table)
        : m_kind(AttributeKind::StackMapTable)
    {
        m_value.sm_table = table;
    }
    AttributeInfo(Code code)
        : m_kind(AttributeKind::Code)
    {
        m_value.code = &code;
    }

    AttributeInfo(AttributeKind kind)
        : m_kind(kind)
    {

    }
private:
    AttributeKind m_kind;
    union AttributeValue {
        //Should these all be structs, or should some of them be primitives (if that's what they are)?
        //Also, should tables be wrapper structs around AK::FixedArray, or should they just directly be that?
        //FIXME: Make more of these pointers to save on struct space. (Or don't do that if that's bad design).
        //FIXME: Another problem here is that a lot of these are unecessarily pointers because the compiler complains about no default constructor otherwise. Fix this! (I don't know how).
        ConstantValue constantvalue_index;
        Code* code;
        StackMapTable sm_table;
        ExceptionTable* exceptions;
        InnerClassTable* inner_classes;
        EnclosingMethod enclosing_method;
        Synthetic synthetic;
        Signature signature;
        SourceFile source_file;
        LineNumberTable* line_number_table;
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
        Module* module; //This is a pointer because the strucutre is very large and would take up too much space
        ModulePackages* module_packages;
        ModuleMainClass module_main_class;
        NestHost nest_host;
        Record* record;
        PermittedSubclasses* permitted_subclasses;
        Custom custom;
        AttributeValue() {

        }
        ~AttributeValue() {
            //FIXME: Add calls to the destructors of the fixed array values here.
            //Unless they aren't deallocated. I'm new to all the AK classes, and there isn't great documentation on them, so please excuse me if I make any errors.
        }
        AttributeValue(AttributeValue const &other) {
            //FIXME: Add copy operations here.
        }
        AttributeValue& operator=(AttributeValue const &other) {

        }
        AttributeValue& operator=(AttributeValue&& other) {

        }
    } m_value;
};

}
