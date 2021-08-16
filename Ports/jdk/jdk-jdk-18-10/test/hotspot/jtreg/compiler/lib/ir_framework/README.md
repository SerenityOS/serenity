# IR Test Framework
This folder contains a test framework whose main purpose is to perform regex-based checks on the C2 IR shape of test methods emitted by the VM flags _-XX:+PrintIdeal_ and _-XX:+PrintOptoAssembly_. The framework can also be used for other non-IR matching (and non-compiler) tests by providing easy to use annotations for commonly used testing patterns and compiler control flags.

## 1. How to Use the Framework
The framework is intended to be used in JTreg tests. The JTreg header of the test must contain `@library /test/lib /` (2 paths) and should be run as a driver with `@run driver`. Annotate the test code with the supported framework annotations and call the framework from within the test's `main()` method. A simple example is shown below:

    /*
     * @test
     * @summary A simple test using the test framework.
     * @library /test/lib /
     * @run driver my.package.MySimpleTest
     */
    
    package my.package;
    
    import compiler.lib.ir_framework.*;

    public class MySimpleTest {
        
        public static void main(String[] args) {
            TestFramework.run(); // The framework runs all tests of this class.
        }
    
        @Test
        @IR(failOn = IRNode.STORE) // Fail if the IR of myTest() contains any stores.
        public void myTest() {
            /* ... */
        }
    }

There are various ways how to set up and run a test within the `main()` method of a JTreg test. These are described and can be found in the [TestFramework](./TestFramework.java) class.
   
## 2. Features
The framework offers various annotations and flags to control how your test code should be invoked and being checked. This section gives an overview over all these features.

### 2.1 Different Tests
There are three kinds of tests depending on how much control is needed over the test invocation.
#### Base Tests
The simplest form of testing provides a single `@Test` annotated method which the framework will invoke as part of the testing. The test method has no or well-defined arguments that the framework can automatically provide. 

More information on base tests with a precise definition can be found in the Javadocs of [Test](./Test.java). Concrete examples on how to specify a base test can be found in [BaseTestsExample](../../../testlibrary_tests/ir_framework/examples/BaseTestExample.java).

#### Checked Tests
The base tests do not provide any way of verification by user code. A checked test enables this by allowing the user to define an additional `@Check` annotated method which is invoked directly after the `@Test` annotated method. This allows the user to perform various checks about the test method including return value verification.

More information on checked tests with a precise definition can be found in the Javadocs of [Check](./Check.java). Concrete examples on how to specify a checked test can be found in [CheckedTestsExample](../../../testlibrary_tests/ir_framework/examples/CheckedTestExample.java).

#### Custom Run Tests
Neither the base nor the checked tests provide any control over how a `@Test` annotated method is invoked in terms of customized argument values and/or conditions for the invocation itself. A custom run test gives full control over the invocation of the `@Test` annotated method to the user. The framework calls a dedicated `@Run` annotated method from which the user can invoke the `@Test` method according to his/her needs.

More information on checked tests with a precise definition can be found in the Javadocs of [Run](./Run.java). Concrete examples on how to specify a custom run test can be found in [CustomRunTestsExample](../../../testlibrary_tests/ir_framework/examples/CustomRunTestExample.java).

### 2.2 IR Verification
The main feature of this framework is to perform a simple but yet powerful regex-based C2 IR matching on the output of _-XX:+PrintIdeal_ and _-XX:+PrintOptoAssembly_. For simplicity, we will refer to the "IR" or "IR matching" when actually meaning the combined output of _-XX:+PrintIdeal_ and _-XX:+PrintOptoAssembly_ for a C2 compilation.

The user has the possibility to add an additional `@IR` annotation to any `@Test` annotated method (regardless of the kind of test mentioned in section 2.1) to specify a constraint/rule on the compiled IR shape. The `@IR` annotation provides two kinds of regex checks:

 - A `failOn` check that verifies that the provided regex is not matched in the C2 IR.
 - A `counts` check that verifies that the provided regex is matched a user defined number of times in the C2 IR.
 
