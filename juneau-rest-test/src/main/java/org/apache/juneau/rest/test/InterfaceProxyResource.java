// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.test;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.jena.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests inteface proxies exposed through <code>@RestMethod(name="PROXY")</code>
 */
@RestResource(
	path="/testInterfaceProxyResource")
public class InterfaceProxyResource extends RestServletJenaDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test that Q-values are being resolved correctly.
	//====================================================================================================
	@RestMethod(name="PROXY", path="/proxy/*")
	public InterfaceProxy getProxy() {
		return new InterfaceProxy() {
			@Override
			public void returnVoid() {
			}
			@Override
			public Integer returnInteger() {
				return 1;
			}
			@Override
			public int returnInt() {
				return 1;
			}
			@Override
			public boolean returnBoolean() {
				return true;
			}
			@Override
			public float returnFloat() {
				return 1f;
			}
			@Override
			public Float returnFloatObject() {
				return 1f;
			}
			@Override
			public String returnString() {
				return "foobar";
			}
			@Override
			public String returnNullString() {
				return null;
			}
			@Override
			public int[][][] returnInt3dArray() {
				return new int[][][]{{{1,2}}};
			}
			@Override
			public Integer[][][] returnInteger3dArray() {
				return new Integer[][][]{{{1,null}}};
			}
			@Override
			public String[][][] returnString3dArray() {
				return new String[][][]{{{"foo","bar",null}}};
			}
			@Override
			public List<Integer> returnIntegerList() {
				return asList(new Integer[]{1,null});
			}
			@Override
			public List<List<List<Integer>>> returnInteger3dList() {
				return new AList<List<List<Integer>>>()
				.append(
					new AList<List<Integer>>()
					.append(
						new AList<Integer>().append(1).append(null)
					)
				);
			}
			@Override
			public List<Integer[][][]> returnInteger1d3dList() {
				return new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null);
			}
			@Override
			public List<int[][][]> returnInt1d3dList() {
				return new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null);
			}
			@Override
			public List<String> returnStringList() {
				return asList(new String[]{"foo","bar",null});
			}
			@Override
			public Bean returnBean() {
				return new Bean().init();
			}
			@Override
			public Bean[][][] returnBean3dArray() {
				return new Bean[][][]{{{new Bean().init(),null},null},null};
			}
			@Override
			public List<Bean> returnBeanList() {
				return asList(new Bean().init());
			}
			@Override
			public List<Bean[][][]> returnBean1d3dList() {
				return new AList<Bean[][][]>().append(new Bean[][][]{{{new Bean().init(),null},null},null}).append(null);
			}
			@Override
			public Map<String,Bean> returnBeanMap() {
				return new AMap<String,Bean>().append("foo",new Bean().init());
			}
			@Override
			public Map<String,List<Bean>> returnBeanListMap() {
				return new AMap<String,List<Bean>>().append("foo",asList(new Bean().init()));
			}
			@Override
			public Map<String,List<Bean[][][]>> returnBean1d3dListMap() {
				return new AMap<String,List<Bean[][][]>>().append("foo", new AList<Bean[][][]>().append(new Bean[][][]{{{new Bean().init(),null},null},null}).append(null));
			}
			@Override
			public Map<Integer,List<Bean>> returnBeanListMapIntegerKeys() {
				return new AMap<Integer,List<Bean>>().append(1,asList(new Bean().init()));
			}
			@Override
			public SwappedPojo returnSwappedPojo() {
				return new SwappedPojo();
			}
			@Override
			public SwappedPojo[][][] returnSwappedPojo3dArray() {
				return new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null};
			}
			@Override
			public void throwException1() throws InterfaceProxy.InterfaceProxyException1 {
				throw new InterfaceProxy.InterfaceProxyException1("foo");
			}
			@Override
			public void throwException2() throws InterfaceProxy.InterfaceProxyException2 {
				throw new InterfaceProxy.InterfaceProxyException2();
			}
			@Override
			public void setNothing() {
			}
			@Override
			public void setInt(int x) {
				assertEquals(1, x);
			}
			@Override
			public void setInteger(Integer x) {
				assertEquals((Integer)1, x);
			}
			@Override
			public void setBoolean(boolean x) {
				assertTrue(x);
			}
			@Override
			public void setFloat(float x) {
				assertTrue(1f == x);
			}
			@Override
			public void setFloatObject(Float x) {
				assertTrue(1f == x);
			}
			@Override
			public void setString(String x) {
				assertEquals("foo", x);
			}
			@Override
			public void setNullString(String x) {
				assertNull(x);
			}
			@Override
			public void setInt3dArray(int[][][] x) {
				assertObjectEquals("[[[1,2]]]", x);
			}
			@Override
			public void setInteger3dArray(Integer[][][] x) {
				assertObjectEquals("[[[1,null]]]", x);
			}
			@Override
			public void setString3dArray(String[][][] x) {
				assertObjectEquals("[[['foo','bar',null]]]", x);
			}
			@Override
			public void setIntegerList(List<Integer> x) {
				assertObjectEquals("[1,null]", x);
				assertEquals(Integer.class, x.get(0).getClass());
			}
			@Override
			public void setInteger2dList(List<List<Integer>> x) {
				assertObjectEquals("[[1,null]]", x);
				assertEquals(Integer.class, x.get(0).get(0).getClass());
			}
			@Override
			public void setInteger3dList(List<List<List<Integer>>> x) {
				assertObjectEquals("[[[1,null]]]", x);
				assertEquals(Integer.class, x.get(0).get(0).get(0).getClass());
			}
			@Override
			public void setInteger1d3dList(List<Integer[][][]> x) {
				assertObjectEquals("[[[[1,null],null],null],null]", x);
				assertEquals(Integer[][][].class, x.get(0).getClass());
				assertEquals(Integer.class, x.get(0)[0][0][0].getClass());
			}
			@Override
			public void setInt1d3dList(List<int[][][]> x) {
				assertObjectEquals("[[[[1,2],null],null],null]", x);
				assertEquals(int[][][].class, x.get(0).getClass());
			}
			@Override
			public void setStringList(List<String> x) {
				assertObjectEquals("['foo','bar',null]", x);
			}
			@Override
			public void setBean(Bean x) {
				assertObjectEquals("{a:1,b:'foo'}", x);
			}
			@Override
			public void setBean3dArray(Bean[][][] x) {
				assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x);
			}
			@Override
			public void setBeanList(List<Bean> x) {
				assertObjectEquals("[{a:1,b:'foo'}]", x);
			}
			@Override
			public void setBean1d3dList(List<Bean[][][]> x) {
				assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x);
			}
			@Override
			public void setBeanMap(Map<String,Bean> x) {
				assertObjectEquals("{foo:{a:1,b:'foo'}}", x);
			}
			@Override
			public void setBeanListMap(Map<String,List<Bean>> x) {
				assertObjectEquals("{foo:[{a:1,b:'foo'}]}", x);
			}
			@Override
			public void setBean1d3dListMap(Map<String,List<Bean[][][]>> x) {
				assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x);
			}
			@Override
			public void setBeanListMapIntegerKeys(Map<Integer,List<Bean>> x) {
				assertObjectEquals("{'1':[{a:1,b:'foo'}]}", x);  // Note: JsonSerializer serializes key as string.
				assertEquals(Integer.class, x.keySet().iterator().next().getClass());
			}
			@Override
			public void setSwappedPojo(SwappedPojo x) {
				assertTrue(x.wasUnswapped);
			}
			@Override
			public void setSwappedPojo3dArray(SwappedPojo[][][] x) {
				assertObjectEquals("[[['[{(<swapped>)}]',null],null],null]", x);
				assertTrue(x[0][0][0].wasUnswapped);
			}
		};
	}

	private static void assertObjectEquals(String e, Object o) {
		Assert.assertEquals(e, JsonSerializer.DEFAULT_LAX.toString(o));
	}
}