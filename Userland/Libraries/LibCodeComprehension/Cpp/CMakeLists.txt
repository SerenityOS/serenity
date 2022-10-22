set(SOURCES
    CppComprehensionEngine.cpp
)

serenity_lib(LibCppComprehension cppcomprehension)
target_link_libraries(LibCppComprehension LibCodeComprehension)

serenity_component(
    CppComprehensionTests
    TARGETS CppComprehensionTests
)

set(SOURCES
    CppComprehensionEngine.cpp
    Tests.cpp
)

serenity_bin(CppComprehensionTests)

target_link_libraries(CppComprehensionTests LibCodeComprehension LibCpp LibRegex LibMain)
