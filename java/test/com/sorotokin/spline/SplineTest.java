package com.sorotokin.spline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import junit.framework.TestCase;

public class SplineTest extends TestCase {
    @Test
    public void testInterpolation_linear_interpolation_for_two_points() {
        Spline spline = Spline.from(new double[] { 0, 1 }, new double[] { 0, 2 });
        assertEquals(0.5, spline.at(0.25));
    }

    private double f(double x) {
        return x * x * (3 - Math.abs(x));
    }

    @Test
    public void testInterpolation_cubic_curve() {
        Spline spline = Spline.from(new double[] { -1, 0, 1 }, new double[] { 2, 0, 2 });
        // interpolation
        assertEquals(f(1), spline.at(1), 1e-10);
        assertEquals(f(0.3), spline.at(0.3), 1e-10);
        assertEquals(f(0.5), spline.at(0.5), 1e-10);
        assertEquals(f(0), spline.at(0), 1e-10);
        assertEquals(f(-0.2), spline.at(-0.2), 1e-10);
        assertEquals(f(-1), spline.at(-1), 1e-10);
        // extrapolation
        assertEquals(5.0, spline.at(2), 1e-10);
        assertEquals(5.0, spline.at(-2), 1e-10);
    }

    @Test
    public void testInterpolation_approximating_sin() {
        final int n = 15;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = 0.1 * (i + 0.05 * i);
            y[i] = Math.sin(x[i]);
        }
        Spline spline = Spline.from(x, y);
        assertEquals(Math.sin(0.3), spline.at(0.3), 1e-5);
        assertEquals(Math.sin(0.7), spline.at(0.7), 1e-5);
        assertEquals(Math.sin(1), spline.at(1), 1e-5);
        assertEquals(Math.sin(1.3), spline.at(1.3), 1e-3);
    };

    @Test
    public void testInverse_spline_of_inverse() {
        Spline spline = Spline.from(new double[] { 0, 1, 3, 7, 9 }, new double[] { 1, 3, 5, 9, 16 });
        assertEquals(0.4, spline.at(spline.inverse(0.4, 1e-10)), 1e-9);
        assertEquals(3.0, spline.at(spline.inverse(3, 1e-10)), 1e-9);
        assertEquals(1.8, spline.at(spline.inverse(1.8, 1e-10)), 1e-9);
        assertEquals(6.6, spline.at(spline.inverse(6.6, 1e-10)), 1e-9);
        assertEquals(14.3, spline.at(spline.inverse(14.3, 1e-10)), 1e-9);
    }

    @Test
    public void testInverse_inverse_of_spline() {
        Spline spline = Spline.from(new double[] { 0, 1, 3, 7, 9 }, new double[] { 1, 3, 5, 9, 16 });
        assertEquals(0.4, spline.inverse(spline.at(0.4), 1e-10), 1e-9);
        assertEquals(1.0, spline.inverse(spline.at(1), 1e-10), 1e-9);
        assertEquals(1.8, spline.inverse(spline.at(1.8), 1e-10), 1e-9);
        assertEquals(5.5, spline.inverse(spline.at(5.5), 1e-10), 1e-9);
        assertEquals(7.3, spline.inverse(spline.at(7.3), 1e-10), 1e-9);
    }

    @Test
    public void testMisc_extracting_curve_parameter() {
        Spline spline = Spline.from(new double[] { 0, 1, 3, 7, 9 }, new double[] { 1, 3, 5, 9, 16 });
        assertEquals(0.5, spline.param(0.5), 1e-9);
        assertEquals(1.0, spline.param(1.0), 1e-9);
        assertEquals(2.25, spline.param(4), 1e-9);
    }

    @Test
    public void testErrorConditions_single_point() {
        assertThrows(IllegalArgumentException.class,
                () -> Spline.from(new double[] { 0 }, new double[] { 2 }));
    }

    @Test
    public void testErrorConditions_nonincreasing_x() {
        assertThrows(IllegalArgumentException.class,
                () -> Spline.from(new double[] { 0, 0, 1 }, new double[] { 2, 0, 2 }));
        assertThrows(IllegalArgumentException.class,
                () -> Spline.from(new double[] { 1, 2, 2 }, new double[] { 2, 0, 2 }));
    }

    @Test
    public void testErrorConditions_different_number_of_points() {
        assertThrows(IllegalArgumentException.class,
                () -> Spline.from(new double[] { 0, 1, 2, 3 }, new double[] { 2, 0, 2 }));
    }

    @Test
    public void testErrorConditions_inverse_of_non_increasing() {
        assertThrows(IllegalStateException.class,
                () -> Spline.from(new double[] { 0, 1, 2 }, new double[] { 2, 0, 2 }).inverse(0.5));
    }
}