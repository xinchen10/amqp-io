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

class FormatCode
{
    public static final byte _described = (byte)0x0;
    public static final byte _invalid = (byte)0xff;
    
    public static final byte _null = (byte)0x40;
    public static final byte _bool = (byte)0x56;
    public static final byte _boolTrue = (byte)0x41;
    public static final byte _boolFalse = (byte)0x42;
    public static final byte _ubyte = (byte)0x50;
    public static final byte _ushort = (byte)0x60;
    public static final byte _uint = (byte)0x70;
    public static final byte _uintSmall = (byte)0x52;
    public static final byte _uint0 = (byte)0x43;
    public static final byte _ulong = (byte)0x80;
    public static final byte _ulongSmall = (byte)0x53;
    public static final byte _ulong0 = (byte)0x44;
    public static final byte _byte = (byte)0x51;
    public static final byte _short = (byte)0x61;
    public static final byte _int = (byte)0x71;
    public static final byte _intSmall = (byte)0x54;
    public static final byte _long = (byte)0x81;
    public static final byte _longSmall = (byte)0x55;
    public static final byte _float = (byte)0x72;
    public static final byte _double = (byte)0x82;
    public static final byte _decimal32 = (byte)0x74;
    public static final byte _decimal64 = (byte)0x84;
    public static final byte _decimal128 = (byte)0x94;
    public static final byte _char = (byte)0x73;
    public static final byte _timestamp = (byte)0x83;
    public static final byte _uuid = (byte)0x98;
    public static final byte _binary8 = (byte)0xa0;
    public static final byte _binary32 = (byte)0xb0;
    public static final byte _string8 = (byte)0xa1;
    public static final byte _string32 = (byte)0xb1;
    public static final byte _symbol8 = (byte)0xa3;
    public static final byte _symbol32 = (byte)0xb3;
    public static final byte _list0 = (byte)0x45;
    public static final byte _list8 = (byte)0xc0;
    public static final byte _list32 = (byte)0xd0;
    public static final byte _map8 = (byte)0xc1;
    public static final byte _map32 = (byte)0xd1;
    public static final byte _array8 = (byte)0xe0;
    public static final byte _array32 = (byte)0xf0;
    
    public static void assertEqual(byte b1, byte b2) throws Exception {
        if (b1 != b2) {
            throw new Exception("value not equal");
        }
    }
}