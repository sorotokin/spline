package com.sorotokin.spline;

import java.util.Arrays;

/**
 * Creates a smooth "cubic spline" interpolation so that spline.at(x[i]) == y[i]
 * for all the given points and y = spline.at(x) smoothly changes as x changes.
 */
public final class Spline {
    private final double[] x;
    private final double[] y;
    private final double[] k;
    private final boolean increasingY;

    static public Spline from(double[] x, double[] y) {
        return new Spline(Arrays.copyOf(x, x.length), Arrays.copyOf(y, y.length));
    }

    static public Spline from(float[] x, float[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Different number of points");
        }
        double[] xc = new double[x.length];
        double[] yc = new double[y.length];
        for (int i = 0; i < xc.length; i++) {
            xc[i] = x[i];
            yc[i] = y[i];
        }
        return new Spline(xc, yc);
    }

    private Spline(double[] x, double[] y) {
        this.x = x;
        this.y = y;
        int n1 = x.length;
        int n = n1 - 1;

        if (n == 0) {
            throw new IllegalArgumentException("Only a single point provided");
        }

        if (n1 != y.length) {
            throw new IllegalArgumentException("Different number of points");
        }

        double[] k = new double[n1];
        this.k = k;
        if (n1 == 1) {
            double dx = (x[1] - x[0]);
            if (dx <= 0) {
                throw new IllegalArgumentException("Non-increasing x values");
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
        double[] a = new double[n1];
        double[] b = new double[n1];

        // Notation is for i: ql=q[i-1], qc=q[i]
        // start with i = 1
        double dx = (x[1] - x[0]);
        if (dx <= 0) {
            throw new IllegalArgumentException("Non-increasing x values");
        }
        double sl = 1 / dx;
        double rl = 3 * (y[1] - y[0]) * sl * sl;
        boolean increasingY = rl > 0;
        a[1] = -0.5;
        b[1] = rl / (2 * sl);
        for (int i = 1; i < n; i++) {
            int i1 = i + 1;
            dx = (x[i1] - x[i]);
            if (dx <= 0) {
                throw new IllegalArgumentException("Non-increasing x values");
            }
            double sc = 1 / dx;
            double rc = 3 * (y[i1] - y[i]) * sc * sc;
            if (rc <= 0) {
                increasingY = false;
            }
            double d = sl * a[i] + 2 * (sc + sl);
            a[i1] = -sc / d;
            b[i1] = (rl + rc - sl * b[i]) / d;
            sl = sc;
            rl = rc;
        }
        this.increasingY = increasingY;

        k[n] = (rl - sl * b[n]) / (sl * (2 + a[n]));

        for (int i = n; i > 0; i--) {
            k[i - 1] = a[i] * k[i] + b[i];
        }
    }

    /**
     * Finds curve "parameter" for the given x.
     * 
     * param(x[i]) == i for the array x that was used to build this Spline.
     * param(x) is linearly interpolated for other values.
     */
    public double param(double xt) {
        double[] x = this.x;
        int l = x.length;
        int i = Arrays.binarySearch(x, xt);
        if (i >= 0) {
            // found exact match
            return i;
        }
        i = -(i + 1);
        if (i == 0) {
            return (xt - x[0]) / (x[1] - x[0]);
        }
        if (i >= l) {
            int n = l - 1;
            return n + (xt - x[n]) / (x[n] - x[n - 1]);
        }
        int il = i - 1;
        return il + (xt - x[il]) / (x[i] - x[il]);
    }

    /**
     * Computes interpolation for y for the given x.
     */
    public double at(double xt) {
        double[] x = this.x;
        double[] y = this.y;
        double[] k = this.k;
        int l = x.length;
        int i = Arrays.binarySearch(x, xt);
        if (i >= 0) {
            // found exact match
            return y[i];
        }
        i = -(i + 1);
        if (i == 0) {
            return k[0] * (xt - x[0]) + y[0];
        }
        if (i >= l) {
            int n = l - 1;
            return k[n] * (xt - x[n]) + y[n];
        }
        int il = i - 1;
        double xl = x[il];
        double xc = x[i];
        double dx = xc - xl;
        double yl = y[il];
        double yc = y[i];
        double dy = yc - yl;
        double t = (xt - xl) / dx;
        double a = k[il] * dx - dy;
        double b = dy - k[i] * dx;
        double q = 1 - t;
        return q * (yl + t * (a * q + b * t)) + t * yc;
    }

    public double inverse(double yt) {
        return inverse(yt, 1e-8);
    }

    /**
     * Computes approximate xt such that at(xt) = yt.
     * 
     * This is only supported if interpolation was built using increasing sequence
     * of y numbers (which still does not guarantee that interpolation istelf will
     * monotonically increase! - in which case it is not define which solution
     * will be found).
     */
    public double inverse(double yt, double maxErr) {
        if (!this.increasingY) {
            throw new IllegalStateException("Function is not increasing in y");
        }
        double[] x = this.x;
        double[] y = this.y;
        double[] k = this.k;
        int l = x.length;
        int i = Arrays.binarySearch(y, yt);
        if (i >= 0) {
            // found exact match
            return x[i];
        }
        i = -(i + 1);
        if (i == 0) {
            return (yt - y[0]) / k[0] + x[0];
        }
        if (i >= l) {
            int n = l - 1;
            return (yt - y[n]) / k[n] + x[n];
        }
        int il = i - 1;
        double xl = x[il];
        double xc = x[i];
        double dx = xc - xl;
        double yl = y[il];
        double yc = y[i];
        double dy = yc - yl;
        double a = k[il] * dx - dy;
        double b = dy - k[i] * dx;
        double t = (yt - yl) / (yc - yl); // initial approximation
        double tl = 0;
        double tr = 1;
        double expectedErr = dy;
        while (true) {
            double q = 1 - t;
            double errR = q * (yl + t * (a * q + b * t)) + t * yc - yt;
            double err;
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
                double fprime = yc - yl + q * (a * q - 2 * t * (a + b)) - b * t * t;
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

    /**
     * Interpolation curve as a sequence of a single moveTo (first two elements of
     * the
     * array) and curveTo (each group of 6 elements after that).
     */
    public double[] asPath() {
        double[] x = this.x;
        double[] y = this.y;
        double[] k = this.k;
        int l = x.length;
        double[] r = new double[l * 6 - 4];
        double xl = x[0];
        double yl = y[0];
        double kl = k[0];
        r[0] = xl;
        r[1] = yl;
        int p = 2;
        for (int i = 1; i < l; i++) {
            double xc = x[i];
            double yc = y[i];
            double kc = k[i];
            double dx3 = (xc - xl) / 3;
            r[p++] = xl + dx3;
            r[p++] = yl + kl * dx3;
            r[p++] = xc - dx3;
            r[p++] = yc - kc * dx3;
            r[p++] = xc;
            r[p++] = yc;
            xl = xc;
            yl = yc;
            kl = kc;
        }
        return r;
    }

    /**
     * Parametric interpolation curve using SVG path syntax.
     * 
     * For parametric interpolation we have two functions x(t) and y(t) where t is
     * a parameter. Each t value can be mapped to a 2d point (x, y). In such case
     * two separate Spline objects are used (splineX to map t values to x, and
     * splineY
     * to map t values to y; natuarally a set of values in t must match: splineX.x
     * array must be exactly equal to splineY.x).
     * 
     * This function produces a curve as a sequence of a single moveTo (first two
     * elements of the array) and curveTo (each group of 6 elements after that).
     * 'this' should be a spline for x coordinates and 'other' should be a spline
     * for y.
     */
    public double[] asPath2(Spline other) {
        double[] t = this.x;
        int l = t.length;
        if (l != other.x.length) {
            throw new IllegalArgumentException("Inconsistent spline lengths");
        }
        for (int i = 0; i < t.length; i++) {
            if (t[i] != other.x[i]) {
                throw new IllegalArgumentException("Inconsistent spline parameters");
            }
        }
        double[] x = this.y;
        double[] kx = this.k;
        double[] y = other.y;
        double[] ky = other.k;
        double tl = t[0];
        double xl = x[0];
        double yl = y[0];
        double kxl = kx[0];
        double kyl = ky[0];
        double[] r = new double[l * 6 - 4];
        r[0] = xl;
        r[1] = yl;
        int p = 2;
        for (int i = 1; i < l; i++) {
            double tc = t[i];
            double xc = x[i];
            double yc = y[i];
            double kxc = kx[i];
            double kyc = ky[i];
            double dt3 = (tc - tl) / 3;
            r[p++] = xl + kxl * dt3;
            r[p++] = yl + kyl * dt3;
            r[p++] = xc - kxc * dt3;
            r[p++] = yc - kyc * dt3;
            r[p++] = xc;
            r[p++] = yc;
            tl = tc;
            xl = xc;
            yl = yc;
            kxl = kxc;
            kyl = kyc;
        }
        return r;
    }
}