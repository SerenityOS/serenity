set(SOURCES
    CodeComprehensionEngine.cpp
    FileDB.cpp
)

serenity_lib(LibCodeComprehension codecomprehension)
if (NOT BUILD_LAGOM AND NOT BUILD_LAGOM_TOOLS)
    serenity_lib_static(LibCodeComprehensionStatic codecomprehension)
endif()

add_subdirectory(Cpp)
add_subdirectory(Shell)
