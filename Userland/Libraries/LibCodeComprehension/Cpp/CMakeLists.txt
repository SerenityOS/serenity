set(SOURCES
    CppComprehensionEngine.cpp
)

serenity_lib(LibCppComprehension cppcomprehension)
target_link_libraries(LibCppComprehension PRIVATE LibCodeComprehension)

serenity_component(
    CppComprehensionTests
    TARGETS CppComprehensionTests
)

set(SOURCES
    CppComprehensionEngine.cpp
    Tests.cpp
)

serenity_bin(CppComprehensionTests)

target_link_libraries(CppComprehensionTests PRIVATE LibCodeComprehension LibCore LibCpp LibFileSystem LibRegex LibMain)