A regex can either be a custom string or any of the default regexes provided by the framework in [IRNode](./IRNode.java) for some commonly used IR nodes (also provides the possibility of composite regexes).

An IR verification cannot always be performed. For example, a JTreg test could be run with _-Xint_ or not a debug build (_-XX:+PrintIdeal_ and _-XX:+PrintOptoAssembly_ are debug build flags). But also CI tier testing could add additional JTreg VM and Javaoptions flags which could make an IR rule unstable. 

In general, the framework will only perform IR verification if the used VM flags allow a C2 compilation and if non-critical additional JTreg VM and Javaoptions are provided (see whiteflag list in [TestFramework](./TestFramework.java)). The user test code, however, can specify any flags which still allow an IR verification to be performed if a C2 compilation is done (expected flags by user defined `@IR` annotations). 

An `@IR` annotation allows additional preconditions/restrictions on the currently present VM flags to enable or disable rules when certain flags are present or have a specific value (see `applyIfXX` properties of an `@IR` annotation).

More information about IR matching can be found in the Javadocs of [IR](./IR.java). Concrete examples on how to specify IR constraint/rules can be found in [IRExample](../../../testlibrary_tests/ir_framework/examples/IRExample.java) and [TestIRMatching](../../../testlibrary_tests/ir_framework/tests/TestIRMatching.java) (an internal framework test).

### 2.3 Test VM Flags and Scenarios
The recommended way to use the framework is by defining a single `@run driver` statement in the JTreg header which, however, does not allow the specification of additional test VM flags. Instead, the user has the possibility to provide VM flags by calling `TestFramework.runWithFlags()` or by creating a `TestFramework` builder object on which `addFlags()` can be called.

If a user wants to provide multiple flag combinations for a single test, he or she has the option to provide different scenarios. A scenario based flag will always have precedence over other user defined flags. More information about scenarios can be found in the Javadocs of [Scenario](./Scenario.java).

### 2.4 Compiler Controls
The framework allows the use of additional compiler control annotations for helper method and classes in the same fashion as JMH does. The following annotations are supported and described in the referenced Javadocs for the annotation class:

- [@DontInline](./DontInline.java)
- [@ForceInline](./ForceInline.java)
- [@DontCompile](./DontCompile.java)
- [@ForceCompile](./DontCompile.java)
- [@ForceCompileClassInitializer](./ForceCompileClassInitializer.java)

### 2.5 Framework Debug and Stress Flags
The framework provides various stress and debug flags. They should mainly be used as JTreg VM and/or Javaoptions (apart from `VerifyIR`). The following (property) flags are supported:

- `-DVerifyIR=false`: Explicitly disable IR verification. This is useful, for example, if some scenarios use VM flags that let `@IR` annotation rules fail and the user does not want to provide separate IR rules or add flag preconditions to the already existing IR rules.
- `-DTest=test1,test2`: Provide a list of `@Test` method names which should be executed.
- `-DExclude=test3`: Provide a list of `@Test` method names which should be excluded from execution.
- `-DScenarios=1,2`: Provide a list of scenario indexes to specify which scenarios should be executed.
- `-DWarmup=200`: Provide a new default value of the number of warm-up iterations (framework default is 2000). This might have an influence on the resulting IR and could lead to matching failures (the user can also set a fixed default warm-up value in a test with `testFrameworkObject.setDefaultWarmup(200)`).
- `-DVerbose=true`: Enable more fain-grained logging (slows the execution down).
- `-DReproduce=true`: Flag to use when directly running a test VM to bypass dependencies to the driver VM state (for example, when reproducing an issue).
- `-DPrintTimes=true`: Print the execution time measurements of each executed test.
- `-DVerifyVM=true`: The framework runs the test VM with additional verification flags (slows the execution down).
- `-DExcluceRandom=true`: The framework randomly excludes some methods from compilation. IR verification is disabled completely with this flag.
- `-DFlipC1C2=true`: The framework compiles all `@Test` annotated method with C1 if a C2 compilation would have been applied and vice versa. IR verification is disabled completely with this flag.
- `-DShuffleTests=false`: Disables the random execution order of all tests (such a shuffling is always done by default).
- `-DDumpReplay=true`: Add the `DumpReplay` directive to the test VM.
- `-DGCAfter=true`: Perform `System.gc()` after each test (slows the execution down).
- `-TestCompilationTimeout=20`: Change the default waiting time (default: 10s) for a compilation of a normal `@Test` annotated method.
- `-DWaitForCompilationTimeout=20`: Change the default waiting time (default: 10s) for a compilation of a `@Test` annotated method with compilation level [WAIT\_FOR\_COMPILATION](./CompLevel.java).
- `-DIgnoreCompilerControls=false`: Ignore all compiler controls applied in the framework. This includes any compiler control annotations (`@DontCompile`, `@DontInline`, `@ForceCompile`, `@ForceInline`, `@ForceCompileStaticInitializer`), the exclusion of `@Run` and `@Check` methods from compilation, and the directive to not inline `@Test` annotated methods.


