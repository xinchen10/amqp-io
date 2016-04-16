//  ------------------------------------------------------------------------------------
//  Copyright (c) xinchen
//  All rights reserved. 
//  
//  Licensed under the Apache License, Version 2.0 (the ""License""); you may not use this 
//  file except in compliance with the License. You may obtain a copy of the License at 
//  http://www.apache.org/licenses/LICENSE-2.0  
//  
//  THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
//  EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR 
//  CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR 
//  NON-INFRINGEMENT. 
// 
//  See the Apache Version 2.0 License for specific language governing permissions and 
//  limitations under the License.
//  ------------------------------------------------------------------------------------

package aikexq.amqp.io;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

class CustomType {
    private final ConcurrentHashMap<Class, Encoder> customEncoders;
    
    public CustomType() {
        this.customEncoders = new ConcurrentHashMap();
    }
    
    private static class FieldInfo {
        public String name;
        public int order;
        public Field field;
        public Encoder encoder;
        
        public FieldInfo(String name, int order, Field field, Encoder encoder) {
            this.name = name;
            this.order = order;
            this.field = field;
            this.encoder = encoder;
        }
    }
    
    private static class EnumEncoder implements Encoder {
        private final Class c;
        private final Object[] constants;
        private final Encoder intEncoder;
        
        public EnumEncoder(Class c) {
            this.c = c;
            this.constants = c.getEnumConstants();
            this.intEncoder = PrimitiveType.getEncoder(Integer.TYPE);
        }

        @Override
        public void write(ByteBuffer buffer, Object obj, HashSet path) throws Exception {
            this.intEncoder.write(buffer, ((Enum)obj).ordinal(), path);
        }

        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object read(ByteBuffer buffer) throws Exception {
            int ordinal = (int)this.intEncoder.read(buffer);
            if (ordinal < this.constants.length) {
                return this.constants[ordinal];
            }
            throw new AmqpIoException(c.getName() + ": ordinal out of range " + ordinal);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            throw new UnsupportedOperationException();
        }
    }
    
    private static abstract class CompositEncoder implements Encoder {
        public final String name;
        protected final Class type;
        protected CustomType customType;
        protected Constructor ctor;
        protected FieldInfo[] fields;
        protected Map<String, Encoder> knownTypes;
        
        public CompositEncoder(Class type, String name) {
            this.type = type;
            this.name = name;
        }
        
        public void init(CustomType customType, Constructor ctor,
                FieldInfo[] fields, Map<String, Encoder> knownTypes) {
            this.customType = customType;
            this.ctor = ctor;
            this.fields = fields;
            this.knownTypes = knownTypes;
        }
        
        @Override
        public void write(ByteBuffer buffer, Object obj, HashSet path) throws Exception {
            buffer.put(FormatCode._described);
            writeSymbol(buffer, this.name);
            this.writeRaw(buffer, obj, path);
        }
        
        @Override
        public Object read(ByteBuffer buffer) throws Exception {
            byte code = buffer.get();
            FormatCode.assertEqual(code, FormatCode._described);
            String symbol = readSymbol(buffer);
            Encoder encoder;
            if (symbol.equals(this.name)) {
                encoder = this;
            } else {
                encoder = this.knownTypes.get(symbol);
            }
            if (encoder == null) {
                throw new Exception("Unknown type name " + symbol);
            }
            return encoder.readRaw(buffer, buffer.get());
        }
        
        protected static void writeSymbol(ByteBuffer buffer, String symbol) {
            byte[] bytes = symbol.getBytes(StandardCharsets.US_ASCII);
            if (bytes.length < 256) {
                buffer.put(FormatCode._symbol8);
                buffer.put((byte)bytes.length);
            } else {
                buffer.put(FormatCode._symbol32);
                buffer.putInt(bytes.length);
            }
            buffer.put(bytes);
        }
        
