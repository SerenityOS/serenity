test("labeled plain scope", () => {
    test: {
        let o = 1;
        expect(o).toBe(1);
        break test;
        expect().fail();
    }
});

test("break on plain scope from inner scope", () => {
    outer: {
        {
            break outer;
        }
        expect().fail();
    }
});

test("labeled for loop with break", () => {
    let counter = 0;
    outer: for (a of [1, 2, 3]) {
        for (b of [4, 5, 6]) {
            if (a === 2 && b === 5) break outer;
            counter++;
        }
    }
    expect(counter).toBe(4);
});

test("labeled for loop with continue", () => {
    let counter = 0;
    outer: for (a of [1, 2, 3]) {
        for (b of [4, 5, 6]) {
            if (b === 6) continue outer;
            counter++;
        }
    }
    expect(counter).toBe(6);
});

test("invalid label across scope", () => {
    expect(`
        label: {
            (() => { break label; });
        }
    `).not.toEval();
});
