set(AK_TEST_SOURCES
    TestAllOf.cpp
    TestArray.cpp
    TestAtomic.cpp
    TestBase64.cpp
    TestBinarySearch.cpp
    TestBitmap.cpp
    TestByteBuffer.cpp
    TestChecked.cpp
    TestCircularDeque.cpp
    TestCircularDuplexStream.cpp
    TestCircularQueue.cpp
    TestDistinctNumeric.cpp
    TestEndian.cpp
    TestFormat.cpp
    TestHashFunctions.cpp
    TestHashMap.cpp
    TestIPv4Address.cpp
    TestJSON.cpp
    TestLexicalPath.cpp
    TestMACAddress.cpp
    TestMemMem.cpp
    TestMemoryStream.cpp
    TestNeverDestroyed.cpp
    TestNonnullRefPtr.cpp
    TestNumberFormat.cpp
    TestOptional.cpp
    TestQueue.cpp
    TestQuickSort.cpp
    TestRefPtr.cpp
    TestSourceGenerator.cpp
    TestSpan.cpp
    TestString.cpp
    TestStringUtils.cpp
    TestStringView.cpp
    TestTrie.cpp
    TestTypedTransfer.cpp
    TestURL.cpp
    TestUtf8.cpp
    TestVector.cpp
    TestWeakPtr.cpp
)

foreach(source ${AK_TEST_SOURCES})
    get_filename_component(name ${source} NAME_WE)
    add_executable(${name} ${source})
    target_link_libraries(${name} LagomCore)
    add_test(
        NAME ${name}
        COMMAND ${name}
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    )

    set_tests_properties(
        ${name}
        PROPERTIES
            FAIL_REGULAR_EXPRESSION
            "FAIL"
    )
endforeach()
