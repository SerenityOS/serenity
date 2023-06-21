serenity_install_sources("Userland/Libraries/LibTest")

set(SOURCES
    TestSuite.cpp
    CrashTest.cpp
)

serenity_lib(LibTest test)

add_library(LibTestMain OBJECT TestMain.cpp)
add_library(JavaScriptTestRunnerMain OBJECT JavaScriptTestRunnerMain.cpp)
