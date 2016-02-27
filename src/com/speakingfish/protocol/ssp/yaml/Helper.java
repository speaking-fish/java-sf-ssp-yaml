package com.speakingfish.protocol.ssp.yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map.Entry;

import com.speakingfish.common.Maps;
import com.speakingfish.common.function.Mapper;
import com.speakingfish.common.mapper.Mappers;
import com.speakingfish.protocol.ssp.Any;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import org.yaml.snakeyaml.nodes.AnchorNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import static com.speakingfish.common.Compares.*;
import static com.speakingfish.common.collection.CollectionHelper.*;
import static com.speakingfish.common.closeable.Closeables.*;
import static com.speakingfish.common.iterator.Iterators.*;
import static com.speakingfish.common.mapper.Mappers.*;
import static com.speakingfish.protocol.ssp.Types.*;
import static com.speakingfish.protocol.ssp.Helper.*;

public class Helper {
    
    public static final Mapper<Any<?>, Node> MAPPER_NODE_TO_ANY = new Mapper<Any<?>, Node>() {
        public Any<?> apply(Node src) { return toAny(src); }
    };  

    public static final Mapper<Node, Any<?>> MAPPER_ANY_TO_NODE = new Mapper<Node, Any<?>>() {
        public Node apply(Any<?> src) { return newNode(src); }
    };  

    public static final Mapper<Entry<String, Any<?>>, NodeTuple> MAPPER_NODE_TUPLE_TO_ANY_ENTRY = new Mapper<Entry<String, Any<?>>, NodeTuple>() {
        public Entry<String, Any<?>> apply(NodeTuple src) { 
            return Maps.<String, Any<?>>keyValue(((ScalarNode) src.getKeyNode()).getValue(), toAny(src.getValueNode())); }
    };

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final Mapper<Entry<String, ? extends Any<?>>, NodeTuple> mapperNodeTupleToAnyEntry() {
        return (Mapper<Entry<String, ? extends Any<?>>, NodeTuple>) (Mapper) MAPPER_NODE_TUPLE_TO_ANY_ENTRY;
    }
    
    public static final Mapper<NodeTuple, Entry<String, Any<?>>> MAPPER_ANY_ENTRY_TO_NODE_TUPLE = new Mapper<NodeTuple, Entry<String, Any<?>>>() {
        public NodeTuple apply(Entry<String, Any<?>> src) { 
            return new NodeTuple(new ScalarNode(Tag.STR, src.getKey(), null, null, null), newNode(src.getValue())); }
    };
    
    public static Any<?> readYaml(Reader in) {
        return toAny(new Yaml().compose(in));
    }

    public static Any<?> readYaml(InputStream in) {
        final InputStreamReader reader = new InputStreamReader(in);
        try {
            return readYaml(reader);
        } finally {
            catchClose(reader);
        }
    }
    
    public static Any<?> readYaml(String in) {
        return readYaml(new StringReader(in));
    }

    public static Any<?> toAny(Node node) {
        if(null == node) {
            return null;
        }
        if(node instanceof ScalarNode) {
            return scalarNodeToAny((ScalarNode) node);
        } else if(node instanceof AnchorNode) {
            return toAny(((AnchorNode) node).getRealNode());
        } else if(node instanceof MappingNode) {
            return mappingNodeToAny((MappingNode) node);
        } else if(node instanceof SequenceNode) {
            return sequenceNodeToAny((SequenceNode) node);
        } else {
            throw new IllegalArgumentException("Unsupported node type: " + node.getClass());
        }
    }
    
    public static Any<?> sequenceNodeToAny(SequenceNode node) {
        return anyArray(mapIterator(node.getValue().iterator(), MAPPER_NODE_TO_ANY));
    }

    public static Any<?> mappingNodeToAny(MappingNode node) {
        return anyObject(iterableOf(mapIterator(node.getValue().iterator(), mapperNodeTupleToAnyEntry())));
    }

    public static Any<?> scalarNodeToAny(ScalarNode node) {
        final Tag tag = node.getTag();
        if(Boolean.FALSE) { return null;
      //} else if(Tag.YAML     .equals(tag)) {
      //} else if(Tag.MERGE    .equals(tag)) {
      //} else if(Tag.SET      .equals(tag)) {
      //} else if(Tag.PAIRS    .equals(tag)) {
      //} else if(Tag.OMAP     .equals(tag)) {
        } else if(Tag.BINARY   .equals(tag)) { return any(        Base64Coder.decode      (node.getValue()) );
        } else if(Tag.INT      .equals(tag)) { return any(        Long       .parseLong   (node.getValue()) );
        } else if(Tag.FLOAT    .equals(tag)) { return any(        Double     .parseDouble (node.getValue()) );
        } else if(Tag.TIMESTAMP.equals(tag)) { return any(                                 node.getValue()  );
        } else if(Tag.BOOL     .equals(tag)) { return any(ordinal(Boolean    .parseBoolean(node.getValue())));
      //} else if(Tag.NULL     .equals(tag)) {
        } else if(Tag.STR      .equals(tag)) { return any(                                 node.getValue()  );
      //} else if(Tag.SEQ      .equals(tag)) {
      //} else if(Tag.MAP      .equals(tag)) {
        } else {
            throw new IllegalArgumentException("Unsupported scalar tag: " + tag);
        }
    }

    public static Node newNode(Any<?> any) {
        if(null == any) {
            return null;
        }
        switch(any.type()) {
          //case SSP_TYPE_HOLDER    :
            case SSP_TYPE_OBJECT    : return new MappingNode(
                                          Tag.MAP,
                                          collect       (new ArrayList<NodeTuple>(), 
                                          mapIterator   (MAPPER_ANY_ENTRY_TO_NODE_TUPLE,
                                          acceptIterator(any.entries().iterator(),
                                          acceptor      (ACCEPTOR_SERIALIZABLE,
                                                         Mappers.<String, Any<?>>mapperEntryValue()
                                                         )))),
                                          true
                                          );
            case SSP_TYPE_ARRAY     : return new SequenceNode(
                                          Tag.SEQ,
                                          collect       (new ArrayList<Node>(),
                                          mapIterator   (MAPPER_ANY_TO_NODE,
                                          acceptIterator(ACCEPTOR_SERIALIZABLE,
                                                         any.values().iterator()
                                                         ))),
                                          true
                                          );
            case SSP_TYPE_STRING    : return new ScalarNode(Tag.STR   , any.asString(), null, null, null);
            case SSP_TYPE_DECIMAL   : return new ScalarNode(Tag.STR   , any.asString(), null, null, null); //TODO
            case SSP_TYPE_INT_8     : return new ScalarNode(Tag.INT   , any.asString(), null, null, null); //TODO
            case SSP_TYPE_INT_16    : return new ScalarNode(Tag.INT   , any.asString(), null, null, null); //TODO
            case SSP_TYPE_INT_32    : return new ScalarNode(Tag.INT   , any.asString(), null, null, null); //TODO
            case SSP_TYPE_INT_64    : return new ScalarNode(Tag.INT   , any.asString(), null, null, null); //TODO
            case SSP_TYPE_FLOAT_32  : return new ScalarNode(Tag.FLOAT , any.asString(), null, null, null); //TODO
            case SSP_TYPE_FLOAT_64  : return new ScalarNode(Tag.FLOAT , any.asString(), null, null, null); //TODO
            case SSP_TYPE_BYTE_ARRAY: return new ScalarNode(Tag.BINARY, any.asString(), null, null, null); //TODO
        default:
            throw new IllegalArgumentException("Unsupported type: " + any.type());
        }
    }
}
