package com.custom.jmeter;

import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import java.util.UUID;

public class BoundaryUUIDCorrelator extends AbstractTestElement implements PreProcessor {
    
    // The variable name that the GUI will inject into the HTTP Request
    private static final String VAR_NAME = "DYNAMIC_UUID";

    @Override
    public void process() {
        JMeterVariables vars = JMeterContextService.getContext().getVariables();
        // Generate a new UUID right before the HTTP Request executes
        vars.put(VAR_NAME, UUID.randomUUID().toString());
    }
}