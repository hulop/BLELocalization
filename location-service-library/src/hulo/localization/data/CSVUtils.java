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

package hulo.localization.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVUtils {

	private CSVUtils(){
		throw new AssertionError();
	}

	static String[][] readCSVasStrings(InputStream is){
		Reader in = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(in);

		List<String[]> stringsList = new ArrayList<String[]>();
		String line = null;
		try {
			while( (line=br.readLine()) != null ){
				String[] tokens = line.split(",");
				stringsList.add(tokens);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[][] strings = stringsList.toArray(new String[0][]);
		return strings;
	}

	public static List<Map<String, String>> readCSVasListMap(InputStream is){

		String[][] strings = readCSVasStrings(is);

		String[] header = strings[0];
		String[][] data = Arrays.copyOfRange(strings, 1, strings.length);

		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		for(String[] datum: data){
			Map<String, String> map = new HashMap<String, String>();
			for(int i=0; i<header.length; i++){
				String key = header[i];
				String value = datum[i];
				map.put(key, value);
			}
			list.add(map);
		}
		return list;
	}


}
