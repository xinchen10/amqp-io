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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class PrimitiveType {
    private static abstract class EncoderImpl implements Encoder {
        private final byte formatCode;
        
        public EncoderImpl(byte formatCode) {
            this.formatCode = formatCode;
        }
        
        @Override
        public void write(ByteBuffer buffer, Object obj, HashSet path) throws Exception {
            if (obj == null) {
                buffer.put(FormatCode._null);
            } else {
                buffer.put(this.formatCode);
                this.writeRaw(buffer, obj, path);
            }
        }
        
        @Override
        public Object read(ByteBuffer buffer) throws Exception {
            byte code = buffer.get();
            if (code == FormatCode._null) {
                return null;
            } else {
                return this.readRaw(buffer, code);
            }
        }
    }
    
    private static final Encoder nullType = new EncoderImpl(FormatCode._null) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            FormatCode.assertEqual(FormatCode._null, formatCode);
            return null;
        }
    };
    private static final Encoder boolType = new EncoderImpl(FormatCode._bool) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            buffer.put((Boolean)obj ? (byte)1 : (byte)0);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            if (formatCode == FormatCode._boolTrue) {
                return true;
            } else if (formatCode == FormatCode._boolFalse) {
                return false;
            } else {
                FormatCode.assertEqual(FormatCode._bool, formatCode);
                return buffer.get() != 0;
            }
        }
    };
    private static final Encoder byteType = new EncoderImpl(FormatCode._byte) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            buffer.put((Byte)obj);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            FormatCode.assertEqual(FormatCode._byte, formatCode);
            return buffer.get();
        }
    };
    private static final Encoder shortType = new EncoderImpl(FormatCode._short) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            buffer.putShort((Short)obj);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            FormatCode.assertEqual(FormatCode._short, formatCode);
            return buffer.getShort();
        }
    };
    private static final Encoder intType = new EncoderImpl(FormatCode._int) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            buffer.putInt((Integer)obj);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            FormatCode.assertEqual(FormatCode._int, formatCode);
            return buffer.getInt();
        }
    };
    private static final Encoder longType = new EncoderImpl(FormatCode._long) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            buffer.putLong((Long)obj);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            if (formatCode == FormatCode._longSmall) {
                return buffer.get();
            } else {
                FormatCode.assertEqual(FormatCode._long, formatCode);
                return buffer.getLong();
            }
        }
    };
    private static final Encoder floatType = new EncoderImpl(FormatCode._float) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            buffer.putFloat((Float)obj);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            FormatCode.assertEqual(FormatCode._float, formatCode);
            return buffer.getFloat();
        }
    };
    private static final Encoder doubleType = new EncoderImpl(FormatCode._double) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            buffer.putDouble((Double)obj);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            FormatCode.assertEqual(FormatCode._double, formatCode);
            return buffer.getDouble();
        }
    };
    private static final Encoder charType = new EncoderImpl(FormatCode._char) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            buffer.putInt((int)(Character)obj & 0xffff);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            FormatCode.assertEqual(FormatCode._char, formatCode);
            return (char)(buffer.getInt() & 0xffff);
        }
    };
    private static final Encoder timestampType = new EncoderImpl(FormatCode._timestamp) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            buffer.putLong(((Date)obj).getTime());
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            FormatCode.assertEqual(FormatCode._timestamp, formatCode);
            return new Date(buffer.getLong());
        }
    };
    private static final Encoder uuidType = new EncoderImpl(FormatCode._uuid) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            UUID uuid = (UUID)obj;
            buffer.putLong(uuid.getMostSignificantBits());
            buffer.putLong(uuid.getLeastSignificantBits());
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            FormatCode.assertEqual(FormatCode._uuid, formatCode);
            long msb = buffer.getLong();
            long lsb = buffer.getLong();
            return new UUID(msb, lsb);
        }
    };
    private static final Encoder binaryType = new EncoderImpl(FormatCode._binary32) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            byte[] bin = (byte[])obj;
            buffer.putInt(bin.length);
            buffer.put(bin);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            int len;
            if (formatCode == FormatCode._binary8) {
                len = buffer.get();
            } else {
                FormatCode.assertEqual(FormatCode._binary32, formatCode);
                len = buffer.getInt();
            }
            byte[] ret = new byte[len];
            return buffer.get(ret).array();
        }
    };
    private static final Encoder stringType = new EncoderImpl(FormatCode._string32) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) {
            byte[] bytes = ((String)obj).getBytes(StandardCharsets.UTF_8);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            int len;
            if (formatCode == FormatCode._string8) {
                len = buffer.get();
            } else {
                FormatCode.assertEqual(FormatCode._string32, formatCode);
                len = buffer.getInt();
            }
            return readString(buffer, len, StandardCharsets.UTF_8);
        }
    };
    private static final Encoder listType = new EncoderImpl(FormatCode._list32) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) throws Exception {
            List list = (List)obj;
            int pos = buffer.position();
            buffer.putInt(0);
            buffer.putInt(list.size());
            for (Object v : list) {
                writeObject(buffer, v, path);
            }
            buffer.putInt(pos, buffer.position() - pos);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            ArrayList list = new ArrayList();
            int count = 0;
            if (formatCode == FormatCode._list0) {
                // Nothing
            } else if (formatCode == FormatCode._list8) {
                buffer.get();
                count = buffer.get();
            } else {
                FormatCode.assertEqual(FormatCode._list32, formatCode);
                buffer.getInt();
                count = buffer.getInt();
            }
            for (int i = 0; i < count; i++) {
                Object v = readObject(buffer);
                list.add(v);
            }
            return list;
        }
    };
    private static final Encoder mapType = new EncoderImpl(FormatCode._map32) {
        @Override
        public void writeRaw(ByteBuffer buffer, Object obj, HashSet path) throws Exception {
            Map map = (Map)obj;
            int pos = buffer.position();
            buffer.putInt(0);
            buffer.putInt(map.size() * 2);
            for (Object k : map.keySet()) {
                Object v = map.get(k);
                writeObject(buffer, k, path);
                writeObject(buffer, v, path);
            }
            buffer.putInt(pos, buffer.position() - pos);
        }

        @Override
        public Object readRaw(ByteBuffer buffer, byte formatCode) throws Exception {
            HashMap map = new HashMap();
            int count = 0;
            if (formatCode == FormatCode._map8) {
                buffer.get();
                count = buffer.get();
            } else {
                FormatCode.assertEqual(FormatCode._map32, formatCode);
                buffer.getInt();
                count = buffer.getInt();
            }
            for (int i = 0; i < count; i += 2) {
                Object k = readObject(buffer);
                Object v = readObject(buffer);
                map.put(k, v);
            }
            return map;
        }
    };
    
    private static final Encoder[] codecArray = new Encoder[] {
        null,           // 0
        nullType,       // 1
        boolType,       // 2
        byteType,       // 3
        shortType,      // 4
        intType,        // 5
        longType,       // 6
        floatType,      // 7
        doubleType,     // 8
        charType,       // 9
        timestampType,  // 10
        uuidType,       // 11
        binaryType,     // 12
        stringType,     // 13
        listType,       // 14
        mapType         // 15
    };
    
    private static final byte[][] codecIndexTable = new byte[][] {
        // 0x40:null, 0x41:boolean.true, 0x42:boolean.false, 0x43:uint0, 0x44:ulong0, 0x45:list0
        new byte[] { 1, 2, 2, 0, 0, 14 },
        // 0x50:ubyte, 0x51:byte, 0x52:small.uint, 0x53:small.ulong, 0x54:small.int, 0x55:small.long, 0x56:boolean
        new byte[] { 0, 3, 0, 0, 5, 6, 2 },
        // 0x60:ushort, 0x61:short
        new byte[] { 0, 4 },
        // 0x70:uint, 0x71:int, 0x72:float, 0x73:char, 0x74:decimal32
        new byte[] { 0, 5, 7, 9, 0 },
        // 0x80:ulong, 0x81:long, 0x82:double, 0x83:timestamp, 0x84:decimal64
        new byte[] { 0, 6, 8, 10, 0 },
        // 0x98:uuid
        new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 11 },
        // 0xa0:bin8, 0xa1:str8, 0xa3:sym8
        new byte[] { 12, 13, 0, 0 },
        // 0xb0:bin32, 0xb1:str32, 0xb3:sym32
        new byte[] { 12, 13, 0, 0 },
        // 0xc0:list8, 0xc1:map8
        new byte[] { 14, 15 },
        // 0xd0:list32, 0xd1:map32
        new byte[] { 14, 15 },
        // 0xe0:array8
        new byte[] { 0 },
        // 0xf0:array32
        new byte[] { 0 }
    };

    private static final Map<Class, Encoder> codecMap = new HashMap<>();
    
    static {
        codecMap.put(Boolean.TYPE, boolType);
        codecMap.put(Byte.TYPE, byteType);
        codecMap.put(Short.TYPE, shortType);
        codecMap.put(Integer.TYPE, intType);
        codecMap.put(Long.TYPE, longType);
        codecMap.put(Float.TYPE, floatType);
        codecMap.put(Double.TYPE, doubleType);
        codecMap.put(Character.TYPE, charType);
        codecMap.put(Boolean.class, boolType);
        codecMap.put(Byte.class, byteType);
        codecMap.put(Short.class, shortType);
        codecMap.put(Integer.class, intType);
        codecMap.put(Long.class, longType);
        codecMap.put(Float.class, floatType);
        codecMap.put(Double.class, doubleType);
        codecMap.put(Character.class, charType);
        codecMap.put(Date.class, timestampType);
        codecMap.put(UUID.class, uuidType);
        codecMap.put(byte[].class, binaryType);
        codecMap.put(String.class, stringType);
    }
    
    public static Encoder getEncoder(Class c) {
        if (List.class.isAssignableFrom(c)) {
            return listType;
        }        
        if (Map.class.isAssignableFrom(c)) {
            return mapType;
        }
        return codecMap.get(c);
    }
    
    public static Encoder getEncoder(byte formatCode) {
        int type = ((formatCode & 0xF0) >> 4) - 4;
        if (type >= 0 && type < codecIndexTable.length) {
            int index = formatCode & 0x0F;
            if (index < codecIndexTable[type].length) {
                return codecArray[codecIndexTable[type][index]];
            }
        }
        return null;
    }
    
    public static String readString(ByteBuffer buffer, int len, Charset charset) {
        String str;
        if (buffer.hasArray()) {
            int pos = buffer.position();
            str = new String(buffer.array(), pos, len, charset);
            buffer.position(pos + len);
        } else {
            byte[] bytes = new byte[len];
            buffer.get(bytes);
            str = new String(bytes, 0, len, charset);
        }
        return str;
    }
    
    public static Object readObject(ByteBuffer buffer) throws Exception {
        byte code = buffer.get();
        Encoder encoder = getEncoder(code);
        if (encoder == null) {
            throw new AmqpIoException("No encoder was found for format code " + code);
        }
        return encoder.readRaw(buffer, code);
    }
    
    public static void writeObject(ByteBuffer buffer, Object obj, HashSet path) throws Exception {
        if (obj == null) {
            buffer.put(FormatCode._null);
        } else {
            Encoder encoder = getEncoder(obj.getClass());
            if (encoder == null) {
                throw new AmqpIoException("No encoder was found for " + obj.getClass().getName());                
            }
            encoder.write(buffer, obj, path);
        }
    }
}