## 3. Test Framework Execution
This section gives an overview of how the framework is executing a JTreg test that calls the framework from within its `main()` method.

The framework will spawn a new "test VM" to execute the user defined tests. The test VM collects all tests of the test class specified by the user code in `main()` and ensures that there is no violation of the required format by the framework. In a next step, the framework does the following for each test in general:
1. Warm the test up for a predefined number of times (default 2000). This can also be adapted for all tests by using `testFrameworkobject.setDefaultWarmup(100)` or for individual tests with an additional [@Warmup](./Warmup.java) annotation. 
2. After the warm-up is finished, the framework compiles the associated `@Test` annotated method at the specified compilation level (default: C2).
3. After the compilation, the test is invoked one more time.

Once the test VM terminates, IR verification (if possible) is performed on the output of the test VM. If any test throws an exception during its execution or if IR matching fails, the failures are collected and reported in a pretty format. Check the standard error and output for more information and how to reproduce these failures.

Some of the steps above can be different due to the kind of the test or due to using non-default annotation properties. These details and differences are described in the Javadocs for the three tests (see section 2.1 Different Tests).

More information about the internals and the workflow of the framework can be found in the Javadocs of [TestFramework](./TestFramework.java).  
 
## 4. Internal Framework Tests
There are various tests to verify the correctness of the test framework. These tests can be found in [ir_framework](../../../testlibrary_tests/ir_framework) and can directly be run with JTreg. The tests are part of the normal JTreg tests of HotSpot and should be run upon changing the framework code as a minimal form of testing.

Additional testing was performed by converting all compiler Inline Types tests that used the currently present IR test framework in Valhalla (see [JDK-8263024](https://bugs.openjdk.java.net/browse/JDK-8263024)). It is strongly advised to make sure a change to the framework still lets these converted tests in Valhalla pass as part of an additional testing step.

## 5. Framework Package Structure
A user only needs to import classes from the package `compiler.lib.ir_framework` (e.g. `import compiler.lib.ir_framework.*;`) which represents the interface classes to the framework. The remaining framework internal classes are kept in separate subpackages and should not directly be imported:

- `compiler.lib.ir_framework.driver`: These classes are used while running the driver VM (same VM as the one running the user code's `main()` method of a JTreg test).
- `compiler.lib.ir_framework.flag`: These classes are used while running the flag VM to determine additional flags for the test VM which are required for IR verification.
- `compiler.lib.ir_framework.test`: These classes are used while running the test VM (i.e. the actual execution of the user tests as described in section 3).
- `compiler.lib.ir_framework.shared`: These classes can be called from either the driver, flag, or test VM.

## 6. Summary
The initial design and feature set was kept simple and straight forward and serves well for small to medium sized tests. There are a lot of possibilities to further enhance the framework and make it more powerful. This can be tackled in additional RFEs. A few ideas can be found as subtasks of the [initial RFE](https://bugs.openjdk.java.net/browse/JDK-8254129) for this framework.
