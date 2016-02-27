package com.speakingfish.protocol.ssp.yaml;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

import com.speakingfish.protocol.ssp.Any;

import static org.junit.Assert.*;
import static com.speakingfish.protocol.ssp.Types.*;
import static com.speakingfish.protocol.ssp.yaml.Helper.*;

public class TestYaml {

    @Test public void test() {
        //fail("Not yet implemented");
        final Any<?> srcAny = anyObject(
            named("one", "string value"),
            named("many of", "long\nstring\nvalue\n"),
            named("sometimes", 1)
            );
        final Node srcAnyToNode = newNode(srcAny);
        //Representer ee = new Representer();
        //ee.getPropertyUtils().
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultScalarStyle(ScalarStyle.LITERAL);
        dumperOptions.setDefaultFlowStyle(FlowStyle.FLOW);
        dumperOptions.setLineBreak(LineBreak.UNIX);
        dumperOptions.setSplitLines(true);
        dumperOptions.setPrettyFlow(true);
        
        
        //System.out.println(new Yaml(dumperOptions).dump(srcAnyToNode));
        
        StringWriter sw = new StringWriter();
        Emitter e = new Emitter(sw, dumperOptions);
        Serializer s = new Serializer(e, new Resolver(), dumperOptions, null);
        try {
            s.open();
            s.serialize(srcAnyToNode);
        } catch(IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        System.out.println(sw.toString());
        
    }
    
    public static void main(String[] args) {
        new TestYaml().test();
    } 

}
