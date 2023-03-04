cmake_minimum_required(VERSION 3.24)

project(LibJSGCVerifier C CXX)

find_package(Clang CONFIG REQUIRED HINTS "../../../../Toolchain/Local/clang")

add_executable(LibJSGCVerifier src/main.cpp src/CellsHandler.cpp)

target_include_directories(LibJSGCVerifier SYSTEM PRIVATE ${CLANG_INCLUDE_DIRS} ${LLVM_INCLUDE_DIRS})
target_compile_features(LibJSGCVerifier PRIVATE cxx_std_20)
target_link_libraries(LibJSGCVerifier PRIVATE clangTooling)

target_compile_options(LibJSGCVerifier PRIVATE
    -Wall
    -Wextra
    -Werror
    -Wno-unused
    -fno-rtti
)
