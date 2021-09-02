set(TEST_SOURCES
    test-math.cpp
)
add_compile_options(-fno-builtin)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibM)
endforeach()
