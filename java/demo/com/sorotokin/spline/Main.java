package com.sorotokin.spline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.MouseInputListener;

import com.sorotokin.spline.Spline;

final class Main extends JComponent {
    final boolean parametric;
    final double[] xs = { 30, 160, 310, 420, 520, 610 };
    final double[] ys = { 250, 130, 180, 100, 160, 200 };

    final static double R = 5;

    private int selectedPointIndex = -1;

    Main(boolean parametric) {
        this.parametric = parametric;
        PointManipulator manipulator = new PointManipulator();
        addMouseListener(manipulator);
        addMouseMotionListener(manipulator);
    }

    private double[] generatePathData() {
        if (parametric) {
            double[] is = new double[xs.length];
            for (int i = 1; i < is.length; i++) {
                double dx = xs[i] - xs[i - 1];
                double dy = ys[i] - ys[i - 1];
                is[i] = is[i - 1] + Math.sqrt(dx * dx + dy * dy) + 0.00001;
            }
            return Spline.from(is, xs).asPath2(Spline.from(is, ys));
        } else {
            return Spline.from(xs, ys).asPath();
        }
    }

    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        GeneralPath path = new GeneralPath();
        double[] pts = generatePathData();
        path.moveTo(pts[0], pts[1]);
        for (int k = 2; k < pts.length; k += 6) {
            path.curveTo(pts[k], pts[k + 1], pts[k + 2], pts[k + 3], pts[k + 4], pts[k + 5]);
        }
        g.setStroke(new BasicStroke(1.0f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(Color.BLUE);
        g.draw(path);
        g.setPaint(Color.RED);
        for (int i = 0; i < xs.length; i++) {
            Shape shape = new Ellipse2D.Double(xs[i] - R, ys[i] - R, 2 * R, 2 * R);
            g.fill(shape);
            if (i == selectedPointIndex) {
                Graphics2D g1 = (Graphics2D) g.create();
                g1.setColor(Color.BLACK);
                g1.draw(shape);
                g1.dispose();
            }
        }
    }

    final class PointManipulator implements MouseInputListener {
        double dx0;
        double dy0;

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            double x = e.getX();
            double y = e.getY();
            for (int i = 0; i < xs.length; i++) {
                double dx = x - xs[i];
                double dy = y - ys[i];
                if (dx * dx + dy * dy <= R * R) {
                    selectedPointIndex = i;
                    dx0 = dx;
                    dy0 = dy;
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    repaint();
                    break;
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (selectedPointIndex >= 0) {
                selectedPointIndex = -1;
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                repaint();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedPointIndex >= 0) {
                double minX = R;
                double maxX = getWidth() - R;
                if (!parametric) {
                    if (selectedPointIndex > 0) {
                        minX = xs[selectedPointIndex - 1] + 2 * R;
                    }
                    if (selectedPointIndex < xs.length - 1) {
                        maxX = xs[selectedPointIndex + 1] - 2 * R;
                    }
                }
                xs[selectedPointIndex] = Math.min(maxX, Math.max(minX, e.getX() - dx0));
                ys[selectedPointIndex] = e.getY() - dy0;
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }

    public static final void main(String[] args) {
        JTabbedPane pane = new JTabbedPane();
        pane.addTab("Spline curve", new Main(false));
        pane.addTab("Parametric spline curve", new Main(true));
        JFrame frame = new JFrame("Spline demo");
        frame.setContentPane(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 400);
        frame.setVisible(true);
    }
}