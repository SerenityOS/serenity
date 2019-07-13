#include "TestHelpers.h"
#include <AK/AKString.h>
#include <AK/HashMap.h>

typedef HashMap<int, int> IntIntMap;

int main()
{
    EXPECT(IntIntMap().is_empty());
    EXPECT(IntIntMap().size() == 0);

    HashMap<int, String> number_to_string;
    number_to_string.set(1, "One");
    number_to_string.set(2, "Two");
    number_to_string.set(3, "Three");
    
    EXPECT_EQ(number_to_string.is_empty(), false);
    EXPECT_EQ(number_to_string.size(), 3);

    int loop_counter = 0;
    for (auto& it : number_to_string) {
        EXPECT(!it.value.is_null());
        ++loop_counter;
    }

    number_to_string.remove(1);
    EXPECT_EQ(number_to_string.size(), 2);
    EXPECT(number_to_string.find(1) == number_to_string.end());

    number_to_string.remove(3);
    EXPECT_EQ(number_to_string.size(), 1);
    EXPECT(number_to_string.find(3) == number_to_string.end());

    EXPECT_EQ(loop_counter, 3);

    {
        HashMap<String, int, CaseInsensitiveStringTraits> casemap;
        EXPECT_EQ(String("nickserv").to_lowercase(), String("NickServ").to_lowercase());
        casemap.set("nickserv", 3);
        casemap.set("NickServ", 3);
        EXPECT_EQ(casemap.size(), 1);
    }

    return 0;
}
