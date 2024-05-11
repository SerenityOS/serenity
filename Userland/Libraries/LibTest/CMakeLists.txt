serenity_install_sources("Userland/Libraries/LibTest")

set(SOURCES
    AsyncTestStreams.cpp
    TestSuite.cpp
    CrashTest.cpp
)

serenity_lib(LibTest test)
target_link_libraries(LibTest PRIVATE LibCore)

add_library(LibTestMain OBJECT TestMain.cpp)
add_library(JavaScriptTestRunnerMain OBJECT JavaScriptTestRunnerMain.cpp)

if (SERENITYOS)
    add_dependencies(LibTestMain install_libc_headers)
    add_dependencies(JavaScriptTestRunnerMain install_libc_headers)
endif()
