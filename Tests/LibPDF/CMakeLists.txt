set(TEST_SOURCES
    TestPDF.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibPDF LIBS LibCore LibPDF)
endforeach()

set(TEST_FILES
    complex.pdf
    linearized.pdf
    non-linearized.pdf
    type1.pdf
)
install(FILES ${TEST_FILES} DESTINATION home/anon/Documents/pdf)
install(FILES ${TEST_FILES} DESTINATION usr/Tests/LibPDF)
