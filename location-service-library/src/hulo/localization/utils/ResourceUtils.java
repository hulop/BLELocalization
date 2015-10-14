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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

public class ResourceUtils {

	private static ResourceUtils ru = null;

	private ResourceUtils(){

	}

	public static ResourceUtils getInstance(){
		if(ru==null){
			ru = new ResourceUtils();
		}
		return ru;
	}

	String resType = "";
	public void setResourceType(String resType){
		this.resType = resType;
	}

	public String getResourceType(){
		return resType;
	}

	public InputStream getInputStream(String resName){
		if(resType.equals("file")){
			return FileIOUtils.newFileInputStream(resName);
		}
		return ResourceIOUtils.getInputStream(resName);
	}

	public URL getResourceURL(String resName){
		if(resType.equals("file")){
			return FileIOUtils.getResouceURL(resName);
		}
		return ResourceIOUtils.getResourceURL(resName);

	}
	public List<String> getResourceNames(String resName){
		return getResourceNames(resName, "");
	}

	public List<String> getResourceNames(String resName, String filteredExt){
		if(resType.equals("file")){
			return FileIOUtils.getResourceNames(resName, filteredExt);
		}
		return ResourceIOUtils.getResourceNames(resName, filteredExt);
	}

	public OutputStream getOutputStream(String resName){
		if(resType.equals("file")){
			return FileIOUtils.newFileOutputStream(resName);
		}else{
			return null;
		}

	}

}
