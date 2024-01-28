import { Spline } from '../src/spline';

describe("Interpolation", () => {
    it("linear interpolation for two points", () => {
        let spline = new Spline([0, 1], [0, 2]);
        expect(spline.at(0.25)).toEqual(0.5);
    });
    it("cubic curve", () => {
        let spline = new Spline([-1, 0, 1], [2, 0, 2]);
        let f = (x: number) => x * x * (3 - Math.abs(x));
        // interpolation
        expect(spline.at(0.3)).toBeCloseTo(f(0.3), 8);
        expect(spline.at(0.5)).toBeCloseTo(f(0.5), 8);
        expect(spline.at(-0.2)).toBeCloseTo(f(-0.2), 8);
        // extrapolation
        expect(spline.at(2)).toBeCloseTo(5, 8);
        expect(spline.at(-2)).toBeCloseTo(5, 8);
    });
    it("approximating sin(x)", () => {
        let x: number[] = [];
        let y: number[] = [];
        for (let i = 0; i < 15; i++) {
            x[i] = 0.1 * (i + 0.05 * i);
            y[i] = Math.sin(x[i]);
        }
        let spline = new Spline(x, y);
        expect(spline.at(0.3)).toBeCloseTo(Math.sin(0.3), 5);
        expect(spline.at(0.7)).toBeCloseTo(Math.sin(0.7), 5);
        expect(spline.at(1)).toBeCloseTo(Math.sin(1), 5);
        expect(spline.at(1.3)).toBeCloseTo(Math.sin(1.3), 3);
    });
});

describe("Inverse", () => {
    it("sline of inverse", () => {
        let spline = new Spline([0, 1, 3, 7, 9], [1, 3, 5, 9, 16]);
        expect(spline.at(spline.inverse(0.4, 1e-10))).toBeCloseTo(0.4, 9);
        expect(spline.at(spline.inverse(1.8, 1e-10))).toBeCloseTo(1.8, 9);
        expect(spline.at(spline.inverse(6.6, 1e-10))).toBeCloseTo(6.6, 9);
        expect(spline.at(spline.inverse(14.3, 1e-10))).toBeCloseTo(14.3, 9);
    });
    it("inverse of spline", () => {
        let spline = new Spline([0, 1, 3, 7, 9], [1, 3, 5, 9, 16]);
        expect(spline.inverse(spline.at(0.4), 1e-10)).toBeCloseTo(0.4, 9);
        expect(spline.inverse(spline.at(1.8), 1e-10)).toBeCloseTo(1.8, 9);
        expect(spline.inverse(spline.at(5.5), 1e-10)).toBeCloseTo(5.5, 9);
        expect(spline.inverse(spline.at(7.3), 1e-10)).toBeCloseTo(7.3, 9);
    });
});

describe("Misc", () => {
    it("extracting curve parameter", () => {
        let spline = new Spline([0, 1, 3, 7, 9], [1, 3, 5, 9, 16]);
        expect(spline.param(0.5)).toBeCloseTo(0.5, 9);
        expect(spline.param(4)).toBeCloseTo(2.25, 9);
    });
});

describe("Error conditions", () => {
    it("single point", () => {
        expect(() => { new Spline([0], [2]) }).toThrow();
    });
    it("nonincreasing x", () => {
        expect(() => new Spline([0, 0, 1], [2, 0, 2])).toThrow();
        expect(() => new Spline([1, 2, 2], [2, 0, 2])).toThrow();
    });
    it("different number of points", () => {
        expect(() => new Spline([0, 1, 2, 3], [2, 0, 2])).toThrow();
    });
    it("inverse of non-increasing", () => {
        expect(() => new Spline([0, 1, 2], [2, 0, 2]).inverse(0.5)).toThrow();
    });
});


