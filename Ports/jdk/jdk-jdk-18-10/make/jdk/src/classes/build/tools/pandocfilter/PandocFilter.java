package build.tools.pandocfilter;

import build.tools.pandocfilter.json.JSON;
import build.tools.pandocfilter.json.JSONArray;
import build.tools.pandocfilter.json.JSONObject;
import build.tools.pandocfilter.json.JSONString;
import build.tools.pandocfilter.json.JSONValue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Map;

public class PandocFilter {
    /**
     * Traverse a tree of pandoc format objects, calling callback on each
     * element, and replacing it if callback returns a new object.
     * <p>
     * Inspired by the walk method in
     * https://github.com/jgm/pandocfilters/blob/master/pandocfilters.py
     */
    public JSONValue traverse(JSONValue obj, Callback callback, boolean deep) {
        if (obj instanceof JSONArray) {
            JSONArray array = (JSONArray) obj;

            JSONArray processed_array = new JSONArray();
            for (JSONValue elem : array) {
                if (elem instanceof JSONObject && elem.contains("t")) {
                    JSONValue replacement = callback.invoke(elem.get("t").asString(), elem.contains("c") ? elem.get("c") : new JSONArray());
                    if (replacement == null) {
                        // no replacement object returned, use original
                        processed_array.add(traverse(elem, callback, deep));
                    } else if (replacement instanceof JSONArray) {
                        // array of objects returned, splice all elements into array
                        JSONArray replacement_array = (JSONArray) replacement;
                        for (JSONValue repl_elem : replacement_array) {
                            processed_array.add(traverse(repl_elem, callback, deep));
                        }
                    } else {
                        // replacement object given, traverse it
                        processed_array.add(traverse(replacement, callback, deep));
                    }
                } else {
                    processed_array.add(traverse(elem, callback, deep));
                }
            }
            return processed_array;
        } else if (obj instanceof JSONObject) {
            if (deep && obj.contains("t")) {
                JSONValue replacement = callback.invoke(obj.get("t").asString(), obj.contains("c") ? obj.get("c") : new JSONArray());
                if (replacement != null) {
                    return replacement;
                }
            }            JSONObject obj_obj = (JSONObject) obj;
            var processed_obj = new JSONObject();
            for (String key : obj_obj.keys()) {
                processed_obj.put(key, traverse(obj_obj.get(key), callback, deep));
            }
            return processed_obj;
        } else {
            return obj;
        }
    }

    public JSONValue createPandocNode(String type, JSONValue content) {
        if (content == null) {
            return new JSONObject(Map.of(
                    "t", new JSONString(type)));
        } else {
            return new JSONObject(Map.of(
                    "t", new JSONString(type),
                    "c", content));
        }
    }

    public JSONValue createPandocNode(String type) {
        return createPandocNode(type, null);
    }

    /*
     * Helper constructors to create pandoc format objects
     */
    public JSONValue createSpace() {
        return createPandocNode("Space");
    }

    public JSONValue createStr(String string) {
        return createPandocNode("Str", new JSONString(string));
    }

    public static JSONValue loadJson(String[] args) throws FileNotFoundException {
        StringBuffer input = new StringBuffer();
        InputStreamReader reader;
        if (args.length > 0)
            reader = new FileReader(args[0]);
        else {
            reader = new InputStreamReader(System.in);
        }
        new BufferedReader(reader).lines().forEach(line -> input.append(line));

        return JSON.parse(input.toString());
    }

    public interface Callback {
        JSONValue invoke(String type, JSONValue value);
    }
}
