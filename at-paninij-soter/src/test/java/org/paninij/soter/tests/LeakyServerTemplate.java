package org.paninij.soter.tests;

import org.paninij.lang.Capsule;

@Capsule
public class LeakyServerTemplate
{
    Secret serverSecret = new Secret();
    
    public Integer getInteger() {
        return new Integer(9);
    }

    public void giveInteger(Integer i) {
        // Nothing to do.
    }
    
    public void giveSecret(Secret s) {
        // Nothing to do.
    }
 
    public Secret getSecret() {
        return serverSecret;
    }
    
    public LeakyServerTemplate getTemplateReference() {
        return this;
    }
}