String.prototype[5] = "five";
String.prototype.foo = "foo";
var last_set_this_value = null;
class TerribleClass {
    get #private_strict() {
        "use strict";
        last_set_this_value = this;
    }
    get #private_non_strict() {
        last_set_this_value = this;
    }
    get 10() {
        "use strict";
        return this.#private_strict;
    }
    get 11() {
        return this.#private_non_strict;
    }
    set 12(v) {
        "use strict";
        return this.#private_strict;
    }
    set 13(v) {
        return this.#private_non_strict;
    }
    get strict_get_private() {
        "use strict";
        return this.#private_strict;
    }
    get non_strict_get_private() {
        return this.#private_non_strict;
    }
    set strict_set_private(v) {
        "use strict";
        this.#private_strict;
    }
    set non_strict_set_private(v) {
        this.#private_non_strict;
    }
}
String.prototype.__proto__ = {
    get non_strict_this() {
        return this;
    },
    get strict_this() {
        "use strict";
        return this;
    },
    set set_non_strict_this(v) {
        last_set_this_value = this;
    },
    set set_strict_this(v) {
        "use strict";
        last_set_this_value = this;
    },
    get 6() {
        "use strict";
        return this;
    },
    get 7() {
        return this;
    },
    set 8(v) {
        "use strict";
        last_set_this_value = this;
    },
    set 9(v) {
        last_set_this_value = this;
    },
};
String.prototype.__proto__.__proto__ = new TerribleClass();

