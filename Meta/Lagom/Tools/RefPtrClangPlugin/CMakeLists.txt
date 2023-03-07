if (EXISTS "${CMAKE_BINARY_DIR}/../../Toolchain/Local/clang")
    find_package(Clang CONFIG REQUIRED HINTS "${CMAKE_BINARY_DIR}/../../Toolchain/Local/clang")

    add_library(RefPtrClangPlugin main.cpp)
    add_library(Lagom::RefPtrClangPlugin ALIAS RefPtrClangPlugin)

    target_include_directories(RefPtrClangPlugin SYSTEM PRIVATE ${CLANG_INCLUDE_DIRS} ${LLVM_INCLUDE_DIRS})
    target_compile_features(RefPtrClangPlugin PRIVATE cxx_std_20)
    target_link_libraries(RefPtrClangPlugin PRIVATE clangTooling)

    target_compile_options(RefPtrClangPlugin PRIVATE
        -Wall
        -Wextra
        -Werror
        -Wno-unused
        -fno-rtti
    )
endif()
