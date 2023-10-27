lagom_test(
    test-runner.cpp
    NAME TestJSSpecCompiler
    LIBS LibDiff
    WORKING_DIRECTORY "${SERENITY_PROJECT_ROOT}/Meta/Lagom/Tools/CodeGenerators/JSSpecCompiler/"
)
