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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FileIOUtils {

	public static InputStream newFileInputStream(String str){
		File file = new File(str);
		InputStream is=null;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return is;
	}

	static URL getResouceURL(String resName){
		try {
			File file = new File(resName);
			if(file.exists()){
				return file.toURI().toURL();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	static List<String> getResourceNames(String resName){
		return getResourceNames(resName, "");
	}

	static List<String> getResourceNames(String resName, String filteredExt){
		List<String> list = new ArrayList<>();
		File file = new File(resName);
		File[] files = file.listFiles();
		if(files!=null){
			for(File f: files){
				String name = f.getName();
				if(name.endsWith(filteredExt)){
					list.add(resName + "/" + name);
				}
			}
		}
		return list;
	}

	public static OutputStream newFileOutputStream(String path){
		File file = new File(path);
		try {
			OutputStream out = new FileOutputStream(file);
			return out;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Deprecated
	public static void makeDirs(File file){
		if(!file.exists()){
			System.out.println("Directory created :"+file.getPath());
			file.mkdirs();
		}else{
			System.out.println("Directory already exists: "+file.getPath());
		}
	}

	@Deprecated
	public static void makeDirs(String dir){
		File file = new File(dir);
		makeDirs(file);
	}


}
