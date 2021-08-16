
var ScriptContext = javax.script.ScriptContext;
print(count);

switch (count) {
        // engine only
	case 1:
            if (key != 'value in engine') {
                throw "unexpected engine scope value";
            }
            if (context.getAttribute("key", ScriptContext.GLOBAL_SCOPE ) != null) {
                throw "unexpected global scope value";
            }
            break;

        // both scopes
        case 2:
            if (key != 'value in engine') {
                throw "unexpected engine scope value";
            }
            if (context.getAttribute("key", ScriptContext.GLOBAL_SCOPE ) != 
                "value in global") {
                throw "unexpected global scope value";
            }
            break;

        // global only
        case 3:
            if (key != 'value in global') {
                throw "unexpected global scope value";
            }
            if (context.getAttribute("key", ScriptContext.GLOBAL_SCOPE ) != 
                "value in global") {
                throw "unexpected global scope value";
            }
            break;

        default:
            throw "unexpected count";
            break;
}
