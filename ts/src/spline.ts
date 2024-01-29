import { findFirstGood } from "./base.js";

export type SplinePoints = Float64Array | Float32Array | number[];

/**
 * Creates a smooth "cubic spline" interpolation so that spline.at(x[i]) == y[i]
 * for all the given points and y = spline.at(x) smoothly changes as x changes.
 */
export class Spline {
    readonly k: SplinePoints;
    readonly increasingY: boolean;

    constructor(readonly x: SplinePoints, readonly y: SplinePoints) {
        const n1 = x.length;
        const n = n1 - 1;

        if (n == 0) {
            throw new Error("Only a single point provided");
        }

        if (n1 != y.length) {
            throw new Error("Different number of points");
        }

        const k = new Float64Array(n1);
        this.k = k;
        if (n1 == 1) {
            let dx = (x[1] - x[0]);
            if (dx <= 0) {
                throw new Error("Non-increasing x values");
            }
            k[0] = (y[1] - y[0]) / dx;
            k[1] = k[0];
            this.increasingY = k[0] > 0;
            return;
        }

        // Solving for k[0]...k[n]:
        //
        // 2*s[0]*k[0] + s[0]*k[1] = r[0]
        // s[i-1]*k[i-1] + 2*(s[i-1] + s[i])*k[i] + s[i]*k[i+1] = r[i-1] + r[i]
        // s[n-1]*k[n-1] + 2*s[n-1]*k[n] = r[n-1]
        //
        // Where:
        // s[i] = 1/(x[i+1] - x[i])
        // s[n] = 0
        // r[i] = 3*(y[i+1] - y[i])*s[i]^2)

        // Looking for a and b such that for i = 1..n
        // k[i-1] = a[i]*k[i] + b[i]
        const a = new Float64Array(n1);
        const b = new Float64Array(n1);

        // Notation is for i: ql=q[i-1], qc=q[i]
        // start with i = 1
        const dx = (x[1] - x[0]);
        if (dx <= 0) {
            throw new Error("Non-increasing x values");
        }
        let sl = 1 / dx;
        let rl = 3 * (y[1] - y[0]) * sl * sl;
        let increasingY = rl > 0;
        a[1] = -0.5;
        b[1] = rl / (2 * sl);
        for (let i = 1; i < n; i++) {
            let i1 = i + 1;
            const dx = (x[i1] - x[i]);
            if (dx <= 0) {
                throw new Error("Non-increasing x values");
            }
            let sc = 1 / dx;
            let rc = 3 * (y[i1] - y[i]) * sc * sc;
            if (rc <= 0) {
                increasingY = false;
            }
            let d = sl * a[i] + 2 * (sc + sl);
            a[i1] = -sc / d;
            b[i1] = (rl + rc - sl * b[i]) / d;
            sl = sc;
            rl = rc;
        }
        this.increasingY = increasingY;

        k[n] = (rl - sl * b[n]) / (sl * (2 + a[n]));

        for (let i = n; i > 0; i--) {
            k[i - 1] = a[i] * k[i] + b[i];
        }
    }

    /**
     * Finds curve "parameter" for the given x.
     * 
     * param(x[i]) == i for the array x that was used to build this Spline.
     * param(x) is linearly interpolated for other values.
     */
    param(xt: number): number {
        const x = this.x;
        const l = x.length;
        let i = findFirstGood(l, (i: number) => x[i] >= xt);
        if (i == 0) {
            return (xt - x[0]) / (x[1] - x[0]);
        } else if (i >= l) {
            const n = l - 1;
            return n + (xt - x[n]) / (x[n] - x[n - 1]);
        } else {
            const il = i - 1;
            return il + (xt - x[il]) / (x[i] - x[il]);
        }
    }

    /**
     * Computes interpolation for y for the given x.
     */
    at(xt: number): number {
        const x = this.x;
        const y = this.y;
        const k = this.k;
        const l = x.length;
        let i = findFirstGood(l, i => x[i] >= xt);
        if (i == 0) {
            return k[0] * (xt - x[0]) + y[0];
        } else if (i >= l) {
            const n = l - 1;
            return k[n] * (xt - x[n]) + y[n];
        } else {
            const il = i - 1;
            const xl = x[il];
            const xc = x[i];
            const dx = xc - xl;
            const yl = y[il];
            const yc = y[i];
            const dy = yc - yl;
            const t = (xt - xl) / dx;
            const a = k[il] * dx - dy;
            const b = dy - k[i] * dx;
            const q = 1 - t;
            return q * (yl + t * (a * q + b * t)) + t * yc;
        }
    }

