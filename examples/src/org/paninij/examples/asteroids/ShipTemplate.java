package org.paninij.examples.asteroids;

import org.paninij.lang.Capsule;

@Capsule
public class ShipTemplate {
    private short state;
    private int x;

    protected void init() {
        this.state = 0;
        this.x = Constants.WIDTH/2;
    }

    public void die() {
        this.state = 2;
    }

    public void fire() {
        this.state = 1;
    }

    public boolean isAlive() {
        return state != 2;
    }

    public boolean isFiring() {
        if (this.state == 1) {
            this.state = 0;
            return true;
        }
        return false;
    }

    public int getPosition() { return this.x; }
    public void moveLeft() { if (this.x > 0) this.x--; }
    public void moveRight() { if (this.x < Constants.WIDTH) this.x++; }
}