set(TEST_SOURCES
    BenchmarkPDF.cpp
    TestPDF.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibPDF LIBS LibCore LibGfx LibPDF)
endforeach()

set(TEST_FILES
    colorspaces.pdf
    complex.pdf
    encoding.pdf
    encryption_nocopy.pdf
    jbig2-globals.pdf
    linearized.pdf
    non-linearized.pdf
    oss-fuzz-testcase-62065.pdf
    password-is-sup.pdf
    pattern.pdf
    text.pdf
    type1.pdf
    type3.pdf
)
install(FILES ${TEST_FILES} DESTINATION home/anon/Documents/pdf)
install(FILES ${TEST_FILES} DESTINATION usr/Tests/LibPDF)