    /**
     * Computes approximate xt such that at(xt) = yt.
     * 
     * This is only supported if interpolation was built using increasing sequence
     * of y numbers (which still does not guarantee that interpolation istelf will
     * monotonically increase! - in which case it is not define which solution
     * will be found).
     */
    inverse(yt: number, maxErr: number = 1e-8): number {
        if (!this.increasingY) {
            throw Error("Function is not increasing in y");
        }
        const x = this.x;
        const y = this.y;
        const k = this.k;
        const l = x.length;
        let i = findFirstGood(l, i => y[i] >= yt);
        if (i == 0) {
            return (yt - y[0]) / k[0] + x[0];
        } else if (i >= l) {
            const n = l - 1;
            return (yt - y[n]) / k[n] + x[n];
        } else {
            const il = i - 1;
            const xl = x[il];
            const xc = x[i];
            const dx = xc - xl;
            const yl = y[il];
            const yc = y[i];
            const dy = yc - yl;
            const a = k[il] * dx - dy;
            const b = dy - k[i] * dx;
            let t = (yt - yl) / (yc - yl); // initial approximation
            let tl = 0;
            let tr = 1;
            let expectedErr = dy;
            while (true) {
                const q = 1 - t;
                let errR = q * (yl + t * (a * q + b * t)) + t * yc - yt;
                let err;
                if (errR > 0) {
                    err = errR;
                    tr = t;
                } else {
                    err = -errR;
                    tl = t;
                }
                if (err < maxErr) {
                    break;
                }
                if (err < expectedErr) {
                    let fprime = yc - yl + q * (a * q - 2 * t * (a + b)) - b * t * t;
                    t -= errR / fprime;
                    if (tl < t && t < tr) {
                        // Newton's method step
                        expectedErr = 0.5 * err;
                        continue;
                    }
                }
                // binary search step
                expectedErr = err;
                t = 0.5 * (tl + tr);
            }
            return xl + t * dx;
        }
    }

    /**
     * Interpolation curve using SVG path syntax.
     * @param flip flips x and y
     */
    asPath(flip: boolean = false): string {
        const x = this.x;
        const y = this.y;
        const k = this.k;
        const l = x.length;
        let xl = x[0];
        let yl = y[0];
        let kl = k[0];
        let s = flip ? "M" + yl + " " + xl : "M" + xl + " " + yl;
        for (let i = 1; i < l; i++) {
            const xc = x[i];
            const yc = y[i];
            const kc = k[i];
            const dx3 = (xc - xl) / 3;
            const x1 = xl + dx3;
            const y1 = yl + kl * dx3;
            const x2 = xc - dx3;
            const y2 = yc - kc * dx3;
            if (flip) {
                s += "C" + y1 + " " + x1 + " " + y2 + " " + x2 + " " + yc + " " + xc;
            } else {
                s += "C" + x1 + " " + y1 + " " + x2 + " " + y2 + " " + xc + " " + yc;
            }
            xl = xc;
            yl = yc;
            kl = kc;
        }
        return s;
    }

    /**
     * Parametric interpolation curve using SVG path syntax.
     * 
     * For parametric interpolation we have two functions x(t) and y(t) where t is
     * a parameter. Each t value can be mapped to a 2d point (x, y). In such case
     * two separate Spline objects are used (splineX to map t values to x, and splineY
     * to map t values to y; natuarally a set of values in t must match: splineX.x
     * array must be exactly equal to splineY.x).
     * 
     * This function produces SVG curve for this case; this should be a spline for
     * x coordinates and other should be a spline for y.
     */
    asPath2(other: Spline, scale: number = 1): string {
        const t = this.x;
        if (t.length != other.x.length) {
            throw "Inconsistent spline lengths";
        }
        for (let i = 0; i < t.length; i++) {
            if (t[i] != other.x[i]) {
                throw "Inconsistent spline parameters";
            }
        }
        const x = this.y;
        const kx = this.k;
        const y = other.y;
        const ky = other.k;
        const l = x.length;
        let tl = t[0];
        let xl = x[0];
        let yl = y[0];
        let kxl = kx[0];
        let kyl = ky[0];
        let s = "M" + (scale * xl) + " " + (scale * yl);
        for (let i = 1; i < l; i++) {
            const tc = t[i];
            const xc = x[i];
            const yc = y[i];
            const kxc = kx[i];
            const kyc = ky[i];
            const dt3 = (tc - tl) / 3;
            const x1 = xl + kxl * dt3;
            const y1 = yl + kyl * dt3;
            const x2 = xc - kxc * dt3;
            const y2 = yc - kyc * dt3;
            s += "C" + (scale * x1) + " " + (scale * y1) + " "
                + (scale * x2) + " " + (scale * y2) + " "
                + (scale * xc) + " " + (scale * yc);
            tl = tc;
            xl = xc;
            yl = yc;
            kxl = kxc;
            kyl = kyc;
        }
        return s;
    }
}