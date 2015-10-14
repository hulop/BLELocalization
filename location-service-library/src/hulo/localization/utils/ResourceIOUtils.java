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
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceIOUtils {

	static String resolveSlashes(String str){
		String strNew = str;
		if( ! str.substring(0,1).equals("/")){
			strNew = "/" + str;
		}
		if(str.contains("//")){
			strNew = str.replace("//", "/");
			return resolveSlashes(strNew);
		}else{
			return strNew;
		}
	}

	static InputStream getInputStream(String resName){
		resName = resolveSlashes(resName);
		return ResourceUtils.class.getResourceAsStream(resName);
	}

	static URL getResourceURL(String resName){
		URL url = ResourceUtils.class.getResource(resName);
		return url;
	}

	static List<String> getResourceNames(String resName){
		return getResourceNames(resName, "");
	}

	static List<String> getResourceNames(String resName, String filteredExt){
		String resNameSub = resName.substring(1);

		List<String> list = new ArrayList<>();
		URL url = ResourceUtils.class.getResource(resName);
		if(url==null){
			System.out.println("Resource was not found. [Resouce name="+resName+"]");
			return list;
		}

		try{
			if(url.getProtocol().equals("file")){
				File file = new File(url.toURI());
				File[] files = file.listFiles();
				if(files!=null){
					for(File f: files){
						String name = f.getName();
						if(name.endsWith(filteredExt)){
							list.add(resName + "/" + name);
						}
					}
				}
			}else if(url.getProtocol().equals("jar")){
				JarURLConnection jarURLConn = (JarURLConnection) url.openConnection();
				JarFile jarFile = jarURLConn.getJarFile();
				Enumeration<JarEntry> jarEnum =  jarFile.entries();
				while(jarEnum.hasMoreElements()){
					JarEntry entry = jarEnum.nextElement();
					String name = entry.getName();
					if(name.startsWith(resName) || name.startsWith(resNameSub)){
						if(name.endsWith(filteredExt)){
							list.add(name);
						}
					}
				}
				jarFile.close();
			}else if(url.getProtocol().equals("zip")){
				File file = new File(url.toURI());
				ZipFile zipFile = new ZipFile(file);
				Enumeration<? extends ZipEntry> zipEnum = zipFile.entries();
				while(zipEnum.hasMoreElements()){
					ZipEntry entry = zipEnum.nextElement();
					String name = entry.getName();
					if(name.startsWith(resName) || name.startsWith(resNameSub)){
						if(name.endsWith(filteredExt)){
							list.add(name);
						}
					}
				}
				zipFile.close();
			}
		}catch(IOException | URISyntaxException e){
			e.printStackTrace();
		}
		return list;
	}

	public static void close(InputStream is){
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
