The description of the tests for the InnerClasses attribute.

InnerClassesTestBase is the base class for tests of inner classes attribute.
Each tests extends the base class.
The scenario of tests:
    1. set possible values of class modifiers, outer/inner class types.
    2. according to set class modifiers, a test generates sources
       and golden data with generateTestCases.
    3. a test loops through all test cases and checks InnerClasses attribute in method test().

Example, possible flags for outer class are Modifier.PRIVATE and Modifier.PUBLIC,
possible flags for inner class are Modifier.EMPTY, outer class type is CLASS
and inner class type is CLASS.
At the second step the test generates two test cases:
  1. public class A {
       public class B {
         class C {}
        }
     }
  2. public class A {
       private class B {
         class C {}
       }
     }

The list of tests.

Test: test if there is not inner class, the InnerClasses attribute
is not generated (NoInnerClasses.java).

Test: inner classes in anonymous class (InnerClassesInAnonymousClassTest.java).
Possible access flags of the inner class: abstract and final.

Test: inner classes in local class (InnerClassesInLocalClassTest.java).
Locations of local class: static and instance initializer, constructor, method, lambda,
default and static methods of interface.

Test: test the outer_class_info_index and inner_name_index of
local and anonymous classes (InnerClassesIndexTest.java).

List of test cases for Inner*InInner*Test:
    * InnerClassesInInnerClassTest
        outer flags: all possible flags
        inner flags: all possible flags
    * InnerClassesInInnerEnumTest
        outer flags: all access flags, abstract
        inner flags: all possible flags
    * InnerClassesInInnerAnnotationTest
        outer flags: all access flags, abstract
        inner flags: all flags, except private and protected
    * InnerClassesInInnerInterfaceTest
        outer flags: all access flags, abstract
        inner flags: all flags, except private and protected

    * InnerEnumsInInnerClassTest
        outer flags: all possible flags
        inner flags: all possible flags
    * InnerEnumsInInnerEnumTest
        outer flags: all possible flags
        inner flags: all possible flags
    * InnerEnumsInInnerAnnotationTest
        outer flags: all access flags, abstract, static
        inner flags: public, static
    * InnerEnumsInInnerInterfaceTest
        outer flags: all access flags, abstract, static
        inner flags: public, static

    * InnerAnnotationInInnerClassTest
        outer flags: all possible flags, except static
        inner flags: all access flags, abstract and static
    * InnerAnnotationInInnerEnumTest
        outer flags: all access flags, static
        inner flags: all access flags, abstract and static
    * InnerAnnotationInInnerAnnotation
        outer flags: all access flags, static and abstract
        inner flags: public, abstract, static
    * InnerAnnotationInInnerInterface
        outer flags: all access flags, static and abstract
        inner flags: public, abstract, static

    * InnerInterfaceInInnerClassTest
        outer flags: all possible flags, except static
        inner flags: all access flags, abstract and static
    * InnerInterfaceInInnerEnumTest
        outer flags: all access flags, static
        inner flags: all access flags, abstract and static
    * InnerInterfaceInInnerAnnotation
        outer flags: all access flags, static and abstract
        inner flags: public, abstract, static
    * InnerInterfaceInInnerInterface
        outer flags: all access flags, static and abstract
        inner flags: public, abstract, static