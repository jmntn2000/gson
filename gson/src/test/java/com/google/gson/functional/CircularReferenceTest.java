/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gson.functional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.common.TestTypes.ClassOverridingEquals;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import junit.framework.TestCase;

/**
 * Functional tests related to circular reference detection and error reporting.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class CircularReferenceTest extends TestCase {
  private Gson gson;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
      gson = new Gson(false, true, false, true, true);
  }

  public void testCircularSerialization() throws Exception {
//    ContainsReferenceToSelfType a = new ContainsReferenceToSelfType();
//    ContainsReferenceToSelfType b = new ContainsReferenceToSelfType();
//    a.children.add(b);
//    b.children.add(a);
      Order a = new Order();
      a.id=new BigInteger("1");
      a.orderName="Order A";
      a.parts = new ArrayList<Parts>();
      Parts l1 = new Parts();
      Parts l2 = new Parts();
      Parts l3 = new Parts();
      l1.id = new BigInteger("1");
      l1.order = a;
      l1.part="Part 1";
      l2.id = new BigInteger("2");
      l2.order = a;
      l2.part="Part 2";
      l3.order = a;
      l3.id = new BigInteger("3");
      l3.part="Part 3";
      a.parts.add(l1);
      a.parts.add(l2);
      a.parts.add(l3);
      a.parts.add(l3);
    try {
        String res = gson.toJson(l1);
        System.out.println(res);
        System.out.println();
    } catch (Exception expected) {
        fail("Circular types should not get printed!");
    }
  }

  public void testSelfReferenceIgnoredInSerialization() throws Exception {
    ClassOverridingEquals objA = new ClassOverridingEquals();
    objA.ref = objA;

    String json = gson.toJson(objA);
    assertFalse(json.contains("ref")); // self-reference is ignored
  }

  public void testSelfReferenceArrayFieldSerialization() throws Exception {
    ClassWithSelfReferenceArray objA = new ClassWithSelfReferenceArray();
    objA.children = new ClassWithSelfReferenceArray[]{objA};

    try {
      gson.toJson(objA);
      fail("Circular reference to self can not be serialized!");
    } catch (StackOverflowError expected) {
    }
  }

  public void testSelfReferenceCustomHandlerSerialization() throws Exception {
    ClassWithSelfReference obj = new ClassWithSelfReference();
    obj.child = obj;
    Gson gson = new GsonBuilder().registerTypeAdapter(ClassWithSelfReference.class, new JsonSerializer<ClassWithSelfReference>() {
      public JsonElement serialize(ClassWithSelfReference src, Type typeOfSrc,
          JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("property", "value");
        obj.add("child", context.serialize(src.child));
        return obj;
      }
    }).create();
    try {
      gson.toJson(obj);
      fail("Circular reference to self can not be serialized!");
    } catch (StackOverflowError expected) {
    }
  }

  public void testDirectedAcyclicGraphSerialization() throws Exception {
    ContainsReferenceToSelfType a = new ContainsReferenceToSelfType();
    ContainsReferenceToSelfType b = new ContainsReferenceToSelfType();
    ContainsReferenceToSelfType c = new ContainsReferenceToSelfType();
    a.children.add(b);
    a.children.add(c);
    b.children.add(c);
    assertNotNull(gson.toJson(a));
  }

  public void testDirectedAcyclicGraphDeserialization() throws Exception {
    String json = "{\"children\":[{\"children\":[{\"children\":[]}]},{\"children\":[]}]}";
    ContainsReferenceToSelfType target = gson.fromJson(json, ContainsReferenceToSelfType.class);
    assertNotNull(target);
    assertEquals(2, target.children.size());
  }

  private static class ContainsReferenceToSelfType {
    Collection<ContainsReferenceToSelfType> children = new ArrayList<ContainsReferenceToSelfType>();
  }

  private static class ClassWithSelfReference {
    ClassWithSelfReference child;
  }

  private static class ClassWithSelfReferenceArray {
    @SuppressWarnings("unused")
    ClassWithSelfReferenceArray[] children;
  }
  
  private static class Parts {
      BigInteger id;
      Order order;
      String part;
  }
  
  private static class Order {
      BigInteger id;
      List<Parts> parts;
      String orderName;
  }
}
