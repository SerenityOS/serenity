test("regular comments", () => {
    const source = `var i = 0;

// i++;
/* i++; */
/*
i++;
*/
return i;`;

    expect(source).toEvalTo(0);
});

test("html comments", () => {
    const source = `var i = 0;
<!-- i++; --> i++;
<!-- i++;
i++;
--> i++;
return i;`;

    expect(source).toEvalTo(1);
});

test("unterminated multi-line comment", () => {
    expect("/*").not.toEval();
    expect("/**").not.toEval();
    expect("/*/").not.toEval();
    expect("/* foo").not.toEval();
    expect("foo /*").not.toEval();
});
