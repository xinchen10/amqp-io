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

import java.util.Date;
import java.util.Map;

@AmqpContract(name = "test.amqp:person")
@AmqpProvide(types = { Student.class, Teacher.class, Administrator.class })
public class Person {
    @AmqpMember(order = 1)
    public String name;
    
    @AmqpMember(order = 2)
    public int age;

    @AmqpMember(order = 3)
    public Date dateOfBirth;

    @AmqpMember(order = 8)
    public Map properties;
    
    public long ver;
}
