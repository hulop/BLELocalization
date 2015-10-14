/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/

package hulo.localization.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.wink.json4j.JSONObject;



public class ClassUtils {

	public static Object scanFieldsInJSON(Object obj, JSONObject json, HashMap<String, Method> methodMap){
		outer: for (Iterator<?> i = json.keySet().iterator(); i.hasNext();) {
			Object key = i.next();
			for (Field f : obj.getClass().getDeclaredFields()) {
				if (f.getName().equals(key)) {
					try {
						f.setAccessible(true);
						Method m = methodMap.get(key);
						if (m != null) {
							f.set(obj, m.invoke(null, json.get(key)));
						} else {
							f.set(obj, json.get(key));
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
					continue outer;
				}
			}
			System.out.println(String.format("No match field %s", key) + " in " + obj.getClass().getSimpleName());
		}
		return obj;
	}

	static public String summary(Object obj){
		Class<?> cls = obj.getClass();
		Field[] fields = obj.getClass().getDeclaredFields();
		List<Field> fs = Arrays.asList(fields);

		Iterator<Field> iter = fs.iterator();

		StringBuilder sb = new StringBuilder();
		sb.append(cls.getSimpleName());
		sb.append("(");

		while(iter.hasNext()){
			Field f = iter.next();
			f.setAccessible(true);
			String name = f.getName();
			try {
				sb.append(name+"="+f.get(obj));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
			if(iter.hasNext()){
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	static public Class<?> findClassSimpleName(String simpleName, Set<Class<?>> clsSet){
		Iterator<Class<?>> iter = clsSet.iterator();
		Class<?> cls = null;
		while(iter.hasNext()){
			Class<?> c = iter.next();
			if(c.getSimpleName().equals(simpleName)){
				cls = c;
			}
		}
		return cls;
	}

}