test("primitive string: numeric indexing", () => {
    expect(""[0]).toBeUndefined();
    expect("foo"[0]).toBe("f");
    expect("foo"[2]).toBe("o");
    expect("foo"[3]).toBeUndefined();
    expect("foo"[-1]).toBeUndefined();
    expect("foo"[1.5]).toBeUndefined();
    expect("foo"[5]).toBe("five");
    expect(typeof "foo"[6]).toBe("string");
    expect("foo"[6]).toBe("foo");
    expect(typeof "foo"[7]).toBe("object");
    expect("foo"[7] instanceof String).toBeTrue();
    expect("foo"[7] !== "foo"[7]).toBeTrue();
    expect("foo"[7] !== String.prototype).toBeTrue();
    "foo"[8] = "test";
    expect(typeof last_set_this_value).toBe("string");
    expect(last_set_this_value).toBe("foo");
    last_set_this_value = null;
    "foo"[9] = "test";
    expect(typeof last_set_this_value).toBe("object");
    expect(last_set_this_value instanceof String).toBeTrue();
    expect(last_set_this_value !== String.prototype);
    let old_this_value = last_set_this_value;
    last_set_this_value = null;
    "foo"[9] = "test";
    expect(last_set_this_value !== old_this_value).toBeTrue();
    last_set_this_value = null;

    expect(() => "foo"[10]).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => "foo"[11]).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => ("foo"[12] = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => ("foo"[13] = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
});
test("primitive string: string property indexing", () => {
    expect(""["0"]).toBeUndefined();
    expect("foo"["0"]).toBe("f");
    expect("foo"["01"]).toBeUndefined();
    expect("foo"[" 1"]).toBeUndefined();
    expect("foo"["1 "]).toBeUndefined();
    expect("foo"["2"]).toBe("o");
    expect("foo"["3"]).toBeUndefined();
    expect("foo"["-1"]).toBeUndefined();
    expect("foo"["1.5"]).toBeUndefined();
    expect("foo"["5"]).toBe("five");
    expect(typeof "foo"["6"]).toBe("string");
    expect("foo"["6"]).toBe("foo");
    expect(typeof "foo"["7"]).toBe("object");
    expect("foo"["7"] instanceof String).toBeTrue();
    expect("foo"["7"] !== "foo"[7]).toBeTrue();
    expect("foo"["7"] !== String.prototope).toBeTrue();
    expect(""["length"]).toBe(0);
    expect("foo"["length"]).toBe(3);
    "foo"["8"] = "test";
    expect(typeof last_set_this_value).toBe("string");
    expect(last_set_this_value).toBe("foo");
    last_set_this_value = null;
    "foo"["9"] = "test";
    expect(typeof last_set_this_value).toBe("object");
    expect(last_set_this_value instanceof String).toBeTrue();
    expect(last_set_this_value !== String.prototype);
    let old_this_value = last_set_this_value;
    last_set_this_value = null;
    "foo"["9"] = "test";
    expect(last_set_this_value !== old_this_value).toBeTrue();
    last_set_this_value = null;

    expect(() => "foo"["10"]).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => "foo"["11"]).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => ("foo"["12"] = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => ("foo"["13"] = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
});

test("primitive string: string property name access", () => {
    expect("".length).toBe(0);
    expect("foo".length).toBe(3);
    expect("foo".bar).toBeUndefined();
    expect("foo".foo).toBe("foo");
    expect(typeof "foo".strict_this).toBe("string");
    expect("foo".strict_this).toBe("foo");
    expect(typeof "foo".non_strict_this).toBe("object");
    expect("foo".non_strict_this !== "foo".non_strict_this).toBeTrue();
    expect("foo".non_strict_this !== String.prototype).toBeTrue();
    expect("foo".non_strict_this instanceof String).toBeTrue();
    let str = new String("foo");
    str.set_strict_this = "test";
    expect(typeof last_set_this_value).toBe("object");
    expect(last_set_this_value).toBe(str);
    last_set_this_value = null;
    str.set_non_strict_this = "test";
    expect(typeof last_set_this_value).toBe("object");
    expect(last_set_this_value instanceof String).toBeTrue();
    expect(last_set_this_value).toBe(str);
    let old_this_value = last_set_this_value;
    last_set_this_value = null;
    str.set_non_strict_this = "test";
    expect(last_set_this_value === old_this_value).toBeTrue();
    last_set_this_value = null;

    expect(() => "foo".strict_get_private).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => "foo".non_strict_get_private).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => ("foo".strict_set_private = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => ("foo".non_strict_set_private = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
});

test("string object: string numeric indexing", () => {
    expect(new String("")[0]).toBeUndefined();
    expect(new String("foo")[0]).toBe("f");
    expect(new String("foo")[2]).toBe("o");
    expect(new String("foo")[3]).toBeUndefined();
    expect(new String("foo")[-1]).toBeUndefined();
    expect(new String("foo")[1.5]).toBeUndefined();
    expect(new String("foo")[5]).toBe("five");
    expect(typeof new String("foo")[6]).toBe("object");
    let str = new String("foo");
    expect(str[7]).toBe(str);
    expect(typeof "foo"[7]).toBe("object");
    expect(str[7] instanceof String).toBeTrue();
    expect(str[7]).toBe(str);
    expect(str[7] !== String.prototope).toBeTrue();
    str["8"] = "test";
    expect(typeof last_set_this_value).toBe("object");
    expect(last_set_this_value).toBe(str);
    last_set_this_value = null;
    str["9"] = "test";
    expect(typeof last_set_this_value).toBe("object");
    expect(last_set_this_value instanceof String).toBeTrue();
    expect(last_set_this_value).toBe(str);
    let old_this_value = last_set_this_value;
    last_set_this_value = null;
    str["9"] = "test";
    expect(last_set_this_value === old_this_value).toBeTrue();
    last_set_this_value = null;
});
test("string object: string property indexing", () => {
    expect(new String("")["0"]).toBeUndefined();
    expect(new String("foo")["0"]).toBe("f");
    expect(new String("foo")["2"]).toBe("o");
    expect(new String("foo")["3"]).toBeUndefined();
    expect(new String("foo")["-1"]).toBeUndefined();
    expect(new String("foo")["1.5"]).toBeUndefined();
    expect(new String("foo")["5"]).toBe("five");
    expect(typeof new String("foo")["6"]).toBe("object");
    let str = new String("foo");
    expect(str["7"]).toBe(str);
    expect(typeof "foo"["7"]).toBe("object");
    expect(str["7"] instanceof String).toBeTrue();
    expect(str["7"]).toBe(str);
    expect(str["7"] !== String.prototope).toBeTrue();
    str["set_strict_this"] = "test";
    expect(typeof last_set_this_value).toBe("object");
    expect(last_set_this_value).toBe(str);
    last_set_this_value = null;
    str["set_non_strict_this"] = "test";
    expect(typeof last_set_this_value).toBe("object");
    expect(last_set_this_value instanceof String).toBeTrue();
    expect(last_set_this_value).toBe(str);
    let old_this_value = last_set_this_value;
    last_set_this_value = null;
    str["set_non_strict_this"] = "test";
    expect(last_set_this_value === old_this_value).toBeTrue();
    last_set_this_value = null;

    expect(() => str.strict_get_private).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => str.non_strict_get_private).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => (str.strict_set_private = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => (str.non_strict_set_private = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
});
test("string object: string property name access", () => {
    expect(new String("").length).toBe(0);
    expect(new String("foo").length).toBe(3);
    expect(new String("foo").bar).toBeUndefined();
    expect(new String("foo").foo).toBe("foo");
    expect(typeof new String("foo").strict_this).toBe("object");
    let str = new String("foo");
    expect(str.strict_this).toBe(str);
    expect(typeof str.non_strict_this).toBe("object");
    expect(str.non_strict_this === str.non_strict_this).toBeTrue();
    expect(str.non_strict_this !== String.prototype).toBeTrue();
    expect(str.non_strict_this instanceof String).toBeTrue();
    expect(new String("foo").non_strict_this !== new String("foo").non_strict_this).toBeTrue();
    expect(new String("foo").strict_this !== new String("foo").strict_this).toBeTrue();

    last_set_this_value = null;
    expect(() => str.strict_get_private).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => str.non_strict_get_private).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => (str.strict_set_private = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
    expect(() => (str.non_strict_set_private = "wat")).toThrow();
    expect(last_set_this_value).toBeNull();
    last_set_this_value = null;
});
