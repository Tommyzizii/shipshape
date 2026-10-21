package com.shipshape;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModularityTests {
    
    ApplicationModules modules = ApplicationModules.of(ShipshapeApplication.class);

    @Test 
    void verifiesModularStructure(){
        modules.verify();
    }

    @Test 
    void printsModules(){
        modules.forEach(System.out::println);
    }


}
