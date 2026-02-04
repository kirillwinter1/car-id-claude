package ru.car.util.svg;

import com.google.zxing.common.BitMatrix;

public class Matrix {
    private static final double RADIUS = 0.5;

    private final int w;
    private final int h;
    private final int[][] m;
    private final int cornerSize;


    public Matrix(BitMatrix bitMatrix) {
        int[] shape = bitMatrix.getEnclosingRectangle();
        w = shape[2];
        h = shape[3];
        m = new int[h + 2][w + 2];

        int number = 0;

        for (int i = 0; i < shape[3]; i++) {
            int j = 0;
            for (; j < shape[2]; ) {
                //skip
                while (j < shape[2] && (!bitMatrix.get(i + shape[0], j + shape[1]) || get(i,j) != 0)) {
                    j++;
                }

                int y = i;
                int x = j;
                if (bitMatrix.get(i + shape[0], j + shape[1])) {
                    number ++;
                    fullFill(bitMatrix, x, y, number);
                }
            }
        }

        cornerSize = getCornerSize();
        clearCorners();
        clearCenter();
        clear(-1, -1);
    }

    private void fullFill(BitMatrix bitMatrix, int x, int y, int num) {
        int[] shape = bitMatrix.getEnclosingRectangle();

        if (!bitMatrix.get(y + shape[0], x + shape[1]) || get(y, x) != 0) {
            return;
        }
        set(y, x, num);
        fullFill(bitMatrix, x + 1, y, num);
        fullFill(bitMatrix, x - 1, y, num);
        fullFill(bitMatrix, x, y - 1, num);
        fullFill(bitMatrix, x, y + 1, num);
    }

    private void fullFill(int x, int y, int fill, int num) {
        if ( x < -1 || y < -1 || x > w || y > h || get(y, x) != fill) {
            return;
        }

        set(y, x, num);
        fullFill(x + 1, y, fill, num);
        fullFill(x - 1, y, fill, num);
        fullFill(x, y - 1, fill, num);
        fullFill(x, y + 1, fill, num);
    }

    private void clear(int x, int y) {
        if ( x < -1 || y < -1 || x > w || y > h || get(y, x) != 0) {
            return;
        }

        set(y, x, -1);
        clear(x + 1, y);
        clear(x - 1, y);
        clear(x, y - 1);
        clear(x, y + 1);
        clear(x + 1, y + 1);
        clear(x + 1, y - 1);
        clear(x - 1, y - 1);
        clear(x - 1, y + 1);
    }

    private int getCornerSize() {
        int x = 0;
        while (get(0, x) != 0) {
            x++;
        }
        return x;
    }

    private void clearCorners() {
        for (int y = 0; y < cornerSize; y++) {
            for (int x = 0; x < cornerSize; x++) {
                set(y, x, 0);
                set(y, x + w - cornerSize, 0);
                set(y + h - cornerSize, x, 0);
            }
        }
    }

    private void clearCenter() {
        int l = 14;
//        int l = 10;
        int s = (w - l) / 2;
        for (int y = s ; y <= s + l; y++) {
            for (int x = s; x <= s + l; x++) {
                set(y, x, 0);
            }
        }
    }

    private void fillCorners(StringBuilder sb, String emptyStyle) {
        fillCorner(sb, 0, 0, emptyStyle);
        fillCorner(sb, w - cornerSize, 0, emptyStyle);
        fillCorner(sb, 0, h - cornerSize, emptyStyle);
    }

    private void fillCorner(StringBuilder sb, int x, int y, String emptyStyle) {
        rect(sb, x, y, cornerSize, cornerSize, RADIUS * 4, "");
        rect(sb, x + 1, y + 1, cornerSize - 2, cornerSize - 2, RADIUS * 2, emptyStyle);
        rect(sb, x + 2, y + 2, cornerSize - 4, cornerSize - 4, RADIUS, "");
    }

