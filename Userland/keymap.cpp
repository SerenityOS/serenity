#include <AK/JsonObject.h>
#include <LibCore/CFile.h>
#include <stdio.h>

#include <Kernel/Syscall.h>
#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <AK/kmalloc.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

char* read_map(const JsonObject& json, const String& name)
{
    char* map = new char[80];

    auto map_arr = json.get(name).as_array();
    for(int i=0; i<map_arr.size(); i++) 
    {
        auto key_value = map_arr.at(i).as_string();
        char character = 0;
        if( key_value.length() == 0) {
            ;
        } else if(key_value.length() == 1){
            character = key_value.characters()[0];
        } else if(key_value.length() == 4){
            if(key_value == "0x08"){
                character = 0x08;
            }else if(key_value == "0x33"){
                character = 0x33;
            }
        } else {
            fprintf(stderr, "Unknown character in %s[%u] = %s.\n", name.characters(), i, key_value.characters());       
            ASSERT_NOT_REACHED();
        }
        
        map[i] = character;
    }

    return map;
}

int read_map_from_file(String filename){
    auto file = CFile::construct(filename);
    if (!file->open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Failed to open %s: %s\n", filename.characters(), file->error_string());
        return 1;
    }

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents).as_object();
 
    char* map = read_map(json, "map");
    char* shift_map = read_map(json, "shift_map");
    char* alt_map = read_map(json, "alt_map");

    return syscall(SC_setkeymap, map, shift_map, alt_map);
}

int main(int argc, char** argv)
{   
    if (argc != 2) {
        fprintf(stderr, "usage: keymap <file>\n");
        return 0;
    }

    String filename = argv[1];
    int ret_val = read_map_from_file(filename);

    if(ret_val == -EPERM)
        fprintf(stderr, "Permission denied.\n");

    if(ret_val == 0)
        fprintf(stderr, "New keymap loaded from \"%s\".\n", filename.characters());

    return ret_val;
}