        protected static String readSymbol(ByteBuffer buffer) throws Exception {
            byte formatCode = buffer.get();
            int len;
            if (formatCode == FormatCode._symbol8) {
                len = buffer.get();
            } else {
                FormatCode.assertEqual(formatCode, FormatCode._symbol32);
                len = buffer.getInt();
            }
            return PrimitiveType.readString(buffer, len, StandardCharsets.US_ASCII);
        }
        
        protected static int readCount(ByteBuffer buffer, byte formatCode,
                byte zeroCode, byte smallCode, byte bigCode) throws Exception {
            int count;
            if (formatCode == zeroCode) {
                count = 0;
            } else if (formatCode == smallCode) {
                buffer.get();
                count = buffer.get();
            } else {
                FormatCode.assertEqual(formatCode, bigCode);
                buffer.getInt();
                count = buffer.getInt();
            }
            return count;
        }
    }
    
    private static class CompositListEncoder extends CompositEncoder {
        public CompositListEncoder(Class type, String name) {
            super(type, name);
        }
        
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) throws Exception {
            buffer.put(FormatCode._list32);
            int sizePos = buffer.position();
            buffer.putInt(0);
            buffer.putInt(this.fields.length);
            for (FieldInfo field : this.fields) {
                Object value = field.field.get(obj);
                this.customType.writeObject(buffer, value, path);
            }            
            buffer.putInt(sizePos, buffer.position() - sizePos + 4);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            Object obj = this.ctor.newInstance();
            int count = readCount(buffer, formatCode, FormatCode._list0,
                    FormatCode._list8, FormatCode._list32);
            for (int i = 0; i < count && i < this.fields.length; i++) {
                Object v = this.fields[i].encoder.read(buffer);
                if (v != null) {
                    this.fields[i].field.set(obj, v);
                }
            }
            return obj;
        }
    }
    
    private static class CompositMapEncoder extends CompositEncoder {
        private final HashMap<String, Field> fieldMap;
        
        public CompositMapEncoder(Class type, String name) {
            super(type, name);
            this.fieldMap = new HashMap();
        }
        
        @Override
        public void init(CustomType customType, Constructor ctor,
                FieldInfo[] fields, Map<String, Encoder> knownTypes) {
            super.init(customType, ctor, fields, knownTypes);
            for (FieldInfo fi : this.fields) {
                this.fieldMap.put(fi.field.getName(), fi.field);
            }
        }
        
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) throws Exception {
            buffer.put(FormatCode._map32);
            int sizePos = buffer.position();
            buffer.putInt(0);
            buffer.putInt(this.fields.length * 2);
            
            for (FieldInfo field : this.fields) {
                Object value = field.field.get(obj);
                writeSymbol(buffer, field.field.getName());
                this.customType.writeObject(buffer, value, path);
            }
            
            buffer.putInt(sizePos, buffer.position() - sizePos + 4);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            Object obj = this.ctor.newInstance();
            int count = readCount(buffer, formatCode, FormatCode._invalid,
                    FormatCode._map8, FormatCode._map32);
            for (int i = 0; i < count / 2 && i < this.fields.length; i++) {
                String symbol = readSymbol(buffer);
                Object value = this.fields[i].encoder.read(buffer);
                Field f = this.fieldMap.get(symbol);
                if (f == null) {
                    throw new Exception("Field not found " + symbol);
                }
                if (value != null) {
                    f.set(obj, value);
                }
            }
            return obj;
        }
    }
    
    public void writeObject(ByteBuffer buffer, Object graph, HashSet path) throws Exception {
        if (graph == null) {
            buffer.put(FormatCode._null);
            return;
        }
        if (path.contains(graph)) {
            throw new AmqpIoException("Cyclic object reference not supported");
        }
        path.add(graph);
        Class c = graph.getClass();
        Encoder encoder = this.getEncoder(c, null);
        if (encoder == null) {
            throw new AmqpIoException("Not supported type " + c.getName());
        }
        encoder.write(buffer, graph, path);
        path.remove(graph);
    }
    
    public Object readObject(Class c, ByteBuffer buffer) throws Exception {
        if (c.equals(Object.class)) {
            return PrimitiveType.readObject(buffer);
        }
        Encoder encoder = this.getEncoder(c, new HashMap());
        if (encoder == null) {
            throw new AmqpIoException("Not supported type " + c.getName());
        }
        return encoder.read(buffer);
    }
    
    private Encoder getEncoder(final Class c,
            final HashMap<Class, Encoder> inBuild) throws Exception {
        Encoder encoder;
        if ((encoder = PrimitiveType.getEncoder(c)) != null) {
            return encoder;
        }
        if (c.isEnum()) {
            return new EnumEncoder(c);
        }
        encoder = customEncoders.get(c);
        if (encoder == null) {
            if (inBuild != null) {
                encoder = inBuild.get(c);
            }
            if (encoder == null) {
                encoder = createEncoder(c, inBuild);
                customEncoders.put(c, encoder);
            }
        }
        return encoder;
    }
    
    private Encoder createEncoder(final Class c,
            HashMap<Class, Encoder> inBuild) throws Exception {
        AmqpContract contract = c.isAnnotationPresent(AmqpContract.class)
                ? (AmqpContract)c.getAnnotation(AmqpContract.class)
                : null;
        EncodingType encoding = contract == null
                ? EncodingType.DescribedList
                : contract.type();
        String name = contract == null ? c.getName() : contract.name();
        CompositEncoder encoder = encoding == EncodingType.DescribedList
                ? new CompositListEncoder(c, name)
                : new CompositMapEncoder(c, name);
        if (inBuild == null) {
            inBuild = new HashMap();
        }
        inBuild.put(c, encoder);
        
        ArrayList<FieldInfo> fields = new ArrayList();
        int order = 0;
        Stack<Class> stack = new Stack();
        Class curr = c;
        while (curr != null && !curr.equals(Object.class)) {
            stack.push(curr);
            curr = curr.getSuperclass();
        }
        while (stack.size() > 0) {
            curr = stack.pop();
            for (Field field : curr.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if ((modifiers & Modifier.STATIC) != 0 ||
                    (modifiers & Modifier.TRANSIENT) != 0) {
                    continue;
                }
                if ((modifiers & Modifier.PUBLIC) == 0) {
                    field.setAccessible(true);
                }
                Encoder fieldEncoder = getEncoder(field.getType(), inBuild);
                if (contract == null) {
                    fields.add(new FieldInfo(
                            field.getName(),
                            order++,
                            field,
                            fieldEncoder));
                } else if (field.isAnnotationPresent(AmqpMember.class)) {
                    AmqpMember member = (AmqpMember)field.getAnnotation(AmqpMember.class);
                    fields.add(new FieldInfo(
                            "".equals(member.name()) ? field.getName() : member.name(),
                            member.order() >= 0 ? member.order() : order++,
                            field,
                            fieldEncoder));
                }
            }
        }
        
        // sort by order
        Collections.sort(fields, new Comparator<FieldInfo>() {
            @Override
            public int compare(FieldInfo f1, FieldInfo f2) {
                return f1.order  - f2.order;
            }
        });
        
        if (encoding == EncodingType.DescribedList) {
            for (int i = 0; i < fields.size() - 1; i++) {
                if (fields.get(i).order == fields.get(i + 1).order) {
                    throw new Exception("Duplicate order " + fields.get(i).order);
                }
            }
        }
        
        Map<String, Encoder> knownTypes = new HashMap<>();
        if (contract != null && c.isAnnotationPresent(AmqpProvide.class)) {
            AmqpProvide provides = (AmqpProvide)c.getAnnotation(AmqpProvide.class);
            for (Class t : provides.types()) {
                Encoder e = getEncoder(t, inBuild);
                knownTypes.put(((CompositEncoder)e).name, e);
            }
        }
        
        Constructor ctor = c.getDeclaredConstructor();
        ctor.setAccessible(true);
        FieldInfo[] array = fields.toArray(new FieldInfo[fields.size()]);
        encoder.init(this, ctor, array, knownTypes);
        
        inBuild.remove(c);
        
        return encoder;
    }
}