    public String toXml(String emptyStyle) {
        StringBuilder sb = new StringBuilder();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (get(y, x) == -1)  {
                    continue;
                }
                if (get(y, x) == 0) {
                    fill(sb, x, y, get(y, x), emptyStyle);
                } else {
                    fill(sb, x, y, get(y, x), "");
                }
                fullFill(x, y, get(y, x), -1);
            }
        }
        fillCorners(sb, emptyStyle);

        return sb.toString();
    }

    private void fill(StringBuilder sb, int x, int y, int num, String style) {
        Point p = new Point(x, y, num);

        if (!eqRight(p) && !eqLeft(p) && !eqBottom(p) && !eqTop(p)) {
            rect(sb, x, y, 1,1, RADIUS, style);
            return;
        }

        sb.append("<path").append(style).append(" d=\"M").append(p.getRx()).append(" ").append(p.getRy());

        while (true) {
            if (p.dir == Dir.UP && eqRight(p) && !eqTopRight(p)) {
                while (eqRight(p) && !eqTopRight(p)) {
                    p.right();
                }
                line(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.LEFT && eqTop(p) && !eqTopLeft(p)) {
                while (eqTop(p) && !eqTopLeft(p)) {
                    p.up();
                }
                line(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.DOWN && eqLeft(p) && !eqBottomLeft(p)) {
                while (eqLeft(p) && !eqBottomLeft(p)) {
                    p.left();
                }
                line(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.RIGHT && eqBottom(p) && !eqBottomRight(p)) {
                while (eqBottom(p) && !eqBottomRight(p)) {
                    p.down();
                }
                line(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.UP && !eqRight(p) && !eqBottom(p)) { //180
                p.dir = Dir.DOWN;
                angle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.UP && !eqRight(p)) { //90
                p.dir = Dir.RIGHT;
                angle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.LEFT && !eqTop(p) && !eqRight(p) && !(p.x == x && p.y == y)) { //180
                p.dir = Dir.RIGHT;
                angle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.LEFT && !eqTop(p)) { //90
                p.dir = Dir.UP;
                angle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.DOWN && !eqLeft(p) && !eqTop(p)) { //180
                p.dir = Dir.UP;
                angle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.DOWN && !eqLeft(p)) { //90
                p.dir = Dir.LEFT;
                angle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.RIGHT && !eqBottom(p) && !eqLeft(p)) { //180
                p.dir = Dir.LEFT;
                angle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.RIGHT && !eqBottom(p)) { //90
                p.dir = Dir.DOWN;
                angle(sb, p.getRx(), p.getRy());

            } else if (p.dir == Dir.UP && eqRight(p) && eqTopRight(p)) {
                p.right();
                p.up();
                p.dir = Dir.LEFT;
                reverseAngle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.LEFT && eqTop(p) && eqTopLeft(p)) {
                p.up();
                p.left();
                p.dir = Dir.DOWN;
                reverseAngle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.DOWN && eqLeft(p) && eqBottomLeft(p)) {
                p.left();
                p.down();
                p.dir = Dir.RIGHT;
                reverseAngle(sb, p.getRx(), p.getRy());
            } else if (p.dir == Dir.RIGHT && eqBottom(p) && eqBottomRight(p)) {
                p.down();
                p.right();
                p.dir = Dir.UP;
                reverseAngle(sb, p.getRx(), p.getRy());
            }
            
            if (p.x == x && p.y == y && p.dir == Dir.UP) {
                break;
            }
        }

        sb.append("Z\"/>");
    }

    private static void line(StringBuilder sb, double x2, double y2) {
        sb.append("L");
        append(sb, x2);
        sb.append(" ");
        append(sb, y2);
    }

    private static void angle(StringBuilder sb, double x2, double y2) {
        sb.append("A0.5 0.5 0 0 1 ");
        append(sb, x2);
        sb.append(" ");
        append(sb, y2);
    }

    private static void reverseAngle(StringBuilder sb, double x2, double y2) {
        sb.append("A0.5 0.5 0 0 0 ");
        append(sb, x2);
        sb.append(" ");
        append(sb, y2);
    }

    private static void append(StringBuilder sb, double val) {
        int v = (int)val;
        if (val == v) {
            sb.append(v);
        } else {
            sb.append(val);
        }
    }

    private static void rect(StringBuilder sb, int x1, int y1, int w, int h, double r, String style) {
        sb.append("<rect").append(style).append(" x=\"").append(x1).append("\"")
                .append(" y=\"").append(y1).append("\"")
                .append(" width=\"").append(w).append("\"")
                .append(" height=\"").append(h).append("\"")
                .append(" rx=\"").append(r).append("\" ry=\"").append(r).append("\" ").append("/>");
    }

    void set(int y, int x, int num) {
        m[y + 1][x + 1] = num;
    }


    boolean eqRight(Point p) {
        return get(p.y, p.x + 1) == p.val;
    }
    boolean eqLeft(Point p) {
        return get(p.y, p.x - 1) == p.val;
    }
    boolean eqTop(Point p) {
        return get(p.y - 1, p.x) == p.val;
    }
    boolean eqBottom(Point p) {
        return get(p.y + 1, p.x) == p.val;
    }
    boolean eqTopRight(Point p) {
        return get(p.y - 1, p.x + 1) == p.val;
    }
    boolean eqTopLeft(Point p) {
        return get(p.y - 1, p.x - 1) == p.val;
    }
    boolean eqBottomRight(Point p) {
        return get(p.y + 1, p.x + 1) == p.val;
    }
    boolean eqBottomLeft(Point p) {
        return get(p.y + 1, p.x - 1) == p.val;
    }
    int get(Point p) {
        return m[p.y + 1][p.x + 1];
    }
    
    int get(int y, int x) {
        return m[y + 1][x + 1];
    }
}
