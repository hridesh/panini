package org.paninij.apt.maingen;

import org.paninij.apt.maingen.PassiveChild;
import org.paninij.lang.Capsule;
import org.paninij.lang.Wired;

@Capsule
public class ActiveChildTemplate {

    @Wired PassiveChild p;

    public void run() {
        System.out.println("Hello world");
    }
}
