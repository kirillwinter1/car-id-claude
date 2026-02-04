package ru.car.util.svg;

public class Point {
    int x;
    int y;
    int val;
    Dir dir = Dir.UP;

    public Point(int x, int y, int val) {
        this.x = x;
        this.y = y;
        this.val = val;
    }

    public double getRx() {
        switch (dir) {
            case UP :  return x + 0.5;
            case DOWN : return  x + 0.5;
            case LEFT : return  x;
            case RIGHT: return  x + 1;
        }
        return x;
    }

    public double getRy() {
        switch (dir) {
            case LEFT :  return y + 0.5;
            case RIGHT : return  y + 0.5;
            case UP : return  y;
            case DOWN : return  y + 1;
        }
        return y;
    }

    void up() {
        y--;
    }

    void right() {
        x++;
    }

    void left() {
        x--;
    }

    void down() {
        y++;
    }
}
